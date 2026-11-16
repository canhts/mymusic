package com.example.ui

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.R
import com.example.data.AudioBeatFrame
import com.example.data.SongSpectrumProfile
import com.example.model.PlayMode
import com.example.model.Song
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.findHostActivity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Trang chơi nhạc toàn màn hình (ẩn thanh trạng thái Android):
 * - Quả cầu hạt 3D gọn nét, bắt nhịp tức thời theo nhạc thật và hỗ trợ xoay tự do mọi hướng bằng tay.
 * - Toàn bộ cụm tên bài, thanh tiến trình, nút điều khiển & hàng nút chức năng được gom gọn
 *   vào Thẻ điều khiển nổi phía dưới với nút chữ V đóng/mở và tự động ẩn sau 1 phút không chạm.
 * - Khi Thẻ điều khiển ẩn xuống, khối cầu 3D tự động dịch chuyển êm xuống chính giữa màn hình.
 */
@Composable
internal fun NowPlayingScreen(
    song: Song,
    isPlaying: Boolean,
    playMode: PlayMode,
    durationMs: Long,
    currentPositionMsFlow: StateFlow<Long>,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCyclePlayMode: () -> Unit,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onCycleThemeMode: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    audioSpectrumProfile: SongSpectrumProfile? = null,
    inactivityTimeoutMs: Long = INACTIVITY_AUTO_COLLAPSE_TIMEOUT_MS,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findHostActivity()?.window
        val controller = if (window != null) {
            WindowCompat.getInsetsController(window, view)
        } else {
            null
        }
        controller?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    BackHandler(onBack = onBack)

    var isControlCardVisible by remember { mutableStateOf(true) }
    val userInteractionFlow = remember { MutableStateFlow(0L) }

    LaunchedEffect(isControlCardVisible, inactivityTimeoutMs) {
        if (isControlCardVisible) {
            userInteractionFlow.collectLatest {
                delay(inactivityTimeoutMs)
                isControlCardVisible = false
            }
        }
    }

    val effectiveDuration = (if (durationMs > 0L) durationMs else song.duration).coerceAtLeast(1L)
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val flowPositionMs by currentPositionMsFlow.collectAsStateWithLifecycle()
    val livePositionAnimatable = remember(song.id) {
        Animatable(flowPositionMs.toFloat().coerceIn(0f, effectiveDuration.toFloat()))
    }

    var isDraggingScrubber by remember(song.id) { mutableStateOf(false) }
    var dragFraction by remember(song.id) { mutableFloatStateOf(0f) }

    LaunchedEffect(song.id, currentPositionMsFlow, effectiveDuration, isPlaying, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            currentPositionMsFlow.collectLatest { posMs ->
                if (!isDraggingScrubber) {
                    val target = posMs.toFloat().coerceIn(0f, effectiveDuration.toFloat())
                    val delta = abs(target - livePositionAnimatable.value)
                    if (!isPlaying || delta > 600f || target <= 10f) {
                        livePositionAnimatable.snapTo(target)
                    } else if (delta > 0.5f) {
                        livePositionAnimatable.animateTo(
                            targetValue = target,
                            animationSpec = tween(durationMillis = 26, easing = LinearEasing)
                        )
                    }
                }
            }
        }
    }

    val livePositionState = remember(livePositionAnimatable) { livePositionAnimatable.asState() }
    val audioEnvelope = remember(song.id) { AudioEnvelopeState() }

    // Bộ lọc bao không đối xứng hợp nhất trên 1 vòng lặp VSync duy nhất (Lifecycle-aware, Zero-Allocation)
    LaunchedEffect(song.id, isPlaying, isDraggingScrubber, audioSpectrumProfile, effectiveDuration, lifecycleOwner) {
        if (!isPlaying || isDraggingScrubber || audioSpectrumProfile == null) {
            if (!audioEnvelope.isNearSilent()) {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    var lastNanos = 0L
                    while (isActive && !audioEnvelope.isNearSilent()) {
                        withFrameNanos { frameNanos ->
                            val dtMs = if (lastNanos == 0L) {
                                16.67f
                            } else {
                                ((frameNanos - lastNanos).coerceIn(1_000_000L, 48_000_000L)) / 1_000_000f
                            }
                            lastNanos = frameNanos
                            audioEnvelope.decayToSilent(dtMs)
                        }
                    }
                    audioEnvelope.snapSilent()
                }
            }
            return@LaunchedEffect
        }

        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                var lastNanos = 0L
                while (isActive) {
                    val samplePos = (livePositionAnimatable.value.toLong() + 15L).coerceIn(0L, effectiveDuration)
                    val target = audioSpectrumProfile.sampleAt(samplePos)
                    if (!livePositionAnimatable.isRunning && audioEnvelope.isNearTarget(target)) {
                        audioEnvelope.snapTo(target)
                        break
                    }
                    withFrameNanos { frameNanos ->
                        val dtMs = if (lastNanos == 0L) {
                            16.67f
                        } else {
                            ((frameNanos - lastNanos).coerceIn(1_000_000L, 48_000_000L)) / 1_000_000f
                        }
                        lastNanos = frameNanos
                        val framePos = (livePositionAnimatable.value.toLong() + 15L).coerceIn(0L, effectiveDuration)
                        audioEnvelope.stepToward(audioSpectrumProfile.sampleAt(framePos), dtMs)
                    }
                }
                snapshotFlow {
                    livePositionAnimatable.isRunning to livePositionAnimatable.value
                }.first { (running, pos) ->
                    if (running) {
                        true
                    } else {
                        val settledPos = (pos.toLong() + 15L).coerceIn(0L, effectiveDuration)
                        !audioEnvelope.isNearTarget(audioSpectrumProfile.sampleAt(settledPos))
                    }
                }
            }
        }
    }

    val bgBase by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.background,
        animationSpec = tween(durationMillis = 260),
        label = "player_theme_bg"
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val primaryAuraFilter = remember(primaryColor) { ColorFilter.tint(primaryColor) }
    val tertiaryAuraFilter = remember(tertiaryColor) { ColorFilter.tint(tertiaryColor) }
    val surfaceHigh by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(durationMillis = 260),
        label = "player_theme_surface_high"
    )
    val onSurface by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 260),
        label = "player_theme_on_surface"
    )
    val onSurfaceVariant by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 260),
        label = "player_theme_on_surface_variant"
    )

    val sphereCenterProgress by animateFloatAsState(
        targetValue = if (isControlCardVisible) 0f else 1f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "sphere_center_transition"
    )

    val cutoutTopPadding = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val topPadding = cutoutTopPadding.coerceAtLeast(10.dp)
    val bottomPadding = navBottomPadding + 10.dp

    val debouncedBack = rememberDebouncedAction(debounceMs = 200L, onClick = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBase)
            .observeUserTouch { userInteractionFlow.value++ }
            .drawBehind {
                val baseAlpha = if (isDarkTheme) 0.15f else 0.10f
                val envOverall = audioEnvelope.overall
                val envBass = audioEnvelope.bass
                val envTreble = audioEnvelope.treble
                val envHighMid = audioEnvelope.highMid
                val reactiveAlpha = (baseAlpha + envOverall * 0.16f + envBass * 0.08f).coerceIn(0f, 1f)
                val isWide = size.width > size.height && size.width >= 600.dp.toPx()
                val auraCenterX = if (isWide) {
                    size.width * (0.28f + 0.22f * sphereCenterProgress)
                } else {
                    size.width * 0.5f
                }
                val auraCenterY = if (isWide) {
                    size.height * 0.5f
                } else {
                    size.height * (0.28f + 0.22f * sphereCenterProgress)
                }

                clipRect {
                    val primaryScale = (size.maxDimension * (0.48f + 0.12f * envBass)) / 100f
                    withTransform({
                        translate(left = auraCenterX, top = auraCenterY)
                        scale(scaleX = primaryScale, scaleY = primaryScale, pivot = Offset.Zero)
                    }) {
                        drawCircle(
                            brush = UNIT_PLAYER_AURA_BRUSH,
                            radius = 100f,
                            center = Offset.Zero,
                            alpha = reactiveAlpha,
                            colorFilter = primaryAuraFilter
                        )
                    }
                    if (envTreble > 0.02f || envHighMid > 0.02f) {
                        val tertiaryScale = (size.maxDimension * 0.44f) / 100f
                        val tertiaryAlpha = (envTreble * 0.10f + envHighMid * 0.06f).coerceIn(0f, 1f)
                        withTransform({
                            translate(left = auraCenterX, top = auraCenterY)
                            scale(scaleX = tertiaryScale, scaleY = tertiaryScale, pivot = Offset.Zero)
                        }) {
                            drawCircle(
                                brush = UNIT_PLAYER_AURA_BRUSH,
                                radius = 100f,
                                center = Offset.Zero,
                                alpha = tertiaryAlpha,
                                colorFilter = tertiaryAuraFilter
                            )
                        }
                    }
                }
            }
            // Chặn toàn bộ thao tác chạm lọt xuống màn hình bên dưới
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .testTag("now_playing_full_screen")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding, bottom = bottomPadding, start = 16.dp, end = 16.dp)
        ) {
            val density = LocalDensity.current
            val isLandscapeWide = maxWidth >= 600.dp && maxWidth > maxHeight
            val availableHeight = maxHeight
            val isCompactHeight = availableHeight < 620.dp
            var measuredCardHeightPx by remember { mutableIntStateOf(0) }
            var measuredCardWidthPx by remember { mutableIntStateOf(0) }

            val fallbackCardHeightDp = when {
                isLandscapeWide -> (availableHeight * 0.86f).coerceIn(260.dp, 340.dp)
                isCompactHeight -> 278.dp
                else -> 324.dp
            }
            val effectiveCardHeightDp = if (measuredCardHeightPx > 0) {
                with(density) { measuredCardHeightPx.toDp() }
            } else {
                fallbackCardHeightDp
            }

            val sphereSize = if (isLandscapeWide) {
                val visibleSize = (availableHeight * 0.78f).coerceIn(176.dp, 286.dp)
                val hiddenSize = (availableHeight * 0.86f).coerceIn(210.dp, 336.dp)
                lerpDp(visibleSize, hiddenSize, sphereCenterProgress)
            } else {
                val openAboveCard = (availableHeight - effectiveCardHeightDp - 12.dp).coerceAtLeast(108.dp)
                val visibleSize = (openAboveCard * 0.86f)
                    .coerceIn(108.dp, 268.dp)
                    .coerceAtMost(maxWidth * 0.80f)
                val hiddenSize = (availableHeight * 0.56f)
                    .coerceIn(196.dp, 340.dp)
                    .coerceAtMost(maxWidth * 0.88f)
                lerpDp(visibleSize, hiddenSize, sphereCenterProgress)
            }

            val sphereOffsetX = if (isLandscapeWide) {
                val effectiveCardWidthDp = if (measuredCardWidthPx > 0) {
                    with(density) { measuredCardWidthPx.toDp() }
                } else {
                    (maxWidth * 0.48f).coerceIn(310.dp, 440.dp)
                }
                val visibleX = -((effectiveCardWidthDp / 2f) + 12.dp)
                    .coerceAtMost(((maxWidth - sphereSize) / 2f).coerceAtLeast(0.dp))
                lerpDp(visibleX, 0.dp, sphereCenterProgress)
            } else {
                0.dp
            }

            val sphereOffsetY = if (isLandscapeWide) {
                0.dp
            } else {
                val maxNegOffset = -((availableHeight - sphereSize) / 2f).coerceAtLeast(0.dp)
                val rawVisibleY = -((effectiveCardHeightDp / 2f) + 4.dp)
                val visibleY = rawVisibleY.coerceIn(maxNegOffset, 0.dp)
                lerpDp(visibleY, 0.dp, sphereCenterProgress)
            }

            // Cụm khối cầu 3D tương tác xoay mọi hướng, tự dịch vào chính giữa màn khi thẻ điều khiển ẩn
            AudioCloudSphereVisualizer(
                songId = song.id,
                audioEnvelope = audioEnvelope,
                livePositionState = livePositionState,
                isPlaying = isPlaying,
                isDarkTheme = isDarkTheme,
                primaryColor = primaryColor,
                tertiaryColor = tertiaryColor,
                neutralDotColor = onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(sphereOffsetX.roundToPx(), sphereOffsetY.roundToPx()) }
                    .size(sphereSize)
            )

            // Thẻ điều khiển nổi phía dưới (chứa tên bài, tiến trình, điều khiển phát & hàng chức năng)
            AnimatedVisibility(
                visible = isControlCardVisible,
                enter = fadeIn(tween(240)) +
                    slideInVertically(
                        initialOffsetY = { it + it / 4 },
                        animationSpec = spring(
                            dampingRatio = 0.78f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                exit = fadeOut(tween(180)) +
                    slideOutVertically(
                        targetOffsetY = { it + it / 4 },
                        animationSpec = tween(220)
                    ),
                modifier = Modifier
                    .align(if (isLandscapeWide) Alignment.BottomEnd else Alignment.BottomCenter)
                    .then(
                        if (isLandscapeWide) {
                            Modifier.width((maxWidth * 0.48f).coerceIn(310.dp, 450.dp))
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp)
                        }
                    )
                    .onSizeChanged { size ->
                        if (size.height > 0) measuredCardHeightPx = size.height
                        if (size.width > 0) measuredCardWidthPx = size.width
                    }
            ) {
                PlayerFloatingControlCard(
                    song = song,
                    isPlaying = isPlaying,
                    playMode = playMode,
                    themeMode = themeMode,
                    effectiveDuration = effectiveDuration,
                    livePositionState = livePositionState,
                    audioEnvelope = audioEnvelope,
                    isDraggingScrubber = isDraggingScrubber,
                    dragFraction = dragFraction,
                    isDarkTheme = isDarkTheme,
                    compact = isCompactHeight || isLandscapeWide,
                    primaryColor = primaryColor,
                    tertiaryColor = tertiaryColor,
                    onPrimaryColor = MaterialTheme.colorScheme.onPrimary,
                    surfaceHigh = surfaceHigh,
                    onSurface = onSurface,
                    onSurfaceVariant = onSurfaceVariant,
                    onHideCard = { isControlCardVisible = false },
                    onDragStart = { frac ->
                        isDraggingScrubber = true
                        dragFraction = frac
                    },
                    onDragUpdate = { frac ->
                        dragFraction = frac
                    },
                    onDragFinish = { frac ->
                        val targetMs = (frac.coerceIn(0f, 1f) * effectiveDuration).toLong()
                        scope.launch { livePositionAnimatable.snapTo(targetMs.toFloat()) }
                        isDraggingScrubber = false
                        onSeekTo(targetMs)
                    },
                    onTapSeek = { frac ->
                        val targetMs = (frac.coerceIn(0f, 1f) * effectiveDuration).toLong()
                        scope.launch { livePositionAnimatable.snapTo(targetMs.toFloat()) }
                        onSeekTo(targetMs)
                    },
                    onTogglePlayPause = onTogglePlayPause,
                    onPlayPrevious = onPlayPrevious,
                    onPlayNext = onPlayNext,
                    onBack = debouncedBack,
                    onOpenQueue = onOpenQueue,
                    onCycleThemeMode = onCycleThemeMode,
                    onCyclePlayMode = onCyclePlayMode
                )
            }

            // Nút chữ V hướng lên nổi ở cạnh dưới khi thẻ điều khiển đang ẩn
            AnimatedVisibility(
                visible = !isControlCardVisible,
                enter = fadeIn(tween(220)) +
                    scaleIn(
                        initialScale = 0.78f,
                        animationSpec = spring(
                            dampingRatio = 0.62f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) +
                    slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut(tween(150)) +
                    scaleOut(targetScale = 0.80f),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PlayerShowControlsChevronButton(
                    surfaceHigh = surfaceHigh,
                    onSurface = onSurface,
                    isDarkTheme = isDarkTheme,
                    onClick = { isControlCardVisible = true }
                )
            }
        }
    }
}

