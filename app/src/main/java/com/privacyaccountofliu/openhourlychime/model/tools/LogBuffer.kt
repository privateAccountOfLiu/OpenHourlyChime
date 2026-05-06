package com.privacyaccountofliu.openhourlychime.model.tools

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
)

object LogBuffer {
    private const val MAX_ENTRIES = 500
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    @Synchronized
    fun record(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )
        val current = _entries.value.toMutableList()
        current.add(entry)
        if (current.size > MAX_ENTRIES) {
            current.removeAt(0)
        }
        _entries.value = current
    }

    @Synchronized
    fun clear() {
        _entries.value = emptyList()
    }
}
