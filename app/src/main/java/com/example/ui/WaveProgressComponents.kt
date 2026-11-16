package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.R
import kotlin.math.sin
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val DIGIT_STRINGS = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")

@Stable
internal class CardCoordinatesHolder {
    var coordinates: LayoutCoordinates? = null
}

@Stable
internal class WaveElementBounds {
    var elementCoords: LayoutCoordinates? = null
    var offsetX: Float = 0f
    var offsetY: Float = 0f
    var cardWidthPx: Float = 0f
    var cardHeightPx: Float = 0f
    var positionResolved: Boolean = false
}

/**
 * Trạng thái sóng nước hợp nhất cho thẻ trình phát:
 * - Gộp toàn bộ animation sóng + tiến trình phát thành 1 hệ thống đồng bộ.
 * - Đọc State thuần túy trong Draw Phase (drawBehind / drawWithContent),
 *   triệt tiêu 100% Recomposition khi phát nhạc.
 */
@Stable
internal class WaveSyncState(
    val fractionState: State<Float>,
    val phaseState: State<Float>,
    val amplitudeDpState: State<Float>,
    val coordsHolder: CardCoordinatesHolder
)

@Composable
internal fun rememberWaveSyncState(
    songId: Long,
    currentPositionMsFlow: StateFlow<Long>,
    durationMs: Long,
    isDraggingState: State<Boolean>,
    dragFractionState: State<Float>,
    isPlaying: Boolean
): WaveSyncState {
    val coordsHolder = remember(songId) { CardCoordinatesHolder() }
    val fractionAnimatable = remember(songId) { Animatable(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(songId, currentPositionMsFlow, durationMs, isPlaying, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var prevTargetFraction = 0f
            var isRewindingSameSong = false

            combine(
                currentPositionMsFlow,
                snapshotFlow { isDraggingState.value to dragFractionState.value }
            ) { posMs, (dragging, dragFrac) ->
                Triple(posMs, dragging, dragFrac)
            }.collectLatest { (posMs, dragging, dragFrac) ->
                val validDuration = durationMs.coerceAtLeast(1L)
                val targetFraction = if (dragging) {
                    dragFrac.coerceIn(0f, 1f)
                } else {
                    (posMs.toFloat() / validDuration.toFloat()).coerceIn(0f, 1f)
                }

                if (!dragging) {
                    if (prevTargetFraction > 0.5f && targetFraction <= 0.05f) {
                        isRewindingSameSong = true
                    } else if (fractionAnimatable.value <= 0.01f) {
                        isRewindingSameSong = false
                    }
                } else {
                    isRewindingSameSong = false
                }
                prevTargetFraction = targetFraction

                when {
                    dragging -> fractionAnimatable.snapTo(targetFraction)
                    isRewindingSameSong -> {
                        fractionAnimatable.animateTo(
                            targetValue = targetFraction,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                        )
                        if (fractionAnimatable.value <= 0.01f) {
                            isRewindingSameSong = false
                        }
                    }
                    !isPlaying || targetFraction < 0.01f -> fractionAnimatable.snapTo(targetFraction)
                    else -> fractionAnimatable.animateTo(
                        targetValue = targetFraction,
                        animationSpec = tween(durationMillis = 400, easing = LinearEasing)
                    )
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "wave_sync_transition")

    val phaseState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isPlaying) 3200 else 9000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_sync_phase"
    )

    val amplitudeDpState = infiniteTransition.animateFloat(
        initialValue = if (isPlaying) 2.0f else 0.8f,
        targetValue = if (isPlaying) 3.2f else 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isPlaying) 2400 else 4200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_sync_amplitude"
    )

    return remember(songId, fractionAnimatable, phaseState, amplitudeDpState, coordsHolder) {
        WaveSyncState(
            fractionState = fractionAnimatable.asState(),
            phaseState = phaseState,
            amplitudeDpState = amplitudeDpState,
            coordsHolder = coordsHolder
        )
    }
}

@Composable
internal fun CardProgressBackground(
    waveSyncState: WaveSyncState,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val trackColor = primaryContainer

    val (frontWaveColor, backWaveColor) = remember(primaryColor, primaryContainer) {
        Pair(primaryColor, primaryColor.copy(alpha = 0.45f))
    }

    val backPath = remember { Path() }
    val mainPath = remember { Path() }

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(color = trackColor)
                val fraction = waveSyncState.fractionState.value
                if (fraction > 0f) {
                    val widthPx = size.width
                    val heightPx = size.height
                    val progressPx = widthPx * fraction
                    val phase = waveSyncState.phaseState.value
                    val baseAmpPx = waveSyncState.amplitudeDpState.value.dp.toPx()

                    val edgeFactor = (progressPx / (baseAmpPx * 2.5f)).coerceIn(0f, 1f) *
                        ((widthPx - progressPx) / (baseAmpPx * 2.5f)).coerceIn(0f, 1f)
                    val ampPx = baseAmpPx * edgeFactor

                    val cycles = 1.25f
                    val frequency = (6.2831855f * cycles) / heightPx.coerceAtLeast(1f)
                    val steps = 24

                    if (ampPx > 0.2f) {
                        backPath.reset()
                        backPath.moveTo(0f, 0f)
                        val backPhase = phase + 2.0f
                        val backAmp = ampPx * 1.15f
                        val backProgressPx = (progressPx + 0.8.dp.toPx()).coerceAtLeast(0f)

                        for (i in 0..steps) {
                            val y = (heightPx * i) / steps
                            val waveX = backProgressPx + backAmp * sin(frequency * y + backPhase)
                            val clampedX = waveX.coerceIn(0f, widthPx)
                            backPath.lineTo(clampedX, y)
                        }
                        backPath.lineTo(0f, heightPx)
                        backPath.close()
                        drawPath(path = backPath, color = backWaveColor)
                    }

                    mainPath.reset()
                    mainPath.moveTo(0f, 0f)
                    for (i in 0..steps) {
                        val y = (heightPx * i) / steps
                        val waveX = progressPx + ampPx * sin(frequency * y + phase)
                        val clampedX = waveX.coerceIn(0f, widthPx)
                        mainPath.lineTo(clampedX, y)
                    }
                    mainPath.lineTo(0f, heightPx)
                    mainPath.close()
                    drawPath(path = mainPath, color = frontWaveColor)
                }
            }
    )
}

