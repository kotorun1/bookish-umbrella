package com.twitchalarm.work

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.twitchalarm.App
import com.twitchalarm.R
import com.twitchalarm.api.TwitchApi
import com.twitchalarm.data.AppDatabase
import com.twitchalarm.ui.AlarmActivity

class StreamCheckWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    companion object {
        private const val TAG = "StreamCheckWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Начинаем проверку стримов...")

        val db  = AppDatabase.getInstance(ctx)
        val dao = db.streamerDao()

        val streamers = dao.getAll()
        if (streamers.isEmpty()) {
            Log.d(TAG, "Нет стримеров для проверки")
            return Result.success()
        }

        val logins  = streamers.filter { it.notifyEnabled }.map { it.login }
        val results = TwitchApi.checkStreams(logins)

        results.forEach { info ->
            val prev = dao.getByLogin(info.login) ?: return@forEach

            // Обновляем статус в БД
            dao.updateLiveStatus(
                login       = info.login,
                isLive      = info.isLive,
                title       = info.title,
                viewers     = info.viewerCount,
                game        = info.gameName,
                displayName = info.displayName
            )

            // Стрим только что начался (был offline, стал online)
            val justWentLive = !prev.isLive && info.isLive
            if (justWentLive && prev.notifyEnabled) {
                Log.d(TAG, "${info.displayName} начал стрим!")
                triggerAlarm(info.displayName, info.title, info.gameName, info.viewerCount)
            }
        }

        return Result.success()
    }

    private fun triggerAlarm(
        displayName: String,
        title: String,
        game: String,
        viewers: Int
    ) {
        // Intent для открытия AlarmActivity (экран будильника)
        val alarmIntent = Intent(ctx, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmActivity.EXTRA_STREAMER,  displayName)
            putExtra(AlarmActivity.EXTRA_TITLE,     title)
            putExtra(AlarmActivity.EXTRA_GAME,      game)
            putExtra(AlarmActivity.EXTRA_VIEWERS,   viewers)
        }

        val fullScreenPi = PendingIntent.getActivity(
            ctx, displayName.hashCode(), alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val viewerText = if (viewers > 0) " · ${formatViewers(viewers)} зрителей" else ""
        val gameText   = if (game.isNotEmpty()) " играет в $game" else ""

        val notification = NotificationCompat.Builder(ctx, App.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_twitch)
            .setContentTitle("🔴 $displayName в эфире!")
            .setContentText(title.ifEmpty { "$displayName$gameText" })
            .setSubText(viewerText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .setOngoing(false)
            .build()

        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.notify(displayName.hashCode(), notification)

        // Запускаем AlarmActivity напрямую (открывает экран даже на lock screen)
        ctx.startActivity(alarmIntent)
    }

    private fun formatViewers(count: Int): String = when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000     -> "${count / 1_000}K"
        else               -> "$count"
    }
}
