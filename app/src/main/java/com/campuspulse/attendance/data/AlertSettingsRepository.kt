package com.campuspulse.attendance.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AlertGatewayConfig(
    val schoolName: String = "Oakridge Elementary Campus",
    val autoSendOnArrival: Boolean = true,
    val autoSendOnLate: Boolean = true,
    val autoSendOnAbsence: Boolean = true,
    val autoSendOnDismissal: Boolean = true,
    val liveDispatchEnabled: Boolean = false, // False = Safe Sandbox Simulation, True = Live SMS/API
    val useAndroidDirectSms: Boolean = true,
    val twilioAccountSid: String = "",
    val twilioAuthToken: String = "",
    val twilioFromSmsNumber: String = "",
    val twilioFromWhatsAppNumber: String = "whatsapp:+14155238886",
    val resendApiKey: String = "",
    val resendFromEmail: String = "alerts@campuspulse.edu",
    val arrivalTemplate: String = "✅ {school}: {student} arrived safely at {class} at {time}.",
    val lateTemplate: String = "⏰ {school}: {student} checked in LATE at {class} at {time}.",
    val absenceTemplate: String = "⚠️ {school}: {student} was marked ABSENT in {class} at {time}. Please contact the attendance office if this is unexpected.",
    val dismissalTemplate: String = "🚌 {school}: {student} was dismissed from campus at {time} via {pickup_method} (Released to: {pickup_person})."
)

class AlertSettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("campuspulse_alert_prefs", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadFromPrefs())
    val config: StateFlow<AlertGatewayConfig> = _config.asStateFlow()

    private fun loadFromPrefs(): AlertGatewayConfig {
        val defaults = AlertGatewayConfig()
        return AlertGatewayConfig(
            schoolName = prefs.getString("schoolName", defaults.schoolName) ?: defaults.schoolName,
            autoSendOnArrival = prefs.getBoolean("autoSendOnArrival", defaults.autoSendOnArrival),
            autoSendOnLate = prefs.getBoolean("autoSendOnLate", defaults.autoSendOnLate),
            autoSendOnAbsence = prefs.getBoolean("autoSendOnAbsence", defaults.autoSendOnAbsence),
            autoSendOnDismissal = prefs.getBoolean("autoSendOnDismissal", defaults.autoSendOnDismissal),
            liveDispatchEnabled = prefs.getBoolean("liveDispatchEnabled", defaults.liveDispatchEnabled),
            useAndroidDirectSms = prefs.getBoolean("useAndroidDirectSms", defaults.useAndroidDirectSms),
            twilioAccountSid = prefs.getString("twilioAccountSid", "") ?: "",
            twilioAuthToken = prefs.getString("twilioAuthToken", "") ?: "",
            twilioFromSmsNumber = prefs.getString("twilioFromSmsNumber", "") ?: "",
            twilioFromWhatsAppNumber = prefs.getString(
                "twilioFromWhatsAppNumber",
                defaults.twilioFromWhatsAppNumber
            ) ?: defaults.twilioFromWhatsAppNumber,
            resendApiKey = prefs.getString("resendApiKey", "") ?: "",
            resendFromEmail = prefs.getString("resendFromEmail", defaults.resendFromEmail)
                ?: defaults.resendFromEmail,
            arrivalTemplate = prefs.getString("arrivalTemplate", defaults.arrivalTemplate)
                ?: defaults.arrivalTemplate,
            lateTemplate = prefs.getString("lateTemplate", defaults.lateTemplate)
                ?: defaults.lateTemplate,
            absenceTemplate = prefs.getString("absenceTemplate", defaults.absenceTemplate)
                ?: defaults.absenceTemplate,
            dismissalTemplate = prefs.getString("dismissalTemplate", defaults.dismissalTemplate)
                ?: defaults.dismissalTemplate
        )
    }

    fun updateConfig(newConfig: AlertGatewayConfig) {
        prefs.edit().apply {
            putString("schoolName", newConfig.schoolName)
            putBoolean("autoSendOnArrival", newConfig.autoSendOnArrival)
            putBoolean("autoSendOnLate", newConfig.autoSendOnLate)
            putBoolean("autoSendOnAbsence", newConfig.autoSendOnAbsence)
            putBoolean("autoSendOnDismissal", newConfig.autoSendOnDismissal)
            putBoolean("liveDispatchEnabled", newConfig.liveDispatchEnabled)
            putBoolean("useAndroidDirectSms", newConfig.useAndroidDirectSms)
            putString("twilioAccountSid", newConfig.twilioAccountSid.trim())
            putString("twilioAuthToken", newConfig.twilioAuthToken.trim())
            putString("twilioFromSmsNumber", newConfig.twilioFromSmsNumber.trim())
            putString("twilioFromWhatsAppNumber", newConfig.twilioFromWhatsAppNumber.trim())
            putString("resendApiKey", newConfig.resendApiKey.trim())
            putString("resendFromEmail", newConfig.resendFromEmail.trim())
            putString("arrivalTemplate", newConfig.arrivalTemplate)
            putString("lateTemplate", newConfig.lateTemplate)
            putString("absenceTemplate", newConfig.absenceTemplate)
            putString("dismissalTemplate", newConfig.dismissalTemplate)
            apply()
        }
        _config.value = newConfig
    }
}
