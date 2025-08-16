package com.privacyaccountofliu.openhourlychime.model.services

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.privacyaccountofliu.openhourlychime.model.tools.AlarmReceiver

@SuppressLint("SpecifyJobSchedulerIdRange")
class KeepAliveJobService : JobService() {

    companion object {

        fun scheduleJob(context: Context) {
            val jobScheduler = context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler

            // 检查任务是否已存在
            val pendingJobs = jobScheduler.allPendingJobs
            if (pendingJobs.any { it.id == JobIds.KEEP_ALIVE_JOB }) {
                return
            }

            val jobInfo = JobInfo.Builder(
                JobIds.KEEP_ALIVE_JOB,
                ComponentName(context, KeepAliveJobService::class.java))
                    .setPersisted(true)
                    .setMinimumLatency(1 * 60 * 1000)
                    .setOverrideDeadline(3 * 60 * 1000)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false)
                    .build()
            jobScheduler.schedule(jobInfo)
        }

    }

    override fun onStartJob(params: JobParameters): Boolean {
        restartCriticalServices()
        scheduleJob(this)
        jobFinished(params, false)
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }

    private fun restartCriticalServices() {
        startAlarmReceiverService()
        startTimeService()
    }

    private fun startAlarmReceiverService() {
        val intent = Intent(this, AlarmReceiver::class.java)
        startForegroundService(intent)
    }

    private fun startTimeService() {
        val intent = Intent(this, TimeService::class.java)
        startForegroundService(intent)
    }


    object JobIds {
        const val KEEP_ALIVE_JOB = 1001
    }
}