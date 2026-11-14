package com.campuspulse.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.campuspulse.attendance.ui.theme.PresentGreen
import com.campuspulse.attendance.ui.theme.PrimaryBlue

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

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = gatewayConfig.schoolName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            Text(
                                text = "${viewModel.readableActiveDate} • Instant Parent Alert System",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PrimaryBlue
                    )
                )

                // Instant Real-Time Parent Alert Confirmation Banner
                AnimatedVisibility(visible = bannerFeedback != null) {
                    bannerFeedback?.let { msg ->
                        Surface(
                            color = PresentGreen,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.clearBannerFeedback() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Dismiss",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedNavTab == 0,
                    onClick = { selectedNavTab = 0 },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                    label = { Text("Roll Call") }
                )
                NavigationBarItem(
                    selected = selectedNavTab == 1,
                    onClick = { selectedNavTab = 1 },
                    icon = { Icon(Icons.Default.DirectionsBus, contentDescription = null) },
                    label = { Text("Dismissal") }
                )
                NavigationBarItem(
                    selected = selectedNavTab == 2,
                    onClick = { selectedNavTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Alert Logs (${alertLogs.size})") }
                )
                NavigationBarItem(
                    selected = selectedNavTab == 3,
                    onClick = { selectedNavTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("SMS/API") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            when (selectedNavTab) {
                0 -> RollCallScreen(
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
