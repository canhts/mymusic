package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY sortOrder ASC, createdAt ASC, id ASC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_songs ORDER BY position ASC, addedAt ASC")
    fun observeAllPlaylistSongs(): Flow<List<PlaylistSongEntity>>

    @Query("SELECT MAX(sortOrder) FROM playlists")
    suspend fun getMaxPlaylistSortOrder(): Int?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun updatePlaylistName(playlistId: Long, newName: String)

    @Query("UPDATE playlists SET sortOrder = :sortOrder WHERE id = :playlistId")
    suspend fun updatePlaylistSortOrder(playlistId: Long, sortOrder: Int)

    @Query("UPDATE playlists SET playCount = playCount + 1 WHERE id = :playlistId")
    suspend fun incrementPlayCount(playlistId: Long)

    @Transaction
    suspend fun reorderPlaylists(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, playlistId ->
            updatePlaylistSortOrder(playlistId, index)
        }
    }

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    @Query("SELECT MAX(position) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSongs(entries: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_songs WHERE songId = :songId")
    suspend fun removeSongFromAllPlaylists(songId: Long)
}
