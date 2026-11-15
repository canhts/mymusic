package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.PlaybackState
import com.example.model.Playlist
import com.example.model.Song
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
internal fun CapsuleSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderText: String,
    containerTestTag: String,
    inputTestTag: String,
    clearButtonTestTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .height(48.dp)
            .testTag(containerTestTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.weight(1f)
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholderText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(inputTestTag)
                )
            }
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.8f, animationSpec = tween(150)),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.8f, animationSpec = tween(120))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") }
                        .testTag(clearButtonTestTag)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SongList(
    songs: List<Song>,
    playlists: List<Playlist> = emptyList(),
    playbackState: PlaybackState,
    onSongClick: (Song) -> Unit,
    onCarouselPlaylistClick: (Playlist) -> Unit = {},
    onCarouselPlayPlaylist: (Playlist) -> Unit = {},
    onAddToPlaylist: (Song) -> Unit,
    onRenameSong: (Song, String) -> Unit,
    onDeleteSong: (Song) -> Unit,
    contentTopPadding: Dp,
    contentBottomPadding: Dp,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var revealedSongId by remember { mutableStateOf<Long?>(null) }
    val currentRevealedId by rememberUpdatedState(revealedSongId)
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(songs.isEmpty()) {
        if (songs.isEmpty()) {
            searchQuery = ""
        }
    }

    val filteredSongs = remember(songs, searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            songs
        } else {
            val normalizedQuery = Song.normalizeForSearch(trimmed)
            songs.filter { song ->
                matchesSongSearchQuery(song, trimmed, normalizedQuery)
            }
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            revealedSongId = null
        }
    }

    val animatedTopPadding by animateDpAsState(
        targetValue = contentTopPadding,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "song_list_top_padding"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    revealedSongId = null
                }
            }
    ) {
        val currentSongId = playbackState.currentSong?.id
        val nonEmptyPlaylists = remember(playlists) {
            playlists.filter { it.songs.isNotEmpty() }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = animatedTopPadding,
                bottom = contentBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("songs_lazy_column")
        ) {
            if (songs.isNotEmpty()) {
                item(
                    key = "songs_search_bar",
                    contentType = "search_bar"
                ) {
                    CapsuleSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholderText = stringResource(R.string.search_placeholder),
                        containerTestTag = "songs_search_bar",
                        inputTestTag = "input_search_songs",
                        clearButtonTestTag = "btn_clear_search_songs",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                    )
                }
            }

            if (nonEmptyPlaylists.isNotEmpty()) {
                item(
                    key = "songs_playlists_carousel",
                    contentType = "playlists_carousel"
                ) {
                    val carouselState = rememberCarouselState { nonEmptyPlaylists.size }
                    val preferredSquareCardSize = 186.dp

                    HorizontalMultiBrowseCarousel(
                        state = carouselState,
                        preferredItemWidth = preferredSquareCardSize,
                        itemSpacing = 8.dp,
                        flingBehavior = CarouselDefaults.multiBrowseFlingBehavior(state = carouselState),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .heightIn(max = 260.dp)
                            .testTag("songs_playlists_carousel")
                    ) { index ->
                        val playlist = nonEmptyPlaylists[index]
                        val isActiveQueue = playbackState.activePlaylistId == playlist.id
                        val isPlayingThis = isActiveQueue && playbackState.isPlaying

                        PlaylistCarouselCard(
                            playlist = playlist,
                            isActiveQueue = isActiveQueue,
                            isPlaying = isPlayingThis,
                            maskRectProvider = { carouselItemInfo.maskRect },
                            itemSizeProvider = { carouselItemInfo.size },
                            onClick = { onCarouselPlaylistClick(playlist) },
                            onPlayClick = { onCarouselPlayPlaylist(playlist) },
                            modifier = Modifier
                                .layout { measurable, constraints ->
                                    val side = constraints.maxWidth.coerceAtLeast(1)
                                    val placeable = measurable.measure(
                                        constraints.copy(
                                            minWidth = side,
                                            maxWidth = side,
                                            minHeight = side,
                                            maxHeight = side
                                        )
                                    )
                                    layout(side, side) {
                                        placeable.place(0, 0)
                                    }
                                }
                                .maskClip(MaterialTheme.shapes.extraLarge)
                        )
                    }
                }
            }

            if (songs.isNotEmpty() && filteredSongs.isEmpty()) {
                item(
                    key = "songs_no_search_results",
                    contentType = "empty_search"
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 40.dp)
                            .testTag("songs_no_search_results")
                    ) {
                        Text(
                            text = stringResource(R.string.no_search_results),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = filteredSongs,
                    key = { _, song -> song.id },
                    contentType = { _, _ -> "song_card" }
                ) { index, song ->
                    val isCurrentSong = currentSongId == song.id
                    val isFirstItem = index == 0
                    val isLastItem = index == filteredSongs.lastIndex
                    val isActionsRevealed = revealedSongId == song.id

                    val onCardClick = remember(song) {
                        {
                            if (currentRevealedId != null) {
                                revealedSongId = null
                            } else {
                                onSongClick(song)
                            }
                        }
                    }
                    val onCardAddToPlaylist = remember(song) { { onAddToPlaylist(song) } }
                    val onCardRename = remember(song) { { newName: String -> onRenameSong(song, newName) } }
                    val onCardDelete = remember(song) { { onDeleteSong(song) } }
                    val onCardToggleActions = remember(song.id) {
                        {
                            revealedSongId = if (currentRevealedId == song.id) null else song.id
                        }
                    }
                    val onCardCloseActions = remember(song.id) {
                        {
                            if (currentRevealedId == song.id) {
                                revealedSongId = null
                            }
                        }
                    }

                    UniformSongCard(
                        song = song,
                        songIndex = index + 1,
                        isHighlighted = isCurrentSong,
                        isPlaying = isCurrentSong && playbackState.isPlaying,
                        isFirst = isFirstItem,
                        isLast = isLastItem,
                        isActionsRevealed = isActionsRevealed,
                        onToggleActions = onCardToggleActions,
                        onCloseActions = onCardCloseActions,
                        onClick = onCardClick,
                        onAddToPlaylist = onCardAddToPlaylist,
                        onRename = onCardRename,
                        onDelete = onCardDelete,
                        onRemoveFromPlaylist = null,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .then(if (isLastItem) Modifier.padding(bottom = 6.dp) else Modifier)
                            .testTag("song_card_${song.id}")
                    )
                }
            }
        }
    }
}

