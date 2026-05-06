package com.privacyaccountofliu.openhourlychime.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.privacyaccountofliu.openhourlychime.R

data class SettingsState(
    val notificationsEnabled: Boolean = true,
    val soundPreference: String = "media_sound_control",
    val timeRangeStart: Int = 420,
    val timeRangeEnd: Int = 1320,
    val language: String = "Chinese",
    val theme: String = "common_theme",
    val advancedLogging: Boolean = false,
    val timeFormat: String = "24",
    val chimeMode: String = "tts",
    val chimeSound: String = "builtin_bell",
    val chimeSystemUri: String? = null
)

private fun minutesToTime(minutes: Int): String {
    return String.format("%02d:%02d", minutes / 60, minutes % 60)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onNotificationChanged: (Boolean) -> Unit,
    onSoundChanged: (String) -> Unit,
    onTimeRangeChanged: (Int, Int) -> Unit,
    onTestChime: () -> Unit,
    onLanguageChanged: (String) -> Unit,
    onThemeChanged: (String) -> Unit,
    onAdvancedLoggingChanged: (Boolean) -> Unit,
    onTimeFormatChanged: (String) -> Unit,
    onChimeModeChanged: (String) -> Unit,
    onChimeSoundChanged: (String) -> Unit,
    onChimeSystemUriChanged: (Uri?) -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSoundDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimeFormatDialog by remember { mutableStateOf(false) }
    var showChimeModeDialog by remember { mutableStateOf(false) }
    var showChimeSoundDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // General Settings card
        SettingsCard(title = stringResource(R.string.set1)) {
            SwitchSetting(
                title = stringResource(R.string.set3),
                subtitle = stringResource(R.string.set2),
                checked = state.notificationsEnabled,
                onCheckedChange = onNotificationChanged
            )
            SettingDivider()
            ListSetting(
                title = stringResource(R.string.set4),
                subtitle = soundLabel(state.soundPreference, context),
                onClick = { showSoundDialog = true }
            )
            SettingDivider()
            ListSetting(
                title = stringResource(R.string.set15),
                subtitle = minutesToTime(state.timeRangeStart) + " - " + minutesToTime(state.timeRangeEnd),
                onClick = {
                    showTimeRangePicker(context, state.timeRangeStart, state.timeRangeEnd, onTimeRangeChanged)
                }
            )
            SettingDivider()
            Button(
                onClick = onTestChime,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.str3))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Advanced Settings card
        SettingsCard(title = stringResource(R.string.set6)) {
            ListSetting(
                title = stringResource(R.string.set16),
                subtitle = state.language,
                onClick = { showLanguageDialog = true }
            )
            SettingDivider()
            ListSetting(
                title = stringResource(R.string.set7),
                subtitle = if (state.theme == "night_theme")
                    stringResource(R.string.night_theme) else stringResource(R.string.common_theme),
                onClick = { showThemeDialog = true }
            )
            SettingDivider()
            SwitchSetting(
                title = stringResource(R.string.set10),
                subtitle = if (state.advancedLogging) stringResource(R.string.set9) else stringResource(R.string.set8),
                checked = state.advancedLogging,
                onCheckedChange = onAdvancedLoggingChanged
            )
            SettingDivider()
            ListSetting(
                title = stringResource(R.string.set17),
                subtitle = if (state.timeFormat == "12")
                    stringResource(R.string.format_12h) else stringResource(R.string.format_24h),
                onClick = { showTimeFormatDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chime Sound card
        SettingsCard(title = stringResource(R.string.set18)) {
            ListSetting(
                title = stringResource(R.string.set18),
                subtitle = if (state.chimeMode == "tts")
                    stringResource(R.string.chime_mode_tts) else stringResource(R.string.chime_mode_custom),
                onClick = { showChimeModeDialog = true }
            )
            if (state.chimeMode == "custom_audio") {
                SettingDivider()
                ListSetting(
                    title = stringResource(R.string.set19),
                    subtitle = chimeLabel(state.chimeSound, context),
                    onClick = { showChimeSoundDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // About
        Button(
            onClick = onAboutClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(stringResource(R.string.set12))
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Dialogs
    if (showSoundDialog) {
        RadioListDialog(
            title = stringResource(R.string.set4),
            options = listOf("media_sound_control", "notification_sound_control", "alarm_sound_control"),
            labels = listOf(
                stringResource(R.string.array_media_player),
                stringResource(R.string.array_notification_player),
                stringResource(R.string.array_alarm_player)
            ),
            selected = state.soundPreference,
            onSelect = { onSoundChanged(it); showSoundDialog = false },
            onDismiss = { showSoundDialog = false }
        )
    }
    if (showLanguageDialog) RadioListDialog(
        title = stringResource(R.string.set16),
        options = listOf("Chinese", "English"),
        labels = listOf(stringResource(R.string.Chinese), stringResource(R.string.English)),
        selected = state.language,
        onSelect = { onLanguageChanged(it); showLanguageDialog = false },
        onDismiss = { showLanguageDialog = false }
    )
    if (showThemeDialog) RadioListDialog(
        title = stringResource(R.string.set7),
        options = listOf("common_theme", "night_theme"),
        labels = listOf(stringResource(R.string.common_theme), stringResource(R.string.night_theme)),
        selected = state.theme,
        onSelect = { onThemeChanged(it); showThemeDialog = false },
        onDismiss = { showThemeDialog = false }
    )
    if (showTimeFormatDialog) RadioListDialog(
        title = stringResource(R.string.set17),
        options = listOf("24", "12"),
        labels = listOf(stringResource(R.string.format_24h), stringResource(R.string.format_12h)),
        selected = state.timeFormat,
        onSelect = { onTimeFormatChanged(it); showTimeFormatDialog = false },
        onDismiss = { showTimeFormatDialog = false }
    )
    if (showChimeModeDialog) RadioListDialog(
        title = stringResource(R.string.set18),
        options = listOf("tts", "custom_audio"),
        labels = listOf(stringResource(R.string.chime_mode_tts), stringResource(R.string.chime_mode_custom)),
        selected = state.chimeMode,
        onSelect = { onChimeModeChanged(it); showChimeModeDialog = false },
        onDismiss = { showChimeModeDialog = false }
    )
    if (showChimeSoundDialog) RadioListDialog(
        title = stringResource(R.string.set19),
        options = listOf("builtin_bell", "builtin_gong", "builtin_chime", "system_picker"),
        labels = listOf(
            stringResource(R.string.chime_builtin_bell),
            stringResource(R.string.chime_builtin_gong),
            stringResource(R.string.chime_builtin_chime),
            stringResource(R.string.chime_system_pick)
        ),
        selected = state.chimeSound,
        onSelect = { onChimeSoundChanged(it); showChimeSoundDialog = false },
        onDismiss = { showChimeSoundDialog = false }
    )
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun SwitchSetting(
    title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ListSetting(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RadioListDialog(
    title: String, options: List<String>, labels: List<String>,
    selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Spacer(Modifier.width(8.dp))
                        Text(labels.getOrElse(index) { option })
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.Cancel)) } }
    )
}

private fun showTimeRangePicker(
    context: Context, startMinutes: Int, endMinutes: Int, onRangeSelected: (Int, Int) -> Unit
) {
    val startHour = startMinutes / 60; val startMin = startMinutes % 60
    val endHour = endMinutes / 60; val endMin = endMinutes % 60
    TimePickerDialog(context, { _, h, m ->
        val ns = h * 60 + m
        TimePickerDialog(context, { _, eh, em ->
            val ne = eh * 60 + em
            if (ns < ne) onRangeSelected(ns, ne)
            else Toast.makeText(context, context.getString(R.string.toast_3), Toast.LENGTH_SHORT).show()
        }, endHour, endMin, true).apply { setTitle(context.getString(R.string.set14)); show() }
    }, startHour, startMin, true).apply { setTitle(context.getString(R.string.set13)); show() }
}

private fun soundLabel(value: String, ctx: Context) = when (value) {
    "notification_sound_control" -> ctx.getString(R.string.array_notification_player)
    "alarm_sound_control" -> ctx.getString(R.string.array_alarm_player)
    else -> ctx.getString(R.string.array_media_player)
}

private fun chimeLabel(value: String, ctx: Context) = when (value) {
    "builtin_gong" -> ctx.getString(R.string.chime_builtin_gong)
    "builtin_chime" -> ctx.getString(R.string.chime_builtin_chime)
    "system_picker" -> ctx.getString(R.string.chime_system_pick)
    else -> ctx.getString(R.string.chime_builtin_bell)
}
