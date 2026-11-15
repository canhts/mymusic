package com.example.data

import com.example.data.local.PlaylistDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongEntity
import com.example.model.Playlist
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class PlaylistRepository(private val playlistDao: PlaylistDao) {

    fun observePlaylistsWithSongs(songsFlow: Flow<List<Song>>): Flow<List<Playlist>> {
        return combine(
            playlistDao.observePlaylists(),
            playlistDao.observeAllPlaylistSongs(),
            songsFlow
        ) { playlistEntities, songRefs, allSongs ->
            if (playlistEntities.isEmpty()) {
                return@combine emptyList()
            }
            val songsById = allSongs.associateBy { it.id }
            val refsByPlaylist = songRefs.groupBy { it.playlistId }

            playlistEntities.map { entity ->
                val refs = refsByPlaylist[entity.id].orEmpty()
                val resolvedSongs = refs.mapNotNull { ref -> songsById[ref.songId] }
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    createdAt = entity.createdAt,
                    songs = resolvedSongs,
                    playCount = entity.playCount
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Playlist name cannot be blank" }
        val nextOrder = (playlistDao.getMaxPlaylistSortOrder() ?: -1) + 1
        playlistDao.insertPlaylist(
            PlaylistEntity(name = cleanName, sortOrder = nextOrder)
        )
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        val cleanName = newName.trim()
        require(cleanName.isNotEmpty()) { "Playlist name cannot be blank" }
        playlistDao.updatePlaylistName(playlistId, cleanName)
    }

    suspend fun reorderPlaylists(orderedIds: List<Long>) = withContext(Dispatchers.IO) {
        if (orderedIds.isEmpty()) return@withContext
        playlistDao.reorderPlaylists(orderedIds)
    }

    suspend fun incrementPlayCount(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.incrementPlayCount(playlistId)
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistById(playlistId)
    }

    suspend fun addSongToPlaylists(songId: Long, playlistIds: List<Long>) = withContext(Dispatchers.IO) {
        if (playlistIds.isEmpty()) return@withContext
        val now = System.currentTimeMillis()
        for (playlistId in playlistIds.distinct()) {
            val nextPos = (playlistDao.getMaxPosition(playlistId) ?: -1) + 1
            playlistDao.insertPlaylistSongs(
                listOf(
                    PlaylistSongEntity(
                        playlistId = playlistId,
                        songId = songId,
                        position = nextPos,
                        addedAt = now
                    )
                )
            )
        }
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) = withContext(Dispatchers.IO) {
        if (songIds.isEmpty()) return@withContext
        val now = System.currentTimeMillis()
        var nextPos = (playlistDao.getMaxPosition(playlistId) ?: -1) + 1
        val entries = songIds.distinct().map { songId ->
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                position = nextPos++,
                addedAt = now
            )
        }
        playlistDao.insertPlaylistSongs(entries)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun removeSongFromAllPlaylists(songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromAllPlaylists(songId)
    }
}
