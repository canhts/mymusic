package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.R
import com.example.model.Playlist
import com.example.model.Song
import com.example.ui.theme.LocalizedAppScope

@Composable
internal fun CreatePlaylistDialog(
    onConfirmCreate: (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var playlistName by remember { mutableStateOf("") }
    var selectedIconId by remember { mutableStateOf(PlaylistIconCatalog.DEFAULT_ICON_ID) }
    var showIconPicker by remember { mutableStateOf(false) }
    val trimmed = playlistName.trim()
    val selectedIconItem = remember(selectedIconId) { PlaylistIconCatalog.resolveOrDefault(selectedIconId) }
    val isCustomIconChosen = selectedIconId != PlaylistIconCatalog.DEFAULT_ICON_ID

    Dialog(onDismissRequest = onDismiss) {
        LocalizedAppScope {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp)
                    .testTag("dialog_create_playlist")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = selectedIconItem.vector,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.playlist_create_dialog_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AlertDialogDefaults.titleContentColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text(stringResource(R.string.playlist_name_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_playlist_name")
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = { showIconPicker = true },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (isCustomIconChosen) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                                contentColor = if (isCustomIconChosen) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_create_playlist_choose_icon")
                        ) {
                            Icon(
                                imageVector = selectedIconItem.vector,
                                contentDescription = stringResource(R.string.cd_choose_playlist_icon),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_cancel_create_playlist")
                            ) {
                                Text(stringResource(R.string.action_cancel))
                            }
                            Button(
                                onClick = {
                                    if (trimmed.isNotEmpty()) {
                                        onConfirmCreate(trimmed, selectedIconId)
                                    }
                                },
                                enabled = trimmed.isNotEmpty(),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_confirm_create_playlist")
                            ) {
                                Text(stringResource(R.string.action_create))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showIconPicker) {
        PlaylistCustomIconPickerDialog(
            selectedIconId = selectedIconId,
            onSelectIcon = { iconId -> selectedIconId = iconId },
            onDismiss = { showIconPicker = false }
        )
    }
}

@Composable
internal fun RenamePlaylistDialog(
    initialName: String,
    onConfirmRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var playlistName by remember(initialName) { mutableStateOf(initialName) }
    val trimmed = playlistName.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        title = {
            LocalizedAppScope {
                Text(
                    text = stringResource(R.string.playlist_rename_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            LocalizedAppScope {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text(stringResource(R.string.playlist_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_rename_playlist_name")
                )
            }
        },
        confirmButton = {
            LocalizedAppScope {
                Button(
                    onClick = {
                        if (trimmed.isNotEmpty()) {
                            onConfirmRename(trimmed)
                        }
                    },
                    enabled = trimmed.isNotEmpty() && trimmed != initialName.trim(),
                    modifier = Modifier.testTag("btn_confirm_rename_playlist")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        },
        dismissButton = {
            LocalizedAppScope {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_cancel_rename_playlist")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
internal fun DeletePlaylistDialog(
    playlist: Playlist,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        title = {
            LocalizedAppScope {
                Text(
                    text = stringResource(R.string.playlist_delete_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            LocalizedAppScope {
                Text(
                    text = stringResource(R.string.playlist_delete_dialog_message, playlist.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            LocalizedAppScope {
                Button(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("btn_confirm_delete_playlist")
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        },
        dismissButton = {
            LocalizedAppScope {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_cancel_delete_playlist")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddToPlaylistBottomSheet(
    song: Song,
    playlists: List<Playlist>,
    onConfirmAdd: (List<Long>) -> Unit,
    onQuickCreatePlaylist: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetShape = remember { RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) }
    val selectedPlaylistIds = remember(song.id) { mutableStateListOf<Long>() }
    var showQuickCreateInput by remember { mutableStateOf(false) }
    var quickPlaylistName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LocalizedAppScope {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
                    .testTag("sheet_add_to_playlist")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.add_to_playlist_sheet_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = song.displayTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = { showQuickCreateInput = !showQuickCreateInput },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_quick_create_playlist")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.playlist_create_action),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (showQuickCreateInput) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quickPlaylistName,
                            onValueChange = { quickPlaylistName = it },
                            label = { Text(stringResource(R.string.playlist_name_label)) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_quick_playlist_name")
                        )
                        Button(
                            onClick = {
                                val trimmed = quickPlaylistName.trim()
                                if (trimmed.isNotEmpty()) {
                                    onQuickCreatePlaylist(trimmed)
                                    onDismiss()
                                }
                            },
                            enabled = quickPlaylistName.trim().isNotEmpty(),
                            shape = CircleShape,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("btn_submit_quick_playlist")
                        ) {
                            Text(stringResource(R.string.action_create))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (playlists.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.playlists_empty_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .padding(vertical = 4.dp)
                        ) {
                            items(items = playlists, key = { it.id }) { playlist ->
                                val alreadyInPlaylist = remember(playlist.songs, song.id) {
                                    playlist.songs.any { it.id == song.id }
                                }
                                val isChecked = alreadyInPlaylist || selectedPlaylistIds.contains(playlist.id)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !alreadyInPlaylist) {
                                            if (selectedPlaylistIds.contains(playlist.id)) {
                                                selectedPlaylistIds.remove(playlist.id)
                                            } else {
                                                selectedPlaylistIds.add(playlist.id)
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                        .testTag("playlist_select_row_${playlist.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        enabled = !alreadyInPlaylist,
                                        onCheckedChange = { checked ->
                                            if (!alreadyInPlaylist) {
                                                if (checked) {
                                                    if (!selectedPlaylistIds.contains(playlist.id)) {
                                                        selectedPlaylistIds.add(playlist.id)
                                                    }
                                                } else {
                                                    selectedPlaylistIds.remove(playlist.id)
                                                }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
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
                                    if (alreadyInPlaylist) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = stringResource(R.string.add_to_playlist_already_added),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onConfirmAdd(selectedPlaylistIds.toList())
                            onDismiss()
                        },
                        enabled = selectedPlaylistIds.isNotEmpty(),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_confirm_add_to_playlists")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.add_to_playlist_confirm),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
        }
    }
}

/**
 * Bộ quan sát chạm bên ngoài khung tìm kiếm viên nhộng trong BottomSheet:
 * Khi khung tìm kiếm đang mở rộng và người dùng chạm bất kỳ vị trí nào bên ngoài viên nhộng,
 * thanh tìm kiếm sẽ thu gọn mượt mà về lại nút tròn ban đầu.
 */
@Composable
private fun Modifier.collapseSheetSearchOnOutsideTap(
    isSearchExpanded: Boolean,
    onUpdateRootCoords: (LayoutCoordinates) -> Unit,
    rootCoordsProvider: () -> LayoutCoordinates?,
    searchBoundsInRootProvider: () -> Rect,
    onCollapse: () -> Unit
): Modifier {
    val currentExpanded by rememberUpdatedState(isSearchExpanded)
    val currentOnCollapse by rememberUpdatedState(onCollapse)
    return this
        .onGloballyPositioned(onUpdateRootCoords)
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
                        if (!maxSlopExceeded && currentExpanded) {
                            val rootCoords = rootCoordsProvider()?.takeIf { it.isAttached }
                            val tapInRoot = rootCoords?.localToRoot(lastPos) ?: lastPos
                            if (!searchBoundsInRootProvider().contains(tapInRoot)) {
                                currentOnCollapse()
                            }
                        }
                        break
                    }
                }
            }
        }
}

/**
 * Cụm tiêu đề + nút tìm kiếm dạng tròn nằm bên phải tiêu đề:
 * - Ban đầu: Nút tròn chỉ có icon Tìm kiếm nằm bên phải tiêu đề (chỉ hiển thị khi có bài hát).
 * - Khi bấm vào: Animation giãn dài sang trái thành viên nhộng dài, kéo dài đến sát vị trí
 *   icon danh sách bên trái, tạm thời đè lên tiêu đề / tên danh sách.
 * - Khi bấm nút X hoặc chạm ra ngoài: Thu gọn mượt mà trở về vị trí hình tròn ban đầu.
 */
@Composable
private fun ExpandableSheetHeaderSearch(
    title: String,
    subtitle: String,
    titleTestTag: String? = null,
    subtitleTestTag: String? = null,
    showSearchButton: Boolean,
    isSearchExpanded: Boolean,
    onExpandSearch: () -> Unit,
    onCollapseSearch: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchButtonTestTag: String,
    searchInputTestTag: String,
    closeButtonTestTag: String,
    onUpdateSearchBoundsInRoot: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    val titleAlpha by animateFloatAsState(
        targetValue = if (isSearchExpanded) 0f else 1f,
        animationSpec = tween(durationMillis = if (isSearchExpanded) 110 else 180),
        label = "sheet_header_title_alpha"
    )

    BoxWithConstraints(
        modifier = modifier.heightIn(min = 44.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val collapsedSize = 40.dp
        val maxCapsuleWidth = maxWidth.coerceAtLeast(collapsedSize)

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = if (showSearchButton) 48.dp else 0.dp)
                .graphicsLayer { alpha = titleAlpha }
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (titleTestTag != null) Modifier.testTag(titleTestTag) else Modifier
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (subtitleTestTag != null) Modifier.testTag(subtitleTestTag) else Modifier
            )
        }

        if (showSearchButton) {
            val targetSearchWidth = if (isSearchExpanded) maxCapsuleWidth else collapsedSize
            val animatedSearchWidth by animateDpAsState(
                targetValue = targetSearchWidth,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "sheet_header_search_width_$searchButtonTestTag"
            )
            val searchContainerColor by animateColorAsState(
                targetValue = if (isSearchExpanded) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                animationSpec = tween(durationMillis = 160),
                label = "sheet_header_search_color_$searchButtonTestTag"
            )

            Surface(
                shape = CircleShape,
                color = searchContainerColor,
                tonalElevation = if (isSearchExpanded) 2.dp else 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(2f)
                    .width(animatedSearchWidth.coerceAtLeast(collapsedSize))
                    .height(collapsedSize)
                    .onGloballyPositioned { coords ->
                        if (coords.isAttached) {
                            onUpdateSearchBoundsInRoot(coords.boundsInRoot())
                        }
                    }
                    .clip(CircleShape)
                    .then(
                        if (!isSearchExpanded) {
                            Modifier.clickable { onExpandSearch() }
                        } else {
                            Modifier
                        }
                    )
                    .testTag(searchButtonTestTag)
            ) {
                if (!isSearchExpanded && animatedSearchWidth <= 48.dp) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.cd_search_in_playlist),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.cd_search_in_playlist),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
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
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester)
                                    .testTag(searchInputTestTag)
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { onCollapseSearch() }
                                .testTag(closeButtonTestTag)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.clear_search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddSongsToPlaylistBottomSheet(
    playlist: Playlist,
    allSongs: List<Song>,
    onConfirmAddSongs: (List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetShape = remember { RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) }
    val focusManager = LocalFocusManager.current
    val existingIds = remember(playlist.songs) { playlist.songs.map { it.id }.toSet() }
    val availableSongs = remember(allSongs, existingIds) {
        allSongs.filter { !existingIds.contains(it.id) }
    }
    val selectedSongIds = remember(playlist.id) { mutableStateListOf<Long>() }
    var isSearchExpanded by remember(playlist.id) { mutableStateOf(false) }
    var searchQuery by remember(playlist.id) { mutableStateOf("") }
    var sheetRootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var searchCapsuleBoundsInRoot by remember { mutableStateOf(Rect.Zero) }

    val collapseSearch = remember(focusManager) {
        {
            if (isSearchExpanded || searchQuery.isNotEmpty()) {
                isSearchExpanded = false
                searchQuery = ""
                focusManager.clearFocus()
            }
        }
    }

    LaunchedEffect(availableSongs.isEmpty()) {
        if (availableSongs.isEmpty()) {
            isSearchExpanded = false
            searchQuery = ""
        }
    }

    BackHandler(enabled = isSearchExpanded) {
        collapseSearch()
    }

    val filteredSongs = remember(availableSongs, searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            availableSongs
        } else {
            val normalizedQuery = Song.normalizeForSearch(trimmed)
            availableSongs.filter { song ->
                matchesSongSearchQuery(song, trimmed, normalizedQuery)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LocalizedAppScope {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
                    .collapseSheetSearchOnOutsideTap(
                        isSearchExpanded = isSearchExpanded,
                        onUpdateRootCoords = { sheetRootCoords = it },
                        rootCoordsProvider = { sheetRootCoords },
                        searchBoundsInRootProvider = { searchCapsuleBoundsInRoot },
                        onCollapse = collapseSearch
                    )
                    .testTag("sheet_add_songs_to_playlist")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("icon_playlist_add_songs")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    ExpandableSheetHeaderSearch(
                        title = stringResource(R.string.add_songs_to_playlist_title),
                        subtitle = playlist.name,
                        titleTestTag = "title_add_songs_to_playlist",
                        subtitleTestTag = "subtitle_add_songs_to_playlist",
                        showSearchButton = availableSongs.isNotEmpty(),
                        isSearchExpanded = isSearchExpanded,
                        onExpandSearch = { isSearchExpanded = true },
                        onCollapseSearch = collapseSearch,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        searchButtonTestTag = "btn_search_add_songs",
                        searchInputTestTag = "input_search_add_songs",
                        closeButtonTestTag = "btn_clear_search_add_songs",
                        onUpdateSearchBoundsInRoot = { searchCapsuleBoundsInRoot = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (availableSongs.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.add_songs_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (filteredSongs.isEmpty()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 28.dp)
                                    .testTag("add_songs_no_search_results")
                            ) {
                                Text(
                                    text = stringResource(R.string.no_search_results),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .padding(vertical = 4.dp)
                            ) {
                                items(items = filteredSongs, key = { it.id }) { song ->
                                    val isChecked = selectedSongIds.contains(song.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isChecked) {
                                                    selectedSongIds.remove(song.id)
                                                } else {
                                                    selectedSongIds.add(song.id)
                                                }
                                            }
                                            .padding(start = 10.dp, end = 12.dp, top = 9.dp, bottom = 9.dp)
                                            .testTag("pick_song_row_${song.id}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 24.dp) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        if (!selectedSongIds.contains(song.id)) {
                                                            selectedSongIds.add(song.id)
                                                        }
                                                    } else {
                                                        selectedSongIds.remove(song.id)
                                                    }
                                                },
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = song.displayTitle,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 14.5.sp,
                                                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                                            ),
                                            color = if (isChecked) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = formatCardDuration(song.duration),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onConfirmAddSongs(selectedSongIds.toList())
                            onDismiss()
                        },
                        enabled = selectedSongIds.isNotEmpty(),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_confirm_add_songs_to_playlist")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.playlist_add_songs_action),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurrentQueueBottomSheet(
    queueSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    activePlaylistName: String?,
    onSelectSong: (Song) -> Unit,
    onOpenPlayer: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetShape = remember { RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sheetRootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var searchCapsuleBoundsInRoot by remember { mutableStateOf(Rect.Zero) }

    val collapseSearch = remember(focusManager) {
        {
            if (isSearchExpanded || searchQuery.isNotEmpty()) {
                isSearchExpanded = false
                searchQuery = ""
                focusManager.clearFocus()
            }
        }
    }

    LaunchedEffect(queueSongs.isEmpty()) {
        if (queueSongs.isEmpty()) {
            isSearchExpanded = false
            searchQuery = ""
        }
    }

    BackHandler(enabled = isSearchExpanded) {
        collapseSearch()
    }

    val filteredQueueSongs = remember(queueSongs, searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            queueSongs
        } else {
            val normalizedQuery = Song.normalizeForSearch(trimmed)
            queueSongs.filter { song ->
                matchesSongSearchQuery(song, trimmed, normalizedQuery)
            }
        }
    }

    LaunchedEffect(currentSong?.id, queueSongs.size) {
        val currentIdx = currentSong?.let { cur -> queueSongs.indexOfFirst { it.id == cur.id } } ?: -1
        if (currentIdx > 1) {
            listState.scrollToItem((currentIdx - 1).coerceAtLeast(0))
        }
    }

    val subtitle = if (activePlaylistName != null) {
        stringResource(R.string.current_queue_playlist_subtitle, activePlaylistName, queueSongs.size)
    } else {
        stringResource(R.string.current_queue_library_subtitle, queueSongs.size)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LocalizedAppScope {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
                    .collapseSheetSearchOnOutsideTap(
                        isSearchExpanded = isSearchExpanded,
                        onUpdateRootCoords = { sheetRootCoords = it },
                        rootCoordsProvider = { sheetRootCoords },
                        searchBoundsInRootProvider = { searchCapsuleBoundsInRoot },
                        onCollapse = collapseSearch
                    )
                    .testTag("sheet_current_queue")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("icon_playlist_current_queue")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    ExpandableSheetHeaderSearch(
                        title = stringResource(R.string.current_queue_sheet_title),
                        subtitle = subtitle,
                        titleTestTag = "title_current_queue",
                        subtitleTestTag = "subtitle_current_queue",
                        showSearchButton = queueSongs.isNotEmpty(),
                        isSearchExpanded = isSearchExpanded,
                        onExpandSearch = { isSearchExpanded = true },
                        onCollapseSearch = collapseSearch,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        searchButtonTestTag = "btn_search_current_queue",
                        searchInputTestTag = "input_search_current_queue",
                        closeButtonTestTag = "btn_close_search_current_queue",
                        onUpdateSearchBoundsInRoot = { searchCapsuleBoundsInRoot = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (queueSongs.isNotEmpty() && filteredQueueSongs.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 28.dp)
                                .testTag("current_queue_no_search_results")
                        ) {
                            Text(
                                text = stringResource(R.string.no_search_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                                .padding(vertical = 6.dp)
                        ) {
                            itemsIndexed(
                                items = filteredQueueSongs,
                                key = { _, song -> song.id }
                            ) { index, song ->
                                val isCurrent = currentSong?.id == song.id
                                val rowCorner by animateDpAsState(
                                    targetValue = resolveQueueRowCornerTarget(isCurrent),
                                    animationSpec = spring(
                                        dampingRatio = 0.56f,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "queue_row_corner_${song.id}"
                                )
                                val rowColor by animateColorAsState(
                                    targetValue = if (isCurrent) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "queue_row_bg_${song.id}"
                                )
                                val rowRadius = rowCorner.coerceAtLeast(0.dp)
                                val rowShape = remember(rowRadius) { RoundedCornerShape(rowRadius) }
                                Surface(
                                    shape = rowShape,
                                    color = rowColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .clip(rowShape)
                                        .clickable {
                                            onSelectSong(song)
                                            onDismiss()
                                        }
                                        .testTag("queue_song_row_${song.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 10.dp, end = 12.dp, top = 9.dp, bottom = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .testTag("queue_song_index_${song.id}")
                                        ) {
                                            if (isCurrent) {
                                                AudioWaveformEqualizer(
                                                    isPlaying = isPlaying,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(width = 16.dp, height = 14.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 12.5.sp,
                                                        fontFeatureSettings = "tnum"
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Text(
                                            text = song.displayTitle,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isCurrent) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        if (isCurrent && onOpenPlayer != null) {
                                            SelectedSongOpenPlayerButton(
                                                onClick = {
                                                    onDismiss()
                                                    onOpenPlayer()
                                                },
                                                tint = MaterialTheme.colorScheme.primary,
                                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                                                modifier = Modifier.testTag("btn_open_player_queue_${song.id}")
                                            )
                                        } else {
                                            Text(
                                                text = formatCardDuration(song.duration),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFeatureSettings = "tnum"
                                                ),
                                                color = if (isCurrent) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.80f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CarouselPlaylistBottomSheet(
    playlist: Playlist,
    currentSong: Song?,
    activePlaylistId: Long?,
    isPlaying: Boolean,
    onPlayPlaylist: (Playlist) -> Unit,
    onSelectSong: (Song) -> Unit,
    onOpenPlayer: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetShape = remember { RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val isThisPlaylistActive = activePlaylistId == playlist.id

    var isSearchExpanded by remember(playlist.id) { mutableStateOf(false) }
    var searchQuery by remember(playlist.id) { mutableStateOf("") }
    var sheetRootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var searchCapsuleBoundsInRoot by remember { mutableStateOf(Rect.Zero) }

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

    LaunchedEffect(currentSong?.id, playlist.songs.size, isThisPlaylistActive) {
        if (isThisPlaylistActive) {
            val currentIdx = currentSong?.let { cur -> playlist.songs.indexOfFirst { it.id == cur.id } } ?: -1
            if (currentIdx > 1) {
                listState.scrollToItem((currentIdx - 1).coerceAtLeast(0))
            }
        }
    }

    val songCountLabel = stringResource(R.string.playlist_songs_count_format, playlist.songs.size)
    val durationLabel = remember(playlist.totalDurationMs) {
        formatCardDuration(playlist.totalDurationMs)
    }
    val subtitle = "$songCountLabel • $durationLabel"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LocalizedAppScope {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
                    .collapseSheetSearchOnOutsideTap(
                        isSearchExpanded = isSearchExpanded,
                        onUpdateRootCoords = { sheetRootCoords = it },
                        rootCoordsProvider = { sheetRootCoords },
                        searchBoundsInRootProvider = { searchCapsuleBoundsInRoot },
                        onCollapse = collapseSearch
                    )
                    .testTag("sheet_carousel_playlist")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("icon_playlist_carousel_sheet")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    ExpandableSheetHeaderSearch(
                        title = playlist.name,
                        subtitle = subtitle,
                        titleTestTag = "sheet_carousel_playlist_title",
                        subtitleTestTag = "sheet_carousel_playlist_subtitle",
                        showSearchButton = playlist.songs.isNotEmpty(),
                        isSearchExpanded = isSearchExpanded,
                        onExpandSearch = { isSearchExpanded = true },
                        onCollapseSearch = collapseSearch,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        searchButtonTestTag = "btn_search_carousel_sheet",
                        searchInputTestTag = "input_search_carousel_sheet",
                        closeButtonTestTag = "btn_close_search_carousel_sheet",
                        onUpdateSearchBoundsInRoot = { searchCapsuleBoundsInRoot = it },
                        modifier = Modifier.weight(1f)
                    )

                    if (playlist.songs.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val isPlayingThisPlaylist = isThisPlaylistActive && isPlaying
                        FilledTonalIconButton(
                            onClick = { onPlayPlaylist(playlist) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("btn_sheet_play_playlist_${playlist.id}")
                        ) {
                            Icon(
                                imageVector = if (isPlayingThisPlaylist) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play_pause),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (playlist.songs.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 16.dp)
                                .testTag("sheet_carousel_playlist_empty")
                        ) {
                            Text(
                                text = stringResource(R.string.playlist_detail_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (filteredSongs.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 28.dp)
                                .testTag("carousel_sheet_no_search_results")
                        ) {
                            Text(
                                text = stringResource(R.string.no_search_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                                .padding(vertical = 6.dp)
                        ) {
                            itemsIndexed(
                                items = filteredSongs,
                                key = { _, song -> song.id }
                            ) { index, song ->
                                val isCurrent = currentSong?.id == song.id
                                val rowCorner by animateDpAsState(
                                    targetValue = resolveQueueRowCornerTarget(isCurrent),
                                    animationSpec = spring(
                                        dampingRatio = 0.56f,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "carousel_sheet_row_corner_${song.id}"
                                )
                                val rowColor by animateColorAsState(
                                    targetValue = if (isCurrent) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "carousel_sheet_row_bg_${song.id}"
                                )
                                val rowRadius = rowCorner.coerceAtLeast(0.dp)
                                val rowShape = remember(rowRadius) { RoundedCornerShape(rowRadius) }
                                Surface(
                                    shape = rowShape,
                                    color = rowColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .clip(rowShape)
                                        .clickable {
                                            onSelectSong(song)
                                        }
                                        .testTag("carousel_sheet_song_row_${song.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 10.dp, end = 12.dp, top = 9.dp, bottom = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .testTag("carousel_sheet_song_index_${song.id}")
                                        ) {
                                            if (isCurrent) {
                                                AudioWaveformEqualizer(
                                                    isPlaying = isPlaying,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(width = 16.dp, height = 14.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 12.5.sp,
                                                        fontFeatureSettings = "tnum"
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Text(
                                            text = song.displayTitle,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isCurrent) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        if (isCurrent && onOpenPlayer != null) {
                                            SelectedSongOpenPlayerButton(
                                                onClick = {
                                                    onDismiss()
                                                    onOpenPlayer()
                                                },
                                                tint = MaterialTheme.colorScheme.primary,
                                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                                                modifier = Modifier.testTag("btn_open_player_carousel_${song.id}")
                                            )
                                        } else {
                                            Text(
                                                text = formatCardDuration(song.duration),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFeatureSettings = "tnum"
                                                ),
                                                color = if (isCurrent) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.80f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

internal fun resolveQueueRowCornerTarget(isCurrent: Boolean): Dp =
    if (isCurrent) 16.dp else 10.dp

internal fun matchesSongSearchQuery(
    song: Song,
    rawQuery: String,
    preNormalizedQuery: String? = null
): Boolean {
    val query = rawQuery.trim()
    if (query.isEmpty()) return true
    if (song.displayTitle.contains(query, ignoreCase = true) ||
        song.title.contains(query, ignoreCase = true)
    ) {
        return true
    }
    val normQuery = preNormalizedQuery ?: Song.normalizeForSearch(query)
    if (normQuery.isEmpty()) return true
    return song.normalizedDisplayTitle.contains(normQuery) ||
        song.normalizedRawTitle.contains(normQuery)
}



