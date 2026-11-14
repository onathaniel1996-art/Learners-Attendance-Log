package com.campuspulse.attendance

import com.campuspulse.attendance.data.model.AttendanceStatus
import com.campuspulse.attendance.data.model.PickupMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class AlertTemplateFormattingTest {

    @Test
    fun attendanceStatus_hasExpectedLabelsAndShortCodes() {
        assertEquals("Present", AttendanceStatus.PRESENT.label)
        assertEquals("P", AttendanceStatus.PRESENT.shortCode)
        assertEquals("Dismissed", AttendanceStatus.DISMISSED.label)
        assertEquals("D", AttendanceStatus.DISMISSED.shortCode)
    }

    @Test
    fun pickupMethod_hasExpectedLabels() {
        assertEquals("Parent Carline", PickupMethod.PARENT_CARLINE.label)
        assertEquals("School Bus", PickupMethod.SCHOOL_BUS.label)
    }
}