@Composable
private fun PlayerFloatingControlCard(
    song: Song,
    isPlaying: Boolean,
    playMode: PlayMode,
    themeMode: ThemeMode,
    effectiveDuration: Long,
    livePositionState: State<Float>,
    audioEnvelope: AudioEnvelopeState,
    isDraggingScrubber: Boolean,
    dragFraction: Float,
    isDarkTheme: Boolean,
    compact: Boolean,
    primaryColor: Color,
    tertiaryColor: Color,
    onPrimaryColor: Color,
    surfaceHigh: Color,
    onSurface: Color,
    onSurfaceVariant: Color,
    onHideCard: () -> Unit,
    onDragStart: (Float) -> Unit,
    onDragUpdate: (Float) -> Unit,
    onDragFinish: (Float) -> Unit,
    onTapSeek: (Float) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    onCycleThemeMode: () -> Unit,
    onCyclePlayMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = surfaceHigh.copy(alpha = if (isDarkTheme) 0.90f else 0.95f)
    val cardBorder = BorderStroke(
        width = 1.dp,
        color = onSurface.copy(alpha = if (isDarkTheme) 0.11f else 0.06f)
    )
    val titleGap = if (compact) 2.dp else 8.dp
    val controlsGap = if (compact) 2.dp else 10.dp
    val bottomRowGap = if (compact) 6.dp else 12.dp
    val chevronInteraction = remember { MutableInteractionSource() }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = cardColor,
        contentColor = onSurface,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        border = cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .testTag("player_control_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 2.dp,
                    bottom = if (compact) 12.dp else 16.dp
                )
        ) {
            // Nút chữ V hướng xuống đặt chính giữa trên đầu thẻ để đóng hoặc vuốt xuống ẩn thẻ
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth(0.38f)
                    .widthIn(min = 76.dp, max = 140.dp)
                    .height(48.dp)
                    .bottomControlBounce(
                        scaleDown = 0.88f,
                        dipY = 2.dp,
                        interactionSource = chevronInteraction
                    )
                    .clip(RoundedCornerShape(percent = 50))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { dragAccumulatedY = 0f },
                            onDragEnd = {
                                if (dragAccumulatedY > 28.dp.toPx()) {
                                    onHideCard()
                                }
                                dragAccumulatedY = 0f
                            },
                            onDragCancel = { dragAccumulatedY = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulatedY += dragAmount
                            }
                        )
                    }
                    .clickable(
                        interactionSource = chevronInteraction,
                        indication = null,
                        onClick = onHideCard
                    )
                    .testTag("btn_player_hide_control_card")
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.cd_hide_player_controls),
                    tint = onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }

            PlayerSongTitle(
                song = song,
                textColor = onSurface,
                maxLines = if (compact) 1 else 2
            )

            Spacer(modifier = Modifier.height(titleGap))

            WavyInteractiveScrubber(
                durationMs = effectiveDuration,
                isPlaying = isPlaying,
                livePositionState = livePositionState,
                audioOverallProvider = { audioEnvelope.overall },
                isDragging = isDraggingScrubber,
                dragFraction = dragFraction,
                primaryColor = primaryColor,
                trackColor = onSurfaceVariant.copy(alpha = 0.22f),
                labelColor = onSurfaceVariant,
                onDragStart = onDragStart,
                onDragUpdate = onDragUpdate,
                onDragFinish = onDragFinish,
                onTapSeek = onTapSeek
            )

            Spacer(modifier = Modifier.height(controlsGap))

            NewDesignMusicControls(
                isPlaying = isPlaying,
                beatPulseProvider = { audioEnvelope.beatPulse },
                primaryColor = primaryColor,
                tertiaryColor = tertiaryColor,
                onPrimaryColor = onPrimaryColor,
                surfaceHigh = surfaceHigh,
                onSurface = onSurface,
                compact = compact,
                onTogglePlayPause = onTogglePlayPause,
                onPlayPrevious = onPlayPrevious,
                onPlayNext = onPlayNext
            )

            Spacer(modifier = Modifier.height(bottomRowGap))

            PlayerBottomUtilityRow(
                playMode = playMode,
                themeMode = themeMode,
                primaryColor = primaryColor,
                tertiaryColor = tertiaryColor,
                surfaceHigh = surfaceHigh,
                onSurface = onSurface,
                isDarkTheme = isDarkTheme,
                onBack = onBack,
                onOpenQueue = onOpenQueue,
                onCycleThemeMode = onCycleThemeMode,
                onCyclePlayMode = onCyclePlayMode
            )
        }
    }
}

