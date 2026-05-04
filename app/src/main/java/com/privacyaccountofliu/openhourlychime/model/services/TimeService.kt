package com.privacyaccountofliu.openhourlychime.model.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.privacyaccountofliu.openhourlychime.MainActivity
import com.privacyaccountofliu.openhourlychime.R
import com.privacyaccountofliu.openhourlychime.model.events.AudioConfigEvent
import com.privacyaccountofliu.openhourlychime.model.tools.LocaleHelper
import com.privacyaccountofliu.openhourlychime.model.tools.LogUtil
import com.privacyaccountofliu.openhourlychime.model.tools.Tools
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Locale


@Suppress("DEPRECATION")
class TimeService : Service(), TextToSpeech.OnInitListener {
    private val notificationId = 1001
    private val appContext: Context by lazy { applicationContext }
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var defaultAudioAttributes: AudioAttributes
    private var isTtsReady = false
    private var pendingSpeakRequest: String? = null
    private var timeRange: List<Int> = listOf(DEFAULT_START, DEFAULT_END)
    private var isNotice: Boolean = true
    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        LocaleHelper.applyServiceLanguage(appContext)
        initTTS()
        EventBus.getDefault().register(this)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val soundPreferencesOpi =
            sharedPreferences.getString("sound_preference", "media_sound_control")
        val timeRangePreferencesOpi =
            sharedPreferences.getString("time_range_preference", "$DEFAULT_START-$DEFAULT_END")
        isNotice = sharedPreferences.getBoolean("notifications_enabled", true)
        defaultAudioAttributes = Tools().yieldAudioAttr(soundPreferencesOpi)
        timeRange = Tools().timeSplit(timeRangePreferencesOpi!!)
        startForeground(notificationId, createNotification())
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            LogUtil.w("Language", appContext.resources.configuration.locale.language)
            val result = when (appContext.resources.configuration.locale.language) {
                "zh" -> textToSpeech.setLanguage(Locale.CHINA)
                "en" -> textToSpeech.setLanguage(Locale.US)
                else -> textToSpeech.setLanguage(Locale.CHINA)
            }
            isTtsReady = if (result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                LogUtil.d("TTS", "引擎初始化成功")
                textToSpeech.setAudioAttributes(defaultAudioAttributes)
                true
            } else {
                LogUtil.e("TTS", "TTS缺失数据")
                false
            }