internal fun Modifier.waveProgressMask(
    waveSyncState: WaveSyncState?,
    isDarkTheme: Boolean,
    coveredColor: Color? = null
): Modifier {
    if (waveSyncState == null) return this
    return this.composed {
        val effectiveCoveredColor = coveredColor ?: remember(isDarkTheme) {
            if (isDarkTheme) {
                Color(0xFF001E30)
            } else {
                Color(0xFFFFFFFF)
            }
        }
        val bounds = remember { WaveElementBounds() }
        val waveMaskPath = remember { Path() }

        Modifier
            .onGloballyPositioned { coords ->
                bounds.elementCoords = coords
                val parent = waveSyncState.coordsHolder.coordinates
                if (parent != null && parent.isAttached && coords.isAttached) {
                    try {
                        val pos = parent.localPositionOf(coords, Offset.Zero)
                        bounds.offsetX = pos.x
                        bounds.offsetY = pos.y
                        bounds.cardWidthPx = parent.size.width.toFloat()
                        bounds.cardHeightPx = parent.size.height.toFloat()
                        bounds.positionResolved = true
                    } catch (_: Throwable) {
                        bounds.positionResolved = false
                    }
                } else {
                    bounds.positionResolved = false
                }
            }
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()

                val parent = waveSyncState.coordsHolder.coordinates
                val elem = bounds.elementCoords
                if (parent != null && parent.isAttached && elem != null && elem.isAttached) {
                    val currentParentWidth = parent.size.width.toFloat()
                    if (!bounds.positionResolved || currentParentWidth != bounds.cardWidthPx) {
                        try {
                            val pos = parent.localPositionOf(elem, Offset.Zero)
                            bounds.offsetX = pos.x
                            bounds.offsetY = pos.y
                            bounds.cardWidthPx = currentParentWidth
                            bounds.cardHeightPx = parent.size.height.toFloat()
                            bounds.positionResolved = true
                        } catch (_: Throwable) {
                        }
                    }
                }

                val fraction = waveSyncState.fractionState.value
                val cardWidthPx = bounds.cardWidthPx
                if (fraction > 0f && cardWidthPx > 0f) {
                    val progressPx = cardWidthPx * fraction
                    val baseAmpPx = waveSyncState.amplitudeDpState.value.dp.toPx()
                    val edgeFactor = (progressPx / (baseAmpPx * 2.5f)).coerceIn(0f, 1f) *
                        ((cardWidthPx - progressPx) / (baseAmpPx * 2.5f)).coerceIn(0f, 1f)
                    val ampPx = baseAmpPx * edgeFactor

                    val compLeftInCard = bounds.offsetX
                    val compWidth = size.width
                    val compHeight = size.height

                    // Fast path 1: Sóng chưa chạm tới cạnh trái của phần tử -> bỏ qua tính toán sin()
                    if (progressPx + ampPx <= compLeftInCard) {
                        return@drawWithContent
                    }

                    // Fast path 2: Sóng đã vượt qua toàn bộ cạnh phải của phần tử -> tô phủ O(1)
                    if (progressPx - ampPx >= compLeftInCard + compWidth) {
                        drawRect(
                            color = effectiveCoveredColor,
                            blendMode = BlendMode.SrcAtop
                        )
                        return@drawWithContent
                    }

                    val phase = waveSyncState.phaseState.value
                    val heightForFreq = bounds.cardHeightPx.coerceAtLeast(1f)
                    val cycles = 1.25f
                    val frequency = (6.2831855f * cycles) / heightForFreq
                    val compTopInCard = bounds.offsetY

                    waveMaskPath.reset()
                    waveMaskPath.moveTo(0f, 0f)

                    val steps = 16
                    var anyCovered = false
                    for (i in 0..steps) {
                        val yInComp = (compHeight * i) / steps
                        val yInCard = compTopInCard + yInComp
                        val waveXInCard = progressPx + ampPx * sin(frequency * yInCard + phase)
                        val waveXInComp = waveXInCard - compLeftInCard
                        if (waveXInComp > 0f) {
                            anyCovered = true
                        }
                        val clampedX = waveXInComp.coerceIn(0f, compWidth)
                        waveMaskPath.lineTo(clampedX, yInComp)
                    }
                    waveMaskPath.lineTo(0f, compHeight)
                    waveMaskPath.close()

                    if (anyCovered) {
                        drawPath(
                            path = waveMaskPath,
                            color = effectiveCoveredColor,
                            blendMode = BlendMode.SrcAtop
                        )
                    }
                }
            }
    }
}

