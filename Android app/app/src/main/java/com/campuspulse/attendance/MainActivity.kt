package com.campuspulse.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campuspulse.attendance.data.model.AttendanceStatus
import com.campuspulse.attendance.ui.AttendanceViewModel
import com.campuspulse.attendance.ui.screens.DismissalScreen
import com.campuspulse.attendance.ui.screens.LogsScreen
import com.campuspulse.attendance.ui.screens.RollCallScreen
import com.campuspulse.attendance.ui.screens.SettingsScreen
import com.campuspulse.attendance.ui.theme.CampusPulseTheme
import com.campuspulse.attendance.ui.theme.CardBorder
import com.campuspulse.attendance.ui.theme.StitchError
import com.campuspulse.attendance.ui.theme.StitchOnSurface
import com.campuspulse.attendance.ui.theme.StitchOnSurfaceVariant
import com.campuspulse.attendance.ui.theme.StitchSecondary
import com.campuspulse.attendance.ui.theme.StitchSecondaryFixed
import com.campuspulse.attendance.ui.theme.StitchSurface
import com.campuspulse.attendance.ui.theme.StitchSurfaceLowest

class MainActivity : ComponentActivity() {

    private val viewModel: AttendanceViewModel by viewModels {
        val app = application as CampusPulseApp
        AttendanceViewModel.Factory(
            dao = app.database.attendanceDao(),
            settingsRepository = app.settingsRepository,
            alertDispatcher = app.alertDispatcher
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CampusPulseTheme {
                CampusPulseMainShell(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CampusPulseMainShell(viewModel: AttendanceViewModel) {
    var selectedNavTab by rememberSaveable { mutableIntStateOf(0) }

    val classrooms by viewModel.classrooms.collectAsState()
    val selectedClassroomId by viewModel.selectedClassroomId.collectAsState()
    val rollCallItems by viewModel.rollCallItems.collectAsState()
    val dismissalItems by viewModel.dismissalItems.collectAsState()
    val stats by viewModel.summaryStats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val pickupFilter by viewModel.pickupFilter.collectAsState()
    val alertLogs by viewModel.parentAlertLogs.collectAsState()
    val gatewayConfig by viewModel.gatewayConfig.collectAsState()
    val bannerFeedback by viewModel.bannerFeedback.collectAsState()

    val tabSubtitle = when (selectedNavTab) {
        0 -> "ROLL CALL"
        1 -> "DISMISSAL"
        2 -> "ALERT LOGS"
        else -> "SETTINGS"
    }

    Scaffold(
        topBar = {
            Surface(color = StitchSurfaceLowest) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: CampusPulse Brand Logo + Title + Active Screen Subtitle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.campuspulse_logo),
                                contentDescription = "CampusPulse Logo",
                                modifier = Modifier
                                    .height(36.dp)
                                    .width(48.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CampusPulse",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = StitchOnSurface
                                )
                                Text(
                                    text = tabSubtitle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = StitchOnSurfaceVariant
                                )
                            }
                        }

                        // Right: Notification Bell with Red Indicator + Teacher Avatar
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedNavTab = 2 }) {
                                Box {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsNone,
                                        contentDescription = "Parent Alert Logs",
                                        tint = StitchOnSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .clip(CircleShape)
                                            .background(StitchError)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(StitchSecondaryFixed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SJ",
                                    color = StitchSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 1.dp, color = CardBorder)
                }
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(thickness = 1.dp, color = CardBorder)
                NavigationBar(
                    containerColor = StitchSurfaceLowest,
                    tonalElevation = 0.dp
                ) {
                    val itemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StitchSecondary,
                        selectedTextColor = StitchSecondary,
                        indicatorColor = StitchSecondaryFixed,
                        unselectedIconColor = StitchOnSurfaceVariant,
                        unselectedTextColor = StitchOnSurfaceVariant
                    )
                    NavigationBarItem(
                        selected = selectedNavTab == 0,
                        onClick = { selectedNavTab = 0 },
                        icon = { Icon(Icons.Default.FactCheck, contentDescription = null) },
                        label = { Text("Roll Call", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = selectedNavTab == 1,
                        onClick = { selectedNavTab = 1 },
                        icon = { Icon(Icons.Default.DirectionsBus, contentDescription = null) },
                        label = { Text("Dismissal", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = selectedNavTab == 2,
                        onClick = { selectedNavTab = 2 },
                        icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null) },
                        label = { Text("Alert Logs", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = selectedNavTab == 3,
                        onClick = { selectedNavTab = 3 },
                        icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                        label = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = itemColors
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StitchSurface)
        ) {
            when (selectedNavTab) {
                0 -> RollCallScreen(
                    schoolName = gatewayConfig.schoolName,
                    readableDate = viewModel.readableActiveDate,
                    latestAlertLog = alertLogs.firstOrNull(),
                    bannerFeedback = bannerFeedback,
                    classrooms = classrooms,
                    selectedClassroomId = selectedClassroomId,
                    onSelectClassroom = viewModel::selectClassroom,
                    stats = stats,
                    students = rollCallItems,
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    selectedStatusFilter = statusFilter,
                    onStatusFilterChange = viewModel::updateStatusFilter,
                    onMarkStatus = { item, newStatus ->
                        viewModel.markStudentStatus(
                            student = item.student,
                            newStatus = newStatus
                        )
                    },
                    onMarkAllUnmarkedPresent = viewModel::markAllUnmarkedAsPresent,
                    onAddStudent = viewModel::addNewStudent
                )

                1 -> DismissalScreen(
                    items = dismissalItems,
                    selectedPickupFilter = pickupFilter,
                    onPickupFilterChange = viewModel::updatePickupFilter,
                    onReleaseStudent = { item, method, releasedTo ->
                        viewModel.markStudentStatus(
                            student = item.student,
                            newStatus = AttendanceStatus.DISMISSED,
                            pickupMethod = method,
                            dismissedToGuardian = releasedTo
                        )
                    },
                    onBatchDismissGroup = viewModel::dismissBatchByPickupMethod
                )

                2 -> LogsScreen(
                    alertLogs = alertLogs,
                    rollCallItems = rollCallItems,
                    onRetryAlert = viewModel::retryParentAlert,
                    onRetryAllFailed = viewModel::retryAllPendingOrFailedAlerts,
                    onBuildCsvReport = viewModel::buildCsvExportReport
                )

                3 -> SettingsScreen(
                    config = gatewayConfig,
                    onSaveConfig = viewModel::updateGatewayConfig,
                    onSendTestAlert = viewModel::sendTestParentAlert
                )
            }
        }
    }
}
