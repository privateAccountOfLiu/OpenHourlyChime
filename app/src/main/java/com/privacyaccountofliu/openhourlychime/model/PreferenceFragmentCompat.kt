package com.privacyaccountofliu.openhourlychime.model

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.privacyaccountofliu.openhourlychime.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // 加载你在 XML 中定义的 preferences
        setPreferencesFromResource(R.xml.settings_preference, rootKey)

        // 你可以在这里通过 findPreference<PreferenceType>("your_key") 来获取并操作特定的 Preference
        // 例如，为某个 Preference 设置点击事件监听器
        /*
        val aboutPreference = findPreference<Preference>("about_app")
        aboutPreference?.setOnPreferenceClickListener {
            // 处理点击事件，例如打开一个 "关于" 界面
            Toast.makeText(context, "打开关于界面...", Toast.SHORT).show()
            true // 表示事件已处理
        }
        */
    }
}