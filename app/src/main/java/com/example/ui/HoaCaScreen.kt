package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.R
import com.example.model.LyricLine
import com.example.model.LyricsSource
import com.example.model.LyricsUiState
import com.example.model.OfflineModelState
import com.example.model.Song
import com.example.model.calculateLineProgress
import com.example.model.findActiveLyricLineIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive

internal const val USER_SCROLL_PAUSE_DURATION_MS = 4_000L

/**
 * Trang "Hòa ca":
 * - Tự động bóc tách lời bài hát trực tiếp từ file MP3 đang phát theo kiến trúc Lai 3 tầng + Room Cache.
 * - Header tối giản: tiêu đề bài hát bên trái (không hiện nghệ sĩ/album bên dưới) và icon trạng thái đồng bộ bên phải.
 * - Hỗ trợ kéo xuống để làm mới lời bài hát (Swipe-to-Refresh).
 * - Đánh dấu dòng đang hát bằng dấu chấm tròn cố định không gây xô lệch văn bản.
 * - Đồng bộ vị trí phát bằng đồng hồ đơn điệu `withFrameNanos` neo trực tiếp vào `currentPositionMsFlow`,
 *   loại bỏ hoàn toàn hiện tượng chạy nhanh hoặc chậm bất thường.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoaCaScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPositionMsFlow: StateFlow<Long>,
    lyricsUiState: LyricsUiState,
    offlineModelState: OfflineModelState,
    onSeekTo: (Long) -> Unit,
    onRescanLyrics: () -> Unit,
    onDownloadOfflineModel: () -> Unit,
    returnToActiveRequestTick: Int = 0,
    onReturnToActiveVisibilityChanged: (Boolean) -> Unit = {},
    contentTopPadding: Dp = 12.dp,
    contentBottomPadding: Dp = 144.dp,
    modifier: Modifier = Modifier
) {
    // Đồng hồ vị trí phát được cô lập xuống HoaCaLyricsContent để không recompose header/PullToRefresh ở 60Hz

    DisposableEffect(Unit) {
        onDispose {
            onReturnToActiveVisibilityChanged(false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = contentTopPadding)
            .testTag("sing_screen")
    ) {
        if (currentSong == null || lyricsUiState is LyricsUiState.Idle) {
            LaunchedEffect(Unit) {
                onReturnToActiveVisibilityChanged(false)
            }
            HoaCaIdleState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = contentBottomPadding, start = 28.dp, end = 28.dp)
            )
            return@Box
        }

        Column(modifier = Modifier.fillMaxSize()) {
            val headerTitle = when (lyricsUiState) {
                is LyricsUiState.Success -> lyricsUiState.songTitle.ifBlank { currentSong.displayTitle }
                is LyricsUiState.Error -> lyricsUiState.songTitle.ifBlank { currentSong.displayTitle }
                else -> currentSong.displayTitle
            }

            val sourceBadge = (lyricsUiState as? LyricsUiState.Success)?.source
            val isSynced = (lyricsUiState as? LyricsUiState.Success)?.isSynced ?: false

            HoaCaSongHeader(
                songTitle = headerTitle,
                source = sourceBadge,
                isSynced = isSynced,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            val pullRefreshState = rememberPullToRefreshState()
            val isRefreshingLyrics = lyricsUiState is LyricsUiState.Loading

            PullToRefreshBox(
                isRefreshing = isRefreshingLyrics,
                onRefresh = onRescanLyrics,
                state = pullRefreshState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullRefreshState,
                        isRefreshing = isRefreshingLyrics,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("hoaca_pull_refresh")
            ) {
                when (lyricsUiState) {
                    is LyricsUiState.Loading -> {
                        LaunchedEffect(Unit) {
                            onReturnToActiveVisibilityChanged(false)
                        }
                        HoaCaLoadingState(
                            statusText = lyricsUiState.statusText,
                            tier = lyricsUiState.tier,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = contentBottomPadding, start = 28.dp, end = 28.dp)
                        )
                    }

                    is LyricsUiState.Error -> {
                        LaunchedEffect(Unit) {
                            onReturnToActiveVisibilityChanged(false)
                        }
                        HoaCaErrorAndOfflineModelState(
                            errorState = lyricsUiState,
                            offlineModelState = offlineModelState,
                            onRetry = onRescanLyrics,
                            onDownloadOfflineModel = onDownloadOfflineModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = contentBottomPadding, start = 24.dp, end = 24.dp)
                        )
                    }

                    is LyricsUiState.Success -> {
                        HoaCaLyricsContent(
                            songId = currentSong.id,
                            isPlaying = isPlaying,
                            currentPositionMsFlow = currentPositionMsFlow,
                            lines = lyricsUiState.lines,
                            isSynced = lyricsUiState.isSynced,
                            contentBottomPadding = contentBottomPadding,
                            returnToActiveRequestTick = returnToActiveRequestTick,
                            onReturnToActiveVisibilityChanged = onReturnToActiveVisibilityChanged,
                            onSeekToLine = onSeekTo,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is LyricsUiState.Idle -> Unit
                }
            }
        }
    }
}

/**
 * Header tối giản trên trang Hòa ca:
 * - Bên trái: Chỉ hiển thị tiêu đề bài hát (tuyệt đối không hiển thị tên nghệ sĩ, album hay chú thích bên dưới).
 * - Bên phải: Chỉ hiển thị icon trạng thái đã đồng bộ lời (không chữ, không nút làm mới).
 */
