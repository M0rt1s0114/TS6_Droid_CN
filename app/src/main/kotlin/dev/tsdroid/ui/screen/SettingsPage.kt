package dev.tsdroid.ui.screen

import android.app.Activity
import android.content.pm.PackageManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tsdroid.data.SettingsStore
import dev.tsdroid.han.R
import dev.tsdroid.ui.component.FloatingTile
import kotlinx.coroutines.launch

@Composable
fun SettingsPage(
    onNavigateToAbout: () -> Unit,
    autoReconnect: Boolean,
    onAutoReconnectChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val showLinkThumbnails by settingsStore.showLinkThumbnails.collectAsStateWithLifecycle(initialValue = false)
    val autoLoadImages by settingsStore.autoLoadImages.collectAsStateWithLifecycle(initialValue = true)
    val enableFloatingWindow by settingsStore.enableFloatingWindow.collectAsStateWithLifecycle(initialValue = false)
    val noiseSuppression by settingsStore.noiseSuppression.collectAsStateWithLifecycle(initialValue = true)
    val audioGain by settingsStore.audioGain.collectAsStateWithLifecycle(initialValue = 1.0f)

    val themeOptions = listOf(
        "system" to stringResource(R.string.theme_mode_system),
        "light" to stringResource(R.string.theme_mode_light),
        "dark" to stringResource(R.string.theme_mode_dark),
        "amoled" to stringResource(R.string.theme_mode_amoled),
    )
    val selectedThemeMode by settingsStore.themeMode.collectAsStateWithLifecycle(initialValue = "system")
    val selectedThemeLabel = themeOptions.firstOrNull { it.first == selectedThemeMode }?.second
        ?: stringResource(R.string.theme_mode_system)
    var themeMenuExpanded by remember { mutableStateOf(false) }

    val languageOptions = listOf(
        "zh" to stringResource(R.string.language_simplified_chinese),
        "en" to stringResource(R.string.language_english),
        "fr" to stringResource(R.string.language_french),
    )
    val selectedLanguageTag by settingsStore.language.collectAsStateWithLifecycle(initialValue = "zh")
    val selectedLanguageLabel = languageOptions.firstOrNull { it.first == selectedLanguageTag }?.second
        ?: stringResource(R.string.language_simplified_chinese)
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var pendingLanguageTag by remember { mutableStateOf<String?>(null) }
    val activity = context as? Activity

    pendingLanguageTag?.let { languageTag ->
        val label = languageOptions.firstOrNull { it.first == languageTag }?.second ?: languageTag
        AlertDialog(
            onDismissRequest = { pendingLanguageTag = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.language_change_title)) },
            text = { Text(stringResource(R.string.language_change_message, label)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        settingsStore.setLanguage(languageTag)
                        activity?.recreate()
                    }
                    pendingLanguageTag = null
                }) {
                    Text(stringResource(R.string.restart))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLanguageTag = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // ── 外观 ──
        SettingsSectionTitle(stringResource(R.string.section_appearance), Icons.Default.Palette)
        FloatingTile(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20,
            contentPadding = 4,
        ) {
            Column(modifier = Modifier.padding(vertical = 2.dp)) {

                // 主题模式
                SettingsClickableRow(
                    label = stringResource(R.string.theme_mode),
                    onClick = { themeMenuExpanded = true },
                    trailing = {
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedThemeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Icon(
                                    Icons.AutoMirrored.Filled.NavigateNext,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DropdownMenu(
                                expanded = themeMenuExpanded,
                                onDismissRequest = { themeMenuExpanded = false },
                            ) {
                                themeOptions.forEach { (mode, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            scope.launch { settingsStore.setThemeMode(mode) }
                                            themeMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    },
                )

                // 悬浮窗
                SettingsSwitchRow(
                    label = stringResource(R.string.enable_floating_window),
                    checked = enableFloatingWindow,
                    onCheckedChange = { scope.launch { settingsStore.setEnableFloatingWindow(it) } },
                )

                // 自定义背景
                CustomBackgroundSection(context)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── 音频 ──
        SettingsSectionTitle(stringResource(R.string.section_audio), Icons.Default.Mic)
        FloatingTile(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20,
            contentPadding = 4,
        ) {
            Column(modifier = Modifier.padding(vertical = 2.dp)) {

                // 音量增益
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "${stringResource(R.string.audio_gain)} : ${stringResource(R.string.audio_gain_value, audioGain)}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = audioGain,
                        onValueChange = { scope.launch { settingsStore.setAudioGain(it) } },
                        valueRange = 1.0f..8.0f,
                        steps = 13,
                    )
                }

                // 麦克风降噪
                SettingsSwitchRow(
                    label = stringResource(R.string.noise_suppression),
                    checked = noiseSuppression,
                    onCheckedChange = { scope.launch { settingsStore.setNoiseSuppression(it) } },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── 聊天 ──
        SettingsSectionTitle(stringResource(R.string.section_chat), Icons.Default.Forum)
        FloatingTile(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20,
            contentPadding = 4,
        ) {
            Column(modifier = Modifier.padding(vertical = 2.dp)) {

                SettingsSwitchRow(
                    label = stringResource(R.string.auto_reconnect),
                    checked = autoReconnect,
                    onCheckedChange = onAutoReconnectChange,
                )
                SettingsSwitchRow(
                    label = stringResource(R.string.show_link_thumbnails),
                    checked = showLinkThumbnails,
                    onCheckedChange = { scope.launch { settingsStore.setShowLinkThumbnails(it) } },
                )
                SettingsSwitchRow(
                    label = stringResource(R.string.auto_load_images),
                    checked = autoLoadImages,
                    onCheckedChange = { scope.launch { settingsStore.setAutoLoadImages(it) } },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── 更多 ──
        SettingsSectionTitle(stringResource(R.string.section_more), Icons.Default.MoreHoriz)
        FloatingTile(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20,
            contentPadding = 4,
        ) {
            Column(modifier = Modifier.padding(vertical = 2.dp)) {

                // 语言切换
                SettingsClickableRow(
                    label = stringResource(R.string.language_change_title),
                    onClick = { languageMenuExpanded = true },
                    trailing = {
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedLanguageLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Icon(
                                    Icons.AutoMirrored.Filled.NavigateNext,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DropdownMenu(
                                expanded = languageMenuExpanded,
                                onDismissRequest = { languageMenuExpanded = false },
                            ) {
                                languageOptions.forEach { (tag, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            pendingLanguageTag = tag
                                            languageMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    },
                )

                // 关于软件
                SettingsClickableRow(
                    label = stringResource(R.string.about_software),
                    onClick = onNavigateToAbout,
                )

                // 检查更新功能已暂时停用，避免打扰用户
            }
        }

        Spacer(Modifier.height(32.dp))

        // 版本号
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) { "" }
        Text(
            text = "TS6 Droid v$versionName",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

// ── 可复用组件 ──

@Composable
private fun SettingsSectionTitle(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsClickableRow(
    label: String,
    onClick: () -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {
        Icon(Icons.AutoMirrored.Filled.NavigateNext, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    },
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        trailing()
    }
}

// ── 自定义背景区 ──

@Composable
private fun CustomBackgroundSection(context: Context) {
    val scope = rememberCoroutineScope()
    var showCropScreen by remember { mutableStateOf(false) }
    var cropBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var hasCustom by remember { mutableStateOf(dev.tsdroid.background.CustomBackgroundManager.hasCustomBackground(context)) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    cropBitmap = bitmap
                    showCropScreen = true
                }
            } catch (_: Exception) {
                Toast.makeText(context, context.getString(R.string.custom_bg_load_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showCropScreen && cropBitmap != null) {
        Dialog(
            onDismissRequest = {
                cropBitmap?.recycle()
                cropBitmap = null
                showCropScreen = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            dev.tsdroid.background.CropScreen(
                bitmap = cropBitmap!!,
                onConfirm = { left, top, right, bottom ->
                    val success = dev.tsdroid.background.CustomBackgroundManager.cropAndSave(context, cropBitmap!!, left, top, right, bottom)
                    if (success) {
                        hasCustom = true
                        scope.launch {
                            dev.tsdroid.ui.component.AnimeWallpaperState.refreshCustomBackground(context)
                        }
                        Toast.makeText(context, context.getString(R.string.custom_bg_saved), Toast.LENGTH_SHORT).show()
                    }
                    cropBitmap?.recycle()
                    cropBitmap = null
                    showCropScreen = false
                },
                onDismiss = {
                    cropBitmap?.recycle()
                    cropBitmap = null
                    showCropScreen = false
                },
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.custom_bg_delete)) },
            text = { Text(stringResource(R.string.custom_bg_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    dev.tsdroid.background.CustomBackgroundManager.deleteBackground(context)
                    hasCustom = false
                    scope.launch {
                        dev.tsdroid.ui.component.AnimeWallpaperState.refreshCustomBackground(context)
                    }
                    showDeleteConfirm = false
                }) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Text(
        text = stringResource(R.string.custom_bg_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 2.dp),
    )
    Text(
        text = if (hasCustom) stringResource(R.string.custom_bg_active) else stringResource(R.string.custom_bg_inactive),
        style = MaterialTheme.typography.bodySmall,
        color = if (hasCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.custom_bg_upload))
        }
        if (hasCustom) {
            OutlinedButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.custom_bg_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

