package com.example.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.Playlist
import com.example.model.Song
import com.example.ui.theme.findHostActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

internal const val INACTIVITY_AUTO_COLLAPSE_TIMEOUT_MS = 60_000L

/**
 * Bộ quan sát chạm toàn cục không tiêu thụ sự kiện (`PointerEventPass.Initial`):
 * Phát hiện mọi thao tác chạm/kéo của người dùng ở bất kỳ vị trí nào trên ứng dụng
 * để làm mới bộ đếm 1 phút không tương tác mà không cản trở cử chỉ của các thành phần con.
 */
@Composable
internal fun Modifier.observeUserTouch(onUserTouch: () -> Unit): Modifier {
    val currentOnUserTouch by rememberUpdatedState(onUserTouch)
    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any { it.pressed || it.previousPressed }) {
                    currentOnUserTouch()
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    inactivityTimeoutMs: Long = INACTIVITY_AUTO_COLLAPSE_TIMEOUT_MS
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsStateWithLifecycle()
    val isPlayerPillCollapsed by viewModel.isPlayerPillCollapsed.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val carouselPlaylists by viewModel.carouselPlaylists.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val isPlaylistsLoading by viewModel.isPlaylistsLoading.collectAsStateWithLifecycle()
    val isInitialDataReady by viewModel.isInitialDataReady.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val permissionIntentSender by viewModel.permissionIntentSender.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onIntentSenderResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(permissionIntentSender) {
        permissionIntentSender?.let { sender ->
            try {
                val request = IntentSenderRequest.Builder(sender).build()
                intentSenderLauncher.launch(request)
            } catch (_: Exception) {
                viewModel.clearPermissionIntentSender()
            }
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsMap[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissionsMap[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        viewModel.onPermissionResult(audioGranted)
    }

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    LaunchedEffect(hasSeenOnboarding) {
        if (hasSeenOnboarding && !hasPermission) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    AnimatedContent(
        targetState = hasSeenOnboarding,
        transitionSpec = {
            (fadeIn(animationSpec = tween(durationMillis = 360)) +
                slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { fullWidth -> fullWidth / 10 })
                .togetherWith(
                    fadeOut(animationSpec = tween(durationMillis = 220)) +
                        slideOutHorizontally(
                            animationSpec = tween(durationMillis = 220)
                        ) { fullWidth -> -fullWidth / 12 }
                )
        },
        label = "welcome_to_main_transition",
        modifier = modifier.fillMaxSize()
    ) { onboardingCompleted ->
        if (!onboardingCompleted) {
            WelcomeScreen(
                onStartClick = {
                    viewModel.completeOnboarding()
                    if (!hasPermission) {
                        permissionLauncher.launch(requiredPermissions)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            return@AnimatedContent
        }

        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var currentTab by remember { mutableStateOf(MainTab.SONGS) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var carouselSheetPlaylistId by remember { mutableStateOf<Long?>(null) }

    var showSettingsPopup by remember { mutableStateOf(false) }
    var showCurrentQueueSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistPendingDelete by remember { mutableStateOf<Playlist?>(null) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var playlistToAddSongs by remember { mutableStateOf<Playlist?>(null) }
    var showNowPlayingScreen by remember { mutableStateOf(false) }

    LaunchedEffect(playbackState.currentSong?.id) {
        if (playbackState.currentSong == null) {
            showNowPlayingScreen = false
        }
    }

    LaunchedEffect(showNowPlayingScreen, playbackState.currentSong?.id) {
        val song = playbackState.currentSong
        if (showNowPlayingScreen && song != null) {
            viewModel.ensureAudioSpectrumAnalysis(
                song = song,
                durationMs = playbackState.durationMs,
                positionMs = viewModel.currentPositionMs.value
            )
        }
    }

    val activeDetailPlaylist = remember(playlists, selectedPlaylistId) {
        selectedPlaylistId?.let { id -> playlists.firstOrNull { it.id == id } }
    }
    val activeCarouselSheetPlaylist = remember(playlists, carouselSheetPlaylistId) {
        carouselSheetPlaylistId?.let { id -> playlists.firstOrNull { it.id == id } }
    }

    LaunchedEffect(playlists, selectedPlaylistId) {
        if (selectedPlaylistId != null && playlists.none { it.id == selectedPlaylistId }) {
            selectedPlaylistId = null
        }
    }

    LaunchedEffect(playlists, carouselSheetPlaylistId) {
        if (carouselSheetPlaylistId != null && playlists.none { it.id == carouselSheetPlaylistId }) {
            carouselSheetPlaylistId = null
        }
    }

    BackHandler(
        enabled = showNowPlayingScreen || showSettingsPopup || showCurrentQueueSheet ||
            carouselSheetPlaylistId != null ||
            songToAddToPlaylist != null || playlistToAddSongs != null ||
            (currentTab == MainTab.PLAYLISTS && selectedPlaylistId != null)
    ) {
        when {
            showCurrentQueueSheet -> showCurrentQueueSheet = false
            showNowPlayingScreen -> showNowPlayingScreen = false
            showSettingsPopup -> showSettingsPopup = false
            carouselSheetPlaylistId != null -> carouselSheetPlaylistId = null
            songToAddToPlaylist != null -> songToAddToPlaylist = null
            playlistToAddSongs != null -> playlistToAddSongs = null
            currentTab == MainTab.PLAYLISTS && selectedPlaylistId != null -> selectedPlaylistId = null
        }
    }

    val hasActiveBottomPlayer = playbackState.currentSong != null
    val activePlaylistName = remember(playlists, playbackState.activePlaylistId) {
        playbackState.activePlaylistId?.let { id -> playlists.firstOrNull { it.id == id }?.name }
    }
    // Hiển thị tràn viền (Edge-to-Edge): nội dung cuộn chạy sát mép trên (0.dp) và mép dưới màn hình,
    // bù khoảng đệm statusBarPadding vào contentTopPadding của từng trang để đầu danh sách không bị che khuất.
    val listTopPadding = statusBarPadding + 12.dp
    val listBottomPadding = navBarPadding + if (hasActiveBottomPlayer) 144.dp else 90.dp

    var allTabsPreRendered by remember { mutableStateOf(false) }
    var isToolbarPreRendered by remember { mutableStateOf(false) }
    var hasCompletedAppPreload by remember { mutableStateOf(false) }

    if (!hasCompletedAppPreload && isInitialDataReady && allTabsPreRendered && isToolbarPreRendered) {
        hasCompletedAppPreload = true
    }

    // Tự động thu gọn thanh tiến trình nếu trong vòng 1 phút người dùng không chạm vào bất kỳ đâu trên ứng dụng.
    // Sử dụng MutableStateFlow thay vì Compose State để mọi thao tác chạm/cuộn (120Hz) không gây recompose MainScreen.
    val userInteractionFlow = remember { MutableStateFlow(0L) }
    LaunchedEffect(
        isPlayerPillCollapsed,
        hasActiveBottomPlayer,
        inactivityTimeoutMs
    ) {
        if (!isPlayerPillCollapsed && hasActiveBottomPlayer && inactivityTimeoutMs > 0L) {
            userInteractionFlow.collectLatest {
                delay(inactivityTimeoutMs)
                viewModel.setPlayerPillCollapsed(true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .observeUserTouch { userInteractionFlow.value++ }
            .testTag("main_screen_root")
    ) {
        AmbientGlowingBackground(
            isDarkTheme = isDarkTheme,
            themeColor = themeColor,
            modifier = Modifier.fillMaxSize()
        )

        // LỚP 1 (Dưới cùng - zIndex = 0f): Vùng nội dung cuộn của 2 trang tràn viền sát mép trên và mép dưới màn hình
        KeepAliveMainTabHost(
            currentTab = currentTab,
            isInitialDataReady = isInitialDataReady,
            onAllTabsPreRendered = { allTabsPreRendered = true },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f),
            songsContent = {
                when {
                    !hasPermission -> {
                        val debouncedPermissionRequest = rememberDebouncedAction(debounceMs = 300L) {
                            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(
                                    Manifest.permission.READ_MEDIA_AUDIO,
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            } else {
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            permissionLauncher.launch(permissionsToRequest)
                        }
                        PermissionRequiredState(
                            onRequestPermission = debouncedPermissionRequest,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 28.dp, vertical = 96.dp)
                        )
                    }
                    isScanning && songs.isEmpty() -> {
                        ScanningState(
                            contentTopPadding = listTopPadding,
                            contentBottomPadding = listBottomPadding,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    songs.isEmpty() -> {
                        EmptySongsState(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 28.dp, vertical = 96.dp)
                        )
                    }
                    else -> {
                        SongList(
                            songs = songs,
                            playlists = carouselPlaylists,
                            playbackState = playbackState,
                            onSongClick = viewModel::onSongClickedFromLibrary,
                            onCarouselPlaylistClick = { playlist ->
                                carouselSheetPlaylistId = playlist.id
                            },
                            onCarouselPlayPlaylist = viewModel::onPlayPlaylistClicked,
                            onAddToPlaylist = { song -> songToAddToPlaylist = song },
                            onRenameSong = viewModel::renameSong,
                            onDeleteSong = viewModel::deleteSong,
                            onOpenPlayer = { showNowPlayingScreen = true },
                            contentTopPadding = listTopPadding,
                            contentBottomPadding = listBottomPadding,
                            isDarkTheme = isDarkTheme,
                            isLoading = isScanning,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            },
            playlistsContent = {
                AnimatedContent(
                    targetState = activeDetailPlaylist,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
                    },
                    label = "playlist_detail_transition",
                    modifier = Modifier.fillMaxSize()
                ) { detailPlaylist ->
                    if (detailPlaylist == null) {
                        PlaylistsOverviewScreen(
                            playlists = playlists,
                            playbackState = playbackState,
                            onCreatePlaylist = { showCreatePlaylistDialog = true },
                            onOpenPlaylist = { playlist -> selectedPlaylistId = playlist.id },
                            onPlayPlaylist = viewModel::onPlayPlaylistClicked,
                            onRequestDeletePlaylist = { playlist -> playlistPendingDelete = playlist },
                            onReorderPlaylists = viewModel::reorderPlaylists,
                            contentTopPadding = listTopPadding,
                            contentBottomPadding = listBottomPadding,
                            isLoading = isPlaylistsLoading,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        PlaylistDetailScreen(
                            playlist = detailPlaylist,
                            playbackState = playbackState,
                            onBack = { selectedPlaylistId = null },
                            onPlayAll = { viewModel.onPlayPlaylistClicked(detailPlaylist) },
                            onAddSongs = { playlistToAddSongs = detailPlaylist },
                            onRenamePlaylist = { newName ->
                                viewModel.renamePlaylist(detailPlaylist, newName)
                            },
                            onUpdatePlaylistIcon = { iconId ->
                                viewModel.updatePlaylistIcon(detailPlaylist, iconId)
                            },
                            onReorderSongs = { reordered ->
                                viewModel.reorderPlaylistSongs(detailPlaylist, reordered)
                            },
                            onSongClick = { song ->
                                viewModel.onSongClickedFromPlaylist(detailPlaylist, song)
                            },
                            onRemoveSongFromPlaylist = { song ->
                                viewModel.removeSongFromPlaylist(detailPlaylist, song)
                            },
                            onOpenPlayer = { showNowPlayingScreen = true },
                            contentTopPadding = listTopPadding,
                            contentBottomPadding = listBottomPadding,
                            isDarkTheme = isDarkTheme,
                            isLoading = isPlaylistsLoading,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        )

        // LỚP 2 (Ở giữa - zIndex = 1f): Hai dải nền chuyển sắc dọc (Vertical Gradient Scrim) ở cạnh trên và cạnh dưới,
        // nằm đè lên nội dung cuộn để tạo hiệu ứng mờ dần ở hai đầu nhưng nằm dưới cụm thanh công cụ, không chặn thao tác chạm.
        EdgeToEdgeGradientScrims(
            statusBarHeight = statusBarPadding,
            navigationBarHeight = navBarPadding,
            backgroundColor = MaterialTheme.colorScheme.background
        )

        // LỚP 3 (Nổi lên trên cùng - zIndex = 3f & 4f): Cụm thanh công cụ phía dưới và thông báo nổi hoàn toàn trên lớp chuyển sắc đáy
        CapsuleBottomToolbar(
            currentTab = currentTab,
            onSelectTab = { newTab ->
                if (currentTab == newTab && newTab == MainTab.PLAYLISTS) {
                    selectedPlaylistId = null
                } else {
                    currentTab = newTab
                }
            },
            playbackState = playbackState,
            activePlaylistName = activePlaylistName,
            currentPositionMsFlow = viewModel.currentPositionMs,
            isDarkTheme = isDarkTheme,
            showSettingsPopup = showSettingsPopup,
            isPlayerPillCollapsed = isPlayerPillCollapsed,
            onTogglePlayerPillCollapsed = {
                viewModel.setPlayerPillCollapsed(!isPlayerPillCollapsed)
            },
            bottomInsetPadding = navBarPadding,
            onCyclePlayMode = {
                showSettingsPopup = false
                viewModel.onCyclePlayMode()
            },
            onTogglePlayPause = {
                showSettingsPopup = false
                viewModel.onTogglePlayPause()
            },
            onSeek = viewModel::onSeekTo,
            onOpenQueue = { showCurrentQueueSheet = true },
            onOpenSettings = {
                showSettingsPopup = !showSettingsPopup
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(3f)
                .onGloballyPositioned {
                    if (!isToolbarPreRendered) {
                        isToolbarPreRendered = true
                    }
                }
        )

        val primaryThemeColor = MaterialTheme.colorScheme.primary

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(4f)
                .padding(top = statusBarPadding + 16.dp)
                .padding(horizontal = 20.dp)
                .testTag("top_capsule_snackbar")
        ) { snackbarData ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
                shadowElevation = 0.dp,
                border = null,
                modifier = Modifier.wrapContentSize()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = primaryThemeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = snackbarData.visuals.message,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (showSettingsPopup) {
            val context = LocalContext.current
            val bassStrength by viewModel.bassStrength.collectAsStateWithLifecycle()
            val trebleStrength by viewModel.trebleStrength.collectAsStateWithLifecycle()
            val vocalStrength by viewModel.vocalStrength.collectAsStateWithLifecycle()
            val virtualizerStrength by viewModel.virtualizerStrength.collectAsStateWithLifecycle()
            SettingsBottomSheet(
                isDarkTheme = isDarkTheme,
                selectedThemeMode = themeMode,
                selectedThemeColor = themeColor,
                selectedLanguage = appLanguage,
                onSelectThemeMode = viewModel::setThemeMode,
                onSelectThemeColor = viewModel::setThemeColor,
                onSelectLanguage = viewModel::setAppLanguage,
                onToggleTheme = viewModel::toggleTheme,
                bassStrength = bassStrength,
                trebleStrength = trebleStrength,
                vocalStrength = vocalStrength,
                virtualizerStrength = virtualizerStrength,
                onBassChange = viewModel::onBassStrengthChange,
                onTrebleChange = viewModel::onTrebleStrengthChange,
                onVocalChange = viewModel::onVocalStrengthChange,
                onVirtualizerChange = viewModel::onVirtualizerStrengthChange,
                onExitApp = {
                    showSettingsPopup = false
                    val activity = context.findHostActivity()
                    viewModel.exitApp(activity)
                },
                onDismiss = { showSettingsPopup = false }
            )
        }

        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onConfirmCreate = { name, iconId ->
                    showCreatePlaylistDialog = false
                    viewModel.createPlaylist(name, iconId = iconId)
                },
                onDismiss = { showCreatePlaylistDialog = false }
            )
        }

        playlistPendingDelete?.let { playlist ->
            DeletePlaylistDialog(
                playlist = playlist,
                onConfirmDelete = {
                    playlistPendingDelete = null
                    if (selectedPlaylistId == playlist.id) {
                        selectedPlaylistId = null
                    }
                    viewModel.deletePlaylist(playlist)
                },
                onDismiss = { playlistPendingDelete = null }
            )
        }

        songToAddToPlaylist?.let { song ->
            AddToPlaylistBottomSheet(
                song = song,
                playlists = playlists,
                onConfirmAdd = { playlistIds ->
                    viewModel.addSongToPlaylists(song, playlistIds)
                },
                onQuickCreatePlaylist = { newName ->
                    viewModel.createPlaylist(newName, initialSong = song)
                },
                onDismiss = { songToAddToPlaylist = null }
            )
        }

        playlistToAddSongs?.let { playlist ->
            val latestPlaylist = playlists.firstOrNull { it.id == playlist.id } ?: playlist
            AddSongsToPlaylistBottomSheet(
                playlist = latestPlaylist,
                allSongs = songs,
                onConfirmAddSongs = { songIds ->
                    viewModel.addSongsToPlaylist(latestPlaylist, songIds)
                },
                onDismiss = { playlistToAddSongs = null }
            )
        }

        if (showCurrentQueueSheet) {
            val activeQueueSongs by viewModel.activeQueueSongs.collectAsStateWithLifecycle()
            if (activeQueueSongs.isNotEmpty()) {
                val activeQueuePlaylistName = remember(playlists, playbackState.activePlaylistId) {
                    playbackState.activePlaylistId?.let { id -> playlists.firstOrNull { it.id == id }?.name }
                }
                CurrentQueueBottomSheet(
                    queueSongs = activeQueueSongs,
                    currentSong = playbackState.currentSong,
                    isPlaying = playbackState.isPlaying,
                    activePlaylistName = activeQueuePlaylistName,
                    onSelectSong = viewModel::onSelectSongFromQueue,
                    onOpenPlayer = if (showNowPlayingScreen) {
                        null
                    } else {
                        { showNowPlayingScreen = true }
                    },
                    onDismiss = { showCurrentQueueSheet = false }
                )
            }
        }

        activeCarouselSheetPlaylist?.let { sheetPlaylist ->
            CarouselPlaylistBottomSheet(
                playlist = sheetPlaylist,
                currentSong = playbackState.currentSong,
                activePlaylistId = playbackState.activePlaylistId,
                isPlaying = playbackState.isPlaying,
                onPlayPlaylist = { pl ->
                    viewModel.onPlayPlaylistClicked(pl)
                },
                onSelectSong = { song ->
                    viewModel.onSongClickedFromPlaylist(sheetPlaylist, song)
                },
                onOpenPlayer = { showNowPlayingScreen = true },
                onDismiss = { carouselSheetPlaylistId = null }
            )
        }

        val activePlayerSong = playbackState.currentSong
        AnimatedVisibility(
            visible = showNowPlayingScreen && activePlayerSong != null,
            enter = fadeIn(tween(240)) +
                slideInVertically(
                    initialOffsetY = { it / 5 },
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) +
                scaleIn(
                    initialScale = 0.95f,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
            exit = fadeOut(tween(180)) +
                slideOutVertically(
                    targetOffsetY = { it / 4 },
                    animationSpec = tween(180)
                ) +
                scaleOut(
                    targetScale = 0.96f,
                    animationSpec = tween(180)
                ),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
        ) {
            if (activePlayerSong != null) {
                val audioSpectrumProfile by viewModel.audioSpectrumProfile.collectAsStateWithLifecycle()
                NowPlayingScreen(
                    song = activePlayerSong,
                    isPlaying = playbackState.isPlaying,
                    playMode = playbackState.playMode,
                    themeMode = themeMode,
                    durationMs = playbackState.durationMs,
                    currentPositionMsFlow = viewModel.currentPositionMs,
                    audioSpectrumProfile = audioSpectrumProfile,
                    isDarkTheme = isDarkTheme,
                    onBack = { showNowPlayingScreen = false },
                    onTogglePlayPause = viewModel::onTogglePlayPause,
                    onPlayNext = viewModel::onPlayNext,
                    onPlayPrevious = viewModel::onPlayPrevious,
                    onSeekTo = viewModel::onSeekTo,
                    onCyclePlayMode = viewModel::onCyclePlayMode,
                    onCycleThemeMode = viewModel::cycleThemeMode,
                    onOpenQueue = { showCurrentQueueSheet = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Màn hình tải trước toàn bộ ứng dụng khi mới mở app (App Preload Screen):
        // Giữ nguyên cho đến khi nạp xong dữ liệu, khởi tạo bộ nhớ đệm và dựng sẵn cả 2 trang + thanh điều hướng,
        // sau đó làm mờ nhẹ nhàng (fade-out) để lộ giao diện chính.
        AnimatedVisibility(
            visible = !hasCompletedAppPreload,
            enter = fadeIn(animationSpec = tween(durationMillis = 120)),
            exit = fadeOut(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(20f)
        ) {
            AppPreloadScreen(modifier = Modifier.fillMaxSize())
        }
    }
    }
}

private val ALL_MAIN_TABS_MASK =
    (1 shl MainTab.SONGS.ordinal) or
        (1 shl MainTab.PLAYLISTS.ordinal)

/**
 * Màn hình tải trước toàn bộ ứng dụng khi mới mở app (App Preload Screen):
 * - Chiếm toàn màn hình trên nền Surface mặc định của app.
 * - Chỉ đặt duy nhất 1 biểu tượng tải trang chuẩn Material Design 3 của Google (MD3 Loading Indicator)
 *   nằm chính giữa màn hình (căn giữa tuyệt đối cả chiều ngang và chiều dọc), không có văn bản hay logo.
 */
@Composable
internal fun AppPreloadScreen(
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                }
            }
            .testTag("app_preload_screen")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Md3LoadingIndicator(
                testTag = "app_preload_loading_indicator"
            )
        }
    }
}

/**
 * Hai dải nền chuyển sắc mờ dần (Vertical Gradient Scrim) ở hai cạnh trên và dưới màn hình
 * theo chuẩn hiển thị tràn viền (Edge-to-Edge) của Android và Material Design 3:
 * - Cạnh trên (`top_edge_gradient_scrim`): Chiều cao khớp `WindowInsets.statusBars`, bắt đầu từ mép trên
 *   với `MaterialTheme.colorScheme.background` (`alpha = 1.0f`) và trong suốt dần xuống dưới về `alpha = 0.0f`.
 * - Cạnh dưới (`bottom_edge_gradient_scrim`): Chiều cao khớp `WindowInsets.navigationBars` (tối thiểu `28.dp`
 *   trong khoảng `24.dp`–`32.dp` cho thiết bị dùng thanh cử chỉ mỏng), bắt đầu từ mép đáy màn hình (`alpha = 1.0f`)
 *   và trong suốt dần hướng lên trên về `alpha = 0.0f`.
 * - Không gắn bộ lắng nghe chạm để mọi thao tác chạm/vuốt danh sách đi xuyên qua 100%.
 */
@Composable
internal fun BoxScope.EdgeToEdgeGradientScrims(
    statusBarHeight: Dp,
    navigationBarHeight: Dp,
    backgroundColor: Color = MaterialTheme.colorScheme.background
) {
    val effectiveTopScrimHeight = if (statusBarHeight > 0.dp) statusBarHeight else 28.dp
    val effectiveBottomScrimHeight = navigationBarHeight.coerceAtLeast(28.dp)

    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(effectiveTopScrimHeight)
            .zIndex(1f)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 1.0f),
                            backgroundColor.copy(alpha = 0.0f)
                        ),
                        startY = 0f,
                        endY = size.height
                    )
                )
            }
            .testTag("top_edge_gradient_scrim")
    )

    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(effectiveBottomScrimHeight)
            .zIndex(1f)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.0f),
                            backgroundColor.copy(alpha = 1.0f)
                        ),
                        startY = 0f,
                        endY = size.height
                    )
                )
            }
            .testTag("bottom_edge_gradient_scrim")
    )
}

/**
 * Bộ chuyển 2 trang chính giữ nguyên trạng thái (Keep-Alive) và dựng sẵn (pre-compose/pre-render)
 * toàn bộ 2 trang ngay từ lúc khởi động:
 * - Cả 2 trang (Bài hát, Danh sách) đều được khởi tạo và đo đạc sẵn trong bộ nhớ,
 *   giữ nguyên `LazyListState` / `LazyGridState` / Carousel xuyên suốt vòng đời ứng dụng.
 * - Khi chuyển trang trên Toolbar, không bao giờ hủy hay khởi tạo lại layout từ đầu,
 *   đảm bảo chuyển trang mượt tức thì và không nháy lại loading khi dữ liệu đã có sẵn.
 */
@Composable
internal fun KeepAliveMainTabHost(
    currentTab: MainTab,
    modifier: Modifier = Modifier,
    isInitialDataReady: Boolean = true,
    onAllTabsPreRendered: () -> Unit = {},
    songsContent: @Composable () -> Unit,
    playlistsContent: @Composable () -> Unit
) {
    var readyMeasuredTabsMask by remember { mutableIntStateOf(0) }
    var lastTab by remember { mutableStateOf(currentTab) }
    var exitingTab by remember { mutableStateOf<MainTab?>(null) }
    var transitionDirection by remember { mutableFloatStateOf(1f) }
    val transitionProgress = remember { Animatable(1f) }

    if (isInitialDataReady && readyMeasuredTabsMask == ALL_MAIN_TABS_MASK) {
        onAllTabsPreRendered()
    }

    LaunchedEffect(readyMeasuredTabsMask, isInitialDataReady) {
        if (isInitialDataReady && readyMeasuredTabsMask == ALL_MAIN_TABS_MASK) {
            onAllTabsPreRendered()
        }
    }

    LaunchedEffect(currentTab) {
        if (lastTab != currentTab) {
            exitingTab = lastTab
            transitionDirection = if (currentTab.ordinal > lastTab.ordinal) 1f else -1f
            lastTab = currentTab
            transitionProgress.snapTo(0f)
            transitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 220,
                    easing = FastOutSlowInEasing
                )
            )
            exitingTab = null
        }
    }

    Box(modifier = modifier) {
        MainTab.entries.forEach { tab ->
            val tabBit = 1 shl tab.ordinal
            val isCurrent = tab == currentTab
            val isExiting = tab == exitingTab && exitingTab != currentTab
            val shouldPlace = isCurrent || isExiting
            val needsPrewarmMeasure = isInitialDataReady && (readyMeasuredTabsMask and tabBit) == 0

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layout { measurable, constraints ->
                        if (!shouldPlace && !needsPrewarmMeasure) {
                            layout(0, 0) {}
                        } else {
                            val placeable = measurable.measure(constraints)
                            if (needsPrewarmMeasure) {
                                readyMeasuredTabsMask = readyMeasuredTabsMask or tabBit
                            }
                            if (!shouldPlace) {
                                layout(0, 0) {}
                            } else {
                                layout(placeable.width, placeable.height) {
                                    placeable.placeWithLayer(0, 0) {
                                        val t = transitionProgress.value.coerceIn(0f, 1f)
                                        if (isCurrent) {
                                            if (exitingTab != null && t < 0.999f) {
                                                alpha = (t / 0.72f).coerceIn(0f, 1f)
                                                translationX = (1f - t) * placeable.width * 0.08f * transitionDirection
                                                scaleX = 1f
                                                scaleY = 1f
                                            } else {
                                                alpha = 1f
                                                translationX = 0f
                                                scaleX = 1f
                                                scaleY = 1f
                                            }
                                        } else {
                                            alpha = (1f - t / 0.55f).coerceIn(0f, 1f)
                                            translationX = -t * placeable.width * 0.06f * transitionDirection
                                            scaleX = 1f
                                            scaleY = 1f
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                when (tab) {
                    MainTab.SONGS -> songsContent()
                    MainTab.PLAYLISTS -> playlistsContent()
                }
            }
        }
    }
}

@Composable
private fun EmptySongsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(120.dp)
                .testTag("icon_empty_music_off")
        )
    }
}

@Composable
private fun ScanningState(
    contentTopPadding: Dp = 0.dp,
    contentBottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    PageContentLoadingBox(
        testTag = "songs_page_loading",
        indicatorTestTag = "songs_page_loading_indicator",
        contentTopPadding = contentTopPadding,
        contentBottomPadding = contentBottomPadding,
        modifier = modifier
    )
}

@Composable
private fun PermissionRequiredState(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "perm_pulse")
    val pulseScaleState = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "perm_scale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    val s = pulseScaleState.value
                    scaleX = s
                    scaleY = s
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.permission_required_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.permission_required_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        val permBtnSource = remember { MutableInteractionSource() }
        Button(
            onClick = onRequestPermission,
            interactionSource = permBtnSource,
            shape = CircleShape,
            modifier = Modifier
                .height(48.dp)
                .springPress(scaleDown = 0.92f, interactionSource = permBtnSource)
                .testTag("grant_permission_button")
        ) {
            Text(
                text = stringResource(R.string.grant_permission_action),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
