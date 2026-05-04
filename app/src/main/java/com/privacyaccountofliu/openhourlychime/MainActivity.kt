package com.privacyaccountofliu.openhourlychime

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager
import com.privacyaccountofliu.openhourlychime.model.events.AudioConfigEvent
import com.privacyaccountofliu.openhourlychime.model.services.TimeService
import com.privacyaccountofliu.openhourlychime.model.tools.AlarmReceiver
import com.privacyaccountofliu.openhourlychime.model.tools.AppRestartManager.restartApp
import com.privacyaccountofliu.openhourlychime.model.tools.BatteryOptimizationHelper
import com.privacyaccountofliu.openhourlychime.model.tools.LocaleHelper
import com.privacyaccountofliu.openhourlychime.model.tools.ToastUtil
import com.privacyaccountofliu.openhourlychime.model.tools.Tools
import com.privacyaccountofliu.openhourlychime.ui.screens.AboutScreen
import com.privacyaccountofliu.openhourlychime.ui.screens.MainScreen
import com.privacyaccountofliu.openhourlychime.ui.screens.SettingsScreen
import com.privacyaccountofliu.openhourlychime.ui.screens.SettingsState
import com.privacyaccountofliu.openhourlychime.ui.theme.BlueMiku
import com.privacyaccountofliu.openhourlychime.ui.theme.HourlyChimeTheme
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

enum class Screen { Home, Settings, About }

class MainActivity : ComponentActivity() {

    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var batteryHelper: BatteryOptimizationHelper