@Composable
private fun PlayerShowControlsChevronButton(
    surfaceHigh: Color,
    onSurface: Color,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val minimalChevronUp = remember {
        ImageVector.Builder(
            name = "minimal_chevron_up",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.65f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.5f, 14.75f)
                lineTo(12f, 8.25f)
                lineTo(18.5f, 14.75f)
            }
        }.build()
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .bottomControlBounce(
                scaleDown = 0.84f,
                dipY = 1.dp,
                interactionSource = interactionSource
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("btn_player_show_control_card")
    ) {
        Icon(
            imageVector = minimalChevronUp,
            contentDescription = stringResource(R.string.cd_show_player_controls),
            tint = onSurface.copy(alpha = if (isDarkTheme) 0.78f else 0.72f),
            modifier = Modifier.size(28.dp)
        )
    }
}

private val UNIT_PLAYER_AURA_BRUSH: Brush by lazy(LazyThreadSafetyMode.PUBLICATION) {
    Brush.radialGradient(
        colors = listOf(Color.White, Color.Transparent),
        center = Offset.Zero,
        radius = 100f
    )
}

@Stable
private class AudioEnvelopeState {
    var overall by mutableFloatStateOf(0f)
    var bass by mutableFloatStateOf(0f)
    var lowMid by mutableFloatStateOf(0f)
    var highMid by mutableFloatStateOf(0f)
    var treble by mutableFloatStateOf(0f)
    var onset by mutableFloatStateOf(0f)

    val beatPulse: Float
        get() = (bass * 0.62f + onset * 0.38f).coerceIn(0f, 1f)

    fun isNearSilent(): Boolean =
        overall < 0.0005f &&
            bass < 0.0005f &&
            lowMid < 0.0005f &&
            highMid < 0.0005f &&
            treble < 0.0005f &&
            onset < 0.0005f

    fun snapSilent() {
        overall = 0f
        bass = 0f
        lowMid = 0f
        highMid = 0f
        treble = 0f
        onset = 0f
    }

    fun isNearTarget(target: AudioBeatFrame): Boolean =
        abs(overall - target.overall) < 0.0005f &&
            abs(bass - target.bass) < 0.0005f &&
            abs(lowMid - target.lowMid) < 0.0005f &&
            abs(highMid - target.highMid) < 0.0005f &&
            abs(treble - target.treble) < 0.0005f &&
            abs(onset - target.onset) < 0.0005f

    fun snapTo(target: AudioBeatFrame) {
        overall = target.overall
        bass = target.bass
        lowMid = target.lowMid
        highMid = target.highMid
        treble = target.treble
        onset = target.onset
    }

    fun stepToward(target: AudioBeatFrame, dtMs: Float) {
        overall = stepAudioBand(overall, target.overall, dtMs, attackMs = 16f, releaseMs = 115f)
        bass = stepAudioBand(bass, target.bass, dtMs, attackMs = 12f, releaseMs = 105f)
        lowMid = stepAudioBand(lowMid, target.lowMid, dtMs, attackMs = 16f, releaseMs = 110f)
        highMid = stepAudioBand(highMid, target.highMid, dtMs, attackMs = 16f, releaseMs = 105f)
        treble = stepAudioBand(treble, target.treble, dtMs, attackMs = 12f, releaseMs = 90f)
        onset = stepAudioBand(onset, target.onset, dtMs, attackMs = 10f, releaseMs = 120f)
    }

