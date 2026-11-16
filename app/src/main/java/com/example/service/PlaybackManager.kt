package com.example.service

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.example.model.AudioOutputDevice
import com.example.model.PlayMode
import com.example.model.PlaybackState
import com.example.model.Playlist
import com.example.model.Song
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PlaybackManager private constructor(private val appContext: Context) {

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private var lastProgressPersistTimeMs = 0L
    private var lastPersistedPositionMs = -1L

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    // Thread-safe queue state guarded by playlistLock
    private val playlistLock = Any()

    @Volatile
    private var songsMap: Map<Long, Song> = emptyMap()

    @Volatile
    private var orderedSongsCache: List<Song> = emptyList()

    @Volatile
    private var orderedSongIdsCache: Set<Long> = emptySet()

    private val _orderedSongs = MutableStateFlow<List<Song>>(emptyList())
    val orderedSongs: StateFlow<List<Song>> = _orderedSongs.asStateFlow()

    @Volatile
    private var activePlaylistId: Long? = null

    private val shuffleQueue = ArrayList<Long>()
    private var shuffleIndex = -1

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _bassStrength = MutableStateFlow(
        prefs.getInt(KEY_BASS_STRENGTH, DEFAULT_BASS_STRENGTH)
    )
    val bassStrength: StateFlow<Int> = _bassStrength.asStateFlow()
    private var lastPersistedBass = _bassStrength.value

    private val _trebleStrength = MutableStateFlow(
        prefs.getInt(KEY_TREBLE_STRENGTH, DEFAULT_TREBLE_STRENGTH)
    )
    val trebleStrength: StateFlow<Int> = _trebleStrength.asStateFlow()
    private var lastPersistedTreble = _trebleStrength.value

    private val _vocalStrength = MutableStateFlow(
        prefs.getInt(KEY_VOCAL_STRENGTH, DEFAULT_VOCAL_STRENGTH)
    )
    val vocalStrength: StateFlow<Int> = _vocalStrength.asStateFlow()
    private var lastPersistedVocal = _vocalStrength.value

    private val _virtualizerStrength = MutableStateFlow(
        prefs.getInt(KEY_VIRTUALIZER_STRENGTH, DEFAULT_VIRTUALIZER_STRENGTH)
    )
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()
    private var lastPersistedVirtualizer = _virtualizerStrength.value

    init {
        clearCommunicationRouting()
        restorePersistedState()
        detectInitialAudioOutput()
        registerAudioDeviceCallback()
    }

    private fun registerAudioDeviceCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                audioManager?.registerAudioDeviceCallback(object : android.media.AudioDeviceCallback() {
                    override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                        updateHeadsetState()
                    }

                    override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                        updateHeadsetState()
                    }
                }, null)
            } catch (_: Throwable) {
                // Device callback not supported
            }
        }
    }

    private fun updateHeadsetState() {
        try {
            val hasHeadset = isHeadsetPluggedIn()
            val newDevice = if (hasHeadset) AudioOutputDevice.HEADPHONES else AudioOutputDevice.SPEAKER
            _playbackState.update {
                if (it.isHeadsetConnected == hasHeadset && it.outputDevice == newDevice) {
                    it
                } else {
                    it.copy(
                        isHeadsetConnected = hasHeadset,
                        outputDevice = newDevice
                    )
                }
            }
        } catch (_: Throwable) {
            // Ignore
        }
    }

    private fun restorePersistedState() {
        val savedModeName = prefs.getString(KEY_PLAY_MODE, PlayMode.REPEAT_ALL.name) ?: PlayMode.REPEAT_ALL.name
        val savedPlayMode = try {
            PlayMode.valueOf(savedModeName)
        } catch (_: Exception) {
            PlayMode.REPEAT_ALL
        }

        val savedPlaylistId = prefs.getLong(KEY_ACTIVE_PLAYLIST_ID, -1L).takeIf { it > 0L }
        activePlaylistId = savedPlaylistId

        val savedPosition = prefs.getLong(KEY_POSITION_MS, 0L)
        _currentPositionMs.value = savedPosition
        lastPersistedPositionMs = savedPosition

        _playbackState.update {
            it.copy(
                playMode = savedPlayMode,
                currentPositionMs = savedPosition,
                activePlaylistId = savedPlaylistId
            )
        }
    }

    fun setPermissionGranted(granted: Boolean) {
        _hasPermission.value = granted
    }

    fun setSongs(songs: List<Song>) {
        synchronized(playlistLock) {
            _songs.value = songs
            songsMap = songs.associateBy { it.id }
            if (activePlaylistId == null) {
                orderedSongsCache = songs
            } else {
                orderedSongsCache = orderedSongsCache.mapNotNull { songsMap[it.id] }
            }
            orderedSongIdsCache = orderedSongsCache.mapTo(HashSet(orderedSongsCache.size)) { it.id }
            _orderedSongs.value = orderedSongsCache
            if (shuffleQueue.size != orderedSongsCache.size) {
                rebuildShuffleQueueLocked()
            }
        }

        val current = _playbackState.value.currentSong
        if (current == null) {
            val lastSongId = prefs.getLong(KEY_LAST_SONG_ID, -1L)
            val matchedSong = if (lastSongId != -1L) {
                songsMap[lastSongId]
            } else {
                null
            }

            if (matchedSong != null) {
                val savedPos = prefs.getLong(KEY_POSITION_MS, 0L)
                _currentPositionMs.value = savedPos
                lastPersistedPositionMs = savedPos
                _playbackState.update {
                    it.copy(
                        currentSong = matchedSong,
                        isPlaying = false,
                        currentPositionMs = savedPos,
                        durationMs = matchedSong.duration
                    )
                }
            }
        } else if (songsMap[current.id] == null) {
            val fallback = orderedSongsCache.firstOrNull() ?: _songs.value.firstOrNull()
            if (fallback != null) {
                _playbackState.update { it.copy(currentSong = fallback) }
            } else {
                _playbackState.update { it.copy(currentSong = null, isPlaying = false) }
            }
        }
    }

    fun syncPlaylists(playlists: List<Playlist>) {
        val currentPlaylistId = activePlaylistId ?: return
        val matched = playlists.firstOrNull { it.id == currentPlaylistId }
        val currentSong = _playbackState.value.currentSong
        var songToAutoPlay: Song? = null
        var shouldPauseEmptyPlaylist = false

        synchronized(playlistLock) {
            if (matched == null) {
                activePlaylistId = null
                orderedSongsCache = _songs.value
                orderedSongIdsCache = orderedSongsCache.mapTo(HashSet(orderedSongsCache.size)) { it.id }
                _orderedSongs.value = orderedSongsCache
                rebuildShuffleQueueLocked(excludeSongId = currentSong?.id)
                _playbackState.update { it.copy(activePlaylistId = null) }
                prefs.edit().remove(KEY_ACTIVE_PLAYLIST_ID).apply()
            } else {
                val updatedQueue = matched.songs
                if (!hasSameSongIds(orderedSongsCache, updatedQueue)) {
                    orderedSongsCache = updatedQueue
                    rebuildShuffleQueueLocked(excludeSongId = currentSong?.id)
                    if (updatedQueue.isEmpty()) {
                        if (_playbackState.value.isPlaying) {
                            shouldPauseEmptyPlaylist = true
                        }
                        activePlaylistId = null
                        orderedSongsCache = _songs.value
                        _playbackState.update { it.copy(activePlaylistId = null) }
                        prefs.edit().remove(KEY_ACTIVE_PLAYLIST_ID).apply()
                    } else if (currentSong != null && updatedQueue.none { it.id == currentSong.id } && _playbackState.value.isPlaying) {
                        songToAutoPlay = updatedQueue.first()
                    }
                    orderedSongIdsCache = orderedSongsCache.mapTo(HashSet(orderedSongsCache.size)) { it.id }
                    _orderedSongs.value = orderedSongsCache
                }
            }
        }

        if (shouldPauseEmptyPlaylist) {
            _playbackState.update { it.copy(isPlaying = false) }
            prefs.edit().putBoolean(KEY_IS_PLAYING, false).apply()
            sendServiceCommand(MusicPlaybackService.ACTION_PAUSE)
        } else {
            songToAutoPlay?.let { playSongInternal(it) }
        }
    }

    private fun rebuildShuffleQueueLocked(excludeSongId: Long? = null) {
        val currentList = orderedSongsCache
        if (currentList.isEmpty()) {
            shuffleQueue.clear()
            shuffleIndex = -1
            return
        }
        val ids = MutableList(currentList.size) { index -> currentList[index].id }
        ids.shuffle(Random.Default)
        if (excludeSongId != null && ids.size > 1 && ids[0] == excludeSongId) {
            val swapIdx = (1..ids.lastIndex).random(Random.Default)
            ids[0] = ids[swapIdx].also { ids[swapIdx] = ids[0] }
        }
        shuffleQueue.clear()
        shuffleQueue.addAll(ids)
        shuffleIndex = 0
    }

    fun playSongFromLibrary(song: Song) {
        synchronized(playlistLock) {
            val switchedQueue = activePlaylistId != null
            activePlaylistId = null
            orderedSongsCache = _songs.value
            orderedSongIdsCache = orderedSongsCache.mapTo(HashSet(orderedSongsCache.size)) { it.id }
            _orderedSongs.value = orderedSongsCache
            if (switchedQueue || shuffleQueue.size != orderedSongsCache.size) {
                rebuildShuffleQueueLocked(excludeSongId = song.id)
            }
        }
        prefs.edit().remove(KEY_ACTIVE_PLAYLIST_ID).apply()
        playSongInternal(song, queuePlaylistId = null)
    }

    private fun hasSameSongIds(a: List<Song>, b: List<Song>): Boolean {
        if (a === b) return true
        if (a.size != b.size) return false
        for (i in a.indices) {
            if (a[i].id != b[i].id) return false
        }
        return true
    }

    fun playSongFromPlaylist(playlistId: Long, playlistSongs: List<Song>, song: Song) {
        if (playlistSongs.isEmpty()) return
        synchronized(playlistLock) {
            val switchedQueue = activePlaylistId != playlistId || !hasSameSongIds(orderedSongsCache, playlistSongs)
            activePlaylistId = playlistId
            orderedSongsCache = playlistSongs
            orderedSongIdsCache = playlistSongs.mapTo(HashSet(playlistSongs.size)) { it.id }
            _orderedSongs.value = orderedSongsCache
            if (switchedQueue || shuffleQueue.size != orderedSongsCache.size) {
                rebuildShuffleQueueLocked(excludeSongId = song.id)
            }
        }
        prefs.edit().putLong(KEY_ACTIVE_PLAYLIST_ID, playlistId).apply()
        playSongInternal(song, queuePlaylistId = playlistId)
    }

    fun playPlaylist(playlistId: Long, playlistSongs: List<Song>) {
        if (playlistSongs.isEmpty()) return
        val current = _playbackState.value
        val isAlreadyThisPlaylist = current.activePlaylistId == playlistId &&
            current.currentSong != null &&
            playlistSongs.any { it.id == current.currentSong.id }
        if (isAlreadyThisPlaylist) {
            togglePlayPause()
        } else {
            playSongFromPlaylist(playlistId, playlistSongs, playlistSongs.first())
        }
    }

    fun playSong(song: Song) {
        playSongInternal(song, queuePlaylistId = activePlaylistId)
    }

    private fun playSongInternal(song: Song, queuePlaylistId: Long? = activePlaylistId) {
        _currentPositionMs.value = 0L
        lastPersistedPositionMs = 0L
        _playbackState.update {
            it.copy(
                currentSong = song,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = song.duration,
                activePlaylistId = queuePlaylistId
            )
        }
        prefs.edit()
            .putLong(KEY_LAST_SONG_ID, song.id)
            .putLong(KEY_POSITION_MS, 0L)
            .putBoolean(KEY_IS_PLAYING, true)
            .apply()
        sendServiceCommand(MusicPlaybackService.ACTION_PLAY)
    }

    fun stopPlayback() {
        _currentPositionMs.value = 0L
        lastPersistedPositionMs = 0L
        _playbackState.update {
            it.copy(
                isPlaying = false,
                currentPositionMs = 0L,
                currentSong = null
            )
        }
        prefs.edit()
            .remove(KEY_LAST_SONG_ID)
            .putLong(KEY_POSITION_MS, 0L)
            .putBoolean(KEY_IS_PLAYING, false)
            .apply()
        sendServiceCommand(MusicPlaybackService.ACTION_STOP)
    }

    fun getOrderedSongs(): List<Song> = orderedSongsCache

    fun onSongDeleted(songId: Long) {
        val current = _playbackState.value.currentSong
        val remainingQueue: List<Song>
        synchronized(playlistLock) {
            val remainingLibrary = _songs.value.filter { it.id != songId }
            _songs.value = remainingLibrary
            songsMap = remainingLibrary.associateBy { it.id }

            if (activePlaylistId == null) {
                orderedSongsCache = remainingLibrary
            } else {
                val filteredPlaylist = orderedSongsCache.filter { it.id != songId }
                if (filteredPlaylist.isEmpty()) {
                    activePlaylistId = null
                    orderedSongsCache = remainingLibrary
                    _playbackState.update { it.copy(activePlaylistId = null) }
                    prefs.edit().remove(KEY_ACTIVE_PLAYLIST_ID).apply()
                } else {
                    orderedSongsCache = filteredPlaylist
                }
            }
            remainingQueue = orderedSongsCache
            orderedSongIdsCache = remainingQueue.mapTo(HashSet(remainingQueue.size)) { it.id }
            _orderedSongs.value = remainingQueue
            rebuildShuffleQueueLocked(excludeSongId = current?.id)
        }
        if (current?.id == songId) {
            if (remainingQueue.isNotEmpty()) {
                playNext()
            } else {
                stopPlayback()
            }
        }
    }

    fun onSongRenamed(songId: Long, newTitle: String) {
        synchronized(playlistLock) {
            val updatedList = _songs.value.map { if (it.id == songId) it.copy(title = newTitle) else it }
            _songs.value = updatedList
            songsMap = updatedList.associateBy { it.id }
            orderedSongsCache = orderedSongsCache.map { if (it.id == songId) it.copy(title = newTitle) else it }
            orderedSongIdsCache = orderedSongsCache.mapTo(HashSet(orderedSongsCache.size)) { it.id }
            _orderedSongs.value = orderedSongsCache
        }
        val current = _playbackState.value.currentSong
        if (current?.id == songId) {
            val updated = current.copy(title = newTitle)
            _playbackState.update { it.copy(currentSong = updated) }
            sendServiceCommand(
                if (_playbackState.value.isPlaying) {
                    MusicPlaybackService.ACTION_PLAY
                } else {
                    MusicPlaybackService.ACTION_PAUSE
                }
            )
        }
    }

    fun togglePlayPause() {
        val currentState = _playbackState.value
        val currentSong = currentState.currentSong ?: getOrderedSongs().firstOrNull() ?: return

        if (currentState.currentSong == null) {
            playSongInternal(currentSong)
            return
        }

        if (currentState.isPlaying) {
            val currentPos = _currentPositionMs.value
            lastPersistedPositionMs = currentPos
            _playbackState.update { it.copy(isPlaying = false, currentPositionMs = currentPos) }
            prefs.edit().putBoolean(KEY_IS_PLAYING, false).putLong(KEY_POSITION_MS, currentPos).apply()
            sendServiceCommand(MusicPlaybackService.ACTION_PAUSE)
        } else {
            _playbackState.update { it.copy(isPlaying = true) }
            prefs.edit().putBoolean(KEY_IS_PLAYING, true).apply()
            sendServiceCommand(MusicPlaybackService.ACTION_RESUME)
        }
    }

    fun playNext() {
        if (_playbackState.value.playMode == PlayMode.SHUFFLE) {
            playNextShuffled()
            return
        }
        val songsList = getOrderedSongs()
        if (songsList.isEmpty()) return

        val currentId = _playbackState.value.currentSong?.id
        val currentIndex = songsList.indexOfFirst { it.id == currentId }
        val nextIndex = if (currentIndex in 0 until songsList.lastIndex) currentIndex + 1 else 0

        playSongInternal(songsList[nextIndex])
    }

    fun playPrevious() {
        if (_playbackState.value.playMode == PlayMode.SHUFFLE) {
            playNextShuffled()
            return
        }
        val songsList = getOrderedSongs()
        if (songsList.isEmpty()) return

        val currentId = _playbackState.value.currentSong?.id
        val currentIndex = songsList.indexOfFirst { it.id == currentId }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else songsList.lastIndex

        playSongInternal(songsList[prevIndex])
    }

    private fun playNextShuffled() {
        val nextSong: Song? = synchronized(playlistLock) {
            val songsList = orderedSongsCache
            if (songsList.isEmpty()) return@synchronized null
            if (songsList.size == 1) return@synchronized songsList[0]

            val currentId = _playbackState.value.currentSong?.id
            if (shuffleQueue.size != songsList.size || shuffleIndex !in shuffleQueue.indices) {
                rebuildShuffleQueueLocked(excludeSongId = currentId)
            }
            if (shuffleQueue.isEmpty()) {
                songsList.firstOrNull { it.id != currentId } ?: songsList.firstOrNull()
            } else {
                var candidateId = shuffleQueue.getOrNull(shuffleIndex)
                shuffleIndex++
                if (candidateId == currentId && songsList.size > 1) {
                    if (shuffleIndex !in shuffleQueue.indices) {
                        rebuildShuffleQueueLocked(excludeSongId = currentId)
                    }
                    candidateId = shuffleQueue.getOrNull(shuffleIndex)
                    shuffleIndex++
                }
                if (shuffleIndex >= shuffleQueue.size) {
                    rebuildShuffleQueueLocked(excludeSongId = candidateId ?: currentId)
                }
                val resolved = candidateId?.let { id ->
                    songsMap[id]?.takeIf { orderedSongIdsCache.contains(id) }
                        ?: songsList.firstOrNull { s -> s.id == id }
                }
                if (resolved != null && resolved.id != currentId) {
                    resolved
                } else {
                    songsList.filter { it.id != currentId }.randomOrNull(Random.Default)
                        ?: songsList.firstOrNull()
                }
            }
        }

        if (nextSong != null) {
            playSongInternal(nextSong)
        } else {
            _playbackState.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
            sendServiceCommand(MusicPlaybackService.ACTION_PAUSE)
        }
    }

    fun onSongCompletion() {
        val currentState = _playbackState.value
        val songsList = getOrderedSongs()
        if (songsList.isEmpty()) return

        when (currentState.playMode) {
            PlayMode.REPEAT_ONE -> currentState.currentSong?.let { playSongInternal(it) }
            PlayMode.REPEAT_ALL -> playNext()
            PlayMode.SHUFFLE -> playNextShuffled()
        }
    }

    fun cyclePlayMode() {
        val nextMode = _playbackState.value.playMode.next()
        if (nextMode == PlayMode.SHUFFLE) {
            synchronized(playlistLock) {
                rebuildShuffleQueueLocked(excludeSongId = _playbackState.value.currentSong?.id)
            }
        }
        _playbackState.update { it.copy(playMode = nextMode) }
        prefs.edit().putString(KEY_PLAY_MODE, nextMode.name).apply()
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
        lastPersistedPositionMs = positionMs
        lastProgressPersistTimeMs = System.currentTimeMillis()
        prefs.edit().putLong(KEY_POSITION_MS, positionMs).apply()
        val intent = Intent(appContext, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_SEEK
            putExtra(MusicPlaybackService.EXTRA_POSITION, positionMs)
        }
        try {
            appContext.startService(intent)
        } catch (_: Throwable) {
            // Service might not be active yet
        }
    }

    fun updateProgress(currentMs: Long, durationMs: Long) {
        _currentPositionMs.value = currentMs
        if (durationMs > 0 && durationMs != _playbackState.value.durationMs) {
            _playbackState.update { it.copy(durationMs = durationMs) }
        }
        val now = System.currentTimeMillis()
        if (now - lastProgressPersistTimeMs >= 5000L && abs(currentMs - lastPersistedPositionMs) >= 1000L) {
            lastProgressPersistTimeMs = now
            lastPersistedPositionMs = currentMs
            prefs.edit().putLong(KEY_POSITION_MS, currentMs).apply()
        }
    }

    fun updateIsPlaying(playing: Boolean) {
        if (_playbackState.value.isPlaying == playing) return
        _playbackState.update { it.copy(isPlaying = playing) }
        prefs.edit().putBoolean(KEY_IS_PLAYING, playing).apply()
    }

    fun toggleAudioOutputDevice() {
        val newTarget = _playbackState.value.outputDevice.toggle()
        _playbackState.update { it.copy(outputDevice = newTarget) }
        prefs.edit().putString(KEY_AUDIO_OUTPUT, newTarget.name).apply()
    }

    private fun detectInitialAudioOutput() {
        val hasHeadset = isHeadsetPluggedIn()
        val defaultDevice = if (hasHeadset) AudioOutputDevice.HEADPHONES else AudioOutputDevice.SPEAKER
        _playbackState.update {
            it.copy(
                outputDevice = defaultDevice,
                isHeadsetConnected = hasHeadset
            )
        }
    }

    fun isHeadsetDevice(info: AudioDeviceInfo): Boolean {
        return try {
            when (info.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_HEARING_AID,
                AudioDeviceInfo.TYPE_LINE_ANALOG,
                AudioDeviceInfo.TYPE_LINE_DIGITAL -> true
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (info.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            info.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                        ) return true
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (info.type == AudioDeviceInfo.TYPE_BLE_BROADCAST) return true
                    }
                    false
                }
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun isHeadsetPluggedIn(): Boolean {
        val am = audioManager ?: return false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                if (devices != null && devices.any { isHeadsetDevice(it) }) {
                    return true
                }
            }
            @Suppress("DEPRECATION")
            if (am.isWiredHeadsetOn) return true
            @Suppress("DEPRECATION")
            try {
                if (am.isBluetoothA2dpOn || am.isBluetoothScoOn) return true
            } catch (_: Throwable) {
                // BLUETOOTH_CONNECT permission not granted
            }
        } catch (_: Throwable) {
            // Ignore
        }
        return false
    }

    fun clearCommunicationRouting() {
        // Không can thiệp vào AudioManager mode hay speakerphone để Android toàn quyền quyết định âm lượng và đường truyền.
    }

    fun applyAudioOutput(target: AudioOutputDevice) {
        // Để hệ điều hành Android tự điều phối đầu ra âm thanh mặc định.
    }

    fun setBassStrength(strength: Int, persist: Boolean = true) {
        val clamped = strength.coerceIn(0, 1000)
        if (_bassStrength.value != clamped) {
            _bassStrength.value = clamped
        }
        if (persist && lastPersistedBass != clamped) {
            lastPersistedBass = clamped
            prefs.edit().putInt(KEY_BASS_STRENGTH, clamped).apply()
        }
    }

    fun setTrebleStrength(strength: Int, persist: Boolean = true) {
        val clamped = strength.coerceIn(0, 1000)
        if (_trebleStrength.value != clamped) {
            _trebleStrength.value = clamped
        }
        if (persist && lastPersistedTreble != clamped) {
            lastPersistedTreble = clamped
            prefs.edit().putInt(KEY_TREBLE_STRENGTH, clamped).apply()
        }
    }

    fun setVocalStrength(strength: Int, persist: Boolean = true) {
        val clamped = strength.coerceIn(0, 1000)
        if (_vocalStrength.value != clamped) {
            _vocalStrength.value = clamped
        }
        if (persist && lastPersistedVocal != clamped) {
            lastPersistedVocal = clamped
            prefs.edit().putInt(KEY_VOCAL_STRENGTH, clamped).apply()
        }
    }

    fun setVirtualizerStrength(strength: Int, persist: Boolean = true) {
        val clamped = strength.coerceIn(0, 1000)
        if (_virtualizerStrength.value != clamped) {
            _virtualizerStrength.value = clamped
        }
        if (persist && lastPersistedVirtualizer != clamped) {
            lastPersistedVirtualizer = clamped
            prefs.edit().putInt(KEY_VIRTUALIZER_STRENGTH, clamped).apply()
        }
    }

    private fun sendServiceCommand(actionStr: String) {
        try {
            val intent = Intent(appContext, MusicPlaybackService::class.java).apply {
                action = actionStr
            }
            if (actionStr == MusicPlaybackService.ACTION_PLAY || actionStr == MusicPlaybackService.ACTION_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        appContext.startForegroundService(intent)
                    } catch (_: Throwable) {
                        appContext.startService(intent)
                    }
                } else {
                    appContext.startService(intent)
                }
            } else {
                appContext.startService(intent)
            }
        } catch (_: Throwable) {
            // Background execution restriction or service start exception fallback
        }
    }

    companion object {
        private const val PREFS_NAME = "music_player_playback_prefs"
        private const val KEY_LAST_SONG_ID = "pref_last_song_id"
        private const val KEY_POSITION_MS = "pref_position_ms"
        private const val KEY_IS_PLAYING = "pref_is_playing"
        private const val KEY_PLAY_MODE = "pref_play_mode"
        private const val KEY_AUDIO_OUTPUT = "pref_audio_output"
        private const val KEY_BASS_STRENGTH = "pref_bass_strength"
        private const val KEY_TREBLE_STRENGTH = "pref_treble_strength"
        private const val KEY_VOCAL_STRENGTH = "pref_vocal_strength"
        private const val KEY_VIRTUALIZER_STRENGTH = "pref_virtualizer_strength"
        private const val KEY_ACTIVE_PLAYLIST_ID = "pref_active_playlist_id"
        const val DEFAULT_BASS_STRENGTH = 800
        const val DEFAULT_TREBLE_STRENGTH = 600
        const val DEFAULT_VOCAL_STRENGTH = 500
        const val DEFAULT_VIRTUALIZER_STRENGTH = 400

        @Volatile
        private var INSTANCE: PlaybackManager? = null

        fun getInstance(context: Context): PlaybackManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlaybackManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
