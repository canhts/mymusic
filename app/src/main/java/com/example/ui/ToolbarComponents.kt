package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PlayMode
import com.example.model.PlaybackState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class MainTab {
    SONGS,
    PLAYLISTS,
    SING
}

/**
 * Cụm điều khiển & điều hướng 2 tầng cố định ở cạnh dưới màn hình:
 * - Vùng bao ngoài trong suốt sát 3 cạnh (trái, phải, đáy) chặn mọi thao tác chạm/vuốt lọt qua khe hở xuống danh sách.
 * - Tầng trên: Thẻ viên nhộng trình phát (Now-Playing Pill Capsule) khi có bài hát đang chọn.
 * - Tầng dưới: Cụm menu viên nhộng 3 trang (Bài hát, Danh sách, Hát) + nút Cài đặt tách rời bên phải.
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
    showReturnToActiveButton: Boolean = false,
    onReturnToActiveClick: () -> Unit = {},
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
            // TẦNG TRÊN: Viên nhộng trình phát (Now-Playing Pill Capsule) + Nút tròn "Về đoạn đang hát" bám theo bên phải
            val returnButtonProgress by animateFloatAsState(
                targetValue = if (showReturnToActiveButton && playbackState.currentSong != null) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = 0.50f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                visibilityThreshold = 0.001f,
                label = "return_to_active_btn_spring"
            )
            val returnSlotFraction = returnButtonProgress.coerceAtLeast(0f)

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
                    BoxWithConstraints(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp)
                    ) {
                        val expandedBaseWidth = maxWidth.coerceAtMost(352.dp)
                        val actionCircleSize = navHeight
                        val actionCircleGap = navGap
                        val reservedTrailingWidth = ((actionCircleSize + actionCircleGap) * returnSlotFraction)
                            .coerceAtLeast(0.dp)
                        val coordinatedPillWidth = (expandedBaseWidth - reservedTrailingWidth)
                            .coerceAtLeast(144.dp)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
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
                                onToggleCollapse = onTogglePlayerPillCollapsed,
                                expandedWidthOverrideDp = coordinatedPillWidth
                            )

                            if (showReturnToActiveButton || returnButtonProgress > 0.01f) {
                                val currentGap = (actionCircleGap * returnSlotFraction).coerceAtLeast(0.dp)
                                val currentSlotWidth = (actionCircleSize * returnSlotFraction).coerceAtLeast(0.dp)
                                Spacer(modifier = Modifier.width(currentGap))
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .width(currentSlotWidth)
                                        .height(actionCircleSize)
                                ) {
                                    val returnSource = remember { MutableInteractionSource() }
                                    val btnScale = returnButtonProgress.coerceIn(0f, 1.12f)
                                    val btnAlpha = returnButtonProgress.coerceIn(0f, 1f)
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        tonalElevation = 4.dp,
                                        shadowElevation = 0.dp,
                                        border = null,
                                        modifier = Modifier
                                            .requiredSize(actionCircleSize)
                                            .graphicsLayer {
                                                scaleX = btnScale
                                                scaleY = btnScale
                                                alpha = btnAlpha
                                            }
                                            .radiantShadow(
                                                color = if (isDarkTheme) {
                                                    shadowBaseColor
                                                } else {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                                },
                                                blurRadius = 14.dp,
                                                offsetY = 3.dp,
                                                isCircle = true
                                            )
                                            .bottomControlBounce(
                                                scaleDown = 0.84f,
                                                dipY = 2.dp,
                                                interactionSource = returnSource
                                            )
                                            .clip(CircleShape)
                                            .clickable(
                                                enabled = showReturnToActiveButton,
                                                interactionSource = returnSource,
                                                indication = null,
                                                onClick = onReturnToActiveClick
                                            )
                                            .testTag("btn_return_to_active_lyric")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MyLocation,
                                                contentDescription = stringResource(R.string.lyrics_return_to_active),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(21.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TẦNG DƯỚI: Menu viên nhộng (Bài hát / Danh sách / Hát) + Cài đặt tách rời
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
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = 0.62f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavPillTabItem(
                            label = stringResource(R.string.nav_tab_songs),
                            icon = Icons.Default.MusicNote,
                            selected = currentTab == MainTab.SONGS,
                            onClick = { onSelectTab(MainTab.SONGS) },
                            testTag = "nav_tab_songs",
                            iconTestTag = "nav_tab_icon_songs"
                        )

                        NavPillTabItem(
                            label = stringResource(R.string.nav_tab_playlists),
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            selected = currentTab == MainTab.PLAYLISTS,
                            onClick = { onSelectTab(MainTab.PLAYLISTS) },
                            testTag = "nav_tab_playlists",
                            iconTestTag = "nav_tab_icon_playlists"
                        )

                        NavPillTabItem(
                            label = stringResource(R.string.nav_tab_sing),
                            icon = Icons.Rounded.Mic,
                            selected = currentTab == MainTab.SING,
                            onClick = { onSelectTab(MainTab.SING) },
                            testTag = "nav_tab_sing",
                            iconTestTag = "nav_tab_icon_sing"
                        )
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

@Composable
private fun NavPillTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    iconTestTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bgColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nav_tab_bg_$testTag"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nav_tab_fg_$testTag"
    )

    val iconScale = remember { Animatable(1f) }
    val iconTilt = remember { Animatable(0f) }
    LaunchedEffect(selected) {
        if (selected) {
            launch {
                iconScale.snapTo(0.68f)
                iconScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.42f,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                iconTilt.snapTo(-14f)
                iconTilt.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.45f,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    Surface(
        shape = CircleShape,
        color = bgColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxHeight()
            .springPress(scaleDown = 0.90f, interactionSource = interactionSource)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.62f,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(
                    expandFrom = Alignment.Start,
                    animationSpec = spring(
                        dampingRatio = 0.60f,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + scaleIn(
                    initialScale = 0.4f,
                    animationSpec = spring(
                        dampingRatio = 0.48f,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(durationMillis = 150)),
                exit = shrinkHorizontally(
                    shrinkTowards = Alignment.Start,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + scaleOut(
                    targetScale = 0.4f,
                    animationSpec = tween(durationMillis = 110)
                ) + fadeOut(animationSpec = tween(durationMillis = 110))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                scaleX = iconScale.value
                                scaleY = iconScale.value
                                rotationZ = iconTilt.value
                            }
                            .testTag(iconTestTag)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor,
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
