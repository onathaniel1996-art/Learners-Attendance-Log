package com.campuspulse.attendance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.campuspulse.attendance.data.model.AlertChannel
import com.campuspulse.attendance.data.model.AlertEventType
import com.campuspulse.attendance.data.model.AttendanceRecordEntity
import com.campuspulse.attendance.data.model.AttendanceStatus
import com.campuspulse.attendance.data.model.ClassroomEntity
import com.campuspulse.attendance.data.model.DeliveryStatus
import com.campuspulse.attendance.data.model.ParentAlertLogEntity
import com.campuspulse.attendance.data.model.PickupMethod
import com.campuspulse.attendance.data.model.StudentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoomConverters {
    @TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus): String = value.name

    @TypeConverter
    fun toAttendanceStatus(value: String): AttendanceStatus =
        runCatching { AttendanceStatus.valueOf(value) }.getOrDefault(AttendanceStatus.UNMARKED)

    @TypeConverter
    fun fromPickupMethod(value: PickupMethod?): String? = value?.name

    @TypeConverter
    fun toPickupMethod(value: String?): PickupMethod? =
        value?.let { runCatching { PickupMethod.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromAlertChannel(value: AlertChannel): String = value.name

    @TypeConverter
    fun toAlertChannel(value: String): AlertChannel =
        runCatching { AlertChannel.valueOf(value) }.getOrDefault(AlertChannel.ANDROID_SMS)

    @TypeConverter
    fun fromAlertEventType(value: AlertEventType): String = value.name

    @TypeConverter
    fun toAlertEventType(value: String): AlertEventType =
        runCatching { AlertEventType.valueOf(value) }.getOrDefault(AlertEventType.ARRIVAL)

    @TypeConverter
    fun fromDeliveryStatus(value: DeliveryStatus): String = value.name

    @TypeConverter
    fun toDeliveryStatus(value: String): DeliveryStatus =
        runCatching { DeliveryStatus.valueOf(value) }.getOrDefault(DeliveryStatus.SIMULATED)
}

@Database(
    entities = [
        ClassroomEntity::class,
        StudentEntity::class,
        AttendanceRecordEntity::class,
        ParentAlertLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "campuspulse_attendance.db"
                )
                    .addCallback(SeedDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedDatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    seedInitialCampusData(database.attendanceDao())
                }
            }
        }

        private suspend fun seedInitialCampusData(dao: AttendanceDao) {
            val classrooms = listOf(
                ClassroomEntity(
                    id = 1L,
                    name = "Grade 3A • Homeroom",
                    gradeLevel = "3rd Grade",
                    roomNumber = "Room 104",
                    teacherName = "Ms. Sarah Jenkins",
                    scheduleWindow = "8:00 AM – 3:15 PM"
                ),
                ClassroomEntity(
                    id = 2L,
                    name = "Grade 5B • STEM Lab",
                    gradeLevel = "5th Grade",
                    roomNumber = "Room 210",
                    teacherName = "Mr. David Okafor",
                    scheduleWindow = "8:10 AM – 3:30 PM"
                ),
                ClassroomEntity(
                    id = 3L,
                    name = "Kindergarten • Sunshine",
                    gradeLevel = "Kindergarten",
                    roomNumber = "Room 101",
                    teacherName = "Mrs. Priya Patel",
                    scheduleWindow = "8:15 AM – 2:45 PM"
                )
            )
            dao.insertClassrooms(classrooms)

            val demoStudents = listOf(
                // Grade 3A (Classroom 1)
                StudentEntity(
                    classroomId = 1L,
                    firstName = "Leo",
                    lastName = "Martinez",
                    studentBadgeCode = "STU-301",
                    avatarColorHex = 0xFF2563EB,
                    guardianName = "Elena Martinez",
                    guardianRelationship = "Mother",
                    guardianPhone = "+15552348901",
                    guardianEmail = "elena.martinez@example.com",
                    preferredChannel = AlertChannel.ANDROID_SMS,
                    defaultPickupMethod = PickupMethod.PARENT_CARLINE,
                    authorizedPickupList = "Elena Martinez (Mother), Carlos Martinez (Father), Rosa Gomez (Grandmother)",
                    busRouteOrNotes = "Carline Tag #A-42"
                ),
                StudentEntity(
                    classroomId = 1L,
                    firstName = "Maya",
                    lastName = "Chen",
                    studentBadgeCode = "STU-302",
                    avatarColorHex = 0xFF7C3AED,
                    guardianName = "Raymond Chen",
                    guardianRelationship = "Father",
                    guardianPhone = "+15552348902",
                    guardianEmail = "raymond.chen@example.com",
                    preferredChannel = AlertChannel.WHATSAPP,
                    defaultPickupMethod = PickupMethod.SCHOOL_BUS,
                    authorizedPickupList = "Raymond Chen (Father), Lin Chen (Mother)",
                    busRouteOrNotes = "Bus #14 • Oak Creek Stop"
                ),
                StudentEntity(
                    classroomId = 1L,
                    firstName = "Liam",
                    lastName = "O'Connor",
                    studentBadgeCode = "STU-303",
                    avatarColorHex = 0xFF059669,
                    guardianName = "Claire O'Connor",
                    guardianRelationship = "Mother",
                    guardianPhone = "+15552348903",
                    guardianEmail = "claire.oconnor@example.com",
                    preferredChannel = AlertChannel.ANDROID_SMS,
                    defaultPickupMethod = PickupMethod.PARENT_CARLINE,
                    authorizedPickupList = "Claire O'Connor (Mother), Sean O'Connor (Father)",
                    busRouteOrNotes = "Carline Tag #A-18 • Peanut Allergy"
                ),
                StudentEntity(
                    classroomId = 1L,
                    firstName = "Zara",
                    lastName = "Al-Mansoor",
                    studentBadgeCode = "STU-304",
                    avatarColorHex = 0xFFD97706,
                    guardianName = "Tariq Al-Mansoor",
                    guardianRelationship = "Father",
                    guardianPhone = "+15552348904",
                    guardianEmail = "tariq.almansoor@example.com",
                    preferredChannel = AlertChannel.RESEND_EMAIL,
                    defaultPickupMethod = PickupMethod.AFTERCARE,
                    authorizedPickupList = "Tariq Al-Mansoor (Father), Nadia Al-Mansoor (Mother)",
                    busRouteOrNotes = "STEAM Aftercare Room B"
                ),
                StudentEntity(
                    classroomId = 1L,
                    firstName = "Noah",
                    lastName = "Brooks",
                    studentBadgeCode = "STU-305",
                    avatarColorHex = 0xFFDC2626,
                    guardianName = "Tasha Brooks",
                    guardianRelationship = "Mother",
                    guardianPhone = "+15552348905",
                    guardianEmail = "tasha.brooks@example.com",
                    preferredChannel = AlertChannel.ANDROID_SMS,
                    defaultPickupMethod = PickupMethod.SCHOOL_BUS,
                    authorizedPickupList = "Tasha Brooks (Mother), Marcus Brooks (Uncle)",
                    busRouteOrNotes = "Bus #7 • Maple Ave"
                ),
                StudentEntity(
                    classroomId = 1L,
                    firstName = "Chloe",
                    lastName = "Dupont",
                    studentBadgeCode = "STU-306",
                    avatarColorHex = 0xFF0891B2,
                    guardianName = "Marc Dupont",
                    guardianRelationship = "Father",
                    guardianPhone = "+15552348906",
                    guardianEmail = "marc.dupont@example.com",
                    preferredChannel = AlertChannel.WHATSAPP,
                    defaultPickupMethod = PickupMethod.WALKER,
                    authorizedPickupList = "Marc Dupont (Father), Sophie Dupont (Older Sister)",
                    busRouteOrNotes = "North Gate Walker Pass"
                ),
                StudentEntity(
                    classroomId = 1L,
                    firstName = "Ethan",
                    lastName = "Kowalski",
                    studentBadgeCode = "STU-307",
                    avatarColorHex = 0xFF4F46E5,
                    guardianName = "Hannah Kowalski",
                    guardianRelationship = "Mother",
                    guardianPhone = "+15552348907",
                    guardianEmail = "hannah.k@example.com",
                    preferredChannel = AlertChannel.ANDROID_SMS,
                    defaultPickupMethod = PickupMethod.PARENT_CARLINE,
                    authorizedPickupList = "Hannah Kowalski (Mother), Piotr Kowalski (Father)",
                    busRouteOrNotes = "Carline Tag #B-09"
                ),
                StudentEntity(
                    classroomId = 1L,
                    firstName = "Amara",
                    lastName = "mensah",
                    studentBadgeCode = "STU-308",
                    avatarColorHex = 0xFFBE185D,
                    guardianName = "Kofi Mensah",
                    guardianRelationship = "Father",
                    guardianPhone = "+15552348908",
                    guardianEmail = "kofi.mensah@example.com",
                    preferredChannel = AlertChannel.TWILIO_SMS,
                    defaultPickupMethod = PickupMethod.SCHOOL_BUS,
                    authorizedPickupList = "Kofi Mensah (Father), Abena Mensah (Mother)",
                    busRouteOrNotes = "Bus #14 • Oak Creek Stop"
                ),

                // Grade 5B (Classroom 2)
                StudentEntity(
                    classroomId = 2L,
                    firstName = "Lucas",
                    lastName = "Silva",
                    studentBadgeCode = "STU-501",
                    avatarColorHex = 0xFF1D4ED8,
                    guardianName = "Camila Silva",
                    guardianRelationship = "Mother",
                    guardianPhone = "+15558765401",
                    guardianEmail = "camila.silva@example.com",
                    preferredChannel = AlertChannel.WHATSAPP,
                    defaultPickupMethod = PickupMethod.WALKER,
                    authorizedPickupList = "Camila Silva (Mother), Mateo Silva (Father)",
                    busRouteOrNotes = "East Crosswalk Walker"
                ),
                StudentEntity(
                    classroomId = 2L,
                    firstName = "Sofia",
                    lastName = "Rossi",
                    studentBadgeCode = "STU-502",
                    avatarColorHex = 0xFF9333EA,
                    guardianName = "Marco Rossi",
                    guardianRelationship = "Father",
                    guardianPhone = "+15558765402",
                    guardianEmail = "marco.rossi@example.com",
                    preferredChannel = AlertChannel.ANDROID_SMS,
                    defaultPickupMethod = PickupMethod.PARENT_CARLINE,
                    authorizedPickupList = "Marco Rossi (Father), Giulia Rossi (Mother)",
                    busRouteOrNotes = "Carline Tag #C-11"
                ),
                StudentEntity(
                    classroomId = 2L,
                    firstName = "Caleb",
                    lastName = "Washington",
                    studentBadgeCode = "STU-503",
                    avatarColorHex = 0xFF047857,
                    guardianName = "Denise Washington",
                    guardianRelationship = "Mother",
                    guardianPhone = "+15558765403",
                    guardianEmail = "denise.w@example.com",
                    preferredChannel = AlertChannel.ANDROID_SMS,
                    defaultPickupMethod = PickupMethod.SCHOOL_BUS,
                    authorizedPickupList = "Denise Washington (Mother), Darnell Washington (Father)",
                    busRouteOrNotes = "Bus #3 • Riverbend"
                ),
                StudentEntity(
                    classroomId = 2L,
                    firstName = "Hana",
                    lastName = "Takahashi",
                    studentBadgeCode = "STU-504",
                    avatarColorHex = 0xFFB45309,
                    guardianName = "Kenji Takahashi",
                    guardianRelationship = "Father",
                    guardianPhone = "+15558765404",
                    guardianEmail = "kenji.t@example.com",
                    preferredChannel = AlertChannel.RESEND_EMAIL,
                    defaultPickupMethod = PickupMethod.AFTERCARE,
                    authorizedPickupList = "Kenji Takahashi (Father), Yumi Takahashi (Mother)",
                    busRouteOrNotes = "Robotics Club T/Th"
                ),

                // Kindergarten (Classroom 3)
                StudentEntity(
                    classroomId = 3L,
                    firstName = "Oliver",
                    lastName = "Bennett",
                    studentBadgeCode = "STU-001",
                    avatarColorHex = 0xFF0284C7,
                    guardianName = "Megan Bennett",
                    guardianRelationship = "Mother",
                    guardianPhone = "+15553331101",
                    guardianEmail = "megan.bennett@example.com",
                    preferredChannel = AlertChannel.ANDROID_SMS,
                    defaultPickupMethod = PickupMethod.PARENT_CARLINE,
                    authorizedPickupList = "Megan Bennett (Mother), Ryan Bennett (Father)",
                    busRouteOrNotes = "Kindergarten Front Loop #K-04"
                ),
                StudentEntity(
                    classroomId = 3L,
                    firstName = "Aria",
                    lastName = "Kapoor",
                    studentBadgeCode = "STU-002",
                    avatarColorHex = 0xFFE11D48,
                    guardianName = "Vikram Kapoor",
                    guardianRelationship = "Father",
                    guardianPhone = "+15553331102",
                    guardianEmail = "vikram.kapoor@example.com",
                    preferredChannel = AlertChannel.WHATSAPP,
                    defaultPickupMethod = PickupMethod.PARENT_CARLINE,
                    authorizedPickupList = "Vikram Kapoor (Father), Ananya Kapoor (Mother)",
                    busRouteOrNotes = "Kindergarten Front Loop #K-12"
                ),
                StudentEntity(
                    classroomId = 3L,
                    firstName = "Mateo",
                    lastName = "Reyes",
                    studentBadgeCode = "STU-003",
                    avatarColorHex = 0xFF16A34A,
                    guardianName = "Lucia Reyes",
                    guardianRelationship = "Mother",
                    guardianPhone = "+15553331103",
                    guardianEmail = "lucia.reyes@example.com",
                    preferredChannel = AlertChannel.ANDROID_SMS,
                    defaultPickupMethod = PickupMethod.SCHOOL_BUS,
                    authorizedPickupList = "Lucia Reyes (Mother), Diego Reyes (Father)",
                    busRouteOrNotes = "Mini-Bus #1 • Escort Required"
                )
            )
            dao.insertStudents(demoStudents)
        }
    }
}
