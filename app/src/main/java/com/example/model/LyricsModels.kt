package com.example.model

import androidx.compose.runtime.Immutable

/**
 * Mốc thời gian chi tiết cho từng từ / cụm từ trong một câu hát (Enhanced LRC `<mm:ss.xx>`).
 */
@Immutable
data class LyricWordSegment(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val startCharIndex: Int,
    val endCharIndex: Int
)

/**
 * Đại diện cho một câu hát trong trang Hòa ca.
 *
 * @property index Thứ tự câu hát (0-based).
 * @property startTimeMs Thời điểm bắt đầu câu hát (ms).
 * @property endTimeMs Thời điểm kết thúc câu hát (ms), bằng startTimeMs của câu kế tiếp hoặc mốc nghỉ nhạc.
 * @property text Nội dung câu hát hiển thị.
 * @property segments Danh sách mốc thời gian từng từ nếu file LRC có định dạng Enhanced LRC (`<mm:ss.xx>`).
 */
@Immutable
data class LyricLine(
    val index: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val segments: List<LyricWordSegment> = emptyList()
)

/**
 * Nguồn trích xuất lời bài hát trong kiến trúc Lai 3 tầng (3-Tier Hybrid Lyrics Pipeline).
 */
enum class LyricsSource(val id: String, val badgeLabel: String) {
    ID3_TAG(id = "ID3_TAG", badgeLabel = "ID3 Tag"),
    LRCLIB(id = "LRCLIB", badgeLabel = "LRCLIB"),
    AI_OFFLINE(id = "AI_OFFLINE", badgeLabel = "AI Offline");

    companion object {
        fun fromId(raw: String?): LyricsSource =
            entries.firstOrNull { it.id.equals(raw, ignoreCase = true) } ?: ID3_TAG
    }
}

/**
 * Trạng thái gói model AI Speech-to-Text Offline (Sherpa-ONNX / Whisper).
 */
@Immutable
data class OfflineModelState(
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val modelSizeLabel: String = "38 MB",
    val modelVersion: String = "sherpa-onnx-whisper-tiny-vi-int8",
    val errorMessage: String? = null
)

/**
 * Trạng thái giao diện trang Hòa ca xuất ra từ LyricsRepository.
 */
@Immutable
sealed interface LyricsUiState {
    data object Idle : LyricsUiState

    data class Loading(
        val statusText: String,
        val tier: Int = 1
    ) : LyricsUiState

    data class Success(
        val songId: Long,
        val cacheKey: String,
        val songTitle: String,
        val artistName: String,
        val lines: List<LyricLine>,
        val source: LyricsSource,
        val isSynced: Boolean,
        val fromCache: Boolean = false
    ) : LyricsUiState

    data class Error(
        val songId: Long,
        val songTitle: String,
        val artistName: String,
        val message: String,
        val requiresOfflineModelDownload: Boolean = false
    ) : LyricsUiState
}

/**
 * Tính tỷ lệ tô màu (`lineProgress`) cho từng câu hát theo đúng thời gian thực `currentPositionMs`:
 * - Câu đã hát qua (`currentPositionMs >= line.endTimeMs`) -> `1.0f` (giữ nguyên 100% màu đã tô).
 * - Câu chưa hát (`currentPositionMs <= line.startTimeMs`) -> `0.0f`.
 * - Nếu câu hát có mốc thời gian từng từ (`segments`), nội suy chính xác theo vị trí từng từ/ký tự.
 * - Nếu chỉ có mốc theo câu: tránh kéo lê quá chậm qua các khoảng lặng dài giữa hai đoạn (> 9.5s)
 *   và chừa khoảng ngân cuối câu (~140ms) để màu chạy khớp nhịp hát thực tế.
 */
