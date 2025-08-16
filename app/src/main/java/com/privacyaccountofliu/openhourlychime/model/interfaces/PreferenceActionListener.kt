package com.privacyaccountofliu.openhourlychime.model.interfaces

interface PreferenceActionListener {
    fun onPreferenceClicked(key: String)
    fun onPreferenceChanged(key: String, newValue: Any)
}