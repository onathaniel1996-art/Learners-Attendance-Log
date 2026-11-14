package com.campuspulse.attendance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campuspulse.attendance.data.model.AlertChannel
import com.campuspulse.attendance.data.model.AttendanceStatus
import com.campuspulse.attendance.data.model.ClassroomEntity
import com.campuspulse.attendance.data.model.PickupMethod
import com.campuspulse.attendance.ui.RollCallSummaryStats
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
fun RollCallScreen(
    classrooms: List<ClassroomEntity>,
    selectedClassroomId: Long,
    onSelectClassroom: (Long) -> Unit,
    stats: RollCallSummaryStats,
    students: List<StudentRollCallItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStatusFilter: AttendanceStatus?,
    onStatusFilterChange: (AttendanceStatus?) -> Unit,
    onMarkStatus: (StudentRollCallItem, AttendanceStatus) -> Unit,
    onMarkAllUnmarkedPresent: () -> Unit,
    onAddStudent: (
        firstName: String,
        lastName: String,
        badgeCode: String,
        guardianName: String,
        guardianRelation: String,
        guardianPhone: String,
        guardianEmail: String,
        channel: AlertChannel,
        pickupMethod: PickupMethod,
        notes: String
    ) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var showAddStudentDialog by remember { mutableStateOf(false) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddStudentDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Student")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Classroom Switcher Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                classrooms.forEach { classroom ->
                    val isSelected = classroom.id == selectedClassroomId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectClassroom(classroom.id) },
                        label = {
                            Text(
                                text = "${classroom.name} (${classroom.roomNumber})",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // 2. Live Summary Metrics + 1-Tap "Mark Remaining Present" Speed Action
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBadge("Present", stats.presentCount, PresentGreen, PresentGreenBg)
                        StatBadge("Late", stats.lateCount, LateAmber, LateAmberBg)
                        StatBadge("Absent", stats.absentCount, AbsentRed, AbsentRedBg)
                        StatBadge("Dismissed", stats.dismissedCount, DismissedIndigo, DismissedIndigoBg)
                        StatBadge("Unmarked", stats.unmarkedCount, UnmarkedSlate, UnmarkedSlateBg)
                    }

                    if (stats.unmarkedCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onMarkAllUnmarkedPresent()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PresentGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "1-Tap Mark Remaining ${stats.unmarkedCount} Present & Alert Parents",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 3. Search Bar & Quick Status Filter Chips
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Search student, badge ID, or parent...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedStatusFilter == null,
                    onClick = { onStatusFilterChange(null) },
                    label = { Text("All (${stats.totalStudents})") }
                )
                AttendanceStatus.entries.forEach { st ->
                    FilterChip(
                        selected = selectedStatusFilter == st,
                        onClick = {
                            onStatusFilterChange(if (selectedStatusFilter == st) null else st)
                        },
                        label = { Text(st.label) }
                    )
                }
            }

            // 4. Student Roster List with 1-Tap Status Controls
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(students, key = { it.student.id }) { item ->
                    StudentRollCallCard(
                        item = item,
                        timeFormatter = timeFormatter,
                        onStatusSelect = { newStatus ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMarkStatus(item, newStatus)
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    if (showAddStudentDialog) {
        AddStudentDialog(
            onDismiss = { showAddStudentDialog = false },
            onConfirm = { first, last, badge, gName, gRel, gPhone, gEmail, channel, pickup, notes ->
                onAddStudent(first, last, badge, gName, gRel, gPhone, gEmail, channel, pickup, notes)
                showAddStudentDialog = false
            }
        )
    }
}

@Composable
private fun StatBadge(
    label: String,
    count: Int,
    textColor: Color,
    bgColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = count.toString(),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = textColor
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun StudentRollCallCard(
    item: StudentRollCallItem,
    timeFormatter: SimpleDateFormat,
    onStatusSelect: (AttendanceStatus) -> Unit
) {
    val student = item.student
    val record = item.record
    val currentStatus = item.status

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = if (currentStatus == AttendanceStatus.UNMARKED) 1.dp else 1.5.dp,
            color = when (currentStatus) {
                AttendanceStatus.PRESENT -> PresentGreen.copy(alpha = 0.5f)
                AttendanceStatus.LATE -> LateAmber.copy(alpha = 0.5f)
                AttendanceStatus.ABSENT -> AbsentRed.copy(alpha = 0.5f)
                AttendanceStatus.DISMISSED -> DismissedIndigo.copy(alpha = 0.5f)
                AttendanceStatus.UNMARKED -> CardBorder
            }
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Initials Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(student.avatarColorHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = student.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = UnmarkedSlateBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = student.studentBadgeCode,
                                fontSize = 11.sp,
                                color = UnmarkedSlate,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${student.guardianName} (${student.guardianRelationship}) • ${student.preferredChannel.label}",
                        fontSize = 12.sp,
                        color = UnmarkedSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Timestamp & Parent Alert Indicator
                    if (record != null && currentStatus != AttendanceStatus.UNMARKED) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = PresentGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val timestampLabel = when (currentStatus) {
                                AttendanceStatus.PRESENT -> record.arrivalTimestampMillis?.let {
                                    "Arrived ${timeFormatter.format(Date(it))} • Parent Alerted"
                                } ?: "Marked Present • Parent Alerted"
                                AttendanceStatus.LATE -> record.arrivalTimestampMillis?.let {
                                    "Late Arrival ${timeFormatter.format(Date(it))} • Parent Alerted"
                                } ?: "Marked Late • Parent Alerted"
                                AttendanceStatus.ABSENT -> "Marked Absent • Parent Notified"
                                AttendanceStatus.DISMISSED -> record.dismissalTimestampMillis?.let {
                                    "Dismissed ${timeFormatter.format(Date(it))} (${record.pickupMethod?.label ?: ""}) • Parent Alerted"
                                } ?: "Dismissed • Parent Alerted"
                                AttendanceStatus.UNMARKED -> ""
                            }
                            Text(
                                text = timestampLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PresentGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1-Tap Action Buttons Row (Mark in < 1 second)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OneTapStatusButton(
                    modifier = Modifier.weight(1f),
                    label = "Present",
                    isSelected = currentStatus == AttendanceStatus.PRESENT,
                    activeColor = PresentGreen,
                    activeBg = PresentGreenBg,
                    onClick = { onStatusSelect(AttendanceStatus.PRESENT) }
                )
                OneTapStatusButton(
                    modifier = Modifier.weight(1f),
                    label = "Late",
                    isSelected = currentStatus == AttendanceStatus.LATE,
                    activeColor = LateAmber,
                    activeBg = LateAmberBg,
                    onClick = { onStatusSelect(AttendanceStatus.LATE) }
                )
                OneTapStatusButton(
                    modifier = Modifier.weight(1f),
                    label = "Absent",
                    isSelected = currentStatus == AttendanceStatus.ABSENT,
                    activeColor = AbsentRed,
                    activeBg = AbsentRedBg,
                    onClick = { onStatusSelect(AttendanceStatus.ABSENT) }
                )
                OneTapStatusButton(
                    modifier = Modifier.weight(1f),
                    label = "Dismiss",
                    isSelected = currentStatus == AttendanceStatus.DISMISSED,
                    activeColor = DismissedIndigo,
                    activeBg = DismissedIndigoBg,
                    onClick = { onStatusSelect(AttendanceStatus.DISMISSED) }
                )
            }
        }
    }
}

@Composable
private fun OneTapStatusButton(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    activeBg: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) activeColor else activeBg.copy(alpha = 0.55f),
        border = BorderStroke(
            1.dp,
            if (isSelected) activeColor else activeColor.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = label,
                color = if (isSelected) Color.White else activeColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AddStudentDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        firstName: String,
        lastName: String,
        badgeCode: String,
        guardianName: String,
        guardianRelation: String,
        guardianPhone: String,
        guardianEmail: String,
        channel: AlertChannel,
        pickupMethod: PickupMethod,
        notes: String
    ) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var badgeCode by remember { mutableStateOf("") }
    var guardianName by remember { mutableStateOf("") }
    var guardianRelation by remember { mutableStateOf("Mother") }
    var guardianPhone by remember { mutableStateOf("+1555") }
    var guardianEmail by remember { mutableStateOf("") }
    var selectedChannel by remember { mutableStateOf(AlertChannel.ANDROID_SMS) }
    var selectedPickup by remember { mutableStateOf(PickupMethod.PARENT_CARLINE) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Student & Parent Alert Contact", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = badgeCode,
                        onValueChange = { badgeCode = it },
                        label = { Text("Badge ID") },
                        placeholder = { Text("STU-309") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = guardianRelation,
                        onValueChange = { guardianRelation = it },
                        label = { Text("Relation") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = guardianName,
                    onValueChange = { guardianName = it },
                    label = { Text("Parent / Guardian Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = guardianPhone,
                    onValueChange = { guardianPhone = it },
                    label = { Text("Parent Mobile Phone (SMS / WhatsApp)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = guardianEmail,
                    onValueChange = { guardianEmail = it },
                    label = { Text("Parent Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Preferred Alert Channel:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AlertChannel.entries.forEach { ch ->
                        FilterChip(
                            selected = selectedChannel == ch,
                            onClick = { selectedChannel = ch },
                            label = { Text(ch.label, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Carline Tag / Bus Route Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (firstName.isNotBlank() && lastName.isNotBlank() && guardianName.isNotBlank()) {
                        onConfirm(
                            firstName,
                            lastName,
                            badgeCode,
                            guardianName,
                            guardianRelation,
                            guardianPhone,
                            guardianEmail,
                            selectedChannel,
                            selectedPickup,
                            notes
                        )
                    }
                }
            ) {
                Text("Save Student")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
