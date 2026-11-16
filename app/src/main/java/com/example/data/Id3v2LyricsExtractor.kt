package com.example.data

import android.content.Context
import com.example.model.LyricLine
import com.example.model.LyricWordSegment
import com.example.model.Song
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Kết quả bóc tách lời bài hát (và thẻ tên bài / ca sĩ nếu có) từ thẻ ID3v2 nhúng sẵn trong file MP3 (Tầng 1).
 */
data class ExtractedEmbeddedLyrics(
    val lines: List<LyricLine>,
    val isSynced: Boolean,
    val embeddedTitle: String? = null,
    val embeddedArtist: String? = null
)

/**
 * Trình phân tích nhị phân thẻ ID3v2 (ID3v2.2, ID3v2.3, ID3v2.4), Vorbis Comments và MP4/M4A bên trong file nhạc:
 * - Tìm kiếm khung lời đồng bộ `SYLT` (Synchronized Lyrics/Text)
 * - Tìm kiếm khung lời văn bản `USLT` (Unsynchronized Lyrics/Text), `TXXX` hoặc `COMM` chứa LRC
 * - Hỗ trợ bỏ qua các khung ảnh bìa `APIC` dung lượng lớn để đọc tới khung `USLT`/`SYLT` nằm cuối tag.
 * - Tự động lọc bỏ hoàn toàn các dòng nhạc dạo / giang tấu / ký hiệu nốt nhạc / thông tin credit,
 *   chỉ giữ lại lời bài hát thực sự.
 */
object Id3v2LyricsExtractor {

    private val LRC_TIMESTAMP_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val WORD_TIMESTAMP_REGEX = Regex("""<(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?>""")
    private val OFFSET_TAG_REGEX = Regex("""^\[offset:\s*([+-]?\d+)\s*]$""", RegexOption.IGNORE_CASE)
    private val METADATA_TAG_LINE_REGEX = Regex(
        """^\[(ti|ar|al|by|offset|length|re|ve|id|hash|sign|qq|total|language|tool|au|wl|wm):.*]$""",
        RegexOption.IGNORE_CASE
    )

    // Các dòng chỉ chứa ký hiệu âm nhạc, dấu chấm lửng, gạch ngang hoặc khoảng trắng
    private val SYMBOLS_ONLY_REGEX = Regex("""^[\s♪♫♬♩♭♮♯~～\-_.*•·…:;!?'",/\\|()\[\]{}<>+]+$""")

    // Các dòng đánh dấu đoạn nhạc dạo, giang tấu, intro, outro, solo, beat...
    private val INSTRUMENTAL_LINE_REGEX = Regex(
        """^[\s♪♫♬♩(\[~\-]*((nhạc\s*dạo(\s*(mở\s*đầu|đầu\s*bài|giữa\s*bài|cuối\s*bài|kết\s*bài|kết))?|dạo\s*nhạc(\s*(mở\s*đầu|đầu\s*bài|giữa\s*bài|cuối\s*bài))?|dạo\s*đầu|nhạc\s*nền|nhạc\s*giang\s*tấu|nhạc\s*không\s*lời|nhạc\s*mở\s*đầu|nhạc\s*kết|nhạc|giang\s*tấu(\s*\d*)?|đoạn\s*nhạc(\s*dạo)?|nghỉ\s*nhạc|vĩ\s*thanh|hợp\s*xướng\s*nhạc|intro(duction)?|outro|instrumental(\s*(break|intro|outro|solo))?|interlude|music(\s*(break|intro|outro|interlude))?|beat(\s*drop)?|drop|solo(\s*(guitar|piano|sax|violin|flute|drum|organ|sáo|đàn\s*bầu|đàn\s*tranh))?|(guitar|piano|sax|violin|flute|drum|organ|sáo)\s*solo|dance\s*break|break|hết\s*bài|kết\s*bài|end)(\s*\d*)?)[\s♪♫♬♩)\]}~\-.:…!]*$""",
        RegexOption.IGNORE_CASE
    )

    // Các dòng chú thích mở đầu/kết thúc không phải câu hát (credit: Sáng tác, Ca sĩ, LRC by, website...)
    private val CREDIT_LINE_REGEX = Regex(
        """^\s*((bài\s*hát|ca\s*sĩ|trình\s*bày|biểu\s*diễn|thể\s*hiện|sáng\s*tác|nhạc\s*sĩ|nhạc\s*và\s*lời|lời\s*bài\s*hát|lời\s*việt|nhạc\s*ngoại|nhạc\s*hoa|hòa\s*âm|phối\s*khí|biên\s*tập|thực\s*hiện|nguồn|album|title|artist|singer|composer|lyricist|lyrics\s*by|written\s*by|arranged\s*by|arranger|produced\s*by|producer|mixed\s*by|mastered\s*by|lrc\s*by|synced\s*by|sub\s*by|timer|edit\s*by|upload\s*by|karaoke\s*by)\s*[:\-]|(www\.|https?://|zing\s*mp3|nhaccuatui|chiasenhac))""",
        RegexOption.IGNORE_CASE
    )

