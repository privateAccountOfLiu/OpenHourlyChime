package com.privacyaccountofliu.openhourlychime.model

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.privacyaccountofliu.openhourlychime.model.services.KeepAliveJobService

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        KeepAliveJobService.scheduleJob(this)
    }

    private fun createNotificationChannels() {
        // 创建通知渠道
        val serviceChannel = NotificationChannel(
            "time_service_channel_open_hourly_chime",
            "整点报时服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "后台服务运行状态通知"
        }

        val alarmChannel = NotificationChannel(
            "alarm_channel_open_hourly_chime",
            "报时提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "整点报时提醒"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.apply {
            createNotificationChannel(serviceChannel)
            createNotificationChannel(alarmChannel)
        }
    }
}