fun calculateLineProgress(
    currentPositionMs: Long,
    line: LyricLine,
    calibrateLongPause: Boolean = false
): Float {
    if (currentPositionMs >= line.endTimeMs) return 1.0f
    if (currentPositionMs <= line.startTimeMs) return 0.0f

    if (line.segments.isNotEmpty()) {
        val totalChars = line.text.length.coerceAtLeast(1)
        val firstSegStart = line.segments.first().startTimeMs
        if (currentPositionMs <= firstSegStart) {
            val leadDuration = (firstSegStart - line.startTimeMs).coerceAtLeast(1L)
            val preFraction = ((currentPositionMs - line.startTimeMs).toFloat() / leadDuration.toFloat()).coerceIn(0f, 1f)
            val firstRatio = line.segments.first().startCharIndex.toFloat() / totalChars.toFloat()
            return (firstRatio * preFraction).coerceIn(0f, 1f)
        }
        for (seg in line.segments) {
            if (currentPositionMs <= seg.startTimeMs) {
                return (seg.startCharIndex.toFloat() / totalChars.toFloat()).coerceIn(0f, 1f)
            }
            if (currentPositionMs < seg.endTimeMs) {
                val segDur = (seg.endTimeMs - seg.startTimeMs).coerceAtLeast(1L)
                val segFrac = ((currentPositionMs - seg.startTimeMs).toFloat() / segDur.toFloat()).coerceIn(0f, 1f)
                val charPos = seg.startCharIndex + (seg.endCharIndex - seg.startCharIndex) * segFrac
                return (charPos / totalChars.toFloat()).coerceIn(0f, 1f)
            }
        }
        return 1.0f
    }

    val rawSpan = (line.endTimeMs - line.startTimeMs).coerceAtLeast(1L)
    val effectiveEndMs = if (calibrateLongPause) {
        estimateEffectiveVocalEndMs(line, rawSpan)
    } else {
        line.endTimeMs
    }
    if (currentPositionMs >= effectiveEndMs) return 1.0f

    val effectiveDuration = (effectiveEndMs - line.startTimeMs).coerceAtLeast(1L)
    return ((currentPositionMs - line.startTimeMs).toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
}

/**
 * Ước lượng thời điểm dứt câu hát tự nhiên khi file LRC chỉ gắn mốc ở đầu câu mà khoảng cách
 * sang câu kế tiếp chứa đoạn nghỉ nhạc dài (> 12 giây) hoặc sát câu tiếp theo.
 */
internal fun estimateEffectiveVocalEndMs(line: LyricLine, rawSpanMs: Long): Long {
    val text = line.text
    var start = 0
    var end = text.length - 1
    while (start <= end && text[start].isWhitespace()) start++
    while (end >= start && text[end].isWhitespace()) end--
    val cleanLen = (end - start + 1).coerceAtLeast(0)

    var words = 0
    var inWord = false
    for (i in start..end) {
        if (text[i].isWhitespace()) {
            inWord = false
        } else if (!inWord) {
            inWord = true
            words++
        }
    }
    val wordCount = words.coerceAtLeast(1)

    if (rawSpanMs > 12_000L) {
        val estimatedVocalSpan = (wordCount * 430L + cleanLen * 48L + 950L).coerceIn(2_800L, 6_800L)
        return minOf(line.endTimeMs, line.startTimeMs + estimatedVocalSpan)
    }

    // Chừa 140ms dứt hơi cuối câu để chữ cuối tô xong đúng lúc ca sĩ ngừng hát trước câu mới
    return if (rawSpanMs >= 1_800L) {
        (line.endTimeMs - 140L).coerceAtLeast(line.startTimeMs + 400L)
    } else {
        line.endTimeMs
    }
}

/**
 * Xác định chỉ số câu đang hát hiện tại dựa trên `currentPositionMs` bằng tìm kiếm nhị phân O(log N).
 */
fun findActiveLyricLineIndex(lines: List<LyricLine>, currentPositionMs: Long): Int {
    if (lines.isEmpty()) return -1
    var low = 0
    var high = lines.lastIndex
    var candidate = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        if (lines[mid].startTimeMs <= currentPositionMs) {
            candidate = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    if (candidate < 0) return -1

    var i = candidate
    while (i >= 0) {
        val line = lines[i]
        if (currentPositionMs >= line.startTimeMs && currentPositionMs < line.endTimeMs) {
            return i
        }
        if (i > 0 && lines[i - 1].endTimeMs <= currentPositionMs) {
            break
        }
        i--
    }
    return candidate
}