    private var pendingServiceStart = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && pendingServiceStart) {
            pendingServiceStart = false
            TimeService.startService(this)
            startAlarmInternal()
        } else if (!isGranted) {
            ToastUtil.showToast(this, getString(R.string.toast_4))
        }
    }

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        if (uri != null) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            prefs.edit().putString("chime_system_uri", uri.toString()).apply()
            prefs.edit().putString("chime_sound_preference", "system_picker").apply()
        }
    }

    override fun attachBaseContext(base: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(base)
        val lang = prefs.getString("language_preference", "Chinese") ?: "Chinese"
        val localeTag = when (lang) {
            "English" -> "en"
            else -> "zh-CN"
        }
        val configCtx = LocaleHelper.setLocale(base, lang)
        super.attachBaseContext(configCtx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        pendingIntent = createAlarmPendingIntent()
        batteryHelper = BatteryOptimizationHelper(this)

        // Restore theme from preferences and apply immediately
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val savedTheme = prefs.getString("theme_preference", "common_theme") ?: "common_theme"
        AppCompatDelegate.setDefaultNightMode(
            if (savedTheme == "night_theme") AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // Apply language
        val lang = prefs.getString("language_preference", "Chinese") ?: "Chinese"
        val localeTag = when (lang) {
            "English" -> "en"
            else -> "zh-CN"
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))

        setContent {
            val isDark = savedTheme == "night_theme"
            HourlyChimeTheme(darkTheme = isDark) {
                MainApp(savedTheme)
            }
        }

        checkBatteryOptimizationStatus()
    }

    private fun createAlarmPendingIntent(): PendingIntent {
        val intent = Intent(this, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun isAlarmSet(): Boolean {
        return try {
            val hasAlarm = PendingIntent.getBroadcast(
                this, 0, Intent(this, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            hasAlarm != null
        } catch (e: Exception) {
            false
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainApp(savedTheme: String) {
        var currentScreen by remember { mutableStateOf(Screen.Home) }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val prefs = remember { PreferenceManager.getDefaultSharedPreferences(this@MainActivity) }
        val scope = rememberCoroutineScope()
        val config = LocalConfiguration.current
        val screenWidth = config.screenWidthDp.dp

        // Service state - check actual alarm state on first composition
        var isServiceRunning by remember { mutableStateOf(isAlarmSet()) }
        var currentTheme by remember { mutableStateOf(savedTheme) }

        val settingsState = remember {
            mutableStateOf(
                SettingsState(
                    notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
                    soundPreference = prefs.getString("sound_preference", "media_sound_control") ?: "media_sound_control",
                    timeRangeStart = (prefs.getString("time_range_preference", "420-1320") ?: "420-1320").split("-").getOrNull(0)?.toIntOrNull() ?: 420,
                    timeRangeEnd = (prefs.getString("time_range_preference", "420-1320") ?: "420-1320").split("-").getOrNull(1)?.toIntOrNull() ?: 1320,
                    language = prefs.getString("language_preference", "Chinese") ?: "Chinese",
                    theme = savedTheme,
                    advancedLogging = prefs.getBoolean("advanced_logging", false),
                    timeFormat = prefs.getString("time_format_preference", "24") ?: "24",
                    chimeMode = prefs.getString("chime_mode_preference", "tts") ?: "tts",
                    chimeSound = prefs.getString("chime_sound_preference", "builtin_bell") ?: "builtin_bell",
                    chimeSystemUri = prefs.getString("chime_system_uri", null)
                )
            )
        }

        val screenTitle = when (currentScreen) {
            Screen.Home -> stringResource(R.string.app_name)
            Screen.Settings -> stringResource(R.string.str9)
            Screen.About -> stringResource(R.string.str12)
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .width(screenWidth * 0.55f),
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerContentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    // Header with gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        BlueMiku,
                                        Color(0xFF26A69A),
                                        Color(0xFF00897B)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, Float.POSITIVE_INFINITY)
                                )
                            )
                            .statusBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // App icon in circular background
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Unspecified
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.msg3).trim(),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Navigation items
                    DrawerNavItem(
                        icon = Icons.Outlined.Home,
                        selectedIcon = Icons.Filled.Home,
                        label = stringResource(R.string.str10),
                        selected = currentScreen == Screen.Home,
                        onClick = {
                            currentScreen = Screen.Home
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerNavItem(
                        icon = Icons.Outlined.Settings,
                        selectedIcon = Icons.Filled.Settings,
                        label = stringResource(R.string.str11),
                        selected = currentScreen == Screen.Settings,
                        onClick = {
                            currentScreen = Screen.Settings
                            scope.launch { drawerState.close() }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // About item
                    DrawerNavItem(
                        icon = Icons.Outlined.Info,
                        selectedIcon = Icons.Filled.Info,
                        label = stringResource(R.string.str12),
                        selected = currentScreen == Screen.About,
                        onClick = {
                            currentScreen = Screen.About
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(Modifier.weight(1f))

                    // Bottom caption
                    Text(
                        text = stringResource(R.string.msg2),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(screenTitle) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.str9))
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (currentScreen) {
                        Screen.Home -> MainScreen(
                            isServiceRunning = isServiceRunning,
                            onToggleService = { enable ->
                                if (enable) startAlarmService()
                                else stopAlarmService()
                                isServiceRunning = enable
                            }
                        )
                        Screen.Settings -> SettingsScreen(
                            state = settingsState.value,
                            onNotificationChanged = { enabled ->
                                prefs.edit().putBoolean("notifications_enabled", enabled).apply()
                                settingsState.value = settingsState.value.copy(notificationsEnabled = enabled)
                                EventBus.getDefault().post(enabled)
                                ToastUtil.showToast(this@MainActivity, getString(R.string.toast_1))
                            },
                            onSoundChanged = { sound ->
                                prefs.edit().putString("sound_preference", sound).apply()
                                settingsState.value = settingsState.value.copy(soundPreference = sound)
                                EventBus.getDefault().post(AudioConfigEvent(Tools().yieldAudioAttr(sound)))
                                ToastUtil.showToast(this@MainActivity, getString(R.string.toast_1))
                            },
                            onTimeRangeChanged = { start, end ->
                                prefs.edit().putString("time_range_preference", "$start-$end").apply()
                                settingsState.value = settingsState.value.copy(timeRangeStart = start, timeRangeEnd = end)
                                EventBus.getDefault().post(listOf(start, end))
                                ToastUtil.showToast(this@MainActivity, getString(R.string.toast_1))
                            },
                            onTestChime = {
                                val intent = Intent(this@MainActivity, TimeService::class.java).apply {
                                    action = "ACTION_TEST_CHIME"
                                }
                                startForegroundService(intent)
                                Toast.makeText(this@MainActivity, getString(R.string.toast_2), Toast.LENGTH_SHORT).show()
                            },
                            onLanguageChanged = { lang ->
                                prefs.edit().putString("language_preference", lang).apply()
                                settingsState.value = settingsState.value.copy(language = lang)
                                ToastUtil.showToast(this@MainActivity, getString(R.string.toast_1))
                                showRestartDialog()
                            },
                            onThemeChanged = { theme ->
                                prefs.edit().putString("theme_preference", theme).apply()
                                settingsState.value = settingsState.value.copy(theme = theme)
                                currentTheme = theme
                                AppCompatDelegate.setDefaultNightMode(
                                    if (theme == "night_theme") AppCompatDelegate.MODE_NIGHT_YES
                                    else AppCompatDelegate.MODE_NIGHT_NO
                                )
                                ToastUtil.showToast(this@MainActivity, getString(R.string.toast_1))
                                showRestartDialog()
                            },
                            onAdvancedLoggingChanged = { enabled ->
                                prefs.edit().putBoolean("advanced_logging", enabled).apply()
                                settingsState.value = settingsState.value.copy(advancedLogging = enabled)
                                ToastUtil.showToast(this@MainActivity, getString(R.string.toast_1))
                            },
                            onTimeFormatChanged = { format ->
                                prefs.edit().putString("time_format_preference", format).apply()
                                settingsState.value = settingsState.value.copy(timeFormat = format)
                                ToastUtil.showToast(this@MainActivity, getString(R.string.toast_1))
                            },
                            onChimeModeChanged = { mode ->
                                prefs.edit().putString("chime_mode_preference", mode).apply()
                                settingsState.value = settingsState.value.copy(chimeMode = mode)
                                ToastUtil.showToast(this@MainActivity, getString(R.string.toast_1))
                            },
                            onChimeSoundChanged = { sound ->
                                if (sound == "system_picker") {
                                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.set19))
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    }
                                    ringtonePickerLauncher.launch(intent)
                                } else {
                                    prefs.edit().putString("chime_sound_preference", sound).apply()
                                    settingsState.value = settingsState.value.copy(chimeSound = sound)
                                    ToastUtil.showToast(this@MainActivity, getString(R.string.toast_1))
                                }
                            },
                            onChimeSystemUriChanged = {},
                            onAboutClick = { currentScreen = Screen.About }
                        )
                        Screen.About -> AboutScreen()
                    }
                }
            }
        }
    }

    private fun startAlarmService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                pendingServiceStart = true
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        TimeService.startService(this)
        startAlarmInternal()
        Toast.makeText(this, getString(R.string.toast_5), Toast.LENGTH_SHORT).show()
    }

    private fun startAlarmInternal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                ToastUtil.showToast(this, getString(R.string.toast_8))
                return
            }
        }
        val calendar = calculateNextAlarmTime()
        try {
            // setAlarmClock gives highest priority - reliable even in Doze mode
            val alarmInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
            alarmManager.setAlarmClock(alarmInfo, pendingIntent)
        } catch (e: SecurityException) {
            android.util.Log.e("Alarm", "Error setting alarm clock, falling back", e)
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                )
            } catch (e2: Exception) {
                android.util.Log.e("Alarm", "Fallback alarm also failed", e2)
            }
        }
    }

    private fun stopAlarmService() {
        alarmManager.cancel(pendingIntent)
        TimeService.stopService(this)
        Toast.makeText(this, getString(R.string.toast_7), Toast.LENGTH_SHORT).show()
    }

    private fun calculateNextAlarmTime(): java.util.Calendar {
        return java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            add(java.util.Calendar.HOUR_OF_DAY, 1)
        }
    }

    private fun checkBatteryOptimizationStatus() {
        if (!batteryHelper.isIgnoringBatteryOptimizations()) {
            showWhitelistRequestDialog()
        }
    }

    private fun showWhitelistRequestDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_white_list_battery_title))
            .setMessage(getString(R.string.dialog_white_list_battery_msg))
            .setPositiveButton(getString(R.string.ToSet)) { _, _ ->
                batteryHelper.guideUserToBatteryWhitelist()
            }
            .setNegativeButton(getString(R.string.Cancel), null)
            .show()
    }

    private fun showRestartDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_need_restart_title))
            .setMessage(getString(R.string.dialog_need_restart_msg))
            .setPositiveButton(getString(R.string.Yes)) { _, _ ->
                runOnUiThread {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        applicationContext.restartApp()
                    }, 200)
                }
            }
            .show()
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    selectedIcon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val iconTint = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer
    else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
