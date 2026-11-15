package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.model.PlaybackState
import com.example.model.Playlist
import com.example.model.Song
import kotlinx.coroutines.delay

internal fun matchesPlaylistSearchQuery(
    playlist: Playlist,
    rawQuery: String,
    normalizedQuery: String = Song.normalizeForSearch(rawQuery)
): Boolean {
    val trimmed = rawQuery.trim()
    if (trimmed.isEmpty()) return true
    if (playlist.name.contains(trimmed, ignoreCase = true)) return true
    if (normalizedQuery.isEmpty()) return true
    return playlist.normalizedName.contains(normalizedQuery)
}

@Composable
internal fun PlaylistsOverviewScreen(
    playlists: List<Playlist>,
    playbackState: PlaybackState,
    onCreatePlaylist: () -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onRequestDeletePlaylist: (Playlist) -> Unit,
    onReorderPlaylists: (List<Long>) -> Unit = {},
    contentTopPadding: Dp,
    contentBottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val animatedTopPadding by animateDpAsState(
        targetValue = contentTopPadding,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "playlists_top_padding"
    )

    val gridState = rememberLazyGridState()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var orderedPlaylists by remember { mutableStateOf(playlists) }
    var searchQuery by remember { mutableStateOf("") }
    var draggedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var dragStartPointerInWindow by remember { mutableStateOf(Offset.Zero) }
    var currentPointerInWindow by remember { mutableStateOf(Offset.Zero) }
    var initialSlotCenterInWindow by remember { mutableStateOf(Offset.Zero) }
    var draggedCenterInGrid by remember { mutableStateOf(Offset.Zero) }
    var pendingReorderedIds by remember { mutableStateOf<List<Long>?>(null) }

    val slotCoordsMap = remember { mutableStateMapOf<Long, LayoutCoordinates>() }
    val slotBoundsInWindow = remember { mutableStateMapOf<Long, Rect>() }
    var gridCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val latestPlaylists by rememberUpdatedState(playlists)
    val latestOnReorder by rememberUpdatedState(onReorderPlaylists)

    LaunchedEffect(playlists.isEmpty()) {
        if (playlists.isEmpty()) {
            searchQuery = ""
        }
    }

    LaunchedEffect(playlists) {
        val currentIds = playlists.map { it.id }
        val pending = pendingReorderedIds
        if (draggedPlaylistId == null) {
            if (pending == null || currentIds == pending || currentIds.toSet() != pending.toSet()) {
                orderedPlaylists = playlists
                pendingReorderedIds = null
            }
        }
    }

    val isSearching = searchQuery.isNotBlank()
    val displayedPlaylists = remember(orderedPlaylists, searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            orderedPlaylists
        } else {
            val normalizedQuery = Song.normalizeForSearch(trimmed)
            orderedPlaylists.filter { playlist ->
                matchesPlaylistSearchQuery(playlist, trimmed, normalizedQuery)
            }
        }
    }

    // Tự động cuộn mượt khi kéo thẻ vuông tới sát mép trên/dưới lưới
    LaunchedEffect(draggedPlaylistId) {
        val edgeThresholdPx = with(density) { 68.dp.toPx() }
        val scrollStepPx = with(density) { 10.dp.toPx() }
        while (draggedPlaylistId != null) {
            val layoutInfo = gridState.layoutInfo
            val topEdge = layoutInfo.viewportStartOffset + edgeThresholdPx
            val bottomEdge = layoutInfo.viewportEndOffset - edgeThresholdPx
            when {
                draggedCenterInGrid.y < topEdge && gridState.canScrollBackward -> {
                    gridState.scrollBy(-scrollStepPx)
                }
                draggedCenterInGrid.y > bottomEdge && gridState.canScrollForward -> {
                    gridState.scrollBy(scrollStepPx)
                }
            }
            delay(16L)
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = animatedTopPadding,
            bottom = contentBottomPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { gridCoords = it }
            .testTag("playlists_lazy_grid")
    ) {
        item(
            key = "playlists_header",
            span = { GridItemSpan(maxLineSpan) },
            contentType = "header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (orderedPlaylists.isNotEmpty()) {
                    CapsuleSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholderText = stringResource(R.string.search_playlists_placeholder),
                        containerTestTag = "playlists_search_bar",
                        inputTestTag = "input_search_playlists",
                        clearButtonTestTag = "btn_clear_search_playlists",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.playlists_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.playlists_count_format, orderedPlaylists.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val createSource = remember { MutableInteractionSource() }
                FilledTonalIconButton(
                    onClick = onCreatePlaylist,
                    shape = CircleShape,
                    interactionSource = createSource,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .springPress(scaleDown = 0.90f, interactionSource = createSource)
                        .testTag("btn_create_playlist")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.playlist_create_action),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        if (orderedPlaylists.isEmpty()) {
            item(
                key = "playlists_empty",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "empty"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.playlists_empty_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.playlists_empty_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    val emptyCreateSource = remember { MutableInteractionSource() }
                    FilledIconButton(
                        onClick = onCreatePlaylist,
                        shape = CircleShape,
                        interactionSource = emptyCreateSource,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .size(56.dp)
                            .springPress(scaleDown = 0.90f, interactionSource = emptyCreateSource)
                            .testTag("btn_empty_create_playlist")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.playlist_create_full_action),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        } else if (displayedPlaylists.isEmpty()) {
            item(
                key = "playlists_no_search_results",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "empty_search"
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 44.dp)
                        .testTag("playlists_no_search_results")
                ) {
                    Text(
                        text = stringResource(R.string.no_playlist_search_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(
                items = displayedPlaylists,
                key = { playlist -> playlist.id },
                contentType = { "playlist_square_card" }
            ) { playlist ->
                val isActiveQueue = playbackState.activePlaylistId == playlist.id
                val isPlayingThisPlaylist = isActiveQueue && playbackState.isPlaying
                val isDragged = draggedPlaylistId == playlist.id

                DisposableEffect(playlist.id) {
                    onDispose {
                        slotCoordsMap.remove(playlist.id)
                        slotBoundsInWindow.remove(playlist.id)
                    }
                }

                val liftScale by animateFloatAsState(
                    targetValue = if (isDragged) 1.04f else 1f,
                    animationSpec = spring(
                        dampingRatio = 0.65f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "playlist_drag_scale_${playlist.id}"
                )

                val outerModifier = if (isDragged) {
                    Modifier.zIndex(10f)
                } else {
                    Modifier
                        .zIndex(0f)
                        .animateItem(
                            placementSpec = spring(
                                dampingRatio = 0.72f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                }

                Box(
                    modifier = outerModifier
                        .onGloballyPositioned { coords ->
                            if (coords.isAttached) {
                                slotCoordsMap[playlist.id] = coords
                                slotBoundsInWindow[playlist.id] = coords.boundsInWindow()
                            }
                        }
                        .pointerInput(playlist.id, isSearching) {
                            if (isSearching) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = { startOffset ->
                                    if (draggedPlaylistId != null && draggedPlaylistId != playlist.id) {
                                        return@detectDragGesturesAfterLongPress
                                    }
                                    val coords = slotCoordsMap[playlist.id]?.takeIf { it.isAttached }
                                        ?: return@detectDragGesturesAfterLongPress
                                    val startWin = coords.localToWindow(startOffset)
                                    val slotRect = coords.boundsInWindow()
                                    slotBoundsInWindow[playlist.id] = slotRect
                                    pendingReorderedIds = null
                                    draggedPlaylistId = playlist.id
                                    dragStartPointerInWindow = startWin
                                    currentPointerInWindow = startWin
                                    initialSlotCenterInWindow = slotRect.center
                                    val gCoords = gridCoords?.takeIf { it.isAttached }
                                    draggedCenterInGrid = gCoords?.windowToLocal(startWin) ?: Offset.Zero
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, _ ->
                                    if (draggedPlaylistId != playlist.id) {
                                        return@detectDragGesturesAfterLongPress
                                    }
                                    change.consume()
                                    val coords = slotCoordsMap[playlist.id]?.takeIf { it.isAttached }
                                    val pointerWin = if (coords != null) {
                                        coords.localToWindow(change.position)
                                    } else {
                                        currentPointerInWindow
                                    }
                                    currentPointerInWindow = pointerWin
                                    val gCoords = gridCoords?.takeIf { it.isAttached }
                                    if (gCoords != null) {
                                        draggedCenterInGrid = gCoords.windowToLocal(pointerWin)
                                    }

                                    val targetId = slotBoundsInWindow.entries.firstOrNull { (id, rect) ->
                                        id != playlist.id &&
                                            rect.contains(pointerWin) &&
                                            orderedPlaylists.any { it.id == id }
                                    }?.key

                                    if (targetId != null) {
                                        val fromIdx = orderedPlaylists.indexOfFirst { it.id == playlist.id }
                                        val toIdx = orderedPlaylists.indexOfFirst { it.id == targetId }
                                        if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                                            val mutable = orderedPlaylists.toMutableList()
                                            val moved = mutable.removeAt(fromIdx)
                                            mutable.add(toIdx, moved)
                                            orderedPlaylists = mutable
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (draggedPlaylistId == playlist.id) {
                                        val finalIds = orderedPlaylists.map { it.id }
                                        pendingReorderedIds = finalIds
                                        draggedPlaylistId = null
                                        latestOnReorder(finalIds)
                                    }
                                },
                                onDragCancel = {
                                    if (draggedPlaylistId == playlist.id) {
                                        draggedPlaylistId = null
                                        pendingReorderedIds = null
                                        orderedPlaylists = latestPlaylists
                                    }
                                }
                            )
                        }
                ) {
                    PlaylistSquareCard(
                        playlist = playlist,
                        isActiveQueue = isActiveQueue,
                        isPlaying = isPlayingThisPlaylist,
                        isDragged = isDragged,
                        onClick = {
                            if (draggedPlaylistId == null) {
                                onOpenPlaylist(playlist)
                            }
                        },
                        onPlayClick = {
                            if (draggedPlaylistId == null) {
                                onPlayPlaylist(playlist)
                            }
                        },
                        onDeleteClick = {
                            if (draggedPlaylistId == null) {
                                onRequestDeletePlaylist(playlist)
                            }
                        },
                        modifier = Modifier.graphicsLayer {
                            if (isDragged) {
                                val currentSlotCenter = slotBoundsInWindow[playlist.id]?.center
                                    ?: initialSlotCenterInWindow
                                val fingerOffset = currentPointerInWindow - dragStartPointerInWindow
                                translationX = (initialSlotCenterInWindow.x - currentSlotCenter.x) + fingerOffset.x
                                translationY = (initialSlotCenterInWindow.y - currentSlotCenter.y) + fingerOffset.y
                            }
                            scaleX = liftScale
                            scaleY = liftScale
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistSquareCard(
    playlist: Playlist,
    isActiveQueue: Boolean,
    isPlaying: Boolean,
    isDragged: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = remember { RoundedCornerShape(26.dp) }
    val interactionSource = remember { MutableInteractionSource() }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val isLightSurface = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val cardContainerColor = if (isActiveQueue) {
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

    val songCountLabel = stringResource(R.string.playlist_songs_count_format, playlist.songs.size)
    val totalDurationText = remember(playlist.totalDurationMs) {
        formatCardDuration(playlist.totalDurationMs)
    }
    val totalDurationDesc = stringResource(R.string.playlist_total_duration_desc, totalDurationText)
    val hasSongs = playlist.songs.isNotEmpty()

    Surface(
        shape = cardShape,
        color = cardContainerColor,
        contentColor = titleColor,
        border = if (isDragged) BorderStroke(2.dp, primaryColor) else null,
        tonalElevation = if (isDragged) 6.dp else if (isActiveQueue) 2.dp else 0.dp,
        shadowElevation = if (isDragged) 14.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .springPress(scaleDown = 0.96f, interactionSource = interactionSource)
            .clip(cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("playlist_card_${playlist.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val accent = if (isActiveQueue) primaryColor else tertiaryColor
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = if (isActiveQueue) 0.18f else 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.16f, size.height * 0.16f),
                            radius = size.width * 0.72f
                        ),
                        radius = size.width * 0.72f,
                        center = Offset(size.width * 0.16f, size.height * 0.16f)
                    )
                    drawCircle(
                        color = accent.copy(alpha = if (isActiveQueue) 0.08f else 0.04f),
                        radius = size.width * 0.34f,
                        center = Offset(size.width * 0.92f, size.height * 0.90f)
                    )
                }
        ) {
            // Góc trên trái: Icon danh sách phát / sóng nhạc & nhãn đang phát
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 12.dp, end = 52.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = if (isActiveQueue) {
                        primaryColor.copy(alpha = 0.22f)
                    } else {
                        primaryColor.copy(alpha = 0.14f)
                    },
                    contentColor = if (isActiveQueue) {
                        primaryColor
                    } else {
                        titleColor
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("playlist_card_icon_${playlist.id}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isActiveQueue) {
                            AudioWaveformEqualizer(
                                isPlaying = isPlaying,
                                tint = primaryColor,
                                modifier = Modifier.size(width = 18.dp, height = 14.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (isActiveQueue) {
                    Surface(
                        shape = CircleShape,
                        color = primaryColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = stringResource(R.string.playlist_playing_badge),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = primaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Góc trên phải: Nút xóa danh sách (vùng chạm 48dp, hình tròn 30dp gọn gàng)
            val deleteSource = remember { MutableInteractionSource() }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .minimumInteractiveComponentSize()
                    .springPress(scaleDown = 0.88f, interactionSource = deleteSource)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = deleteSource,
                        indication = null,
                        onClick = onDeleteClick
                    )
                    .testTag("btn_delete_playlist_${playlist.id}")
            ) {
                Surface(
                    shape = CircleShape,
                    color = titleColor.copy(alpha = 0.10f),
                    contentColor = metaColor,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Tên danh sách đặt dưới sát icon danh sách và nằm phía trên nút phát
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 18.sp
                ),
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 50.dp, end = 12.dp)
                    .testTag("playlist_card_title_${playlist.id}")
            )

            // Góc dưới trái: Số bài hát và tổng thời lượng tất cả bài hát trong danh sách
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 12.dp, bottom = 12.dp, end = 54.dp)
            ) {
                Text(
                    text = songCountLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = metaColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .semantics { contentDescription = totalDurationDesc }
                        .testTag("playlist_card_duration_${playlist.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = metaColor.copy(alpha = 0.80f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = totalDurationText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = metaColor.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
            }

            // Góc dưới phải: Nút phát nhanh (vùng chạm 48dp)
            val playBtnSource = remember { MutableInteractionSource() }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 4.dp, end = 4.dp)
                    .minimumInteractiveComponentSize()
                    .springPress(scaleDown = 0.88f, interactionSource = playBtnSource)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = playBtnSource,
                        indication = null,
                        onClick = onPlayClick
                    )
                    .testTag("btn_play_playlist_${playlist.id}")
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
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlaylistDetailScreen(
    playlist: Playlist,
    playbackState: PlaybackState,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onAddSongs: () -> Unit,
    onRenamePlaylist: (String) -> Unit = {},
    onSongClick: (Song) -> Unit,
    onRemoveSongFromPlaylist: (Song) -> Unit,
    contentTopPadding: Dp,
    contentBottomPadding: Dp,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }

    var revealedSongId by remember { mutableStateOf<Long?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var isSearchExpanded by remember(playlist.id) { mutableStateOf(false) }
    var searchQuery by remember(playlist.id) { mutableStateOf("") }
    var screenRootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var searchCapsuleBoundsInRoot by remember { mutableStateOf(Rect.Zero) }

    val currentRevealedId by rememberUpdatedState(revealedSongId)
    val currentIsSearchExpanded by rememberUpdatedState(isSearchExpanded)

    val collapseSearch = remember(focusManager) {
        {
            if (isSearchExpanded || searchQuery.isNotEmpty()) {
                isSearchExpanded = false
                searchQuery = ""
                focusManager.clearFocus()
            }
        }
    }

    LaunchedEffect(playlist.songs.isEmpty()) {
        if (playlist.songs.isEmpty()) {
            isSearchExpanded = false
            searchQuery = ""
        }
    }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    BackHandler(enabled = isSearchExpanded) {
        collapseSearch()
    }

    val filteredSongs = remember(playlist.songs, searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            playlist.songs
        } else {
            val normalizedQuery = Song.normalizeForSearch(trimmed)
            playlist.songs.filter { song ->
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
        label = "playlist_detail_top_padding"
    )

    val isActiveQueue = playbackState.activePlaylistId == playlist.id
    val isPlayingThisPlaylist = isActiveQueue && playbackState.isPlaying
    val hasSongs = playlist.songs.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { screenRootCoords = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val downPos = down.position
                    val touchSlop = viewConfiguration.touchSlop
                    var maxSlopExceeded = false
                    var lastPos = downPos

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        lastPos = change.position
                        if ((lastPos - downPos).getDistance() > touchSlop) {
                            maxSlopExceeded = true
                        }
                        if (!change.pressed) {
                            if (!maxSlopExceeded) {
                                revealedSongId = null
                                if (currentIsSearchExpanded) {
                                    val rootCoords = screenRootCoords?.takeIf { it.isAttached }
                                    val tapInRoot = rootCoords?.localToRoot(lastPos) ?: lastPos
                                    if (!searchCapsuleBoundsInRoot.contains(tapInRoot)) {
                                        collapseSearch()
                                    }
                                }
                            }
                            break
                        }
                    }
                }
            }
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = animatedTopPadding,
                bottom = contentBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("playlist_detail_lazy_column")
        ) {
            item(key = "playlist_detail_header", contentType = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("btn_back_playlists")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_close),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val renameActionDesc = stringResource(R.string.cd_rename_playlist)
                        val titleAlpha by animateFloatAsState(
                            targetValue = if (isSearchExpanded) 0f else 1f,
                            animationSpec = tween(durationMillis = if (isSearchExpanded) 110 else 180),
                            label = "playlist_detail_title_alpha"
                        )

                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val maxSearchCapsuleWidth = maxWidth.coerceAtLeast(48.dp)

                            Column(
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = if (hasSongs) 52.dp else 0.dp)
                                    .heightIn(min = 48.dp)
                                    .graphicsLayer { alpha = titleAlpha }
                                    .clip(RoundedCornerShape(12.dp))
                                    .semantics { contentDescription = renameActionDesc }
                                    .clickable(enabled = !isSearchExpanded) { showRenameDialog = true }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("btn_rename_playlist")
                            ) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.playlist_songs_count_format, playlist.songs.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (hasSongs) {
                                val targetSearchWidth = if (isSearchExpanded) maxSearchCapsuleWidth else 48.dp
                                val animatedSearchWidth by animateDpAsState(
                                    targetValue = targetSearchWidth,
                                    animationSpec = spring(
                                        dampingRatio = 0.72f,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "playlist_detail_search_width"
                                )
                                val searchContainerColor by animateColorAsState(
                                    targetValue = if (isSearchExpanded) {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    },
                                    animationSpec = tween(durationMillis = 160),
                                    label = "playlist_detail_search_color"
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = searchContainerColor,
                                    tonalElevation = if (isSearchExpanded) 2.dp else 0.dp,
                                    shadowElevation = 0.dp,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .zIndex(2f)
                                        .width(animatedSearchWidth.coerceAtLeast(48.dp))
                                        .height(48.dp)
                                        .onGloballyPositioned { coords ->
                                            if (coords.isAttached) {
                                                searchCapsuleBoundsInRoot = coords.boundsInRoot()
                                            }
                                        }
                                        .clip(CircleShape)
                                        .then(
                                            if (!isSearchExpanded) {
                                                Modifier.clickable { isSearchExpanded = true }
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .testTag("btn_search_playlist_detail")
                                ) {
                                    if (!isSearchExpanded && animatedSearchWidth <= 56.dp) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Search,
                                                contentDescription = stringResource(R.string.cd_search_in_playlist),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(start = 14.dp, end = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Search,
                                                contentDescription = stringResource(R.string.cd_search_in_playlist),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                contentAlignment = Alignment.CenterStart,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (searchQuery.isEmpty()) {
                                                    Text(
                                                        text = stringResource(R.string.search_placeholder),
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                BasicTextField(
                                                    value = searchQuery,
                                                    onValueChange = { searchQuery = it },
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .focusRequester(searchFocusRequester)
                                                        .testTag("input_search_playlist_detail")
                                                )
                                            }
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .clickable { collapseSearch() }
                                                    .testTag("btn_close_search_playlist_detail")
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
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val addSongsSource = remember { MutableInteractionSource() }
                        FilledTonalIconButton(
                            onClick = onAddSongs,
                            shape = CircleShape,
                            interactionSource = addSongsSource,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .springPress(scaleDown = 0.90f, interactionSource = addSongsSource)
                                .testTag("btn_add_songs_to_playlist")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.playlist_add_songs_action),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    if (playlist.songs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onPlayAll,
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_play_playlist_detail")
                        ) {
                            Icon(
                                imageVector = if (isPlayingThisPlaylist) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPlayingThisPlaylist) {
                                    stringResource(R.string.action_pause)
                                } else {
                                    stringResource(R.string.playlist_play_action)
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            if (playlist.songs.isEmpty()) {
                item(key = "playlist_detail_empty", contentType = "empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.playlist_detail_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (filteredSongs.isEmpty()) {
                item(key = "playlist_detail_no_search_results", contentType = "empty_search") {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 44.dp)
                            .testTag("playlist_detail_no_search_results")
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
                    contentType = { _, _ -> "playlist_song_card" }
                ) { index, song ->
                    val isCurrentSong = playbackState.currentSong?.id == song.id
                    val isFirst = index == 0
                    val isLast = index == filteredSongs.lastIndex
                    val isActionsRevealed = revealedSongId == song.id

                    val onCardClick = remember(song, playlist.id) {
                        {
                            if (currentRevealedId != null) {
                                revealedSongId = null
                            } else {
                                onSongClick(song)
                            }
                        }
                    }
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
                    val onCardRemove = remember(song.id) { { onRemoveSongFromPlaylist(song) } }

                    UniformSongCard(
                        song = song,
                        songIndex = index + 1,
                        isHighlighted = isCurrentSong,
                        isPlaying = isCurrentSong && playbackState.isPlaying,
                        isFirst = isFirst,
                        isLast = isLast,
                        isActionsRevealed = isActionsRevealed,
                        onToggleActions = onCardToggleActions,
                        onCloseActions = onCardCloseActions,
                        onClick = onCardClick,
                        onRemoveFromPlaylist = onCardRemove,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isLast) Modifier.padding(bottom = 6.dp) else Modifier)
                            .testTag("playlist_song_card_${song.id}")
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        RenamePlaylistDialog(
            initialName = playlist.name,
            onConfirmRename = { newName ->
                showRenameDialog = false
                onRenamePlaylist(newName)
            },
            onDismiss = { showRenameDialog = false }
        )
    }
}
