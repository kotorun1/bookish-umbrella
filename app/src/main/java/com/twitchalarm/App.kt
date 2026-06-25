package com.twitchalarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager

class App : Application(), Configuration.Provider {

    companion object {
        const val CHANNEL_ALARM   = "twitch_alarm"
        const val CHANNEL_SERVICE = "twitch_service"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)

        // High-priority channel for stream-start alarm
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            "Стрим начался",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Уведомление когда начинается стрим на Twitch"
            enableVibration(true)
            enableLights(true)
            setShowBadge(true)
        }

        // Low-priority channel for background worker
        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            "Проверка стримов",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Фоновая проверка статуса стримеров"
        }

        nm.createNotificationChannels(listOf(alarmChannel, serviceChannel))
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
