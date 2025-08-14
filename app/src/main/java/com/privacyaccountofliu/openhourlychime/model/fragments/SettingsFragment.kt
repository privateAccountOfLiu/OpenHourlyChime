package com.privacyaccountofliu.openhourlychime.model.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.privacyaccountofliu.openhourlychime.R
import com.privacyaccountofliu.openhourlychime.model.PreferenceActionListener
import com.privacyaccountofliu.openhourlychime.model.ToastUtil
import com.privacyaccountofliu.openhourlychime.widget.TimeRangePreference

@Suppress("DEPRECATION")
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
        val timeRangePreference = findPreference<TimeRangePreference>("time_range_preference")

        testBroadCastPreference?.setOnPreferenceClickListener {
            listener?.onPreferenceClicked("test_broad_cast_preference")
            Toast.makeText(context, "正在进行测试播报...", Toast.LENGTH_SHORT).show()
            true
        }

        soundPreference?.setOnPreferenceChangeListener { _, new ->
            listener?.onPreferenceChanged("sound_preference", new)
            context?.let { ToastUtil.showToast(it, "更改成功") }
            true
        }

        aboutPreference?.setOnPreferenceClickListener {
            listener?.onPreferenceClicked("about_preference")
            true
        }

        timeRangePreference?.onTimeRangeChangeListener = object : TimeRangePreference.OnTimeRangeChangeListener {
            override fun onTimeRangeChanged(startMinutes: Int, endMinutes: Int) {
                handleTimeRangeChange(startMinutes, endMinutes)
                listener?.onPreferenceChanged("time_range_preference", listOf(startMinutes, endMinutes))
                context?.let { ToastUtil.showToast(it, "更改成功") }
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is TimeRangePreference) {
            val fragment = TimeRangePreferenceFragment.newInstance(preference.key)
            fragment.setTargetFragment(this, 0)
            fragment.show(parentFragmentManager, "TimeRangePreferenceFragment")
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun handleTimeRangeChange(start: Int, end: Int){
        val startTime = String.format("%02d:%02d", start / 60, start % 60)
        val endTime = String.format("%02d:%02d", end / 60, end % 60)
        Log.d("TimeRange", "时间范围已更新: $startTime - $endTime")
    }
}