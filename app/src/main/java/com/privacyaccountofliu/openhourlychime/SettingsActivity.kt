package com.privacyaccountofliu.openhourlychime

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import com.privacyaccountofliu.openhourlychime.databinding.ActivitySettingsBinding
import com.privacyaccountofliu.openhourlychime.model.events.AudioConfigEvent
import com.privacyaccountofliu.openhourlychime.model.fragments.SettingsFragment
import com.privacyaccountofliu.openhourlychime.model.interfaces.PreferenceActionListener
import com.privacyaccountofliu.openhourlychime.model.services.TimeService
import com.privacyaccountofliu.openhourlychime.model.tools.AppRestartManager.restartApp
import com.privacyaccountofliu.openhourlychime.model.tools.LocaleHelper
import com.privacyaccountofliu.openhourlychime.model.tools.Tools
import org.greenrobot.eventbus.EventBus

@Suppress("DEPRECATION")
class SettingsActivity : BaseActivity(), PreferenceActionListener {

    private lateinit var binding: ActivitySettingsBinding
    private var soundPreferencesOpi: String = "media_sound_control"

    override fun getLayoutResId() = R.layout.activity_settings


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navView.setCheckedItem(R.id.nav_home)
        binding = ActivitySettingsBinding.inflate(layoutInflater)

        setupToolbar()
        setupNavigation()


        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        val fragment = SettingsFragment().apply {
            setPreferenceActionListener(this@SettingsActivity)
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, fragment)
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        navView.setCheckedItem(R.id.nav_settings)
    }

    override fun onPreferenceClicked(key: String) {
        when (key) {
            "test_broad_cast_preference" -> triggerTestChime()
            "about_preference" -> startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    override fun onPreferenceChanged(key: String, newValue: Any) {
        when (key) {
            "sound_preference" -> {
                soundPreferencesOpi = newValue.toString()
                updateTtsSoundConfig(soundPreferencesOpi)
            }
            "time_range_preference" -> {
                val timeRange = newValue as List<*>
                updateTimeRangeConfig(timeRange)
            }
            "notifications_enabled" -> {
                val isNotice = newValue as Boolean
                updateIsNoticeConfig(isNotice)
            }
            "language_preference" -> {
                saveLanguageSetting(newValue.toString())
                showRestartDialog()
            }
        }
    }

    private fun triggerTestChime() {
        TimeService.startService(this)
        val intent = Intent(this, TimeService::class.java).apply {
            action = "ACTION_TEST_CHIME"
        }
        startService(intent)
    }

    private fun updateIsNoticeConfig(isNotice: Boolean) {
        EventBus.getDefault().post(isNotice)
    }

    private fun updateTtsSoundConfig(soundPreferencesOpi: String?) {
        val attributes = Tools().yieldAudioAttr(soundPreferencesOpi)
        EventBus.getDefault().post(AudioConfigEvent(attributes))
    }

    private fun updateTimeRangeConfig(timeRange: List<*>) {
        EventBus.getDefault().post(timeRange)
    }

    private fun saveLanguageSetting(language: String) {
        LocaleHelper.setLocale(this, language)
    }

    private fun showRestartDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_need_restart_title))
            .setMessage(getString(R.string.dialog_need_restart_msg))
            .setPositiveButton(getString(R.string.Yes)) { _, _ ->
                restartApp()
            }
            .show()
    }

    private fun restartApp() {
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        applicationContext.restartApp()
    }
}