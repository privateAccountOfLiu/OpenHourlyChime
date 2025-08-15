package com.privacyaccountofliu.openhourlychime.model

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.privacyaccountofliu.openhourlychime.model.services.KeepAliveJobService
import com.privacyaccountofliu.openhourlychime.model.services.TimeService
import java.text.SimpleDateFormat
import java.util.Calendar

// AlarmReceiver.kt
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null) {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                Log.d("BootReceiver", "Device boot completed, rescheduling jobs")
                KeepAliveJobService.scheduleJob(context)
            }
        }
        startTimeService(context)
        setNextAlarm(context)
    }

    private fun startTimeService(context: Context) {
        val serviceIntent = Intent(context, TimeService::class.java).apply {
            action = "ACTION_HOURLY_CHIME"
        }

        context.startForegroundService(serviceIntent)
    }

    @SuppressLint("SimpleDateFormat")
    private fun setNextAlarm(context: Context) {
        val nextAlarm = calculateNextAlarmTime()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createAlarmPendingIntent(context)

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextAlarm.timeInMillis,
                pendingIntent
            )
            Log.d("Alarm", "下一个整点闹钟已设置: ${SimpleDateFormat("HH:mm").format(nextAlarm.time)}")
        } catch (e: SecurityException) {
            Log.e("Alarm", "设置下一个闹钟时权限错误", e)
        }
    }

    private fun calculateNextAlarmTime(): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.HOUR_OF_DAY, 1)
        }
    }

    private fun createAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}