@Composable
private fun HoaCaSongHeader(
    songTitle: String,
    source: LyricsSource?,
    isSynced: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.testTag("hoaca_header"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = songTitle,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
                .testTag("hoaca_song_title")
        )

        if (source != null) {
            LyricsSyncIconBadge(
                source = source,
                isSynced = isSynced
            )
        }
    }
}

@Composable
private fun LyricsSyncIconBadge(
    source: LyricsSource,
    isSynced: Boolean,
    modifier: Modifier = Modifier
) {
    if (isSynced) {
        val syncedIcon = when (source) {
            LyricsSource.ID3_TAG -> Icons.Default.LibraryMusic
            LyricsSource.LRCLIB -> Icons.Default.CloudDone
            LyricsSource.AI_OFFLINE -> Icons.Default.AutoAwesome
        }
        val cdSynced = stringResource(R.string.cd_lyrics_synced)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = modifier
                .size(32.dp)
                .semantics { contentDescription = cdSynced }
                .testTag("lyrics_source_badge")
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = syncedIcon,
                    contentDescription = cdSynced,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    } else {
        val cdUnsynced = stringResource(R.string.cd_lyrics_unsynced)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
                .size(32.dp)
                .semantics { contentDescription = cdUnsynced }
                .testTag("lyrics_unsynced_badge")
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Notes,
                    contentDescription = cdUnsynced,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

/**
 * Khung trình bày toàn bộ lời bài hát (`LazyColumn`) kèm hiệu ứng "Thanh tiến trình bằng chữ",
 * dấu chấm tròn đầu dòng không gây xô lệch chữ, Tap-to-Seek và tự động cuộn.
 */
@Composable
private fun HoaCaLyricsContent(
    songId: Long,
    isPlaying: Boolean,
    currentPositionMsFlow: StateFlow<Long>,
    lines: List<LyricLine>,
    isSynced: Boolean,
    contentBottomPadding: Dp,
    returnToActiveRequestTick: Int,
    onReturnToActiveVisibilityChanged: (Boolean) -> Unit,
    onSeekToLine: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val flowPositionMs by currentPositionMsFlow.collectAsStateWithLifecycle()
    var anchorPositionMs by remember(songId) { mutableLongStateOf(flowPositionMs) }
    var anchorNanoTime by remember(songId) { mutableLongStateOf(System.nanoTime()) }
    val livePositionState = remember(songId) { mutableLongStateOf(flowPositionMs) }

    // Neo đồng hồ đơn điệu trực tiếp vào mẫu vị trí phát thực tế từ MediaPlayer
    LaunchedEffect(flowPositionMs, songId) {
        anchorPositionMs = flowPositionMs
        anchorNanoTime = System.nanoTime()
        livePositionState.longValue = flowPositionMs
    }

    // Nội suy mượt ở tần số khung hình (60/120Hz), tự động tạm dừng khi app/tab rời trạng thái STARTED
    LaunchedEffect(isPlaying, songId, isSynced, lifecycleOwner) {
        if (isPlaying && isSynced) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                anchorPositionMs = currentPositionMsFlow.value
                anchorNanoTime = System.nanoTime()
                while (isActive) {
                    withFrameNanos { frameNanos ->
                        val elapsedSinceSync = ((frameNanos - anchorNanoTime) / 1_000_000L).coerceIn(0L, 65L)
                        livePositionState.longValue = anchorPositionMs + elapsedSinceSync
                    }
                }
            }
        }
    }

    var viewportHeightPx by remember { mutableIntStateOf(0) }
    var userScrollPauseTick by remember(songId) { mutableLongStateOf(0L) }
    var isUserScrollPaused by remember(songId) { mutableStateOf(false) }

    val activeLineIndex by remember(lines, isSynced) {
        derivedStateOf {
            if (isSynced) findActiveLyricLineIndex(lines, livePositionState.longValue) else -1
        }
    }

    val shouldShowReturnButton = isSynced && isUserScrollPaused && activeLineIndex in lines.indices
    LaunchedEffect(shouldShowReturnButton) {
        onReturnToActiveVisibilityChanged(shouldShowReturnButton)
    }

    // Xử lý yêu cầu cuộn về đoạn đang hát từ nút tròn bên phải thanh tiến trình
    LaunchedEffect(returnToActiveRequestTick) {
        if (returnToActiveRequestTick > 0) {
            isUserScrollPaused = false
            if (activeLineIndex in lines.indices) {
                val targetOffset = if (viewportHeightPx > 0) -(viewportHeightPx / 3) else -180
                runCatching {
                    listState.animateScrollToItem(
                        index = activeLineIndex,
                        scrollOffset = targetOffset
                    )
                }
            }
        }
    }

    // Khi người dùng chủ động vuốt màn hình, tạm dừng auto-scroll trong 4 giây
    LaunchedEffect(userScrollPauseTick) {
        if (userScrollPauseTick > 0L) {
            isUserScrollPaused = true
            delay(USER_SCROLL_PAUSE_DURATION_MS)
            isUserScrollPaused = false
        }
    }

    val nestedScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val isPullDownAtTop = available.y > 0f &&
                    listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                if (source == NestedScrollSource.UserInput &&
                    kotlin.math.abs(available.y) > 0.5f &&
                    !isPullDownAtTop
                ) {
                    userScrollPauseTick++
                }
                return Offset.Zero
            }
        }
    }

    // Tự động cuộn giữ câu đang hát ở khoảng 1/3 phía trên màn hình khi điểm chuyển màu tiến xuống dưới
    LaunchedEffect(activeLineIndex, isUserScrollPaused, isSynced) {
        if (isSynced && !isUserScrollPaused && activeLineIndex in lines.indices) {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val activeItemInfo = visibleItems.firstOrNull { it.index == activeLineIndex }
            val viewportEnd = layoutInfo.viewportEndOffset

            val shouldAutoScroll = if (activeItemInfo == null) {
                true
            } else {
                val itemCenter = activeItemInfo.offset + activeItemInfo.size / 2
                itemCenter > (viewportEnd * 0.48f) || activeItemInfo.offset < 0
            }

            if (shouldAutoScroll) {
                val targetOffset = if (viewportHeightPx > 0) {
                    -(viewportHeightPx / 3)
                } else {
                    -180
                }
                runCatching {
                    listState.animateScrollToItem(
                        index = activeLineIndex,
                        scrollOffset = targetOffset
                    )
                }
            }
        }
    }

    val activePrimaryColor = MaterialTheme.colorScheme.primary
    val inactiveSyncedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
    val unsyncedReadableColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
    val baseTitleMedium = MaterialTheme.typography.titleMedium
    val lyricTextStyle = remember(baseTitleMedium) {
        baseTitleMedium.copy(
            fontSize = 19.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
            lineBreak = LineBreak.Paragraph
        )
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = contentBottomPadding + 48.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewportHeightPx = it.height }
            .nestedScroll(nestedScrollConnection)
            .testTag("lyrics_lazy_column")
    ) {
        itemsIndexed(
            items = lines,
            key = { idx, line -> "lyric_${idx}_${line.startTimeMs}" },
            contentType = { _, _ -> "lyric_line" }
        ) { index, line ->
            val isRowActiveIndex = isSynced && index == activeLineIndex
            LyricLineRow(
                line = line,
                livePositionState = livePositionState,
                isSynced = isSynced,
                isRowActiveIndex = isRowActiveIndex,
                activeColor = activePrimaryColor,
                inactiveColor = if (isSynced) inactiveSyncedColor else unsyncedReadableColor,
                textStyle = lyricTextStyle,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    anchorPositionMs = line.startTimeMs
                    anchorNanoTime = System.nanoTime()
                    livePositionState.longValue = line.startTimeMs
                    onSeekToLine(line.startTimeMs)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun LyricLineRow(
    line: LyricLine,
    livePositionState: State<Long>,
    isSynced: Boolean,
    isRowActiveIndex: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    textStyle: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val progressPct by remember(line, isSynced) {
        derivedStateOf {
            if (!isSynced) {
                0
            } else {
                val p = calculateLineProgress(
                    currentPositionMs = livePositionState.value,
                    line = line,
                    calibrateLongPause = true
                )
                (p.coerceIn(0f, 1f) * 100f).toInt()
            }
        }
    }
    val isCurrentActiveLine by remember(line, isSynced, isRowActiveIndex) {
        derivedStateOf {
            if (!isSynced) {
                false
            } else if (isRowActiveIndex) {
                true
            } else {
                val p = calculateLineProgress(
                    currentPositionMs = livePositionState.value,
                    line = line,
                    calibrateLongPause = true
                )
                p > 0f && p < 1f
            }
        }
    }

    // Kích thước khung chứa dấu chấm tròn cố định (18.dp x 28.dp) để văn bản lời bài hát tuyệt đối không bị xô lệch
    val dotScale by animateFloatAsState(
        targetValue = if (isCurrentActiveLine) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.52f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "lyric_active_dot_scale"
    )

    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
            .semantics {
                stateDescription = "progress_$progressPct"
            }
            .testTag("lyric_line_${line.index}")
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .width(18.dp)
                .height(28.dp)
                .testTag("lyric_dot_slot_${line.index}")
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        scaleX = dotScale
                        scaleY = dotScale
                        alpha = dotScale.coerceIn(0f, 1f)
                    }
                    .clip(CircleShape)
                    .background(activeColor)
                    .then(
                        if (isCurrentActiveLine) {
                            Modifier.testTag("active_lyric_indicator_${line.index}")
                        } else {
                            Modifier
                        }
                    )
            )
        }

        if (isSynced) {
            ProgressLyricText(
                text = line.text,
                progressProvider = {
                    calculateLineProgress(
                        currentPositionMs = livePositionState.value,
                        line = line,
                        calibrateLongPause = true
                    )
                },
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                style = textStyle,
                modifier = Modifier
                    .weight(1f)
                    .testTag("lyric_text_${line.index}")
            )
        } else {
            Text(
                text = line.text,
                color = inactiveColor,
                style = textStyle,
                modifier = Modifier
                    .weight(1f)
                    .testTag("lyric_text_${line.index}")
            )
        }
    }
}

