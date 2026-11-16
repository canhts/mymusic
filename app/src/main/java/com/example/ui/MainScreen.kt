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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val activeQueueSongs by viewModel.activeQueueSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val carouselPlaylists by viewModel.carouselPlaylists.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val lyricsUiState by viewModel.lyricsUiState.collectAsStateWithLifecycle()
    val offlineModelState by viewModel.offlineModelState.collectAsStateWithLifecycle()
    val audioSpectrumProfile by viewModel.audioSpectrumProfile.collectAsStateWithLifecycle()
    val bassStrength by viewModel.bassStrength.collectAsStateWithLifecycle()
    val trebleStrength by viewModel.trebleStrength.collectAsStateWithLifecycle()
    val vocalStrength by viewModel.vocalStrength.collectAsStateWithLifecycle()
    val virtualizerStrength by viewModel.virtualizerStrength.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
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
    var isHoaCaReturnToActiveVisible by remember { mutableStateOf(false) }
    var hoaCaReturnToActiveTick by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(currentTab, playbackState.currentSong?.id) {
        if (currentTab != MainTab.SING || playbackState.currentSong == null) {
            isHoaCaReturnToActiveVisible = false
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
    val listTopPadding = 12.dp
    val listBottomPadding = navBarPadding + if (hasActiveBottomPlayer) 144.dp else 90.dp

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    val isMovingForward = targetState.ordinal > initialState.ordinal
                    val enterSlide = slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = 0.80f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { fullWidth -> if (isMovingForward) fullWidth / 5 else -fullWidth / 5 }
                    val exitSlide = slideOutHorizontally(
                        animationSpec = tween(durationMillis = 160)
                    ) { fullWidth -> if (isMovingForward) -fullWidth / 7 else fullWidth / 7 }
                    (enterSlide +
                        fadeIn(animationSpec = tween(220)) +
                        scaleIn(
                            initialScale = 0.96f,
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ))
                        .togetherWith(
                            exitSlide +
                                fadeOut(animationSpec = tween(150)) +
                                scaleOut(targetScale = 0.96f, animationSpec = tween(150))
                        )
                },
                label = "main_tab_transition",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    MainTab.SONGS -> {
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
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 28.dp, vertical = 96.dp)
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
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    MainTab.PLAYLISTS -> {
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
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    MainTab.SING -> {
                        HoaCaScreen(
                            currentSong = playbackState.currentSong,
                            isPlaying = playbackState.isPlaying,
                            currentPositionMsFlow = viewModel.currentPositionMs,
                            lyricsUiState = lyricsUiState,
                            offlineModelState = offlineModelState,
                            onSeekTo = viewModel::onSeekTo,
                            onRescanLyrics = viewModel::rescanCurrentSongLyrics,
                            onDownloadOfflineModel = viewModel::downloadOfflineLyricsModel,
                            returnToActiveRequestTick = hoaCaReturnToActiveTick,
                            onReturnToActiveVisibilityChanged = { visible ->
                                isHoaCaReturnToActiveVisible = visible
                            },
                            contentTopPadding = listTopPadding,
                            contentBottomPadding = listBottomPadding,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            val primaryThemeColor = MaterialTheme.colorScheme.primary

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
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

            key(songs.isNotEmpty()) {
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
                    showReturnToActiveButton = currentTab == MainTab.SING && isHoaCaReturnToActiveVisible,
                    onReturnToActiveClick = {
                        hoaCaReturnToActiveTick++
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        if (showSettingsPopup) {
            val context = LocalContext.current
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

        if (showCurrentQueueSheet && activeQueueSongs.isNotEmpty()) {
            val activePlaylistName = remember(playlists, playbackState.activePlaylistId) {
                playbackState.activePlaylistId?.let { id -> playlists.firstOrNull { it.id == id }?.name }
            }
            CurrentQueueBottomSheet(
                queueSongs = activeQueueSongs,
                currentSong = playbackState.currentSong,
                isPlaying = playbackState.isPlaying,
                activePlaylistName = activePlaylistName,
                onSelectSong = viewModel::onSelectSongFromQueue,
                onOpenPlayer = if (showNowPlayingScreen) {
                    null
                } else {
                    { showNowPlayingScreen = true }
                },
                onDismiss = { showCurrentQueueSheet = false }
            )
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
            modifier = Modifier.fillMaxSize()
        ) {
            if (activePlayerSong != null) {
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
private fun ScanningState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.scanning_songs),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
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
