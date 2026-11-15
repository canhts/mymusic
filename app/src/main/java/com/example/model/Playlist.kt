package com.example.model

import androidx.compose.runtime.Immutable

@Immutable
data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songs: List<Song>,
    val playCount: Int = 0
) {
    val totalDurationMs: Long = songs.sumOf { it.duration.coerceAtLeast(0L) }

    @delegate:Transient
    internal val normalizedName: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Song.normalizeForSearch(name)
    }
}

