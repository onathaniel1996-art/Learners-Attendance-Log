package com.campuspulse.attendance.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class AttendanceStatus(val label: String, val shortCode: String) {
    UNMARKED("Unmarked", "—"),
    PRESENT("Present", "P"),
    LATE("Late", "L"),
    ABSENT("Absent", "A"),
    DISMISSED("Dismissed", "D")
}

enum class PickupMethod(val label: String, val iconTag: String) {
    PARENT_CARLINE("Parent Carline", "CAR"),
    SCHOOL_BUS("School Bus", "BUS"),
    WALKER("Authorized Walker", "WALK"),
    AFTERCARE("Campus Aftercare", "CARE")
}

enum class AlertChannel(val label: String) {
    ANDROID_SMS("Direct Cellular SMS"),
    TWILIO_SMS("Twilio SMS"),
    WHATSAPP("WhatsApp Alert"),
    RESEND_EMAIL("Email (Resend)")
}

enum class AlertEventType(val label: String) {
    ARRIVAL("Campus Arrival"),
    LATE_ARRIVAL("Late Arrival"),
    ABSENCE_NOTICE("Absence Alert"),
    DISMISSAL("Campus Dismissal")
}

enum class DeliveryStatus(val label: String) {
    SENT("Delivered"),
    SIMULATED("Simulated (Sandbox)"),
    QUEUED_OFFLINE("Queued (Offline)"),
    FAILED("Failed")
}

@Entity(tableName = "classrooms")
data class ClassroomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gradeLevel: String,
    val roomNumber: String,
    val teacherName: String,
    val scheduleWindow: String
)

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = ClassroomEntity::class,
            parentColumns = ["id"],
            childColumns = ["classroomId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("classroomId")]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classroomId: Long,
    val firstName: String,
    val lastName: String,
    val studentBadgeCode: String,
    val avatarColorHex: Long,
    val guardianName: String,
    val guardianRelationship: String,
    val guardianPhone: String,
    val guardianEmail: String,
    val preferredChannel: AlertChannel = AlertChannel.ANDROID_SMS,
    val defaultPickupMethod: PickupMethod = PickupMethod.PARENT_CARLINE,
    val authorizedPickupList: String, // Comma-separated names of authorized adults
    val busRouteOrNotes: String = ""
) {
    val fullName: String get() = "$firstName $lastName"
    val initials: String
        get() = "${firstName.firstOrNull()?.uppercaseChar() ?: ""}${lastName.firstOrNull()?.uppercaseChar() ?: ""}"
}

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId", "dateString"], unique = true),
        Index(value = ["classroomId", "dateString"])
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val classroomId: Long,
    val dateString: String, // Format: YYYY-MM-DD
    val status: AttendanceStatus = AttendanceStatus.UNMARKED,
    val arrivalTimestampMillis: Long? = null,
    val dismissalTimestampMillis: Long? = null,
    val dismissedToGuardian: String? = null,
    val pickupMethod: PickupMethod? = null,
    val markedByTeacher: String = "",
    val notes: String = "",
    val lastAlertSentAtMillis: Long? = null
)

@Entity(
    tableName = "parent_alert_logs",
    indices = [Index("studentId"), Index("timestampMillis")]
)
data class ParentAlertLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val studentName: String,
    val classroomName: String,
    val guardianName: String,
    val recipientContact: String,
    val channel: AlertChannel,
    val eventType: AlertEventType,
    val messageBody: String,
    val deliveryStatus: DeliveryStatus,
    val timestampMillis: Long = System.currentTimeMillis(),
    val statusDetail: String = ""
)

/**
 * Combined UI projection pairing a Student with their AttendanceRecord for the active date.
 */
data class StudentWithAttendance(
    @Embedded val student: StudentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "studentId"
    )
    val records: List<AttendanceRecordEntity>
) {
    fun recordForDate(dateString: String): AttendanceRecordEntity? {
        return records.firstOrNull { it.dateString == dateString }
    }
}
