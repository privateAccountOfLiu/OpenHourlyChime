package com.privacyaccountofliu.openhourlychime.model.tools

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.privacyaccountofliu.openhourlychime.model.services.KeepAliveJobService
import com.privacyaccountofliu.openhourlychime.model.services.TimeService
import java.text.SimpleDateFormat
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val wakelock = acquireWakeLock(context)
        try {
            if (intent != null && intent.action == Intent.ACTION_BOOT_COMPLETED) {
                LogUtil.d("BootReceiver", "Device boot completed, rescheduling")
                KeepAliveJobService.scheduleJob(context)
                setNextAlarm(context)
                return
            }
            startTimeService(context)
            setNextAlarm(context)
        } finally {
            wakelock?.let {
                if (it.isHeld) it.release()
            }
        }
    }

    private fun acquireWakeLock(context: Context): PowerManager.WakeLock? {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "OpenHourlyChime:AlarmWakeLock"
            )
            wl.acquire(10 * 1000L) // 10 second timeout
            wl
        } catch (e: Exception) {
            LogUtil.e("Alarm", "Failed to acquire wakelock", e)
            null
        }
    }

    private fun startTimeService(context: Context) {
        val serviceIntent = Intent(context, TimeService::class.java).apply {
            action = "ACTION_HOURLY_CHIME"
        }
        try {
            context.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            LogUtil.e("Alarm", "Failed to start TimeService", e)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun setNextAlarm(context: Context) {
        val nextAlarm = calculateNextAlarmTime()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createAlarmPendingIntent(context)

        try {
            // Use setAlarmClock for maximum priority - wakes device even in Doze mode
            val alarmInfo = AlarmManager.AlarmClockInfo(nextAlarm.timeInMillis, pendingIntent)
            alarmManager.setAlarmClock(alarmInfo, pendingIntent)
            LogUtil.d("Alarm", "Next alarm set: ${SimpleDateFormat("HH:mm").format(nextAlarm.time)}")
        } catch (e: SecurityException) {
            LogUtil.e("Alarm", "Security exception setting alarm, falling back", e)
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextAlarm.timeInMillis,
                    pendingIntent
                )
            } catch (e2: Exception) {
                LogUtil.e("Alarm", "Fallback alarm also failed", e2)
            }
        } catch (e: Exception) {
            LogUtil.e("Alarm", "Failed to set next alarm", e)
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
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