            pendingSpeakRequest?.let { text ->
                speak(text)
                pendingSpeakRequest = null
            }
        } else {
            LogUtil.e("TTS", "语音引擎初始化失败: $status")
            isTtsReady = false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateLanguage()
        when (intent?.action) {
            "ACTION_HOURLY_CHIME" -> handleHourlyChime()
            "ACTION_TEST_CHIME" -> handleTestChime()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAudioConfig(event: AudioConfigEvent) {
        textToSpeech.setAudioAttributes(event.attributes)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTimeRangeConfig(event: List<Int>) {
        timeRange = event
        DEFAULT_START = event[0]
        DEFAULT_END = event[1]
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onIsNoticeConfig(tag: Boolean) {
        isNotice = tag
    }

    private fun updateLanguage() {
        LocaleHelper.applyServiceLanguage(appContext)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "time_service_channel_open_hourly_chime")
            .setContentTitle(appContext.getString(R.string.notice_1))
            .setContentText(appContext.getString(R.string.notice_2))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(if (isNotice) NotificationCompat.VISIBILITY_PUBLIC else NotificationCompat.VISIBILITY_PRIVATE)
            .build()
    }

    private fun initTTS() {
        textToSpeech = TextToSpeech(this, this)
        textToSpeech.apply {
            setSpeechRate(0.5f)
            setPitch(1.0f)
        }
    }

    private fun handleHourlyChime() {
        val now = Calendar.getInstance()
        val defaultZoneId = TimeZone.getDefault().displayName
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        if (timeRange[0] <= hour * 60 + minute && hour * 60 + minute <= timeRange[1]) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val chimeMode = prefs.getString("chime_mode_preference", "tts") ?: "tts"

            if (chimeMode == "custom_audio") {
                val soundPref = prefs.getString("chime_sound_preference", "builtin_bell") ?: "builtin_bell"
                val systemUri = prefs.getString("chime_system_uri", null)
                playChimeSound(soundPref, systemUri)
                sendChimeNotification(getString(R.string.notice_3))
            } else {
                val timeText = buildChimeText(hour, minute, defaultZoneId, prefs)
                speak(timeText)
                sendChimeNotification(timeText)
            }
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun buildChimeText(hour: Int, minute: Int, defaultZoneId: String, prefs: android.content.SharedPreferences): String {
        val timeFormat = prefs.getString("time_format_preference", "24") ?: "24"

        if (timeFormat == "12") {
            val hour12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val ampm = if (hour < 12) "AM" else "PM"
            val locale = appContext.resources.configuration.locale.language
            val ampmZh = if (hour < 12) "上午" else "下午"
            val ampmDisplay = if (locale == "zh") ampmZh else ampm

            return when (hour) {
                0 -> appContext.getString(R.string.TTS_5, defaultZoneId, 12, ampmDisplay)
                12 -> appContext.getString(R.string.TTS_5, defaultZoneId, 12, ampmDisplay)
                else -> appContext.getString(R.string.TTS_5, defaultZoneId, hour12, ampmDisplay)
            }
        } else {
            return when (hour) {
                0 -> appContext.getString(R.string.TTS_1, defaultZoneId)
                12 -> appContext.getString(R.string.TTS_2, defaultZoneId)
                else -> appContext.getString(R.string.TTS_3, defaultZoneId, hour)
            }
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun handleTestChime() {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val defaultZoneId = TimeZone.getDefault().displayName
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val timeFormat = prefs.getString("time_format_preference", "24") ?: "24"

        LogUtil.d("TimeService", "$timeRange")

        val timeText = if (timeFormat == "12") {
            val hour12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val locale = appContext.resources.configuration.locale.language
            val ampm = if (hour < 12) "AM" else "PM"
            val ampmZh = if (hour < 12) "上午" else "下午"
            val ampmDisplay = if (locale == "zh") ampmZh else ampm
            appContext.getString(R.string.TTS_5, defaultZoneId, hour12, ampmDisplay)
        } else {
            appContext.getString(R.string.TTS_4, defaultZoneId, hour, minute)
        }
        speak(timeText)
        LogUtil.d("TimeRange", "时间范围: $timeRange")
    }

    private fun sendChimeNotification(timeText: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "alarm_channel_open_hourly_chime")
            .setContentTitle(getString(R.string.notice_3))
            .setContentText(timeText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1002, notification)
    }

    private fun speak(text: String) {
        if (isTtsReady) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            pendingSpeakRequest = text
        }
    }

    private fun playChimeSound(soundPref: String, systemUri: String?) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                if (soundPref == "system_picker" && systemUri != null) {
                    try {
                        setDataSource(this@TimeService, Uri.parse(systemUri))
                    } catch (e: Exception) {
                        LogUtil.e("TimeService", "Failed to load system ringtone, falling back to bell", e)
                        setBuiltinDataSource(this, "builtin_bell")
                    }
                } else {
                    setBuiltinDataSource(this, soundPref)
                }
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                prepare()
                start()
                setOnCompletionListener { mp -> mp.release() }
            }
        } catch (e: Exception) {
            LogUtil.e("TimeService", "Failed to play chime sound", e)
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun setBuiltinDataSource(mp: MediaPlayer, soundPref: String) {
        val resId = when (soundPref) {
            "builtin_gong" -> R.raw.chime_gong
            "builtin_chime" -> R.raw.chime_chime
            else -> R.raw.chime_bell
        }
        try {
            val afd: AssetFileDescriptor = resources.openRawResourceFd(resId)
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
        } catch (e: Exception) {
            LogUtil.e("TimeService", "Failed to load built-in sound, trying bell", e)
            val afd: AssetFileDescriptor = resources.openRawResourceFd(R.raw.chime_bell)
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
        }
    }

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, TimeService::class.java)
            context.startForegroundService(intent)
        }

        @SuppressLint("ImplicitSamInstance")
        fun stopService(context: Context) {
            context.stopService(Intent(context, TimeService::class.java))
        }

        private var DEFAULT_START = 420
        private var DEFAULT_END = 1320
    }
}
