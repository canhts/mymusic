package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "lyrics_cache",
    indices = [
        Index(value = ["songId"])
    ]
)
data class LyricsCacheEntity(
    @PrimaryKey val cacheKey: String,
    val songId: Long,
    val songTitle: String,
    val artistName: String,
    val source: String,
    val isSynced: Boolean,
    val linesJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface LyricsCacheDao {
    @Query("SELECT * FROM lyrics_cache WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getByCacheKey(cacheKey: String): LyricsCacheEntity?

    @Query("SELECT * FROM lyrics_cache WHERE songId = :songId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getBySongId(songId: Long): LyricsCacheEntity?

    @Query("SELECT * FROM lyrics_cache WHERE songId = :songId ORDER BY updatedAt DESC LIMIT 1")
    fun observeBySongId(songId: Long): Flow<LyricsCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(entity: LyricsCacheEntity)

    @Query("DELETE FROM lyrics_cache WHERE cacheKey = :cacheKey")
    suspend fun deleteByCacheKey(cacheKey: String)

    @Query("DELETE FROM lyrics_cache WHERE songId = :songId")
    suspend fun deleteBySongId(songId: Long)
}