    fun decayToSilent(dtMs: Float) {
        overall = stepAudioBand(overall, 0f, dtMs, attackMs = 16f, releaseMs = 115f)
        bass = stepAudioBand(bass, 0f, dtMs, attackMs = 12f, releaseMs = 105f)
        lowMid = stepAudioBand(lowMid, 0f, dtMs, attackMs = 16f, releaseMs = 110f)
        highMid = stepAudioBand(highMid, 0f, dtMs, attackMs = 16f, releaseMs = 105f)
        treble = stepAudioBand(treble, 0f, dtMs, attackMs = 12f, releaseMs = 90f)
        onset = stepAudioBand(onset, 0f, dtMs, attackMs = 10f, releaseMs = 120f)
    }

    private fun stepAudioBand(
        current: Float,
        target: Float,
        dtMs: Float,
        attackMs: Float,
        releaseMs: Float
    ): Float {
        val tau = (if (target >= current) attackMs else releaseMs) * 0.45f
        val alpha = (1f - exp(-dtMs / tau.coerceAtLeast(1f))).coerceIn(0f, 1f)
        return current + (target - current) * alpha
    }
}

@Immutable
internal class SphereCloudBuffers(
    val count: Int,
    val nx: FloatArray,
    val ny: FloatArray,
    val nz: FloatArray,
    val baseR: FloatArray,
    val azimuth: FloatArray,
    val elevation: FloatArray,
    val layer: IntArray, // 0 = lớp vỏ ngoài gọn nét, 1 = các dải nổi khối, 2 = lõi chiều sâu bên trong
    val sizeWeight: FloatArray,
    val alphaWeight: FloatArray,
    val phaseSeed: FloatArray
)

@Immutable
private data class SphereQuat(
    val w: Float,
    val x: Float,
    val y: Float,
    val z: Float
) {
    fun normalized(): SphereQuat {
        val len = sqrt(w * w + x * x + y * y + z * z)
        if (len < 1e-6f) return IDENTITY
        val inv = 1f / len
        return SphereQuat(w * inv, x * inv, y * inv, z * inv)
    }

    operator fun times(o: SphereQuat): SphereQuat = SphereQuat(
        w = w * o.w - x * o.x - y * o.y - z * o.z,
        x = w * o.x + x * o.w + y * o.z - z * o.y,
        y = w * o.y - x * o.z + y * o.w + z * o.x,
        z = w * o.z + x * o.y - y * o.x + z * o.w
    )

    companion object {
        val IDENTITY = SphereQuat(1f, 0f, 0f, 0f)

        /** Quay quanh trục Y màn hình (vuốt ngang): góc > 0 đẩy mặt trước (+Z) sang phải (+X). */
        fun fromScreenYaw(angleRad: Float): SphereQuat {
            val half = angleRad * 0.5f
            return SphereQuat(cos(half), 0f, sin(half), 0f)
        }

        /** Quay quanh trục X màn hình (vuốt dọc): góc > 0 đẩy mặt trước (+Z) xuống dưới (+Y). */
        fun fromScreenPitch(angleRad: Float): SphereQuat {
            val half = angleRad * 0.5f
            return SphereQuat(cos(half), -sin(half), 0f, 0f)
        }
    }
}

/**
 * Khởi tạo trước 1020 hạt vi cầu gọn nét, không phân tán ra ngoài vỏ cầu:
 * gồm lớp vỏ Fibonacci mịn (0.94..1.00), 6 dải nổi khối (0.88..0.98), và lõi chiều sâu (0.62..0.86).
 */
internal val SPHERE_CLOUD_BUFFERS: SphereCloudBuffers by lazy {
    val shellCount = 560
    val ribbonCount = 320
    val innerCount = 140
    val total = shellCount + ribbonCount + innerCount

    val nx = FloatArray(total)
    val ny = FloatArray(total)
    val nz = FloatArray(total)
    val baseR = FloatArray(total)
    val azimuth = FloatArray(total)
    val elevation = FloatArray(total)
    val layer = IntArray(total)
    val sizeWeight = FloatArray(total)
    val alphaWeight = FloatArray(total)
    val phaseSeed = FloatArray(total)

    fun pseudoRand(seed: Int): Float {
        var x = seed * 747796405 + 2891336453.toInt()
        x = ((x ushr ((x ushr 28) + 4)) xor x) * 277803737
        x = (x ushr 22) xor x
        return ((x and 0x7FFFFFFF).toFloat() / 0x7FFFFFFF.toFloat())
    }

    val goldenAngle = (PI * (3.0 - kotlin.math.sqrt(5.0))).toFloat()
    var idx = 0

    // 1. Lớp vỏ cầu Fibonacci gọn chặt, đồng đều tạo hình khối rõ ràng
    for (i in 0 until shellCount) {
        val h1 = pseudoRand(i * 13 + 7)
        val h2 = pseudoRand(i * 29 + 19)
        val h3 = pseudoRand(i * 43 + 31)
        val yUnit = 1f - (2f * (i + 0.5f) / shellCount)
        val theta = goldenAngle * i + (h1 - 0.5f) * 0.14f
        val elev = kotlin.math.asin(yUnit.coerceIn(-1f, 1f)) + (h2 - 0.5f) * 0.05f
        val cosE = cos(elev)

        nx[idx] = cosE * cos(theta)
        ny[idx] = sin(elev)
        nz[idx] = cosE * sin(theta)
        baseR[idx] = 0.94f + h3 * 0.06f
        azimuth[idx] = theta
        elevation[idx] = elev
        layer[idx] = 0
        sizeWeight[idx] = 0.58f + h1 * 0.92f
        alphaWeight[idx] = 0.54f + h2 * 0.46f
        phaseSeed[idx] = h3 * 6.28318f
        idx++
    }

    // 2. 6 dải sương uốn lượn ôm sát mặt cầu tạo độ nổi khối tinh tế
    val ribbonCurves = 6
    val perRibbon = ribbonCount / ribbonCurves
    for (c in 0 until ribbonCurves) {
        val tilt = (c * 0.52f) - 1.1f
        val phaseShift = c * 1.047f
        for (j in 0 until perRibbon) {
            val h1 = pseudoRand(idx * 17 + 3)
            val h2 = pseudoRand(idx * 37 + 11)
            val u = (j.toFloat() / perRibbon.toFloat()) * 2f * PI.toFloat()
            val elev = (sin(u * 2f + phaseShift) * 0.88f + tilt * 0.28f + (h1 - 0.5f) * 0.05f)
                .coerceIn(-1.42f, 1.42f)
            val theta = u + cos(u * 3f + c) * 0.30f + (h2 - 0.5f) * 0.05f
            val cosE = cos(elev)

            nx[idx] = cosE * cos(theta)
            ny[idx] = sin(elev)
            nz[idx] = cosE * sin(theta)
            baseR[idx] = 0.88f + (h1 * 0.10f)
            azimuth[idx] = theta
            elevation[idx] = elev
            layer[idx] = 1
            sizeWeight[idx] = 0.70f + h2 * 1.05f
            alphaWeight[idx] = 0.62f + h1 * 0.38f
            phaseSeed[idx] = u + c
            idx++
        }
    }
    while (idx < shellCount + ribbonCount) {
        val h1 = pseudoRand(idx * 19 + 5)
        val h2 = pseudoRand(idx * 31 + 17)
        val theta = h1 * 6.28318f
        val elev = (h2 - 0.5f) * 2.4f
        val cosE = cos(elev)
        nx[idx] = cosE * cos(theta)
        ny[idx] = sin(elev)
        nz[idx] = cosE * sin(theta)
        baseR[idx] = 0.89f + h1 * 0.09f
        azimuth[idx] = theta
        elevation[idx] = elev
        layer[idx] = 1
        sizeWeight[idx] = 0.65f + h2 * 0.90f
        alphaWeight[idx] = 0.58f + h1 * 0.40f
        phaseSeed[idx] = h2 * 6.28318f
        idx++
    }

    // 3. Hạt lõi chiều sâu nằm trọn bên trong quả cầu (0.62..0.86) — không bay văng ra rìa ngoài
    for (k in 0 until innerCount) {
        val h1 = pseudoRand(idx * 23 + 9)
        val h2 = pseudoRand(idx * 41 + 13)
        val h3 = pseudoRand(idx * 53 + 29)
        val theta = h1 * 6.28318f
        val elev = kotlin.math.asin((h2 * 2f - 1f).coerceIn(-1f, 1f))
        val cosE = cos(elev)

        nx[idx] = cosE * cos(theta)
        ny[idx] = sin(elev)
        nz[idx] = cosE * sin(theta)
        baseR[idx] = 0.62f + h3 * 0.24f
        azimuth[idx] = theta
        elevation[idx] = elev
        layer[idx] = 2
        sizeWeight[idx] = 0.50f + h2 * 0.65f
        alphaWeight[idx] = 0.34f + h1 * 0.38f
        phaseSeed[idx] = h3 * 6.28318f
        idx++
    }

    SphereCloudBuffers(
        count = total,
        nx = nx,
        ny = ny,
        nz = nz,
        baseR = baseR,
        azimuth = azimuth,
        elevation = elevation,
        layer = layer,
        sizeWeight = sizeWeight,
        alphaWeight = alphaWeight,
        phaseSeed = phaseSeed
    )
}

