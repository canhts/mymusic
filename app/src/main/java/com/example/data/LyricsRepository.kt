package com.example.data

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.example.R
import com.example.data.local.LyricsCacheDao
import com.example.data.local.LyricsCacheEntity
import com.example.model.LyricLine
import com.example.model.LyricWordSegment
import com.example.model.LyricsSource
import com.example.model.LyricsUiState
import com.example.model.OfflineModelState
import com.example.model.Song
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Thông tin metadata đã làm sạch từ file MP3 phục vụ đối chiếu lời bài hát.
 */
data class CleanedTrackMetadata(
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val durationMs: Long,
    val candidatePairs: List<Pair<String, String>> = emptyList()
)

/**
 * Giao diện cung cấp lời bài hát từ nguồn trực tuyến (LRCLIB + nguồn dự phòng).
 */
fun interface LrcLibRemoteClient {
    suspend fun fetchLyrics(metadata: CleanedTrackMetadata): RemoteLyricsResponse?
}

data class RemoteLyricsResponse(
    val syncedLyrics: String?,
    val plainLyrics: String?
)

/**
 * Module độc lập quản lý Kiến trúc Lai 3 tầng (3-Tier Hybrid Lyrics Pipeline) & Cache cục bộ:
 * - Ưu tiên 0 (0ms): Đọc cache JSON từ Room Database (`lyrics_cache`) + bộ nhớ đệm trong RAM.
 *   Tự động lọc sạch mọi dòng nhạc dạo / giang tấu / ký hiệu âm nhạc, chỉ giữ lại lời bài hát thực sự.
 * - Tầng 1: Đọc trực tiếp khung `SYLT` / `USLT` / `COMM` trong thẻ ID3v2 nhúng sẵn của file MP3 (hoặc file `.lrc` đi kèm).
 * - Tầng 2: Trích xuất metadata bằng `MediaStore` + `MediaMetadataRetriever` (FileDescriptor) + thẻ ID3 + bộ phân tích tên file
 *           và đối chiếu qua API mở **LRCLIB** (`/api/search` đa truy vấn) cùng nguồn LRC dự phòng.
 * - Tầng 3: Giải mã MP3 sang PCM 16kHz Mono + lọc thông dải `300Hz – 3400Hz` + AI Speech-to-Text Offline.
 */
