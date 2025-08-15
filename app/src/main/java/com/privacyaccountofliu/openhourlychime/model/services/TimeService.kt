package com.privacyaccountofliu.openhourlychime.model.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.media.AudioAttributes
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.privacyaccountofliu.openhourlychime.MainActivity
import com.privacyaccountofliu.openhourlychime.R
import com.privacyaccountofliu.openhourlychime.model.AudioConfigEvent
import com.privacyaccountofliu.openhourlychime.model.Tools
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Locale


class TimeService : Service(), TextToSpeech.OnInitListener {
    private val notificationId = 1001
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var defaultAudioAttributes: AudioAttributes
    private var isTtsReady = false
    private var pendingSpeakRequest: String? = null
    private var timeRange: List<Int> = listOf(DEFAULT_START, DEFAULT_END)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        initTTS()
        startForeground(notificationId, createNotification())
        EventBus.getDefault().register(this)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val soundPreferencesOpi =
            sharedPreferences.getString("sound_preference", "media_sound_control")
        defaultAudioAttributes = Tools().yieldAudioAttr(soundPreferencesOpi)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.CHINA)
            isTtsReady = if (result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.d("TTS", "引擎初始化成功")
                textToSpeech.setAudioAttributes(defaultAudioAttributes)
                true
            } else {
                Log.e("TTS", "不支持中文语音")
                false
            }

            pendingSpeakRequest?.let { text ->
                speak(text)
                pendingSpeakRequest = null
            }
        } else {
            Log.e("TTS", "语音引擎初始化失败: $status")
            isTtsReady = false
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
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAudioConfig(event: AudioConfigEvent) {
        textToSpeech.setAudioAttributes(event.attributes)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTimeRangeConfig(event: List<Int>) {
        timeRange = event
        DEFAULT_START = event[0]
        DEFAULT_START = event[1]
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "time_service_channel_open_hourly_chime")
            .setContentTitle("整点报时服务运行中")
            .setContentText("将在每小时整点语音播报时间")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
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
            val timeText = when (hour) {
                0 -> "整点播报：现在是{$defaultZoneId} 午夜 12 点整"
                12 -> "整点播报：现在是{$defaultZoneId} 中午 12 点整"
                else -> "整点播报：现在是{$defaultZoneId} $hour 点整"
            }
            speak(timeText)
            sendChimeNotification(timeText)
        }
    }

    private fun handleTestChime() {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val defaultZoneId = TimeZone.getDefault().displayName
        Log.d("TimeService", "$timeRange")
        val timeText = "测试报时：现在是$defaultZoneId ${hour}点${minute}分"
        speak(timeText)
    }

    private fun sendChimeNotification(timeText: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "alarm_channel_open_hourly_chime")
            .setContentTitle("整点报时")
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
        private const val DEFAULT_END = 1320
    }
}