@Composable
internal fun AudioWaveformEqualizer(
    isPlaying: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    if (!isPlaying) {
        Canvas(modifier = modifier) {
            val barCount = 5
            val barWidth = 2.4.dp.toPx()
            val totalWidth = size.width
            val totalHeight = size.height
            val totalBarsWidth = barWidth * barCount
            val spacing = ((totalWidth - totalBarsWidth) / (barCount - 1)).coerceAtLeast(1.5.dp.toPx())
            val actualTotalWidth = totalBarsWidth + spacing * (barCount - 1)
            val startX = (totalWidth - actualTotalWidth) / 2f
            val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

            for (i in 0 until barCount) {
                val fraction = when (i) {
                    0 -> 0.35f
                    1 -> 0.65f
                    2 -> 1.00f
                    3 -> 0.75f
                    else -> 0.35f
                }
                val barH = (totalHeight * fraction).coerceAtLeast(barWidth)
                val left = startX + i * (barWidth + spacing)
                val top = (totalHeight - barH) / 2f

                drawRoundRect(
                    color = tint,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barH),
                    cornerRadius = cornerRadius
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "waveform_anim")
    val anim1 = transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(460, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val anim2 = transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(580, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val anim3 = transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val anim4 = transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )
    val anim5 = transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.68f,
        animationSpec = infiniteRepeatable(
            animation = tween(430, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar5"
    )

    Canvas(modifier = modifier) {
        val barCount = 5
        val barWidth = 2.4.dp.toPx()
        val totalWidth = size.width
        val totalHeight = size.height
        val totalBarsWidth = barWidth * barCount
        val spacing = ((totalWidth - totalBarsWidth) / (barCount - 1)).coerceAtLeast(1.5.dp.toPx())
        val actualTotalWidth = totalBarsWidth + spacing * (barCount - 1)
        val startX = (totalWidth - actualTotalWidth) / 2f
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

        for (i in 0 until barCount) {
            val fraction = when (i) {
                0 -> anim1.value
                1 -> anim2.value
                2 -> anim3.value
                3 -> anim4.value
                else -> anim5.value
            }
            val barH = (totalHeight * fraction).coerceAtLeast(barWidth)
            val left = startX + i * (barWidth + spacing)
            val top = (totalHeight - barH) / 2f

            drawRoundRect(
                color = tint,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = cornerRadius
            )
        }
    }
}

@Composable
internal fun SongCardTimeDisplay(
    isActive: Boolean,
    currentPositionMsFlow: StateFlow<Long>?,
    durationMs: Long,
    isDraggingState: State<Boolean>,
    dragFractionState: State<Float>,
    contentColor: Color,
    isDarkTheme: Boolean,
    waveSyncState: WaveSyncState?,
    modifier: Modifier = Modifier
) {
    val baseLabelMedium = MaterialTheme.typography.labelMedium
    val timeTextStyle = remember(baseLabelMedium) {
        baseLabelMedium.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 11.5.sp,
            fontFeatureSettings = "tnum"
        )
    }
    val pipeTextStyle = remember(timeTextStyle) {
        timeTextStyle.copy(fontWeight = FontWeight.Normal)
    }

    val digitSlotWidth = 7.dp
    val colonSlotWidth = 3.5.dp
    val pipeSlotWidth = 11.dp

    if (!isActive || currentPositionMsFlow == null) {
        val durationText = remember(durationMs) { formatCardDuration(durationMs) }
        val digitColor = remember(contentColor) { contentColor.copy(alpha = 0.60f) }
        val colonColor = remember(contentColor) { contentColor.copy(alpha = 0.45f) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = modifier
                .height(36.dp)
                .testTag("song_time_display_inactive")
        ) {
            for (char in durationText) {
                when {
                    char in '0'..'9' -> {
                        Box(
                            modifier = Modifier.width(digitSlotWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = DIGIT_STRINGS[char - '0'],
                                style = timeTextStyle,
                                color = digitColor,
                                maxLines = 1
                            )
                        }
                    }
                    char == ':' -> {
                        Box(
                            modifier = Modifier.width(colonSlotWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ":",
                                style = timeTextStyle,
                                color = colonColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    } else {
        val positionSecFlow = remember(currentPositionMsFlow) {
            currentPositionMsFlow
                .map { it / 1000L }
                .distinctUntilChanged()
        }
        val currentPositionSec by positionSecFlow.collectAsStateWithLifecycle(
            initialValue = currentPositionMsFlow.value / 1000L
        )
        val validDuration = durationMs.coerceAtLeast(0L)
        val totalSec = validDuration / 1000L
        val isDragging = isDraggingState.value
        val displaySec = if (isDragging && validDuration > 0L) {
            ((dragFractionState.value * validDuration).toLong().coerceIn(0L, validDuration)) / 1000L
        } else {
            currentPositionSec.coerceIn(0L, totalSec.coerceAtLeast(1L))
        }

        val (curTimeText, totalTimeText) = remember(displaySec, totalSec) {
            formatTimePair(displaySec, totalSec)
        }

        val curDigitColor = remember(contentColor) { contentColor.copy(alpha = 0.95f) }
        val curColonColor = remember(contentColor) { contentColor.copy(alpha = 0.70f) }
        val pipeColor = remember(contentColor) { contentColor.copy(alpha = 0.40f) }
        val totDigitColor = remember(contentColor) { contentColor.copy(alpha = 0.60f) }
        val totColonColor = remember(contentColor) { contentColor.copy(alpha = 0.45f) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = modifier
                .height(36.dp)
                .waveProgressMask(
                    waveSyncState = waveSyncState,
                    isDarkTheme = isDarkTheme
                )
                .testTag("active_song_time_display")
        ) {
            for (char in curTimeText) {
                when {
                    char in '0'..'9' -> {
                        Box(
                            modifier = Modifier.width(digitSlotWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = DIGIT_STRINGS[char - '0'],
                                style = timeTextStyle,
                                color = curDigitColor,
                                maxLines = 1
                            )
                        }
                    }
                    char == ':' -> {
                        Box(
                            modifier = Modifier.width(colonSlotWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ":",
                                style = timeTextStyle,
                                color = curColonColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.width(pipeSlotWidth),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "|",
                    style = pipeTextStyle,
                    color = pipeColor,
                    maxLines = 1
                )
            }

            for (char in totalTimeText) {
                when {
                    char in '0'..'9' -> {
                        Box(
                            modifier = Modifier.width(digitSlotWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = DIGIT_STRINGS[char - '0'],
                                style = timeTextStyle,
                                color = totDigitColor,
                                maxLines = 1
                            )
                        }
                    }
                    char == ':' -> {
                        Box(
                            modifier = Modifier.width(colonSlotWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ":",
                                style = timeTextStyle,
                                color = totColonColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Icon mở Trang chơi nhạc toàn màn hình, thay thế vị trí tổng thời gian trên thẻ bài hát đang chạy.
 * Chiều cao 36.dp khớp hoàn toàn với SongCardTimeDisplay để giữ nguyên kích thước thẻ đồng nhất.
 */
@Composable
internal fun SelectedSongOpenPlayerButton(
    onClick: () -> Unit,
    tint: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val debouncedClick = rememberDebouncedAction(debounceMs = 200L, onClick = onClick)

    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = modifier
            .width(42.dp)
            .height(36.dp)
            .bottomControlBounce(
                scaleDown = 0.84f,
                dipY = 1.dp,
                interactionSource = interactionSource
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = debouncedClick
            )
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            contentColor = tint,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = null,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.OpenInFull,
                    contentDescription = stringResource(R.string.cd_open_player_screen),
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

internal fun formatTimePair(currentSec: Long, totalSec: Long): Pair<String, String> {
    val safeTotalSec = totalSec.coerceAtLeast(0)
    val safeCurSec = currentSec.coerceIn(0, safeTotalSec.coerceAtLeast(1))

    val totalHours = safeTotalSec / 3600
    val totalMinutes = (safeTotalSec % 3600) / 60
    val totalSeconds = safeTotalSec % 60

    val curHours = safeCurSec / 3600
    val curMinutes = (safeCurSec % 3600) / 60
    val curSeconds = safeCurSec % 60

    val hasHours = totalHours > 0

    val totalStr = buildString(8) {
        if (hasHours) {
            if (totalHours < 10) append('0')
            append(totalHours)
            append(':')
        }
        if (totalMinutes < 10) append('0')
        append(totalMinutes)
        append(':')
        if (totalSeconds < 10) append('0')
        append(totalSeconds)
    }

    val curStr = buildString(8) {
        if (hasHours) {
            if (curHours < 10) append('0')
            append(curHours)
            append(':')
        }
        if (curMinutes < 10) append('0')
        append(curMinutes)
        append(':')
        if (curSeconds < 10) append('0')
        append(curSeconds)
    }

    return curStr to totalStr
}

internal fun formatCardDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return buildString(8) {
        if (hours > 0) {
            if (hours < 10) append('0')
            append(hours)
            append(':')
        }
        if (minutes < 10) append('0')
        append(minutes)
        append(':')
        if (seconds < 10) append('0')
        append(seconds)
    }
}
