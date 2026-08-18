package dev.tsdroid.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.tsdroid.han.R
import dev.tslib.ConnectionState
import dev.tsdroid.ui.component.AnimeWallpaperState
import dev.tsdroid.ui.component.ChannelTree
import dev.tsdroid.ui.component.CustomBackground
import dev.tsdroid.ui.component.FloatingTile
import dev.tsdroid.viewmodel.ConnectionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    onConnected: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: ConnectionViewModel = viewModel(),
) {
    val address by viewModel.address.collectAsStateWithLifecycle()
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val channel by viewModel.channel.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val bookmarkIcons by viewModel.bookmarkIcons.collectAsStateWithLifecycle()
    val autoReconnect by viewModel.autoReconnect.collectAsStateWithLifecycle()
    val editingIndex by viewModel.editingIndex.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val browsedChannels by viewModel.browsedChannels.collectAsStateWithLifecycle()
    val isBrowsing by viewModel.isBrowsing.collectAsStateWithLifecycle()
    val showChannelPicker by viewModel.showChannelPicker.collectAsStateWithLifecycle()
    val refreshingBookmarks by viewModel.refreshingBookmarks.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var deleteConfirmIndex by remember { mutableStateOf<Int?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    val isConnecting = connectionState == ConnectionState.CONNECTING
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
        viewModel.resumeExistingConnection(onConnected)
    }

    LaunchedEffect(autoReconnect) {
        if (autoReconnect) {
            viewModel.tryAutoReconnect(onConnected)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CustomBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                val adaptiveTopBarColor = if (AnimeWallpaperState.customBitmap.value != null) {
                    AnimeWallpaperState.recommendedContentColor.value
                        ?: MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (selectedTab == 0) stringResource(R.string.app_name)
                                       else stringResource(R.string.tab_settings)
                            )
                            Spacer(Modifier.width(8.dp))
                            if (selectedTab == 0) {
                                IconButton(
                                    onClick = { viewModel.refreshBookmarkInfo() },
                                    enabled = !refreshingBookmarks,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    if (refreshingBookmarks) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = stringResource(R.string.refresh),
                                            tint = adaptiveTopBarColor,
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = adaptiveTopBarColor,
                    ),
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_home)) },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_settings)) },
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(onClick = { showBottomSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.manual_connection))
                    }
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (selectedTab == 0) {
                    // Tab 0: Home
                    Box(modifier = Modifier.fillMaxSize()) {
                    if (bookmarks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 32.dp),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(72.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Cloud,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(36.dp),
                                        )
                                    }
                                }
                                Text(
                                    stringResource(R.string.no_connection),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    stringResource(R.string.no_connection_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(4.dp))
                                Button(onClick = { showBottomSheet = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.manual_connection))
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 88.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            bookmarks.forEachIndexed { index, bookmark ->
                                val icon = if (bookmark.iconId != 0L) bookmarkIcons[bookmark.iconId] else null
                                val active = dev.tsdroid.service.TsConnectionService.instance
                                    ?.hasActiveConnection(bookmark.address) == true
                                val statusColor = when {
                                    active -> Color(0xFF4CAF50)
                                    bookmark.lastSeenAt > 0 -> Color(0xFFFFB300)
                                    else -> MaterialTheme.colorScheme.outline
                                }

                                FloatingTile(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier.size(48.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (icon != null) {
                                                    Image(
                                                        bitmap = icon,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(32.dp),
                                                        contentScale = ContentScale.Fit,
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Outlined.Star,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(28.dp),
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = bookmark.serverName ?: bookmark.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(statusColor, CircleShape),
                                                )
                                            }
                                            Text(
                                                text = bookmark.address,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }

                                        var menuExpanded by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { menuExpanded = true }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = null)
                                            }
                                            DropdownMenu(
                                                expanded = menuExpanded,
                                                onDismissRequest = { menuExpanded = false },
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.edit)) },
                                                    onClick = {
                                                        menuExpanded = false
                                                        viewModel.editBookmark(bookmark, index)
                                                        showBottomSheet = true
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.remove)) },
                                                    onClick = {
                                                        menuExpanded = false
                                                        deleteConfirmIndex = index
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        StatChip(
                                            icon = Icons.Outlined.Person,
                                            text = when {
                                                bookmark.maxClients > 0 -> "${bookmark.clientsOnline}/${bookmark.maxClients}"
                                                bookmark.clientsOnline > 0 -> "${bookmark.clientsOnline}"
                                                else -> "--"
                                            },
                                            modifier = Modifier.weight(1f),
                                        )
                                        StatChip(
                                            icon = Icons.Outlined.Schedule,
                                            text = formatConnectedTime(bookmark.connectedSeconds),
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    Spacer(Modifier.height(10.dp))

                                    Button(
                                        onClick = { viewModel.connectBookmark(bookmark, onConnected) },
                                        enabled = !isConnecting,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        if (isConnecting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                            )
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Text(stringResource(R.string.connect))
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                } else {
                    // Tab 1: Settings
                    SettingsPage(
                    onNavigateToAbout = onNavigateToAbout,
                    autoReconnect = autoReconnect,
                    onAutoReconnectChange = { viewModel.setAutoReconnect(it) },
                    )
                }
            }
            deleteConfirmIndex?.let { idx ->
                val bookmarkName = bookmarks.getOrNull(idx)?.let { it.serverName ?: it.name } ?: ""
                AlertDialog(
                    onDismissRequest = { deleteConfirmIndex = null },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    title = { Text(stringResource(R.string.remove)) },
                    text = { Text(stringResource(R.string.confirm_delete, bookmarkName)) },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.removeBookmark(idx)
                            deleteConfirmIndex = null
                        }) {
                            Text(stringResource(R.string.remove))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteConfirmIndex = null }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                )
            }

            if (showChannelPicker) {
                Dialog(onDismissRequest = { viewModel.showChannelPicker.value = false }) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 0.dp,
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                stringResource(R.string.select_channel),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(Modifier.height(16.dp))
                            ChannelTree(
                                channels = browsedChannels,
                                users = emptyList(),
                                onChannelClick = { viewModel.selectChannel(it) },
                                modifier = Modifier.fillMaxWidth().height(400.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                            TextButton(
                                onClick = { viewModel.showChannelPicker.value = false },
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    }
                }
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                        viewModel.cancelEdit()
                    },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    val isEditing = editingIndex >= 0
                    val glassTextFieldColors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(if (isEditing) R.string.edit_bookmark else R.string.manual_connection),
                            style = MaterialTheme.typography.titleLarge,
                        )

                        OutlinedTextField(
                            value = address,
                            onValueChange = { viewModel.address.value = it },
                            label = { Text(stringResource(R.string.server_address)) },
                            placeholder = { Text(stringResource(R.string.server_address_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            maxLines = 1,
                            enabled = !isConnecting,
                            colors = glassTextFieldColors,
                        )

                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { viewModel.nickname.value = it },
                            label = { Text(stringResource(R.string.nickname)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            maxLines = 1,
                            enabled = !isConnecting,
                            colors = glassTextFieldColors,
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { viewModel.password.value = it },
                            label = { Text(stringResource(R.string.password_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            maxLines = 1,
                            enabled = !isConnecting,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = glassTextFieldColors,
                        )

                        OutlinedTextField(
                            value = channel,
                            onValueChange = { viewModel.channel.value = it },
                            label = { Text(stringResource(R.string.channel_optional)) },
                            placeholder = { Text(stringResource(R.string.default_channel)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            maxLines = 1,
                            enabled = !isConnecting,
                            trailingIcon = {
                                if (isBrowsing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.height(24.dp).width(24.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    IconButton(onClick = { viewModel.browseChannels() }, enabled = !isConnecting) {
                                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.browse_channels))
                                    }
                                }
                            },
                            colors = glassTextFieldColors,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        sheetState.hide()
                                        showBottomSheet = false
                                    }
                                    viewModel.connect(onConnected)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isConnecting,
                            ) {
                                if (isConnecting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.height(20.dp).width(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(stringResource(if (isConnecting) R.string.connecting else R.string.connect))
                            }

                            FilledTonalButton(onClick = {
                                viewModel.saveBookmark()
                                scope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                }
                            }) {
                                Icon(Icons.Default.Star, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(if (isEditing) R.string.save else R.string.add_bookmark))
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatConnectedTime(totalSeconds: Long): String {
    if (totalSeconds <= 0) return "-/-/-/-/-"
    var seconds = totalSeconds
    val years = seconds / (365 * 86_400)
    seconds %= (365 * 86_400)
    val days = seconds / 86_400
    seconds %= 86_400
    val hours = seconds / 3_600
    seconds %= 3_600
    val minutes = seconds / 60
    seconds %= 60

    val parts = mutableListOf<String>()
    if (years > 0) parts += "${years}y"
    if (days > 0) parts += "${days}d"
    if (hours > 0) parts += "${hours}h"
    if (minutes > 0) parts += "${minutes}m"
    if (seconds > 0 || parts.isEmpty()) parts += "${seconds}s"
    return parts.joinToString(" ")
}
