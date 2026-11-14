package com.campuspulse.attendance.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.campuspulse.attendance.data.AlertGatewayConfig
import com.campuspulse.attendance.ui.theme.CardBorder
import com.campuspulse.attendance.ui.theme.PresentGreen
import com.campuspulse.attendance.ui.theme.PrimaryBlue
import com.campuspulse.attendance.ui.theme.UnmarkedSlate

@Composable
fun SettingsScreen(
    config: AlertGatewayConfig,
    onSaveConfig: (AlertGatewayConfig) -> Unit,
    onSendTestAlert: (phone: String, email: String) -> Unit
) {
    val context = LocalContext.current
    var draft by remember(config) { mutableStateOf(config) }
    var testPhone by remember { mutableStateOf("+15552348901") }
    var testEmail by remember { mutableStateOf("parent.test@example.com") }

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSmsPermission = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Dispatch Mode vs Sandbox Simulation
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Parent Alert Dispatch Mode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (draft.liveDispatchEnabled) {
                                "LIVE DISPATCH ENABLED (Cellular SMS / Twilio / Resend)"
                            } else {
                                "SAFE SANDBOX MODE (Logs & Previews All Alerts Instantly)"
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (draft.liveDispatchEnabled) PresentGreen else PrimaryBlue
                        )
                        Text(
                            text = "Keep Sandbox Mode on while testing rosters, or switch to Live Dispatch to send real SMS/WhatsApp/Email messages.",
                            fontSize = 12.sp,
                            color = UnmarkedSlate
                        )
                    }
                    Switch(
                        checked = draft.liveDispatchEnabled,
                        onCheckedChange = { draft = draft.copy(liveDispatchEnabled = it) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = draft.schoolName,
                    onValueChange = { draft = draft.copy(schoolName = it) },
                    label = { Text("School / Campus Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // 2. Direct Android Cellular SmsManager Integration
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Direct Android Cellular SMS (Zero API Cost)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    "Uses this Android device's built-in SIM card (SmsManager) to text parents directly.",
                    fontSize = 12.sp,
                    color = UnmarkedSlate
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasSmsPermission) {
                            "✅ SEND_SMS Permission Granted"
                        } else {
                            "⚠️ SEND_SMS Permission Required for Live Cellular SMS"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasSmsPermission) PresentGreen else UnmarkedSlate
                    )
                    if (!hasSmsPermission) {
                        OutlinedButton(
                            onClick = { smsPermissionLauncher.launch(Manifest.permission.SEND_SMS) }
                        ) {
                            Text("Grant Permission", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. Twilio SMS & WhatsApp Cloud Gateway
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Twilio SMS & WhatsApp Business API",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                OutlinedTextField(
                    value = draft.twilioAccountSid,
                    onValueChange = { draft = draft.copy(twilioAccountSid = it) },
                    label = { Text("Twilio Account SID (AC...)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.twilioAuthToken,
                    onValueChange = { draft = draft.copy(twilioAuthToken = it) },
                    label = { Text("Twilio Auth Token") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.twilioFromSmsNumber,
                        onValueChange = { draft = draft.copy(twilioFromSmsNumber = it) },
                        label = { Text("From SMS Number") },
                        placeholder = { Text("+15550001111") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = draft.twilioFromWhatsAppNumber,
                        onValueChange = { draft = draft.copy(twilioFromWhatsAppNumber = it) },
                        label = { Text("From WhatsApp") },
                        placeholder = { Text("whatsapp:+14155238886") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }

        // 4. Resend Email API Gateway
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Resend Transactional Email API",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                OutlinedTextField(
                    value = draft.resendApiKey,
                    onValueChange = { draft = draft.copy(resendApiKey = it) },
                    label = { Text("Resend API Key (re_...)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.resendFromEmail,
                    onValueChange = { draft = draft.copy(resendFromEmail = it) },
                    label = { Text("Sender Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // 5. Customizable Message Templates
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Automated Alert Message Templates",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    "Variables: {school}, {student}, {guardian}, {class}, {time}, {pickup_method}, {pickup_person}",
                    fontSize = 11.sp,
                    color = UnmarkedSlate
                )
                OutlinedTextField(
                    value = draft.arrivalTemplate,
                    onValueChange = { draft = draft.copy(arrivalTemplate = it) },
                    label = { Text("Campus Arrival Template") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.lateTemplate,
                    onValueChange = { draft = draft.copy(lateTemplate = it) },
                    label = { Text("Late Arrival Template") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.dismissalTemplate,
                    onValueChange = { draft = draft.copy(dismissalTemplate = it) },
                    label = { Text("Campus Dismissal Template") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.absenceTemplate,
                    onValueChange = { draft = draft.copy(absenceTemplate = it) },
                    label = { Text("Absence Alert Template") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 6. Save & Test Dispatch Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onSaveConfig(draft) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Alert Settings", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    onSaveConfig(draft)
                    onSendTestAlert(testPhone, testEmail)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Send Test Alert", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
