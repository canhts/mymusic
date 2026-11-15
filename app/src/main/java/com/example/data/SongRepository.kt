package com.example.data

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.database.ContentObserver
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.model.Song
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class StorageOpResult {
    data object Success : StorageOpResult()
    data class RequiresUserConsent(val intentSender: IntentSender) : StorageOpResult()
    data class Failure(val message: String) : StorageOpResult()
}

class SongRepository(context: Context) {

    private val appContext: Context = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver

    fun getSongsFlow(): Flow<List<Song>> = callbackFlow {
        val refreshSignal = MutableSharedFlow<Unit>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                refreshSignal.tryEmit(Unit)
            }
        }

        val collectorJob = launch(Dispatchers.IO) {
            refreshSignal.collectLatest {
                delay(OBSERVER_DEBOUNCE_MS)
                trySend(querySongs())
            }
        }

        try {
            contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        } catch (_: SecurityException) {
            // Permission not granted yet during observer registration
        } catch (_: Exception) {
            // Ignore registration failures on restricted devices
        }

        // Send initial songs list immediately
        trySend(querySongs())

        awaitClose {
            collectorJob.cancel()
            try {
                contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {
                // Ignore
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun loadSongsOnce(): List<Song> = withContext(Dispatchers.IO) {
        querySongs()
    }

    private fun querySongs(): List<Song> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ARTIST
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        return try {
            contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val count = cursor.count
                if (count <= 0) return@use emptyList()

                val songsList = ArrayList<Song>(count)
                val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val nameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val dateAddedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)

                while (cursor.moveToNext()) {
                    val id = if (idColumn >= 0) cursor.getLong(idColumn) else 0L
                    val rawTitle = if (titleColumn >= 0) cursor.getString(titleColumn).orEmpty() else ""
                    val displayName = if (nameColumn >= 0) cursor.getString(nameColumn).orEmpty() else ""
                    val duration = if (durationColumn >= 0) cursor.getLong(durationColumn) else 0L
                    val dateAdded = if (dateAddedColumn >= 0) cursor.getLong(dateAddedColumn) else 0L
                    val filePath = if (dataColumn >= 0) cursor.getString(dataColumn) else null
                    val rawArtist = if (artistColumn >= 0) cursor.getString(artistColumn).orEmpty().trim() else ""
                    val cleanArtist = if (rawArtist.equals("<unknown>", ignoreCase = true)) "" else rawArtist

                    val cleanTitle = when {
                        displayName.isNotBlank() -> displayName.trim()
                        rawTitle.isNotBlank() -> rawTitle.trim()
                        else -> FALLBACK_UNTITLED
                    }

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    songsList.add(
                        Song(
                            id = id,
                            title = cleanTitle,
                            uri = contentUri,
                            duration = duration,
                            dateAdded = dateAdded,
                            path = filePath,
                            artist = cleanArtist
                        )
                    )
                }
                songsList
            } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun renameSong(
        song: Song,
        newTitle: String,
        userGrantedConsent: Boolean = false
    ): StorageOpResult = withContext(Dispatchers.IO) {
        val trimmed = newTitle.trim()
        val existingExt = if (song.title.contains('.')) {
            song.title.substringAfterLast('.').ifBlank { DEFAULT_EXT }
        } else {
            DEFAULT_EXT
        }

        val finalFileName = if (trimmed.endsWith(".$existingExt", ignoreCase = true)) {
            trimmed
        } else {
            "$trimmed.$existingExt"
        }

        val baseTitle = finalFileName.substringBeforeLast('.')

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !userGrantedConsent) {
            try {
                val pendingIntent = MediaStore.createWriteRequest(contentResolver, listOf(song.uri))
                return@withContext StorageOpResult.RequiresUserConsent(pendingIntent.intentSender)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Fallback to direct update
            }
        }

        val values = ContentValues(2).apply {
            put(MediaStore.Audio.Media.TITLE, baseTitle)
            put(MediaStore.Audio.Media.DISPLAY_NAME, finalFileName)
        }

        var fileRenamed = false
        song.path?.let { path ->
            try {
                val file = File(path)
                val parent = file.parentFile
                if (file.exists() && parent != null) {
                    val newFile = File(parent, finalFileName)
                    if (file.renameTo(newFile)) {
                        fileRenamed = true
                        MediaScannerConnection.scanFile(
                            appContext,
                            arrayOf(newFile.absolutePath, file.absolutePath),
                            null,
                            null
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Ignore file system rename error, proceed with MediaStore update
            }
        }

        try {
            val rows = contentResolver.update(song.uri, values, null, null)
            if (rows > 0 || fileRenamed) {
                StorageOpResult.Success
            } else {
                StorageOpResult.Failure("Không thể cập nhật tên bài hát")
            }
        } catch (recoverable: RecoverableSecurityException) {
            StorageOpResult.RequiresUserConsent(recoverable.userAction.actionIntent.intentSender)
        } catch (securityEx: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !userGrantedConsent) {
                try {
                    val pendingIntent = MediaStore.createWriteRequest(contentResolver, listOf(song.uri))
                    StorageOpResult.RequiresUserConsent(pendingIntent.intentSender)
                } catch (_: Exception) {
                    StorageOpResult.Failure("Không có quyền chỉnh sửa file: ${securityEx.localizedMessage}")
                }
            } else {
                StorageOpResult.Failure("Không có quyền chỉnh sửa file")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            StorageOpResult.Failure(e.localizedMessage ?: "Lỗi khi đổi tên")
        }
    }

    suspend fun deleteSong(song: Song): StorageOpResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, listOf(song.uri))
                return@withContext StorageOpResult.RequiresUserConsent(pendingIntent.intentSender)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Fallback to direct delete
            }
        }

        try {
            val rows = contentResolver.delete(song.uri, null, null)
            if (rows > 0) {
                StorageOpResult.Success
            } else {
                StorageOpResult.Failure("Không thể xóa bài hát")
            }
        } catch (recoverable: RecoverableSecurityException) {
            StorageOpResult.RequiresUserConsent(recoverable.userAction.actionIntent.intentSender)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            StorageOpResult.Failure(e.localizedMessage ?: "Lỗi khi xóa bài hát")
        }
    }

    private companion object {
        const val OBSERVER_DEBOUNCE_MS = 200L
        const val DEFAULT_EXT = "mp3"
        const val FALLBACK_UNTITLED = "Bài hát không tên"
    }
}
