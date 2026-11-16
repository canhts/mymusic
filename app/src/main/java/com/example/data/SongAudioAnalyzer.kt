package com.example.data

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.LruCache
import androidx.compose.runtime.Immutable
import com.example.model.Song
import java.io.File
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Khung năng lượng âm thanh thực theo thời gian tại một thời điểm phát của bài hát.
 * Mọi thành phần đều nằm trong khoảng [0f, 1f], triệt tiêu hoàn toàn trạng thái giả lập khi im lặng.
 */
@Immutable
data class AudioBeatFrame(
    val overall: Float,
    val bass: Float,
    val lowMid: Float,
    val highMid: Float,
    val treble: Float,
    val onset: Float
) {
    val beatPulse: Float
        get() = (bass * 0.56f + onset * 0.44f).coerceIn(0f, 1f)

    companion object {
        val SILENT = AudioBeatFrame(
            overall = 0f,
            bass = 0f,
            lowMid = 0f,
            highMid = 0f,
            treble = 0f,
            onset = 0f
        )
    }
}

/**
 * Bản đồ phổ tần số 4 dải và xung nhịp (onset) thực tế của một bài hát ở độ phân giải 25ms.
 */
@Immutable
class SongSpectrumProfile(
    val songId: Long,
    val windowMs: Long,
    private val bass: ByteArray,
    private val lowMid: ByteArray,
    private val highMid: ByteArray,
    private val treble: ByteArray,
    private val overall: ByteArray,
    private val onset: ByteArray
) {
    val frameCount: Int get() = overall.size

    fun sampleAt(positionMs: Long): AudioBeatFrame {
        val count = overall.size
        if (count == 0 || windowMs <= 0L) return AudioBeatFrame.SILENT

        val clampedMs = positionMs.coerceAtLeast(0L)
        val exactIndex = clampedMs.toFloat() / windowMs.toFloat()
        val idx0 = exactIndex.toInt().coerceIn(0, count - 1)
        val idx1 = (idx0 + 1).coerceAtMost(count - 1)
        val frac = (exactIndex - idx0).coerceIn(0f, 1f)

        return AudioBeatFrame(
            overall = lerpByte(overall[idx0], overall[idx1], frac),
            bass = lerpByte(bass[idx0], bass[idx1], frac),
            lowMid = lerpByte(lowMid[idx0], lowMid[idx1], frac),
            highMid = lerpByte(highMid[idx0], highMid[idx1], frac),
            treble = lerpByte(treble[idx0], treble[idx1], frac),
            onset = lerpByte(onset[idx0], onset[idx1], frac)
        )
    }

    private fun lerpByte(b0: Byte, b1: Byte, frac: Float): Float {
        val v0 = (b0.toInt() and 0xFF) / 255f
        val v1 = (b1.toInt() and 0xFF) / 255f
        return (v0 + (v1 - v0) * frac).coerceIn(0f, 1f)
    }
}

/**
 * Bộ phân tích phổ âm thanh và nhịp trống thực từ chính tệp nhạc MP3 đang phát
 * thông qua `MediaExtractor` + `MediaCodec` (không cần quyền ghi âm microphone).
 */
