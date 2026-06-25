package com.twitchalarm.ui

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.twitchalarm.databinding.ActivityAlarmBinding

class AlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmBinding
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    companion object {
        const val EXTRA_STREAMER = "streamer_name"
        const val EXTRA_TITLE    = "stream_title"
        const val EXTRA_GAME     = "stream_game"
        const val EXTRA_VIEWERS  = "viewer_count"
        private const val TAG    = "AlarmActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON   or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val streamer = intent.getStringExtra(EXTRA_STREAMER) ?: "Стример"
        val title    = intent.getStringExtra(EXTRA_TITLE)    ?: ""
        val game     = intent.getStringExtra(EXTRA_GAME)     ?: ""
        val viewers  = intent.getIntExtra(EXTRA_VIEWERS, 0)

        bindUI(streamer, title, game, viewers)
        startAlarmSound()
        startVibration()
    }

    private fun bindUI(streamer: String, title: String, game: String, viewers: Int) {
        binding.tvStreamerName.text = streamer
        binding.tvLiveBadge.text   = "🔴 В ЭФИРЕ"
        binding.tvStreamTitle.text = title.ifEmpty { "Начался стрим!" }

        val parts = mutableListOf<String>()
        if (game.isNotEmpty()) parts.add(game)
        if (viewers > 0) {
            val vStr = when {
                viewers >= 1_000_000 -> "M зрителей"
                viewers >= 1_000     -> "K зрителей"
                else                 -> " зрителей"
            }
            parts.add(vStr)
        }
        binding.tvMeta.text = parts.joinToString(" · ")

        binding.btnDismiss.setOnClickListener {
            stopAlarm()
            finish()
        }

        binding.btnWatch.setOnClickListener {
            stopAlarm()
            val twitchIntent = packageManager.getLaunchIntentForPackage("tv.twitch.android.app")
            if (twitchIntent != null) {
                startActivity(twitchIntent)
            } else {
                val webIntent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://twitch.tv/")
                )
                startActivity(webIntent)
            }
            finish()
        }
    }

    private fun startAlarmSound() {
        audioManager = getSystemService(AudioManager::class.java)

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttrs)
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(false)
                .build()
            audioManager?.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN)
        }

        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 7
        audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

        val candidates: List<Uri?> = listOf(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            Settings.System.DEFAULT_ALARM_ALERT_URI,
            Settings.System.DEFAULT_RINGTONE_URI
        )

        val uri = candidates.firstOrNull { it != null } ?: run {
            Log.e(TAG, "Не найден ни один звуковой URI")
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttrs)
                setDataSource(this@AlarmActivity, uri)
                isLooping = true
                setOnPreparedListener { 
                    Log.d(TAG, "MediaPlayer prepared, starting...")
                    start() 
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what= extra=")
                    tryFallbackSound(candidates, uri, audioAttrs)
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer init failed: ")
            tryFallbackSound(candidates, uri, audioAttrs)
        }
    }

    private fun tryFallbackSound(candidates: List<Uri?>, failedUri: Uri?, audioAttrs: AudioAttributes) {
        val fallback = candidates.firstOrNull { it != null && it != failedUri } ?: return
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttrs)
                setDataSource(this@AlarmActivity, fallback)
                isLooping = true
                setOnPreparedListener { 
                    Log.d(TAG, "Fallback MediaPlayer prepared, starting...")
                    start() 
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback sound also failed: ")
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 500, 300, 500, 300, 1000, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VibratorManager::class.java)
            vibrator = vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(Vibrator::class.java)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.apply { if (isPlaying) stop(); release() }
        } catch (_: Exception) {}
        mediaPlayer = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }

        vibrator?.cancel()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    override fun onBackPressed() {}
}
