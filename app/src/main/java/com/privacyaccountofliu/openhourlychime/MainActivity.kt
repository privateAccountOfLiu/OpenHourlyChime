package com.privacyaccountofliu.openhourlychime

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.privacyaccountofliu.openhourlychime.databinding.ActivityMainBinding
import com.privacyaccountofliu.openhourlychime.model.services.TimeService
import com.privacyaccountofliu.openhourlychime.model.tools.AlarmReceiver
import com.privacyaccountofliu.openhourlychime.model.tools.BatteryOptimizationHelper
import com.privacyaccountofliu.openhourlychime.model.tools.ToastUtil
import java.text.SimpleDateFormat
import java.util.Calendar

class MainActivity : BaseActivity(){
    private lateinit var binding: ActivityMainBinding
    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var batteryHelper: BatteryOptimizationHelper
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            ToastUtil.showToast(this, getString(R.string.toast_4))
        }
    }

    override fun getLayoutResId() = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        pendingIntent = createAlarmPendingIntent()
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navView.setCheckedItem(R.id.nav_home)
        setupToolbar()
        setupNavigation()


        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        batteryHelper = BatteryOptimizationHelper(this)
        checkBatteryOptimizationStatus()

        binding.switchOpenService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startAlarmService()
            } else {
                stopAlarmService()
            }
        }
    }

    override fun onResume() {
        super.onResume()
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
        Toast.makeText(this, getString(R.string.toast_5), Toast.LENGTH_SHORT).show()
    }

    private fun startServiceWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionList = listOf(Manifest.permission.POST_NOTIFICATIONS)
            for (permission in permissionList) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(permission)
                } else { if (ContextCompat.checkSelfPermission(
                        this,
                        permission )
                    == PackageManager.PERMISSION_DENIED) {
                    ToastUtil.showToast(this, getString(R.string.toast_4))
                }
                }
            }
        } else {
            Toast.makeText(this, getString(R.string.toast_6), Toast.LENGTH_SHORT).show()
        }
        TimeService.startService(this)
    }

    private fun stopAlarmService() {
        alarmManager.cancel(pendingIntent)
        TimeService.stopService(this)
        Toast.makeText(this, getString(R.string.toast_7), Toast.LENGTH_SHORT).show()
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

    private fun checkBatteryOptimizationStatus() {
        if (!batteryHelper.isIgnoringBatteryOptimizations()) {
            showWhitelistRequestDialog()
        }
    }

    private fun showWhitelistRequestDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_white_list_battery_title))
            .setMessage(getString(R.string.dialog_white_list_battery_msg))
            .setPositiveButton(getString(R.string.ToSet)) { _, _ ->
                batteryHelper.guideUserToBatteryWhitelist()
            }
            .setNegativeButton(getString(R.string.Cancel), null)
            .show()
    }
}