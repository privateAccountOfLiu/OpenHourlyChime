package com.privacyaccountofliu.openhourlychime.model.tools

import android.media.AudioAttributes
import android.media.AudioManager

class Tools {
    fun yieldAudioAttr(soundPreferencesOpi: String? = "media_sound_control"): AudioAttributes {
        var usageType = AudioAttributes.USAGE_MEDIA
        var streamType = AudioManager.STREAM_MUSIC
        when(soundPreferencesOpi) {
            "media_sound_control" -> {
                usageType = AudioAttributes.USAGE_MEDIA
                streamType = AudioManager.STREAM_MUSIC
            }
            "notification_sound_control" -> {
                usageType = AudioAttributes.USAGE_NOTIFICATION
                streamType = AudioManager.STREAM_NOTIFICATION
            }
            "alarm_sound_control" -> {
                usageType = AudioAttributes.USAGE_ALARM
                streamType = AudioManager.STREAM_ALARM
            }
        }
        return AudioAttributes.Builder()
            .setUsage(usageType)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setLegacyStreamType(streamType)
            .build()
    }

    fun timeSplit(timeS: String): List<Int> {
        val parts = timeS.split("-")
        if (parts.size != 2) return listOf(DEFAULT_START, DEFAULT_END)
        val start = parts[0].toIntOrNull() ?: DEFAULT_START
        val end = parts[1].toIntOrNull() ?: DEFAULT_END
        return listOf(start, end)
    }

    companion object {
        private const val DEFAULT_START = 420
        private const val DEFAULT_END = 1320
    }
}