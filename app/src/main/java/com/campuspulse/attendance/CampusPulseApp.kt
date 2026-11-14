package com.campuspulse.attendance

import android.app.Application
import com.campuspulse.attendance.data.AlertSettingsRepository
import com.campuspulse.attendance.data.AppDatabase
import com.campuspulse.attendance.service.ParentAlertDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CampusPulseApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this, applicationScope)
    }

    val settingsRepository: AlertSettingsRepository by lazy {
        AlertSettingsRepository(this)
    }

    val alertDispatcher: ParentAlertDispatcher by lazy {
        ParentAlertDispatcher(
            context = this,
            dao = database.attendanceDao(),
            settingsRepository = settingsRepository
        )
    }
}
