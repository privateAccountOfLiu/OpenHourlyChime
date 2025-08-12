package com.privacyaccountofliu.openhourlychime.model

import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.privacyaccountofliu.openhourlychime.R

class SettingsFragment : PreferenceFragmentCompat() {
    private var listener: PreferenceActionListener? = null

    fun setPreferenceActionListener(listener: PreferenceActionListener) {
        this.listener = listener
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preference, rootKey)

        val soundPreference = findPreference<Preference>("sound_preference")
        val testBroadCastPreference = findPreference<Preference>("test_broad_cast_preference")
        val aboutPreference = findPreference<Preference>("about_preference")

        testBroadCastPreference?.setOnPreferenceClickListener {
            listener?.onPreferenceClicked("test_broad_cast_preference")
            Toast.makeText(context, "正在进行测试播报...", Toast.LENGTH_SHORT).show()
            true
        }

        soundPreference?.setOnPreferenceChangeListener { _, new ->
            listener?.onPreferenceChanged("sound_preference", new)
            Toast.makeText(context, "更改成功", Toast.LENGTH_SHORT).show()
            true
        }

        aboutPreference?.setOnPreferenceClickListener {
            listener?.onPreferenceClicked("about_preference")
            true
        }
    }
}