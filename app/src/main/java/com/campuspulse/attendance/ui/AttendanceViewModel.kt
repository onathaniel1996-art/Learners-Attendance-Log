package com.campuspulse.attendance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.campuspulse.attendance.data.AlertGatewayConfig
import com.campuspulse.attendance.data.AlertSettingsRepository
import com.campuspulse.attendance.data.AttendanceDao
import com.campuspulse.attendance.data.model.AlertChannel
import com.campuspulse.attendance.data.model.AlertEventType
import com.campuspulse.attendance.data.model.AttendanceRecordEntity
import com.campuspulse.attendance.data.model.AttendanceStatus
import com.campuspulse.attendance.data.model.ClassroomEntity
import com.campuspulse.attendance.data.model.DeliveryStatus
import com.campuspulse.attendance.data.model.ParentAlertLogEntity
import com.campuspulse.attendance.data.model.PickupMethod
import com.campuspulse.attendance.data.model.StudentEntity
import com.campuspulse.attendance.data.model.StudentWithAttendance
import com.campuspulse.attendance.service.ParentAlertDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RollCallSummaryStats(
    val totalStudents: Int = 0,
    val presentCount: Int = 0,
    val lateCount: Int = 0,
    val absentCount: Int = 0,
    val dismissedCount: Int = 0,
    val unmarkedCount: Int = 0
)