/**
 * Hiệu ứng Play khối cầu hạt 3D tương tác:
 * - Hạt li ti kết thành khối cầu 3D gọn gàng, không phân tán loãng.
 * - Phản ứng tức thì theo âm thanh thật của từng bài hát (Bass, Mid, Treble, Onset).
 * - Hỗ trợ xoay tự do theo mọi hướng bằng thao tác chạm/kéo có quán tính êm, đồng thời
 *   vẫn giữ trọn chuyển động nhịp theo nhạc trong suốt lúc tay đang giữ/xoay quả cầu.
 */
@Composable
private fun AudioCloudSphereVisualizer(
    songId: Long,
    audioEnvelope: AudioEnvelopeState,
    livePositionState: State<Float>,
    isPlaying: Boolean,
    isDarkTheme: Boolean,
    primaryColor: Color,
    tertiaryColor: Color,
    neutralDotColor: Color,
    modifier: Modifier = Modifier
) {
    var userQuat by remember(songId) { mutableStateOf(SphereQuat.IDENTITY) }
    var isUserDragging by remember(songId) { mutableStateOf(false) }
    var angularVelX by remember(songId) { mutableFloatStateOf(0f) }
    var angularVelY by remember(songId) { mutableFloatStateOf(0f) }

    // Quán tính mượt sau khi người dùng nhấc tay khỏi quả cầu
    LaunchedEffect(songId, isUserDragging) {
        if (!isUserDragging && (abs(angularVelX) > 0.0008f || abs(angularVelY) > 0.0008f)) {
            var lastNanos = 0L
            while (isActive && (abs(angularVelX) > 0.0008f || abs(angularVelY) > 0.0008f)) {
                withFrameNanos { frameNanos ->
                    if (lastNanos != 0L) {
                        val dt = ((frameNanos - lastNanos).coerceIn(1_000_000L, 48_000_000L)) / 16_666_667f
                        val qYaw = SphereQuat.fromScreenYaw(angularVelY * dt)
                        val qPitch = SphereQuat.fromScreenPitch(angularVelX * dt)
                        userQuat = (qPitch * qYaw * userQuat).normalized()
                        val damping = kotlin.math.exp(-0.085f * dt)
                        angularVelX *= damping
                        angularVelY *= damping
                    }
                    lastNanos = frameNanos
                }
            }
            angularVelX = 0f
            angularVelY = 0f
        }
    }

    val sphereContentDesc = stringResource(R.string.cd_interactive_audio_sphere)
    val orientationTag = remember(userQuat) {
        val qx = (userQuat.x * 1000f).toInt()
        val qy = (userQuat.y * 1000f).toInt()
        val qz = (userQuat.z * 1000f).toInt()
        val qw = (userQuat.w * 1000f).toInt()
        "${qw}_${qx}_${qy}_${qz}"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .pointerInput(songId) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isUserDragging = true
                    angularVelX = 0f
                    angularVelY = 0f
                    var lastPos = down.position
                    val minSide = minOf(size.width, size.height).toFloat().coerceAtLeast(140f)
                    val sensitivity = 3.2f / minSide
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                                ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            val delta = change.position - lastPos
                            if (delta.x != 0f || delta.y != 0f) {
                                change.consume()
                                lastPos = change.position
                                val dYaw = delta.x * sensitivity
                                val dPitch = delta.y * sensitivity
                                val qYaw = SphereQuat.fromScreenYaw(dYaw)
                                val qPitch = SphereQuat.fromScreenPitch(dPitch)
                                userQuat = (qPitch * qYaw * userQuat).normalized()
                                angularVelY = (angularVelY * 0.45f) + (dYaw * 0.55f)
                                angularVelX = (angularVelX * 0.45f) + (dPitch * 0.55f)
                            }
                        }
                    } finally {
                        isUserDragging = false
                    }
                }
            }
            .semantics {
                contentDescription = sphereContentDesc
                stateDescription = orientationTag
            }
            .testTag("player_kinetic_artwork")
    ) {
        val auraBrush = remember(primaryColor, tertiaryColor) {
            Brush.radialGradient(
                colors = listOf(
                    primaryColor,
                    tertiaryColor.copy(alpha = 0.36f),
                    Color.Transparent
                ),
                center = Offset.Zero,
                radius = 100f
            )
        }
        val crestColor = remember(isDarkTheme, primaryColor, tertiaryColor) {
            if (isDarkTheme) {
                lerp(primaryColor, Color.White, 0.72f)
            } else {
                lerp(primaryColor, tertiaryColor, 0.25f)
            }
        }
        val deepMistColor = remember(neutralDotColor, primaryColor) {
            lerp(neutralDotColor, primaryColor, 0.45f)
        }

        AnimatedContent(
            targetState = songId,
            transitionSpec = {
                (fadeIn(tween(260)) +
                    scaleIn(
                        initialScale = 0.88f,
                        animationSpec = spring(
                            dampingRatio = 0.68f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ))
                    .togetherWith(
                        fadeOut(tween(180)) + scaleOut(targetScale = 0.90f)
                    )
            },
            label = "cloud_sphere_song_transition",
            modifier = Modifier.fillMaxSize()
        ) { _ ->
            Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = size.minDimension
            val center = Offset(size.width / 2f, size.height / 2f)
            val sphereRadius = minDim * 0.405f

            val bass = audioEnvelope.bass
            val lowMid = audioEnvelope.lowMid
            val highMid = audioEnvelope.highMid
            val treble = audioEnvelope.treble
            val overall = audioEnvelope.overall
            val onset = audioEnvelope.onset

            // Quầng khí quyển gọn ôm quanh khối cầu (Zero-Allocation via normalized brush scale)
            val auraAlpha = (if (isPlaying) 0.06f + overall * 0.20f + bass * 0.09f else 0.04f).coerceIn(0f, 1f)
            val auraRadius = minDim * (0.44f + 0.045f * bass)
            val auraScale = auraRadius / 100f
            withTransform({
                translate(left = center.x, top = center.y)
                scale(scaleX = auraScale, scaleY = auraScale, pivot = Offset.Zero)
            }) {
                drawCircle(
                    brush = auraBrush,
                    radius = 100f,
                    center = Offset.Zero,
                    alpha = auraAlpha
                )
            }

            // Hướng xoay 3D tổng hợp từ thao tác tay (Quaternion không gimbal lock) + tự xoay nhẹ theo nhạc
            val timeSec = livePositionState.value * 0.001f
            val autoYaw = timeSec * 0.36f
            val autoPitch = 0.20f + sin(timeSec * 0.24f) * 0.14f
            val autoQuat = (SphereQuat.fromScreenPitch(autoPitch) * SphereQuat.fromScreenYaw(autoYaw))
            val totalQuat = (userQuat * autoQuat).normalized()

            // Chuyển Quaternion sang ma trận xoay 3x3 một lần cho toàn bộ khung hình
            val qw = totalQuat.w
            val qx = totalQuat.x
            val qy = totalQuat.y
            val qz = totalQuat.z
            val m00 = 1f - 2f * (qy * qy + qz * qz)
            val m01 = 2f * (qx * qy - qz * qw)
            val m02 = 2f * (qx * qz + qy * qw)
            val m10 = 2f * (qx * qy + qz * qw)
            val m11 = 1f - 2f * (qx * qx + qz * qz)
            val m12 = 2f * (qy * qz - qx * qw)
            val m20 = 2f * (qx * qz - qy * qw)
            val m21 = 2f * (qy * qz + qx * qw)
            val m22 = 1f - 2f * (qx * qx + qy * qy)

            val bodyColor = lerp(primaryColor, tertiaryColor, (0.35f + treble * 0.45f).coerceIn(0f, 1f))

            val baseDotPx = (minDim * 0.0048f).coerceIn(0.85.dp.toPx(), 1.95.dp.toPx())
            val waveFlowPhase = timeSec * 1.45f
            val wf085 = waveFlowPhase * 0.85f
            val wf055 = waveFlowPhase * 0.55f
            val wf125 = waveFlowPhase * 1.25f
            val wf190 = waveFlowPhase * 1.9f
            val bassScale = 0.022f + bass * 0.082f + lowMid * 0.028f
            val midScale = 0.012f + highMid * 0.042f
            val trebleScale = 0.006f + treble * 0.032f
            val curlAmp = 0.032f * (0.3f + lowMid + highMid)
            val breathScale = 1f + bass * 0.055f + onset * 0.035f

            val b = SPHERE_CLOUD_BUFFERS
            for (i in 0 until b.count) {
                val az = b.azimuth[i]
                val el = b.elevation[i]
                val seed = b.phaseSeed[i]
                val particleLayer = b.layer[i]

                // Sóng gợn gọn trên bề mặt cầu theo nhịp thật (vẫn đập đều ngay cả khi đang dùng tay xoay)
                val macroFold = sin(3f * az + wf085) * cos(2f * el - wf055)
                val midRipple = sin(5f * el - 3f * az + wf125 + seed * 0.25f)
                val fineSpark = cos(7f * az + 5f * el - wf190)

                val pulsePush = onset * (if (particleLayer == 1) 0.048f else 0.032f)

                val displacedR = b.baseR[i] * breathScale * (
                    1f +
                        macroFold * bassScale +
                        midRipple * midScale +
                        fineSpark * trebleScale +
                        pulsePush
                    )

                val curlShift = macroFold * curlAmp
                val curIdx = i
                val x0 = (b.nx[curIdx] - b.nz[curIdx] * curlShift) * displacedR
                val y0 = b.ny[i] * displacedR
                val z0 = (b.nz[i] + b.nx[i] * curlShift) * displacedR

                // Xoay 3D theo ma trận tổng hợp Quaternion
                val x1 = m00 * x0 + m01 * y0 + m02 * z0
                val y2 = m10 * x0 + m11 * y0 + m12 * z0
                val z2 = m20 * x0 + m21 * y0 + m22 * z0

                val persp = 2.75f / (2.75f - z2.coerceIn(-1.25f, 1.25f))
                val px = center.x + x1 * sphereRadius * persp
                val py = center.y + y2 * sphereRadius * persp

                val depthFactor = ((z2 + 1.10f) / 2.20f).coerceIn(0f, 1f)
                val rimDensity = sqrt((x1 * x1 + y2 * y2).coerceIn(0f, 1.30f)).coerceIn(0f, 1f)
                val crestIntensity = (abs(macroFold) * 0.55f + abs(midRipple) * 0.45f).coerceIn(0f, 1f)

                val radiusMult = when (particleLayer) {
                    1 -> 1.12f + crestIntensity * 0.44f + onset * 0.30f
                    2 -> 0.88f + treble * 0.40f
                    else -> 0.94f + bass * 0.30f + crestIntensity * 0.22f
                }
                val dotRadius = baseDotPx * b.sizeWeight[i] * persp * radiusMult

                val highlightMix = (rimDensity * 0.52f + crestIntensity * 0.36f + depthFactor * 0.30f + onset * 0.24f - 0.16f)
                    .coerceIn(0f, 1f)
                val particleColor = when {
                    particleLayer == 2 -> deepMistColor
                    highlightMix > 0.55f -> lerp(bodyColor, crestColor, ((highlightMix - 0.55f) / 0.45f).coerceIn(0f, 1f))
                    else -> lerp(deepMistColor, bodyColor, (highlightMix / 0.55f).coerceIn(0f, 1f))
                }

                val interiorDim = if (particleLayer == 2) 0.52f else (0.46f + 0.54f * rimDensity)
                val playingBoost = if (isPlaying) {
                    (0.54f + overall * 0.34f + crestIntensity * 0.24f + onset * 0.18f).coerceIn(0.48f, 1.05f)
                } else {
                    0.56f
                }
                val finalAlpha = (b.alphaWeight[i] * (0.26f + 0.74f * depthFactor) * interiorDim * playingBoost)
                    .coerceIn(0.08f, 0.96f)

                drawCircle(
                    color = particleColor.copy(alpha = finalAlpha),
                    radius = dotRadius,
                    center = Offset(px, py)
                )
            }
            }
        }
    }
}