class SongAudioAnalyzer(
    private val appContext: Context,
    private val scope: CoroutineScope
) {
    private val profileCache = LruCache<String, SongSpectrumProfile>(MAX_CACHED_PROFILES)

    private val _currentProfile = MutableStateFlow<SongSpectrumProfile?>(null)
    val currentProfile: StateFlow<SongSpectrumProfile?> = _currentProfile.asStateFlow()

    private var activeJob: Job? = null
    private var activeCacheKey: String? = null

    fun analyzeForSong(
        song: Song?,
        durationMs: Long,
        initialPositionMs: Long
    ) {
        if (song == null) {
            activeJob?.cancel()
            activeJob = null
            activeCacheKey = null
            _currentProfile.value = null
            return
        }

        val effectiveDuration = (if (durationMs > 0L) durationMs else song.duration).coerceAtLeast(1_000L)
        val cacheKey = "${song.id}_${song.uri}_$effectiveDuration"

        synchronized(profileCache) {
            profileCache.get(cacheKey)?.let { cached ->
                if (activeCacheKey == cacheKey && _currentProfile.value != null) return
                activeJob?.cancel()
                activeCacheKey = cacheKey
                _currentProfile.value = cached
                return
            }
        }

        if (activeCacheKey == cacheKey && activeJob?.isActive == true) {
            return
        }

        activeJob?.cancel()
        activeCacheKey = cacheKey
        _currentProfile.value = null

        activeJob = scope.launch(Dispatchers.Default) {
            try {
                val totalWindows = ((effectiveDuration / WINDOW_MS).toInt() + 1).coerceIn(40, MAX_WINDOWS)
                val builder = MutableSpectrumBuffer(song.id, WINDOW_MS, totalWindows)

                // Nếu người dùng mở trang chơi nhạc ở đoạn giữa bài, ưu tiên giải mã ngay vùng lân cận vị trí hiện tại
                if (initialPositionMs > PRIORITY_SEEK_THRESHOLD_MS) {
                    val quickStartMs = (initialPositionMs - 1_200L).coerceAtLeast(0L)
                    decodeRangeIntoBuffer(
                        song = song,
                        buffer = builder,
                        seekToMs = quickStartMs,
                        maxChunkMs = PRIORITY_WINDOW_DURATION_MS,
                        isActiveCheck = { isActive }
                    )
                    if (isActive && builder.hasAnySignal) {
                        _currentProfile.value = builder.toImmutableSnapshot()
                    }
                }

                // Giải mã toàn bộ bài hát từ đầu để mọi vị trí tua đều có nhịp thật chính xác
                decodeRangeIntoBuffer(
                    song = song,
                    buffer = builder,
                    seekToMs = 0L,
                    maxChunkMs = Long.MAX_VALUE,
                    onProgressMilestone = {
                        if (isActive) {
                            _currentProfile.value = builder.toImmutableSnapshot()
                        }
                    },
                    isActiveCheck = { isActive }
                )

                if (isActive && builder.hasAnySignal) {
                    val completed = builder.toImmutableSnapshot()
                    synchronized(profileCache) {
                        profileCache.put(cacheKey, completed)
                    }
                    _currentProfile.value = completed
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                // Giữ trạng thái im lặng nếu tệp không giải mã được, không phát sinh nhịp giả
            }
        }
    }

    private fun decodeRangeIntoBuffer(
        song: Song,
        buffer: MutableSpectrumBuffer,
        seekToMs: Long,
        maxChunkMs: Long,
        onProgressMilestone: (() -> Unit)? = null,
        isActiveCheck: () -> Boolean
    ) {
        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null
        try {
            extractor = MediaExtractor()
            var dataSourceSet = false
            if (!song.path.isNullOrBlank()) {
                val f = File(song.path)
                if (f.exists() && f.canRead()) {
                    extractor.setDataSource(f.absolutePath)
                    dataSourceSet = true
                }
            }
            if (!dataSourceSet) {
                extractor.setDataSource(appContext, song.uri, null)
            }

            var audioTrackIndex = -1
            var trackFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    trackFormat = format
                    break
                }
            }
            if (audioTrackIndex < 0 || trackFormat == null) return

            extractor.selectTrack(audioTrackIndex)
            if (seekToMs > 0L) {
                extractor.seekTo(seekToMs * 1_000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }

            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: return
            var sampleRate = if (trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceIn(8_000, 192_000)
            } else {
                44_100
            }
            var channelCount = if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 8)
            } else {
                2
            }
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(trackFormat, null, null, 0)
            codec.start()

            val processor = StreamingFourBandProcessor(sampleRate)
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var firstPtsMs = -1L
            var lastMilestoneEmitWindow = -1

            while (!outputDone && isActiveCheck()) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(2_000L)
                    if (inIndex >= 0) {
                        val inBuffer = codec.getInputBuffer(inIndex)
                        if (inBuffer != null) {
                            val sampleSize = extractor.readSampleData(inBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                val ptsUs = extractor.sampleTime
                                codec.queueInputBuffer(inIndex, 0, sampleSize, ptsUs, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 2_000L)
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = codec.outputFormat
                    if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceIn(8_000, 192_000)
                        processor.updateSampleRate(sampleRate)
                    }
                    if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 8)
                    }
                    if (newFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        pcmEncoding = newFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    }
                } else if (outIndex >= 0) {
                    val outBuffer = codec.getOutputBuffer(outIndex)
                    if (outBuffer != null && bufferInfo.size > 0) {
                        val ptsMs = (bufferInfo.presentationTimeUs / 1_000L).coerceAtLeast(0L)
                        if (firstPtsMs < 0L) firstPtsMs = ptsMs

                        outBuffer.position(bufferInfo.offset)
                        outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        outBuffer.order(ByteOrder.LITTLE_ENDIAN)

                        processor.processBuffer(
                            byteBuffer = outBuffer,
                            sizeBytes = bufferInfo.size,
                            channelCount = channelCount,
                            pcmEncoding = pcmEncoding,
                            startTimeMs = ptsMs,
                            targetBuffer = buffer
                        )

                        val curWin = (ptsMs / buffer.windowMs).toInt()
                        if (onProgressMilestone != null && curWin - lastMilestoneEmitWindow >= MILESTONE_WINDOWS) {
                            lastMilestoneEmitWindow = curWin
                            onProgressMilestone()
                        }

                        if (maxChunkMs != Long.MAX_VALUE && ptsMs - firstPtsMs >= maxChunkMs) {
                            outputDone = true
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER && inputDone) {
                    break
                }
            }
            processor.flushRemaining(buffer)
        } finally {
            try {
                codec?.stop()
            } catch (_: Throwable) {
            }
            try {
                codec?.release()
            } catch (_: Throwable) {
            }
            try {
                extractor?.release()
            } catch (_: Throwable) {
            }
        }
    }

    companion object {
        const val WINDOW_MS = 25L
        private const val MAX_WINDOWS = 48_000 // Tối đa ~20 phút ở 25ms/khung
        private const val MAX_CACHED_PROFILES = 8
        private const val PRIORITY_SEEK_THRESHOLD_MS = 2_500L
        private const val PRIORITY_WINDOW_DURATION_MS = 18_000L
        private const val MILESTONE_WINDOWS = 48 // Phát cập nhật mỗi ~1.2s nhạc đã giải mã
    }
}

