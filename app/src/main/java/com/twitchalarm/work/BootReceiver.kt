package com.twitchalarm.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.work.*
import com.twitchalarm.ui.SettingsActivity
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleWork(context)
        }
    }

    companion object {
        fun scheduleWork(context: Context) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val intervalMinutes = prefs.getInt(
                SettingsActivity.KEY_CHECK_INTERVAL, 
                SettingsActivity.DEFAULT_INTERVAL
            )

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<StreamCheckWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "StreamCheck",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }
    }
}