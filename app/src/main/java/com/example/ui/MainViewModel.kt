package com.example.ui

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LyricsRepository
import com.example.data.PlaylistRepository
import com.example.data.SongRepository
import com.example.data.StorageOpResult
import com.example.data.local.AppDatabase
import com.example.model.AudioOutputDevice
import com.example.model.LyricsUiState
import com.example.model.OfflineModelState
import com.example.model.PlaybackState
import com.example.model.Playlist
import com.example.model.Song
import com.example.service.MusicPlaybackService
import com.example.service.PlaybackManager
import com.example.ui.theme.AppThemeColor
import com.example.ui.theme.ThemeMode
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = SongRepository(application)
    private val playlistRepository = PlaylistRepository(database.playlistDao())
    private val lyricsRepository = LyricsRepository(
        context = application,
        lyricsCacheDao = database.lyricsCacheDao()
    )
    private val playbackManager = PlaybackManager.getInstance(application)
    private val themePrefs: SharedPreferences = application.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)

    private val _isSystemDark = MutableStateFlow(
        (application.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    )

    private val _themeMode = MutableStateFlow(loadInitialThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(
        resolveIsDark(_themeMode.value, _isSystemDark.value)
    )
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private fun updateEffectiveDarkTheme() {
        _isDarkTheme.value = resolveIsDark(_themeMode.value, _isSystemDark.value)
    }

    private fun loadInitialThemeMode(): ThemeMode {
        if (themePrefs.contains(KEY_THEME_MODE)) {
            return ThemeMode.fromId(themePrefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.id))
        }
        if (themePrefs.contains(KEY_IS_DARK)) {
            return if (themePrefs.getBoolean(KEY_IS_DARK, false)) ThemeMode.DARK else ThemeMode.LIGHT
        }
        return ThemeMode.SYSTEM
    }

    private fun resolveIsDark(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    private val _themeColor = MutableStateFlow(
        AppThemeColor.fromId(themePrefs.getString(KEY_THEME_COLOR_ID, AppThemeColor.DEFAULT.id))
    )
    val themeColor: StateFlow<AppThemeColor> = _themeColor.asStateFlow()

    private val _hasSeenOnboarding = MutableStateFlow(
        themePrefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
    )
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding.asStateFlow()

    private val _isPlayerPillCollapsed = MutableStateFlow(
        themePrefs.getBoolean(KEY_PLAYER_PILL_COLLAPSED, false)
    )
    val isPlayerPillCollapsed: StateFlow<Boolean> = _isPlayerPillCollapsed.asStateFlow()

    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val songs: StateFlow<List<Song>> = playbackManager.songs
    val activeQueueSongs: StateFlow<List<Song>> = playbackManager.orderedSongs
    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState
    val currentPositionMs: StateFlow<Long> = playbackManager.currentPositionMs
    val hasPermission: StateFlow<Boolean> = playbackManager.hasPermission
    val bassStrength: StateFlow<Int> = playbackManager.bassStrength
    val trebleStrength: StateFlow<Int> = playbackManager.trebleStrength
    val vocalStrength: StateFlow<Int> = playbackManager.vocalStrength
    val virtualizerStrength: StateFlow<Int> = playbackManager.virtualizerStrength

    val lyricsUiState: StateFlow<LyricsUiState> = lyricsRepository.uiState
    val offlineModelState: StateFlow<OfflineModelState> = lyricsRepository.offlineModelState

    val playlists: StateFlow<List<Playlist>> = playlistRepository
        .observePlaylistsWithSongs(songs)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val carouselPlaylists: StateFlow<List<Playlist>> = playlists
        .map { list ->
            list.filter { it.songs.isNotEmpty() }
                .sortedWith(
                    compareByDescending<Playlist> { it.playCount }
                        .thenBy { it.createdAt }
                )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _permissionIntentSender = MutableStateFlow<IntentSender?>(null)
    val permissionIntentSender: StateFlow<IntentSender?> = _permissionIntentSender.asStateFlow()

    private var songsJob: Job? = null
    private var lyricsJob: Job? = null
    private var pendingOperation: (() -> Unit)? = null
    private val storageMutex = Mutex()
    private val isExiting = AtomicBoolean(false)

    init {
        checkInitialPermission()
        observePlaylistQueueSync()
        observeCurrentSongForLyrics()
    }

    private fun observePlaylistQueueSync() {
        viewModelScope.launch {
            playlists.collect { playlistList ->
                playbackManager.syncPlaylists(playlistList)
            }
        }
    }

    private fun observeCurrentSongForLyrics() {
        viewModelScope.launch {
            playbackState
                .distinctUntilChangedBy { it.currentSong?.id to it.currentSong?.title }
                .collectLatest { state ->
                    lyricsRepository.loadLyricsForSong(state.currentSong, forceRescan = false)
                }
        }
    }

    fun rescanCurrentSongLyrics() {
        val currentSong = playbackState.value.currentSong ?: return
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            lyricsRepository.rescanLyrics(currentSong)
            _userMessage.value = "Đã làm mới lời bài hát"
        }
    }

    fun downloadOfflineLyricsModel() {
        val currentSong = playbackState.value.currentSong
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            lyricsRepository.downloadOfflineModelAndTranscribe(currentSong)
        }
    }

    private fun checkInitialPermission() {
        val app = getApplication<Application>()
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(app, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(app, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        if (isGranted) {
            onPermissionResult(true)
        }
    }

    fun onBassStrengthChange(strength: Int, persist: Boolean = true) {
        playbackManager.setBassStrength(strength, persist)
    }

    fun onTrebleStrengthChange(strength: Int, persist: Boolean = true) {
        playbackManager.setTrebleStrength(strength, persist)
    }

    fun onVocalStrengthChange(strength: Int, persist: Boolean = true) {
        playbackManager.setVocalStrength(strength, persist)
    }

    fun onVirtualizerStrengthChange(strength: Int, persist: Boolean = true) {
        playbackManager.setVirtualizerStrength(strength, persist)
    }

    fun initTheme(isSystemDark: Boolean) {
        onSystemDarkChanged(isSystemDark)
    }

    fun onSystemDarkChanged(isSystemDark: Boolean) {
        if (_isSystemDark.value != isSystemDark) {
            _isSystemDark.value = isSystemDark
            updateEffectiveDarkTheme()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        if (_themeMode.value != mode) {
            _themeMode.value = mode
            themePrefs.edit().putString(KEY_THEME_MODE, mode.id).apply()
            updateEffectiveDarkTheme()
        } else if (!themePrefs.contains(KEY_THEME_MODE)) {
            themePrefs.edit().putString(KEY_THEME_MODE, mode.id).apply()
        }
    }

    fun toggleTheme() {
        val nextMode = if (isDarkTheme.value) ThemeMode.LIGHT else ThemeMode.DARK
        setThemeMode(nextMode)
    }

    fun setTheme(isDark: Boolean) {
        setThemeMode(if (isDark) ThemeMode.DARK else ThemeMode.LIGHT)
    }

    fun setThemeColor(color: AppThemeColor) {
        if (_themeColor.value != color) {
            _themeColor.value = color
            themePrefs.edit().putString(KEY_THEME_COLOR_ID, color.id).apply()
        }
    }

    fun completeOnboarding() {
        if (!_hasSeenOnboarding.value) {
            _hasSeenOnboarding.value = true
            themePrefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, true).apply()
        }
    }

    fun setPlayerPillCollapsed(collapsed: Boolean) {
        if (_isPlayerPillCollapsed.value != collapsed) {
            _isPlayerPillCollapsed.value = collapsed
            themePrefs.edit().putBoolean(KEY_PLAYER_PILL_COLLAPSED, collapsed).apply()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        playbackManager.setPermissionGranted(granted)
        if (granted) {
            if (songsJob?.isActive != true) {
                startObservingSongs()
            }
        } else {
            _isScanning.value = false
        }
    }

    fun startObservingSongs() {
        songsJob?.cancel()
        _isScanning.value = true
        songsJob = viewModelScope.launch {
            repository.getSongsFlow().collect { songList ->
                playbackManager.setSongs(songList)
                _isScanning.value = false
            }
        }
    }

    fun onSongClickedFromLibrary(song: Song) {
        val current = playbackState.value
        if (current.currentSong?.id == song.id && current.activePlaylistId == null) {
            playbackManager.togglePlayPause()
        } else {
            playbackManager.playSongFromLibrary(song)
        }
    }

    fun onSongClickedFromPlaylist(playlist: Playlist, song: Song) {
        val current = playbackState.value
        if (current.currentSong?.id == song.id && current.activePlaylistId == playlist.id) {
            playbackManager.togglePlayPause()
        } else {
            if (current.activePlaylistId != playlist.id) {
                viewModelScope.launch {
                    runCatching { playlistRepository.incrementPlayCount(playlist.id) }
                }
            }
            playbackManager.playSongFromPlaylist(playlist.id, playlist.songs, song)
        }
    }

    fun onPlayPlaylistClicked(playlist: Playlist) {
        if (playlist.songs.isEmpty()) {
            _userMessage.value = "Danh sách phát này chưa có bài hát nào"
            return
        }
        val current = playbackState.value
        val isAlreadyPlayingThis = current.activePlaylistId == playlist.id &&
            current.currentSong != null &&
            playlist.songs.any { it.id == current.currentSong.id } &&
            current.isPlaying
        if (!isAlreadyPlayingThis) {
            viewModelScope.launch {
                runCatching { playlistRepository.incrementPlayCount(playlist.id) }
            }
        }
        playbackManager.playPlaylist(playlist.id, playlist.songs)
    }

    fun createPlaylist(name: String, initialSong: Song? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val newId = playlistRepository.createPlaylist(trimmed)
                if (initialSong != null) {
                    playlistRepository.addSongToPlaylists(initialSong.id, listOf(newId))
                }
            }.onSuccess {
                _userMessage.value = if (initialSong != null) {
                    "Đã tạo \"$trimmed\" và thêm bài hát"
                } else {
                    "Đã tạo danh sách \"$trimmed\""
                }
            }.onFailure {
                _userMessage.value = "Không thể tạo danh sách phát"
            }
        }
    }

    fun onSelectSongFromQueue(song: Song) {
        val current = playbackState.value
        if (current.currentSong?.id == song.id) {
            playbackManager.togglePlayPause()
        } else {
            playbackManager.playSong(song)
        }
    }

    fun renamePlaylist(playlist: Playlist, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == playlist.name) return
        viewModelScope.launch {
            runCatching {
                playlistRepository.renamePlaylist(playlist.id, trimmed)
            }.onSuccess {
                _userMessage.value = "Đã đổi tên thành \"$trimmed\""
            }.onFailure {
                _userMessage.value = "Không thể đổi tên danh sách phát"
            }
        }
    }

    fun reorderPlaylists(orderedIds: List<Long>) {
        if (orderedIds.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                playlistRepository.reorderPlaylists(orderedIds)
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            runCatching {
                playlistRepository.deletePlaylist(playlist.id)
            }.onSuccess {
                _userMessage.value = "Đã xóa danh sách \"${playlist.name}\""
            }.onFailure {
                _userMessage.value = "Không thể xóa danh sách phát"
            }
        }
    }

    fun addSongToPlaylists(song: Song, playlistIds: List<Long>) {
        if (playlistIds.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                playlistRepository.addSongToPlaylists(song.id, playlistIds)
            }.onSuccess {
                _userMessage.value = if (playlistIds.size == 1) {
                    "Đã thêm bài hát vào danh sách phát"
                } else {
                    "Đã thêm bài hát vào ${playlistIds.size} danh sách phát"
                }
            }.onFailure {
                _userMessage.value = "Không thể thêm vào danh sách phát"
            }
        }
    }

    fun addSongsToPlaylist(playlist: Playlist, songIds: List<Long>) {
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                playlistRepository.addSongsToPlaylist(playlist.id, songIds)
            }.onSuccess {
                _userMessage.value = "Đã thêm ${songIds.size} bài hát vào \"${playlist.name}\""
            }.onFailure {
                _userMessage.value = "Không thể thêm bài hát vào danh sách"
            }
        }
    }

    fun removeSongFromPlaylist(playlist: Playlist, song: Song) {
        viewModelScope.launch {
            runCatching {
                playlistRepository.removeSongFromPlaylist(playlist.id, song.id)
            }.onSuccess {
                _userMessage.value = "Đã gỡ khỏi \"${playlist.name}\""
            }.onFailure {
                _userMessage.value = "Không thể gỡ bài hát khỏi danh sách"
            }
        }
    }

    fun onTogglePlayPause() = playbackManager.togglePlayPause()

    fun onPlayNext() = playbackManager.playNext()

    fun onPlayPrevious() = playbackManager.playPrevious()

    fun onCyclePlayMode() = playbackManager.cyclePlayMode()

    fun onToggleAudioOutput() {
        playbackManager.toggleAudioOutputDevice()
        val currentDevice = playbackState.value.outputDevice
        _userMessage.value = if (currentDevice == AudioOutputDevice.SPEAKER) {
            "Đã chuyển sang phát qua loa điện thoại"
        } else {
            "Đã chuyển sang phát qua tai nghe"
        }
    }

    fun onSeekTo(positionMs: Long) = playbackManager.seekTo(positionMs)

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun clearPermissionIntentSender() {
        _permissionIntentSender.value = null
    }

    fun onIntentSenderResult(success: Boolean) {
        _permissionIntentSender.value = null
        if (success) {
            val op = pendingOperation
            pendingOperation = null
            op?.invoke() ?: refreshSongs()
        } else {
            pendingOperation = null
            _userMessage.value = "Thao tác đã bị hủy"
        }
    }

    fun refreshSongs() {
        viewModelScope.launch {
            val updatedList = repository.loadSongsOnce()
            playbackManager.setSongs(updatedList)
        }
    }

    fun renameSong(song: Song, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return

        val existingExt = if (song.title.contains('.')) {
            song.title.substringAfterLast('.').ifBlank { "mp3" }
        } else {
            "mp3"
        }
        val finalFileName = if (trimmed.endsWith(".$existingExt", ignoreCase = true)) {
            trimmed
        } else {
            "$trimmed.$existingExt"
        }

        viewModelScope.launch {
            storageMutex.withLock {
                when (val result = repository.renameSong(song, finalFileName)) {
                    is StorageOpResult.Success -> {
                        playbackManager.onSongRenamed(song.id, finalFileName)
                        delay(200)
                        playbackManager.setSongs(repository.loadSongsOnce())
                        _userMessage.value = "Đã đổi tên bài hát thành công"
                    }
                    is StorageOpResult.RequiresUserConsent -> {
                        pendingOperation = {
                            viewModelScope.launch {
                                storageMutex.withLock {
                                    val opResult = repository.renameSong(song, finalFileName, userGrantedConsent = true)
                                    if (opResult is StorageOpResult.Success) {
                                        playbackManager.onSongRenamed(song.id, finalFileName)
                                        delay(300)
                                        playbackManager.setSongs(repository.loadSongsOnce())
                                        _userMessage.value = "Đã đổi tên bài hát thành công"
                                    } else if (opResult is StorageOpResult.Failure) {
                                        _userMessage.value = "Không thể đổi tên: ${opResult.message}"
                                    }
                                }
                            }
                        }
                        _permissionIntentSender.value = result.intentSender
                    }
                    is StorageOpResult.Failure -> {
                        _userMessage.value = "Không thể đổi tên: ${result.message}"
                    }
                }
            }
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            storageMutex.withLock {
                when (val result = repository.deleteSong(song)) {
                    is StorageOpResult.Success -> {
                        playlistRepository.removeSongFromAllPlaylists(song.id)
                        playbackManager.onSongDeleted(song.id)
                        playbackManager.setSongs(repository.loadSongsOnce())
                        _userMessage.value = "Đã xóa bài hát"
                    }
                    is StorageOpResult.RequiresUserConsent -> {
                        pendingOperation = {
                            viewModelScope.launch {
                                storageMutex.withLock {
                                    playlistRepository.removeSongFromAllPlaylists(song.id)
                                    playbackManager.onSongDeleted(song.id)
                                    delay(200)
                                    playbackManager.setSongs(repository.loadSongsOnce())
                                    _userMessage.value = "Đã xóa bài hát"
                                }
                            }
                        }
                        _permissionIntentSender.value = result.intentSender
                    }
                    is StorageOpResult.Failure -> {
                        _userMessage.value = "Không thể xóa bài hát: ${result.message}"
                    }
                }
            }
        }
    }

    fun exitApp(activity: Activity?) {
        if (!isExiting.compareAndSet(false, true)) return
        playbackManager.stopPlayback()
        val app = getApplication<Application>()
        try {
            app.stopService(Intent(app, MusicPlaybackService::class.java))
            val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancelAll()
        } catch (_: Exception) {
        }
        activity?.finishAffinity()
        Process.killProcess(Process.myPid())
    }

    override fun onCleared() {
        songsJob?.cancel()
        lyricsJob?.cancel()
        pendingOperation = null
        super.onCleared()
    }

    companion object {
        private const val PREFS_THEME = "music_player_theme_prefs"
        private const val KEY_IS_DARK = "pref_is_dark_theme"
        const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_THEME_COLOR_ID = "pref_theme_color_id"
        const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        const val KEY_PLAYER_PILL_COLLAPSED = "pref_player_pill_collapsed"
    }
}
