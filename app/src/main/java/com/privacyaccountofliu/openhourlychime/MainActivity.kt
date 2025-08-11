package com.privacyaccountofliu.openhourlychime

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.privacyaccountofliu.openhourlychime.databinding.ActivityMainBinding
import com.privacyaccountofliu.openhourlychime.model.AlarmReceiver
import com.privacyaccountofliu.openhourlychime.model.AudioConfigEvent
import com.privacyaccountofliu.openhourlychime.model.TimeService
import com.privacyaccountofliu.openhourlychime.model.Tools
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.Calendar

class MainActivity : BaseActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    private var soundPreferencesOpi: String = "media_sound_control"
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，启动服务
            TimeService.startService(this)
        } else {
            Toast.makeText(this, "需要通知权限才能显示后台运行状态", Toast.LENGTH_LONG).show()
        }
    }

    override fun getLayoutResId() = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化组件
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        pendingIntent = createAlarmPendingIntent()
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navView.setCheckedItem(R.id.nav_home)
        setupToolbar()
        setupNavigation()


        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu) // 使用菜单图标
        }

        // 设置按钮监听器
        binding.btnStart.setOnClickListener {
            startAlarmService()
        }

        binding.btnStop.setOnClickListener {
            stopAlarmService()
        }

        binding.btnDebug.setOnClickListener {
            triggerTestChime()
        }

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "sound_preference" -> {
                soundPreferencesOpi = sharedPreferences?.getString(key, "media_sound_control").toString()
            }
        }
        updateTtsConfig(soundPreferencesOpi)
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        // 每次返回时更新选中状态
        navView.setCheckedItem(R.id.nav_home)
    }

    private fun createAlarmPendingIntent(): PendingIntent {
        val intent = Intent(this, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun startAlarmService() {
        startAlarm()
        startServiceWithPermissionCheck()
        Toast.makeText(this, "整点报时服务已启动", Toast.LENGTH_SHORT).show()
    }

    private fun startServiceWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                TimeService.startService(this)
            }
        } else {
            TimeService.startService(this)
        }
    }

    private fun stopAlarmService() {
        alarmManager.cancel(pendingIntent)
        TimeService.stopService(this)
        Toast.makeText(this, "整点报时服务已停止", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SimpleDateFormat")
    private fun startAlarm() {
        val calendar = calculateNextAlarmTime()

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("Alarm", "整点闹钟已设置: ${SimpleDateFormat("HH:mm").format(calendar.time)}")
        } catch (e: SecurityException) {
            Log.e("Alarm", "设置闹钟时权限错误", e)
        }
    }

    private fun calculateNextAlarmTime(): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.HOUR_OF_DAY, 1) // 下一个整点
        }
    }

    private fun triggerTestChime() {
        TimeService.startService(this)
        val intent = Intent(this, TimeService::class.java).apply {
            action = "ACTION_TEST_CHIME"
        }
        startService(intent)
    }

    private fun updateTtsConfig(soundPreferencesOpi: String?) {
        val attributes = Tools().yieldAudioAttr(soundPreferencesOpi)
        EventBus.getDefault().post(AudioConfigEvent(attributes))
    }
}