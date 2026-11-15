package com.campuspulse.attendance.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campuspulse.attendance.data.model.AlertEventType
import com.campuspulse.attendance.data.model.AttendanceStatus
import com.campuspulse.attendance.data.model.DeliveryStatus
import com.campuspulse.attendance.data.model.ParentAlertLogEntity
import com.campuspulse.attendance.ui.StudentRollCallItem
import com.campuspulse.attendance.ui.theme.AbsentRed
import com.campuspulse.attendance.ui.theme.AbsentRedBg
import com.campuspulse.attendance.ui.theme.CardBorder
import com.campuspulse.attendance.ui.theme.DismissedIndigo
import com.campuspulse.attendance.ui.theme.DismissedIndigoBg
import com.campuspulse.attendance.ui.theme.LateAmber
import com.campuspulse.attendance.ui.theme.LateAmberBg
import com.campuspulse.attendance.ui.theme.PresentGreen
import com.campuspulse.attendance.ui.theme.PresentGreenBg
import com.campuspulse.attendance.ui.theme.PrimaryBlue
import com.campuspulse.attendance.ui.theme.UnmarkedSlate
import com.campuspulse.attendance.ui.theme.UnmarkedSlateBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    alertLogs: List<ParentAlertLogEntity>,
    rollCallItems: List<StudentRollCallItem>,
    onRetryAlert: (ParentAlertLogEntity) -> Unit,
    onRetryAllFailed: () -> Unit,
    onBuildCsvReport: () -> String
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val dateTimeFormatter = remember { SimpleDateFormat("MMM d • h:mm:ss a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = PrimaryBlue
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "Parent Alerts (${alertLogs.size})",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "Attendance & Dismissal Log",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
        }

        if (selectedTab == 0) {
            // Parent Alert Dispatch Log
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Real-time SMS, WhatsApp & Email Dispatch History",
                    fontSize = 12.sp,
                    color = UnmarkedSlate,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(
                    onClick = onRetryAllFailed,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry Queued", fontSize = 12.sp)
                }
            }

            if (alertLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Sms,
                            contentDescription = null,
                            tint = UnmarkedSlate,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No parent alerts triggered yet today.",
                            fontWeight = FontWeight.SemiBold,
                            color = UnmarkedSlate
                        )
                        Text(
                            "Mark a student Present, Late, Absent, or Dismissed to auto-alert their guardian.",
                            fontSize = 12.sp,
                            color = UnmarkedSlate
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(alertLogs, key = { it.id }) { log ->
                        ParentAlertLogCard(
                            log = log,
                            dateTimeFormatter = dateTimeFormatter,
                            onRetry = { onRetryAlert(log) }
                        )
                    }
                }
            }
        } else {
            // Daily Attendance & Dismissal Audit Log + CSV Export
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Official Daily Check-In & Dismissal Timestamps",
                    fontSize = 12.sp,
                    color = UnmarkedSlate,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick = {
                        val csv = onBuildCsvReport()
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "CampusPulse Attendance & Dismissal Log")
                            putExtra(Intent.EXTRA_TEXT, csv)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export Attendance CSV"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rollCallItems, key = { it.student.id }) { item ->
                    val rec = item.record
                    val arrivalStr = rec?.arrivalTimestampMillis?.let {
                        dateTimeFormatter.format(Date(it))
                    } ?: "—"
                    val dismissalStr = rec?.dismissalTimestampMillis?.let {
                        dateTimeFormatter.format(Date(it))
                    } ?: "—"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.student.fullName} (${item.student.studentBadgeCode})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                StatusPill(status = item.status)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Arrival Check-In: $arrivalStr",
                                fontSize = 12.sp,
                                color = UnmarkedSlate
                            )
                            Text(
                                text = "Campus Dismissal: $dismissalStr • Pickup: ${(rec?.pickupMethod ?: item.student.defaultPickupMethod).label}",
                                fontSize = 12.sp,
                                color = UnmarkedSlate
                            )
                            if (!rec?.dismissedToGuardian.isNullOrBlank()) {
                                Text(
                                    text = "Released To: ${rec?.dismissedToGuardian}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DismissedIndigo
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentAlertLogCard(
    log: ParentAlertLogEntity,
    dateTimeFormatter: SimpleDateFormat,
    onRetry: () -> Unit
) {
    val (eventColor, eventBg) = when (log.eventType) {
        AlertEventType.ARRIVAL -> PresentGreen to PresentGreenBg
        AlertEventType.LATE_ARRIVAL -> LateAmber to LateAmberBg
        AlertEventType.ABSENCE_NOTICE -> AbsentRed to AbsentRedBg
        AlertEventType.DISMISSAL -> DismissedIndigo to DismissedIndigoBg
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = eventBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = "${log.eventType.label} • ${log.channel.label}",
                        color = eventColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = dateTimeFormatter.format(Date(log.timestampMillis)),
                    fontSize = 11.sp,
                    color = UnmarkedSlate
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To: ${log.guardianName} (${log.recipientContact})",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = UnmarkedSlateBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = log.messageBody,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = when (log.deliveryStatus) {
                    DeliveryStatus.SENT -> PresentGreen
                    DeliveryStatus.SIMULATED -> PrimaryBlue
                    DeliveryStatus.QUEUED_OFFLINE -> LateAmber
                    DeliveryStatus.FAILED -> AbsentRed
                }
                Text(
                    text = "${log.deliveryStatus.label}: ${log.statusDetail}",
                    fontSize = 11.sp,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Resend", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: AttendanceStatus) {
    val (fg, bg) = when (status) {
        AttendanceStatus.PRESENT -> PresentGreen to PresentGreenBg
        AttendanceStatus.LATE -> LateAmber to LateAmberBg
        AttendanceStatus.ABSENT -> AbsentRed to AbsentRedBg
        AttendanceStatus.DISMISSED -> DismissedIndigo to DismissedIndigoBg
        AttendanceStatus.UNMARKED -> UnmarkedSlate to UnmarkedSlateBg
    }
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = status.label,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
