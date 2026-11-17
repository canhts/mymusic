package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.model.PlaybackState
import com.example.model.Playlist
import com.example.model.Song
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val quickReturnState = rememberQuickReturnHeaderState(
        defaultHeaderHeightDp = 56.dp,
        topPaddingDp = contentTopPadding,
        canScrollForwardProvider = { gridState.canScrollForward },
        canScrollBackwardProvider = { gridState.canScrollBackward }
    )
    val canScrollGrid by remember(gridState) {
        derivedStateOf { gridState.canScrollForward || gridState.canScrollBackward }
    }
    LaunchedEffect(canScrollGrid) {
        if (!canScrollGrid) {
            quickReturnState.resetToVisible()
        }
    }

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

    val topInsetPx = with(density) { contentTopPadding.toPx() }
    val headerReservedHeight = 68.dp

    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            PageContentLoadingBox(
                testTag = "playlists_page_loading",
                indicatorTestTag = "playlists_page_loading_indicator",
                contentTopPadding = contentTopPadding,
                contentBottomPadding = contentBottomPadding
            )
            return@Box
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(2f)
                .padding(top = contentTopPadding, start = 16.dp, end = 16.dp)
                .padding(vertical = 4.dp)
                .onSizeChanged { size ->
                    quickReturnState.updateHeaderHeightPx(size.height, topInsetPx)
                }
                .graphicsLayer {
                    translationY = quickReturnState.offsetPx
                }
                .testTag("playlists_floating_header"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (orderedPlaylists.isNotEmpty()) {
                CapsuleSearchField(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        quickReturnState.resetToVisible()
                    },
                    placeholderText = stringResource(R.string.search_playlists_placeholder),
                    containerTestTag = "playlists_search_bar",
                    inputTestTag = "input_search_playlists",
                    clearButtonTestTag = "btn_clear_search_playlists",
                    shadowElevation = 6.dp,
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
                    .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                    .testTag("btn_create_playlist")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.playlist_create_action),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentTopPadding + headerReservedHeight,
                bottom = contentBottomPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { gridCoords = it }
                .nestedScroll(quickReturnState.nestedScrollConnection)
                .testTag("playlists_lazy_grid")
        ) {

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
    val resolvedIcon = remember(playlist.iconId) { PlaylistIconCatalog.resolveOrDefault(playlist.iconId) }
    val watermarkPainter = androidx.compose.ui.graphics.vector.rememberVectorPainter(image = resolvedIcon.vector)

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
                .drawWithCache {
                    val accent = if (isActiveQueue) primaryColor else tertiaryColor
                    val glowCenter = Offset(size.width * 0.16f, size.height * 0.16f)
                    val glowRadius = (size.width * 0.72f).coerceAtLeast(1f)
                    val glowBrush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = if (isActiveQueue) 0.18f else 0.10f),
                            Color.Transparent
                        ),
                        center = glowCenter,
                        radius = glowRadius
                    )
                    val watermarkTint = if (isActiveQueue) {
                        primaryColor
                    } else {
                        lerp(titleColor, primaryColor, 0.42f)
                    }
                    val watermarkCenter = Offset(size.width * 0.68f, size.height * 0.58f)
                    val watermarkSize = size.width * 0.94f
                    val watermarkAlpha = if (isActiveQueue) 0.095f else 0.072f

                    onDrawBehind {
                        drawCircle(
                            brush = glowBrush,
                            radius = glowRadius,
                            center = glowCenter
                        )
                        drawSoftPlaylistWatermark(
                            painter = watermarkPainter,
                            center = watermarkCenter,
                            iconSizePx = watermarkSize,
                            tint = watermarkTint,
                            alpha = watermarkAlpha,
                            rotationDegrees = -14f
                        )
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { stateDescription = resolvedIcon.id }
                    .testTag("playlist_card_watermark_${playlist.id}")
            )

            // Góc trên trái: Icon danh sách phát / sóng nhạc đặt trong nền tròn kích thước bằng nút xóa (30dp) & nhãn đang phát
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 13.dp, top = 13.dp, end = 52.dp)
                    .height(30.dp)
            ) {
                Surface(
                    shape = CircleShape,
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
                        .size(30.dp)
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
                                modifier = Modifier.size(width = 16.dp, height = 13.dp)
                            )
                        } else {
                            Icon(
                                imageVector = resolvedIcon.vector,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
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
                    .size(48.dp)
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
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("playlist_delete_circle_${playlist.id}")
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
    onUpdatePlaylistIcon: (String?) -> Unit = {},
    onReorderSongs: (List<Song>) -> Unit = {},
    onSongClick: (Song) -> Unit,
    onRemoveSongFromPlaylist: (Song) -> Unit,
    onOpenPlayer: (() -> Unit)? = null,
    contentTopPadding: Dp,
    contentBottomPadding: Dp,
    isDarkTheme: Boolean,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var revealedSongId by remember { mutableStateOf<Long?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var isSearchExpanded by remember(playlist.id) { mutableStateOf(false) }
    var searchQuery by remember(playlist.id) { mutableStateOf("") }
    var screenRootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var searchCapsuleBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    val resolvedPlaylistIcon = remember(playlist.iconId) { PlaylistIconCatalog.resolveOrDefault(playlist.iconId) }
    val hasCustomNonDefaultIcon = playlist.iconId != null && resolvedPlaylistIcon.id != PlaylistIconCatalog.DEFAULT_ICON_ID

    // Trạng thái sắp xếp bài hát bằng kéo thả dọc từ icon 4 chấm
    var orderedSongs by remember(playlist.id) { mutableStateOf(playlist.songs) }
    var pendingReorderedSongIds by remember(playlist.id) { mutableStateOf<List<Long>?>(null) }
    var draggedSongId by remember(playlist.id) { mutableStateOf<Long?>(null) }
    var droppingSongId by remember(playlist.id) { mutableStateOf<Long?>(null) }
    val dropSnapAnim = remember(playlist.id) { Animatable(0f) }

    var dragStartPointerWindowY by remember(playlist.id) { mutableFloatStateOf(0f) }
    var currentPointerWindowY by remember(playlist.id) { mutableFloatStateOf(0f) }
    var initialSlotTopInWindow by remember(playlist.id) { mutableFloatStateOf(0f) }
    var draggedCardHeightPx by remember(playlist.id) { mutableFloatStateOf(0f) }

    var listBoundsInWindow by remember { mutableStateOf(Rect.Zero) }
    var headerBottomInWindow by remember { mutableFloatStateOf(0f) }
    val slotBoundsInWindow = remember(playlist.id) { mutableStateMapOf<Long, Rect>() }
    val handleCoordsMap = remember(playlist.id) { mutableStateMapOf<Long, LayoutCoordinates>() }

    LaunchedEffect(playlist.songs) {
        val pending = pendingReorderedSongIds
        val incomingIds = playlist.songs.map { it.id }
        if (pending != null) {
            if (incomingIds == pending || incomingIds.toSet() != pending.toSet()) {
                orderedSongs = playlist.songs
                pendingReorderedSongIds = null
            }
        } else if (draggedSongId == null) {
            orderedSongs = playlist.songs
        }
    }

    val currentRevealedId by rememberUpdatedState(revealedSongId)
    val currentIsSearchExpanded by rememberUpdatedState(isSearchExpanded)
    val latestOnReorderSongs by rememberUpdatedState(onReorderSongs)
    val latestOnSongClick by rememberUpdatedState(onSongClick)

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

    val isSearching = searchQuery.trim().isNotEmpty()
    val filteredSongs = remember(orderedSongs, searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            orderedSongs
        } else {
            val normalizedQuery = Song.normalizeForSearch(trimmed)
            orderedSongs.filter { song ->
                matchesSongSearchQuery(song, trimmed, normalizedQuery)
            }
        }
    }
    val displayedSongs = if (isSearching) filteredSongs else orderedSongs

    val itemSpacingPx = remember(density) { with(density) { 3.dp.toPx() } }
    val computeClampedCardTop: () -> Float = {
        val rawTop = initialSlotTopInWindow + (currentPointerWindowY - dragStartPointerWindowY)
        if (listBoundsInWindow.height > 1f) {
            var minTop = listBoundsInWindow.top
            if (listState.firstVisibleItemIndex == 0 && headerBottomInWindow > 0f) {
                minTop = maxOf(minTop, headerBottomInWindow + itemSpacingPx)
            }
            if (!listState.canScrollBackward) {
                val firstSlotTop = orderedSongs.firstOrNull()?.id?.let { slotBoundsInWindow[it]?.top }
                if (firstSlotTop != null) {
                    minTop = maxOf(minTop, firstSlotTop)
                }
            }
            var maxBottom = listBoundsInWindow.bottom
            if (!listState.canScrollForward) {
                val lastSlotBottom = orderedSongs.lastOrNull()?.id?.let { slotBoundsInWindow[it]?.bottom }
                if (lastSlotBottom != null) {
                    maxBottom = minOf(maxBottom, lastSlotBottom)
                }
            }
            val maxTop = (maxBottom - draggedCardHeightPx).coerceAtLeast(minTop)
            rawTop.coerceIn(minTop, maxTop)
        } else {
            rawTop
        }
    }

    val trySwapAtDraggedPosition: (Long) -> Unit = { activeId ->
        val currentList = orderedSongs
        val currentIdx = currentList.indexOfFirst { it.id == activeId }
        if (currentIdx != -1) {
            val clampedTop = computeClampedCardTop()
            val clampedBottom = clampedTop + draggedCardHeightPx
            val draggedCenterY = clampedTop + draggedCardHeightPx * 0.5f

            var targetIdx = currentIdx
            for (i in (currentIdx + 1)..currentList.lastIndex) {
                val rect = slotBoundsInWindow[currentList[i].id] ?: continue
                if (draggedCenterY > rect.center.y + 4f || clampedBottom >= rect.bottom - 2f) {
                    targetIdx = i
                }
            }
            if (targetIdx == currentIdx) {
                for (i in (currentIdx - 1) downTo 0) {
                    val rect = slotBoundsInWindow[currentList[i].id] ?: continue
                    if (draggedCenterY < rect.center.y - 4f || clampedTop <= rect.top + 2f) {
                        targetIdx = i
                    }
                }
            }

            if (targetIdx != currentIdx) {
                val mutable = currentList.toMutableList()
                val moved = mutable.removeAt(currentIdx)
                mutable.add(targetIdx, moved)
                orderedSongs = mutable
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    // Tự động cuộn danh sách mượt mà khi kéo bài hát chạm mép trên/dưới (chỉ chạy trong lúc đang kéo)
    LaunchedEffect(draggedSongId) {
        val activeId = draggedSongId ?: return@LaunchedEffect
        val edgeZonePx = with(density) { 56.dp.toPx() }
        val maxScrollStepPx = with(density) { 10.dp.toPx() }
        while (isActive && draggedSongId == activeId) {
            val bounds = listBoundsInWindow
            if (bounds.height > 1f) {
                val pointerY = currentPointerWindowY
                val topZoneEnd = bounds.top + edgeZonePx
                val bottomZoneStart = bounds.bottom - edgeZonePx
                val scrollDelta = when {
                    pointerY < topZoneEnd && listState.canScrollBackward -> {
                        val ratio = ((topZoneEnd - pointerY) / edgeZonePx).coerceIn(0.25f, 1f)
                        -maxScrollStepPx * ratio
                    }
                    pointerY > bottomZoneStart && listState.canScrollForward -> {
                        val ratio = ((pointerY - bottomZoneStart) / edgeZonePx).coerceIn(0.25f, 1f)
                        maxScrollStepPx * ratio
                    }
                    else -> 0f
                }
                if (scrollDelta != 0f) {
                    listState.scrollBy(scrollDelta)
                    trySwapAtDraggedPosition(activeId)
                }
            }
            delay(16L)
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && draggedSongId == null) {
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
                    val revealedAtDown = currentRevealedId
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
                            if (!maxSlopExceeded && draggedSongId == null) {
                                // Chỉ đóng nút xóa đang mở khi chạm ra ngoài; không đóng nút vừa mới mở do nhấn giữ
                                if (revealedAtDown != null && revealedAtDown == currentRevealedId) {
                                    revealedSongId = null
                                }
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
        if (isLoading) {
            PageContentLoadingBox(
                testTag = "playlists_page_loading",
                indicatorTestTag = "playlists_page_loading_indicator",
                contentTopPadding = contentTopPadding,
                contentBottomPadding = contentBottomPadding
            )
            return@Box
        }

        LazyColumn(
            state = listState,
            userScrollEnabled = draggedSongId == null,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = animatedTopPadding,
                bottom = contentBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    if (coords.isAttached) {
                        listBoundsInWindow = coords.boundsInWindow()
                    }
                }
                .testTag("playlist_detail_lazy_column")
        ) {
            item(key = "playlist_detail_header", contentType = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            if (coords.isAttached) {
                                headerBottomInWindow = coords.boundsInWindow().bottom
                            }
                        }
                        .padding(bottom = 10.dp)
                ) {
                    val actionBtnSize = 48.dp
                    val actionBtnGap = 8.dp
                    val expandProgress by animateFloatAsState(
                        targetValue = if (isSearchExpanded) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.58f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "playlist_detail_search_expand"
                    )
                    val clampedExpand = expandProgress.coerceAtLeast(0f)
                    val headerContentAlpha = (1f - clampedExpand * 1.65f).coerceIn(0f, 1f)
                    val bounceScale = 1f + if (expandProgress < 0f) {
                        expandProgress * 0.40f
                    } else {
                        (expandProgress - 1f).coerceAtLeast(0f) * 0.18f
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = actionBtnSize),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val totalHeaderWidth = maxWidth.coerceAtLeast(actionBtnSize)
                        val rightClusterWidth = actionBtnSize + actionBtnGap + actionBtnSize + actionBtnGap
                        val collapsedSearchLeft = (totalHeaderWidth - rightClusterWidth - actionBtnSize).coerceAtLeast(0.dp)
                        val searchLeftDp = (collapsedSearchLeft * (1f - clampedExpand)).coerceIn(0.dp, collapsedSearchLeft)
                        val searchWidthDp = (actionBtnSize + (totalHeaderWidth - actionBtnSize) * clampedExpand)
                            .coerceIn(actionBtnSize, totalHeaderWidth)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = actionBtnSize)
                                .graphicsLayer { alpha = headerContentAlpha },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                enabled = !isSearchExpanded,
                                modifier = Modifier
                                    .size(actionBtnSize)
                                    .testTag("btn_back_playlists")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_close),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val renameActionDesc = stringResource(R.string.cd_rename_playlist)
                            val titleShape = remember { RoundedCornerShape(12.dp) }
                            Column(
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = actionBtnSize)
                                    .clip(titleShape)
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
                                Spacer(modifier = Modifier.width(actionBtnSize + actionBtnGap))
                            }

                            val chooseIconSource = remember { MutableInteractionSource() }
                            FilledTonalIconButton(
                                onClick = { if (!isSearchExpanded) showIconPicker = true },
                                enabled = !isSearchExpanded,
                                shape = CircleShape,
                                interactionSource = chooseIconSource,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (hasCustomNonDefaultIcon) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    },
                                    contentColor = if (hasCustomNonDefaultIcon) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                ),
                                modifier = Modifier
                                    .size(actionBtnSize)
                                    .springPress(scaleDown = 0.90f, interactionSource = chooseIconSource)
                                    .testTag("btn_choose_playlist_icon")
                            ) {
                                Icon(
                                    imageVector = resolvedPlaylistIcon.vector,
                                    contentDescription = stringResource(R.string.cd_choose_playlist_icon),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(actionBtnGap))

                            val addSongsSource = remember { MutableInteractionSource() }
                            FilledTonalIconButton(
                                onClick = { if (!isSearchExpanded) onAddSongs() },
                                enabled = !isSearchExpanded,
                                shape = CircleShape,
                                interactionSource = addSongsSource,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier
                                    .size(actionBtnSize)
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

                        if (hasSongs) {
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
                                    .zIndex(3f)
                                    .width(searchWidthDp)
                                    .height(actionBtnSize)
                                    .graphicsLayer {
                                        translationX = searchLeftDp.toPx()
                                        scaleX = bounceScale
                                        scaleY = bounceScale
                                    }
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
                                if (!isSearchExpanded && searchWidthDp <= 60.dp) {
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
                                            .padding(start = 16.dp, end = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = stringResource(R.string.cd_search_in_playlist),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
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
                                                .size(34.dp)
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
            } else if (displayedSongs.isEmpty()) {
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
                    items = displayedSongs,
                    key = { _, song -> song.id },
                    contentType = { _, _ -> "playlist_song_card" }
                ) { index, song ->
                    val isCurrentSong = playbackState.currentSong?.id == song.id
                    val isFirst = index == 0
                    val isLast = index == displayedSongs.lastIndex
                    val isActionsRevealed = revealedSongId == song.id
                    val isDragged = draggedSongId == song.id
                    val isReorderEnabled = !isSearching && displayedSongs.size > 1

                    DisposableEffect(song.id) {
                        onDispose {
                            slotBoundsInWindow.remove(song.id)
                            handleCoordsMap.remove(song.id)
                        }
                    }

                    val onCardClick = remember(song, playlist.id) {
                        {
                            if (currentRevealedId != null) {
                                revealedSongId = null
                            } else {
                                latestOnSongClick(song)
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

                    val itemWrapperModifier = if (isDragged) {
                        Modifier.zIndex(10f)
                    } else {
                        Modifier
                            .zIndex(if (droppingSongId == song.id) 5f else 0f)
                            .animateItem(
                                placementSpec = spring(
                                    dampingRatio = 0.72f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                    }

                    val dragHandleGestureModifier = Modifier
                        .onGloballyPositioned { coords ->
                            if (coords.isAttached) {
                                handleCoordsMap[song.id] = coords
                            }
                        }
                        .pointerInput(playlist.id, song.id, isReorderEnabled) {
                            if (!isReorderEnabled) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = { startOffset ->
                                    if (draggedSongId != null && draggedSongId != song.id) {
                                        return@detectDragGesturesAfterLongPress
                                    }
                                    val hCoords = handleCoordsMap[song.id]?.takeIf { it.isAttached }
                                        ?: return@detectDragGesturesAfterLongPress
                                    val slotRect = slotBoundsInWindow[song.id]
                                        ?: return@detectDragGesturesAfterLongPress
                                    val startWin = hCoords.localToWindow(startOffset)
                                    revealedSongId = null
                                    droppingSongId = null
                                    pendingReorderedSongIds = null
                                    dragStartPointerWindowY = startWin.y
                                    currentPointerWindowY = startWin.y
                                    initialSlotTopInWindow = slotRect.top
                                    draggedCardHeightPx = slotRect.height
                                    draggedSongId = song.id
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, dragAmount ->
                                    if (draggedSongId != song.id) {
                                        return@detectDragGesturesAfterLongPress
                                    }
                                    change.consume()
                                    val hCoords = handleCoordsMap[song.id]?.takeIf { it.isAttached }
                                    val pointerWinY = if (hCoords != null) {
                                        hCoords.localToWindow(change.position).y
                                    } else {
                                        currentPointerWindowY + dragAmount.y
                                    }
                                    currentPointerWindowY = pointerWinY
                                    trySwapAtDraggedPosition(song.id)
                                },
                                onDragEnd = {
                                    if (draggedSongId == song.id) {
                                        val finalSongs = orderedSongs
                                        val slotTopNow = slotBoundsInWindow[song.id]?.top ?: initialSlotTopInWindow
                                        val releaseOffsetY = computeClampedCardTop() - slotTopNow
                                        draggedSongId = null
                                        droppingSongId = song.id
                                        coroutineScope.launch {
                                            dropSnapAnim.snapTo(releaseOffsetY)
                                            dropSnapAnim.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.72f,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                            if (droppingSongId == song.id) {
                                                droppingSongId = null
                                            }
                                        }
                                        val finalIds = finalSongs.map { it.id }
                                        if (finalIds != playlist.songs.map { it.id }) {
                                            pendingReorderedSongIds = finalIds
                                            latestOnReorderSongs(finalSongs)
                                        }
                                    }
                                },
                                onDragCancel = {
                                    if (draggedSongId == song.id) {
                                        draggedSongId = null
                                        droppingSongId = null
                                        pendingReorderedSongIds = null
                                        orderedSongs = playlist.songs
                                    }
                                }
                            )
                        }

                    Box(
                        modifier = itemWrapperModifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                if (coords.isAttached) {
                                    slotBoundsInWindow[song.id] = coords.boundsInWindow()
                                }
                            }
                    ) {
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
                            onOpenPlayer = onOpenPlayer,
                            isDragged = isDragged,
                            showDragHandle = true,
                            isDragHandleEnabled = isReorderEnabled,
                            dragHandleModifier = dragHandleGestureModifier,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    translationX = 0f
                                    translationY = when {
                                        isDragged -> {
                                            val currentSlotTop = slotBoundsInWindow[song.id]?.top ?: initialSlotTopInWindow
                                            computeClampedCardTop() - currentSlotTop
                                        }
                                        droppingSongId == song.id -> dropSnapAnim.value
                                        else -> 0f
                                    }
                                }
                                .then(if (isLast) Modifier.padding(bottom = 6.dp) else Modifier)
                                .testTag("playlist_song_card_${song.id}")
                        )
                    }
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

    if (showIconPicker) {
        PlaylistCustomIconPickerDialog(
            selectedIconId = resolvedPlaylistIcon.id,
            onSelectIcon = { iconId ->
                onUpdatePlaylistIcon(iconId)
            },
            onDismiss = { showIconPicker = false }
        )
    }
}