/**
 * Composable vẽ hiệu ứng "Thanh tiến trình bằng chữ" (Continuous Text Progress Fill)
 * chuẩn xác cho cả câu 1 dòng và câu dài nhiều dòng (Multi-line Wrap).
 */
@Composable
fun ProgressLyricText(
    text: String,
    progress: Float,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium
) {
    ProgressLyricText(
        text = text,
        progressProvider = { progress },
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        modifier = modifier,
        style = style
    )
}

@Composable
private fun ProgressLyricText(
    text: String,
    progressProvider: () -> Float,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium
) {
    var textLayoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }

    Box(modifier = modifier) {
        Text(
            text = text,
            color = inactiveColor,
            style = style,
            onTextLayout = { result ->
                textLayoutResult = result
            },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = text,
            color = activeColor,
            style = style,
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    val clampedProgress = progressProvider().coerceIn(0f, 1f)
                    if (clampedProgress <= 0f) return@drawWithContent
                    if (clampedProgress >= 1f) {
                        drawContent()
                    } else {
                        val layout = textLayoutResult
                        if (layout == null || layout.lineCount <= 1) {
                            val lineLeft = layout?.getLineLeft(0) ?: 0f
                            val lineRight = layout?.getLineRight(0) ?: size.width
                            val lineWidth = (lineRight - lineLeft).coerceAtLeast(1f)
                            clipRect(
                                left = 0f,
                                top = 0f,
                                right = (lineLeft + lineWidth * clampedProgress).coerceAtMost(size.width),
                                bottom = size.height
                            ) {
                                this@drawWithContent.drawContent()
                            }
                        } else {
                            val lineCount = layout.lineCount
                            var totalTextWidth = 0f
                            for (row in 0 until lineCount) {
                                val w = (layout.getLineRight(row) - layout.getLineLeft(row)).coerceAtLeast(0f)
                                totalTextWidth += w
                            }

                            if (totalTextWidth <= 0f) {
                                clipRect(
                                    left = 0f,
                                    top = 0f,
                                    right = size.width * clampedProgress,
                                    bottom = size.height
                                ) {
                                    this@drawWithContent.drawContent()
                                }
                            } else {
                                var remainingFillWidth = totalTextWidth * clampedProgress
                                for (row in 0 until lineCount) {
                                    if (remainingFillWidth <= 0f) break
                                    val rowLeft = layout.getLineLeft(row)
                                    val rowRight = layout.getLineRight(row)
                                    val rowWidth = (rowRight - rowLeft).coerceAtLeast(0f)
                                    val rowTop = layout.getLineTop(row)
                                    val rowBottom = layout.getLineBottom(row)

                                    if (remainingFillWidth >= rowWidth) {
                                        clipRect(
                                            left = 0f,
                                            top = rowTop,
                                            right = size.width,
                                            bottom = rowBottom
                                        ) {
                                            this@drawWithContent.drawContent()
                                        }
                                        remainingFillWidth -= rowWidth
                                    } else {
                                        clipRect(
                                            left = rowLeft,
                                            top = rowTop,
                                            right = (rowLeft + remainingFillWidth).coerceAtMost(size.width),
                                            bottom = rowBottom
                                        ) {
                                            this@drawWithContent.drawContent()
                                        }
                                        remainingFillWidth = 0f
                                    }
                                }
                            }
                        }
                    }
                }
        )
    }
}

