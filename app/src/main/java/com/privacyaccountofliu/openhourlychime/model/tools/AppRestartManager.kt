package com.privacyaccountofliu.openhourlychime.model.tools

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

object AppRestartManager {
    private const val RESTART_DELAY = 300L

    fun restartApp(context: Context, delay: Long = RESTART_DELAY) {
        Handler(Looper.getMainLooper()).postDelayed({
            doRestart(context.applicationContext)
        }, delay)
    }

    private fun doRestart(context: Context) {
        try {
            context.restartApp()
        } catch (e: Exception) {
            Log.w("AppRestart", "Intent restart failed, trying AlarmManager")
            context.restartAppWithAlarmManager()
        }
    }

    fun Context.restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("IS_RESTART", true)
        }
        startActivity(intent)
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    fun Context.restartAppWithAlarmManager() {
        val restartIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            restartIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent)
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}