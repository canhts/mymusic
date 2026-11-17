package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PlayMode
import com.example.model.PlaybackState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class MainTab {
    SONGS,
    PLAYLISTS
}

/**
 * Cụm điều khiển & điều hướng 2 tầng cố định ở cạnh dưới màn hình:
 * - Vùng bao ngoài trong suốt sát 3 cạnh (trái, phải, đáy) chặn mọi thao tác chạm/vuốt lọt qua khe hở xuống danh sách.
 * - Tầng trên: Thẻ viên nhộng trình phát (Now-Playing Pill Capsule) khi có bài hát đang chọn.
 * - Tầng dưới: Cụm menu viên nhộng 2 trang (Bài hát, Danh sách) + nút Cài đặt tách rời bên phải.
 */
@Composable
internal fun CapsuleBottomToolbar(
    currentTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    playbackState: PlaybackState,
    activePlaylistName: String? = null,
    currentPositionMsFlow: StateFlow<Long>,
    isDarkTheme: Boolean,
    showSettingsPopup: Boolean = false,
    isPlayerPillCollapsed: Boolean = false,
    onTogglePlayerPillCollapsed: (() -> Unit)? = null,
    bottomInsetPadding: Dp = 0.dp,
    onCyclePlayMode: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navHeight = 52.dp
    val navGap = 10.dp

    val shadowBaseColor = if (isDarkTheme) {
        Color.Black.copy(alpha = 0.54f)
    } else {
        Color.Black.copy(alpha = 0.16f)
    }
    val navContainerColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    val debouncedOpenSettings = rememberDebouncedAction(debounceMs = 200L, onClick = onOpenSettings)

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .fillMaxWidth()
            .testTag("bottom_control_zone")
    ) {
        // Lớp nền chặn thao tác chạm/vuốt lọt qua khe hở xuống danh sách phía sau
        // Đặt làm sibling phía dưới Column để không hủy cử chỉ kéo ngang của thanh tiến trình
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (!down.isConsumed) {
                            down.consume()
                        }
                        do {
                            val event = awaitPointerEvent(pass = PointerEventPass.Main)
                            event.changes.forEach { change ->
                                if (!change.isConsumed) {
                                    change.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 10.dp,
                    bottom = bottomInsetPadding + 14.dp
                )
        ) {
            // TẦNG TRÊN: Viên nhộng trình phát (Now-Playing Pill Capsule)
            AnimatedVisibility(
                visible = playbackState.currentSong != null,
                enter = fadeIn(animationSpec = tween(220)) +
                    slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) +
                    scaleIn(initialScale = 0.94f),
                exit = fadeOut(animationSpec = tween(180)) +
                    slideOutVertically(targetOffsetY = { it / 2 }) +
                    scaleOut(targetScale = 0.94f)
            ) {
                playbackState.currentSong?.let { activeSong ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp)
                    ) {
                        BottomNowPlayingPill(
                            song = activeSong,
                            activePlaylistName = activePlaylistName,
                            isPlaying = playbackState.isPlaying,
                            playMode = playbackState.playMode,
                            durationMs = playbackState.durationMs,
                            currentPositionMsFlow = currentPositionMsFlow,
                            onTogglePlayPause = onTogglePlayPause,
                            onCyclePlayMode = onCyclePlayMode,
                            onSeek = onSeek,
                            onOpenQueue = onOpenQueue,
                            isDarkTheme = isDarkTheme,
                            isCollapsed = if (onTogglePlayerPillCollapsed != null) isPlayerPillCollapsed else null,
                            onToggleCollapse = onTogglePlayerPillCollapsed
                        )
                    }
                }
            }

            // TẦNG DƯỚI: Menu viên nhộng (Bài hát / Danh sách) + Cài đặt tách rời
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(navHeight),
                horizontalArrangement = Arrangement.spacedBy(navGap, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = navContainerColor,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 4.dp,
                    shadowElevation = 0.dp,
                    border = null,
                    modifier = Modifier
                        .height(navHeight)
                        .radiantShadow(
                            color = shadowBaseColor,
                            borderRadius = 26.dp,
                            blurRadius = 14.dp,
                            offsetY = 3.dp
                        )
                        .testTag("nav_pill_menu")
                ) {
                    val tabBounds = remember { mutableStateMapOf<MainTab, NavTabBounds>() }
                    val indicatorLeftAnim = remember { Animatable(0f) }
                    val indicatorWidthAnim = remember { Animatable(0f) }
                    val pillJourney = remember { NavPillJourneyState() }
                    var hasInitializedIndicator by remember { mutableStateOf(false) }

                    val activeTabBounds = tabBounds[currentTab]
                    LaunchedEffect(currentTab, activeTabBounds != null) {
                        val target = activeTabBounds ?: return@LaunchedEffect
                        if (!hasInitializedIndicator) {
                            indicatorLeftAnim.snapTo(target.leftPx)
                            indicatorWidthAnim.snapTo(target.widthPx)
                            pillJourney.lastTab = currentTab
                            pillJourney.startLeftPx = target.leftPx
                            pillJourney.targetLeftPx = target.leftPx
                            pillJourney.maxProgress = 1f
                            pillJourney.isActive = false
                            hasInitializedIndicator = true
                        }
                    }

                    LaunchedEffect(currentTab, activeTabBounds?.leftPx, hasInitializedIndicator) {
                        if (!hasInitializedIndicator) return@LaunchedEffect
                        val targetLeft = activeTabBounds?.leftPx ?: return@LaunchedEffect
                        if (pillJourney.lastTab != currentTab) {
                            pillJourney.lastTab = currentTab
                            pillJourney.startLeftPx = indicatorLeftAnim.value
                            pillJourney.targetLeftPx = targetLeft
                            pillJourney.maxProgress = 0f
                            pillJourney.isActive = abs(targetLeft - pillJourney.startLeftPx) > 1f
                        } else if (pillJourney.isActive) {
                            pillJourney.targetLeftPx = targetLeft
                        }
                        indicatorLeftAnim.animateTo(
                            targetValue = targetLeft,
                            initialVelocity = indicatorLeftAnim.velocity,
                            animationSpec = spring(
                                dampingRatio = NAV_PILL_SPRING_DAMPING,
                                stiffness = NAV_PILL_SPRING_STIFFNESS
                            )
                        )
                        pillJourney.isActive = false
                    }

                    LaunchedEffect(currentTab, activeTabBounds?.widthPx, hasInitializedIndicator) {
                        if (!hasInitializedIndicator) return@LaunchedEffect
                        val targetWidth = activeTabBounds?.widthPx ?: return@LaunchedEffect
                        indicatorWidthAnim.animateTo(
                            targetValue = targetWidth,
                            initialVelocity = indicatorWidthAnim.velocity,
                            animationSpec = spring(
                                dampingRatio = NAV_PILL_SPRING_DAMPING,
                                stiffness = NAV_PILL_SPRING_STIFFNESS
                            )
                        )
                    }

                    val updateTabBounds: (MainTab, Float, Float) -> Unit = remember {
                        { tab, leftPx, widthPx ->
                            val prev = tabBounds[tab]
                            if (prev == null ||
                                abs(prev.leftPx - leftPx) > 0.25f ||
                                abs(prev.widthPx - widthPx) > 0.25f
                            ) {
                                tabBounds[tab] = NavTabBounds(leftPx = leftPx, widthPx = widthPx)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(4.dp)
                    ) {
                        // Lớp nền viên nhộng màu đậm trượt qua lại giữa các trang với độ nảy đàn hồi
                        Box(
                            modifier = Modifier.matchParentSize()
                        ) {
                            val activePillColor = MaterialTheme.colorScheme.primary
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .layout { measurable, constraints ->
                                        val containerW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                                        val rawLeft = if (hasInitializedIndicator) {
                                            indicatorLeftAnim.value
                                        } else {
                                            activeTabBounds?.leftPx ?: 0f
                                        }
                                        val rawWidth = if (hasInitializedIndicator) {
                                            indicatorWidthAnim.value
                                        } else {
                                            activeTabBounds?.widthPx ?: 0f
                                        }
                                        if (rawWidth < 1f) {
                                            val empty = measurable.measure(Constraints.fixed(0, 0))
                                            layout(0, 0) { empty.place(0, 0) }
                                        } else {
                                            val clampedBaseWidth = rawWidth.coerceAtMost(containerW)
                                            val leftOvershoot = (-rawLeft).coerceAtLeast(0f)
                                            val rightOvershoot = (rawLeft + clampedBaseWidth - containerW).coerceAtLeast(0f)
                                            val wallBleedPx = 1.5.dp.toPx()

                                            val visualWidth: Float
                                            val visualLeft: Float
                                            when {
                                                leftOvershoot > 0f -> {
                                                    visualWidth = (clampedBaseWidth - leftOvershoot * 0.35f)
                                                        .coerceAtLeast(clampedBaseWidth * 0.84f)
                                                    visualLeft = (-leftOvershoot * 0.18f).coerceAtLeast(-wallBleedPx)
                                                }
                                                rightOvershoot > 0f -> {
                                                    visualWidth = (clampedBaseWidth - rightOvershoot * 0.35f)
                                                        .coerceAtLeast(clampedBaseWidth * 0.84f)
                                                    val maxRight = containerW + (rightOvershoot * 0.18f).coerceAtMost(wallBleedPx)
                                                    visualLeft = (maxRight - visualWidth).coerceAtLeast(0f)
                                                }
                                                else -> {
                                                    visualWidth = clampedBaseWidth
                                                    visualLeft = rawLeft.coerceIn(0f, (containerW - visualWidth).coerceAtLeast(0f))
                                                }
                                            }

                                            val widthPx = visualWidth.roundToInt().coerceAtLeast(1)
                                            val placeable = measurable.measure(
                                                Constraints.fixed(
                                                    width = widthPx,
                                                    height = constraints.maxHeight.coerceAtLeast(1)
                                                )
                                            )
                                            layout(constraints.maxWidth, constraints.maxHeight) {
                                                placeable.place(x = visualLeft.roundToInt(), y = 0)
                                            }
                                        }
                                    }
                                    .graphicsLayer {
                                        val pillScale = if (hasInitializedIndicator && pillJourney.isActive) {
                                            val span = pillJourney.targetLeftPx - pillJourney.startLeftPx
                                            if (abs(span) > 1f) {
                                                val rawProgress = ((indicatorLeftAnim.value - pillJourney.startLeftPx) / span)
                                                    .coerceIn(0f, 1f)
                                                val progress = maxOf(pillJourney.maxProgress, rawProgress)
                                                if (progress > pillJourney.maxProgress) {
                                                    pillJourney.maxProgress = progress
                                                }
                                                1f + NAV_PILL_TRAVEL_PEAK_SCALE_BOOST * sin(PI * progress).toFloat()
                                            } else {
                                                1f
                                            }
                                        } else {
                                            1f
                                        }
                                        scaleX = pillScale
                                        scaleY = pillScale
                                    }
                                    .background(
                                        color = activePillColor,
                                        shape = CircleShape
                                    )
                                    .semantics { stateDescription = currentTab.name }
                                    .testTag("nav_pill_active_indicator")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxHeight(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NavPillTabItem(
                                tab = MainTab.SONGS,
                                currentTab = currentTab,
                                label = stringResource(R.string.nav_tab_songs),
                                icon = Icons.Default.MusicNote,
                                selected = currentTab == MainTab.SONGS,
                                tabBoundsProvider = { tabBounds[MainTab.SONGS] },
                                indicatorLeftProvider = { indicatorLeftAnim.value },
                                indicatorWidthProvider = { indicatorWidthAnim.value },
                                isIndicatorInitializedProvider = { hasInitializedIndicator },
                                onBoundsChanged = { leftPx, widthPx ->
                                    updateTabBounds(MainTab.SONGS, leftPx, widthPx)
                                },
                                onClick = { onSelectTab(MainTab.SONGS) },
                                testTag = "nav_tab_songs",
                                iconTestTag = "nav_tab_icon_songs"
                            )

                            NavPillTabItem(
                                tab = MainTab.PLAYLISTS,
                                currentTab = currentTab,
                                label = stringResource(R.string.nav_tab_playlists),
                                icon = Icons.AutoMirrored.Filled.QueueMusic,
                                selected = currentTab == MainTab.PLAYLISTS,
                                tabBoundsProvider = { tabBounds[MainTab.PLAYLISTS] },
                                indicatorLeftProvider = { indicatorLeftAnim.value },
                                indicatorWidthProvider = { indicatorWidthAnim.value },
                                isIndicatorInitializedProvider = { hasInitializedIndicator },
                                onBoundsChanged = { leftPx, widthPx ->
                                    updateTabBounds(MainTab.PLAYLISTS, leftPx, widthPx)
                                },
                                onClick = { onSelectTab(MainTab.PLAYLISTS) },
                                testTag = "nav_tab_playlists",
                                iconTestTag = "nav_tab_icon_playlists"
                            )
                        }
                    }
                }

                // Nút Cài đặt tách biệt bên phải
                val settingsSource = remember { MutableInteractionSource() }
                val isSettingsPressed by settingsSource.collectIsPressedAsState()
                val settingsRotation by animateFloatAsState(
                    targetValue = if (isSettingsPressed || showSettingsPopup) 45f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "settings_rotation"
                )

                Surface(
                    shape = CircleShape,
                    color = if (showSettingsPopup) MaterialTheme.colorScheme.primary else navContainerColor,
                    contentColor = if (showSettingsPopup) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 4.dp,
                    shadowElevation = 0.dp,
                    border = null,
                    modifier = Modifier
                        .size(navHeight)
                        .radiantShadow(
                            color = if (isDarkTheme) {
                                shadowBaseColor
                            } else {
                                if (showSettingsPopup) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else shadowBaseColor
                            },
                            blurRadius = 14.dp,
                            offsetY = 3.dp,
                            isCircle = true
                        )
                        .bottomControlBounce(
                            scaleDown = 0.84f,
                            dipY = 2.dp,
                            interactionSource = settingsSource
                        )
                        .clickable(
                            enabled = true,
                            interactionSource = settingsSource,
                            indication = null,
                            onClick = debouncedOpenSettings
                        )
                        .testTag("btn_settings")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = settingsRotation }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            tint = if (showSettingsPopup) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
    }
}

private const val NAV_PILL_SPRING_DAMPING = 0.52f
private const val NAV_PILL_SPRING_STIFFNESS = Spring.StiffnessMediumLow
private const val NAV_PILL_TRAVEL_PEAK_SCALE_BOOST = 0.12f

private class NavPillJourneyState {
    var lastTab: MainTab? = null
    var startLeftPx: Float = 0f
    var targetLeftPx: Float = 0f
    var maxProgress: Float = 0f
    var isActive: Boolean = false
}

private data class NavTabBounds(
    val leftPx: Float,
    val widthPx: Float
)

@Composable
private fun NavPillTabItem(
    tab: MainTab,
    currentTab: MainTab,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    tabBoundsProvider: () -> NavTabBounds?,
    indicatorLeftProvider: () -> Float,
    indicatorWidthProvider: () -> Float,
    isIndicatorInitializedProvider: () -> Boolean,
    onBoundsChanged: (leftPx: Float, widthPx: Float) -> Unit,
    onClick: () -> Unit,
    testTag: String,
    iconTestTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val activeFgColor = MaterialTheme.colorScheme.onPrimary
    val inactiveFgColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    val iconPainter = rememberVectorPainter(image = icon)

    var lastObservedTab by remember { mutableStateOf(currentTab) }
    var tapPulseTick by remember { mutableStateOf(0) }
    val tabContentBounceScale = remember { Animatable(1f) }
    LaunchedEffect(currentTab, tapPulseTick) {
        val tabChangedToThis = lastObservedTab != currentTab && currentTab == tab
        val isRetapOnSelected = lastObservedTab == currentTab && currentTab == tab && tapPulseTick > 0
        lastObservedTab = currentTab

        if (tabChangedToThis || isRetapOnSelected) {
            // Phóng to tên + icon ra một chút rồi thu hồi về kích thước ban đầu với độ nảy đàn hồi
            tabContentBounceScale.animateTo(
                targetValue = 1.18f,
                animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing)
            )
            tabContentBounceScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.36f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else if (tabContentBounceScale.value != 1f) {
            tabContentBounceScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    // Đọc độ phủ của viên nhộng đậm chỉ trong pha Vẽ (Draw Phase) -> 0 Recomposition khi trượt 120Hz
    val pillCoverageState = remember(selected) {
        derivedStateOf {
            val initialized = isIndicatorInitializedProvider()
            val bounds = tabBoundsProvider()
            val pWidth = indicatorWidthProvider()
            if (!initialized || bounds == null || bounds.widthPx <= 1f || pWidth <= 1f) {
                if (selected) 1f else 0f
            } else {
                val tLeft = bounds.leftPx
                val tRight = tLeft + bounds.widthPx
                val pLeft = indicatorLeftProvider()
                val pRight = pLeft + pWidth
                val overlapPx = (minOf(tRight, pRight) - maxOf(tLeft, pLeft)).coerceAtLeast(0f)
                val raw = (overlapPx / bounds.widthPx).coerceIn(0f, 1f)
                raw * raw * (3f - 2f * raw)
            }
        }
    }
    val textColorProducer = remember(inactiveFgColor, activeFgColor, pillCoverageState) {
        ColorProducer { lerp(inactiveFgColor, activeFgColor, pillCoverageState.value) }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxHeight()
            .onPlaced { coords ->
                onBoundsChanged(coords.positionInParent().x, coords.size.width.toFloat())
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (currentTab == tab) {
                        tapPulseTick++
                    }
                    onClick()
                }
            )
            .testTag(testTag)
            .padding(horizontal = 12.dp)
    ) {
        // Cụm Icon + Chữ đứng tại chỗ (translationX = 0f), phóng to nhẹ khi chọn rồi đàn hồi về kích thước gốc
        Row(
            modifier = Modifier
                .height(24.dp)
                .graphicsLayer {
                    val bounceScale = tabContentBounceScale.value
                    translationX = 0f
                    scaleX = bounceScale
                    scaleY = bounceScale
                }
                .testTag("${testTag}_content"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(
                    expandFrom = Alignment.Start,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 140)),
                exit = shrinkHorizontally(
                    shrinkTowards = Alignment.Start,
                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 110))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(
                        modifier = Modifier
                            .size(18.dp)
                            .testTag(iconTestTag)
                    ) {
                        val tintColor = lerp(inactiveFgColor, activeFgColor, pillCoverageState.value)
                        with(iconPainter) {
                            draw(
                                size = size,
                                colorFilter = ColorFilter.tint(tintColor)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                }
            }

            BasicText(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = textColorProducer,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun AnimatedPlayModeIcon(
    playMode: PlayMode,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val path1 = remember { Path() }
    val path2 = remember { Path() }
    val path3 = remember { Path() }
    val path4 = remember { Path() }

    AnimatedContent(
        targetState = playMode,
        transitionSpec = {
            (scaleIn(spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow)) +
                fadeIn(animationSpec = tween(160)))
                .togetherWith(
                    scaleOut(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) +
                        fadeOut(animationSpec = tween(120))
                )
        },
        label = "play_mode_icon_switch",
        modifier = modifier
    ) { mode ->
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val trackStroke = 1.9.dp.toPx()

            when (mode) {
                PlayMode.REPEAT_ALL -> {
                    path1.reset()
                    path1.apply {
                        moveTo(w * 0.22f, h * 0.46f)
                        cubicTo(w * 0.22f, h * 0.28f, w * 0.32f, h * 0.26f, w * 0.44f, h * 0.26f)
                        lineTo(w * 0.68f, h * 0.26f)
                    }
                    drawPath(
                        path = path1,
                        color = tint,
                        style = Stroke(width = trackStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    path2.reset()
                    path2.apply {
                        moveTo(w * 0.82f, h * 0.26f)
                        lineTo(w * 0.65f, h * 0.14f)
                        lineTo(w * 0.65f, h * 0.38f)
                        close()
                    }
                    drawPath(path2, color = tint)

                    path3.reset()
                    path3.apply {
                        moveTo(w * 0.78f, h * 0.54f)
                        cubicTo(w * 0.78f, h * 0.72f, w * 0.68f, h * 0.74f, w * 0.56f, h * 0.74f)
                        lineTo(w * 0.32f, h * 0.74f)
                    }
                    drawPath(
                        path = path3,
                        color = tint,
                        style = Stroke(width = trackStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    path4.reset()
                    path4.apply {
                        moveTo(w * 0.18f, h * 0.74f)
                        lineTo(w * 0.35f, h * 0.62f)
                        lineTo(w * 0.35f, h * 0.86f)
                        close()
                    }
                    drawPath(path4, color = tint)
                }

                PlayMode.REPEAT_ONE -> {
                    path1.reset()
                    path1.apply {
                        moveTo(w * 0.22f, h * 0.46f)
                        cubicTo(w * 0.22f, h * 0.28f, w * 0.32f, h * 0.26f, w * 0.44f, h * 0.26f)
                        lineTo(w * 0.68f, h * 0.26f)
                    }
                    drawPath(
                        path = path1,
                        color = tint,
                        style = Stroke(width = trackStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    path2.reset()
                    path2.apply {
                        moveTo(w * 0.82f, h * 0.26f)
                        lineTo(w * 0.65f, h * 0.14f)
                        lineTo(w * 0.65f, h * 0.38f)
                        close()
                    }
                    drawPath(path2, color = tint)

                    path3.reset()
                    path3.apply {
                        moveTo(w * 0.78f, h * 0.54f)
                        cubicTo(w * 0.78f, h * 0.72f, w * 0.68f, h * 0.74f, w * 0.56f, h * 0.74f)
                        lineTo(w * 0.32f, h * 0.74f)
                    }
                    drawPath(
                        path = path3,
                        color = tint,
                        style = Stroke(width = trackStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    path4.reset()
                    path4.apply {
                        moveTo(w * 0.18f, h * 0.74f)
                        lineTo(w * 0.35f, h * 0.62f)
                        lineTo(w * 0.35f, h * 0.86f)
                        close()
                    }
                    drawPath(path4, color = tint)

                    val numH = 2.2.dp.toPx()
                    val numStroke = 1.5.dp.toPx()
                    val posX = cx + 0.3.dp.toPx()
                    drawLine(
                        color = tint,
                        start = Offset(posX, cy - numH),
                        end = Offset(posX, cy + numH),
                        strokeWidth = numStroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = tint,
                        start = Offset(posX - 1.5.dp.toPx(), cy - numH * 0.35f),
                        end = Offset(posX, cy - numH),
                        strokeWidth = numStroke,
                        cap = StrokeCap.Round
                    )
                }

                PlayMode.SHUFFLE -> {
                    path1.reset()
                    path1.apply {
                        moveTo(w * 0.18f, h * 0.30f)
                        lineTo(w * 0.30f, h * 0.30f)
                        cubicTo(w * 0.48f, h * 0.30f, w * 0.52f, h * 0.70f, w * 0.70f, h * 0.70f)
                    }
                    drawPath(
                        path = path1,
                        color = tint,
                        style = Stroke(width = trackStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    path2.reset()
                    path2.apply {
                        moveTo(w * 0.83f, h * 0.70f)
                        lineTo(w * 0.66f, h * 0.57f)
                        lineTo(w * 0.66f, h * 0.83f)
                        close()
                    }
                    drawPath(path2, color = tint)

                    path3.reset()
                    path3.apply {
                        moveTo(w * 0.18f, h * 0.70f)
                        lineTo(w * 0.30f, h * 0.70f)
                        cubicTo(w * 0.48f, h * 0.70f, w * 0.52f, h * 0.30f, w * 0.70f, h * 0.30f)
                    }
                    drawPath(
                        path = path3,
                        color = tint,
                        style = Stroke(width = trackStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    path4.reset()
                    path4.apply {
                        moveTo(w * 0.83f, h * 0.30f)
                        lineTo(w * 0.66f, h * 0.17f)
                        lineTo(w * 0.66f, h * 0.43f)
                        close()
                    }
                    drawPath(path4, color = tint)
                }
            }
        }
    }
}