@Composable
private fun PlayerSongTitle(
    song: Song,
    textColor: Color,
    maxLines: Int = 2,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = song.id to song.displayTitle,
        transitionSpec = {
            (fadeIn(tween(220)) +
                slideInHorizontally(
                    initialOffsetX = { it / 6 },
                    animationSpec = spring(
                        dampingRatio = 0.76f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ))
                .togetherWith(
                    fadeOut(tween(140)) +
                        slideOutHorizontally(targetOffsetX = { -it / 6 })
                )
        },
        label = "player_song_title_anim",
        modifier = modifier.fillMaxWidth()
    ) { (_, title) ->
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                lineHeight = 27.sp
            ),
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("player_song_title")
        )
    }
}

@Composable
private fun WavyInteractiveScrubber(
    durationMs: Long,
    isPlaying: Boolean,
    livePositionState: State<Float>,
    audioOverallProvider: () -> Float,
    isDragging: Boolean,
    dragFraction: Float,
    primaryColor: Color,
    trackColor: Color,
    labelColor: Color,
    onDragStart: (Float) -> Unit,
    onDragUpdate: (Float) -> Unit,
    onDragFinish: (Float) -> Unit,
    onTapSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val widthPxHolder = remember { floatArrayOf(1f) }
    val wavePath = remember { Path() }
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragUpdate by rememberUpdatedState(onDragUpdate)
    val currentOnDragFinish by rememberUpdatedState(onDragFinish)
    val currentOnTapSeek by rememberUpdatedState(onTapSeek)
    val currentDragFraction by rememberUpdatedState(dragFraction)
    val currentIsDragging by rememberUpdatedState(isDragging)

    val thumbRadiusDp by animateDpAsState(
        targetValue = if (isDragging) 9.5.dp else 6.dp,
        animationSpec = spring(
            dampingRatio = 0.52f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scrubber_thumb_radius"
    )
    val wavePlayStateFactor by animateFloatAsState(
        targetValue = if (isDragging || isPlaying) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scrubber_wave_play_factor"
    )

    val validDuration = durationMs.coerceAtLeast(1L)
    val totalSec = (validDuration / 1000L).coerceAtLeast(0L)
    val displaySec by remember(validDuration) {
        derivedStateOf {
            val posMs = if (currentIsDragging) {
                (currentDragFraction.coerceIn(0f, 1f) * validDuration).toLong()
            } else {
                livePositionState.value.toLong().coerceIn(0L, validDuration)
            }
            (posMs / 1000L).coerceAtLeast(0L)
        }
    }
    val (curTimeText, totalTimeText) = remember(displaySec, totalSec) {
        formatTimePair(displaySec, totalSec)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .onSizeChanged { widthPxHolder[0] = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val w = widthPxHolder[0].coerceAtLeast(1f)
                        val frac = (offset.x / w).coerceIn(0f, 1f)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentOnTapSeek(frac)
                    }
                }
                .pointerInput(Unit) {
                    var lastFraction = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val w = widthPxHolder[0].coerceAtLeast(1f)
                            lastFraction = (offset.x / w).coerceIn(0f, 1f)
                            currentOnDragStart(lastFraction)
                        },
                        onDragEnd = {
                            currentOnDragFinish(lastFraction)
                        },
                        onDragCancel = {
                            currentOnDragFinish(lastFraction)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val w = widthPxHolder[0].coerceAtLeast(1f)
                            lastFraction = (change.position.x / w).coerceIn(0f, 1f)
                            currentOnDragUpdate(lastFraction)
                        }
                    )
                }
                .testTag("player_wavy_scrubber")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val livePosMs = livePositionState.value
                val clampedProgress = if (currentIsDragging) {
                    currentDragFraction.coerceIn(0f, 1f)
                } else {
                    (livePosMs / validDuration.toFloat()).coerceIn(0f, 1f)
                }
                val wavePhase = (livePosMs * 0.0085f) % (2f * PI.toFloat())
                val ampTarget = when {
                    currentIsDragging -> 0.18f
                    isPlaying -> (0.35f + audioOverallProvider() * 0.75f).coerceIn(0.35f, 1.05f)
                    else -> 0f
                }
                val waveAmpFraction = ampTarget * wavePlayStateFactor

                val centerY = size.height / 2f
                val strokePx = 4.dp.toPx()
                val horizontalPad = 10.dp.toPx()
                val usableStart = horizontalPad
                val usableEnd = (size.width - horizontalPad).coerceAtLeast(usableStart + 1f)
                val usableWidth = usableEnd - usableStart
                val splitX = usableStart + usableWidth * clampedProgress

                if (splitX < usableEnd) {
                    drawLine(
                        color = trackColor,
                        start = Offset(splitX, centerY),
                        end = Offset(usableEnd, centerY),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }

                val waveMaxAmpPx = 4.dp.toPx() * waveAmpFraction
                if (splitX > usableStart + 1f) {
                    if (waveMaxAmpPx < 0.35f) {
                        drawLine(
                            color = primaryColor,
                            start = Offset(usableStart, centerY),
                            end = Offset(splitX, centerY),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                    } else {
                        wavePath.reset()
                        val wavelengthPx = 28.dp.toPx()
                        val wavelengthFactor = (2f * PI.toFloat()) / wavelengthPx
                        val stepPx = 3.dp.toPx()
                        val taperPx = 14.dp.toPx()
                        var x = usableStart
                        val startY = centerY + sin(x * wavelengthFactor - wavePhase) * waveMaxAmpPx
                        wavePath.moveTo(x, startY)
                        while (x < splitX) {
                            x = (x + stepPx).coerceAtMost(splitX)
                            val edgeTaper = ((splitX - x) / taperPx).coerceIn(0f, 1f)
                            val y = centerY + sin(x * wavelengthFactor - wavePhase) *
                                waveMaxAmpPx * edgeTaper
                            wavePath.lineTo(x, y)
                        }
                        drawPath(
                            path = wavePath,
                            color = primaryColor,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }

                val thumbPx = thumbRadiusDp.toPx()
                drawCircle(
                    color = primaryColor.copy(alpha = if (currentIsDragging) 0.26f else 0.16f),
                    radius = thumbPx * 1.7f,
                    center = Offset(splitX, centerY)
                )
                drawCircle(
                    color = primaryColor,
                    radius = thumbPx,
                    center = Offset(splitX, centerY)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = curTimeText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = labelColor,
                modifier = Modifier.testTag("player_current_time")
            )
            Text(
                text = totalTimeText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = labelColor,
                modifier = Modifier.testTag("player_total_time")
            )
        }
    }
}

@Composable
private fun NewDesignMusicControls(
    isPlaying: Boolean,
    beatPulseProvider: () -> Float,
    primaryColor: Color,
    tertiaryColor: Color,
    onPrimaryColor: Color,
    surfaceHigh: Color,
    onSurface: Color,
    compact: Boolean = false,
    onTogglePlayPause: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val prevNudgeX = remember { Animatable(0f) }
    val nextNudgeX = remember { Animatable(0f) }

    // Play (đang phát) -> Viên nhộng (rộng hơn cao); Pause (tạm dừng) -> Hình tròn
    val targetPlayWidth = when {
        isPlaying && compact -> 90.dp
        isPlaying -> 110.dp
        compact -> 54.dp
        else -> 68.dp
    }
    val targetPlayHeight = when {
        isPlaying && compact -> 54.dp
        isPlaying -> 66.dp
        compact -> 54.dp
        else -> 68.dp
    }

    val morphSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.48f,
        stiffness = Spring.StiffnessMediumLow
    )
    val playBtnWidth by animateDpAsState(
        targetValue = targetPlayWidth,
        animationSpec = morphSpec,
        label = "player_hero_width"
    )
    val playBtnHeight by animateDpAsState(
        targetValue = targetPlayHeight,
        animationSpec = morphSpec,
        label = "player_hero_height"
    )
    val heroShape = RoundedCornerShape(percent = 50)

    val skipSize = if (compact) 48.dp else 56.dp
    val boxSlotWidth = if (compact) 118.dp else 148.dp
    val boxSlotHeight = if (compact) 68.dp else 94.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 20.dp, Alignment.CenterHorizontally),
        modifier = modifier.fillMaxWidth()
    ) {
        // Nút Bài trước
        val prevSource = remember { MutableInteractionSource() }
        Surface(
            shape = CircleShape,
            color = surfaceHigh,
            contentColor = onSurface,
            tonalElevation = 2.dp,
            shadowElevation = 0.dp,
            border = null,
            modifier = Modifier
                .size(skipSize)
                .graphicsLayer { translationX = prevNudgeX.value }
                .bottomControlBounce(
                    scaleDown = 0.84f,
                    dipY = 1.5.dp,
                    interactionSource = prevSource
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = prevSource,
                    indication = null,
                    onClick = {
                        scope.launch {
                            prevNudgeX.snapTo(-16f)
                            prevNudgeX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.45f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                        onPlayPrevious()
                    }
                )
                .testTag("btn_player_previous")
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous),
                    tint = onSurface,
                    modifier = Modifier.size(if (compact) 25.dp else 28.dp)
                )
            }
        }

        // Nút Play (Viên nhộng) <-> Pause (Hình tròn) với quầng sáng blur mềm mọi hướng nhịp theo nhạc thật
        val playSource = remember { MutableInteractionSource() }
        val isPlayPressed by playSource.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
            targetValue = if (isPlayPressed) 0.90f else 1f,
            animationSpec = spring(
                dampingRatio = 0.45f,
                stiffness = Spring.StiffnessMedium
            ),
            label = "hero_play_press_scale"
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(boxSlotWidth)
                .height(boxSlotHeight)
        ) {
            if (isPlaying) {
                val maxPossibleSpread = 36.dp
                Canvas(
                    modifier = Modifier
                        .width(playBtnWidth + maxPossibleSpread * 2f)
                        .height(playBtnHeight + maxPossibleSpread * 2f)
                        .testTag("player_play_outer_halo")
                ) {
                    val pulse = beatPulseProvider().coerceIn(0f, 1f)
                    val peakAlpha = 0.30f + 0.34f * pulse
                    val haloColor = lerp(primaryColor, tertiaryColor, pulse * 0.22f)
                    val coreW = playBtnWidth.toPx()
                    val coreH = playBtnHeight.toPx()
                    val maxSpreadPx = (18.dp + 18.dp * pulse).toPx()
                    val virtualCanvasW = (coreW + maxSpreadPx * 2f).coerceAtMost(size.width)
                    val virtualCanvasH = (coreH + maxSpreadPx * 2f).coerceAtMost(size.height)
                    val steps = 22
                    for (step in steps downTo 1) {
                        val t = step.toFloat() / steps.toFloat()
                        val spread = maxSpreadPx * t
                        val layerW = (coreW + spread * 2f).coerceAtMost(virtualCanvasW)
                        val layerH = (coreH + spread * 2f).coerceAtMost(virtualCanvasH)
                        val left = (size.width - layerW) / 2f
                        val top = (size.height - layerH) / 2f
                        val corner = layerH / 2f
                        // Suy giảm Gaussian mượt ra mọi hướng và triệt tiêu về 0 trước mép Canvas (không có viền sắc)
                        val gaussian = exp(-3.8f * t * t) * (1f - t)
                        val layerAlpha = (peakAlpha * 0.22f * gaussian).coerceIn(0f, 0.22f)
                        if (layerAlpha > 0.001f) {
                            drawRoundRect(
                                color = haloColor.copy(alpha = layerAlpha),
                                topLeft = Offset(left, top),
                                size = Size(layerW, layerH),
                                cornerRadius = CornerRadius(corner, corner)
                            )
                        }
                    }
                }
            }

            Surface(
                shape = heroShape,
                color = primaryColor,
                contentColor = onPrimaryColor,
                tonalElevation = 4.dp,
                shadowElevation = 0.dp,
                border = null,
                modifier = Modifier
                    .width(playBtnWidth)
                    .height(playBtnHeight)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .clip(heroShape)
                    .clickable(
                        interactionSource = playSource,
                        indication = null,
                        onClick = onTogglePlayPause
                    )
                    .semantics {
                        stateDescription = if (isPlaying) "capsule_playing" else "circle_paused"
                    }
                    .testTag("btn_player_play_pause")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn())
                                .togetherWith(scaleOut(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut())
                        },
                        label = "hero_play_pause_icon"
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_pause),
                            tint = onPrimaryColor,
                            modifier = Modifier.size(if (compact) 30.dp else 34.dp)
                        )
                    }
                }
            }
        }

        // Nút Bài tiếp theo
        val nextSource = remember { MutableInteractionSource() }
        Surface(
            shape = CircleShape,
            color = surfaceHigh,
            contentColor = onSurface,
            tonalElevation = 2.dp,
            shadowElevation = 0.dp,
            border = null,
            modifier = Modifier
                .size(skipSize)
                .graphicsLayer { translationX = nextNudgeX.value }
                .bottomControlBounce(
                    scaleDown = 0.84f,
                    dipY = 1.5.dp,
                    interactionSource = nextSource
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = nextSource,
                    indication = null,
                    onClick = {
                        scope.launch {
                            nextNudgeX.snapTo(16f)
                            nextNudgeX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.45f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                        onPlayNext()
                    }
                )
                .testTag("btn_player_next")
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.cd_next),
                    tint = onSurface,
                    modifier = Modifier.size(if (compact) 25.dp else 28.dp)
                )
            }
        }
    }
}

