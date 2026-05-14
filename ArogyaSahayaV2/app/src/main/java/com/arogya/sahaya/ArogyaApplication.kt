package com.arogya.sahaya

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ArogyaApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannels(listOf(
                NotificationChannel(CHANNEL_PILLS, "Pill Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Medicine intake reminders"; enableVibration(true)
                },
                NotificationChannel(CHANNEL_VITALS, "Vital Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Abnormal vital sign alerts"
                },
                NotificationChannel(CHANNEL_ASHA, "ASHA & Health Camp Alerts", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_EMERGENCY, "Emergency SOS", NotificationManager.IMPORTANCE_MAX),
                NotificationChannel(CHANNEL_VOICE, "Voice Assistant", NotificationManager.IMPORTANCE_LOW)
            ))
        }
    }

    companion object {
        const val CHANNEL_PILLS = "pill_reminders"
        const val CHANNEL_VITALS = "vital_alerts"
        const val CHANNEL_ASHA = "asha_alerts"
        const val CHANNEL_EMERGENCY = "emergency"
        const val CHANNEL_VOICE = "voice_assistant"
    }
}
