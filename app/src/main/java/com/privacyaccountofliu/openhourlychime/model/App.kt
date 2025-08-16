package com.privacyaccountofliu.openhourlychime.model

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.privacyaccountofliu.openhourlychime.R
import com.privacyaccountofliu.openhourlychime.model.services.KeepAliveJobService
import com.privacyaccountofliu.openhourlychime.model.tools.LocaleHelper

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        setupLocale()
        createNotificationChannels()
        KeepAliveJobService.scheduleJob(this)
    }

    private fun createNotificationChannels() {
        // 创建通知渠道
        val serviceChannel = NotificationChannel(
            "time_service_channel_open_hourly_chime",
            getString(R.string.channel_1),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_2)
        }

        val alarmChannel = NotificationChannel(
            "alarm_channel_open_hourly_chime",
            getString(R.string.channel_3),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_4)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.apply {
            createNotificationChannel(serviceChannel)
            createNotificationChannel(alarmChannel)
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(base, LocaleHelper.getLanguage(base)))
    }

    private fun setupLocale() {
        val language = LocaleHelper.getLanguage(this)
        LocaleHelper.setLocale(this, language)
    }
}