/**
 * Hàng 4 nút tiện ích nằm gọn 1 hàng, cách đều nhau ngay dưới cụm điều khiển theo đúng thứ tự:
 * Quay lại • Danh sách • Giao diện • Lặp lại
 */
@Composable
private fun PlayerBottomUtilityRow(
    playMode: PlayMode,
    themeMode: ThemeMode,
    primaryColor: Color,
    tertiaryColor: Color,
    surfaceHigh: Color,
    onSurface: Color,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    onCycleThemeMode: () -> Unit,
    onCyclePlayMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modeTint by animateColorAsState(
        targetValue = when (playMode) {
            PlayMode.REPEAT_ALL -> onSurface.copy(alpha = 0.82f)
            PlayMode.REPEAT_ONE -> primaryColor
            PlayMode.SHUFFLE -> tertiaryColor
        },
        animationSpec = tween(durationMillis = 200),
        label = "player_mode_tint"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .testTag("player_bottom_utility_row"),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Quay lại
        val backSource = remember { MutableInteractionSource() }
        Surface(
            shape = CircleShape,
            color = surfaceHigh.copy(alpha = if (isDarkTheme) 0.78f else 0.86f),
            contentColor = onSurface,
            tonalElevation = 1.dp,
            shadowElevation = 0.dp,
            border = null,
            modifier = Modifier
                .size(48.dp)
                .bottomControlBounce(
                    scaleDown = 0.84f,
                    dipY = 1.dp,
                    interactionSource = backSource
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = backSource,
                    indication = null,
                    onClick = onBack
                )
                .testTag("btn_player_back")
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back_from_player),
                    tint = onSurface,
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        // 2. Danh sách bài hát đang phát
        PlayerQueueCircle(
            surfaceHigh = surfaceHigh,
            onSurface = onSurface,
            isDarkTheme = isDarkTheme,
            onOpenQueue = onOpenQueue
        )

        // 3. Đổi nhanh 3 giao diện
        PlayerQuickThemeCircle(
            themeMode = themeMode,
            primaryColor = primaryColor,
            tertiaryColor = tertiaryColor,
            surfaceHigh = surfaceHigh,
            onSurface = onSurface,
            isDarkTheme = isDarkTheme,
            onCycleThemeMode = onCycleThemeMode
        )

        // 4. Lặp lại / Phát ngẫu nhiên
        val modeSource = remember { MutableInteractionSource() }
        Surface(
            shape = CircleShape,
            color = surfaceHigh.copy(alpha = if (isDarkTheme) 0.78f else 0.86f),
            contentColor = modeTint,
            tonalElevation = 1.dp,
            shadowElevation = 0.dp,
            border = null,
            modifier = Modifier
                .size(48.dp)
                .bottomControlBounce(
                    scaleDown = 0.84f,
                    dipY = 1.dp,
                    interactionSource = modeSource
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = modeSource,
                    indication = null,
                    onClick = onCyclePlayMode
                )
                .testTag("btn_player_play_mode")
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                AnimatedPlayModeIcon(
                    playMode = playMode,
                    tint = modeTint,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerQuickThemeCircle(
    themeMode: ThemeMode,
    primaryColor: Color,
    tertiaryColor: Color,
    surfaceHigh: Color,
    onSurface: Color,
    isDarkTheme: Boolean,
    onCycleThemeMode: () -> Unit
) {
    val iconTint by animateColorAsState(
        targetValue = when (themeMode) {
            ThemeMode.SYSTEM -> onSurface.copy(alpha = 0.82f)
            ThemeMode.LIGHT -> primaryColor
            ThemeMode.DARK -> tertiaryColor
        },
        animationSpec = tween(durationMillis = 220),
        label = "player_quick_theme_tint"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        shape = CircleShape,
        color = surfaceHigh.copy(alpha = if (isDarkTheme) 0.78f else 0.86f),
        contentColor = iconTint,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        border = null,
        modifier = Modifier
            .size(48.dp)
            .bottomControlBounce(
                scaleDown = 0.84f,
                dipY = 1.dp,
                interactionSource = interactionSource
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onCycleThemeMode
            )
            .semantics {
                stateDescription = themeMode.id
            }
            .testTag("btn_player_theme_mode")
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = themeMode,
                transitionSpec = {
                    (scaleIn(
                        initialScale = 0.7f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(tween(180)))
                        .togetherWith(
                            scaleOut(targetScale = 0.7f) + fadeOut(tween(130))
                        )
                },
                label = "player_quick_theme_icon"
            ) { mode ->
                Icon(
                    imageVector = when (mode) {
                        ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                        ThemeMode.LIGHT -> Icons.Rounded.LightMode
                        ThemeMode.DARK -> Icons.Rounded.DarkMode
                    },
                    contentDescription = stringResource(R.string.cd_quick_theme_cycle),
                    tint = iconTint,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerQueueCircle(
    surfaceHigh: Color,
    onSurface: Color,
    isDarkTheme: Boolean,
    onOpenQueue: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        shape = CircleShape,
        color = surfaceHigh.copy(alpha = if (isDarkTheme) 0.78f else 0.86f),
        contentColor = onSurface,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        border = null,
        modifier = Modifier
            .size(48.dp)
            .bottomControlBounce(
                scaleDown = 0.84f,
                dipY = 1.dp,
                interactionSource = interactionSource
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onOpenQueue
            )
            .testTag("btn_player_queue")
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = stringResource(R.string.cd_open_current_queue),
                tint = onSurface.copy(alpha = 0.86f),
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

