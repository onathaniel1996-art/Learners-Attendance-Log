package com.campuspulse.attendance.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.campuspulse.attendance.data.AlertGatewayConfig
import com.campuspulse.attendance.data.AlertSettingsRepository
import com.campuspulse.attendance.data.AttendanceDao
import com.campuspulse.attendance.data.model.AlertChannel
import com.campuspulse.attendance.data.model.AlertEventType
import com.campuspulse.attendance.data.model.ClassroomEntity
import com.campuspulse.attendance.data.model.DeliveryStatus
import com.campuspulse.attendance.data.model.ParentAlertLogEntity
import com.campuspulse.attendance.data.model.PickupMethod
import com.campuspulse.attendance.data.model.StudentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AlertDispatchResult(
    val logEntry: ParentAlertLogEntity,
    val wasSuccessful: Boolean
)

class ParentAlertDispatcher(
    private val context: Context,
    private val dao: AttendanceDao,
    private val settingsRepository: AlertSettingsRepository
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

    suspend fun dispatchStudentEventAlert(
        student: StudentEntity,
        classroom: ClassroomEntity,
        eventType: AlertEventType,
        timestampMillis: Long = System.currentTimeMillis(),
        pickupMethod: PickupMethod? = null,
        dismissedToGuardian: String? = null
    ): AlertDispatchResult? = withContext(Dispatchers.IO) {
        val cfg = settingsRepository.config.value

        // Check if automatic alerts are enabled for this event type
        val shouldTrigger = when (eventType) {
            AlertEventType.ARRIVAL -> cfg.autoSendOnArrival
            AlertEventType.LATE_ARRIVAL -> cfg.autoSendOnLate
            AlertEventType.ABSENCE_NOTICE -> cfg.autoSendOnAbsence
            AlertEventType.DISMISSAL -> cfg.autoSendOnDismissal
        }
        if (!shouldTrigger) return@withContext null

        val formattedTime = timeFormatter.format(Date(timestampMillis))
        val rawTemplate = when (eventType) {
            AlertEventType.ARRIVAL -> cfg.arrivalTemplate
            AlertEventType.LATE_ARRIVAL -> cfg.lateTemplate
            AlertEventType.ABSENCE_NOTICE -> cfg.absenceTemplate
            AlertEventType.DISMISSAL -> cfg.dismissalTemplate
        }

        val messageBody = formatTemplate(
            template = rawTemplate,
            schoolName = cfg.schoolName,
            studentName = student.fullName,
            guardianName = student.guardianName,
            classroomName = "${classroom.name} (${classroom.roomNumber})",
            timeString = formattedTime,
            pickupMethod = (pickupMethod ?: student.defaultPickupMethod).label,
            pickupPerson = dismissedToGuardian?.takeIf { it.isNotBlank() } ?: student.guardianName
        )

        val channel = resolveEffectiveChannel(student.preferredChannel, cfg)
        val recipient = if (channel == AlertChannel.RESEND_EMAIL) {
            student.guardianEmail
        } else {
            student.guardianPhone
        }

        val (status, detail) = executeChannelSend(
            channel = channel,
            recipient = recipient,
            subject = "${cfg.schoolName} • ${eventType.label} (${student.fullName})",
            messageBody = messageBody,
            cfg = cfg
        )

        val log = ParentAlertLogEntity(
            studentId = student.id,
            studentName = student.fullName,
            classroomName = classroom.name,
            guardianName = "${student.guardianName} (${student.guardianRelationship})",
            recipientContact = recipient,
            channel = channel,
            eventType = eventType,
            messageBody = messageBody,
            deliveryStatus = status,
            timestampMillis = timestampMillis,
            statusDetail = detail
        )

        val insertedId = dao.insertParentAlertLog(log)
        val savedLog = log.copy(id = insertedId)
        AlertDispatchResult(
            logEntry = savedLog,
            wasSuccessful = status == DeliveryStatus.SENT || status == DeliveryStatus.SIMULATED
        )
    }

    suspend fun retryAlertLog(log: ParentAlertLogEntity): AlertDispatchResult =
        withContext(Dispatchers.IO) {
            val cfg = settingsRepository.config.value
            val (status, detail) = executeChannelSend(
                channel = log.channel,
                recipient = log.recipientContact,
                subject = "${cfg.schoolName} • ${log.eventType.label} (${log.studentName})",
                messageBody = log.messageBody,
                cfg = cfg
            )
            val updated = log.copy(
                deliveryStatus = status,
                timestampMillis = System.currentTimeMillis(),
                statusDetail = detail
            )
            dao.updateParentAlertLog(updated)
            AlertDispatchResult(
                logEntry = updated,
                wasSuccessful = status == DeliveryStatus.SENT || status == DeliveryStatus.SIMULATED
            )
        }

    private fun resolveEffectiveChannel(
        preferred: AlertChannel,
        cfg: AlertGatewayConfig
    ): AlertChannel {
        if (preferred == AlertChannel.ANDROID_SMS && !cfg.useAndroidDirectSms && cfg.twilioAccountSid.isNotBlank()) {
            return AlertChannel.TWILIO_SMS
        }
        return preferred
    }

    private fun executeChannelSend(
        channel: AlertChannel,
        recipient: String,
        subject: String,
        messageBody: String,
        cfg: AlertGatewayConfig
    ): Pair<DeliveryStatus, String> {
        if (!cfg.liveDispatchEnabled) {
            return DeliveryStatus.SIMULATED to "Sandbox mode active • Instant preview logged (${channel.label})"
        }

        return try {
            when (channel) {
                AlertChannel.ANDROID_SMS -> sendViaAndroidSmsManager(recipient, messageBody)
                AlertChannel.TWILIO_SMS -> sendViaTwilioRest(
                    recipient = recipient,
                    messageBody = messageBody,
                    cfg = cfg,
                    isWhatsApp = false
                )
                AlertChannel.WHATSAPP -> sendViaTwilioRest(
                    recipient = recipient,
                    messageBody = messageBody,
                    cfg = cfg,
                    isWhatsApp = true
                )
                AlertChannel.RESEND_EMAIL -> sendViaResendEmail(
                    recipientEmail = recipient,
                    subject = subject,
                    messageBody = messageBody,
                    cfg = cfg
                )
            }
        } catch (e: Exception) {
            DeliveryStatus.FAILED to (e.localizedMessage ?: "Dispatch exception")
        }
    }

    private fun sendViaAndroidSmsManager(
        phoneNumber: String,
        messageBody: String
    ): Pair<DeliveryStatus, String> {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return DeliveryStatus.QUEUED_OFFLINE to "SEND_SMS permission not granted yet. Grant in Settings and tap Retry."
        }

        val smsManager: SmsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        if (smsManager == null) {
            return DeliveryStatus.FAILED to "Device does not have an active cellular SmsManager."
        }

        val parts = smsManager.divideMessage(messageBody)
        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
        return DeliveryStatus.SENT to "Sent directly via Android Cellular SmsManager to $phoneNumber"
    }

    private fun sendViaTwilioRest(
        recipient: String,
        messageBody: String,
        cfg: AlertGatewayConfig,
        isWhatsApp: Boolean
    ): Pair<DeliveryStatus, String> {
        if (cfg.twilioAccountSid.isBlank() || cfg.twilioAuthToken.isBlank()) {
            return DeliveryStatus.QUEUED_OFFLINE to "Twilio SID/Auth Token not configured. Queued for retry."
        }

        val fromValue = if (isWhatsApp) {
            if (cfg.twilioFromWhatsAppNumber.startsWith("whatsapp:")) {
                cfg.twilioFromWhatsAppNumber
            } else {
                "whatsapp:${cfg.twilioFromWhatsAppNumber}"
            }
        } else {
            cfg.twilioFromSmsNumber
        }

        val toValue = if (isWhatsApp && !recipient.startsWith("whatsapp:")) {
            "whatsapp:$recipient"
        } else {
            recipient
        }

        if (fromValue.isBlank()) {
            return DeliveryStatus.QUEUED_OFFLINE to "Twilio sender phone number is missing in Settings."
        }

        val url = "https://api.twilio.com/2010-04-01/Accounts/${cfg.twilioAccountSid}/Messages.json"
        val formBody = FormBody.Builder()
            .add("To", toValue)
            .add("From", fromValue)
            .add("Body", messageBody)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", Credentials.basic(cfg.twilioAccountSid, cfg.twilioAuthToken))
            .post(formBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            return if (response.isSuccessful) {
                val modeLabel = if (isWhatsApp) "Twilio WhatsApp API" else "Twilio SMS API"
                DeliveryStatus.SENT to "Dispatched via $modeLabel (HTTP ${response.code})"
            } else {
                DeliveryStatus.FAILED to "Twilio API HTTP ${response.code}: ${response.message}"
            }
        }
    }

    private fun sendViaResendEmail(
        recipientEmail: String,
        subject: String,
        messageBody: String,
        cfg: AlertGatewayConfig
    ): Pair<DeliveryStatus, String> {
        if (cfg.resendApiKey.isBlank()) {
            return DeliveryStatus.QUEUED_OFFLINE to "Resend Email API key not configured. Queued for retry."
        }

        val payload = JSONObject().apply {
            put("from", cfg.resendFromEmail)
            put("to", JSONArray().put(recipientEmail))
            put("subject", subject)
            put("text", messageBody)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url("https://api.resend.com/emails")
            .header("Authorization", "Bearer ${cfg.resendApiKey}")
            .post(payload.toString().toRequestBody(mediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            return if (response.isSuccessful) {
                DeliveryStatus.SENT to "Dispatched via Resend Email API to $recipientEmail"
            } else {
                DeliveryStatus.FAILED to "Resend API HTTP ${response.code}: ${response.message}"
            }
        }
    }

    private fun formatTemplate(
        template: String,
        schoolName: String,
        studentName: String,
        guardianName: String,
        classroomName: String,
        timeString: String,
        pickupMethod: String,
        pickupPerson: String
    ): String {
        return template
            .replace("{school}", schoolName)
            .replace("{student}", studentName)
            .replace("{guardian}", guardianName)
            .replace("{class}", classroomName)
            .replace("{time}", timeString)
            .replace("{pickup_method}", pickupMethod)
            .replace("{pickup_person}", pickupPerson)
    }
}
