package com.privacyaccountofliu.openhourlychime.model

interface PreferenceActionListener {
    fun onPreferenceClicked(key: String)
    fun onPreferenceChanged(key: String, newValue: Any)
}