/**
 * Thẻ Carousel thích ứng (Adaptive Card) chuẩn Material 3:
 * - Tự động đo và co giãn theo kích thước hiển thị thực tế (`maskRect` / `size`) của Carousel
 *   mà không gây Recomposition khi cuộn.
 * - Trạng thái Lớn: Đầy đủ tiêu đề, số bài hát, tổng thời lượng và nút Phát/Tạm dừng 38.dp.
 * - Trạng thái Trung bình: Mờ dần nội dung phụ, thu gọn tiêu đề vừa khít chiều rộng, thu nhỏ nút.
 * - Trạng thái Nhỏ: Giữ nút Phát/Tạm dừng thu nhỏ căn giữa mượt mà, không bao giờ cắt chữ hay tràn khung.
 */
@Composable
internal fun PlaylistCarouselCard(
    playlist: Playlist,
    isActiveQueue: Boolean,
    isPlaying: Boolean,
    maskRectProvider: () -> Rect = { Rect.Zero },
    itemSizeProvider: () -> Float,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val isLightSurface = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val containerColor = if (isActiveQueue) {
        lerp(
            MaterialTheme.colorScheme.primaryContainer,
            primaryColor,
            if (isLightSurface) 0.28f else 0.22f
        )
    } else {
        if (isLightSurface) {
            lerp(MaterialTheme.colorScheme.primaryContainer, primaryColor, 0.16f)
        } else {
            lerp(MaterialTheme.colorScheme.secondaryContainer, primaryColor, 0.18f)
        }
    }
    val titleColor = if (isActiveQueue) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val metaColor = titleColor.copy(alpha = 0.80f)
    val hasSongs = playlist.songs.isNotEmpty()
    val songCountLabel = stringResource(R.string.playlist_songs_count_format, playlist.songs.size)
    val totalDurationText = remember(playlist.totalDurationMs) {
        formatCardDuration(playlist.totalDurationMs)
    }

    val cardSource = remember { MutableInteractionSource() }
    val playBtnSource = remember { MutableInteractionSource() }
    val noteScratchPath = remember { Path() }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = titleColor,
        border = null,
        tonalElevation = if (isActiveQueue) 2.dp else 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .springPress(scaleDown = 0.96f, interactionSource = cardSource)
            .clickable(
                interactionSource = cardSource,
                indication = null,
                onClick = onClick
            )
            .testTag("carousel_playlist_card_${playlist.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val accent = if (isActiveQueue) primaryColor else tertiaryColor
                    val mask = maskRectProvider()
                    val visibleLeftPx = if (mask.width > 1f) mask.left.coerceAtLeast(0f) else 0f
                    val visibleRightPx = if (mask.width > 1f) mask.right.coerceAtMost(size.width) else size.width
                    val visibleWidthPx = (visibleRightPx - visibleLeftPx).coerceAtLeast(1f)
                    val widthDp = visibleWidthPx / density

                    // Độ đậm nhạt của hoa văn nốt nhạc nghệ thuật chìm biến thiên mượt theo kích thước thực tế của mỗi thẻ
                    val detailFactor = ((widthDp - 50f) / 106f).coerceIn(0.28f, 1f)
                    val baseNoteAlpha = (if (isActiveQueue) 0.135f else 0.095f) * (0.65f + 0.35f * detailFactor)

                    clipRect(
                        left = visibleLeftPx,
                        top = 0f,
                        right = visibleRightPx,
                        bottom = size.height
                    ) {
                        // 1. Quầng sáng nền nghệ thuật ở góc trên trái và góc dưới phải của vùng nhìn thấy
                        val glowCenter = Offset(
                            x = visibleLeftPx + visibleWidthPx * 0.24f,
                            y = size.height * 0.20f
                        )
                        val glowRadius = visibleWidthPx.coerceAtLeast(size.height) * 0.82f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accent.copy(alpha = if (isActiveQueue) 0.18f else 0.11f),
                                    Color.Transparent
                                ),
                                center = glowCenter,
                                radius = glowRadius
                            ),
                            radius = glowRadius,
                            center = glowCenter
                        )

                        // 2. Dải khuông nhạc lượn sóng thư pháp (3 đường cong Bézier mảnh chìm dưới nền)
                        drawSubmergedMusicalStaffRibbons(
                            visibleLeftPx = visibleLeftPx,
                            visibleWidthPx = visibleWidthPx,
                            cardHeightPx = size.height,
                            color = accent.copy(alpha = baseNoteAlpha * 0.46f),
                            scratchPath = noteScratchPath
                        )

                        // 3. Hoa văn nốt nhạc đôi thư pháp (♬/♫) chủ đạo với dải nối uốn lượn & vòng hào quang cộng hưởng
                        val primaryNoteScale = minOf(visibleWidthPx * 0.58f, size.height * 0.50f).coerceAtLeast(20f)
                        val primaryNoteCenter = Offset(
                            x = visibleLeftPx + visibleWidthPx * 0.68f,
                            y = size.height * 0.45f
                        )
                        drawSubmergedBeamedNotes(
                            center = primaryNoteCenter,
                            noteSize = primaryNoteScale,
                            color = accent.copy(alpha = baseNoteAlpha),
                            scratchPath = noteScratchPath,
                            rotationDegrees = -12f
                        )

                        // 4. Hoa văn nốt nhạc đơn (♪) đuôi lụa kép ở góc dưới trái khi thẻ mở rộng vừa/lớn
                        if (widthDp > 70f) {
                            val secondaryAlpha = baseNoteAlpha * 0.74f * ((widthDp - 70f) / 46f).coerceIn(0f, 1f)
                            val secondaryScale = minOf(visibleWidthPx * 0.36f, size.height * 0.34f).coerceAtLeast(15f)
                            val secondaryCenter = Offset(
                                x = visibleLeftPx + visibleWidthPx * 0.30f,
                                y = size.height * 0.64f
                            )
                            drawSubmergedSingleEighthNote(
                                center = secondaryCenter,
                                noteSize = secondaryScale,
                                color = accent.copy(alpha = secondaryAlpha),
                                scratchPath = noteScratchPath,
                                rotationDegrees = 14f
                            )
                        }

                        // 5. Nốt nhạc phụ bay bổng (♪) kèm chấm staccato tạo chiều sâu không gian khi thẻ ở trạng thái lớn
                        if (widthDp > 116f) {
                            val tertiaryAlpha = baseNoteAlpha * 0.56f * ((widthDp - 116f) / 38f).coerceIn(0f, 1f)
                            val tertiaryScale = (size.height * 0.24f).coerceAtLeast(13f)
                            val tertiaryCenter = Offset(
                                x = visibleLeftPx + visibleWidthPx * 0.46f,
                                y = size.height * 0.24f
                            )
                            drawSubmergedSingleEighthNote(
                                center = tertiaryCenter,
                                noteSize = tertiaryScale,
                                color = accent.copy(alpha = tertiaryAlpha),
                                scratchPath = noteScratchPath,
                                rotationDegrees = -9f
                            )
                        }
                    }
                }
        ) {
            Layout(
                modifier = Modifier.fillMaxSize(),
                content = {
                    // 1. Tên danh sách phát
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            lineHeight = 20.sp
                        ),
                        color = titleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .layoutId("title")
                            .testTag("carousel_playlist_title_${playlist.id}")
                    )

                    // 3 & 4. Nội dung phụ: Số lượng bài hát & Tổng thời lượng
                    Column(
                        modifier = Modifier.layoutId("meta")
                    ) {
                        Text(
                            text = songCountLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = metaColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("carousel_playlist_count_${playlist.id}")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = totalDurationText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = metaColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("carousel_playlist_duration_${playlist.id}")
                        )
                    }

                    // 2. Nút Play/Pause tròn độc lập tự co giãn theo kích thước thẻ
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .layoutId("play_btn")
                            .size(44.dp)
                            .springPress(scaleDown = 0.88f, interactionSource = playBtnSource)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = playBtnSource,
                                indication = null,
                                onClick = onPlayClick
                            )
                            .testTag("btn_carousel_play_playlist_${playlist.id}")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (hasSongs) {
                                primaryColor
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            contentColor = if (hasSongs) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.playlist_play_action),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            ) { measurables, constraints ->
                val maxW = constraints.maxWidth.coerceAtLeast(1)
                val maxH = constraints.maxHeight.coerceAtLeast(1)
                val mask = maskRectProvider()
                val rawItemSize = itemSizeProvider()

                val visibleWidthPx: Float
                val visibleLeftPx: Float
                if (mask.width > 1f) {
                    visibleWidthPx = mask.width.coerceIn(1f, maxW.toFloat())
                    visibleLeftPx = mask.left.coerceIn(0f, (maxW - visibleWidthPx).coerceAtLeast(0f))
                } else {
                    visibleWidthPx = if (rawItemSize > 1f) rawItemSize.coerceIn(1f, maxW.toFloat()) else maxW.toFloat()
                    visibleLeftPx = 0f
                }

                val densityVal = density
                val widthDp = visibleWidthPx / densityVal

                val padTopPx = 14.dp.toPx()
                val padBottomPx = 14.dp.toPx()
                val padXT = ((widthDp - 64f) / 92f).coerceIn(0f, 1f)
                val padXPx = (8f + (14f - 8f) * padXT) * densityVal
                val innerW = (visibleWidthPx - padXPx * 2f).coerceAtLeast(1f)

                // Nút Play/Pause luôn giữ nguyên kích thước 100%, thẻ càng nhỏ thì nút càng mờ dần
                val btnAlpha = ((widthDp - 40f) / 116f).coerceIn(0f, 1f)
                val playVisualPx = 38.dp.toPx()

                val titleAlpha = ((widthDp - 74f) / 28f).coerceIn(0f, 1f)
                val titleScaleT = ((widthDp - 94f) / 66f).coerceIn(0f, 1f)
                val titleScale = 0.86f + (1.0f - 0.86f) * titleScaleT
                val metaAlpha = ((widthDp - 126f) / 30f).coerceIn(0f, 1f)

                val titleMeasurable = measurables.first { it.layoutId == "title" }
                val metaMeasurable = measurables.first { it.layoutId == "meta" }
                val playMeasurable = measurables.first { it.layoutId == "play_btn" }

                val playPlaceable = playMeasurable.measure(Constraints())

                val titleMeasureMaxW = (innerW / titleScale).roundToInt().coerceAtLeast(1)
                val titleMaxH = ((maxH - padTopPx - padBottomPx - playVisualPx - 8.dp.toPx()) / titleScale)
                    .roundToInt()
                    .coerceAtLeast(1)
                val titlePlaceable = titleMeasurable.measure(
                    Constraints(maxWidth = titleMeasureMaxW, maxHeight = titleMaxH)
                )

                val metaMeasureMaxW = (innerW - playVisualPx - 8.dp.toPx()).roundToInt().coerceAtLeast(1)
                val metaMaxH = (maxH - padTopPx - padBottomPx).roundToInt().coerceAtLeast(1)
                val metaPlaceable = metaMeasurable.measure(
                    Constraints(maxWidth = metaMeasureMaxW, maxHeight = metaMaxH)
                )

                layout(maxW, maxH) {
                    if (titleAlpha > 0.01f) {
                        titlePlaceable.placeWithLayer(
                            x = (visibleLeftPx + padXPx).roundToInt(),
                            y = padTopPx.roundToInt()
                        ) {
                            alpha = titleAlpha
                            scaleX = titleScale
                            scaleY = titleScale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                    }

                    if (metaAlpha > 0.01f) {
                        metaPlaceable.placeWithLayer(
                            x = (visibleLeftPx + padXPx).roundToInt(),
                            y = (maxH - padBottomPx - metaPlaceable.height).roundToInt()
                        ) {
                            alpha = metaAlpha
                        }
                    }

                    if (btnAlpha > 0.01f) {
                        val rightEdgeX = (visibleLeftPx + visibleWidthPx - padXPx - playVisualPx / 2f)
                            .coerceAtLeast(visibleLeftPx + visibleWidthPx / 2f)
                        val bottomEdgeY = maxH - padBottomPx - playVisualPx / 2f

                        playPlaceable.placeWithLayer(
                            x = (rightEdgeX - playPlaceable.width / 2f).roundToInt(),
                            y = (bottomEdgeY - playPlaceable.height / 2f).roundToInt()
                        ) {
                            alpha = btnAlpha
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun UniformSongCard(
    song: Song,
    songIndex: Int,
    isHighlighted: Boolean,
    isPlaying: Boolean = false,
    isFirst: Boolean,
    isLast: Boolean,
    isActionsRevealed: Boolean,
    onToggleActions: () -> Unit,
    onCloseActions: () -> Unit,
    onClick: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    onRename: ((String) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    var hasEverRevealedActions by remember { mutableStateOf(false) }
    LaunchedEffect(isActionsRevealed) {
        if (isActionsRevealed) {
            hasEverRevealedActions = true
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editSongNameText by remember(song.id, song.title) { mutableStateOf(song.displayTitle) }
    val emptyDraggingState = remember { mutableStateOf(false) }
    val emptyDragFracState = remember { mutableStateOf(0f) }

    val cardBgColor = MaterialTheme.colorScheme.surfaceContainer
    val contentColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    val selectedCornerRadius = 18.dp
    val largeRadius = 24.dp
    val tinyRadius = 4.dp

    val targetRadiusTop = resolveSongCardCornerTarget(
        isSelectedOrRevealed = isActionsRevealed || isHighlighted,
        isEdge = isFirst,
        selectedRadius = selectedCornerRadius,
        edgeRadius = largeRadius,
        defaultRadius = tinyRadius
    )
    val targetRadiusBottom = resolveSongCardCornerTarget(
        isSelectedOrRevealed = isActionsRevealed || isHighlighted,
        isEdge = isLast,
        selectedRadius = selectedCornerRadius,
        edgeRadius = largeRadius,
        defaultRadius = tinyRadius
    )

    val cornerSpringSpec = remember {
        spring<Dp>(
            dampingRatio = 0.56f,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    val cornerTop by animateDpAsState(
        targetValue = targetRadiusTop,
        animationSpec = cornerSpringSpec,
        label = "corner_top"
    )
    val cornerBottom by animateDpAsState(
        targetValue = targetRadiusBottom,
        animationSpec = cornerSpringSpec,
        label = "corner_bottom"
    )

    val topR = cornerTop.coerceAtLeast(0.dp)
    val bottomR = cornerBottom.coerceAtLeast(0.dp)
    val cardShape = remember(topR, bottomR) {
        RoundedCornerShape(
            topStart = topR,
            topEnd = topR,
            bottomStart = bottomR,
            bottomEnd = bottomR
        )
    }

    val cardInteractionSource = remember { MutableInteractionSource() }
    val animScale = remember { Animatable(1f) }
    val animTranslationY = remember { Animatable(0f) }

    val density = LocalDensity.current
    val cardDipYPx = remember(density) { with(density) { 1.5.dp.toPx() } }

    var wasHighlighted by remember { mutableStateOf(isHighlighted) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted && !wasHighlighted) {
            animScale.snapTo(0.972f)
            animScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.46f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        wasHighlighted = isHighlighted
    }

    LaunchedEffect(cardInteractionSource) {
        cardInteractionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    launch {
                        animScale.animateTo(
                            targetValue = 0.962f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessHigh
                            )
                        )
                    }
                    launch {
                        animTranslationY.animateTo(
                            targetValue = cardDipYPx,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessHigh
                            )
                        )
                    }
                }
                is PressInteraction.Release -> {
                    launch {
                        if (animScale.value > 0.962f + 0.02f) {
                            animScale.animateTo(
                                targetValue = 0.962f,
                                animationSpec = tween(durationMillis = 50, easing = FastOutLinearInEasing)
                            )
                        }
                        animScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.42f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    launch {
                        if (animTranslationY.value < cardDipYPx * 0.7f) {
                            animTranslationY.animateTo(
                                targetValue = cardDipYPx,
                                animationSpec = tween(durationMillis = 50, easing = FastOutLinearInEasing)
                            )
                        }
                        animTranslationY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.42f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
                is PressInteraction.Cancel -> {
                    launch {
                        animScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    launch {
                        animTranslationY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            }
        }
    }

    val isPlaylistSingleAction = onRemoveFromPlaylist != null
    val actionButtonWidth = 42.dp
    val targetActionsWidth = if (isPlaylistSingleAction) {
        actionButtonWidth + 6.dp
    } else {
        (actionButtonWidth * 3) + 18.dp
    }

    val leftIconWidth: Dp
    val leftIconOffsetX: Dp
    val leftIconAlpha: Float
    val leftSpacerWidth: Dp
    val actionsContainerWidth: Dp
    val firstScale: Float
    val firstAlpha: Float
    val firstOffsetX: Dp
    val editScale: Float
    val editAlpha: Float
    val editOffsetX: Dp
    val deleteScale: Float
    val deleteAlpha: Float
    val deleteOffsetX: Dp

    if (hasEverRevealedActions) {
        val animLeftIconW by animateDpAsState(
            targetValue = if (isActionsRevealed) 0.dp else 22.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
            label = "left_icon_width"
        )
        val animLeftIconOffX by animateDpAsState(
            targetValue = if (isActionsRevealed) (-24).dp else 0.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
            label = "left_icon_offset"
        )
        val animLeftIconA by animateFloatAsState(
            targetValue = if (isActionsRevealed) 0f else 1f,
            animationSpec = tween(120),
            label = "left_icon_alpha"
        )
        val animLeftSpacerW by animateDpAsState(
            targetValue = if (isActionsRevealed) 0.dp else 6.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
            label = "left_spacer_width"
        )
        val animActionsW by animateDpAsState(
            targetValue = if (isActionsRevealed) targetActionsWidth else 0.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
            label = "actions_container_width"
        )
        val animFirstS by animateFloatAsState(
            targetValue = if (isActionsRevealed) 1f else 0.2f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "first_action_scale"
        )
        val animFirstA by animateFloatAsState(
            targetValue = if (isActionsRevealed) 1f else 0f,
            animationSpec = tween(120),
            label = "first_action_alpha"
        )
        val animFirstOffX by animateDpAsState(
            targetValue = if (isActionsRevealed) 0.dp else 16.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "first_action_offset"
        )
        val animEditS by animateFloatAsState(
            targetValue = if (isActionsRevealed) 1f else 0.2f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "edit_scale"
        )
        val animEditA by animateFloatAsState(
            targetValue = if (isActionsRevealed) 1f else 0f,
            animationSpec = tween(120, delayMillis = 15),
            label = "edit_alpha"
        )
        val animEditOffX by animateDpAsState(
            targetValue = if (isActionsRevealed) 0.dp else 32.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "edit_offset"
        )
        val animDelS by animateFloatAsState(
            targetValue = if (isActionsRevealed) 1f else 0.2f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "delete_scale"
        )
        val animDelA by animateFloatAsState(
            targetValue = if (isActionsRevealed) 1f else 0f,
            animationSpec = tween(120, delayMillis = 30),
            label = "delete_alpha"
        )
        val animDelOffX by animateDpAsState(
            targetValue = if (isActionsRevealed) 0.dp else 48.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "delete_offset"
        )

        leftIconWidth = animLeftIconW
        leftIconOffsetX = animLeftIconOffX
        leftIconAlpha = animLeftIconA
        leftSpacerWidth = animLeftSpacerW
        actionsContainerWidth = animActionsW
        firstScale = animFirstS
        firstAlpha = animFirstA
        firstOffsetX = animFirstOffX
        editScale = animEditS
        editAlpha = animEditA
        editOffsetX = animEditOffX
        deleteScale = animDelS
        deleteAlpha = animDelA
        deleteOffsetX = animDelOffX
    } else {
        leftIconWidth = 22.dp
        leftIconOffsetX = 0.dp
        leftIconAlpha = 1f
        leftSpacerWidth = 6.dp
        actionsContainerWidth = 0.dp
        firstScale = 0.2f
        firstAlpha = 0f
        firstOffsetX = 16.dp
        editScale = 0.2f
        editAlpha = 0f
        editOffsetX = 32.dp
        deleteScale = 0.2f
        deleteAlpha = 0f
        deleteOffsetX = 48.dp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Thẻ bài hát kích thước đồng nhất
        Surface(
            shape = cardShape,
            color = cardBgColor,
            contentColor = contentColor,
            border = null,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .heightIn(min = 52.dp)
                .graphicsLayer {
                    scaleX = animScale.value
                    scaleY = animScale.value
                    translationY = animTranslationY.value
                }
                .clip(cardShape)
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        interactionSource = cardInteractionSource,
                        indication = null,
                        onLongClick = {
                            if (!isActionsRevealed) {
                                onToggleActions()
                            }
                        },
                        onClick = {
                            if (isActionsRevealed) {
                                onCloseActions()
                            } else {
                                onClick()
                            }
                        }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leftIconWidth.value > 0.5f && leftIconAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .width(leftIconWidth)
                                .graphicsLayer {
                                    translationX = leftIconOffsetX.toPx()
                                    alpha = leftIconAlpha
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(22.dp)
                                    .testTag("song_index_${song.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isHighlighted) {
                                    AudioWaveformEqualizer(
                                        isPlaying = isPlaying,
                                        tint = primaryColor,
                                        modifier = Modifier
                                            .size(width = 16.dp, height = 14.dp)
                                            .testTag("song_equalizer_${song.id}")
                                    )
                                } else {
                                    Text(
                                        text = "$songIndex",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = contentColor.copy(alpha = 0.58f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    if (leftSpacerWidth.value > 0.5f) {
                        Spacer(modifier = Modifier.width(leftSpacerWidth))
                    }

                    Text(
                        text = song.displayTitle,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 15.sp
                        ),
                        color = if (isHighlighted) primaryColor else contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!isActionsRevealed) {
                        Spacer(modifier = Modifier.width(6.dp))
                        SongCardTimeDisplay(
                            isActive = false,
                            currentPositionMsFlow = null,
                            durationMs = song.duration,
                            isDraggingState = emptyDraggingState,
                            dragFractionState = emptyDragFracState,
                            contentColor = contentColor,
                            isDarkTheme = isDarkTheme,
                            waveSyncState = null
                        )
                    }
                }
            }
        }

        // 2. Nút thao tác viên nhộng đứng bên phải khi nhấn giữ
        if (actionsContainerWidth.value > 0.5f) {
            val capsuleShape = remember { RoundedCornerShape(percent = 50) }

            Row(
                modifier = Modifier
                    .width(actionsContainerWidth)
                    .fillMaxHeight()
                    .clipToBounds(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (isPlaylistSingleAction) {
                    Spacer(modifier = Modifier.width(6.dp))
                    if (firstAlpha > 0.02f) {
                        val removeBgColor = if (isDarkTheme) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            Color(0xFFFFEBEE)
                        }
                        val removeIconColor = if (isDarkTheme) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            Color(0xFFD32F2F)
                        }
                        val debouncedRemove = rememberDebouncedAction(debounceMs = 200L) {
                            onCloseActions()
                            onRemoveFromPlaylist?.invoke()
                        }
                        val removeSource = remember { MutableInteractionSource() }
                        Surface(
                            shape = capsuleShape,
                            color = removeBgColor,
                            contentColor = removeIconColor,
                            tonalElevation = 2.dp,
                            shadowElevation = 0.dp,
                            border = null,
                            modifier = Modifier
                                .width(actionButtonWidth)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    scaleX = firstScale
                                    scaleY = firstScale
                                    translationX = firstOffsetX.toPx()
                                    alpha = firstAlpha
                                }
                                .clip(capsuleShape)
                                .clickable(
                                    interactionSource = removeSource,
                                    indication = null,
                                    onClick = debouncedRemove
                                )
                                .springPress(scaleDown = 0.88f, interactionSource = removeSource)
                                .testTag("action_remove_from_playlist_${song.id}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RemoveCircleOutline,
                                    contentDescription = stringResource(R.string.action_remove_from_playlist),
                                    tint = removeIconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    val debouncedAddClick = rememberDebouncedAction(debounceMs = 200L) {
                        onCloseActions()
                        onAddToPlaylist?.invoke()
                    }
                    val debouncedEditClick = rememberDebouncedAction(debounceMs = 200L) {
                        onCloseActions()
                        editSongNameText = song.displayTitle
                        showEditDialog = true
                    }
                    val debouncedDeleteClick = rememberDebouncedAction(debounceMs = 240L) {
                        onCloseActions()
                        onDelete?.invoke()
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (firstAlpha > 0.02f) {
                        val addSource = remember { MutableInteractionSource() }
                        Surface(
                            shape = capsuleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            tonalElevation = 2.dp,
                            shadowElevation = 0.dp,
                            border = null,
                            modifier = Modifier
                                .width(actionButtonWidth)
                                .fillMaxHeight()
                                .then(
                                    if (isDarkTheme) {
                                        Modifier.radiantShadow(
                                            color = Color.Black.copy(alpha = 0.35f),
                                            borderRadius = 24.dp,
                                            blurRadius = 10.dp,
                                            offsetY = 2.dp
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .graphicsLayer {
                                    scaleX = firstScale
                                    scaleY = firstScale
                                    translationX = firstOffsetX.toPx()
                                    alpha = firstAlpha
                                }
                                .clip(capsuleShape)
                                .clickable(
                                    interactionSource = addSource,
                                    indication = null,
                                    onClick = debouncedAddClick
                                )
                                .springPress(scaleDown = 0.88f, interactionSource = addSource)
                                .testTag("action_add_to_playlist_${song.id}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = stringResource(R.string.action_add_to_playlist),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (editAlpha > 0.02f) {
                        val editCapsuleSource = remember { MutableInteractionSource() }
                        Surface(
                            shape = capsuleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            tonalElevation = 2.dp,
                            shadowElevation = 0.dp,
                            border = null,
                            modifier = Modifier
                                .width(actionButtonWidth)
                                .fillMaxHeight()
                                .then(
                                    if (isDarkTheme) {
                                        Modifier.radiantShadow(
                                            color = Color.Black.copy(alpha = 0.30f),
                                            borderRadius = 24.dp,
                                            blurRadius = 10.dp,
                                            offsetY = 2.dp
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .graphicsLayer {
                                    scaleX = editScale
                                    scaleY = editScale
                                    translationX = editOffsetX.toPx()
                                    alpha = editAlpha
                                }
                                .clip(capsuleShape)
                                .clickable(
                                    interactionSource = editCapsuleSource,
                                    indication = null,
                                    onClick = debouncedEditClick
                                )
                                .springPress(scaleDown = 0.88f, interactionSource = editCapsuleSource)
                                .testTag("action_edit_${song.id}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.action_edit),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (deleteAlpha > 0.02f) {
                        val deleteBgColor = if (isDarkTheme) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            Color(0xFFFFEBEE)
                        }
                        val deleteIconColor = if (isDarkTheme) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            Color(0xFFD32F2F)
                        }

                        val deleteCapsuleSource = remember { MutableInteractionSource() }
                        Surface(
                            shape = capsuleShape,
                            color = deleteBgColor,
                            contentColor = deleteIconColor,
                            tonalElevation = 2.dp,
                            shadowElevation = 0.dp,
                            border = null,
                            modifier = Modifier
                                .width(actionButtonWidth)
                                .fillMaxHeight()
                                .then(
                                    if (isDarkTheme) {
                                        Modifier.radiantShadow(
                                            color = Color.Black.copy(alpha = 0.35f),
                                            borderRadius = 24.dp,
                                            blurRadius = 10.dp,
                                            offsetY = 2.dp
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .graphicsLayer {
                                    scaleX = deleteScale
                                    scaleY = deleteScale
                                    translationX = deleteOffsetX.toPx()
                                    alpha = deleteAlpha
                                }
                                .clip(capsuleShape)
                                .clickable(
                                    interactionSource = deleteCapsuleSource,
                                    indication = null,
                                    onClick = debouncedDeleteClick
                                )
                                .springPress(scaleDown = 0.88f, interactionSource = deleteCapsuleSource)
                                .testTag("action_delete_${song.id}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = deleteIconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && onRename != null) {
        val debouncedConfirmEdit = rememberDebouncedAction(debounceMs = 240L) {
            val trimmed = editSongNameText.trim()
            if (trimmed.isNotEmpty()) {
                onRename(trimmed)
            }
            showEditDialog = false
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.edit_song_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editSongNameText,
                        onValueChange = { editSongNameText = it },
                        label = { Text(stringResource(R.string.edit_song_name_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_edit_song_name")
                    )
                }
            },
            confirmButton = {
                val editConfirmSource = remember { MutableInteractionSource() }
                Button(
                    onClick = debouncedConfirmEdit,
                    interactionSource = editConfirmSource,
                    modifier = Modifier
                        .springPress(scaleDown = 0.92f, interactionSource = editConfirmSource)
                        .testTag("btn_confirm_edit")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                val editCancelSource = remember { MutableInteractionSource() }
                TextButton(
                    onClick = { showEditDialog = false },
                    interactionSource = editCancelSource,
                    modifier = Modifier
                        .springPress(scaleDown = 0.92f, interactionSource = editCancelSource)
                        .testTag("btn_cancel_edit")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

internal fun resolveSongCardCornerTarget(
    isSelectedOrRevealed: Boolean,
    isEdge: Boolean,
    selectedRadius: Dp = 18.dp,
    edgeRadius: Dp = 24.dp,
    defaultRadius: Dp = 4.dp
): Dp = when {
    isSelectedOrRevealed -> selectedRadius
    isEdge -> edgeRadius
    else -> defaultRadius
}

/**
 * Vẽ dải khuông nhạc 3 đường cong lụa (Flowing Musical Staff Ribbons) chìm dưới nền thẻ Carousel.
 */
private fun DrawScope.drawSubmergedMusicalStaffRibbons(
    visibleLeftPx: Float,
    visibleWidthPx: Float,
    cardHeightPx: Float,
    color: Color,
    scratchPath: Path
) {
    val lineStroke = (cardHeightPx * 0.0085f).coerceAtLeast(1.0f)
    val startX = visibleLeftPx - visibleWidthPx * 0.06f
    val endX = visibleLeftPx + visibleWidthPx * 1.06f
    val spanX = endX - startX

    for (i in 0..2) {
        val offsetFactor = (i - 1) * 0.055f
        val startY = cardHeightPx * (0.68f + offsetFactor)
        val ctrl1Y = cardHeightPx * (0.46f + offsetFactor * 0.9f)
        val ctrl2Y = cardHeightPx * (0.76f + offsetFactor * 1.1f)
        val endY = cardHeightPx * (0.34f + offsetFactor)

        scratchPath.reset()
        scratchPath.moveTo(startX, startY)
        scratchPath.cubicTo(
            startX + spanX * 0.34f, ctrl1Y,
            startX + spanX * 0.66f, ctrl2Y,
            endX, endY
        )
        drawPath(
            path = scratchPath,
            color = color.copy(alpha = color.alpha * (1f - i * 0.18f)),
            style = Stroke(width = lineStroke, cap = StrokeCap.Round)
        )
    }
}

/**
 * Vẽ hoa văn nốt nhạc đôi nghệ thuật (♬ / ♫) với đầu nốt thư pháp nghiêng,
 * vòng hào quang cộng hưởng âm thanh và dải nối kép uốn cong mềm mại.
 */
private fun DrawScope.drawSubmergedBeamedNotes(
    center: Offset,
    noteSize: Float,
    color: Color,
    scratchPath: Path,
    rotationDegrees: Float = -12f
) {
    // Vòng hào quang âm thanh đồng tâm mảnh quanh cụm nốt nhạc chính
    val ringStroke = (noteSize * 0.018f).coerceAtLeast(0.9f)
    drawCircle(
        color = color.copy(alpha = color.alpha * 0.32f),
        radius = noteSize * 0.56f,
        center = center,
        style = Stroke(width = ringStroke)
    )
    drawCircle(
        color = color.copy(alpha = color.alpha * 0.18f),
        radius = noteSize * 0.74f,
        center = center,
        style = Stroke(width = ringStroke * 0.85f)
    )

    rotate(degrees = rotationDegrees, pivot = center) {
        val headWidth = noteSize * 0.31f
        val headHeight = noteSize * 0.21f
        val stemHeight = noteSize * 0.70f
        val stemStroke = (noteSize * 0.064f).coerceAtLeast(1.3f)
        val noteSpacing = noteSize * 0.46f
        val beamSlope = noteSize * 0.14f
        val beamThickness = (noteSize * 0.115f).coerceAtLeast(2.0f)

        val leftHeadCenter = Offset(
            x = center.x - noteSpacing * 0.5f,
            y = center.y + stemHeight * 0.34f
        )
        val rightHeadCenter = Offset(
            x = center.x + noteSpacing * 0.5f,
            y = leftHeadCenter.y - beamSlope
        )

        // Đầu nốt nhạc trái & phải nghiêng chuẩn thư pháp (-24 độ) kèm viền hào quang dịu
        rotate(degrees = -24f, pivot = leftHeadCenter) {
            drawOval(
                color = color.copy(alpha = color.alpha * 0.36f),
                topLeft = Offset(
                    x = leftHeadCenter.x - headWidth * 0.64f,
                    y = leftHeadCenter.y - headHeight * 0.64f
                ),
                size = Size(headWidth * 1.28f, headHeight * 1.28f)
            )
            drawOval(
                color = color,
                topLeft = Offset(
                    x = leftHeadCenter.x - headWidth * 0.5f,
                    y = leftHeadCenter.y - headHeight * 0.5f
                ),
                size = Size(headWidth, headHeight)
            )
        }
        rotate(degrees = -24f, pivot = rightHeadCenter) {
            drawOval(
                color = color.copy(alpha = color.alpha * 0.36f),
                topLeft = Offset(
                    x = rightHeadCenter.x - headWidth * 0.64f,
                    y = rightHeadCenter.y - headHeight * 0.64f
                ),
                size = Size(headWidth * 1.28f, headHeight * 1.28f)
            )
            drawOval(
                color = color,
                topLeft = Offset(
                    x = rightHeadCenter.x - headWidth * 0.5f,
                    y = rightHeadCenter.y - headHeight * 0.5f
                ),
                size = Size(headWidth, headHeight)
            )
        }

        // Thân nốt nhạc trái và phải
        val leftStemX = leftHeadCenter.x + headWidth * 0.38f
        val rightStemX = rightHeadCenter.x + headWidth * 0.38f
        val leftStemTopY = leftHeadCenter.y - stemHeight
        val rightStemTopY = rightHeadCenter.y - stemHeight

        drawLine(
            color = color,
            start = Offset(leftStemX, leftHeadCenter.y - headHeight * 0.08f),
            end = Offset(leftStemX, leftStemTopY),
            strokeWidth = stemStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(rightStemX, rightHeadCenter.y - headHeight * 0.08f),
            end = Offset(rightStemX, rightStemTopY),
            strokeWidth = stemStroke,
            cap = StrokeCap.Round
        )

        // Dải nối chính (Primary Curved Beam) uốn lượn mềm mại như dải lụa (tái sử dụng scratchPath)
        val spanX = rightStemX - leftStemX
        val curveArch = noteSize * 0.055f
        scratchPath.reset()
        scratchPath.moveTo(leftStemX - stemStroke * 0.4f, leftStemTopY)
        scratchPath.cubicTo(
            leftStemX + spanX * 0.33f, leftStemTopY - beamSlope * 0.33f - curveArch,
            leftStemX + spanX * 0.66f, leftStemTopY - beamSlope * 0.66f + curveArch * 0.6f,
            rightStemX + stemStroke * 0.4f, rightStemTopY
        )
        scratchPath.lineTo(rightStemX + stemStroke * 0.4f, rightStemTopY + beamThickness)
        scratchPath.cubicTo(
            leftStemX + spanX * 0.66f, leftStemTopY - beamSlope * 0.66f + beamThickness + curveArch * 0.6f,
            leftStemX + spanX * 0.33f, leftStemTopY - beamSlope * 0.33f + beamThickness - curveArch,
            leftStemX - stemStroke * 0.4f, leftStemTopY + beamThickness
        )
        scratchPath.close()
        drawPath(path = scratchPath, color = color)

        // Dải nối phụ (Secondary Harmonic Beam - tạo hình nốt móc kép ♬ thanh thoát)
        val secondBeamOffset = beamThickness * 1.55f
        val secondBeamStroke = (beamThickness * 0.52f).coerceAtLeast(1.2f)
        scratchPath.reset()
        scratchPath.moveTo(leftStemX, leftStemTopY + secondBeamOffset)
        scratchPath.cubicTo(
            leftStemX + spanX * 0.33f, leftStemTopY - beamSlope * 0.33f + secondBeamOffset - curveArch * 0.8f,
            leftStemX + spanX * 0.66f, leftStemTopY - beamSlope * 0.66f + secondBeamOffset + curveArch * 0.5f,
            rightStemX, rightStemTopY + secondBeamOffset
        )
        drawPath(
            path = scratchPath,
            color = color.copy(alpha = color.alpha * 0.72f),
            style = Stroke(width = secondBeamStroke, cap = StrokeCap.Round)
        )
    }
}

/**
 * Vẽ hoa văn nốt nhạc móc đơn nghệ thuật (♪) với đuôi cờ lụa kép uốn cong mềm mại.
 */
private fun DrawScope.drawSubmergedSingleEighthNote(
    center: Offset,
    noteSize: Float,
    color: Color,
    scratchPath: Path,
    rotationDegrees: Float = 10f
) {
    rotate(degrees = rotationDegrees, pivot = center) {
        val headWidth = noteSize * 0.34f
        val headHeight = noteSize * 0.23f
        val stemHeight = noteSize * 0.74f
        val stemStroke = (noteSize * 0.072f).coerceAtLeast(1.2f)

        val headCenter = Offset(
            x = center.x - noteSize * 0.08f,
            y = center.y + stemHeight * 0.32f
        )

        rotate(degrees = -24f, pivot = headCenter) {
            drawOval(
                color = color.copy(alpha = color.alpha * 0.34f),
                topLeft = Offset(
                    x = headCenter.x - headWidth * 0.64f,
                    y = headCenter.y - headHeight * 0.64f
                ),
                size = Size(headWidth * 1.28f, headHeight * 1.28f)
            )
            drawOval(
                color = color,
                topLeft = Offset(
                    x = headCenter.x - headWidth * 0.5f,
                    y = headCenter.y - headHeight * 0.5f
                ),
                size = Size(headWidth, headHeight)
            )
        }

        val stemX = headCenter.x + headWidth * 0.38f
        val stemTopY = headCenter.y - stemHeight
        drawLine(
            color = color,
            start = Offset(stemX, headCenter.y - headHeight * 0.08f),
            end = Offset(stemX, stemTopY),
            strokeWidth = stemStroke,
            cap = StrokeCap.Round
        )

        // Đuôi cờ lụa chính uốn chữ S thanh thoát (tái sử dụng scratchPath, zero allocation)
        scratchPath.reset()
        scratchPath.moveTo(stemX, stemTopY)
        scratchPath.cubicTo(
            stemX + noteSize * 0.32f, stemTopY + noteSize * 0.08f,
            stemX + noteSize * 0.38f, stemTopY + noteSize * 0.30f,
            stemX + noteSize * 0.16f, stemTopY + noteSize * 0.48f
        )
        drawPath(
            path = scratchPath,
            color = color,
            style = Stroke(width = stemStroke * 1.18f, cap = StrokeCap.Round)
        )

        // Đuôi cờ lụa phụ mảnh hơn tạo cảm giác chuyển động ngân vang
        scratchPath.reset()
        scratchPath.moveTo(stemX, stemTopY + noteSize * 0.14f)
        scratchPath.cubicTo(
            stemX + noteSize * 0.24f, stemTopY + noteSize * 0.22f,
            stemX + noteSize * 0.26f, stemTopY + noteSize * 0.38f,
            stemX + noteSize * 0.10f, stemTopY + noteSize * 0.52f
        )
        drawPath(
            path = scratchPath,
            color = color.copy(alpha = color.alpha * 0.58f),
            style = Stroke(width = stemStroke * 0.78f, cap = StrokeCap.Round)
        )

        // Chấm âm thanh (Staccato dot) nghệ thuật cạnh đầu nốt
        drawCircle(
            color = color.copy(alpha = color.alpha * 0.65f),
            radius = (noteSize * 0.045f).coerceAtLeast(1.2f),
            center = Offset(
                x = headCenter.x + headWidth * 0.82f,
                y = headCenter.y - headHeight * 0.18f
            )
        )
    }
}