internal class MutableSpectrumBuffer(
    private val songId: Long,
    val windowMs: Long,
    private val capacity: Int
) {
    private val rawTotal = FloatArray(capacity)
    private val rawBass = FloatArray(capacity)
    private val rawLowMid = FloatArray(capacity)
    private val rawHighMid = FloatArray(capacity)
    private val rawTreble = FloatArray(capacity)
    private var maxFilledIndex = -1

    @Volatile
    var hasAnySignal = false
        private set

    fun recordWindow(
        windowIndex: Int,
        totalRms: Float,
        bassRms: Float,
        lowMidRms: Float,
        highMidRms: Float,
        trebleRms: Float
    ) {
        if (windowIndex !in 0 until capacity) return
        if (totalRms > rawTotal[windowIndex]) rawTotal[windowIndex] = totalRms
        if (bassRms > rawBass[windowIndex]) rawBass[windowIndex] = bassRms
        if (lowMidRms > rawLowMid[windowIndex]) rawLowMid[windowIndex] = lowMidRms
        if (highMidRms > rawHighMid[windowIndex]) rawHighMid[windowIndex] = highMidRms
        if (trebleRms > rawTreble[windowIndex]) rawTreble[windowIndex] = trebleRms
        if (windowIndex > maxFilledIndex) maxFilledIndex = windowIndex
        if (totalRms > NOISE_FLOOR) hasAnySignal = true
    }

    fun toImmutableSnapshot(): SongSpectrumProfile {
        val len = (maxFilledIndex + 1).coerceIn(0, capacity)
        if (len == 0) {
            return SongSpectrumProfile(
                songId = songId,
                windowMs = windowMs,
                bass = ByteArray(0),
                lowMid = ByteArray(0),
                highMid = ByteArray(0),
                treble = ByteArray(0),
                overall = ByteArray(0),
                onset = ByteArray(0)
            )
        }

        val outBass = ByteArray(len)
        val outLowMid = ByteArray(len)
        val outHighMid = ByteArray(len)
        val outTreble = ByteArray(len)
        val outOverall = ByteArray(len)
        val outOnset = ByteArray(len)

        var globalPeakTotal = 0.06f
        for (i in 0 until len) {
            if (rawTotal[i] > globalPeakTotal) globalPeakTotal = rawTotal[i]
        }

        // Kết hợp đỉnh toàn cục + đường bao cục bộ (~1.5s) để cả đoạn êm lẫn đoạn cao trào đều phân tách rõ từng nhịp trống
        var localPeak = globalPeakTotal * 0.55f
        var avgBass = 0f
        var avgMid = 0f
        var avgTreble = 0f
        var onsetEnv = 0f

        for (i in 0 until len) {
            val t = rawTotal[i]
            val b = rawBass[i]
            val lm = rawLowMid[i]
            val hm = rawHighMid[i]
            val tr = rawTreble[i]

            localPeak = max(t, localPeak * 0.985f).coerceAtLeast(0.05f)
            val hybridCeiling = (localPeak * 0.55f + globalPeakTotal * 0.45f).coerceAtLeast(0.06f)
            val normScale = 1f / hybridCeiling

            val totalNorm = perceptualShape(t * normScale)
            val bassNorm = perceptualShape(b * normScale * 1.18f)
            val lowMidNorm = perceptualShape(lm * normScale * 1.55f)
            val highMidNorm = perceptualShape(hm * normScale * 2.05f)
            val trebleNorm = perceptualShape(tr * normScale * 2.95f)

            val midCombined = lowMidNorm * 0.5f + highMidNorm * 0.5f
            val flux = (
                max(0f, bassNorm - avgBass) * 0.60f +
                    max(0f, midCombined - avgMid) * 0.25f +
                    max(0f, trebleNorm - avgTreble) * 0.15f
                ) * 3.1f

            avgBass = avgBass * 0.74f + bassNorm * 0.26f
            avgMid = avgMid * 0.74f + midCombined * 0.26f
            avgTreble = avgTreble * 0.74f + trebleNorm * 0.26f

            onsetEnv = if (flux > onsetEnv) {
                flux.coerceIn(0f, 1f)
            } else {
                (onsetEnv * 0.62f).coerceIn(0f, 1f)
            }

            outOverall[i] = quantize(totalNorm)
            outBass[i] = quantize(bassNorm)
            outLowMid[i] = quantize(lowMidNorm)
            outHighMid[i] = quantize(highMidNorm)
            outTreble[i] = quantize(trebleNorm)
            outOnset[i] = quantize(onsetEnv)
        }

        return SongSpectrumProfile(
            songId = songId,
            windowMs = windowMs,
            bass = outBass,
            lowMid = outLowMid,
            highMid = outHighMid,
            treble = outTreble,
            overall = outOverall,
            onset = outOnset
        )
    }

    private fun perceptualShape(linear: Float): Float {
        if (linear <= NOISE_FLOOR) return 0f
        return linear.coerceIn(0f, 1f).pow(0.78f)
    }

    private fun quantize(v: Float): Byte = (v.coerceIn(0f, 1f) * 255f).toInt().toByte()

    companion object {
        private const val NOISE_FLOOR = 0.003f
    }
}