    private val LEADING_TRAILING_NOTES_REGEX = Regex("""^[\s♪♫♬♩~～]+|[\s♪♫♬♩~～]+$""")
    private val INLINE_INSTRUMENTAL_TAG_REGEX = Regex(
        """(\[(nhạc(\s*dạo[^\]]*)?|giang\s*tấu[^\]]*|dạo\s*đầu[^\]]*|dạo\s*nhạc[^\]]*|đoạn\s*nhạc[^\]]*|nhạc\s*không\s*lời[^\]]*|intro[^\]]*|outro[^\]]*|instrumental[^\]]*|interlude[^\]]*|solo[^\]]*|music[^\]]*|beat[^\]]*)]|\((nhạc(\s*dạo[^\)]*)?|giang\s*tấu[^\)]*|dạo\s*đầu[^\)]*|dạo\s*nhạc[^\)]*|đoạn\s*nhạc[^\)]*|nhạc\s*không\s*lời[^\)]*|intro[^\)]*|outro[^\)]*|instrumental[^\)]*|interlude[^\)]*|solo[^\)]*|music[^\)]*|beat[^\)]*)\))""",
        RegexOption.IGNORE_CASE
    )
    private val SECTION_PREFIX_REGEX = Regex(
        """^(\[(?:verse|chorus|pre-chorus|post-chorus|bridge|outro|intro|hook|refrain|interlude|điệp\s*khúc|lời|đoạn)[^\]]*]|\((?:verse|chorus|pre-chorus|post-chorus|bridge|outro|intro|hook|refrain|interlude|điệp\s*khúc|lời|đoạn)[^)]*\))\s*[:\-–—]?\s*""",
        RegexOption.IGNORE_CASE
    )
    private val TARGET_FRAME_IDS = hashSetOf(
        "SYLT", "SLT",
        "USLT", "ULT",
        "TXXX", "TXX",
        "COMM", "COM",
        "TIT2", "TT2",
        "TPE1", "TP1"
    )

    /**
     * Kiểm tra và làm sạch một dòng văn bản lời bài hát:
     * - Trả về `null` nếu dòng đó là đoạn nhạc dạo, ký hiệu nốt nhạc, dòng trống hoặc dòng credit.
     * - Trả về nội dung câu hát sạch nếu là lời hát thực sự.
     */
    fun cleanLyricLineText(raw: String, isNearBoundary: Boolean = false): String? {
        var text = raw
            .replace("\uFEFF", "")
            .replace("\u0000", "")
            .replace("\r", "")
            .replace(INLINE_INSTRUMENTAL_TAG_REGEX, " ")
            .replace(LEADING_TRAILING_NOTES_REGEX, "")
            .trim()

        if (text.isEmpty()) return null
        if (METADATA_TAG_LINE_REGEX.matches(text)) return null
        if (SYMBOLS_ONLY_REGEX.matches(text)) return null
        if (INSTRUMENTAL_LINE_REGEX.matches(text)) return null
        if (CREDIT_LINE_REGEX.containsMatchIn(text)) return null

        // Loại bỏ nhãn phân đoạn đứng đầu câu như "[Điệp khúc]", "[Verse 1]:", "(Chorus)"
        text = text.replace(SECTION_PREFIX_REGEX, "")
            .replace(LEADING_TRAILING_NOTES_REGEX, "")
            .trim()

        if (text.isEmpty() || SYMBOLS_ONLY_REGEX.matches(text) || INSTRUMENTAL_LINE_REGEX.matches(text)) {
            return null
        }

        if (isNearBoundary && (text.contains(".vn", ignoreCase = true) || text.contains(".com", ignoreCase = true) || text.contains(".net", ignoreCase = true))) {
            return null
        }

        return text
    }

