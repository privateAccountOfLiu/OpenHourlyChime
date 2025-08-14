package com.privacyaccountofliu.openhourlychime

import android.content.Intent
import android.os.Bundle
import com.privacyaccountofliu.openhourlychime.databinding.ActivitySettingsBinding
import com.privacyaccountofliu.openhourlychime.model.AudioConfigEvent
import com.privacyaccountofliu.openhourlychime.model.PreferenceActionListener
import com.privacyaccountofliu.openhourlychime.model.services.TimeService
import com.privacyaccountofliu.openhourlychime.model.Tools
import com.privacyaccountofliu.openhourlychime.model.fragments.SettingsFragment
import org.greenrobot.eventbus.EventBus

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
                val timeRange = newValue as List<Int>
                updateTimeRangeConfig(timeRange)
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

    private fun updateTtsSoundConfig(soundPreferencesOpi: String?) {
        val attributes = Tools().yieldAudioAttr(soundPreferencesOpi)
        EventBus.getDefault().post(AudioConfigEvent(attributes))
    }

    private fun updateTimeRangeConfig(timeRange: List<Int>) {
        EventBus.getDefault().post(timeRange)
    }
}