/**
 * Bộ tách 4 dải tần số bậc 2 (12dB/octave) không trôi thời gian tích lũy giữa các khung 25ms.
 */
internal class StreamingFourBandProcessor(sampleRateHz: Int) {
    private var sr = sampleRateHz.coerceIn(8_000, 192_000)
    private var alphaBass = 0f
    private var alphaLowMid = 0f
    private var alphaHighMid = 0f

    // Hai tầng lọc nối tiếp cho mỗi ngưỡng cắt giúp tách biệt rõ Bass / Mid / Treble
    private var lpBass1 = 0f
    private var lpBass2 = 0f
    private var lpLowMid1 = 0f
    private var lpLowMid2 = 0f
    private var lpHighMid1 = 0f
    private var lpHighMid2 = 0f

    private var accTotalSq = 0.0
    private var accBassSq = 0.0
    private var accLowMidSq = 0.0
    private var accHighMidSq = 0.0
    private var accTrebleSq = 0.0
    private var accCount = 0

    private var exactSamplesPerWindow = (sr * SongAudioAnalyzer.WINDOW_MS) / 1000.0
    private var exactSampleRemainder = 0.0
    private var currentWindowIndex = -1

    init {
        recomputeCoefficients()
    }

    fun updateSampleRate(newSampleRateHz: Int) {
        val clamped = newSampleRateHz.coerceIn(8_000, 192_000)
        if (sr != clamped) {
            sr = clamped
            recomputeCoefficients()
        }
    }

    private fun recomputeCoefficients() {
        val dt = 1f / sr.toFloat()
        alphaBass = alphaForCutoff(140f, dt)
        alphaLowMid = alphaForCutoff(580f, dt)
        alphaHighMid = alphaForCutoff(2800f, dt)
        exactSamplesPerWindow = (sr * SongAudioAnalyzer.WINDOW_MS) / 1000.0
    }

    private fun alphaForCutoff(cutoffHz: Float, dt: Float): Float {
        val rc = 1f / (2f * PI.toFloat() * cutoffHz)
        return (dt / (rc + dt)).coerceIn(0.001f, 0.999f)
    }