    fun extractFromSong(context: Context, song: Song): ExtractedEmbeddedLyrics? {
        // 1. Kiểm tra file .lrc cùng tên nằm cạnh file .mp3 trên bộ nhớ (nếu có)
        extractSidecarLrcIfPresent(song)?.let { return it }

        // 2. Đọc trực tiếp từ luồng file MP3 qua ContentResolver (an toàn với Scoped Storage Android 10-16)
        return try {
            openSongInputStream(context, song)?.use { stream ->
                extractFromStream(stream, song.duration)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractSidecarLrcIfPresent(song: Song): ExtractedEmbeddedLyrics? {
        val path = song.path?.takeIf { it.isNotBlank() } ?: return null
        return try {
            val mp3File = File(path)
            val parent = mp3File.parentFile ?: return null
            val baseName = mp3File.nameWithoutExtension
            val candidates = listOf(
                File(parent, "$baseName.lrc"),
                File(parent, "$baseName.LRC")
            )
            for (lrcFile in candidates) {
                if (lrcFile.exists() && lrcFile.canRead() && lrcFile.length() in 16..(512 * 1024)) {
                    val rawLrc = lrcFile.readText(Charsets.UTF_8)
                    val synced = parseLrcText(rawLrc, song.duration)
                    if (synced.isNotEmpty()) {
                        return ExtractedEmbeddedLyrics(lines = synced, isSynced = true)
                    }
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    internal fun openSongInputStream(context: Context, song: Song): InputStream? {
        try {
            context.contentResolver.openInputStream(song.uri)?.let { return it }
        } catch (_: Throwable) {
        }
        try {
            if (!song.path.isNullOrBlank()) {
                val file = File(song.path)
                if (file.exists() && file.canRead()) {
                    return FileInputStream(file)
                }
            }
        } catch (_: Throwable) {
        }
        return null
    }

    fun extractFromStream(inputStream: InputStream, totalDurationMs: Long): ExtractedEmbeddedLyrics? {
        val header = ByteArray(10)
        if (readFully(inputStream, header, 0, 10) < 10) return null
        if (header[0] != 'I'.code.toByte() ||
            header[1] != 'D'.code.toByte() ||
            header[2] != '3'.code.toByte()
        ) {
            return null
        }

        val versionMajor = header[3].toInt() and 0xFF
        val flags = header[5].toInt() and 0xFF
        val tagSize = decodeSynchsafeInt(header, 6)
        if (tagSize <= 0) return null

        if (tagSize <= 1024 * 1024) {
            val tagBytes = ByteArray(tagSize)
            val actualRead = readFully(inputStream, tagBytes, 0, tagSize)
            if (actualRead <= 0) return null
            return parseId3v2TagBytes(tagBytes, actualRead, versionMajor, flags, totalDurationMs)
        }

        return parseLargeId3v2Stream(inputStream, tagSize, versionMajor, flags, totalDurationMs)
    }

    private fun parseLargeId3v2Stream(
        inputStream: InputStream,
        tagSize: Int,
        versionMajor: Int,
        flags: Int,
        totalDurationMs: Long
    ): ExtractedEmbeddedLyrics? {
        var remaining = tagSize.coerceAtMost(15 * 1024 * 1024)

        if ((flags and 0x40) != 0 && remaining >= 4) {
            val extHeaderSizeBuf = ByteArray(4)
            if (readFully(inputStream, extHeaderSizeBuf, 0, 4) < 4) return null
            remaining -= 4
            val extSize = if (versionMajor == 4) {
                decodeSynchsafeInt(extHeaderSizeBuf, 0) - 4
            } else {
                decodeBigEndianInt(extHeaderSizeBuf, 0)
            }
            if (extSize > 0 && extSize <= remaining) {
                skipFully(inputStream, extSize.toLong())
                remaining -= extSize
            }
        }

        val frameHeaderSize = if (versionMajor == 2) 6 else 10
        val frameHeader = ByteArray(frameHeaderSize)

        var syltLines: List<LyricLine>? = null
        var usltRawText: String? = null
        var commLrcText: String? = null
        var embeddedTitle: String? = null
        var embeddedArtist: String? = null

        while (remaining >= frameHeaderSize) {
            val readHdr = readFully(inputStream, frameHeader, 0, frameHeaderSize)
            if (readHdr < frameHeaderSize) break
            remaining -= frameHeaderSize

            if (frameHeader[0].toInt() == 0) break

            val frameId: String
            val frameSize: Int
            var extraFlagBytes = 0
            if (versionMajor == 2) {
                frameId = String(frameHeader, 0, 3, StandardCharsets.ISO_8859_1)
                frameSize = ((frameHeader[3].toInt() and 0xFF) shl 16) or
                    ((frameHeader[4].toInt() and 0xFF) shl 8) or
                    (frameHeader[5].toInt() and 0xFF)
            } else {
                frameId = String(frameHeader, 0, 4, StandardCharsets.ISO_8859_1)
                val rawBigEndian = decodeBigEndianInt(frameHeader, 4)
                val rawSynchsafe = decodeSynchsafeInt(frameHeader, 4)
                frameSize = if (versionMajor == 4) {
                    if (rawSynchsafe in 1..remaining && (rawBigEndian > remaining || rawBigEndian == rawSynchsafe)) {
                        rawSynchsafe
                    } else {
                        rawBigEndian
                    }
                } else {
                    rawBigEndian
                }
                val formatFlags = frameHeader[9].toInt() and 0xFF
                if (versionMajor == 4 && (formatFlags and 0x01) != 0) {
                    extraFlagBytes = 4
                }
            }

            if (frameSize <= 0 || frameSize > remaining) break

            val isTargetFrame = frameId in TARGET_FRAME_IDS
            if (isTargetFrame && frameSize <= 512 * 1024) {
                val payload = ByteArray(frameSize)
                val readPayload = readFully(inputStream, payload, 0, frameSize)
                remaining -= readPayload
                if (readPayload == frameSize && extraFlagBytes < payload.size) {
                    val pStart = extraFlagBytes
                    val pEnd = payload.size
                    when (frameId) {
                        "SYLT", "SLT" -> {
                            val parsed = parseSyltFrame(payload, pStart, pEnd, totalDurationMs)
                            if (!parsed.isNullOrEmpty()) syltLines = parsed
                        }
                        "USLT", "ULT" -> {
                            val text = parseUsltFrame(payload, pStart, pEnd)
                            if (!text.isNullOrBlank()) usltRawText = text
                        }
                        "TXXX", "TXX" -> {
                            val text = parseTxxxLyricsFrame(payload, pStart, pEnd)
                            if (!text.isNullOrBlank() && usltRawText.isNullOrBlank()) usltRawText = text
                        }
                        "COMM", "COM" -> {
                            val text = parseUsltFrame(payload, pStart, pEnd)
                            if (!text.isNullOrBlank() && LRC_TIMESTAMP_REGEX.containsMatchIn(text)) {
                                commLrcText = text
                            }
                        }
                        "TIT2", "TT2" -> {
                            embeddedTitle = parseTextInformationFrame(payload, pStart, pEnd)?.takeIf { it.isNotBlank() }
                        }
                        "TPE1", "TP1" -> {
                            embeddedArtist = parseTextInformationFrame(payload, pStart, pEnd)?.takeIf { it.isNotBlank() }
                        }
                    }
                }
            } else {
                skipFully(inputStream, frameSize.toLong())
                remaining -= frameSize
            }
        }

        val finalUslt = usltRawText ?: commLrcText
        return buildExtractionResult(syltLines, finalUslt, embeddedTitle, embeddedArtist, totalDurationMs)
    }

    internal fun parseId3v2TagBytes(
        tagBytes: ByteArray,
        limit: Int = tagBytes.size,
        versionMajor: Int = 3,
        flags: Int = 0,
        totalDurationMs: Long = 180_000L
    ): ExtractedEmbeddedLyrics? {
        var offset = 0

        if ((flags and 0x40) != 0 && offset + 4 <= limit) {
            val extSize = if (versionMajor == 4) {
                decodeSynchsafeInt(tagBytes, offset)
            } else {
                decodeBigEndianInt(tagBytes, offset)
            }
            offset += if (versionMajor == 4) extSize else (extSize + 4)
        }

        var syltLines: List<LyricLine>? = null
        var usltRawText: String? = null
        var commLrcText: String? = null
        var embeddedTitle: String? = null
        var embeddedArtist: String? = null

        val frameHeaderSize = if (versionMajor == 2) 6 else 10

        while (offset + frameHeaderSize <= limit) {
            if (tagBytes[offset].toInt() == 0) break

            val frameId: String
            val frameSize: Int
            var extraFlagBytes = 0

            if (versionMajor == 2) {
                frameId = String(tagBytes, offset, 3, StandardCharsets.ISO_8859_1)
                frameSize = ((tagBytes[offset + 3].toInt() and 0xFF) shl 16) or
                    ((tagBytes[offset + 4].toInt() and 0xFF) shl 8) or
                    (tagBytes[offset + 5].toInt() and 0xFF)
            } else {
                frameId = String(tagBytes, offset, 4, StandardCharsets.ISO_8859_1)
                val remaining = limit - (offset + frameHeaderSize)
                val bigEndian = decodeBigEndianInt(tagBytes, offset + 4)
                val synchsafe = decodeSynchsafeInt(tagBytes, offset + 4)
                frameSize = if (versionMajor == 4) {
                    if (synchsafe in 1..remaining && (bigEndian > remaining || bigEndian == synchsafe)) {
                        synchsafe
                    } else {
                        bigEndian
                    }
                } else {
                    bigEndian
                }
                val formatFlags = tagBytes[offset + 9].toInt() and 0xFF
                if (versionMajor == 4 && (formatFlags and 0x01) != 0) {
                    extraFlagBytes = 4
                }
            }

            if (frameSize <= 0 || offset + frameHeaderSize + frameSize > limit) {
                break
            }

            val payloadStart = offset + frameHeaderSize + extraFlagBytes
            val payloadEnd = offset + frameHeaderSize + frameSize

            if (payloadStart < payloadEnd) {
                when (frameId) {
                    "SYLT", "SLT" -> {
                        val parsed = parseSyltFrame(tagBytes, payloadStart, payloadEnd, totalDurationMs)
                        if (!parsed.isNullOrEmpty()) {
                            syltLines = parsed
                        }
                    }
                    "USLT", "ULT" -> {
                        val text = parseUsltFrame(tagBytes, payloadStart, payloadEnd)
                        if (!text.isNullOrBlank()) {
                            usltRawText = text
                        }
                    }
                    "TXXX", "TXX" -> {
                        val text = parseTxxxLyricsFrame(tagBytes, payloadStart, payloadEnd)
                        if (!text.isNullOrBlank() && usltRawText.isNullOrBlank()) {
                            usltRawText = text
                        }
                    }
                    "COMM", "COM" -> {
                        val text = parseUsltFrame(tagBytes, payloadStart, payloadEnd)
                        if (!text.isNullOrBlank() && LRC_TIMESTAMP_REGEX.containsMatchIn(text)) {
                            commLrcText = text
                        }
                    }
                    "TIT2", "TT2" -> {
                        embeddedTitle = parseTextInformationFrame(tagBytes, payloadStart, payloadEnd)?.takeIf { it.isNotBlank() }
                    }
                    "TPE1", "TP1" -> {
                        embeddedArtist = parseTextInformationFrame(tagBytes, payloadStart, payloadEnd)?.takeIf { it.isNotBlank() }
                    }
                }
            }

            offset = payloadEnd
        }

        if (syltLines.isNullOrEmpty() && usltRawText.isNullOrBlank() && commLrcText.isNullOrBlank()) {
            usltRawText = scanTagForUsltSignature(tagBytes, limit)
        }

        val finalUslt = usltRawText ?: commLrcText
        return buildExtractionResult(syltLines, finalUslt, embeddedTitle, embeddedArtist, totalDurationMs)
    }

    private fun buildExtractionResult(
        syltLines: List<LyricLine>?,
        usltRawText: String?,
        embeddedTitle: String?,
        embeddedArtist: String?,
        totalDurationMs: Long
    ): ExtractedEmbeddedLyrics? {
        if (!syltLines.isNullOrEmpty()) {
            return ExtractedEmbeddedLyrics(
                lines = syltLines,
                isSynced = true,
                embeddedTitle = embeddedTitle,
                embeddedArtist = embeddedArtist
            )
        }

        if (!usltRawText.isNullOrBlank()) {
            val lrcLines = parseLrcText(usltRawText, totalDurationMs)
            if (lrcLines.isNotEmpty()) {
                return ExtractedEmbeddedLyrics(
                    lines = lrcLines,
                    isSynced = true,
                    embeddedTitle = embeddedTitle,
                    embeddedArtist = embeddedArtist
                )
            }
            val plainLines = parsePlainLyrics(usltRawText, totalDurationMs)
            if (plainLines.isNotEmpty()) {
                return ExtractedEmbeddedLyrics(
                    lines = plainLines,
                    isSynced = false,
                    embeddedTitle = embeddedTitle,
                    embeddedArtist = embeddedArtist
                )
            }
        }

        if (!embeddedTitle.isNullOrBlank() || !embeddedArtist.isNullOrBlank()) {
            return ExtractedEmbeddedLyrics(
                lines = emptyList(),
                isSynced = false,
                embeddedTitle = embeddedTitle,
                embeddedArtist = embeddedArtist
            )
        }

        return null
    }

    private fun scanTagForUsltSignature(bytes: ByteArray, limit: Int): String? {
        var i = 0
        while (i + 14 < limit) {
            if (bytes[i] == 'U'.code.toByte() &&
                bytes[i + 1] == 'S'.code.toByte() &&
                bytes[i + 2] == 'L'.code.toByte() &&
                bytes[i + 3] == 'T'.code.toByte()
            ) {
                val sizeBe = decodeBigEndianInt(bytes, i + 4)
                val sizeSync = decodeSynchsafeInt(bytes, i + 4)
                val remaining = limit - (i + 10)
                val size = when {
                    sizeBe in 6..remaining -> sizeBe
                    sizeSync in 6..remaining -> sizeSync
                    else -> -1
                }
                if (size > 0) {
                    val text = parseUsltFrame(bytes, i + 10, i + 10 + size)
                    if (!text.isNullOrBlank()) return text
                }
            }
            i++
        }
        return null
    }

    private fun parseTextInformationFrame(bytes: ByteArray, start: Int, end: Int): String? {
        if (end - start < 2) return null
        val encoding = bytes[start].toInt() and 0xFF
        val charset = resolveCharset(encoding)
        return decodeText(bytes, start + 1, end - (start + 1), encoding, charset)
            .replace("\u0000", "")
            .trim()
    }

    internal fun parseSyltFrame(
        bytes: ByteArray,
        start: Int,
        end: Int,
        totalDurationMs: Long
    ): List<LyricLine>? {
        if (end - start < 7) return null
        val encoding = bytes[start].toInt() and 0xFF
        val timeFormat = bytes[start + 4].toInt() and 0xFF
        val charset = resolveCharset(encoding)
        val isTwoByteNull = encoding == 1 || encoding == 2

        var cursor = findTerminatorEnd(bytes, start + 6, end, isTwoByteNull)
        val rawEvents = ArrayList<TimedRawEntry>()

        while (cursor < end) {
            val textStart = cursor
            val termPos = findTerminatorStart(bytes, textStart, end, isTwoByteNull)
            val textEnd = if (termPos >= 0) termPos else break
            val rawSegment = decodeText(bytes, textStart, textEnd - textStart, encoding, charset)
                .replace("\r", "")
                .trim()

            cursor = if (isTwoByteNull) textEnd + 2 else textEnd + 1
            if (cursor + 4 > end) break

            val rawTime = decodeBigEndianInt(bytes, cursor).toLong() and 0xFFFFFFFFL
            cursor += 4

            val timeMs = if (timeFormat == 1) rawTime * 26L else rawTime
            val cleaned = cleanLyricLineText(rawSegment, isNearBoundary = rawEvents.size < 2)
            rawEvents.add(TimedRawEntry(timeMs = timeMs, cleanText = cleaned))
        }

        val result = buildSyncedLyricLinesWithInstrumentalGaps(rawEvents, totalDurationMs)
        return result.takeIf { it.isNotEmpty() }
    }

    internal fun parseUsltFrame(bytes: ByteArray, start: Int, end: Int): String? {
        if (end - start < 5) return null
        val encoding = bytes[start].toInt() and 0xFF
        val charset = resolveCharset(encoding)
        val isTwoByteNull = encoding == 1 || encoding == 2

        val textStart = findTerminatorEnd(bytes, start + 4, end, isTwoByteNull)
        if (textStart >= end) return null
        return decodeText(bytes, textStart, end - textStart, encoding, charset).trim()
    }

    private fun parseTxxxLyricsFrame(bytes: ByteArray, start: Int, end: Int): String? {
        if (end - start < 3) return null
        val encoding = bytes[start].toInt() and 0xFF
        val charset = resolveCharset(encoding)
        val isTwoByteNull = encoding == 1 || encoding == 2

        val descEnd = findTerminatorStart(bytes, start + 1, end, isTwoByteNull)
        if (descEnd < 0) return null
        val description = decodeText(bytes, start + 1, descEnd - (start + 1), encoding, charset)
            .trim()
            .lowercase()

        if (!description.contains("lyric") && !description.contains("lrc") && !description.contains("unsynced")) {
            return null
        }

        val valueStart = if (isTwoByteNull) descEnd + 2 else descEnd + 1
        if (valueStart >= end) return null
        return decodeText(bytes, valueStart, end - valueStart, encoding, charset).trim()
    }

    internal data class RawWordStamp(
        val timeMs: Long,
        val startCharIndex: Int,
        val endCharIndex: Int
    )

    internal data class TimedRawEntry(
        val timeMs: Long,
        val cleanText: String?,
        val rawWordStamps: List<RawWordStamp> = emptyList()
    )

    private fun parseMatchToMs(match: MatchResult): Long {
        val minutes = match.groupValues[1].toLongOrNull() ?: 0L
        val seconds = match.groupValues[2].toLongOrNull() ?: 0L
        val fractionStr = match.groupValues.getOrNull(3).orEmpty()
        val millis = when (fractionStr.length) {
            1 -> (fractionStr.toLongOrNull() ?: 0L) * 100L
            2 -> (fractionStr.toLongOrNull() ?: 0L) * 10L
            3 -> fractionStr.toLongOrNull() ?: 0L
            else -> 0L
        }
        return minutes * 60_000L + seconds * 1_000L + millis
    }

    /**
     * Parse chuỗi lời bài hát chuẩn LRC (`[mm:ss.xx] Nội dung câu`) và Enhanced LRC (`<mm:ss.xx>` từng từ):
     * - Hỗ trợ thẻ `[offset:+/-ms]` để dịch chuyển đồng bộ chính xác toàn bài.
     * - Tự động bỏ qua hoàn toàn các đoạn nhạc dạo, giang tấu, dòng trống `[mm:ss.xx]` hoặc ký hiệu âm nhạc `♪`.
     * - Sử dụng mốc thời gian của dòng nhạc dạo / dòng trống bị lược bỏ để ngắt chuẩn xác `endTimeMs`
     *   của câu hát liền trước, giúp câu hát kết thúc đúng lúc ca sĩ ngừng hát.
     */
    fun parseLrcText(rawLrc: String, totalDurationMs: Long): List<LyricLine> {
        if (rawLrc.isBlank()) return emptyList()
        val rawEntries = ArrayList<TimedRawEntry>()
        val allLines = rawLrc.lines()

        var offsetMs = 0L
        for (rawLine in allLines) {
            val offsetMatch = OFFSET_TAG_REGEX.matchEntire(rawLine.trim())
            if (offsetMatch != null) {
                offsetMs = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
            }
        }

        allLines.forEachIndexed { lineIdx, rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || METADATA_TAG_LINE_REGEX.matches(trimmed)) {
                return@forEachIndexed
            }

            val matches = LRC_TIMESTAMP_REGEX.findAll(trimmed).toList()
            if (matches.isEmpty()) return@forEachIndexed

            val lastMatch = matches.last()
            val contentWithWordTags = trimmed.substring(lastMatch.range.last + 1)
                .replace(LRC_TIMESTAMP_REGEX, "")
                .trim()

            val wordMatches = WORD_TIMESTAMP_REGEX.findAll(contentWithWordTags).toList()
            val isBoundary = lineIdx < 3 || lineIdx >= allLines.size - 2

            if (wordMatches.isEmpty()) {
                val cleanedText = cleanLyricLineText(contentWithWordTags, isNearBoundary = isBoundary)
                for (match in matches) {
                    val timestampMs = (parseMatchToMs(match) - offsetMs).coerceAtLeast(0L)
                    rawEntries.add(TimedRawEntry(timeMs = timestampMs, cleanText = cleanedText))
                }
            } else {
                // Tách lời sạch và ghi nhận mốc thời gian từng từ (<mm:ss.xx>)
                val textBuilder = StringBuilder()
                val rawStamps = ArrayList<RawWordStamp>(wordMatches.size)
                var cursor = 0
                val leadText = contentWithWordTags.substring(0, wordMatches.first().range.first).trim()
                if (leadText.isNotEmpty()) {
                    textBuilder.append(leadText)
                    cursor = textBuilder.length
                }
                for (idx in wordMatches.indices) {
                    val wm = wordMatches[idx]
                    val nextStart = if (idx < wordMatches.lastIndex) {
                        wordMatches[idx + 1].range.first
                    } else {
                        contentWithWordTags.length
                    }
                    val token = contentWithWordTags.substring(wm.range.last + 1, nextStart)
                    val trimmedToken = token.trim()
                    if (trimmedToken.isNotEmpty()) {
                        if (textBuilder.isNotEmpty() && !textBuilder.endsWith(" ")) {
                            textBuilder.append(' ')
                        }
                        val startIdx = textBuilder.length
                        textBuilder.append(trimmedToken)
                        val endIdx = textBuilder.length
                        val wordTimeMs = (parseMatchToMs(wm) - offsetMs).coerceAtLeast(0L)
                        rawStamps.add(
                            RawWordStamp(
                                timeMs = wordTimeMs,
                                startCharIndex = startIdx,
                                endCharIndex = endIdx
                            )
                        )
                    }
                }

                val plainMerged = textBuilder.toString().trim()
                val cleanedText = cleanLyricLineText(plainMerged, isNearBoundary = isBoundary)
                val validStamps = if (cleanedText != null && cleanedText == plainMerged) {
                    rawStamps
                } else {
                    emptyList()
                }
                for (match in matches) {
                    val timestampMs = (parseMatchToMs(match) - offsetMs).coerceAtLeast(0L)
                    rawEntries.add(
                        TimedRawEntry(
                            timeMs = timestampMs,
                            cleanText = cleanedText,
                            rawWordStamps = validStamps
                        )
                    )
                }
            }
        }

        if (rawEntries.isEmpty()) return emptyList()
        return buildSyncedLyricLinesWithInstrumentalGaps(rawEntries, totalDurationMs)
    }

    /**
     * Chuẩn hóa lời bài hát thuần văn bản (không có mốc thời gian) thành `List<LyricLine>`,
     * lọc bỏ các đoạn đánh dấu nhạc dạo `[Nhạc]`, `[Intro]`, `[Instrumental]`, `♪`, v.v.
     */
    fun parsePlainLyrics(rawText: String, totalDurationMs: Long): List<LyricLine> {
        val rawLines = rawText.lines()
        val cleanLines = rawLines.mapIndexedNotNull { idx, line ->
            val isBoundary = idx < 2 || idx >= rawLines.size - 2
            cleanLyricLineText(line, isNearBoundary = isBoundary)
        }

        if (cleanLines.isEmpty()) return emptyList()
        val safeDuration = totalDurationMs.coerceAtLeast(cleanLines.size * 3000L)
        val stepMs = (safeDuration / cleanLines.size).coerceAtLeast(1000L)

        return cleanLines.mapIndexed { idx, text ->
            val startMs = idx * stepMs
            val endMs = if (idx == cleanLines.lastIndex) {
                safeDuration.coerceAtLeast(startMs + stepMs)
            } else {
                (idx + 1) * stepMs
            }
            LyricLine(
                index = idx,
                startTimeMs = startMs,
                endTimeMs = endMs,
                text = text
            )
        }
    }

    private fun buildSyncedLyricLinesWithInstrumentalGaps(
        entries: List<TimedRawEntry>,
        totalDurationMs: Long
    ): List<LyricLine> {
        val sorted = entries.sortedBy { it.timeMs }

        data class MutableLyricBuilder(
            val startTimeMs: Long,
            var endBoundaryMs: Long?,
            var text: String,
            val rawWordStamps: List<RawWordStamp> = emptyList()
        )

        val builders = ArrayList<MutableLyricBuilder>()
        for (entry in sorted) {
            val t = entry.timeMs.coerceAtLeast(0L)
            val text = entry.cleanText
            if (text == null) {
                val prev = builders.lastOrNull()
                if (prev != null && prev.endBoundaryMs == null && t > prev.startTimeMs + 250L) {
                    prev.endBoundaryMs = t
                }
            } else {
                val prev = builders.lastOrNull()
                if (prev != null && prev.startTimeMs == t && prev.endBoundaryMs == null) {
                    if (!prev.text.equals(text, ignoreCase = true)) {
                        prev.text = "${prev.text} $text"
                    }
                } else {
                    builders.add(
                        MutableLyricBuilder(
                            startTimeMs = t,
                            endBoundaryMs = null,
                            text = text,
                            rawWordStamps = entry.rawWordStamps
                        )
                    )
                }
            }
        }

        if (builders.isEmpty()) return emptyList()

        val lastStart = builders.last().startTimeMs
        val effectiveTotalDuration = maxOf(totalDurationMs, lastStart + 5_000L)

        return builders.mapIndexed { index, item ->
            val nextStartMs = if (index < builders.lastIndex) {
                builders[index + 1].startTimeMs.coerceAtLeast(item.startTimeMs + 250L)
            } else {
                effectiveTotalDuration.coerceAtLeast(item.startTimeMs + 1_000L)
            }
            val boundedEndMs = item.endBoundaryMs
                ?.coerceIn(item.startTimeMs + 250L, nextStartMs)
                ?: nextStartMs

            val segments = if (item.rawWordStamps.isNotEmpty()) {
                item.rawWordStamps.mapIndexed { segIdx, stamp ->
                    val segEnd = if (segIdx < item.rawWordStamps.lastIndex) {
                        item.rawWordStamps[segIdx + 1].timeMs.coerceAtLeast(stamp.timeMs + 60L)
                    } else {
                        val gapToNextLine = boundedEndMs - stamp.timeMs
                        if (gapToNextLine > 2_500L) {
                            val avgWordSpan = if (item.rawWordStamps.size >= 2) {
                                ((stamp.timeMs - item.rawWordStamps.first().timeMs) /
                                    (item.rawWordStamps.size - 1).coerceAtLeast(1))
                                    .coerceIn(450L, 1_800L)
                            } else {
                                1_400L
                            }
                            (stamp.timeMs + avgWordSpan).coerceAtMost(boundedEndMs)
                        } else {
                            boundedEndMs.coerceAtLeast(stamp.timeMs + 120L)
                        }
                    }
                    LyricWordSegment(
                        startTimeMs = stamp.timeMs.coerceIn(item.startTimeMs, boundedEndMs),
                        endTimeMs = segEnd.coerceIn(item.startTimeMs, boundedEndMs),
                        startCharIndex = stamp.startCharIndex,
                        endCharIndex = stamp.endCharIndex
                    )
                }
            } else {
                emptyList()
            }

            LyricLine(
                index = index,
                startTimeMs = item.startTimeMs,
                endTimeMs = boundedEndMs,
                text = item.text,
                segments = segments
            )
        }
    }

    internal fun buildSyncedLyricLines(
        sortedEntries: List<Pair<Long, String>>,
        totalDurationMs: Long
    ): List<LyricLine> {
        val timedEntries = sortedEntries.mapIndexed { idx, (timeMs, rawText) ->
            TimedRawEntry(
                timeMs = timeMs,
                cleanText = cleanLyricLineText(rawText, isNearBoundary = idx < 2)
            )
        }
        return buildSyncedLyricLinesWithInstrumentalGaps(timedEntries, totalDurationMs)
    }

    private fun resolveCharset(encoding: Int): Charset = when (encoding) {
        1 -> StandardCharsets.UTF_16
        2 -> StandardCharsets.UTF_16BE
        3 -> StandardCharsets.UTF_8
        else -> StandardCharsets.ISO_8859_1
    }

    private fun decodeText(
        bytes: ByteArray,
        offset: Int,
        length: Int,
        encoding: Int,
        fallbackCharset: Charset
    ): String {
        if (length <= 0 || offset < 0 || offset + length > bytes.size) return ""
        return try {
            if (encoding == 1 && length >= 2) {
                val b0 = bytes[offset].toInt() and 0xFF
                val b1 = bytes[offset + 1].toInt() and 0xFF
                when {
                    b0 == 0xFF && b1 == 0xFE ->
                        String(bytes, offset + 2, length - 2, StandardCharsets.UTF_16LE)
                    b0 == 0xFE && b1 == 0xFF ->
                        String(bytes, offset + 2, length - 2, StandardCharsets.UTF_16BE)
                    else ->
                        String(bytes, offset, length, StandardCharsets.UTF_16LE)
                }
            } else {
                String(bytes, offset, length, fallbackCharset)
            }
        } catch (_: Throwable) {
            ""
        }
    }

    private fun findTerminatorStart(
        bytes: ByteArray,
        start: Int,
        end: Int,
        isTwoByteNull: Boolean
    ): Int {
        if (isTwoByteNull) {
            var i = start
            while (i + 1 < end) {
                if (bytes[i].toInt() == 0 && bytes[i + 1].toInt() == 0) {
                    return i
                }
                i += 2
            }
            return -1
        } else {
            for (i in start until end) {
                if (bytes[i].toInt() == 0) return i
            }
            return -1
        }
    }

    private fun findTerminatorEnd(
        bytes: ByteArray,
        start: Int,
        end: Int,
        isTwoByteNull: Boolean
    ): Int {
        val termStart = findTerminatorStart(bytes, start, end, isTwoByteNull)
        if (termStart < 0) return start
        return if (isTwoByteNull) (termStart + 2).coerceAtMost(end) else (termStart + 1).coerceAtMost(end)
    }

    internal fun decodeSynchsafeInt(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)
    }

    internal fun decodeBigEndianInt(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun readFully(stream: InputStream, buffer: ByteArray, offset: Int, length: Int): Int {
        var total = 0
        while (total < length) {
            val read = stream.read(buffer, offset + total, length - total)
            if (read <= 0) break
            total += read
        }
        return total
    }

    private fun skipFully(stream: InputStream, bytesToSkip: Long) {
        var remaining = bytesToSkip
        val discardBuf = ByteArray(8192)
        while (remaining > 0L) {
            val skipped = stream.skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped
            } else {
                val toRead = remaining.coerceAtMost(discardBuf.size.toLong()).toInt()
                val read = stream.read(discardBuf, 0, toRead)
                if (read <= 0) break
                remaining -= read
            }
        }
    }

    /**
     * Tiện ích đóng gói khung ID3v2 (USLT) phục vụ kiểm thử và xác minh parser.
     */
    internal fun buildTestId3v2TagWithUslt(lyricsText: String): ByteArray {
        val textBytes = lyricsText.toByteArray(StandardCharsets.UTF_8)
        val payload = ByteArray(1 + 3 + 1 + textBytes.size)
        payload[0] = 3
        payload[1] = 'v'.code.toByte()
        payload[2] = 'i'.code.toByte()
        payload[3] = 'e'.code.toByte()
        payload[4] = 0
        System.arraycopy(textBytes, 0, payload, 5, textBytes.size)

        val frameSize = payload.size
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 3, 0, 0))
        val totalTagSize = 10 + frameSize
        out.write(
            byteArrayOf(
                ((totalTagSize ushr 21) and 0x7F).toByte(),
                ((totalTagSize ushr 14) and 0x7F).toByte(),
                ((totalTagSize ushr 7) and 0x7F).toByte(),
                (totalTagSize and 0x7F).toByte()
            )
        )
        out.write(byteArrayOf('U'.code.toByte(), 'S'.code.toByte(), 'L'.code.toByte(), 'T'.code.toByte()))
        out.write(
            byteArrayOf(
                ((frameSize ushr 24) and 0xFF).toByte(),
                ((frameSize ushr 16) and 0xFF).toByte(),
                ((frameSize ushr 8) and 0xFF).toByte(),
                (frameSize and 0xFF).toByte()
            )
        )
        out.write(byteArrayOf(0, 0))
        out.write(payload)
        return out.toByteArray()
    }
}
