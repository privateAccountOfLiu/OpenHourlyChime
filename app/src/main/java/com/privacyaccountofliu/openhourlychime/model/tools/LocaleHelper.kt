@file:Suppress("DEPRECATION")

package com.privacyaccountofliu.openhourlychime.model.tools

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import java.util.Locale

object LocaleHelper {
    private const val SELECTED_LANGUAGE = "language_preference"

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
        return preferences.getString(SELECTED_LANGUAGE, "Chinese") ?: "Chinese"
    }

    fun localeForLanguage(language: String): Locale = when (language) {
        "English" -> Locale.ENGLISH
        else -> Locale("zh", "CN")
    }

    private fun updateResources(context: Context, language: String): Context {
        val locale = localeForLanguage(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun applyServiceLanguage(context: Context): Context {
        val language = getLanguage(context)
        return updateResources(context, language)
    }
}