    fun processBuffer(
        byteBuffer: java.nio.ByteBuffer,
        sizeBytes: Int,
        channelCount: Int,
        pcmEncoding: Int,
        startTimeMs: Long,
        targetBuffer: MutableSpectrumBuffer
    ) {
        val packetWindowIndex = (startTimeMs / targetBuffer.windowMs).toInt().coerceAtLeast(0)
        if (currentWindowIndex < 0 || abs(packetWindowIndex - currentWindowIndex) > 3) {
            currentWindowIndex = packetWindowIndex
            accTotalSq = 0.0
            accBassSq = 0.0
            accLowMidSq = 0.0
            accHighMidSq = 0.0
            accTrebleSq = 0.0
            accCount = 0
            exactSampleRemainder = 0.0
        }

        if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
            val floatBuf = byteBuffer.asFloatBuffer()
            val totalFloats = (sizeBytes / 4).coerceAtMost(floatBuf.remaining())
            val frameCount = totalFloats / channelCount
            for (f in 0 until frameCount) {
                var sum = 0f
                for (c in 0 until channelCount) {
                    sum += floatBuf.get()
                }
                pushMonoSample((sum / channelCount).coerceIn(-1f, 1f), targetBuffer)
            }
        } else {
            val shortBuf = byteBuffer.asShortBuffer()
            val totalShorts = (sizeBytes / 2).coerceAtMost(shortBuf.remaining())
            val frameCount = totalShorts / channelCount
            for (f in 0 until frameCount) {
                var sum = 0f
                for (c in 0 until channelCount) {
                    sum += shortBuf.get().toFloat() / 32768f
                }
                pushMonoSample((sum / channelCount).coerceIn(-1f, 1f), targetBuffer)
            }
        }
    }

    fun pushMonoSample(mono: Float, targetBuffer: MutableSpectrumBuffer) {
        if (currentWindowIndex < 0) currentWindowIndex = 0

        lpBass1 += alphaBass * (mono - lpBass1)
        lpBass2 += alphaBass * (lpBass1 - lpBass2)

        lpLowMid1 += alphaLowMid * (mono - lpLowMid1)
        lpLowMid2 += alphaLowMid * (lpLowMid1 - lpLowMid2)

        lpHighMid1 += alphaHighMid * (mono - lpHighMid1)
        lpHighMid2 += alphaHighMid * (lpHighMid1 - lpHighMid2)

        val bassSample = lpBass2
        val lowMidSample = lpLowMid2 - lpBass2
        val highMidSample = lpHighMid2 - lpLowMid2
        val trebleSample = mono - lpHighMid2

        accTotalSq += (mono * mono).toDouble()
        accBassSq += (bassSample * bassSample).toDouble()
        accLowMidSq += (lowMidSample * lowMidSample).toDouble()
        accHighMidSq += (highMidSample * highMidSample).toDouble()
        accTrebleSq += (trebleSample * trebleSample).toDouble()
        accCount++
        exactSampleRemainder += 1.0

        if (exactSampleRemainder >= exactSamplesPerWindow) {
            exactSampleRemainder -= exactSamplesPerWindow
            flushWindow(targetBuffer)
        }
    }

    fun flushRemaining(targetBuffer: MutableSpectrumBuffer) {
        if (accCount > 0) {
            flushWindow(targetBuffer)
        }
    }

    private fun flushWindow(targetBuffer: MutableSpectrumBuffer) {
        if (currentWindowIndex < 0) currentWindowIndex = 0
        val inv = 1.0 / accCount.coerceAtLeast(1)
        targetBuffer.recordWindow(
            windowIndex = currentWindowIndex,
            totalRms = sqrt(accTotalSq * inv).toFloat(),
            bassRms = sqrt(accBassSq * inv).toFloat(),
            lowMidRms = sqrt(accLowMidSq * inv).toFloat(),
            highMidRms = sqrt(accHighMidSq * inv).toFloat(),
            trebleRms = sqrt(accTrebleSq * inv).toFloat()
        )
        currentWindowIndex++
        accTotalSq = 0.0
        accBassSq = 0.0
        accLowMidSq = 0.0
        accHighMidSq = 0.0
        accTrebleSq = 0.0
        accCount = 0
    }
}

/**
 * Hàm phân tích thuần Kotlin cho phép kiểm thử đơn vị DSP với tín hiệu PCM thực.
 */
internal fun analyzePcmSamplesToProfile(
    songId: Long,
    samples: FloatArray,
    sampleRateHz: Int,
    windowMs: Long = SongAudioAnalyzer.WINDOW_MS
): SongSpectrumProfile {
    val durationMs = ((samples.size.toLong() * 1_000L) / sampleRateHz.coerceAtLeast(1)).coerceAtLeast(windowMs)
    val capacity = ((durationMs / windowMs).toInt() + 2).coerceAtLeast(2)
    val buffer = MutableSpectrumBuffer(songId, windowMs, capacity)
    val processor = StreamingFourBandProcessor(sampleRateHz)
    for (s in samples) {
        processor.pushMonoSample(s, buffer)
    }
    processor.flushRemaining(buffer)
    return buffer.toImmutableSnapshot()
}
