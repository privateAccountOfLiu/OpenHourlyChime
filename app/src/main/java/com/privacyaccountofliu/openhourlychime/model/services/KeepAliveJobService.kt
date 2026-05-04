package com.privacyaccountofliu.openhourlychime.model.services

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.privacyaccountofliu.openhourlychime.model.tools.AlarmReceiver
import com.privacyaccountofliu.openhourlychime.model.tools.LogUtil
import java.util.Calendar

@SuppressLint("SpecifyJobSchedulerIdRange")
class KeepAliveJobService : JobService() {

    companion object {

        fun scheduleJob(context: Context) {
            val jobScheduler = context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler

            // Check if already scheduled
            val pendingJobs = jobScheduler.allPendingJobs
            if (pendingJobs.any { it.id == JobIds.KEEP_ALIVE_JOB }) return

            val jobInfo = JobInfo.Builder(
                JobIds.KEEP_ALIVE_JOB,
                ComponentName(context, KeepAliveJobService::class.java)
            )
                .setPersisted(true)
                .setMinimumLatency(5 * 60 * 1000L)
                .setOverrideDeadline(10 * 60 * 1000L)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .setRequiresBatteryNotLow(false)
                .setRequiresStorageNotLow(false)
                .build()
            jobScheduler.schedule(jobInfo)
            LogUtil.d("KeepAlive", "Keep-alive job scheduled")
        }
    }

    override fun onStartJob(params: JobParameters): Boolean {
        LogUtil.d("KeepAlive", "Job started - restarting critical services")
        restartCriticalServices()
        scheduleJob(this)
        jobFinished(params, false)
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        LogUtil.d("KeepAlive", "Job stopped - will reschedule")
        scheduleJob(this)
        return true // Reschedule if stopped
    }

    private fun restartCriticalServices() {
        // Restart TimeService if needed
        val serviceIntent = Intent(this, TimeService::class.java)
        try {
            startForegroundService(serviceIntent)
            LogUtil.d("KeepAlive", "TimeService restarted")
        } catch (e: Exception) {
            LogUtil.e("KeepAlive", "Failed to start TimeService", e)
        }

        // Reschedule the next hourly alarm if needed
        rescheduleAlarmIfNeeded()
    }

    private fun rescheduleAlarmIfNeeded() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = PendingIntent.getBroadcast(
                this, 0, Intent(this, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent == null) {
                // Alarm was cancelled, reschedule it
                LogUtil.d("KeepAlive", "Alarm missing, rescheduling")
                val nextAlarm = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.HOUR_OF_DAY, 1)
                }
                val intent = Intent(this, AlarmReceiver::class.java)
                val newPi = PendingIntent.getBroadcast(
                    this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmInfo = AlarmManager.AlarmClockInfo(nextAlarm.timeInMillis, newPi)
                alarmManager.setAlarmClock(alarmInfo, newPi)
                LogUtil.d("KeepAlive", "Alarm rescheduled for ${nextAlarm.time}")
            }
        } catch (e: Exception) {
            LogUtil.e("KeepAlive", "Failed to reschedule alarm", e)
        }
    }

    object JobIds {
        const val KEEP_ALIVE_JOB = 1001
    }
}
