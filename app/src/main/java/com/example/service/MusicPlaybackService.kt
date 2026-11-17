package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.IntentCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.example.MainActivity
import com.example.R
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicPlaybackService : Service(),
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnPreparedListener,
    AudioManager.OnAudioFocusChangeListener {

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var progressJob: Job? = null

    @Volatile
    private var mediaPlayer: MediaPlayer? = null
    private var prepareGeneration: Int = 0
    @Volatile
    private var cachedPlayerDurationMs: Long = 0L
    private lateinit var playbackManager: PlaybackManager
    private var audioManager: AudioManager? = null
    private var mediaSession: MediaSessionCompat? = null

    // Cached PendingIntents to avoid repeated Binder IPC allocations on notification updates
    private val contentPendingIntent: PendingIntent by lazy {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val prevPendingIntent: PendingIntent by lazy {
        PendingIntent.getService(
            this,
            1,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val pausePendingIntent: PendingIntent by lazy {
        PendingIntent.getService(
            this,
            2,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val resumePendingIntent: PendingIntent by lazy {
        PendingIntent.getService(
            this,
            5,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_RESUME },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val nextPendingIntent: PendingIntent by lazy {
        PendingIntent.getService(
            this,
            3,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val stopPendingIntent: PendingIntent by lazy {
        PendingIntent.getService(
            this,
            4,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private var lastMetadataSongId: Long = -1L
    private var lastMetadataTitle: String? = null

    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var resumeOnFocusGain = false

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pauseMusic()
            }
        }
    }
    private var isNoisyReceiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        playbackManager = PlaybackManager.getInstance(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        setupMediaSession()
        createNotificationChannel()
        registerNoisyReceiver()
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicPlaybackServiceSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                    val keyEvent = mediaButtonEvent?.let {
                        IntentCompat.getParcelableExtra(it, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    }
                    if (keyEvent != null) {
                        val isTargetKey = when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_NEXT,
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                            KeyEvent.KEYCODE_MEDIA_PLAY,
                            KeyEvent.KEYCODE_MEDIA_PAUSE,
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> true
                            else -> false
                        }
                        if (isTargetKey) {
                            if (keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.repeatCount == 0) {
                                when (keyEvent.keyCode) {
                                    KeyEvent.KEYCODE_MEDIA_NEXT -> playbackManager.playNext()
                                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> playbackManager.playPrevious()
                                    KeyEvent.KEYCODE_MEDIA_PLAY -> resumeMusic()
                                    KeyEvent.KEYCODE_MEDIA_PAUSE -> pauseMusic()
                                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> playbackManager.togglePlayPause()
                                }
                            }
                            return true
                        }
                    }
                    return super.onMediaButtonEvent(mediaButtonEvent)
                }

                override fun onPlay() = resumeMusic()
                override fun onPause() = pauseMusic()
                override fun onSkipToNext() = playbackManager.playNext()
                override fun onSkipToPrevious() = playbackManager.playPrevious()
                override fun onSeekTo(pos: Long) = seekToPosition(pos)
                override fun onStop() = stopMusicService()
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundWithNotification(playbackManager.playbackState.value.isPlaying)
        }

        when (intent?.action) {
            ACTION_PLAY -> {
                playbackManager.playbackState.value.currentSong?.let { playSong(it) }
            }
            ACTION_PAUSE -> pauseMusic()
            ACTION_RESUME -> resumeMusic()
            ACTION_NEXT -> playbackManager.playNext()
            ACTION_PREV -> playbackManager.playPrevious()
            ACTION_SEEK -> {
                val position = intent.getLongExtra(EXTRA_POSITION, 0L)
                seekToPosition(position)
            }
            ACTION_STOP -> stopMusicService()
        }
        return START_NOT_STICKY
    }

    private fun releasePlayerSafely(player: MediaPlayer?) {
        if (player == null) return
        try {
            player.setOnPreparedListener(null)
            player.setOnCompletionListener(null)
            player.setOnErrorListener(null)
        } catch (_: Throwable) {
        }
        try {
            if (player.isPlaying) {
                player.stop()
            }
        } catch (_: Throwable) {
        }
        try {
            player.reset()
        } catch (_: Throwable) {
        }
        try {
            player.release()
        } catch (_: Throwable) {
        }
    }

    private fun playSong(song: Song) {
        requestAudioFocus()
        updateMediaSessionMetadata(song)
        updateMediaSessionState(true, 0L)

        val currentGen = ++prepareGeneration
        cachedPlayerDurationMs = song.duration.coerceAtLeast(0L)

        try {
            stopProgressTracker()
            val oldPlayer = mediaPlayer
            mediaPlayer = null
            if (oldPlayer != null) {
                serviceScope.launch(Dispatchers.IO) {
                    releasePlayerSafely(oldPlayer)
                }
            }

            val player = MediaPlayer()
            mediaPlayer = player
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            player.setDataSource(applicationContext, song.uri)
            player.setOnPreparedListener { preparedMp ->
                if (prepareGeneration == currentGen) {
                    onPrepared(preparedMp)
                } else {
                    serviceScope.launch(Dispatchers.IO) {
                        releasePlayerSafely(preparedMp)
                    }
                }
            }
            player.setOnCompletionListener(this@MusicPlaybackService)
            player.setOnErrorListener(this@MusicPlaybackService)
            player.prepareAsync()
        } catch (_: Throwable) {
            playbackManager.updateIsPlaying(false)
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        if (mp == null || mp !== mediaPlayer) return

        val seekPos = playbackManager.playbackState.value.currentPositionMs
        val durationMs = try {
            mp.duration.toLong()
        } catch (_: Throwable) {
            0L
        }
        if (durationMs > 0L) {
            cachedPlayerDurationMs = durationMs
        }

        if (seekPos > 0 && seekPos < durationMs) {
            try {
                mp.seekTo(seekPos.toInt())
            } catch (_: Throwable) {
            }
        }

        val shouldPlay = playbackManager.playbackState.value.isPlaying
        if (shouldPlay) {
            try {
                mp.start()
                playbackManager.updateIsPlaying(true)
                startForegroundWithNotification(true)
                startProgressTracker()
            } catch (_: Throwable) {
                playbackManager.updateIsPlaying(false)
            }
        } else {
            playbackManager.updateIsPlaying(false)
            startForegroundWithNotification(false)
        }
        playbackManager.updateProgress(if (seekPos > 0) seekPos else 0L, durationMs)
    }

    private fun pauseMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Throwable) {
            // Ignore
        }
        playbackManager.updateIsPlaying(false)
        stopProgressTracker()
        startForegroundWithNotification(false)
    }

    private fun resumeMusic() {
        requestAudioFocus()
        val mp = mediaPlayer
        if (mp != null) {
            try {
                mp.start()
                playbackManager.updateIsPlaying(true)
                startForegroundWithNotification(true)
                startProgressTracker()
            } catch (_: Throwable) {
                playbackManager.updateIsPlaying(false)
            }
        } else {
            val song = playbackManager.playbackState.value.currentSong
            if (song != null) {
                playSong(song)
            }
        }
    }

    private fun seekToPosition(positionMs: Long) {
        val mp = mediaPlayer ?: return
        try {
            mp.seekTo(positionMs.toInt())
            val duration = mp.duration.toLong()
            playbackManager.updateProgress(positionMs, duration)
            updateMediaSessionState(mp.isPlaying, positionMs)
        } catch (_: Exception) {
            // Ignore
        }
    }

    private fun stopMusicService() {
        stopProgressTracker()
        val oldPlayer = mediaPlayer
        mediaPlayer = null
        serviceScope.launch(Dispatchers.IO) {
            releasePlayerSafely(oldPlayer)
        }
        mediaSession?.isActive = false
        playbackManager.updateIsPlaying(false)
        abandonAudioFocus()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        stopProgressTracker()
        playbackManager.onSongCompletion()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        stopProgressTracker()
        if (mp === mediaPlayer) {
            mediaPlayer = null
        }
        serviceScope.launch(Dispatchers.IO) {
            releasePlayerSafely(mp)
        }
        playbackManager.updateIsPlaying(false)
        return true
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        val am = audioManager ?: return false

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(this)
                .build()

            audioFocusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        return true
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        val am = audioManager
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(this)
            }
        }
        hasAudioFocus = false
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                resumeOnFocusGain = false
                pauseMusic()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                if (mediaPlayer?.isPlaying == true) {
                    resumeOnFocusGain = true
                    pauseMusic()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Không can thiệp hạ âm lượng riêng của MediaPlayer, để hệ điều hành Android tự quản lý âm lượng.
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    resumeMusic()
                }
            }
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = serviceScope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    val mp = mediaPlayer
                    if (mp != null && mp.isPlaying) {
                        val current = mp.currentPosition.toLong()
                        var total = cachedPlayerDurationMs
                        if (total <= 0L) {
                            total = mp.duration.toLong()
                            if (total > 0L) cachedPlayerDurationMs = total
                        }
                        playbackManager.updateProgress(current, total)
                    }
                } catch (_: Exception) {
                }
                delay(PROGRESS_POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updateMediaSessionState(isPlaying: Boolean, currentPositionMs: Long) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackSpeed = if (isPlaying) 1.0f else 0.0f
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, currentPositionMs, playbackSpeed)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    private fun updateMediaSessionMetadata(song: Song) {
        val displayTitle = song.displayTitle
        if (lastMetadataSongId == song.id && lastMetadataTitle == displayTitle) return
        lastMetadataSongId = song.id
        lastMetadataTitle = displayTitle

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, displayTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getString(R.string.app_name))
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
            .build()
        mediaSession?.setMetadata(metadata)
    }

    private fun startForegroundWithNotification(isPlaying: Boolean) {
        val currentSong = playbackManager.playbackState.value.currentSong
        val currentPos = try {
            mediaPlayer?.currentPosition?.toLong()
        } catch (_: Throwable) {
            null
        } ?: playbackManager.playbackState.value.currentPositionMs

        val notification = if (currentSong != null) {
            updateMediaSessionMetadata(currentSong)
            updateMediaSessionState(isPlaying, currentPos)
            buildNotification(currentSong, isPlaying)
        } else {
            buildIdleNotification()
        }

        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                foregroundServiceType
            )
        } catch (_: Exception) {
            // Foreground service start exception fallback
        }
    }

    private fun buildIdleNotification(): Notification {
        val appName = getString(R.string.app_name)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_music)
            .setContentTitle(appName)
            .setContentText(appName)
            .setContentIntent(contentPendingIntent)
            .setOngoing(false)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun buildNotification(song: Song, isPlaying: Boolean): Notification {
        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseTitle = if (isPlaying) getString(R.string.action_pause) else getString(R.string.action_play)
        val toggleIntent = if (isPlaying) pausePendingIntent else resumePendingIntent

        val mediaStyle = MediaStyle().setShowActionsInCompactView(0, 1, 2)
        mediaSession?.sessionToken?.let { token ->
            mediaStyle.setMediaSession(token)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_music)
            .setContentTitle(song.displayTitle)
            .setContentText(getString(R.string.app_name))
            .setContentIntent(contentPendingIntent)
            .setOngoing(isPlaying)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(mediaStyle)
            .addAction(android.R.drawable.ic_media_previous, getString(R.string.action_prev), prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, toggleIntent)
            .addAction(android.R.drawable.ic_media_next, getString(R.string.action_next), nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.action_close), stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun registerNoisyReceiver() {
        if (!isNoisyReceiverRegistered) {
            registerReceiver(
                becomingNoisyReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            )
            isNoisyReceiverRegistered = true
        }
    }

    private fun unregisterNoisyReceiver() {
        if (isNoisyReceiverRegistered) {
            try {
                unregisterReceiver(becomingNoisyReceiver)
            } catch (_: Exception) {
            }
            isNoisyReceiverRegistered = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProgressTracker()
        serviceScope.cancel()
        unregisterNoisyReceiver()
        abandonAudioFocus()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        releasePlayerSafely(mediaPlayer)
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val PROGRESS_POLL_INTERVAL_MS = 25L

        const val CHANNEL_ID = "music_player_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.action.PLAY"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_RESUME = "com.example.action.RESUME"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PREV = "com.example.action.PREV"
        const val ACTION_SEEK = "com.example.action.SEEK"
        const val ACTION_SET_BASS = "com.example.action.SET_BASS"
        const val ACTION_SET_AUDIO_FX = "com.example.action.SET_AUDIO_FX"
        const val ACTION_STOP = "com.example.action.STOP"

        const val EXTRA_POSITION = "extra_position"
        const val EXTRA_BASS_STRENGTH = "extra_bass_strength"
    }
}
