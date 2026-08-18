package dev.tsdroid.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.tsdroid.han.R
import dev.tslib.ConnectionState
import dev.tslib.User
import dev.tsdroid.ui.component.AnimeWallpaperState
import dev.tsdroid.ui.component.CustomBackground
import dev.tsdroid.ui.component.FloatingTile
import dev.tsdroid.ui.component.ChannelTree
import dev.tsdroid.ui.component.ChatView
import dev.tsdroid.ui.component.FileManagerDialog
import dev.tsdroid.ui.component.ShareTarget
import dev.tsdroid.viewmodel.ChatMessage
import dev.tsdroid.viewmodel.DownloadState
import dev.tsdroid.viewmodel.FileAttachment
import dev.tsdroid.viewmodel.ServerViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(
    onDisconnected: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: ServerViewModel = viewModel(),
) {
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val channelIcons by viewModel.channelIcons.collectAsStateWithLifecycle()
    val userAvatars by viewModel.avatars.collectAsStateWithLifecycle()
    val serverInfo by viewModel.serverInfo.collectAsStateWithLifecycle()
    val channelMessages by viewModel.channelMessages.collectAsStateWithLifecycle()
    val privateMessages by viewModel.privateMessages.collectAsStateWithLifecycle()
    val isMicMuted by viewModel.isMicMuted.collectAsStateWithLifecycle()
    val isOutputMuted by viewModel.isOutputMuted.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connectionStartedAt by viewModel.connectionStartedAtMillis.collectAsStateWithLifecycle()
    val unreadChannel by viewModel.unreadChannel.collectAsStateWithLifecycle()
    val unreadPrivate by viewModel.unreadPrivate.collectAsStateWithLifecycle()
    val audioGain by viewModel.audioGain.collectAsStateWithLifecycle()
    val showLinkThumbnails by viewModel.showLinkThumbnails.collectAsStateWithLifecycle()
    val autoLoadImages by viewModel.autoLoadImages.collectAsStateWithLifecycle()
    val enableFloatingWindow by viewModel.enableFloatingWindow.collectAsStateWithLifecycle()
    val noiseSuppression by viewModel.noiseSuppression.collectAsStateWithLifecycle()
    val mutedUserIds by viewModel.mutedUserIds.collectAsStateWithLifecycle()
    val fileManagerOpen by viewModel.fileManagerOpen.collectAsStateWithLifecycle()
    val fileList by viewModel.fileList.collectAsStateWithLifecycle()
    val previewImageBytes by viewModel.previewImageBytes.collectAsStateWithLifecycle()
    val previewImageName by viewModel.previewImageName.collectAsStateWithLifecycle()
    val currentFilePath by viewModel.currentFilePath.collectAsStateWithLifecycle()
    val fileManagerLoading by viewModel.fileManagerLoading.collectAsStateWithLifecycle()
    val channelPermissions by viewModel.currentChannelPermissions.collectAsStateWithLifecycle()

    var chatOpen by remember { mutableStateOf(false) }
    var chatEverOpened by remember { mutableStateOf(false) }
    var chatTab by remember { mutableIntStateOf(0) }
    var messageText by remember { mutableStateOf("") }
    var pmTargetId by remember { mutableStateOf<Int?>(null) }

    // Resolve pmTarget User from users list
    val pmTarget = pmTargetId?.let { id -> users.find { it.id == id } }

    // Build PM conversation user list (id → name) from message map + users list
    val context = LocalContext.current
    val resources = LocalResources.current
    val pmConversationUsers = remember(privateMessages, users) {
        privateMessages.keys.map { userId ->
            val name = users.find { it.id == userId }?.nickname
                ?: privateMessages[userId]?.lastOrNull { !it.isMe }?.sender
                ?: resources.getString(R.string.user_fallback, userId)
            userId to name
        }
    }

    val totalUnreadPrivate = unreadPrivate.values.sum()

    // Sync chat state to ViewModel for unread tracking
    LaunchedEffect(chatOpen, chatTab, pmTargetId) {
        viewModel.setChatState(chatOpen, chatTab, pmTargetId)
    }

    DisposableEffect(Unit) {
        viewModel.bindToService()
        onDispose {}
    }

    // Navigate away on disconnect 鈥?one-shot via LaunchedEffect
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.DISCONNECTED) {
            onDisconnected()
        }
    }
    if (connectionState == ConnectionState.DISCONNECTED) return

    // Show floating window when entering ServerScreen if enabled
    LaunchedEffect(enableFloatingWindow) {
        if (enableFloatingWindow && android.provider.Settings.canDrawOverlays(context)) {
            dev.tsdroid.service.TsConnectionService.instance?.showFloatingWindow()
        } else {
            dev.tsdroid.service.TsConnectionService.instance?.hideFloatingWindow()
        }
    }

    val totalUnread = unreadChannel + totalUnreadPrivate

    Box(modifier = Modifier.fillMaxSize()) {
        CustomBackground()

        Scaffold(
            containerColor = Color.Transparent,
        topBar = {
            // Only trust wallpaper-derived colors when the wallpaper is
            // actually visible; otherwise fall back to the theme color
            // (fixes black text/icons on the AMOLED black background).
            val adaptiveTopBarColor = if (AnimeWallpaperState.customBitmap.value != null) {
                AnimeWallpaperState.recommendedContentColor.value
                    ?: MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                // Same width and border language as the channel tile below.
                FloatingTile(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20,
                    contentPadding = 12,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Dns,
                                    contentDescription = null,
                                    tint = adaptiveTopBarColor,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = serverInfo?.name ?: stringResource(R.string.server),
                                style = MaterialTheme.typography.titleSmall,
                                color = adaptiveTopBarColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val maxClients = serverInfo?.maxClients ?: 0
                            val onlineText = if (maxClients > 0) {
                                "${users.size}/$maxClients"
                            } else {
                                "${users.size}"
                            }
                            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                            LaunchedEffect(connectionStartedAt) {
                                while (true) {
                                    now = System.currentTimeMillis()
                                    delay(1_000)
                                }
                            }
                            val elapsed = ((now - connectionStartedAt) / 1_000).coerceAtLeast(0)
                            val sessionText = String.format(
                                java.util.Locale.US,
                                "%02d:%02d:%02d",
                                elapsed / 3_600,
                                (elapsed % 3_600) / 60,
                                elapsed % 60,
                            )
                            Text(
                                text = "$onlineText · $sessionText",
                                style = MaterialTheme.typography.labelSmall,
                                color = adaptiveTopBarColor.copy(alpha = 0.7f),
                                maxLines = 1,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF4CAF50), CircleShape),
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = { viewModel.toggleFileManager() },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Folder,
                                contentDescription = stringResource(R.string.file_manager),
                                tint = adaptiveTopBarColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color.White.copy(
                            alpha = if (MaterialTheme.colorScheme.background.luminance() < 0.05f) 0.14f else 0.10f,
                        ),
                    ),
                    shadowElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 1. Talk button: status display or hold-to-talk
                        // Track press independently so recomposition (mic state
                        // flips while held) cannot destroy the active gesture.
                        var pttPressed by remember { mutableStateOf(false) }
                        when {
                            isOutputMuted -> {
                                // Cannot listen => cannot talk.
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.MicOff,
                                            contentDescription = stringResource(R.string.input_muted_state),
                                            modifier = Modifier.size(28.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            stringResource(R.string.input_muted_state),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            isMicMuted || pttPressed -> {
                                // Mic muted + speaker on = hold-to-talk.
                                val pttBackground = if (pttPressed) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                val pttTint = if (pttPressed) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(pttBackground)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    pttPressed = true
                                                    viewModel.setPushToTalk(true)
                                                    try {
                                                        tryAwaitRelease()
                                                    } finally {
                                                        viewModel.setPushToTalk(false)
                                                        pttPressed = false
                                                    }
                                                },
                                            )
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Mic,
                                            contentDescription = stringResource(R.string.push_to_talk),
                                            modifier = Modifier.size(28.dp),
                                            tint = pttTint,
                                        )
                                        Text(
                                            text = if (pttPressed) stringResource(R.string.ptt_active) else stringResource(R.string.ptt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = pttTint,
                                        )
                                    }
                                }
                            }

                            else -> {
                                // Mic open status indicator, intentionally not clickable.
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Mic,
                                            contentDescription = stringResource(R.string.mic_open),
                                            modifier = Modifier.size(28.dp),
                                            tint = Color.White,
                                        )
                                        Text(
                                            stringResource(R.string.mic_open),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Mic control (disabled while the speaker is muted)
                        IconButton(
                            onClick = { viewModel.toggleMicMute() },
                            enabled = !isOutputMuted,
                        ) {
                            Icon(
                                if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = stringResource(if (isMicMuted) R.string.unmute_mic else R.string.mute_mic),
                                tint = when {
                                    isOutputMuted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    isMicMuted -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.primary
                                },
                            )
                        }

                        // 3. Speaker control
                        IconButton(onClick = { viewModel.toggleOutputMute() }) {
                            Icon(
                                if (isOutputMuted) Icons.Default.HeadsetOff else Icons.Default.Headset,
                                contentDescription = stringResource(if (isOutputMuted) R.string.notif_unmute else R.string.notif_mute),
                                tint = if (isOutputMuted) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                            )
                        }

                        // 4. Server messages with unread badge
                        Box {
                            IconButton(onClick = { chatOpen = !chatOpen }) {
                                Icon(
                                    Icons.Default.ChatBubble,
                                    contentDescription = stringResource(R.string.chat),
                                    tint = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                            if (totalUnread > 0) {
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-4).dp, y = 4.dp),
                                ) {
                                    Text("$totalUnread")
                                }
                            }
                        }

                        // 5. Exit server
                        IconButton(onClick = { viewModel.disconnect() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = stringResource(R.string.disconnect),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Channel tree inside one large floating tile
            FloatingTile(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                cornerRadius = 20,
                contentPadding = 8,
            ) {
                ChannelTree(
                    channels = channels,
                    users = users,
                    onChannelClick = { channelId -> viewModel.moveToChannel(channelId) },
                    onUserClick = { user ->
                        pmTargetId = user.id
                        chatTab = 1
                        chatOpen = true
                    },
                    onUserLongClick = { user -> viewModel.toggleMuteUser(user.id) },
                    mutedUserIds = mutedUserIds,
                    channelIcons = channelIcons,
                    userAvatars = userAvatars,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }

            // File manager 鈥?slides up from bottom, fills content area
            val fileManagerProgress by animateFloatAsState(
                targetValue = if (fileManagerOpen) 0f else 1f,
                animationSpec = tween(300),
                label = "fileManager",
            )
            if (fileManagerOpen || fileManagerProgress < 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = size.height * fileManagerProgress },
                ) {
                    FileManagerDialog(
                        currentPath = currentFilePath,
                        files = fileList,
                        isLoading = fileManagerLoading,
                        users = users,
                        permissionHints = channelPermissions,
                        onNavigateToFolder = { viewModel.navigateToFolder(it) },
                        onNavigateUp = { viewModel.navigateUp() },
                        onRefresh = { viewModel.refreshFileList() },
                        onDownload = { viewModel.downloadFileFromManager(it) },
                        onDelete = { viewModel.deleteFileInChannel(it) },
                        onRename = { old, new -> viewModel.renameFileInChannel(old, new) },
                        onCreateDirectory = { viewModel.createDirectoryInChannel(it) },
                        onUploadFile = { name, data -> viewModel.uploadFileToChannel(name, data) },
                        onShareFile = { target, name, size ->
                            when (target) {
                                is ShareTarget.Channel -> viewModel.shareFile(null, name, size)
                                is ShareTarget.PrivateMessage -> viewModel.shareFile(target.userId, name, size)
                            }
                        },
                        onDismiss = { viewModel.closeFileManager() },
                        onPreviewImage = { fileName ->
                            viewModel.previewImageFile(fileName)
                        },
                    )
                }
            }

            // Chat panel 鈥?slides up from bottom, fills content area
            // Once opened, stay composed so re-opening is instant (no recomposition)
            if (chatOpen) chatEverOpened = true
            val chatProgress by animateFloatAsState(
                targetValue = if (chatOpen) 0f else 1f,
                animationSpec = tween(300),
                label = "chat",
            )
            if (chatEverOpened) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .graphicsLayer { translationY = size.height * chatProgress },
                ) {
                    ChatPanel(
                        chatTab = chatTab,
                        onTabChange = { chatTab = it },
                        channelMessages = channelMessages,
                        privateMessages = pmTargetId?.let { id ->
                            privateMessages[id] ?: emptyList()
                        } ?: privateMessages.values.flatten().sortedBy { it.timestamp },
                        privateMessagesByUser = privateMessages,
                        userAvatars = userAvatars,
                        messageText = messageText,
                        onMessageChange = { messageText = it },
                        pmTarget = pmTarget,
                        pmConversationUsers = pmConversationUsers,
                        onSelectPmUser = { userId -> pmTargetId = userId },
                        onClearPmTarget = { pmTargetId = null },
                        onSend = {
                            when (chatTab) {
                                0 -> viewModel.sendChannelMessage(messageText)
                                1 -> pmTargetId?.let { viewModel.sendPrivateMessage(it, messageText) }
                            }
                            messageText = ""
                        },
                        onClose = { chatOpen = false },
                        unreadChannel = unreadChannel,
                        unreadPrivateTotal = totalUnreadPrivate,
                        unreadPrivatePerUser = unreadPrivate,
                        showLinkThumbnails = showLinkThumbnails,
                        autoLoadImages = autoLoadImages,
                        canUploadFiles = (channelPermissions and dev.tslib.Channel.PERM_FILE_UPLOAD) != 0L,
                        onUploadFile = { fileName, data ->
                            viewModel.uploadAndSendFile(fileName, data, chatTab == 1, pmTargetId)
                        },
                        onDownload = { attachment -> viewModel.downloadAttachment(attachment) },
                    )
                }
            }
        }
    }

    // Image preview overlay
    if (previewImageBytes != null) {
        Dialog(onDismissRequest = { viewModel.closePreview() }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .clipToBounds()
                    .clickable { viewModel.closePreview() },
                contentAlignment = Alignment.Center,
            ) {
                previewImageBytes?.let { bytes ->
                    val bitmap = remember(bytes) {
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = previewImageName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPanel(
    chatTab: Int,
    onTabChange: (Int) -> Unit,
    channelMessages: List<ChatMessage>,
    privateMessages: List<ChatMessage>,
    privateMessagesByUser: Map<Int, List<ChatMessage>> = emptyMap(),
    userAvatars: Map<String, ImageBitmap> = emptyMap(),
    messageText: String,
    onMessageChange: (String) -> Unit,
    pmTarget: User?,
    pmConversationUsers: List<Pair<Int, String>>,
    onSelectPmUser: (Int) -> Unit,
    onClearPmTarget: () -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit,
    unreadChannel: Int,
    unreadPrivateTotal: Int,
    unreadPrivatePerUser: Map<Int, Int>,
    showLinkThumbnails: Boolean,
    autoLoadImages: Boolean = true,
    canUploadFiles: Boolean = true,
    onUploadFile: (String, ByteArray) -> Unit = { _, _ -> },
    onDownload: ((FileAttachment) -> StateFlow<DownloadState>)? = null,
) {
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME) ?: -1
            cursor?.moveToFirst()
            val fileName = if (nameIndex >= 0) cursor?.getString(nameIndex) ?: "file" else "file"
            cursor?.close()
            val data = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@rememberLauncherForActivityResult
            if (data.size > 10_485_760) return@rememberLauncherForActivityResult // 10MB max
            onUploadFile(fileName, data)
        } catch (_: Exception) {}
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(8.dp),
            ) {
            // Header: conversation view or tabs
            if (chatTab == 1 && pmTarget != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClearPmTarget) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.about_back),
                        )
                    }
                    Text(
                        text = pmTarget.nickname,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PrimaryTabRow(
                        selectedTabIndex = chatTab,
                        modifier = Modifier.weight(1f),
                        containerColor = Color.Transparent,
                    ) {
                        Tab(
                            selected = chatTab == 0,
                            onClick = { onTabChange(0) },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.tab_channel))
                                    if (unreadChannel > 0) {
                                        Spacer(Modifier.width(4.dp))
                                        Badge { Text("$unreadChannel") }
                                    }
                                }
                            },
                        )
                        Tab(
                            selected = chatTab == 1,
                            onClick = { onTabChange(1) },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.tab_private))
                                    if (unreadPrivateTotal > 0) {
                                        Spacer(Modifier.width(4.dp))
                                        Badge { Text("$unreadPrivateTotal") }
                                    }
                                }
                            },
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }
            }

            if (chatTab == 1 && pmTarget == null) {
                // Telegram-like conversation list.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    pmConversationUsers.forEach { (userId, nickname) ->
                        val lastMessage = privateMessagesByUser[userId]?.lastOrNull()
                        val userUnread = unreadPrivatePerUser[userId] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPmUser(userId) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = nickname.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(nickname, style = MaterialTheme.typography.titleSmall)
                                if (lastMessage != null) {
                                    Text(
                                        text = lastMessage.text.ifBlank { stringResource(R.string.image) },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (userUnread > 0) {
                                Badge { Text("$userUnread") }
                            }
                        }
                    }
                }
            } else {
                val messages = when (chatTab) {
                    0 -> channelMessages
                    1 -> privateMessages
                    else -> emptyList()
                }
                ChatView(
                    messages = messages,
                    showLinkThumbnails = showLinkThumbnails,
                    autoLoadImages = autoLoadImages,
                    onDownload = onDownload,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }

            // Message input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (canUploadFiles) {
                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        enabled = chatTab == 0 || pmTarget != null,
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = stringResource(R.string.attach_file))
                    }
                }
                OutlinedTextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = if (chatTab == 0) {
                                stringResource(R.string.message_channel_placeholder)
                            } else {
                                stringResource(R.string.message_private_placeholder, pmTarget?.nickname ?: "?")
                            }
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    enabled = chatTab == 0 || pmTarget != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                IconButton(
                    onClick = onSend,
                    enabled = messageText.isNotBlank() && (chatTab == 0 || pmTarget != null),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send))
                }
            }
        }
    }
}
