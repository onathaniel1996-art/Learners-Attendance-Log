package com.campuspulse.attendance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campuspulse.attendance.data.model.AttendanceStatus
import com.campuspulse.attendance.data.model.PickupMethod
import com.campuspulse.attendance.ui.StudentRollCallItem
import com.campuspulse.attendance.ui.theme.CardBorder
import com.campuspulse.attendance.ui.theme.DismissedIndigo
import com.campuspulse.attendance.ui.theme.DismissedIndigoBg
import com.campuspulse.attendance.ui.theme.PresentGreen
import com.campuspulse.attendance.ui.theme.PresentGreenBg
import com.campuspulse.attendance.ui.theme.UnmarkedSlate
import com.campuspulse.attendance.ui.theme.UnmarkedSlateBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DismissalScreen(
    items: List<StudentRollCallItem>,
    selectedPickupFilter: PickupMethod?,
    onPickupFilterChange: (PickupMethod?) -> Unit,
    onReleaseStudent: (
        item: StudentRollCallItem,
        pickupMethod: PickupMethod,
        releasedToGuardian: String
    ) -> Unit,
    onBatchDismissGroup: (PickupMethod?) -> Unit
) {
    var studentForCustomRelease by remember { mutableStateOf<StudentRollCallItem?>(null) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    val awaitingDismissalCount = items.count { it.status != AttendanceStatus.DISMISSED && it.status != AttendanceStatus.ABSENT }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Pickup Mode Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedPickupFilter == null,
                onClick = { onPickupFilterChange(null) },
                label = { Text("All Pickup Modes (${items.size})") }
            )
            PickupMethod.entries.forEach { method ->
                FilterChip(
                    selected = selectedPickupFilter == method,
                    onClick = {
                        onPickupFilterChange(if (selectedPickupFilter == method) null else method)
                    },
                    label = { Text(method.label) }
                )
            }
        }

        // 2. Batch Dismissal Card for Carline / Bus Wave
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = DismissedIndigoBg),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedPickupFilter != null) {
                            "${selectedPickupFilter.label} Queue"
                        } else {
                            "Afternoon Campus Dismissal Queue"
                        },
                        fontWeight = FontWeight.Bold,
                        color = DismissedIndigo,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "$awaitingDismissalCount students awaiting release • Auto-alerts parents on release",
                        fontSize = 12.sp,
                        color = DismissedIndigo.copy(alpha = 0.8f)
                    )
                }

                if (awaitingDismissalCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onBatchDismissGroup(selectedPickupFilter) },
                        colors = ButtonDefaults.buttonColors(containerColor = DismissedIndigo),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Default.DirectionsBus,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Release All ($awaitingDismissalCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 3. Student Dismissal Cards with Authorized Pickup Verification
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.student.id }) { item ->
                val student = item.student
                val record = item.record
                val isDismissed = item.status == AttendanceStatus.DISMISSED
                val effectiveMethod = record?.pickupMethod ?: student.defaultPickupMethod

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(
                        1.dp,
                        if (isDismissed) DismissedIndigo.copy(alpha = 0.5f) else CardBorder
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(student.avatarColorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = student.initials,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = student.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = DismissedIndigoBg,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = effectiveMethod.label,
                                            color = DismissedIndigo,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (student.busRouteOrNotes.isNotBlank()) {
                                    Text(
                                        text = student.busRouteOrNotes,
                                        fontSize = 12.sp,
                                        color = UnmarkedSlate,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Authorized Adults Box
                        Surface(
                            color = UnmarkedSlateBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = PresentGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Authorized Pickup: ${student.authorizedPickupList}",
                                    fontSize = 11.sp,
                                    color = UnmarkedSlate
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isDismissed) {
                            val dismissedTime = record?.dismissalTimestampMillis?.let {
                                timeFormatter.format(Date(it))
                            } ?: "Just now"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PresentGreenBg)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = PresentGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Dismissed at $dismissedTime to ${record?.dismissedToGuardian ?: student.guardianName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PresentGreen
                                    )
                                }
                                TextButton(
                                    onClick = { studentForCustomRelease = item },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Update", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onReleaseStudent(
                                            item,
                                            effectiveMethod,
                                            "${student.guardianName} (${student.guardianRelationship})"
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = DismissedIndigo),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.HowToReg,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "1-Tap Release & Alert ${student.guardianName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { studentForCustomRelease = item },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Other Adult", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    studentForCustomRelease?.let { item ->
        val authorizedOptions = remember(item) {
            item.student.authorizedPickupList
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }
        var selectedAdult by remember(item) {
            mutableStateOf(authorizedOptions.firstOrNull() ?: item.student.guardianName)
        }
        var selectedMethod by remember(item) {
            mutableStateOf(item.record?.pickupMethod ?: item.student.defaultPickupMethod)
        }

        AlertDialog(
            onDismissRequest = { studentForCustomRelease = null },
            title = { Text("Confirm Campus Release • ${item.student.fullName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Select Verified Authorized Adult:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        authorizedOptions.forEach { adult ->
                            FilterChip(
                                selected = selectedAdult == adult,
                                onClick = { selectedAdult = adult },
                                label = { Text(adult, fontSize = 12.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = selectedAdult,
                        onValueChange = { selectedAdult = it },
                        label = { Text("Released To (Name & Relation)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Pickup Mode:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PickupMethod.entries.forEach { m ->
                            FilterChip(
                                selected = selectedMethod == m,
                                onClick = { selectedMethod = m },
                                label = { Text(m.label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReleaseStudent(item, selectedMethod, selectedAdult)
                        studentForCustomRelease = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DismissedIndigo)
                ) {
                    Text("Release & Send Parent Alert")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentForCustomRelease = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