@Composable
private fun HoaCaIdleState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
            modifier = Modifier.size(84.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("sing_screen_icon")
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.sing_page_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("sing_screen_title")
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.sing_idle_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp)
        )
    }
}

@Composable
private fun HoaCaLoadingState(
    statusText: String,
    tier: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.testTag("hoaca_loading_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Text(
                text = stringResource(R.string.lyrics_loading_tier_format, tier),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Thẻ thông báo gọn nhẹ cho phép người dùng tải gói model AI Offline khi cần dùng chế độ nhận diện offline
 * hoặc thử quét lại lời bài hát.
 */
@Composable
private fun HoaCaErrorAndOfflineModelState(
    errorState: LyricsUiState.Error,
    offlineModelState: OfflineModelState,
    onRetry: () -> Unit,
    onDownloadOfflineModel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.testTag("hoaca_error_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .testTag("card_offline_ai_model")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.lyrics_offline_model_card_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = errorState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (offlineModelState.isDownloading) {
                    val pct = (offlineModelState.downloadProgress * 100f).toInt().coerceIn(0, 100)
                    LinearProgressIndicator(
                        progress = { offlineModelState.downloadProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .testTag("progress_offline_model_download")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.lyrics_offline_model_downloading_format, pct),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    if (errorState.requiresOfflineModelDownload && !offlineModelState.isDownloaded) {
                        Button(
                            onClick = onDownloadOfflineModel,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_download_offline_ai_model")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.lyrics_offline_model_download_action,
                                    offlineModelState.modelSizeLabel
                                ),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    FilledTonalButton(
                        onClick = onRetry,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_retry_lyrics")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.lyrics_retry_action),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
