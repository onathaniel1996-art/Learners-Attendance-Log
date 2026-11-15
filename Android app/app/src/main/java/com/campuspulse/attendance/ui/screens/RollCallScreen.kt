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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campuspulse.attendance.data.model.AlertChannel
import com.campuspulse.attendance.data.model.AttendanceStatus
import com.campuspulse.attendance.data.model.ClassroomEntity
import com.campuspulse.attendance.data.model.ParentAlertLogEntity
import com.campuspulse.attendance.data.model.PickupMethod
import com.campuspulse.attendance.ui.RollCallSummaryStats
import com.campuspulse.attendance.ui.StudentRollCallItem
import com.campuspulse.attendance.ui.theme.CardBorder
import com.campuspulse.attendance.ui.theme.StitchError
import com.campuspulse.attendance.ui.theme.StitchErrorContainer
import com.campuspulse.attendance.ui.theme.StitchOnErrorContainer
import com.campuspulse.attendance.ui.theme.StitchOnPrimaryFixed
import com.campuspulse.attendance.ui.theme.StitchOnSecondaryFixed
import com.campuspulse.attendance.ui.theme.StitchOnSurface
import com.campuspulse.attendance.ui.theme.StitchOnSurfaceVariant
import com.campuspulse.attendance.ui.theme.StitchOnTertiaryFixed
import com.campuspulse.attendance.ui.theme.StitchOutline
import com.campuspulse.attendance.ui.theme.StitchPrimary
import com.campuspulse.attendance.ui.theme.StitchPrimaryFixed
import com.campuspulse.attendance.ui.theme.StitchSecondary
import com.campuspulse.attendance.ui.theme.StitchSecondaryFixed
import com.campuspulse.attendance.ui.theme.StitchSurface
import com.campuspulse.attendance.ui.theme.StitchSurfaceContainer
import com.campuspulse.attendance.ui.theme.StitchSurfaceHigh
import com.campuspulse.attendance.ui.theme.StitchSurfaceLow
import com.campuspulse.attendance.ui.theme.StitchSurfaceLowest
import com.campuspulse.attendance.ui.theme.StitchTertiary
import com.campuspulse.attendance.ui.theme.StitchTertiaryFixed
import com.campuspulse.attendance.ui.theme.StitchTertiarySoftBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RollCallScreen(
    schoolName: String,
    readableDate: String,
    latestAlertLog: ParentAlertLogEntity?,
    bannerFeedback: String?,
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
                containerColor = StitchPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Student",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = StitchSurface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top School Context & Live Sync Pill + Live Notification Toast Banner
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = schoolName.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = StitchOnSurfaceVariant
                            )
                            Text(
                                text = "$readableDate • Fall Term",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StitchOnSurface
                            )
                        }

                        Surface(
                            color = StitchSurfaceHigh,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StitchSecondary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE SYNC",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = StitchSecondary
                                )
                            }
                        }
                    }

                    // Live Notification Toast Banner (from Google Stitch design)
                    val headlineTitle = when {
                        bannerFeedback != null -> bannerFeedback
                        latestAlertLog != null -> "${latestAlertLog.eventType.label}: ${latestAlertLog.studentName}"
                        else -> "Present: Leo Martinez"
                    }
                    val subtitleLine = when {
                        latestAlertLog != null -> "${latestAlertLog.channel.label} sent to ${latestAlertLog.guardianName} at ${timeFormatter.format(Date(latestAlertLog.timestampMillis))}"
                        else -> "SMS & WhatsApp parent alert dispatcher ready • Tap any student below"
                    }

                    Surface(
                        color = StitchTertiarySoftBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(StitchTertiary)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = buildAnnotatedString {
                                            if (headlineTitle.contains(":")) {
                                                val parts = headlineTitle.split(":", limit = 2)
                                                withStyle(
                                                    SpanStyle(
                                                        color = StitchTertiary,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                ) {
                                                    append("${parts[0]}:")
                                                }
                                                withStyle(
                                                    SpanStyle(
                                                        color = StitchOnSurface,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                ) {
                                                    append(parts[1])
                                                }
                                            } else {
                                                withStyle(
                                                    SpanStyle(
                                                        color = StitchTertiary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                ) {
                                                    append(headlineTitle)
                                                }
                                            }
                                        },
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = subtitleLine,
                                        fontSize = 12.sp,
                                        color = StitchOnSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Just now",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchOnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Classroom Switcher (Horizontal Scrollable Pills matching Stitch)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    classrooms.forEach { classroom ->
                        val isSelected = classroom.id == selectedClassroomId
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onSelectClassroom(classroom.id) },
                            color = if (isSelected) StitchPrimary else StitchSurfaceLowest,
                            border = if (isSelected) null else BorderStroke(1.dp, CardBorder),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.MeetingRoom,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = "${classroom.name.substringBefore("•").trim()} • ${classroom.roomNumber}",
                                    color = if (isSelected) Color.White else StitchOnSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. Compact Attendance Summary Card (4 Stat Boxes + Deep Emerald 1-Tap Action Button)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StitchSurfaceLowest),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CLASSROOM ROSTER SUMMARY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = StitchOnSurfaceVariant
                            )
                            Text(
                                text = "${stats.totalStudents} Enrolled",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchSecondary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StitchStatBox(
                                modifier = Modifier.weight(1f),
                                count = stats.presentCount,
                                label = "PRESENT",
                                countColor = StitchTertiary,
                                labelColor = StitchOnTertiaryFixed,
                                bgColor = StitchTertiarySoftBg
                            )
                            StitchStatBox(
                                modifier = Modifier.weight(1f),
                                count = stats.lateCount,
                                label = "LATE",
                                countColor = StitchSecondary,
                                labelColor = StitchOnSurfaceVariant,
                                bgColor = StitchSurfaceHigh
                            )
                            StitchStatBox(
                                modifier = Modifier.weight(1f),
                                count = stats.absentCount,
                                label = "ABSENT",
                                countColor = StitchError,
                                labelColor = StitchOnErrorContainer,
                                bgColor = StitchErrorContainer
                            )
                            StitchStatBox(
                                modifier = Modifier.weight(1f),
                                count = stats.unmarkedCount,
                                label = "UNMARKED",
                                countColor = StitchOnSurface,
                                labelColor = StitchOnSurfaceVariant,
                                bgColor = StitchSurfaceContainer
                            )
                        }

                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onMarkAllUnmarkedPresent()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StitchTertiary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 13.dp)
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (stats.unmarkedCount > 0) {
                                    "1-Tap Mark Remaining ${stats.unmarkedCount} Present & Alert"
                                } else {
                                    "All ${stats.totalStudents} Students Marked • Tap Any Student to Update"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 4. Search Bar & Uppercase Pill Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StitchSurfaceLowest, RoundedCornerShape(10.dp)),
                        placeholder = {
                            Text(
                                "Search student name or badge ID...",
                                color = StitchOnSurfaceVariant,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = StitchOutline
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            } else {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan Badge",
                                    tint = StitchOutline
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StitchFilterPill(
                            label = "ALL (${stats.totalStudents})",
                            isSelected = selectedStatusFilter == null,
                            onClick = { onStatusFilterChange(null) }
                        )
                        StitchFilterPill(
                            label = "UNMARKED (${stats.unmarkedCount})",
                            isSelected = selectedStatusFilter == AttendanceStatus.UNMARKED,
                            onClick = {
                                onStatusFilterChange(
                                    if (selectedStatusFilter == AttendanceStatus.UNMARKED) null
                                    else AttendanceStatus.UNMARKED
                                )
                            }
                        )
                        StitchFilterPill(
                            label = "PRESENT (${stats.presentCount})",
                            isSelected = selectedStatusFilter == AttendanceStatus.PRESENT,
                            onClick = {
                                onStatusFilterChange(
                                    if (selectedStatusFilter == AttendanceStatus.PRESENT) null
                                    else AttendanceStatus.PRESENT
                                )
                            }
                        )
                        StitchFilterPill(
                            label = "LATE (${stats.lateCount})",
                            isSelected = selectedStatusFilter == AttendanceStatus.LATE,
                            onClick = {
                                onStatusFilterChange(
                                    if (selectedStatusFilter == AttendanceStatus.LATE) null
                                    else AttendanceStatus.LATE
                                )
                            }
                        )
                        StitchFilterPill(
                            label = "ABSENT (${stats.absentCount})",
                            isSelected = selectedStatusFilter == AttendanceStatus.ABSENT,
                            onClick = {
                                onStatusFilterChange(
                                    if (selectedStatusFilter == AttendanceStatus.ABSENT) null
                                    else AttendanceStatus.ABSENT
                                )
                            }
                        )
                        if (stats.dismissedCount > 0) {
                            StitchFilterPill(
                                label = "DISMISSED (${stats.dismissedCount})",
                                isSelected = selectedStatusFilter == AttendanceStatus.DISMISSED,
                                onClick = {
                                    onStatusFilterChange(
                                        if (selectedStatusFilter == AttendanceStatus.DISMISSED) null
                                        else AttendanceStatus.DISMISSED
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 5. Student Roster Cards (Matching Google Stitch Layout)
            items(students, key = { it.student.id }) { item ->
                StitchStudentRosterCard(
                    item = item,
                    timeFormatter = timeFormatter,
                    onStatusSelect = { newStatus ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onMarkStatus(item, newStatus)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(76.dp))
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
private fun StitchStatBox(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    countColor: Color,
    labelColor: Color,
    bgColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Text(
            text = count.toString(),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = countColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = labelColor
        )
    }
}

@Composable
private fun StitchFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        color = if (isSelected) StitchPrimary else StitchSurfaceLowest,
        border = if (isSelected) null else BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else StitchOnSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun StitchStudentRosterCard(
    item: StudentRollCallItem,
    timeFormatter: SimpleDateFormat,
    onStatusSelect: (AttendanceStatus) -> Unit
) {
    val student = item.student
    val record = item.record
    val currentStatus = item.status

    val (avatarBg, avatarFg) = when (currentStatus) {
        AttendanceStatus.PRESENT -> StitchTertiaryFixed to StitchOnTertiaryFixed
        AttendanceStatus.UNMARKED -> StitchSurfaceHigh to StitchSecondary
        AttendanceStatus.LATE -> StitchSecondaryFixed to StitchOnSecondaryFixed
        AttendanceStatus.ABSENT -> StitchErrorContainer to StitchOnErrorContainer
        AttendanceStatus.DISMISSED -> StitchPrimaryFixed to StitchOnPrimaryFixed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StitchSurfaceLowest),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Rounded-Square Avatar + Student Name + Badge ID + Guardian Info + More Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.initials,
                            color = avatarFg,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = student.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = StitchOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = StitchSurfaceContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = student.studentBadgeCode,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchOnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        val shortChannel = when (student.preferredChannel) {
                            AlertChannel.ANDROID_SMS, AlertChannel.TWILIO_SMS -> "SMS"
                            AlertChannel.WHATSAPP -> "WhatsApp"
                            AlertChannel.RESEND_EMAIL -> "Email"
                        }
                        Text(
                            text = "${student.guardianName} • ${student.guardianRelationship} • $shortChannel (${student.guardianPhone})",
                            fontSize = 13.sp,
                            color = StitchOnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Student Options",
                    tint = StitchOutline,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                            // Reset to unmarked if tapped
                            onStatusSelect(AttendanceStatus.UNMARKED)
                        }
                )
            }

            // Middle Row: Live Status Tag Pill (Matching Stitch)
            val (tagBg, tagFg, tagIcon, tagText) = when (currentStatus) {
                AttendanceStatus.PRESENT -> {
                    val timeStr = record?.arrivalTimestampMillis?.let {
                        timeFormatter.format(Date(it))
                    } ?: "8:04 AM"
                    Quadruple(
                        StitchTertiarySoftBg,
                        StitchTertiary,
                        Icons.Default.CheckCircle,
                        "Arrived $timeStr • Parent Alerted"
                    )
                }
                AttendanceStatus.UNMARKED -> {
                    Quadruple(
                        StitchSurfaceContainer,
                        StitchOnSurfaceVariant,
                        Icons.Default.Schedule,
                        "Not Marked Yet • Awaiting Arrival"
                    )
                }
                AttendanceStatus.LATE -> {
                    val timeStr = record?.arrivalTimestampMillis?.let {
                        timeFormatter.format(Date(it))
                    } ?: "8:22 AM"
                    Quadruple(
                        StitchSecondaryFixed,
                        StitchSecondary,
                        Icons.Default.Warning,
                        "Marked Late at $timeStr • Parent Notified via SMS"
                    )
                }
                AttendanceStatus.ABSENT -> {
                    Quadruple(
                        StitchErrorContainer,
                        StitchError,
                        Icons.Default.Cancel,
                        "Absence Logged • Parent Notified"
                    )
                }
                AttendanceStatus.DISMISSED -> {
                    val timeStr = record?.dismissalTimestampMillis?.let {
                        timeFormatter.format(Date(it))
                    } ?: "3:15 PM"
                    Quadruple(
                        StitchPrimaryFixed,
                        StitchPrimary,
                        Icons.Default.DirectionsBus,
                        "Dismissed at $timeStr (${(record?.pickupMethod ?: student.defaultPickupMethod).label}) • Parent Alerted"
                    )
                }
            }

            Surface(
                color = tagBg,
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tagIcon,
                        contentDescription = null,
                        tint = tagFg,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tagText,
                        color = tagFg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Bottom Row: Enclosed 4-Way Segmented Attendance Container (Matching Stitch)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StitchSurfaceLow)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StitchSegmentButton(
                    modifier = Modifier.weight(1f),
                    label = "PRESENT",
                    isSelected = currentStatus == AttendanceStatus.PRESENT,
                    selectedBg = StitchTertiary,
                    onClick = { onStatusSelect(AttendanceStatus.PRESENT) }
                )
                StitchSegmentButton(
                    modifier = Modifier.weight(1f),
                    label = "LATE",
                    isSelected = currentStatus == AttendanceStatus.LATE,
                    selectedBg = StitchSecondary,
                    onClick = { onStatusSelect(AttendanceStatus.LATE) }
                )
                StitchSegmentButton(
                    modifier = Modifier.weight(1f),
                    label = "ABSENT",
                    isSelected = currentStatus == AttendanceStatus.ABSENT,
                    selectedBg = StitchError,
                    onClick = { onStatusSelect(AttendanceStatus.ABSENT) }
                )
                StitchSegmentButton(
                    modifier = Modifier.weight(1f),
                    label = "DISMISS",
                    isSelected = currentStatus == AttendanceStatus.DISMISSED,
                    selectedBg = StitchPrimary,
                    onClick = { onStatusSelect(AttendanceStatus.DISMISSED) }
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
private fun StitchSegmentButton(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    selectedBg: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) selectedBg else Color.Transparent,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = label,
                color = if (isSelected) Color.White else StitchOnSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
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
                },
                colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary)
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
