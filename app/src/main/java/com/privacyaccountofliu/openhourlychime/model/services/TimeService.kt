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
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.privacyaccountofliu.openhourlychime.MainActivity
import com.privacyaccountofliu.openhourlychime.R
import com.privacyaccountofliu.openhourlychime.model.events.AudioConfigEvent
import com.privacyaccountofliu.openhourlychime.model.events.ChimeConfigEvent
import com.privacyaccountofliu.openhourlychime.model.events.NoticeEnabledEvent
import com.privacyaccountofliu.openhourlychime.model.events.TimeRangeEvent
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
    private lateinit var audioManager: AudioManager
    private var isTtsReady = false
    private var pendingSpeakRequests = ArrayDeque<String>()
    private var timeRange: List<Int> = listOf(420, 1320)
    private var isNotice: Boolean = true
    private var mediaPlayer: MediaPlayer? = null
    private var chimeMode: String = "tts"
    private var chimeSound: String = "builtin_bell"
    private var chimeSystemUri: String? = null
    private var utteranceIdCounter = 0
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        LocaleHelper.applyServiceLanguage(appContext)
        initTTS()
        EventBus.getDefault().register(this)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val soundPref = prefs.getString("sound_preference", "media_sound_control")
        val timeRangeStr = prefs.getString("time_range_preference", "420-1320")
        isNotice = prefs.getBoolean("notifications_enabled", true)
        chimeMode = prefs.getString("chime_mode_preference", "tts") ?: "tts"
        chimeSound = prefs.getString("chime_sound_preference", "builtin_bell") ?: "builtin_bell"
        chimeSystemUri = prefs.getString("chime_system_uri", null)
        defaultAudioAttributes = Tools().yieldAudioAttr(soundPref)
        timeRange = Tools().timeSplit(timeRangeStr!!)
        startForeground(notificationId, createNotification())
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val userLang = LocaleHelper.getLanguage(appContext)
            LogUtil.w("Language", "User preference: $userLang")

            val preferredLocale = LocaleHelper.localeForLanguage(userLang)
            val altLocale = LocaleHelper.localeForLanguage(
                if (userLang == "Chinese") "English" else "Chinese"
            )

            var result = textToSpeech.setLanguage(preferredLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                LogUtil.w("TTS", "Preferred language $preferredLocale not available, trying alt $altLocale")
                result = textToSpeech.setLanguage(altLocale)
            }
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                LogUtil.w("TTS", "Alt language failed, trying system default")
                result = textToSpeech.setLanguage(Locale.getDefault())
            }
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                LogUtil.w("TTS", "System default failed, trying en-US")
                result = textToSpeech.setLanguage(Locale.US)
            }

            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                LogUtil.d("TTS", "TTS initialized with ${textToSpeech.language}")
                textToSpeech.setAudioAttributes(defaultAudioAttributes)
                textToSpeech.setOnUtteranceProgressListener(utteranceListener)
                isTtsReady = true
            } else {
                LogUtil.e("TTS", "All languages failed, TTS unavailable")
                isTtsReady = false
            }

            flushPendingQueue()
        } else {
            LogUtil.e("TTS", "TTS engine init failed: $status")
            isTtsReady = false
        }
    }

    private fun flushPendingQueue() {
        while (pendingSpeakRequests.isNotEmpty()) {
            val text = pendingSpeakRequests.removeFirst()
            speak(text)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_HOURLY_CHIME" -> handleHourlyChime()
            "ACTION_TEST_CHIME" -> handleTestChime()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        if (audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
        }
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
    fun onTimeRangeConfig(event: TimeRangeEvent) {
        timeRange = event.data
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onIsNoticeConfig(event: NoticeEnabledEvent) {
        isNotice = event.enabled
        startForeground(notificationId, createNotification())
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChimeConfig(event: ChimeConfigEvent) {
        chimeMode = event.mode
        chimeSound = event.sound
        chimeSystemUri = event.systemUri
        LogUtil.d("TimeService", "Chime config updated: mode=$chimeMode sound=$chimeSound")
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, "time_service_channel_open_hourly_chime")
            .setContentTitle(getString(R.string.notice_1))
            .setContentText(getString(R.string.notice_2))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(if (isNotice) NotificationCompat.VISIBILITY_PUBLIC else NotificationCompat.VISIBILITY_PRIVATE)
            .build()
    }

    private fun initTTS() {
        textToSpeech = TextToSpeech(this, this)
        textToSpeech.setSpeechRate(0.5f)
        textToSpeech.setPitch(1.0f)
    }

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            LogUtil.d("TTS", "Utterance started: $utteranceId")
        }

        override fun onDone(utteranceId: String?) {
            LogUtil.d("TTS", "Utterance done: $utteranceId")
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
                audioFocusRequest = null
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            LogUtil.e("TTS", "Utterance error: $utteranceId")
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
                audioFocusRequest = null
            }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            LogUtil.e("TTS", "Utterance error: $utteranceId, code=$errorCode")
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
                audioFocusRequest = null
            }
        }
    }

    private fun handleHourlyChime() {
        val now = Calendar.getInstance()
        val defaultZoneId = TimeZone.getDefault().displayName
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        if (timeRange[0] <= hour * 60 + minute && hour * 60 + minute <= timeRange[1]) {
            if (chimeMode == "custom_audio") {
                playChimeSound(chimeSound, chimeSystemUri)
                sendChimeNotification(getString(R.string.notice_3))
            } else {
                val prefs = PreferenceManager.getDefaultSharedPreferences(this)
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
            val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val locale = appContext.resources.configuration.locale.language
            val ampm = if (hour < 12) "AM" else "PM"
            val ampmDisplay = if (locale == "zh") (if (hour < 12) "上午" else "下午") else ampm
            return appContext.getString(R.string.TTS_5, defaultZoneId, hour12, ampmDisplay)
        }
        return when (hour) {
            0 -> appContext.getString(R.string.TTS_1, defaultZoneId)
            12 -> appContext.getString(R.string.TTS_2, defaultZoneId)
            else -> appContext.getString(R.string.TTS_3, defaultZoneId, hour)
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
        LogUtil.d("TimeService", "Test chime: timeRange=$timeRange chimeMode=$chimeMode")

        if (chimeMode == "custom_audio") {
            playChimeSound(chimeSound, chimeSystemUri)
            sendChimeNotification(getString(R.string.notice_3))
            return
        }

        val timeText = if (timeFormat == "12") {
            val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val locale = appContext.resources.configuration.locale.language
            val ampm = if (hour < 12) "AM" else "PM"
            val ampmDisplay = if (locale == "zh") (if (hour < 12) "上午" else "下午") else ampm
            appContext.getString(R.string.TTS_5, defaultZoneId, hour12, ampmDisplay)
        } else {
            appContext.getString(R.string.TTS_4, defaultZoneId, hour, minute)
        }
        speak(timeText)
    }

    private fun sendChimeNotification(timeText: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, "alarm_channel_open_hourly_chime")
            .setContentTitle(getString(R.string.notice_3))
            .setContentText(timeText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(1002, n)
    }

    private fun speak(text: String) {
        if (!isTtsReady) {
            pendingSpeakRequests.addLast(text)
            return
        }
        // Request audio focus
        try {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(defaultAudioAttributes)
                .setWillPauseWhenDucked(false)
                .build()
            val result = audioManager.requestAudioFocus(focusRequest)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocusRequest = focusRequest
            } else {
                LogUtil.w("TTS", "Audio focus request denied: $result")
            }
        } catch (e: Exception) {
            LogUtil.e("TTS", "Audio focus request failed", e)
        }

        val utteranceId = "chime_" + (++utteranceIdCounter)
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun playChimeSound(soundPref: String, systemUri: String?) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                if (soundPref == "system_picker" && systemUri != null) {
                    try {
                        setDataSource(this@TimeService, Uri.parse(systemUri))
                    } catch (e: Exception) {
                        LogUtil.e("TimeService", "Failed to load system ringtone", e)
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
            LogUtil.e("TimeService", "Failed to load built-in sound", e)
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
    }
}