data class StudentRollCallItem(
    val student: StudentEntity,
    val record: AttendanceRecordEntity?
) {
    val status: AttendanceStatus
        get() = record?.status ?: AttendanceStatus.UNMARKED
}

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModel(
    private val dao: AttendanceDao,
    private val settingsRepository: AlertSettingsRepository,
    private val alertDispatcher: ParentAlertDispatcher
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val readableDateFormatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("h:mm:ss a", Locale.getDefault())

    private val _activeDateString = MutableStateFlow(dateFormatter.format(Date()))
    val activeDateString: StateFlow<String> = _activeDateString.asStateFlow()

    val readableActiveDate: String
        get() = readableDateFormatter.format(Date())

    val classrooms: StateFlow<List<ClassroomEntity>> = dao.observeClassrooms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedClassroomId = MutableStateFlow(1L)
    val selectedClassroomId: StateFlow<Long> = _selectedClassroomId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow<AttendanceStatus?>(null)
    val statusFilter: StateFlow<AttendanceStatus?> = _statusFilter.asStateFlow()

    private val _pickupFilter = MutableStateFlow<PickupMethod?>(null)
    val pickupFilter: StateFlow<PickupMethod?> = _pickupFilter.asStateFlow()

    private val _bannerFeedback = MutableStateFlow<String?>(null)
    val bannerFeedback: StateFlow<String?> = _bannerFeedback.asStateFlow()

    val gatewayConfig: StateFlow<AlertGatewayConfig> = settingsRepository.config

    val parentAlertLogs: StateFlow<List<ParentAlertLogEntity>> = dao.observeParentAlertLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val rawClassroomStudents: StateFlow<List<StudentWithAttendance>> =
        _selectedClassroomId.flatMapLatest { classId ->
            dao.observeStudentsWithAttendance(classId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rollCallItems: StateFlow<List<StudentRollCallItem>> = combine(
        rawClassroomStudents,
        _activeDateString,
        _searchQuery,
        _statusFilter
    ) { list, dateStr, query, statusFilter ->
        val mapped = list.map { swa ->
            StudentRollCallItem(
                student = swa.student,
                record = swa.recordForDate(dateStr)
            )
        }
        mapped.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.student.fullName.contains(query, ignoreCase = true) ||
                item.student.studentBadgeCode.contains(query, ignoreCase = true) ||
                item.student.guardianName.contains(query, ignoreCase = true) ||
                item.student.busRouteOrNotes.contains(query, ignoreCase = true)

            val matchesStatus = statusFilter == null || item.status == statusFilter
            matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dismissalItems: StateFlow<List<StudentRollCallItem>> = combine(
        rawClassroomStudents,
        _activeDateString,
        _searchQuery,
        _pickupFilter
    ) { list, dateStr, query, methodFilter ->
        val mapped = list.map { swa ->
            StudentRollCallItem(
                student = swa.student,
                record = swa.recordForDate(dateStr)
            )
        }
        mapped.filter { item ->
            val effectiveMethod = item.record?.pickupMethod ?: item.student.defaultPickupMethod
            val matchesMethod = methodFilter == null || effectiveMethod == methodFilter
            val matchesQuery = query.isBlank() ||
                item.student.fullName.contains(query, ignoreCase = true) ||
                item.student.busRouteOrNotes.contains(query, ignoreCase = true) ||
                item.student.guardianName.contains(query, ignoreCase = true)
            matchesMethod && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summaryStats: StateFlow<RollCallSummaryStats> = combine(
        rawClassroomStudents,
        _activeDateString
    ) { list, dateStr ->
        var present = 0
        var late = 0
        var absent = 0
        var dismissed = 0
        var unmarked = 0

        list.forEach { swa ->
            when (swa.recordForDate(dateStr)?.status ?: AttendanceStatus.UNMARKED) {
                AttendanceStatus.PRESENT -> present++
                AttendanceStatus.LATE -> late++
                AttendanceStatus.ABSENT -> absent++
                AttendanceStatus.DISMISSED -> dismissed++
                AttendanceStatus.UNMARKED -> unmarked++
            }
        }
        RollCallSummaryStats(
            totalStudents = list.size,
            presentCount = present,
            lateCount = late,
            absentCount = absent,
            dismissedCount = dismissed,
            unmarkedCount = unmarked
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RollCallSummaryStats())

    fun selectClassroom(classroomId: Long) {
        _selectedClassroomId.value = classroomId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateStatusFilter(status: AttendanceStatus?) {
        _statusFilter.value = status
    }

    fun updatePickupFilter(method: PickupMethod?) {
        _pickupFilter.value = method
    }

    fun clearBannerFeedback() {
        _bannerFeedback.value = null
    }

    /**
     * Marks a student's status in a single tap and immediately fires the corresponding
     * parent arrival, late arrival, absence, or dismissal notification.
     */
    fun markStudentStatus(
        student: StudentEntity,
        newStatus: AttendanceStatus,
        pickupMethod: PickupMethod? = null,
        dismissedToGuardian: String? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dateStr = _activeDateString.value
            val classroom = dao.getClassroomById(student.classroomId) ?: return@launch
            val existing = dao.getAttendanceRecord(student.id, dateStr)

            val updatedRecord = AttendanceRecordEntity(
                id = existing?.id ?: 0L,
                studentId = student.id,
                classroomId = student.classroomId,
                dateString = dateStr,
                status = newStatus,
                arrivalTimestampMillis = when (newStatus) {
                    AttendanceStatus.PRESENT, AttendanceStatus.LATE -> existing?.arrivalTimestampMillis ?: now
                    AttendanceStatus.ABSENT, AttendanceStatus.UNMARKED -> null
                    AttendanceStatus.DISMISSED -> existing?.arrivalTimestampMillis ?: now
                },
                dismissalTimestampMillis = if (newStatus == AttendanceStatus.DISMISSED) now else null,
                dismissedToGuardian = if (newStatus == AttendanceStatus.DISMISSED) {
                    dismissedToGuardian ?: student.guardianName
                } else {
                    existing?.dismissedToGuardian
                },
                pickupMethod = pickupMethod ?: existing?.pickupMethod ?: student.defaultPickupMethod,
                markedByTeacher = classroom.teacherName,
                notes = notes.ifBlank { existing?.notes ?: "" },
                lastAlertSentAtMillis = now
            )

            dao.upsertAttendanceRecord(updatedRecord)

            // Trigger Parent Alert if status represents an actionable campus event
            val alertEvent = when (newStatus) {
                AttendanceStatus.PRESENT -> AlertEventType.ARRIVAL
                AttendanceStatus.LATE -> AlertEventType.LATE_ARRIVAL
                AttendanceStatus.ABSENT -> AlertEventType.ABSENCE_NOTICE
                AttendanceStatus.DISMISSED -> AlertEventType.DISMISSAL
                AttendanceStatus.UNMARKED -> null
            }

            if (alertEvent != null) {
                val result = alertDispatcher.dispatchStudentEventAlert(
                    student = student,
                    classroom = classroom,
                    eventType = alertEvent,
                    timestampMillis = now,
                    pickupMethod = updatedRecord.pickupMethod,
                    dismissedToGuardian = updatedRecord.dismissedToGuardian
                )
                if (result != null) {
                    _bannerFeedback.value =
                        "${newStatus.label}: ${student.fullName} • Alerted ${student.guardianName} via ${result.logEntry.channel.label}"
                } else {
                    _bannerFeedback.value = "${newStatus.label} marked for ${student.fullName}"
                }
            } else {
                _bannerFeedback.value = "Reset ${student.fullName} to Unmarked"
            }
        }
    }

    /**
     * 1-Tap Speed Action: Marks all currently UNMARKED students in the active classroom as PRESENT
     * and dispatches arrival notifications to every parent.
     */
    fun markAllUnmarkedAsPresent() {
        viewModelScope.launch {
            val dateStr = _activeDateString.value
            val currentList = rawClassroomStudents.value
            val classroom = dao.getClassroomById(_selectedClassroomId.value) ?: return@launch
            val unmarkedStudents = currentList.filter { swa ->
                (swa.recordForDate(dateStr)?.status ?: AttendanceStatus.UNMARKED) == AttendanceStatus.UNMARKED
            }

            if (unmarkedStudents.isEmpty()) {
                _bannerFeedback.value = "All students in ${classroom.name} are already marked!"
                return@launch
            }

            val now = System.currentTimeMillis()
            var alertsTriggered = 0

            for (swa in unmarkedStudents) {
                val student = swa.student
                val existing = swa.recordForDate(dateStr)
                val record = AttendanceRecordEntity(
                    id = existing?.id ?: 0L,
                    studentId = student.id,
                    classroomId = student.classroomId,
                    dateString = dateStr,
                    status = AttendanceStatus.PRESENT,
                    arrivalTimestampMillis = now,
                    pickupMethod = student.defaultPickupMethod,
                    markedByTeacher = classroom.teacherName,
                    lastAlertSentAtMillis = now
                )
                dao.upsertAttendanceRecord(record)

                val result = alertDispatcher.dispatchStudentEventAlert(
                    student = student,
                    classroom = classroom,
                    eventType = AlertEventType.ARRIVAL,
                    timestampMillis = now
                )
                if (result != null) alertsTriggered++
            }

            _bannerFeedback.value =
                "⚡ Marked ${unmarkedStudents.size} students Present & sent $alertsTriggered parent arrival alerts!"
        }
    }

    /**
     * Batch Dismissal Action: Dismisses all Present/Late students for a chosen pickup mode
     * (or all remaining students in the classroom) and notifies all parents.
     */
    fun dismissBatchByPickupMethod(filterMethod: PickupMethod?) {
        viewModelScope.launch {
            val dateStr = _activeDateString.value
            val classroom = dao.getClassroomById(_selectedClassroomId.value) ?: return@launch
            val candidates = rawClassroomStudents.value.filter { swa ->
                val rec = swa.recordForDate(dateStr)
                val status = rec?.status ?: AttendanceStatus.UNMARKED
                val effectiveMethod = rec?.pickupMethod ?: swa.student.defaultPickupMethod
                val isOnCampus = status == AttendanceStatus.PRESENT || status == AttendanceStatus.LATE || status == AttendanceStatus.UNMARKED
                val matchesMethod = filterMethod == null || effectiveMethod == filterMethod
                isOnCampus && matchesMethod
            }

            if (candidates.isEmpty()) {
                _bannerFeedback.value = "No remaining students to dismiss for this group."
                return@launch
            }

            val now = System.currentTimeMillis()
            var alertsSent = 0
            for (swa in candidates) {
                val student = swa.student
                val existing = swa.recordForDate(dateStr)
                val effectiveMethod = existing?.pickupMethod ?: student.defaultPickupMethod
                val record = AttendanceRecordEntity(
                    id = existing?.id ?: 0L,
                    studentId = student.id,
                    classroomId = student.classroomId,
                    dateString = dateStr,
                    status = AttendanceStatus.DISMISSED,
                    arrivalTimestampMillis = existing?.arrivalTimestampMillis ?: now,
                    dismissalTimestampMillis = now,
                    dismissedToGuardian = student.guardianName,
                    pickupMethod = effectiveMethod,
                    markedByTeacher = classroom.teacherName,
                    lastAlertSentAtMillis = now
                )
                dao.upsertAttendanceRecord(record)

                val res = alertDispatcher.dispatchStudentEventAlert(
                    student = student,
                    classroom = classroom,
                    eventType = AlertEventType.DISMISSAL,
                    timestampMillis = now,
                    pickupMethod = effectiveMethod,
                    dismissedToGuardian = student.guardianName
                )
                if (res != null) alertsSent++
            }

            val label = filterMethod?.label ?: "All Groups"
            _bannerFeedback.value =
                "🚌 Dismissed ${candidates.size} students ($label) & dispatched $alertsSent parent alerts!"
        }
    }

    fun addNewStudent(
        firstName: String,
        lastName: String,
        badgeCode: String,
        guardianName: String,
        guardianRelation: String,
        guardianPhone: String,
        guardianEmail: String,
        preferredChannel: AlertChannel,
        defaultPickupMethod: PickupMethod,
        busRouteOrNotes: String
    ) {
        viewModelScope.launch {
            val palette = listOf(0xFF2563EB, 0xFF7C3AED, 0xFF059669, 0xFFD97706, 0xFFDC2626, 0xFF0891B2)
            val color = palette[(firstName.hashCode().and(0x7FFFFFFF)) % palette.size]
            val student = StudentEntity(
                classroomId = _selectedClassroomId.value,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                studentBadgeCode = badgeCode.trim().ifBlank { "STU-${(100..999).random()}" },
                avatarColorHex = color,
                guardianName = guardianName.trim(),
                guardianRelationship = guardianRelation.trim().ifBlank { "Parent" },
                guardianPhone = guardianPhone.trim(),
                guardianEmail = guardianEmail.trim(),
                preferredChannel = preferredChannel,
                defaultPickupMethod = defaultPickupMethod,
                authorizedPickupList = "${guardianName.trim()} (${guardianRelation.trim().ifBlank { "Parent" }})",
                busRouteOrNotes = busRouteOrNotes.trim()
            )
            dao.insertStudent(student)
            _bannerFeedback.value = "Added ${student.fullName} to roster!"
        }
    }

    fun retryParentAlert(log: ParentAlertLogEntity) {
        viewModelScope.launch {
            val result = alertDispatcher.retryAlertLog(log)
            _bannerFeedback.value =
                "Retried alert for ${log.studentName}: ${result.logEntry.deliveryStatus.label}"
        }
    }

    fun retryAllPendingOrFailedAlerts() {
        viewModelScope.launch {
            val pending = dao.getAlertsByStatus(
                listOf(DeliveryStatus.FAILED, DeliveryStatus.QUEUED_OFFLINE)
            )
            if (pending.isEmpty()) {
                _bannerFeedback.value = "No queued or failed alerts to retry."
                return@launch
            }
            var succeeded = 0
            for (item in pending) {
                val res = alertDispatcher.retryAlertLog(item)
                if (res.wasSuccessful) succeeded++
            }
            _bannerFeedback.value = "Retried ${pending.size} alerts ($succeeded succeeded)."
        }
    }

    fun updateGatewayConfig(newConfig: AlertGatewayConfig) {
        settingsRepository.updateConfig(newConfig)
        _bannerFeedback.value = "Saved parent alert settings."
    }

    fun sendTestParentAlert(testRecipientPhone: String, testRecipientEmail: String) {
        viewModelScope.launch {
            val classroom = classrooms.value.firstOrNull() ?: ClassroomEntity(
                id = 1L,
                name = "Grade 3A • Homeroom",
                gradeLevel = "3rd Grade",
                roomNumber = "Room 104",
                teacherName = "Ms. Sarah Jenkins",
                scheduleWindow = "8:00 AM – 3:15 PM"
            )
            val dummyStudent = StudentEntity(
                id = 9999L,
                classroomId = classroom.id,
                firstName = "Alex",
                lastName = "DemoStudent",
                studentBadgeCode = "TEST-01",
                avatarColorHex = 0xFF2563EB,
                guardianName = "Parent Test Contact",
                guardianRelationship = "Guardian",
                guardianPhone = testRecipientPhone.ifBlank { "+15550001234" },
                guardianEmail = testRecipientEmail.ifBlank { "parent.test@example.com" },
                preferredChannel = AlertChannel.ANDROID_SMS,
                defaultPickupMethod = PickupMethod.PARENT_CARLINE,
                authorizedPickupList = "Parent Test Contact"
            )
            val res = alertDispatcher.dispatchStudentEventAlert(
                student = dummyStudent,
                classroom = classroom,
                eventType = AlertEventType.ARRIVAL,
                timestampMillis = System.currentTimeMillis()
            )
            if (res != null) {
                _bannerFeedback.value =
                    "Test alert logged (${res.logEntry.deliveryStatus.label}): ${res.logEntry.statusDetail}"
            }
        }
    }

    fun buildCsvExportReport(): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Student,Badge ID,Status,Arrival Time,Dismissal Time,Pickup Method,Released To,Guardian Phone")
        val dateStr = _activeDateString.value
        val items = rollCallItems.value
        for (item in items) {
            val rec = item.record
            val arrivalStr = rec?.arrivalTimestampMillis?.let { timeFormatter.format(Date(it)) } ?: "-"
            val dismissalStr = rec?.dismissalTimestampMillis?.let { timeFormatter.format(Date(it)) } ?: "-"
            val methodStr = (rec?.pickupMethod ?: item.student.defaultPickupMethod).label
            val releasedTo = rec?.dismissedToGuardian ?: "-"
            sb.appendLine(
                "\"$dateStr\",\"${item.student.fullName}\",\"${item.student.studentBadgeCode}\",\"${item.status.label}\",\"$arrivalStr\",\"$dismissalStr\",\"$methodStr\",\"$releasedTo\",\"${item.student.guardianPhone}\""
            )
        }
        return sb.toString()
    }

    class Factory(
        private val dao: AttendanceDao,
        private val settingsRepository: AlertSettingsRepository,
        private val alertDispatcher: ParentAlertDispatcher
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AttendanceViewModel(dao, settingsRepository, alertDispatcher) as T
        }
    }
}