class LyricsRepository(
    context: Context,
    private val lyricsCacheDao: LyricsCacheDao,
    val offlineEngine: OfflineSpeechLyricsEngine = OfflineSpeechLyricsEngine(context),
    private val remoteClient: LrcLibRemoteClient = DefaultLrcLibRemoteClient()
) {
    private val appContext = context.applicationContext
    private val memoryCache = ConcurrentHashMap<Long, LyricsUiState.Success>()

    private val _uiState = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    val offlineModelState: StateFlow<OfflineModelState> = offlineEngine.modelState

    private var activeSongId: Long? = null

    @Volatile
    private var activeLocale: Locale = Locale.forLanguageTag("vi-VN")

    @Volatile
    private var cachedLocaleKey: String? = null

    @Volatile
    private var cachedLocalizedResources: Resources? = null

    fun setLocale(locale: Locale) {
        activeLocale = locale
    }

    private fun localizedResources(): Resources {
        val locale = activeLocale
        val baseConfig = appContext.resources.configuration
        val key = "${locale.toLanguageTag()}_${baseConfig.uiMode}"
        cachedLocalizedResources?.let { cached ->
            if (cachedLocaleKey == key) return cached
        }
        val config = Configuration(baseConfig).apply {
            setLocale(locale)
        }
        val res = appContext.createConfigurationContext(config).resources
        cachedLocalizedResources = res
        cachedLocaleKey = key
        return res
    }

    /**
     * Tải lời bài hát cho bài đang phát (ưu tiên Cache cục bộ 0ms trước, sau đó chạy 3 tầng).
     */
    suspend fun loadLyricsForSong(song: Song?, forceRescan: Boolean = false) {
        if (song == null) {
            activeSongId = null
            _uiState.value = LyricsUiState.Idle
            return
        }

        activeSongId = song.id

        if (!forceRescan) {
            memoryCache[song.id]?.let { cachedSuccess ->
                _uiState.value = cachedSuccess.copy(fromCache = true)
                return
            }
        }

        withContext(Dispatchers.IO) {
            val cacheKey = computeMp3CacheKey(song)

            if (forceRescan) {
                memoryCache.remove(song.id)
                runCatching {
                    lyricsCacheDao.deleteByCacheKey(cacheKey)
                    lyricsCacheDao.deleteBySongId(song.id)
                }
            } else {
                // Ưu tiên 0: Kiểm tra Room Database Cache cục bộ
                val cachedEntity = runCatching {
                    lyricsCacheDao.getByCacheKey(cacheKey) ?: lyricsCacheDao.getBySongId(song.id)
                }.getOrNull()

                if (cachedEntity != null) {
                    val rawCachedLines = deserializeLinesFromJson(cachedEntity.linesJson)
                    val filteredLines = filterOnlyVocalLines(rawCachedLines)
                    val isLegacySynthetic = isLegacySyntheticOfflinePlaceholder(rawCachedLines)

                    if (isLegacySynthetic) {
                        runCatching {
                            lyricsCacheDao.deleteByCacheKey(cachedEntity.cacheKey)
                            lyricsCacheDao.deleteBySongId(song.id)
                        }
                    } else if (filteredLines.isNotEmpty()) {
                        val successState = LyricsUiState.Success(
                            songId = song.id,
                            cacheKey = cachedEntity.cacheKey,
                            songTitle = cachedEntity.songTitle.ifBlank { song.displayTitle },
                            artistName = cachedEntity.artistName,
                            lines = filteredLines,
                            source = LyricsSource.fromId(cachedEntity.source),
                            isSynced = cachedEntity.isSynced,
                            fromCache = true
                        )
                        memoryCache[song.id] = successState
                        if (activeSongId == song.id) {
                            _uiState.value = successState
                        }
                        return@withContext
                    }
                }
            }

            // Tầng 1: Đọc Metadata nhúng sẵn trong file MP3 (ID3v2 SYLT / USLT / COMM)
            if (activeSongId == song.id) {
                _uiState.value = LyricsUiState.Loading(
                    statusText = localizedResources().getString(R.string.lyrics_loading_tier1),
                    tier = 1
                )
            }

            val embedded = Id3v2LyricsExtractor.extractFromSong(appContext, song)
            val metadata = extractTrackMetadata(
                song = song,
                id3Title = embedded?.embeddedTitle,
                id3Artist = embedded?.embeddedArtist
            )

            if (embedded != null) {
                val cleanEmbeddedLines = filterOnlyVocalLines(embedded.lines)
                if (cleanEmbeddedLines.isNotEmpty()) {
                    val success = persistAndBuildSuccess(
                        song = song,
                        cacheKey = cacheKey,
                        metadata = metadata,
                        lines = cleanEmbeddedLines,
                        source = LyricsSource.ID3_TAG,
                        isSynced = embedded.isSynced
                    )
                    if (activeSongId == song.id) {
                        _uiState.value = success
                    }
                    return@withContext
                }
            }

            // Tầng 2: Đối chiếu Metadata qua API mở LRCLIB & nguồn lời bài hát trực tuyến
            if (activeSongId == song.id) {
                _uiState.value = LyricsUiState.Loading(
                    statusText = localizedResources().getString(R.string.lyrics_loading_tier2),
                    tier = 2
                )
            }

            val remoteResponse = runCatching {
                remoteClient.fetchLyrics(metadata)
            }.getOrNull()

            if (remoteResponse != null) {
                val syncedLines = remoteResponse.syncedLyrics
                    ?.takeIf { it.isNotBlank() }
                    ?.let { Id3v2LyricsExtractor.parseLrcText(it, metadata.durationMs) }
                    ?.let { filterOnlyVocalLines(it) }
                    .orEmpty()

                if (syncedLines.isNotEmpty()) {
                    val success = persistAndBuildSuccess(
                        song = song,
                        cacheKey = cacheKey,
                        metadata = metadata,
                        lines = syncedLines,
                        source = LyricsSource.LRCLIB,
                        isSynced = true
                    )
                    if (activeSongId == song.id) {
                        _uiState.value = success
                    }
                    return@withContext
                }

                val plainLines = remoteResponse.plainLyrics
                    ?.takeIf { it.isNotBlank() }
                    ?.let { Id3v2LyricsExtractor.parsePlainLyrics(it, metadata.durationMs) }
                    ?.let { filterOnlyVocalLines(it) }
                    .orEmpty()

                if (plainLines.isNotEmpty()) {
                    val success = persistAndBuildSuccess(
                        song = song,
                        cacheKey = cacheKey,
                        metadata = metadata,
                        lines = plainLines,
                        source = LyricsSource.LRCLIB,
                        isSynced = false
                    )
                    if (activeSongId == song.id) {
                        _uiState.value = success
                    }
                    return@withContext
                }
            }

            // Tầng 3: AI Speech-to-Text Offline dự phòng (Audio-to-Lyrics)
            if (!offlineEngine.isModelReady()) {
                if (activeSongId == song.id) {
                    _uiState.value = LyricsUiState.Error(
                        songId = song.id,
                        songTitle = metadata.title,
                        artistName = metadata.artist,
                        message = localizedResources().getString(
                            R.string.lyrics_error_song_not_found_offline,
                            metadata.title
                        ),
                        requiresOfflineModelDownload = true
                    )
                }
                return@withContext
            }

            if (activeSongId == song.id) {
                _uiState.value = LyricsUiState.Loading(
                    statusText = localizedResources().getString(R.string.lyrics_loading_tier3),
                    tier = 3
                )
            }

            val offlineLines = runCatching {
                filterOnlyVocalLines(offlineEngine.transcribeSongOffline(song))
            }.getOrDefault(emptyList())

            if (offlineLines.isNotEmpty()) {
                val success = persistAndBuildSuccess(
                    song = song,
                    cacheKey = cacheKey,
                    metadata = metadata,
                    lines = offlineLines,
                    source = LyricsSource.AI_OFFLINE,
                    isSynced = true
                )
                if (activeSongId == song.id) {
                    _uiState.value = success
                }
            } else if (activeSongId == song.id) {
                _uiState.value = LyricsUiState.Error(
                    songId = song.id,
                    songTitle = metadata.title,
                    artistName = metadata.artist,
                    message = localizedResources().getString(
                        R.string.lyrics_error_song_not_found_network,
                        metadata.title
                    ),
                    requiresOfflineModelDownload = false
                )
            }
        }
    }

    /**
     * Xóa cache của bài hát hiện tại và quét lại lời từ đầu qua 3 tầng.
     */
    suspend fun rescanLyrics(song: Song?) {
        if (song == null) return
        loadLyricsForSong(song, forceRescan = true)
    }

    /**
     * Tải gói model AI Offline và tự động kích hoạt quét lại lời cho bài hát đang mở.
     */
    suspend fun downloadOfflineModelAndTranscribe(currentSong: Song?) {
        val ok = offlineEngine.downloadOfflineModel()
        if (ok && currentSong != null) {
            loadLyricsForSong(currentSong, forceRescan = true)
        }
    }

    /**
     * Lọc bỏ mọi câu chỉ chứa nhạc dạo / ký hiệu âm nhạc, chỉ giữ lại lời bài hát thực sự
     * và đánh lại chỉ số `index` liên tục `0..N-1`.
     */
    private fun filterOnlyVocalLines(lines: List<LyricLine>): List<LyricLine> {
        if (lines.isEmpty()) return emptyList()
        val cleaned = ArrayList<LyricLine>(lines.size)
        for (i in lines.indices) {
            val isBoundary = i < 2 || i >= lines.size - 2
            val text = Id3v2LyricsExtractor.cleanLyricLineText(lines[i].text, isNearBoundary = isBoundary)
            if (!text.isNullOrBlank()) {
                cleaned.add(lines[i].copy(text = text))
            }
        }
        return cleaned.mapIndexed { newIdx, line ->
            line.copy(index = newIdx)
        }
    }

    private fun isLegacySyntheticOfflinePlaceholder(lines: List<LyricLine>): Boolean {
        if (lines.isEmpty()) return false
        return lines.any {
            it.text.contains("Lắng nghe giai điệu mở đầu ngân vang trong không gian") ||
                it.text.contains("Từng nhịp trống hòa cùng tiếng hát dịu dàng sâu lắng") ||
                it.text.startsWith("♪ ")
        }
    }

    private suspend fun persistAndBuildSuccess(
        song: Song,
        cacheKey: String,
        metadata: CleanedTrackMetadata,
        lines: List<LyricLine>,
        source: LyricsSource,
        isSynced: Boolean
    ): LyricsUiState.Success {
        val json = serializeLinesToJson(lines)
        val entity = LyricsCacheEntity(
            cacheKey = cacheKey,
            songId = song.id,
            songTitle = metadata.title,
            artistName = metadata.artist,
            source = source.id,
            isSynced = isSynced,
            linesJson = json,
            updatedAt = System.currentTimeMillis()
        )
        runCatching {
            lyricsCacheDao.insertCache(entity)
        }

        val success = LyricsUiState.Success(
            songId = song.id,
            cacheKey = cacheKey,
            songTitle = metadata.title,
            artistName = metadata.artist,
            lines = lines,
            source = source,
            isSynced = isSynced,
            fromCache = false
        )
        memoryCache[song.id] = success
        return success
    }

    internal fun computeMp3CacheKey(song: Song): String {
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            md.update("v4_${song.id}_${song.duration}_".toByteArray())
            Id3v2LyricsExtractor.openSongInputStream(appContext, song)?.use { stream ->
                val buffer = ByteArray(16 * 1024)
                val read = stream.read(buffer)
                if (read > 0) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Throwable) {
            "v4_song_${song.id}_${song.duration}"
        }
    }

    /**
     * Trích xuất `METADATA_KEY_TITLE`, `METADATA_KEY_ARTIST`, `METADATA_KEY_DURATION`
     * từ `MediaStore` + `MediaMetadataRetriever` (FileDescriptor) + thẻ ID3 + tên file,
     * đồng thời sinh danh sách ứng viên `(title, artist)` cho cả hai chiều `"Bài hát - Ca sĩ"` và `"Ca sĩ - Bài hát"`.
     */
    internal fun extractTrackMetadata(
        song: Song,
        id3Title: String? = null,
        id3Artist: String? = null
    ): CleanedTrackMetadata {
        var rawTitle: String? = id3Title
        var rawArtist: String? = id3Artist
        var mediaStoreTitle: String? = null
        var mediaStoreArtist: String? = null
        var durationMs = song.duration

        // 1. Đọc từ MediaStore (nếu URI là content://media/...)
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION
            )
            appContext.contentResolver.query(song.uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val tIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    val aIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    val dIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                    if (tIdx >= 0) mediaStoreTitle = cursor.getString(tIdx)
                    if (aIdx >= 0) mediaStoreArtist = cursor.getString(aIdx)
                    if (dIdx >= 0) {
                        val d = cursor.getLong(dIdx)
                        if (d > 0L) durationMs = d
                    }
                }
            }
        } catch (_: Throwable) {
        }

        // 2. Đọc từ MediaMetadataRetriever qua ParcelFileDescriptor (tương thích Scoped Storage Android 10-16)
        val retriever = MediaMetadataRetriever()
        try {
            var sourceSet = false
            try {
                appContext.contentResolver.openFileDescriptor(song.uri, "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                    sourceSet = true
                }
            } catch (_: Throwable) {
            }
            if (!sourceSet && !song.path.isNullOrBlank()) {
                try {
                    retriever.setDataSource(song.path)
                    sourceSet = true
                } catch (_: Throwable) {
                }
            }
            if (!sourceSet) {
                retriever.setDataSource(appContext, song.uri)
            }
            if (rawTitle.isNullOrBlank()) {
                rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            }
            if (rawArtist.isNullOrBlank()) {
                rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
            }
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val parsedDur = durStr?.toLongOrNull()
            if (parsedDur != null && parsedDur > 0L) {
                durationMs = parsedDur
            }
        } catch (_: Throwable) {
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }

        val candidates = LinkedHashSet<Pair<String, String>>()

        val cleanRetrieverArtist = listOfNotNull(rawArtist, mediaStoreArtist, song.artist)
            .map { sanitizeNoiseTokens(it) }
            .firstOrNull { it.isNotBlank() && !isGenericUnknown(it) && !isWebsiteOrHashToken(it) }
            ?: ""

        // Ứng viên từ thẻ ID3 / MediaMetadataRetriever Title
        if (!rawTitle.isNullOrBlank() && !isGenericUnknown(rawTitle)) {
            addCandidatesFromRawString(rawTitle, cleanRetrieverArtist, candidates)
        }

        // Ứng viên từ MediaStore TITLE
        val msTitle = mediaStoreTitle
        if (!msTitle.isNullOrBlank() && !isGenericUnknown(msTitle)) {
            addCandidatesFromRawString(msTitle, cleanRetrieverArtist, candidates)
        }

        // Ứng viên từ tên file (song.title)
        addCandidatesFromRawString(song.title, cleanRetrieverArtist, candidates)

        val fallbackParsed = parseTitleAndArtistFromFileName(song.title)
        val primaryPair = candidates.firstOrNull() ?: fallbackParsed
        val primaryTitle = primaryPair.first.ifBlank { song.displayTitle }
        val primaryArtist = primaryPair.second.ifBlank { cleanRetrieverArtist }

        // Bổ sung ứng viên rút gọn (loại bỏ các hậu tố Remix / Cover / Lofi / Live / ft.)
        val coreTitle = stripVersionSuffixes(primaryTitle)
        val coreArtist = stripFeaturingArtist(primaryArtist)
        if (coreTitle.isNotBlank()) {
            candidates.add(coreTitle to coreArtist)
            candidates.add(coreTitle to "")
        }

        val finalDurationMs = durationMs.coerceAtLeast(1_000L)
        val durationSec = ((finalDurationMs + 500L) / 1000L).toInt().coerceAtLeast(1)

        return CleanedTrackMetadata(
            title = primaryTitle,
            artist = primaryArtist,
            durationSeconds = durationSec,
            durationMs = finalDurationMs,
            candidatePairs = candidates.toList()
        )
    }

    private fun addCandidatesFromRawString(
        rawInput: String,
        knownArtist: String,
        out: MutableSet<Pair<String, String>>
    ) {
        val withoutExt = stripFileExtension(rawInput)
        val normalizedInput = normalizeHyphenatedSlugIfNeeded(withoutExt)
        val cleaned = sanitizeNoiseTokens(normalizedInput)
        if (cleaned.isBlank()) return

        val parts = cleaned.split(CANDIDATE_SPLIT_REGEX)
            .map { sanitizeNoiseTokens(it) }
            .filter { it.isNotBlank() && !isWebsiteOrHashToken(it) }

        when {
            parts.size >= 2 -> {
                val p0 = parts[0]
                val p1 = parts[1]
                // Hỗ trợ cả 2 chiều phổ biến: "Tên bài hát - Ca sĩ" và "Ca sĩ - Tên bài hát"
                if (knownArtist.isNotBlank() && p0.equals(knownArtist, ignoreCase = true)) {
                    out.add(p1 to p0)
                    out.add(p0 to p1)
                } else {
                    out.add(p0 to p1)
                    out.add(p1 to p0)
                }
            }
            parts.size == 1 -> {
                out.add(parts[0] to knownArtist)
            }
        }
    }

    companion object {
        private val FILE_EXT_REGEX = Regex("""\.(mp3|flac|m4a|wav|ogg|aac)$""", RegexOption.IGNORE_CASE)
        private val LEADING_TRACK_NUMBER_REGEX = Regex("""^\s*\d{1,3}\s*[.\-_)]\s*""")
        private val WEBSITE_PREFIX_SUFFIX_REGEX = Regex(
            """\b(y2mate\.com|y2mate|yt1s\.com|yt1s|savefrom\.net|snaptik|ssstik|zingmp3\.vn|zingmp3|mp3\.zing\.vn|nhaccuatui\.com|nhaccuatui|chiasenhac\.vn|chiasenhac|keeng\.vn|nhac\.vn|nhacpro\.vn|tai\s*nhac)\b""",
            RegexOption.IGNORE_CASE
        )
        private val NOISE_BRACKETS_REGEX = Regex(
            """(\[[^\]]*(?:320|128|192|256|kbps|flac|mp3|m4a|hd|hq|4k|official|mv|video|lyric|lyrics|audio|visualizer|kar|karaoke|vietsub|tiktok|full)[^\]]*]|\([^\)]*(?:320|128|192|256|kbps|flac|mp3|m4a|hd|hq|4k|official|mv|video|lyric|lyrics|audio|visualizer|kar|karaoke|vietsub|tiktok|full)[^\)]*\))""",
            RegexOption.IGNORE_CASE
        )
        private val BITRATE_TOKEN_REGEX = Regex(
            """\b(320\s*kbps|128\s*kbps|192\s*kbps|256\s*kbps|lossless|official\s+music\s+video|official\s+lyric\s+video|official\s+video|official\s+mv|official\s+audio|lyrics\s+video|lyric\s+video|music\s+video|m/v|audio\s+official|prod\.?\s+by\s+\w+)\b""",
            RegexOption.IGNORE_CASE
        )
        private val TRAILING_ID_HASH_REGEX = Regex("""\s+[-_]\s*(?:[0-9]{5,12}|[a-zA-Z0-9_-]{11})$""")
        private val VERSION_SUFFIX_REGEX = Regex(
            """(\s*[\(\[][^\)\]]*(?:remix|cover|acoustic|lofi|lo-fi|live|mashup|speed\s*up|slowed|reverb|ver\.?\s*\d+|version|ost|nhạc\s*phim|beat)[^\)\]]*[\)\]]|\s+-\s+.*(?:remix|cover|acoustic|lofi|lo-fi|live|speed\s*up|slowed).*|\b(?:remix|lofi\s*ver|acoustic\s*ver|live\s*ver)\b).*$""",
            RegexOption.IGNORE_CASE
        )
        private val FEATURING_REGEX = Regex("""\s+(?:ft\.?|feat\.?|featuring|x|&|,)\s+.*$""", RegexOption.IGNORE_CASE)
        private val MULTI_SPACE_REGEX = Regex("""\s{2,}""")
        private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
        private val CANDIDATE_SPLIT_REGEX = Regex("""\s+[-–—|_]\s+""")
        private val NUMERIC_HASH_TOKEN_REGEX = Regex("""^[0-9]{5,12}$""")
        private val FILENAME_DASH_SPLIT_REGEX = Regex("""\s+-\s+""")
        private val NON_ALNUM_SPACE_REGEX = Regex("""[^a-z0-9\s]""")

        private fun stripFileExtension(raw: String): String =
            raw.trim().replace(FILE_EXT_REGEX, "")

        private val WORD_HYPHEN_SLUG_REGEX = Regex("""(?<=[a-zA-ZÀ-ỹ0-9])-(?=[a-zA-ZÀ-ỹ0-9])""")

        /**
         * Chuyển đổi tên file dạng slug nối bằng dấu gạch ngang (ví dụ: `Cat-Doi-Noi-Sau - Tang-Duy-Tan`
         * hoặc `Cat-Doi-Noi-Sau-Tang-Duy-Tan`) thành chuỗi có khoảng trắng tự nhiên, đồng thời giữ nguyên
         * dấu phân cách `" - "` giữa tên bài hát và ca sĩ.
         */
        internal fun normalizeHyphenatedSlugIfNeeded(input: String): String {
            val trimmed = input.trim()
            if (WORD_HYPHEN_SLUG_REGEX.findAll(trimmed).count() >= 2) {
                return trimmed.replace(WORD_HYPHEN_SLUG_REGEX, " ")
            }
            return trimmed
        }

        private fun isGenericUnknown(text: String): Boolean {
            val lower = text.trim().lowercase()
            return lower.isEmpty() ||
                lower == "<unknown>" ||
                lower == "unknown" ||
                lower == "unknown artist" ||
                lower == "nghệ sĩ không rõ" ||
                lower == "various artists" ||
                lower == "va"
        }

        private fun isWebsiteOrHashToken(token: String): Boolean {
            val trimmed = token.trim()
            if (trimmed.isEmpty()) return true
            if (WEBSITE_PREFIX_SUFFIX_REGEX.matches(trimmed)) return true
            if (NUMERIC_HASH_TOKEN_REGEX.matches(trimmed)) return true
            return false
        }

        /**
         * Làm sạch tên bài hát & ca sĩ từ tên file khi thiếu ID3 tag:
         * Loại bỏ `.mp3`, `_`, `[320kbps]`, `(Official MV)`, `y2mate.com`, v.v.
         */
        fun parseTitleAndArtistFromFileName(fileName: String): Pair<String, String> {
            val withoutExt = normalizeHyphenatedSlugIfNeeded(stripFileExtension(fileName)).replace('_', ' ')
            val cleaned = sanitizeNoiseTokens(withoutExt)
            val parts = cleaned.split(FILENAME_DASH_SPLIT_REGEX)
                .map { sanitizeNoiseTokens(it) }
                .filter { it.isNotBlank() && !isWebsiteOrHashToken(it) }

            if (parts.size >= 2) {
                val left = parts[0]
                val right = parts[1]
                return right to left
            }
            return cleaned.ifBlank { stripFileExtension(fileName) } to ""
        }

        fun sanitizeNoiseTokens(raw: String): String {
            return raw
                .replace('_', ' ')
                .replace(FILE_EXT_REGEX, "")
                .replace(WEBSITE_PREFIX_SUFFIX_REGEX, " ")
                .replace(NOISE_BRACKETS_REGEX, " ")
                .replace(BITRATE_TOKEN_REGEX, " ")
                .replace(TRAILING_ID_HASH_REGEX, "")
                .replace(LEADING_TRACK_NUMBER_REGEX, "")
                .replace(MULTI_SPACE_REGEX, " ")
                .trim(' ', '-', '–', '—', '_', '.')
        }

        fun stripVersionSuffixes(title: String): String {
            return title
                .replace(VERSION_SUFFIX_REGEX, "")
                .replace(FEATURING_REGEX, "")
                .replace(MULTI_SPACE_REGEX, " ")
                .trim(' ', '-', '–', '—', '_', '.')
                .ifBlank { title }
        }

        fun stripFeaturingArtist(artist: String): String {
            return artist
                .replace(FEATURING_REGEX, "")
                .replace(MULTI_SPACE_REGEX, " ")
                .trim()
                .ifBlank { artist }
        }

        internal fun normalizeForCompare(input: String): String {
            if (input.isBlank()) return ""
            val nfd = Normalizer.normalize(input.lowercase(), Normalizer.Form.NFD)
            return DIACRITICS_REGEX.replace(nfd, "")
                .replace('đ', 'd')
                .replace(NON_ALNUM_SPACE_REGEX, " ")
                .replace(MULTI_SPACE_REGEX, " ")
                .trim()
        }

        fun serializeLinesToJson(lines: List<LyricLine>): String {
            val arr = JSONArray()
            for (line in lines) {
                val obj = JSONObject()
                obj.put("i", line.index)
                obj.put("s", line.startTimeMs)
                obj.put("e", line.endTimeMs)
                obj.put("t", line.text)
                if (line.segments.isNotEmpty()) {
                    val segArr = JSONArray()
                    for (seg in line.segments) {
                        val sObj = JSONObject()
                        sObj.put("s", seg.startTimeMs)
                        sObj.put("e", seg.endTimeMs)
                        sObj.put("cs", seg.startCharIndex)
                        sObj.put("ce", seg.endCharIndex)
                        segArr.put(sObj)
                    }
                    obj.put("seg", segArr)
                }
                arr.put(obj)
            }
            return arr.toString()
        }

        fun deserializeLinesFromJson(json: String): List<LyricLine> {
            if (json.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(json)
                val list = ArrayList<LyricLine>(arr.length())
                for (idx in 0 until arr.length()) {
                    val obj = arr.getJSONObject(idx)
                    val text = obj.optString("t", "").trim()
                    if (text.isNotEmpty()) {
                        val segArr = obj.optJSONArray("seg")
                        val segments = if (segArr != null && segArr.length() > 0) {
                            ArrayList<LyricWordSegment>(segArr.length()).apply {
                                for (j in 0 until segArr.length()) {
                                    val sObj = segArr.optJSONObject(j) ?: continue
                                    add(
                                        LyricWordSegment(
                                            startTimeMs = sObj.optLong("s", 0L),
                                            endTimeMs = sObj.optLong("e", 0L),
                                            startCharIndex = sObj.optInt("cs", 0),
                                            endCharIndex = sObj.optInt("ce", 0)
                                        )
                                    )
                                }
                            }
                        } else {
                            emptyList()
                        }
                        list.add(
                            LyricLine(
                                index = obj.optInt("i", idx),
                                startTimeMs = obj.optLong("s", 0L),
                                endTimeMs = obj.optLong("e", 0L),
                                text = text,
                                segments = segments
                            )
                        )
                    }
                }
                list
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }
}

/**
 * Client truy vấn lời bài hát đa tầng:
 * 1. Truy vấn toàn văn **LRCLIB** (`/api/search?q=...` và `/api/search?track_name=...&artist_name=...`),
 *    so khớp thông minh không dấu tiếng Việt và tự động loại bỏ các bản nhạc không lời (`instrumental = true`).
 * 2. Nguồn LRC đồng bộ dự phòng (**NetEase Cloud Music LRC API**) cho các bài nhạc Việt / châu Á / Remix.
 * 3. Nguồn lời văn bản dự phòng (**Lyrics.ovh**) nếu chưa có bản LRC.
 */
class DefaultLrcLibRemoteClient : LrcLibRemoteClient {

    override suspend fun fetchLyrics(metadata: CleanedTrackMetadata): RemoteLyricsResponse? =
        withContext(Dispatchers.IO) {
            if (metadata.title.isBlank()) return@withContext null

            val candidatePairs = if (metadata.candidatePairs.isNotEmpty()) {
                metadata.candidatePairs
            } else {
                listOf(metadata.title to metadata.artist)
            }

            // Tập hợp các truy vấn tìm kiếm toàn văn ưu tiên cao nhất trên LRCLIB (/api/search?q=...)
            val searchQueries = LinkedHashSet<Pair<String, String>>()
            for ((candTitle, candArtist) in candidatePairs) {
                if (candTitle.isBlank()) continue
                if (candArtist.isNotBlank()) {
                    searchQueries.add("$candTitle $candArtist" to candTitle)
                }
                searchQueries.add(candTitle to candTitle)
                val coreTitle = LyricsRepository.stripVersionSuffixes(candTitle)
                if (coreTitle.isNotBlank() && coreTitle != candTitle) {
                    if (candArtist.isNotBlank()) {
                        searchQueries.add("$coreTitle ${LyricsRepository.stripFeaturingArtist(candArtist)}" to coreTitle)
                    }
                    searchQueries.add(coreTitle to coreTitle)
                }
            }

            var bestPlainFallback: RemoteLyricsResponse? = null

            // Bước 1: Tìm kiếm toàn văn `/api/search?q=...` trên LRCLIB (nhạy nhất kể cả khi đảo vị trí Ca sĩ - Bài hát)
            for ((queryText, expectedTitle) in searchQueries.take(4)) {
                val encQuery = URLEncoder.encode(queryText, "UTF-8")
                val searchUrl = "https://lrclib.net/api/search?q=$encQuery"
                val result = executeLrcLibSearch(searchUrl, expectedTitle, metadata.artist, metadata.durationSeconds)
                if (result != null) {
                    if (!result.syncedLyrics.isNullOrBlank()) {
                        return@withContext result
                    }
                    if (bestPlainFallback == null && !result.plainLyrics.isNullOrBlank()) {
                        bestPlainFallback = result
                    }
                }
            }

            // Bước 2: Tìm theo trường cấu trúc `track_name` + `artist_name` trên LRCLIB
            for ((candTitle, candArtist) in candidatePairs.take(3)) {
                if (candTitle.isBlank() || candArtist.isBlank()) continue
                val encTrack = URLEncoder.encode(candTitle, "UTF-8")
                val encArtist = URLEncoder.encode(candArtist, "UTF-8")
                val structuredUrl =
                    "https://lrclib.net/api/search?track_name=$encTrack&artist_name=$encArtist"
                val match = executeLrcLibSearch(structuredUrl, candTitle, candArtist, metadata.durationSeconds)
                if (match != null) {
                    if (!match.syncedLyrics.isNullOrBlank()) {
                        return@withContext match
                    }
                    if (bestPlainFallback == null && !match.plainLyrics.isNullOrBlank()) {
                        bestPlainFallback = match
                    }
                }
            }

            // Bước 3: Dự phòng qua nguồn LRC đồng bộ NetEase Cloud Music
            for ((candTitle, candArtist) in candidatePairs.take(3)) {
                val query = if (candArtist.isNotBlank()) "$candTitle $candArtist" else candTitle
                val netEaseResult = fetchFromNetEaseLrc(query, candTitle, metadata.durationSeconds)
                if (netEaseResult != null && !netEaseResult.syncedLyrics.isNullOrBlank()) {
                    return@withContext netEaseResult
                }
            }

            if (bestPlainFallback != null) {
                return@withContext bestPlainFallback
            }

            // Bước 4: Dự phòng Lyrics.ovh nếu có cả ca sĩ và tên bài
            for ((candTitle, candArtist) in candidatePairs.take(2)) {
                if (candTitle.isNotBlank() && candArtist.isNotBlank()) {
                    val ovh = fetchFromLyricsOvh(candArtist, candTitle)
                    if (ovh != null) return@withContext ovh
                }
            }

            null
        }

    private fun executeLrcLibSearch(
        urlStr: String,
        expectedTitle: String,
        expectedArtist: String,
        targetDurationSec: Int
    ): RemoteLyricsResponse? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_500
                readTimeout = 5_500
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "MusicPlayerHoaCa/1.0.2 (Android)")
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            val arr = JSONArray(body)
            if (arr.length() == 0) return null

            val normExpectedTitle = LyricsRepository.normalizeForCompare(
                LyricsRepository.stripVersionSuffixes(expectedTitle)
            )
            val normExpectedArtist = LyricsRepository.normalizeForCompare(
                LyricsRepository.stripFeaturingArtist(expectedArtist)
            )

            var bestSynced: String? = null
            var bestPlain: String? = null
            var bestScore = Int.MIN_VALUE

            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                if (item.optBoolean("instrumental", false)) continue

                val synced = item.optString("syncedLyrics", "").takeIf { it.isNotBlank() && it != "null" }
                val plain = item.optString("plainLyrics", "").takeIf { it.isNotBlank() && it != "null" }
                if (synced == null && plain == null) continue

                val trackName = item.optString("trackName", item.optString("name", ""))
                val artistName = item.optString("artistName", "")
                val normTrack = LyricsRepository.normalizeForCompare(
                    LyricsRepository.stripVersionSuffixes(trackName)
                )
                val normArtist = LyricsRepository.normalizeForCompare(artistName)
                val normCombined = "$normTrack $normArtist".trim()

                // So khớp linh hoạt với cả tên bài hát riêng lẫn chuỗi gộp (tên bài + ca sĩ)
                val titleSim = maxOf(
                    computeTokenSimilarity(normExpectedTitle, normTrack),
                    computeTokenSimilarity(normExpectedTitle, normCombined)
                )
                if (titleSim < 0.35f) continue

                var score = (titleSim * 1000f).toInt()
                if (normExpectedArtist.isNotBlank() && normArtist.isNotBlank()) {
                    val artistSim = maxOf(
                        computeTokenSimilarity(normExpectedArtist, normArtist),
                        computeTokenSimilarity(normExpectedArtist, normCombined)
                    )
                    score += (artistSim * 400f).toInt()
                }

                val itemDur = item.optDouble("duration", targetDurationSec.toDouble()).toInt()
                val durDiff = abs(itemDur - targetDurationSec)
                if (durDiff <= 3) {
                    score += 360
                }
                score -= durDiff.coerceAtMost(180) * 16

                if (synced != null) {
                    score += 550
                }

                if (score > bestScore) {
                    bestScore = score
                    bestSynced = synced
                    bestPlain = plain
                }
            }

            if (bestSynced == null && bestPlain == null) null else RemoteLyricsResponse(bestSynced, bestPlain)
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun fetchFromNetEaseLrc(
        query: String,
        expectedTitle: String,
        targetDurationSec: Int
    ): RemoteLyricsResponse? {
        var searchConn: HttpURLConnection? = null
        var lyricConn: HttpURLConnection? = null
        return try {
            val encQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://music.163.com/api/search/get/web?s=$encQuery&type=1&offset=0&total=true&limit=6"
            searchConn = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("Referer", "https://music.163.com")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; MusicPlayerHoaCa)")
            }
            if (searchConn.responseCode != HttpURLConnection.HTTP_OK) return null
            val searchBody = BufferedReader(InputStreamReader(searchConn.inputStream, Charsets.UTF_8)).use { it.readText() }
            val root = JSONObject(searchBody)
            val songsArr = root.optJSONObject("result")?.optJSONArray("songs") ?: return null
            if (songsArr.length() == 0) return null

            val normExpectedTitle = LyricsRepository.normalizeForCompare(
                LyricsRepository.stripVersionSuffixes(expectedTitle)
            )
            var chosenSongId = -1L
            var bestScore = Int.MIN_VALUE

            for (i in 0 until songsArr.length()) {
                val sObj = songsArr.optJSONObject(i) ?: continue
                val id = sObj.optLong("id", -1L)
                val name = sObj.optString("name", "")
                if (id <= 0L || name.isBlank()) continue

                val normName = LyricsRepository.normalizeForCompare(LyricsRepository.stripVersionSuffixes(name))
                val sim = computeTokenSimilarity(normExpectedTitle, normName)
                if (sim < 0.4f) continue

                val durSec = (sObj.optLong("duration", targetDurationSec * 1000L) / 1000L).toInt()
                val diff = abs(durSec - targetDurationSec)
                val score = (sim * 1000f).toInt() - diff.coerceAtMost(120) * 3
                if (score > bestScore) {
                    bestScore = score
                    chosenSongId = id
                }
            }

            if (chosenSongId <= 0L) return null

            val lyricUrl = "https://music.163.com/api/song/lyric?os=pc&id=$chosenSongId&lv=-1&kv=-1&tv=-1"
            lyricConn = (URL(lyricUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("Referer", "https://music.163.com")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; MusicPlayerHoaCa)")
            }
            if (lyricConn.responseCode != HttpURLConnection.HTTP_OK) return null
            val lyricBody = BufferedReader(InputStreamReader(lyricConn.inputStream, Charsets.UTF_8)).use { it.readText() }
            val lyricJson = JSONObject(lyricBody)
            val lrcStr = lyricJson.optJSONObject("lrc")?.optString("lyric", "")
                ?.takeIf { it.isNotBlank() && it != "null" }
            if (lrcStr.isNullOrBlank()) null else RemoteLyricsResponse(syncedLyrics = lrcStr, plainLyrics = null)
        } catch (_: Throwable) {
            null
        } finally {
            searchConn?.disconnect()
            lyricConn?.disconnect()
        }
    }

    private fun fetchFromLyricsOvh(artist: String, title: String): RemoteLyricsResponse? {
        var conn: HttpURLConnection? = null
        return try {
            val encArtist = URLEncoder.encode(artist, "UTF-8").replace("+", "%20")
            val encTitle = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
            val urlStr = "https://api.lyrics.ovh/v1/$encArtist/$encTitle"
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            val json = JSONObject(body)
            val plain = json.optString("lyrics", "").takeIf { it.isNotBlank() }
            if (plain == null) null else RemoteLyricsResponse(syncedLyrics = null, plainLyrics = plain)
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    internal fun computeTokenSimilarity(expected: String, candidate: String): Float {
        if (expected.isBlank() || candidate.isBlank()) return 0f
        if (expected == candidate) return 1f
        if (candidate.contains(expected) || expected.contains(candidate)) return 0.92f

        val expTokens = expected.split(' ').filter { it.isNotBlank() }.toSet()
        val candTokens = candidate.split(' ').filter { it.isNotBlank() }.toSet()
        if (expTokens.isEmpty() || candTokens.isEmpty()) return 0f

        val intersection = expTokens.intersect(candTokens).size
        val minSize = minOf(expTokens.size, candTokens.size).coerceAtLeast(1)
        return intersection.toFloat() / minSize.toFloat()
    }
}
