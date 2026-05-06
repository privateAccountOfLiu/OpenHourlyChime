package com.privacyaccountofliu.openhourlychime.model.events

data class ChimeConfigEvent(
    val mode: String,
    val sound: String,
    val systemUri: String?
)
