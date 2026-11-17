package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.PlayMode
import com.example.model.Song
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Thẻ viên nhộng trình phát (Now-Playing Pill Capsule) đặt ở cạnh dưới ngay trên thanh điều hướng:
 * - Nút Lặp lại (`btn_play_mode`) được đặt tách rời bên ngoài ở cạnh phải, đồng bộ kích thước `52.dp` và bám sát theo thanh khi thu gọn/mở rộng.
 * - Hỗ trợ 2 trạng thái Mở rộng <-> Thu gọn với chuyển động lò xo đàn hồi mượt mà:
 *   + Mở rộng: [Play/Pause viên nhộng] [Tên bài hát + Thời gian • Danh sách] [Nút Thu gọn <]  +  (Nút Lặp lại)
 *   + Thu gọn: [Play/Pause hình tròn] [Nút Mở rộng >]  +  (Nút Lặp lại bám theo bên phải)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BottomNowPlayingPill(
    song: Song,
    activePlaylistName: String? = null,
    isPlaying: Boolean,
    playMode: PlayMode,
    durationMs: Long,
    currentPositionMsFlow: StateFlow<Long>,
    onTogglePlayPause: () -> Unit,
    onCyclePlayMode: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenQueue: () -> Unit = {},
    isDarkTheme: Boolean,
    isCollapsed: Boolean? = null,
    onToggleCollapse: (() -> Unit)? = null,
    expandedWidthOverrideDp: androidx.compose.ui.unit.Dp? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val cardWidthPxHolder = remember { floatArrayOf(1f) }
    val isDraggingProgressState = remember { mutableStateOf(false) }
    val dragFractionState = remember { mutableFloatStateOf(0f) }

    var localCollapsed by rememberSaveable { mutableStateOf(false) }
    val effectiveCollapsed = isCollapsed ?: localCollapsed
    LaunchedEffect(effectiveCollapsed) {
        if (effectiveCollapsed) {
            isDraggingProgressState.value = false
        }
    }

    // Chuyển động lò xo có độ đàn hồi (elastic spring) khi thu gọn và mở rộng thanh tiến trình
    val collapseFraction by animateFloatAsState(
        targetValue = if (effectiveCollapsed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.54f,
            stiffness = Spring.StiffnessMediumLow
        ),
        visibilityThreshold = 0.0005f,
        label = "pill_collapse_spring"
    )

    // Hiệu ứng nảy đàn hồi theo phương ngang/dọc (squash & stretch) khi bấm thu gọn hoặc mở rộng
    val collapseScaleX = remember { Animatable(1f) }
    val collapseScaleY = remember { Animatable(1f) }
    val modeFollowScale = remember { Animatable(1f) }
    var previousCollapsed by remember { mutableStateOf(effectiveCollapsed) }
    LaunchedEffect(effectiveCollapsed) {
        if (previousCollapsed != effectiveCollapsed) {
            previousCollapsed = effectiveCollapsed
            launch {
                collapseScaleX.snapTo(if (effectiveCollapsed) 0.91f else 1.05f)
                collapseScaleX.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.44f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                collapseScaleY.snapTo(if (effectiveCollapsed) 1.06f else 0.94f)
                collapseScaleY.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.44f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                modeFollowScale.snapTo(if (effectiveCollapsed) 0.88f else 0.92f)
                modeFollowScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.42f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    val effectiveDuration = if (durationMs > 0L) durationMs else song.duration
    val currentEffectiveDuration by rememberUpdatedState(effectiveDuration)
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentOnOpenQueue by rememberUpdatedState(onOpenQueue)

    val waveSyncState = rememberWaveSyncState(
        songId = song.id,
        currentPositionMsFlow = currentPositionMsFlow,
        durationMs = effectiveDuration,
        isDraggingState = isDraggingProgressState,
        dragFractionState = dragFractionState,
        isPlaying = isPlaying
    )

    val bounceScale = remember { Animatable(1f) }
    var lastSongId by remember { mutableStateOf(song.id) }
    LaunchedEffect(song.id) {
        if (lastSongId != song.id) {
            lastSongId = song.id
            launch {
                bounceScale.snapTo(0.94f)
                bounceScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.48f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    val shadowColor = if (isDarkTheme) {
        Color.Black.copy(alpha = 0.44f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    }
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val queueNameText = activePlaylistName?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.now_playing_library_queue)

    // Màu tương phản cao đồng nhất cho cả nút Play/Pause và Lặp lại (không đổi màu nền, không viền)
    val controlContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val playPauseContentColor = MaterialTheme.colorScheme.primary
    val targetModeContent = when (playMode) {
        PlayMode.REPEAT_ALL -> MaterialTheme.colorScheme.onSurface
        PlayMode.REPEAT_ONE -> MaterialTheme.colorScheme.primary
        PlayMode.SHUFFLE -> MaterialTheme.colorScheme.tertiary
    }
    val modeContentColor by animateColorAsState(
        targetValue = targetModeContent,
        animationSpec = tween(durationMillis = 200),
        label = "play_mode_fg"
    )

    val repeatCircleSize = 52.dp
    val repeatCircleGap = 10.dp
    val repeatContainerColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    val bounceScaleProvider = remember(bounceScale) { { bounceScale.value } }
    val collapseScaleXProvider = remember(collapseScaleX) { { collapseScaleX.value } }
    val collapseScaleYProvider = remember(collapseScaleY) { { collapseScaleY.value } }
    val modeFollowScaleProvider = remember(modeFollowScale) { { modeFollowScale.value } }

    if (expandedWidthOverrideDp != null) {
        val totalAvailableWidth = expandedWidthOverrideDp.coerceAtMost(352.dp)
        val expandedPillWidthDp = (totalAvailableWidth - repeatCircleSize - repeatCircleGap).coerceAtLeast(82.dp)
        val compactPillWidthDp = 82.dp.coerceAtMost(expandedPillWidthDp)
        val minBounceWidthDp = (compactPillWidthDp - 6.dp).coerceAtLeast(74.dp)
        val maxBounceWidthDp = expandedPillWidthDp + 12.dp
        val pillWidthDp = lerp(expandedPillWidthDp, compactPillWidthDp, collapseFraction)
            .coerceIn(minBounceWidthDp, maxBounceWidthDp)
        val playPauseWidthDp = lerp(56.dp, 44.dp, collapseFraction)
            .coerceIn(42.dp, 59.dp)

        BottomNowPlayingPillWithExternalRepeat(
            song = song,
            isPlaying = isPlaying,
            playMode = playMode,
            effectiveCollapsed = effectiveCollapsed,
            collapseFraction = collapseFraction,
            pillWidthDp = pillWidthDp,
            playPauseWidthDp = playPauseWidthDp,
            bounceScaleProvider = bounceScaleProvider,
            collapseScaleXProvider = collapseScaleXProvider,
            collapseScaleYProvider = collapseScaleYProvider,
            modeFollowScaleProvider = modeFollowScaleProvider,
            shadowColor = shadowColor,
            contentColor = contentColor,
            onPrimaryColor = onPrimaryColor,
            controlContainerColor = controlContainerColor,
            repeatContainerColor = repeatContainerColor,
            repeatCircleSize = repeatCircleSize,
            repeatCircleGap = repeatCircleGap,
            playPauseContentColor = playPauseContentColor,
            modeContentColor = modeContentColor,
            queueNameText = queueNameText,
            currentPositionMsFlow = currentPositionMsFlow,
            isDarkTheme = isDarkTheme,
            waveSyncState = waveSyncState,
            cardWidthPxHolder = cardWidthPxHolder,
            currentEffectiveDuration = currentEffectiveDuration,
            isDraggingProgressState = isDraggingProgressState,
            dragFractionState = dragFractionState,
            onSeek = currentOnSeek,
            onOpenQueue = currentOnOpenQueue,
            onTogglePlayPause = onTogglePlayPause,
            onCyclePlayMode = onCyclePlayMode,
            onToggleCollapseInternal = {
                if (onToggleCollapse != null) {
                    onToggleCollapse()
                } else {
                    localCollapsed = !localCollapsed
                }
            },
            modifier = modifier
        )
    } else {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        ) {
            val totalAvailableWidth = maxWidth.coerceAtMost(352.dp)
            val expandedPillWidthDp = (totalAvailableWidth - repeatCircleSize - repeatCircleGap).coerceAtLeast(82.dp)
            val compactPillWidthDp = 82.dp.coerceAtMost(expandedPillWidthDp)
            val minBounceWidthDp = (compactPillWidthDp - 6.dp).coerceAtLeast(74.dp)
            val maxBounceWidthDp = (expandedPillWidthDp + 12.dp)
                .coerceAtMost((maxWidth - repeatCircleSize - repeatCircleGap + 10.dp).coerceAtLeast(expandedPillWidthDp))
            val pillWidthDp = lerp(expandedPillWidthDp, compactPillWidthDp, collapseFraction)
                .coerceIn(minBounceWidthDp, maxBounceWidthDp)
            val playPauseWidthDp = lerp(56.dp, 44.dp, collapseFraction)
                .coerceIn(42.dp, 59.dp)

            BottomNowPlayingPillWithExternalRepeat(
                song = song,
                isPlaying = isPlaying,
                playMode = playMode,
                effectiveCollapsed = effectiveCollapsed,
                collapseFraction = collapseFraction,
                pillWidthDp = pillWidthDp,
                playPauseWidthDp = playPauseWidthDp,
                bounceScaleProvider = bounceScaleProvider,
                collapseScaleXProvider = collapseScaleXProvider,
                collapseScaleYProvider = collapseScaleYProvider,
                modeFollowScaleProvider = modeFollowScaleProvider,
                shadowColor = shadowColor,
                contentColor = contentColor,
                onPrimaryColor = onPrimaryColor,
                controlContainerColor = controlContainerColor,
                repeatContainerColor = repeatContainerColor,
                repeatCircleSize = repeatCircleSize,
                repeatCircleGap = repeatCircleGap,
                playPauseContentColor = playPauseContentColor,
                modeContentColor = modeContentColor,
                queueNameText = queueNameText,
                currentPositionMsFlow = currentPositionMsFlow,
                isDarkTheme = isDarkTheme,
                waveSyncState = waveSyncState,
                cardWidthPxHolder = cardWidthPxHolder,
                currentEffectiveDuration = currentEffectiveDuration,
                isDraggingProgressState = isDraggingProgressState,
                dragFractionState = dragFractionState,
                onSeek = currentOnSeek,
                onOpenQueue = currentOnOpenQueue,
                onTogglePlayPause = onTogglePlayPause,
                onCyclePlayMode = onCyclePlayMode,
                onToggleCollapseInternal = {
                    if (onToggleCollapse != null) {
                        onToggleCollapse()
                    } else {
                        localCollapsed = !localCollapsed
                    }
                }
            )
        }
    }
}

@Composable
private fun BottomNowPlayingPillWithExternalRepeat(
    song: Song,
    isPlaying: Boolean,
    playMode: PlayMode,
    effectiveCollapsed: Boolean,
    collapseFraction: Float,
    pillWidthDp: androidx.compose.ui.unit.Dp,
    playPauseWidthDp: androidx.compose.ui.unit.Dp,
    bounceScaleProvider: () -> Float,
    collapseScaleXProvider: () -> Float,
    collapseScaleYProvider: () -> Float,
    modeFollowScaleProvider: () -> Float,
    shadowColor: Color,
    contentColor: Color,
    onPrimaryColor: Color,
    controlContainerColor: Color,
    repeatContainerColor: Color,
    repeatCircleSize: androidx.compose.ui.unit.Dp,
    repeatCircleGap: androidx.compose.ui.unit.Dp,
    playPauseContentColor: Color,
    modeContentColor: Color,
    queueNameText: String,
    currentPositionMsFlow: StateFlow<Long>,
    isDarkTheme: Boolean,
    waveSyncState: WaveSyncState,
    cardWidthPxHolder: FloatArray,
    currentEffectiveDuration: Long,
    isDraggingProgressState: androidx.compose.runtime.MutableState<Boolean>,
    dragFractionState: androidx.compose.runtime.MutableFloatState,
    onSeek: (Long) -> Unit,
    onOpenQueue: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onCyclePlayMode: () -> Unit,
    onToggleCollapseInternal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        BottomNowPlayingPillSurface(
            song = song,
            isPlaying = isPlaying,
            effectiveCollapsed = effectiveCollapsed,
            collapseFraction = collapseFraction,
            pillWidthDp = pillWidthDp,
            playPauseWidthDp = playPauseWidthDp,
            bounceScaleProvider = bounceScaleProvider,
            collapseScaleXProvider = collapseScaleXProvider,
            collapseScaleYProvider = collapseScaleYProvider,
            shadowColor = shadowColor,
            contentColor = contentColor,
            onPrimaryColor = onPrimaryColor,
            controlContainerColor = controlContainerColor,
            playPauseContentColor = playPauseContentColor,
            queueNameText = queueNameText,
            currentPositionMsFlow = currentPositionMsFlow,
            isDarkTheme = isDarkTheme,
            waveSyncState = waveSyncState,
            cardWidthPxHolder = cardWidthPxHolder,
            currentEffectiveDuration = currentEffectiveDuration,
            isDraggingProgressState = isDraggingProgressState,
            dragFractionState = dragFractionState,
            onSeek = onSeek,
            onOpenQueue = onOpenQueue,
            onTogglePlayPause = onTogglePlayPause,
            onToggleCollapseInternal = onToggleCollapseInternal
        )

        Spacer(modifier = Modifier.width(repeatCircleGap))

        val playModeSource = remember { MutableInteractionSource() }
        val glassPalette = rememberLiquidGlassPalette(isDarkTheme = isDarkTheme)
        val isModeToggled = playMode != PlayMode.REPEAT_ALL
        val activeIconTint = if (isModeToggled) Color.White else modeContentColor
        LiquidGlassCircleButton(
            onClick = onCyclePlayMode,
            palette = glassPalette,
            size = repeatCircleSize,
            isActive = isModeToggled,
            shadowColor = shadowColor,
            interactionSource = playModeSource,
            extraScaleProvider = { bounceScaleProvider() * modeFollowScaleProvider() },
            testTag = "btn_play_mode"
        ) {
            AnimatedPlayModeIcon(
                playMode = playMode,
                tint = activeIconTint,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomNowPlayingPillSurface(
    song: Song,
    isPlaying: Boolean,
    effectiveCollapsed: Boolean,
    collapseFraction: Float,
    pillWidthDp: androidx.compose.ui.unit.Dp,
    playPauseWidthDp: androidx.compose.ui.unit.Dp,
    bounceScaleProvider: () -> Float,
    collapseScaleXProvider: () -> Float,
    collapseScaleYProvider: () -> Float,
    shadowColor: Color,
    contentColor: Color,
    onPrimaryColor: Color,
    controlContainerColor: Color,
    playPauseContentColor: Color,
    queueNameText: String,
    currentPositionMsFlow: StateFlow<Long>,
    isDarkTheme: Boolean,
    waveSyncState: WaveSyncState,
    cardWidthPxHolder: FloatArray,
    currentEffectiveDuration: Long,
    isDraggingProgressState: androidx.compose.runtime.MutableState<Boolean>,
    dragFractionState: androidx.compose.runtime.MutableFloatState,
    onSeek: (Long) -> Unit,
    onOpenQueue: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleCollapseInternal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = contentColor,
        tonalElevation = 3.dp,
        shadowElevation = 0.dp,
        border = null,
        modifier = modifier
            .width(pillWidthDp)
            .height(52.dp)
                .graphicsLayer {
                    val bounce = bounceScaleProvider()
                    scaleX = bounce * collapseScaleXProvider()
                    scaleY = bounce * collapseScaleYProvider()
                }
                .radiantShadow(
                    color = shadowColor,
                    borderRadius = 26.dp,
                    blurRadius = 14.dp,
                    offsetY = 3.dp
                )
                .clip(CircleShape)
                .testTag("bottom_now_playing_pill")
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { waveSyncState.coordsHolder.coordinates = it }
                    .onSizeChanged { cardWidthPxHolder[0] = it.width.toFloat().coerceAtLeast(1f) }
                    .pointerInput(song.id, effectiveCollapsed) {
                        if (!effectiveCollapsed) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val dur = currentEffectiveDuration
                                    if (cardWidthPxHolder[0] > 0f && dur > 0L) {
                                        val fraction = (offset.x / cardWidthPxHolder[0]).coerceIn(0f, 1f)
                                        onSeek((fraction * dur).toLong())
                                    }
                                },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onOpenQueue()
                                }
                            )
                        }
                    }
                    .then(
                        if (!effectiveCollapsed) {
                            Modifier.pointerInput(song.id) {
                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        val widthPx = cardWidthPxHolder[0].coerceAtLeast(1f)
                                        isDraggingProgressState.value = true
                                        dragFractionState.floatValue = (offset.x / widthPx).coerceIn(0f, 1f)
                                    },
                                    onDragEnd = {
                                        if (isDraggingProgressState.value) {
                                            val dur = currentEffectiveDuration
                                            if (dur > 0L) {
                                                onSeek((dragFractionState.floatValue * dur).toLong())
                                            }
                                            isDraggingProgressState.value = false
                                        }
                                    },
                                    onDragCancel = {
                                        isDraggingProgressState.value = false
                                    },
                                    onHorizontalDrag = { change, _ ->
                                        if (isDraggingProgressState.value) {
                                            change.consume()
                                            val widthPx = cardWidthPxHolder[0].coerceAtLeast(1f)
                                            dragFractionState.floatValue = (change.position.x / widthPx).coerceIn(0f, 1f)
                                        }
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                CardProgressBackground(
                    waveSyncState = waveSyncState,
                    modifier = Modifier.matchParentSize()
                )

                Layout(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    content = {
                        // 1. Trái: Nút Phát / Tạm dừng biến đổi mượt mà giữa viên nhộng (56.dp) và hình tròn (44.dp)
                        val playPauseSource = remember { MutableInteractionSource() }
                        Surface(
                            shape = CircleShape,
                            color = controlContainerColor,
                            contentColor = playPauseContentColor,
                            tonalElevation = 0.dp,
                            shadowElevation = 2.dp,
                            border = null,
                            modifier = Modifier
                                .layoutId("play_pause")
                                .width(playPauseWidthDp)
                                .height(44.dp)
                                .bottomControlBounce(
                                    scaleDown = 0.88f,
                                    dipY = 1.5.dp,
                                    interactionSource = playPauseSource
                                )
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = playPauseSource,
                                    indication = null,
                                    onClick = onTogglePlayPause
                                )
                                .testTag("btn_play_pause")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                AnimatedContent(
                                    targetState = isPlaying,
                                    transitionSpec = {
                                        (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn())
                                            .togetherWith(scaleOut(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut())
                                    },
                                    label = "pill_play_pause_anim"
                                ) { playing ->
                                    Icon(
                                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.cd_play_pause),
                                        tint = playPauseContentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // 2. Giữa: Tên bài hát + Thời gian chạy - Tổng thời lượng • Tên danh sách
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .layoutId("song_info")
                                .clipToBounds()
                                .testTag("bottom_now_playing_info")
                        ) {
                            Text(
                                text = song.displayTitle,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    lineHeight = 16.sp
                                ),
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .waveProgressMask(
                                        waveSyncState = waveSyncState,
                                        isDarkTheme = isDarkTheme,
                                        coveredColor = onPrimaryColor
                                    )
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        repeatDelayMillis = 1500,
                                        initialDelayMillis = 1200,
                                        velocity = 32.dp
                                    )
                                    .testTag("bottom_now_playing_title")
                            )

                            BottomPillTimeAndQueueText(
                                queueNameText = queueNameText,
                                currentPositionMsFlow = currentPositionMsFlow,
                                durationMs = currentEffectiveDuration,
                                isDraggingState = isDraggingProgressState,
                                dragFractionState = dragFractionState,
                                contentColor = contentColor,
                                coveredColor = onPrimaryColor,
                                isDarkTheme = isDarkTheme,
                                waveSyncState = waveSyncState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bottom_now_playing_queue")
                            )
                        }

                        // 3. Nút Thu gọn / Mở rộng không nền, chỉ hiển thị icon chevron xoay mượt theo spring
                        val collapseExpandSource = remember { MutableInteractionSource() }
                        val chevronCd = if (effectiveCollapsed) {
                            stringResource(R.string.cd_expand_player)
                        } else {
                            stringResource(R.string.cd_collapse_player)
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .layoutId("collapse_expand")
                                .height(44.dp)
                                .bottomControlBounce(
                                    scaleDown = 0.84f,
                                    dipY = 1.dp,
                                    interactionSource = collapseExpandSource
                                )
                                .clickable(
                                    interactionSource = collapseExpandSource,
                                    indication = null,
                                    onClick = onToggleCollapseInternal
                                )
                                .testTag("btn_pill_collapse_expand")
                        ) {
                            // Đặt waveProgressMask ở Box chưa xoay để tọa độ mặt nạ sóng không bị đảo ngược khi xoay 180 độ
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(22.dp)
                                    .waveProgressMask(
                                        waveSyncState = waveSyncState,
                                        isDarkTheme = isDarkTheme,
                                        coveredColor = onPrimaryColor
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronLeft,
                                    contentDescription = chevronCd,
                                    tint = contentColor,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            rotationZ = 180f * collapseFraction
                                        }
                                )
                            }
                        }
                    }
                ) { measurables, constraints ->
                    val maxW = constraints.maxWidth.coerceAtLeast(1)
                    val maxH = constraints.maxHeight.coerceAtLeast(1)

                    val playMeasurable = measurables.first { it.layoutId == "play_pause" }
                    val infoMeasurable = measurables.first { it.layoutId == "song_info" }
                    val chevronMeasurable = measurables.first { it.layoutId == "collapse_expand" }

                    val rawT = collapseFraction.coerceIn(0f, 1f)
                    val clampedT = when {
                        rawT < 0.002f -> 0f
                        rawT > 0.998f -> 1f
                        else -> rawT
                    }
                    val playWidthPx = playPauseWidthDp.toPx().roundToInt().coerceAtLeast(1)
                    val playPlaceable = playMeasurable.measure(Constraints.fixed(playWidthPx, maxH))
                    val chevronExpandedWidthPx = 22.dp.toPx()
                    val chevronCompactWidthPx = 30.dp.toPx()
                    val currentChevronWidthPx = (
                        chevronExpandedWidthPx + (chevronCompactWidthPx - chevronExpandedWidthPx) * clampedT
                        ).roundToInt().coerceAtLeast(1)
                    val chevronPlaceable = chevronMeasurable.measure(Constraints.fixed(currentChevronWidthPx, maxH))

                    val gapPlayInfoPx = 6.dp.toPx()
                    val chevronMinX = playPlaceable.width.toFloat()
                    val chevronX = (maxW - chevronPlaceable.width).toFloat().coerceAtLeast(chevronMinX)
                    val infoX = (playPlaceable.width + gapPlayInfoPx).roundToInt()
                    val currentInfoWidthPx = (chevronX.roundToInt() - infoX).coerceAtLeast(1)

                    val infoPlaceable = infoMeasurable.measure(
                        Constraints.fixedWidth(currentInfoWidthPx)
                    )

                    layout(maxW, maxH) {
                        val infoAlpha = (1f - clampedT * 2.4f).coerceIn(0f, 1f)

                        playPlaceable.placeRelative(x = 0, y = 0)

                        if (infoAlpha > 0.01f) {
                            val infoY = ((maxH - infoPlaceable.height) / 2f).roundToInt()
                            infoPlaceable.placeWithLayer(x = infoX, y = infoY) {
                                alpha = infoAlpha
                                translationX = -6.dp.toPx() * clampedT
                                scaleX = 1f - 0.04f * clampedT
                                scaleY = 1f - 0.04f * clampedT
                                transformOrigin = TransformOrigin(0f, 0.5f)
                            }
                        }

                        chevronPlaceable.placeRelative(x = chevronX.roundToInt(), y = 0)
                    }
                }
            }
        }
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomPillTimeAndQueueText(
    queueNameText: String,
    currentPositionMsFlow: StateFlow<Long>,
    durationMs: Long,
    isDraggingState: androidx.compose.runtime.State<Boolean>,
    dragFractionState: androidx.compose.runtime.FloatState,
    contentColor: Color,
    coveredColor: Color,
    isDarkTheme: Boolean,
    waveSyncState: WaveSyncState,
    modifier: Modifier = Modifier
) {
    val positionSecFlow = remember(currentPositionMsFlow) {
        currentPositionMsFlow
            .map { it / 1000L }
            .distinctUntilChanged()
    }
    val currentPositionSec by positionSecFlow.collectAsStateWithLifecycle(
        initialValue = currentPositionMsFlow.value / 1000L
    )
    val safeDurationMs = durationMs.coerceAtLeast(0L)
    val totalSec = safeDurationMs / 1000L
    val displaySec = if (isDraggingState.value && safeDurationMs > 0L) {
        ((dragFractionState.floatValue * safeDurationMs).toLong().coerceIn(0L, safeDurationMs)) / 1000L
    } else {
        currentPositionSec.coerceIn(0L, totalSec.coerceAtLeast(1L))
    }
    val (curTimeText, totalTimeText) = remember(displaySec, totalSec) {
        formatTimePair(displaySec, totalSec)
    }
    val timeAndQueueText = stringResource(
        R.string.now_playing_time_queue_format,
        curTimeText,
        totalTimeText,
        queueNameText
    )

    Text(
        text = timeAndQueueText,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontFeatureSettings = "tnum"
        ),
        color = contentColor.copy(alpha = 0.86f),
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = modifier
            .waveProgressMask(
                waveSyncState = waveSyncState,
                isDarkTheme = isDarkTheme,
                coveredColor = coveredColor
            )
            .basicMarquee(
                iterations = Int.MAX_VALUE,
                repeatDelayMillis = 1800,
                initialDelayMillis = 1500,
                velocity = 28.dp
            )
    )
}

