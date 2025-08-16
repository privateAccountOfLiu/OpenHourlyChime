@file:Suppress("DEPRECATION")

package com.privacyaccountofliu.openhourlychime.model.tools

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.preference.PreferenceManager
import androidx.core.content.edit
import java.util.Locale

object LocaleHelper {
    private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"

    fun setLocale(context: Context, language: String): Context {
        persist(context, language)
        return updateResources(context, language)
    }

    private fun persist(context: Context, language: String) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit { putString(SELECTED_LANGUAGE, language) }
    }

    fun getLanguage(context: Context): String {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString(SELECTED_LANGUAGE, "system") ?: "system"
    }

    private fun updateResources(context: Context, language: String): Context {
        val locale = when (language) {
            "system" -> Resources.getSystem().configuration.locales[0]
            "zh" -> Locale("zh", "CN")
            else -> Locale.ENGLISH
        }

        Locale.setDefault(locale)

        val res = context.resources
        val config = Configuration(res.configuration)

        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}