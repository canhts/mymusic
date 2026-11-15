package com.example.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.text.Normalizer

@Immutable
data class Song(
    val id: Long,
    val title: String,
    val uri: Uri,
    val duration: Long,
    val dateAdded: Long,
    val path: String? = null,
    val artist: String = ""
) {
    val displayTitle: String = formatDisplayTitle(title)

    @delegate:Transient
    internal val normalizedDisplayTitle: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        normalizeForSearch(displayTitle)
    }

    @delegate:Transient
    internal val normalizedRawTitle: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        normalizeForSearch(title)
    }

    internal companion object {
        private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")

        internal fun normalizeForSearch(input: String): String {
            if (input.isEmpty()) return ""
            val nfd = Normalizer.normalize(input.lowercase(), Normalizer.Form.NFD)
            return DIACRITICS_REGEX.replace(nfd, "")
                .replace('đ', 'd')
        }

        fun formatDisplayTitle(raw: String): String {
            val trimmed = raw.trim()
            val end = if (trimmed.endsWith(".mp3", ignoreCase = true)) {
                trimmed.length - 4
            } else {
                trimmed.length
            }
            if (end <= 0) return ""

            // Trim trailing spaces if any existed before ".mp3"
            var actualEnd = end
            while (actualEnd > 0 && trimmed[actualEnd - 1].isWhitespace()) {
                actualEnd--
            }
            if (actualEnd <= 0) return ""

            val clean = if (actualEnd == trimmed.length) trimmed else trimmed.substring(0, actualEnd)
            val lower = clean.lowercase()
            return lower.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase() else ch.toString()
            }
        }
    }
}

enum class PlayMode {
    REPEAT_ONE,
    REPEAT_ALL,
    SHUFFLE;

    fun next(): PlayMode = when (this) {
        REPEAT_ONE -> REPEAT_ALL
        REPEAT_ALL -> SHUFFLE
        SHUFFLE -> REPEAT_ONE
    }
}

enum class AudioOutputDevice {
    HEADPHONES,
    SPEAKER;

    fun toggle(): AudioOutputDevice = if (this == HEADPHONES) SPEAKER else HEADPHONES
}

@Immutable
data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playMode: PlayMode = PlayMode.REPEAT_ALL,
    val outputDevice: AudioOutputDevice = AudioOutputDevice.SPEAKER,
    val isHeadsetConnected: Boolean = false,
    val activePlaylistId: Long? = null
)
