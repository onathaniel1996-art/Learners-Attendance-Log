package com.campuspulse.attendance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.campuspulse.attendance.data.model.AttendanceRecordEntity
import com.campuspulse.attendance.data.model.ClassroomEntity
import com.campuspulse.attendance.data.model.DeliveryStatus
import com.campuspulse.attendance.data.model.ParentAlertLogEntity
import com.campuspulse.attendance.data.model.StudentEntity
import com.campuspulse.attendance.data.model.StudentWithAttendance
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    // --- Classrooms ---
    @Query("SELECT * FROM classrooms ORDER BY id ASC")
    fun observeClassrooms(): Flow<List<ClassroomEntity>>

    @Query("SELECT * FROM classrooms WHERE id = :classroomId LIMIT 1")
    suspend fun getClassroomById(classroomId: Long): ClassroomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassrooms(classrooms: List<ClassroomEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassroom(classroom: ClassroomEntity): Long

    // --- Students & Roll Call ---
    @Transaction
    @Query("SELECT * FROM students WHERE classroomId = :classroomId ORDER BY lastName ASC, firstName ASC")
    fun observeStudentsWithAttendance(classroomId: Long): Flow<List<StudentWithAttendance>>

    @Transaction
    @Query("SELECT * FROM students ORDER BY classroomId ASC, lastName ASC")
    fun observeAllStudentsWithAttendance(): Flow<List<StudentWithAttendance>>

    @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
    suspend fun getStudentById(studentId: Long): StudentEntity?

    @Query("SELECT * FROM students WHERE studentBadgeCode = :badgeCode LIMIT 1")
    suspend fun getStudentByBadgeCode(badgeCode: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    // --- Attendance Records ---
    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND dateString = :dateString LIMIT 1")
    suspend fun getAttendanceRecord(studentId: Long, dateString: String): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records WHERE dateString = :dateString ORDER BY COALESCE(dismissalTimestampMillis, arrivalTimestampMillis, 0) DESC")
    fun observeAttendanceRecordsByDate(dateString: String): Flow<List<AttendanceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendanceRecord(record: AttendanceRecordEntity): Long

    // --- Parent Alert Logs ---
    @Query("SELECT * FROM parent_alert_logs ORDER BY timestampMillis DESC LIMIT 250")
    fun observeParentAlertLogs(): Flow<List<ParentAlertLogEntity>>

    @Query("SELECT * FROM parent_alert_logs WHERE deliveryStatus IN (:statuses) ORDER BY timestampMillis ASC")
    suspend fun getAlertsByStatus(statuses: List<DeliveryStatus>): List<ParentAlertLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParentAlertLog(log: ParentAlertLogEntity): Long

    @Update
    suspend fun updateParentAlertLog(log: ParentAlertLogEntity)

    @Query("DELETE FROM parent_alert_logs")
    suspend fun clearParentAlertLogs()
}
