package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.Id3v2LyricsExtractor
import com.example.data.LrcLibRemoteClient
import com.example.data.LyricsRepository
import com.example.data.OfflineSpeechLyricsEngine
import com.example.data.PlaylistRepository
import com.example.data.RemoteLyricsResponse
import com.example.data.local.AppDatabase
import com.example.model.LyricLine
import com.example.model.LyricsSource
import com.example.model.LyricsUiState
import com.example.model.OfflineModelState
import com.example.model.PlayMode
import com.example.model.PlaybackState
import com.example.model.Playlist
import com.example.model.Song
import com.example.model.calculateLineProgress
import com.example.service.PlaybackManager
import com.example.ui.AddSongsToPlaylistBottomSheet
import com.example.ui.AddToPlaylistBottomSheet
import com.example.ui.AudioEffectsBottomSheet
import com.example.ui.BottomNowPlayingPill
import com.example.ui.CapsuleBottomToolbar
import com.example.ui.CarouselPlaylistBottomSheet
import com.example.ui.CurrentQueueBottomSheet
import com.example.ui.HoaCaScreen
import com.example.ui.INACTIVITY_AUTO_COLLAPSE_TIMEOUT_MS
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.PlaylistCarouselCard
import com.example.ui.PlaylistDetailScreen
import com.example.ui.PlaylistsOverviewScreen
import com.example.ui.SettingsBottomSheet
import com.example.ui.SongList
import com.example.ui.WelcomeScreen
import com.example.ui.matchesSongSearchQuery
import com.example.ui.observeUserTouch
import com.example.ui.resolveQueueRowCornerTarget
import com.example.ui.resolveSongCardCornerTarget
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import java.io.ByteArrayInputStream
import java.io.File
import com.example.ui.theme.AppThemeColor
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeMode
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Music Player", appName)
    }

    @Test
    fun `song displayTitle removes mp3 extension with real android Uri`() {
        val song = Song(
            id = 100L,
            title = "Bai_Hat_Viet_Nam.mp3",
            uri = Uri.parse("content://media/external/audio/media/100"),
            duration = 240000L,
            dateAdded = 123456L
        )
        assertEquals("Bai_hat_viet_nam", song.displayTitle)
    }

    @Test
    fun `song displayTitle trims trailing spaces before uppercase extension`() {
        val song = Song(
            id = 101L,
            title = "  nhac hay   .MP3  ",
            uri = Uri.parse("content://media/external/audio/media/101"),
            duration = 180000L,
            dateAdded = 123457L
        )
        assertEquals("Nhac hay", song.displayTitle)
    }

    @Test
    fun `playbackManager scopes next and previous to active library or playlist queue`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = PlaybackManager.getInstance(context)
        val librarySongs = (1L..8L).map { id ->
            Song(id, "song_$id.mp3", Uri.parse("content://media/$id"), 120_000L, id)
        }
        manager.setSongs(librarySongs)

        while (manager.playbackState.value.playMode != PlayMode.REPEAT_ALL) {
            manager.cyclePlayMode()
        }

        // 1. Play from full library (REPEAT_ALL): Next and Previous step in list order;
        //    Previous immediately jumps to previous song even when currentPositionMs > 0 (never restarts current song)
        manager.playSongFromLibrary(librarySongs[1]) // song 2
        assertNull(manager.playbackState.value.activePlaylistId)
        assertEquals((1L..8L).toList(), manager.getOrderedSongs().map { it.id })
        manager.updateProgress(45_000L, 120_000L)
        manager.playPrevious()
        assertEquals(1L, manager.playbackState.value.currentSong?.id)
        assertEquals(0L, manager.currentPositionMs.value)

        // Wrap-around Previous in REPEAT_ONE also jumps immediately to previous song instead of restarting current song
        while (manager.playbackState.value.playMode != PlayMode.REPEAT_ONE) {
            manager.cyclePlayMode()
        }
        manager.updateProgress(60_000L, 120_000L)
        manager.playPrevious()
        assertEquals(8L, manager.playbackState.value.currentSong?.id)

        // 2. Switch to SHUFFLE: both Next and Previous jump randomly within the active queue and never replay the same song
        while (manager.playbackState.value.playMode != PlayMode.SHUFFLE) {
            manager.cyclePlayMode()
        }
        val visitedIds = mutableListOf<Long>()
        var prevId = manager.playbackState.value.currentSong!!.id
        repeat(4) {
            manager.updateProgress(30_000L, 120_000L)
            manager.playNext()
            val curId = manager.playbackState.value.currentSong!!.id
            assertTrue(prevId != curId)
            visitedIds.add(curId)
            prevId = curId
        }
        repeat(4) {
            manager.updateProgress(30_000L, 120_000L)
            manager.playPrevious()
            val curId = manager.playbackState.value.currentSong!!.id
            assertTrue(prevId != curId)
            visitedIds.add(curId)
            prevId = curId
        }
        assertTrue(visitedIds.all { it in 1L..8L })

        // 3. Switch to a Playlist queue containing only songs [4L, 2L]
        val playlistSongs = listOf(librarySongs[3], librarySongs[1])
        manager.playSongFromPlaylist(playlistId = 99L, playlistSongs = playlistSongs, song = librarySongs[3])
        assertEquals(99L, manager.playbackState.value.activePlaylistId)
        assertEquals(listOf(4L, 2L), manager.getOrderedSongs().map { it.id })
        assertEquals(4L, manager.playbackState.value.currentSong?.id)

        // Next/Previous stay inside playlist queue [4L, 2L] and always jump to the other song
        manager.playNext()
        assertEquals(2L, manager.playbackState.value.currentSong?.id)
        manager.playNext()
        assertEquals(4L, manager.playbackState.value.currentSong?.id)
        manager.updateProgress(50_000L, 120_000L)
        manager.playPrevious()
        assertEquals(2L, manager.playbackState.value.currentSong?.id)

        // 4. Rename updates song title across both library and active playlist queue
        manager.onSongRenamed(2L, "renamed_song.mp3")
        assertEquals("Renamed_song", manager.getOrderedSongs().first { it.id == 2L }.displayTitle)

        // 5. Switching back to Library restores full library queue
        manager.playSongFromLibrary(librarySongs[0])
        assertNull(manager.playbackState.value.activePlaylistId)
        assertEquals((1L..8L).toList(), manager.getOrderedSongs().map { it.id })
    }

    @Test
    fun `playlistRepository creates playlists and deletes references without touching library songs`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = PlaylistRepository(db.playlistDao())
            val song1 = Song(10L, "alpha.mp3", Uri.parse("content://media/10"), 120_000L, 10L)
            val song2 = Song(20L, "beta.mp3", Uri.parse("content://media/20"), 180_000L, 20L)
            val songsFlow = MutableStateFlow(listOf(song1, song2))

            val playlistId = repo.createPlaylist("Nhạc Chill")
            val playlistId2 = repo.createPlaylist("Nhạc Lofi")
            repo.addSongToPlaylists(song1.id, listOf(playlistId))
            repo.addSongsToPlaylist(playlistId, listOf(song2.id))

            val resolved = repo.observePlaylistsWithSongs(songsFlow)
                .first { it.size == 2 && it.first().songs.size == 2 }
            assertEquals("Nhạc Chill", resolved.first().name)
            assertEquals(listOf(playlistId, playlistId2), resolved.map { it.id })
            assertEquals(listOf(10L, 20L), resolved.first().songs.map { it.id })
            assertEquals(300_000L, resolved.first().totalDurationMs)

            // Rename playlist
            repo.renamePlaylist(playlistId, "Chill Tối")
            val afterRename = repo.observePlaylistsWithSongs(songsFlow)
                .first { it.firstOrNull { p -> p.id == playlistId }?.name == "Chill Tối" }
            assertEquals("Chill Tối", afterRename.first { it.id == playlistId }.name)

            // Reorder playlists persistently
            repo.reorderPlaylists(listOf(playlistId2, playlistId))
            val afterReorder = repo.observePlaylistsWithSongs(songsFlow)
                .first { it.map { p -> p.id } == listOf(playlistId2, playlistId) }
            assertEquals(listOf(playlistId2, playlistId), afterReorder.map { it.id })

            // Increment playCount persistently
            repo.incrementPlayCount(playlistId2)
            repo.incrementPlayCount(playlistId2)
            val afterPlayCount = repo.observePlaylistsWithSongs(songsFlow)
                .first { it.first { p -> p.id == playlistId2 }.playCount == 2 }
            assertEquals(2, afterPlayCount.first { it.id == playlistId2 }.playCount)
            assertEquals(0, afterPlayCount.first { it.id == playlistId }.playCount)

            // Removing a song from playlist only deletes the reference; library songs remain untouched
            repo.removeSongFromPlaylist(playlistId, song1.id)
            val afterSongRemove = repo.observePlaylistsWithSongs(songsFlow)
                .first { it.first { p -> p.id == playlistId }.songs.size == 1 }
            assertEquals(listOf(20L), afterSongRemove.first { it.id == playlistId }.songs.map { it.id })
            assertEquals(2, songsFlow.value.size)

            // Deleting the playlist keeps all library songs untouched
            repo.deletePlaylist(playlistId)
            repo.deletePlaylist(playlistId2)
            val afterPlaylistDelete = repo.observePlaylistsWithSongs(songsFlow)
                .first { it.isEmpty() }
            assertEquals(0, afterPlaylistDelete.size)
            assertEquals(2, songsFlow.value.size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `bottom control zone spans full bottom 3 edges and blocks gap taps across both tiers`() {
        var backgroundClicks = 0
        var playModeClicks = 0
        var playPauseClicks = 0
        var selectedTab by mutableStateOf(MainTab.SONGS)
        val sampleSong = Song(1L, "Track.mp3", Uri.parse("content://media/1"), 120_000L, 1L, artist = "Artist")
        val posFlow = MutableStateFlow(10_000L)

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("root_container")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { backgroundClicks++ }
                    )
                    CapsuleBottomToolbar(
                        currentTab = selectedTab,
                        onSelectTab = { selectedTab = it },
                        playbackState = PlaybackState(currentSong = sampleSong, isPlaying = false, durationMs = 120_000L),
                        currentPositionMsFlow = posFlow,
                        isDarkTheme = false,
                        onCyclePlayMode = { playModeClicks++ },
                        onTogglePlayPause = { playPauseClicks++ },
                        onSeek = {},
                        onOpenQueue = {},
                        onOpenSettings = {},
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        // Equalizer button is no longer on main bottom toolbar
        composeTestRule.onNodeWithTag("btn_bass_boost_toggle").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_now_playing_pill").assertIsDisplayed()

        val rootBounds = composeTestRule.onNodeWithTag("root_container").fetchSemanticsNode().boundsInRoot
        val zoneNode = composeTestRule.onNodeWithTag("bottom_control_zone")
        val zoneBounds = zoneNode.fetchSemanticsNode().boundsInRoot

        // Verify flush against left, right, and bottom edges
        assertEquals(rootBounds.left, zoneBounds.left, 1f)
        assertEquals(rootBounds.right, zoneBounds.right, 1f)
        assertEquals(rootBounds.bottom, zoneBounds.bottom, 1f)

        val pillBounds = composeTestRule.onNodeWithTag("bottom_now_playing_pill").fetchSemanticsNode().boundsInRoot
        val navPillBounds = composeTestRule.onNodeWithTag("nav_pill_menu").fetchSemanticsNode().boundsInRoot
        val settingsBounds = composeTestRule.onNodeWithTag("btn_settings").fetchSemanticsNode().boundsInRoot

        val betweenTiersY = (pillBounds.bottom + navPillBounds.top) / 2f
        val navSettingsGapX = (navPillBounds.right + settingsBounds.left) / 2f
        val navSettingsGapY = settingsBounds.center.y

        // Tap in gaps between Tier 1 & Tier 2, between Nav pill & Settings, and on 3 edges
        zoneNode.performTouchInput {
            click(Offset(zoneBounds.width / 2f, betweenTiersY - zoneBounds.top))
            click(Offset(navSettingsGapX - zoneBounds.left, navSettingsGapY - zoneBounds.top))
            click(Offset(2f, zoneBounds.height / 2f))
            click(Offset(zoneBounds.width - 2f, zoneBounds.height / 2f))
            click(Offset(zoneBounds.width / 2f, zoneBounds.height - 2f))
        }

        assertEquals(0, backgroundClicks)
        assertEquals(0, playModeClicks)
        assertEquals(0, playPauseClicks)
        assertEquals(MainTab.SONGS, selectedTab)

        // Only the active SONGS tab displays its icon initially
        composeTestRule.onNodeWithTag("nav_tab_icon_songs", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_icon_playlists", useUnmergedTree = true).assertDoesNotExist()

        // Real touch taps on Tier 1 controls (Play/Pause & PlayMode) and Tier 2 tabs work
        composeTestRule.onNodeWithTag("btn_play_pause").performTouchInput { click() }
        composeTestRule.onNodeWithTag("btn_play_mode").performTouchInput { click() }
        assertEquals(1, playPauseClicks)
        assertEquals(1, playModeClicks)

        composeTestRule.onNodeWithTag("nav_tab_playlists").performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainTab.PLAYLISTS, selectedTab)
        assertEquals(0, backgroundClicks)

        // After switching to PLAYLISTS, only PLAYLISTS displays its icon
        composeTestRule.onNodeWithTag("nav_tab_icon_playlists", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_icon_songs", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `bottom now playing pill renders title active playlist and capsule play pause without artist`() {
        val songs = listOf(
            Song(1L, "First Song.mp3", Uri.parse("content://media/1"), 120_000L, 1L, artist = "Hidden Artist"),
            Song(2L, "Second Song.mp3", Uri.parse("content://media/2"), 180_000L, 2L)
        )
        val posFlow = MutableStateFlow(30_000L)
        var toggleClicks = 0
        var cycleModeClicks = 0
        var addToPlaylistSong: Song? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    SongList(
                        songs = songs,
                        playbackState = PlaybackState(currentSong = songs[0], isPlaying = true, durationMs = 120_000L),
                        onSongClick = {},
                        onAddToPlaylist = { addToPlaylistSong = it },
                        onRenameSong = { _, _ -> },
                        onDeleteSong = {},
                        contentTopPadding = 12.dp,
                        contentBottomPadding = 144.dp,
                        isDarkTheme = false
                    )
                    BottomNowPlayingPill(
                        song = songs[0],
                        activePlaylistName = "Chill Vibes",
                        isPlaying = true,
                        playMode = PlayMode.REPEAT_ONE,
                        durationMs = 120_000L,
                        currentPositionMsFlow = posFlow,
                        onTogglePlayPause = { toggleClicks++ },
                        onCyclePlayMode = { cycleModeClicks++ },
                        onSeek = {},
                        isDarkTheme = false,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("bottom_now_playing_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_now_playing_title", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_now_playing_queue", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("00:30 - 02:00 • Chill Vibes", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_now_playing_artist", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Hidden Artist", useUnmergedTree = true).assertDoesNotExist()

        // Left button = Play/Pause capsule (wider than tall), Right button = external Repeat / PlayMode circle outside right edge of pill
        val pillBounds = composeTestRule.onNodeWithTag("bottom_now_playing_pill").fetchSemanticsNode().boundsInRoot
        val playBtnBounds = composeTestRule.onNodeWithTag("btn_play_pause").fetchSemanticsNode().boundsInRoot
        val modeBtnBounds = composeTestRule.onNodeWithTag("btn_play_mode").fetchSemanticsNode().boundsInRoot
        assertTrue(playBtnBounds.width > playBtnBounds.height)
        assertTrue(playBtnBounds.right < modeBtnBounds.left)
        assertTrue(playBtnBounds.top > pillBounds.top && playBtnBounds.bottom < pillBounds.bottom)
        assertTrue(modeBtnBounds.left >= pillBounds.right)
        assertTrue(abs(modeBtnBounds.height - pillBounds.height) < 2.5f)
        assertTrue(abs(modeBtnBounds.width - modeBtnBounds.height) < 2.5f)

        composeTestRule.onNodeWithTag("btn_play_pause").performClick()
        composeTestRule.onNodeWithTag("btn_play_mode").performClick()
        assertEquals(1, toggleClicks)
        assertEquals(1, cycleModeClicks)

        // Both cards in SongList remain uniform height
        val card1Height = composeTestRule.onNodeWithTag("song_card_1").fetchSemanticsNode().size.height
        val card2Height = composeTestRule.onNodeWithTag("song_card_2").fetchSemanticsNode().size.height
        assertEquals(card2Height, card1Height)

        // Long pressing a song card reveals Add to Playlist action instead of Favorite/Pin
        composeTestRule.onNodeWithTag("song_card_2").performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("action_add_to_playlist_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("action_pin_2").assertDoesNotExist()
        composeTestRule.onNodeWithTag("action_add_to_playlist_2").performClick()
        assertEquals(2L, addToPlaylistSong?.id)
    }

    @Test
    fun `playlists overview and add to playlist sheet handle selection and quick create`() {
        val song1 = Song(1L, "Track One.mp3", Uri.parse("content://media/1"), 90_000L, 1L)
        val playlist1 = Playlist(id = 10L, name = "Favorites Mix", createdAt = 1L, songs = listOf(song1))
        val playlist2 = Playlist(id = 20L, name = "Road Trip", createdAt = 2L, songs = emptyList())
        val targetSong = Song(2L, "Track Two.mp3", Uri.parse("content://media/2"), 150_000L, 2L)

        var playedPlaylistId: Long? = null
        var confirmedPlaylistIds: List<Long> = emptyList()
        var quickCreatedName: String? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    PlaylistsOverviewScreen(
                        playlists = listOf(playlist1, playlist2),
                        playbackState = PlaybackState(),
                        onCreatePlaylist = {},
                        onOpenPlaylist = {},
                        onPlayPlaylist = { playedPlaylistId = it.id },
                        onRequestDeletePlaylist = {},
                        contentTopPadding = 12.dp,
                        contentBottomPadding = 148.dp
                    )
                    AddToPlaylistBottomSheet(
                        song = targetSong,
                        playlists = listOf(playlist1, playlist2),
                        onConfirmAdd = { confirmedPlaylistIds = it },
                        onQuickCreatePlaylist = { quickCreatedName = it },
                        onDismiss = {}
                    )
                }
            }
        }

        // Play button inside AddToPlaylist sheet flow: select Road Trip (id 20L) and confirm
        composeTestRule.onNodeWithTag("sheet_add_to_playlist").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_confirm_add_to_playlists").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("playlist_select_row_20").performClick()
        composeTestRule.onNodeWithTag("btn_confirm_add_to_playlists").assertIsEnabled()
        composeTestRule.onNodeWithTag("btn_confirm_add_to_playlists").performClick()
        assertEquals(listOf(20L), confirmedPlaylistIds)

        // Quick create playlist inline
        composeTestRule.onNodeWithTag("btn_quick_create_playlist").performClick()
        composeTestRule.onNodeWithTag("input_quick_playlist_name").performTextInput("Gym Hits")
        composeTestRule.onNodeWithTag("btn_submit_quick_playlist").performClick()
        assertEquals("Gym Hits", quickCreatedName)
    }

    @Test
    fun `playlists overview screen displays 2-column square grid left-to-right top-to-bottom with full actions`() {
        val song1 = Song(1L, "Track One.mp3", Uri.parse("content://media/1"), 90_000L, 1L)
        val song2 = Song(2L, "Track Two.mp3", Uri.parse("content://media/2"), 150_000L, 2L)
        val playlist1 = Playlist(id = 10L, name = "Focus Flow", createdAt = 1L, songs = listOf(song1))
        val playlist2 = Playlist(id = 20L, name = "Night Drive", createdAt = 2L, songs = emptyList())
        val playlist3 = Playlist(id = 30L, name = "Morning Coffee", createdAt = 3L, songs = listOf(song1, song2))
        var playedPlaylist: Playlist? = null
        var openedPlaylist: Playlist? = null
        var deletedPlaylist: Playlist? = null
        var reorderedIds: List<Long> = emptyList()

        composeTestRule.setContent {
            MyApplicationTheme {
                PlaylistsOverviewScreen(
                    playlists = listOf(playlist1, playlist2, playlist3),
                    playbackState = PlaybackState(activePlaylistId = 10L, isPlaying = true),
                    onCreatePlaylist = {},
                    onOpenPlaylist = { openedPlaylist = it },
                    onPlayPlaylist = { playedPlaylist = it },
                    onRequestDeletePlaylist = { deletedPlaylist = it },
                    onReorderPlaylists = { reorderedIds = it },
                    contentTopPadding = 12.dp,
                    contentBottomPadding = 148.dp
                )
            }
        }

        composeTestRule.onNodeWithTag("playlists_lazy_grid").assertIsDisplayed()
        composeTestRule.onNodeWithText("Focus Flow").assertIsDisplayed()

        val bounds1 = composeTestRule.onNodeWithTag("playlist_card_10").fetchSemanticsNode().boundsInRoot
        val bounds2 = composeTestRule.onNodeWithTag("playlist_card_20").fetchSemanticsNode().boundsInRoot
        val bounds3 = composeTestRule.onNodeWithTag("playlist_card_30").fetchSemanticsNode().boundsInRoot

        // Square aspect ratio check
        assertEquals(bounds1.width, bounds1.height, 1.5f)
        assertEquals(bounds2.width, bounds2.height, 1.5f)
        assertEquals(bounds3.width, bounds3.height, 1.5f)

        // Row 1 has 2 cards ordered left-to-right; Row 2 starts on the left below Row 1
        assertEquals(bounds1.top, bounds2.top, 1.5f)
        assertEquals(true, bounds1.right < bounds2.left)
        assertEquals(true, bounds3.top > bounds1.bottom)
        assertEquals(bounds1.left, bounds3.left, 1.5f)

        // Circular icon-only add playlist button check
        val createBtnBounds = composeTestRule.onNodeWithTag("btn_create_playlist").fetchSemanticsNode().boundsInRoot
        assertEquals(createBtnBounds.width, createBtnBounds.height, 1.5f)
        composeTestRule.onNodeWithText("Tạo mới").assertDoesNotExist()

        // Title sits below playlist icon and above play button; total duration is shown
        val iconBounds1 = composeTestRule.onNodeWithTag("playlist_card_icon_10", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val titleBounds1 = composeTestRule.onNodeWithTag("playlist_card_title_10", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val playBounds1 = composeTestRule.onNodeWithTag("btn_play_playlist_10", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(titleBounds1.top >= iconBounds1.bottom)
        assertTrue(titleBounds1.bottom <= playBounds1.top)
        composeTestRule.onNodeWithTag("playlist_card_duration_10", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("01:30", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("04:00", useUnmergedTree = true).assertIsDisplayed()

        // Long pressing & dragging card 20 onto card 10 locks target by playlist.id without grabbing wrong card
        val card20Node = composeTestRule.onNodeWithTag("playlist_card_20")
        val card20Bounds = card20Node.fetchSemanticsNode().boundsInRoot
        card20Node.performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 150L)
            moveTo(Offset(bounds1.center.x - card20Bounds.left, bounds1.center.y - card20Bounds.top))
            up()
        }
        composeTestRule.waitForIdle()
        assertNull(deletedPlaylist)
        assertEquals(listOf(20L, 10L, 30L), reorderedIds)

        // Also drag card 30 onto slot 1 (currently occupied by card 20) -> verifies card 30 is dragged, not card 10
        val card30Node = composeTestRule.onNodeWithTag("playlist_card_30")
        val card30Bounds = card30Node.fetchSemanticsNode().boundsInRoot
        card30Node.performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 150L)
            moveTo(Offset(bounds1.center.x - card30Bounds.left, bounds1.center.y - card30Bounds.top))
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(listOf(30L, 20L, 10L), reorderedIds)

        composeTestRule.onNodeWithTag("btn_play_playlist_10").performClick()
        assertEquals(10L, playedPlaylist?.id)

        composeTestRule.onNodeWithTag("btn_delete_playlist_20").performClick()
        assertEquals(20L, deletedPlaylist?.id)

        composeTestRule.onNodeWithTag("playlist_card_10").performClick()
        assertEquals(10L, openedPlaylist?.id)
    }

    @Test
    fun `playlist detail screen allows editing playlist name by clicking its title without pencil icon and shows circular add button`() {
        val song1 = Song(1L, "Track One.mp3", Uri.parse("content://media/1"), 90_000L, 1L)
        val playlist = Playlist(id = 10L, name = "Old Mix", createdAt = 1L, songs = listOf(song1))
        var renamedTo: String? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                PlaylistDetailScreen(
                    playlist = playlist,
                    playbackState = PlaybackState(),
                    onBack = {},
                    onPlayAll = {},
                    onAddSongs = {},
                    onRenamePlaylist = { renamedTo = it },
                    onSongClick = {},
                    onRemoveSongFromPlaylist = {},
                    contentTopPadding = 12.dp,
                    contentBottomPadding = 148.dp,
                    isDarkTheme = false
                )
            }
        }

        // Circular icon-only add songs button check
        val addBtnBounds = composeTestRule.onNodeWithTag("btn_add_songs_to_playlist").fetchSemanticsNode().boundsInRoot
        assertEquals(addBtnBounds.width, addBtnBounds.height, 1.5f)
        composeTestRule.onNodeWithText("Thêm bài hát").assertDoesNotExist()

        // No pencil edit icon inside title button
        composeTestRule.onNodeWithTag("btn_rename_playlist").performClick()
        composeTestRule.onNodeWithTag("input_rename_playlist_name").performTextReplacement("Evening Beats")
        composeTestRule.onNodeWithTag("btn_confirm_rename_playlist").performClick()
        assertEquals("Evening Beats", renamedTo)
    }

    @Test
    fun `welcome screen renders MD3 elements and triggers start action`() {
        var started = false

        composeTestRule.setContent {
            MyApplicationTheme {
                WelcomeScreen(onStartClick = { started = true })
            }
        }

        composeTestRule.onNodeWithTag("welcome_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_hero_visual").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_subtitle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nghe ngoại tuyến").assertIsDisplayed()
        composeTestRule.onNodeWithText("Danh sách phát").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bộ chỉnh âm").assertIsDisplayed()

        val ctaBounds = composeTestRule.onNodeWithTag("btn_welcome_start").fetchSemanticsNode().boundsInRoot
        assertTrue(ctaBounds.height > 48f)
        composeTestRule.onNodeWithTag("btn_welcome_start").performClick()
        assertTrue(started)
    }

    @Test
    fun `mainViewModel persists has_seen_onboarding across launches`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences("music_player_theme_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove(MainViewModel.KEY_HAS_SEEN_ONBOARDING).commit()

        val firstLaunchVm = MainViewModel(app)
        assertFalse(firstLaunchVm.hasSeenOnboarding.value)

        firstLaunchVm.completeOnboarding()
        assertTrue(firstLaunchVm.hasSeenOnboarding.value)
        assertTrue(prefs.getBoolean(MainViewModel.KEY_HAS_SEEN_ONBOARDING, false))

        val secondLaunchVm = MainViewModel(app)
        assertTrue(secondLaunchVm.hasSeenOnboarding.value)
    }

    @Test
    fun `bottom now playing pill long press opens current queue sheet and allows switching songs`() {
        val song1 = Song(1L, "First.mp3", Uri.parse("content://media/1"), 120_000L, 1L)
        val song2 = Song(2L, "Second.mp3", Uri.parse("content://media/2"), 180_000L, 2L)
        val posFlow = MutableStateFlow(15_000L)
        var showQueue by mutableStateOf(false)
        var selectedQueueSongId: Long? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    BottomNowPlayingPill(
                        song = song1,
                        isPlaying = true,
                        playMode = PlayMode.REPEAT_ALL,
                        durationMs = 120_000L,
                        currentPositionMsFlow = posFlow,
                        onTogglePlayPause = {},
                        onCyclePlayMode = {},
                        onSeek = {},
                        onOpenQueue = { showQueue = true },
                        isDarkTheme = false,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                    if (showQueue) {
                        CurrentQueueBottomSheet(
                            queueSongs = listOf(song1, song2),
                            currentSong = song1,
                            isPlaying = true,
                            activePlaylistName = "Road Trip",
                            onSelectSong = { selectedQueueSongId = it.id },
                            onDismiss = { showQueue = false }
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("bottom_now_playing_pill").performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sheet_current_queue").assertIsDisplayed()
        composeTestRule.onNodeWithTag("queue_song_row_2").performClick()
        assertEquals(2L, selectedQueueSongId)
    }

    @Test
    fun `settings bottom sheet integrates equalizer inline below appearance and places exit button in header`() {
        var bass by mutableIntStateOf(800)
        var treble by mutableIntStateOf(600)
        var vocal by mutableIntStateOf(500)
        var spatial by mutableIntStateOf(400)
        var themeMode by mutableStateOf(ThemeMode.SYSTEM)

        composeTestRule.setContent {
            MyApplicationTheme {
                SettingsBottomSheet(
                    isDarkTheme = false,
                    selectedThemeMode = themeMode,
                    selectedThemeColor = AppThemeColor.DEFAULT,
                    onSelectThemeMode = { themeMode = it },
                    onSelectThemeColor = {},
                    onToggleTheme = {},
                    bassStrength = bass,
                    trebleStrength = treble,
                    vocalStrength = vocal,
                    virtualizerStrength = spatial,
                    onBassChange = { v, _ -> bass = v },
                    onTrebleChange = { v, _ -> treble = v },
                    onVocalChange = { v, _ -> vocal = v },
                    onVirtualizerChange = { v, _ -> spatial = v },
                    onExitApp = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("card_setting_about").assertExists()
        composeTestRule.onNodeWithTag("card_setting_clear_cache").assertDoesNotExist()
        composeTestRule.onNodeWithTag("card_setting_equalizer").assertDoesNotExist()
        composeTestRule.onNodeWithTag("btn_equalizer_back").assertDoesNotExist()

        // Settings sheet height wraps content when items are few and caps at a maximum of 3/4 (75%) of the screen height
        val panelBounds = composeTestRule.onNodeWithTag("panel_settings")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(panelBounds.height <= panelBounds.bottom * 0.75f + 2.0f)
        assertTrue(panelBounds.height > 150f)

        // Settings header removes the small subtitle under the title
        composeTestRule.onNodeWithText("Giao diện, màu sắc & hệ thống").assertDoesNotExist()

        // System / Light / Dark 3-way theme mode selector uses M3 Split Buttons without border and shows "Tự động theo hệ thống"
        composeTestRule.onNodeWithTag("theme_mode_split_button_group").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tự động theo hệ thống").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_mode_dark").performClick()
        assertEquals(ThemeMode.DARK, themeMode)
        composeTestRule.onNodeWithTag("theme_mode_light").performClick()
        assertEquals(ThemeMode.LIGHT, themeMode)
        composeTestRule.onNodeWithTag("theme_mode_system").performClick()
        assertEquals(ThemeMode.SYSTEM, themeMode)

        // Exit button is horizontally aligned on the right of the Settings title header row
        val titleBounds = composeTestRule.onNodeWithTag("settings_title_text", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val exitBounds = composeTestRule.onNodeWithTag("btn_setting_exit_app")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(titleBounds.right < exitBounds.left)
        assertTrue(exitBounds.top < titleBounds.bottom && exitBounds.bottom > titleBounds.top)

        // Custom audio/volume adjuster panel is completely removed from SettingsBottomSheet
        composeTestRule.onNodeWithTag("panel_audio_effects").assertDoesNotExist()
        composeTestRule.onNodeWithTag("card_setting_about").performScrollTo()
        composeTestRule.waitForIdle()
        val themeCardBounds = composeTestRule.onNodeWithTag("card_setting_theme_color")
            .fetchSemanticsNode().boundsInRoot
        val aboutCardBounds = composeTestRule.onNodeWithTag("card_setting_about")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(aboutCardBounds.top >= themeCardBounds.bottom)
    }

    @Test
    fun `mainViewModel follows system theme when ThemeMode is SYSTEM and persists explicit modes`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences("music_player_theme_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove(MainViewModel.KEY_THEME_MODE).remove("pref_is_dark_theme").commit()

        val vm = MainViewModel(app)
        assertEquals(ThemeMode.SYSTEM, vm.themeMode.value)

        vm.onSystemDarkChanged(true)
        assertTrue(vm.isDarkTheme.value)
        vm.onSystemDarkChanged(false)
        assertFalse(vm.isDarkTheme.value)

        vm.setThemeMode(ThemeMode.DARK)
        assertTrue(vm.isDarkTheme.value)
        vm.onSystemDarkChanged(false)
        assertTrue(vm.isDarkTheme.value)

        val reloadedVm = MainViewModel(app)
        assertEquals(ThemeMode.DARK, reloadedVm.themeMode.value)
        assertTrue(reloadedVm.isDarkTheme.value)
    }

    @Test
    fun `carousel card adapts layout cleanly across large medium and small widths without clipping`() {
        val song1 = Song(1L, "Track.mp3", Uri.parse("content://media/1"), 150_000L, 1L)
        val playlist = Playlist(id = 77L, name = "Nhạc 9x Bất Hủ Tuyển Chọn", createdAt = 1L, songs = listOf(song1))
        var widthDp by mutableFloatStateOf(186f)

        composeTestRule.setContent {
            val density = LocalDensity.current
            val widthPx = with(density) { widthDp.dp.toPx() }
            val heightPx = with(density) { 152.dp.toPx() }
            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    PlaylistCarouselCard(
                        playlist = playlist,
                        isActiveQueue = false,
                        isPlaying = false,
                        maskRectProvider = { Rect(0f, 0f, widthPx, heightPx) },
                        itemSizeProvider = { widthPx },
                        onClick = {},
                        onPlayClick = {},
                        modifier = Modifier
                            .width(widthDp.dp)
                            .height(152.dp)
                    )
                }
            }
        }

        // Large (186dp): all 4 elements are visible and fit inside card bounds
        composeTestRule.waitForIdle()
        val largeCardBounds = composeTestRule.onNodeWithTag("carousel_playlist_card_77").fetchSemanticsNode().boundsInRoot
        val largeTitleBounds = composeTestRule.onNodeWithTag("carousel_playlist_title_77", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val largePlayBounds = composeTestRule.onNodeWithTag("btn_carousel_play_playlist_77", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        composeTestRule.onNodeWithTag("carousel_playlist_count_77", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_playlist_duration_77", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(largeTitleBounds.right <= largeCardBounds.right)
        assertTrue(largePlayBounds.right <= largeCardBounds.right)

        // Medium (112dp): secondary metadata fades out; title and play button fit inside 112dp without clipping
        widthDp = 112f
        composeTestRule.waitForIdle()
        val medCardBounds = composeTestRule.onNodeWithTag("carousel_playlist_card_77").fetchSemanticsNode().boundsInRoot
        val medTitleBounds = composeTestRule.onNodeWithTag("carousel_playlist_title_77", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val medPlayBounds = composeTestRule.onNodeWithTag("btn_carousel_play_playlist_77", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(medTitleBounds.right <= medCardBounds.right)
        assertTrue(medPlayBounds.right <= medCardBounds.right)
        assertTrue(medPlayBounds.left >= medCardBounds.left)
        assertEquals(largePlayBounds.width, medPlayBounds.width, 0.5f)
        assertEquals(largePlayBounds.height, medPlayBounds.height, 0.5f)
        composeTestRule.onNodeWithTag("carousel_playlist_count_77", useUnmergedTree = true).assertIsNotDisplayed()

        // Small (52dp): title and metadata hidden; play button keeps constant size and bottom-right anchor
        widthDp = 52f
        composeTestRule.waitForIdle()
        val smallCardBounds = composeTestRule.onNodeWithTag("carousel_playlist_card_77").fetchSemanticsNode().boundsInRoot
        val smallPlayBounds = composeTestRule.onNodeWithTag("btn_carousel_play_playlist_77", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        composeTestRule.onNodeWithTag("carousel_playlist_title_77", useUnmergedTree = true).assertIsNotDisplayed()
        assertTrue(smallPlayBounds.left >= smallCardBounds.left)
        assertTrue(smallPlayBounds.right <= smallCardBounds.right)
        assertTrue(smallPlayBounds.bottom <= smallCardBounds.bottom)
        assertTrue(smallPlayBounds.center.y > smallCardBounds.center.y)
        assertEquals(largePlayBounds.width, smallPlayBounds.width, 0.5f)
        assertEquals(largePlayBounds.height, smallPlayBounds.height, 0.5f)
        assertEquals(largePlayBounds.bottom, smallPlayBounds.bottom, 1f)
    }

    @Test
    fun `song list displays MD3 playlist carousel with 4 minimalist items and independent play button`() {
        val song1 = Song(1L, "Alpha.mp3", Uri.parse("content://media/1"), 90_000L, 1L)
        val song2 = Song(2L, "Beta.mp3", Uri.parse("content://media/2"), 150_000L, 2L)
        val mostPlayed = Playlist(id = 20L, name = "Top Hits", createdAt = 2L, songs = listOf(song1, song2), playCount = 9)
        val lessPlayed = Playlist(id = 10L, name = "Chill Mix", createdAt = 1L, songs = listOf(song1), playCount = 2)

        var playedPlaylistId: Long? = null
        var openedPlaylistId: Long? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                SongList(
                    songs = listOf(song1, song2),
                    playlists = listOf(mostPlayed, lessPlayed),
                    playbackState = PlaybackState(),
                    onSongClick = {},
                    onCarouselPlaylistClick = { openedPlaylistId = it.id },
                    onCarouselPlayPlaylist = { playedPlaylistId = it.id },
                    onAddToPlaylist = {},
                    onRenameSong = { _, _ -> },
                    onDeleteSong = {},
                    contentTopPadding = 12.dp,
                    contentBottomPadding = 90.dp,
                    isDarkTheme = false
                )
            }
        }

        composeTestRule.onNodeWithTag("songs_playlists_carousel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Danh sách phát").assertDoesNotExist()
        composeTestRule.onNodeWithTag("carousel_playlist_title_20", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_playlist_count_20", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_playlist_duration_20", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("04:00", useUnmergedTree = true).assertIsDisplayed()

        // Carousel orders left-to-right matching supplied list (mostPlayed first)
        val cardTopHitsBounds = composeTestRule.onNodeWithTag("carousel_playlist_card_20").fetchSemanticsNode().boundsInRoot
        val cardChillBounds = composeTestRule.onNodeWithTag("carousel_playlist_card_10").fetchSemanticsNode().boundsInRoot
        assertTrue(cardTopHitsBounds.left < cardChillBounds.left)

        // Independent play button plays playlist without opening playlist details
        composeTestRule.onNodeWithTag("btn_carousel_play_playlist_20").performClick()
        assertEquals(20L, playedPlaylistId)
        assertNull(openedPlaylistId)

        // Tapping carousel card itself opens playlist details
        composeTestRule.onNodeWithTag("carousel_playlist_card_20").performClick()
        assertEquals(20L, openedPlaylistId)
    }

    @Test
    fun `audio effects bottom sheet displays minimalist controls and applies preset and reset`() {
        var bass by mutableIntStateOf(500)
        var treble by mutableIntStateOf(500)
        var vocal by mutableIntStateOf(500)
        var spatial by mutableIntStateOf(0)

        composeTestRule.setContent {
            MyApplicationTheme {
                AudioEffectsBottomSheet(
                    bassStrength = bass,
                    trebleStrength = treble,
                    vocalStrength = vocal,
                    virtualizerStrength = spatial,
                    onBassChange = { v, _ -> bass = v },
                    onTrebleChange = { v, _ -> treble = v },
                    onVocalChange = { v, _ -> vocal = v },
                    onVirtualizerChange = { v, _ -> spatial = v },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("panel_audio_effects").assertExists()
        composeTestRule.onNodeWithTag("slider_bass").assertExists()
        composeTestRule.onNodeWithTag("slider_treble").assertExists()
        composeTestRule.onNodeWithTag("slider_vocal").assertExists()
        composeTestRule.onNodeWithTag("slider_spatial").assertExists()

        composeTestRule.onNodeWithTag("preset_Siêu trầm").performClick()
        assertEquals(950, bass)
        assertEquals(650, treble)
        assertEquals(450, vocal)
        assertEquals(600, spatial)

        composeTestRule.onNodeWithTag("btn_reset_audio_state").performClick()
        assertEquals(800, bass)
        assertEquals(600, treble)
        assertEquals(500, vocal)
        assertEquals(400, spatial)
    }

    @Test
    fun `bottom now playing pill collapses smoothly morphs play pause to circle keeps external repeat tracking and locks scrubbing`() {
        val song = Song(1L, "Classic Hit.mp3", Uri.parse("content://media/1"), 120_000L, 1L)
        val posFlow = MutableStateFlow(30_000L)
        var seekCalls = 0
        var cycleModeCalls = 0

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    BottomNowPlayingPill(
                        song = song,
                        activePlaylistName = "Nhạc 9x",
                        isPlaying = true,
                        playMode = PlayMode.REPEAT_ALL,
                        durationMs = 120_000L,
                        currentPositionMsFlow = posFlow,
                        onTogglePlayPause = {},
                        onCyclePlayMode = { cycleModeCalls++ },
                        onSeek = { seekCalls++ },
                        isDarkTheme = false,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        // Expanded state: capsule Play/Pause, info reaches chevron, external circular Repeat to the right of pill
        composeTestRule.waitForIdle()
        val expandedPillBounds = composeTestRule.onNodeWithTag("bottom_now_playing_pill").fetchSemanticsNode().boundsInRoot
        val expandedPlayBounds = composeTestRule.onNodeWithTag("btn_play_pause").fetchSemanticsNode().boundsInRoot
        val expandedInfoBounds = composeTestRule.onNodeWithTag("bottom_now_playing_info", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val expandedTitleBounds = composeTestRule.onNodeWithTag("bottom_now_playing_title", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val expandedQueueBounds = composeTestRule.onNodeWithTag("bottom_now_playing_queue", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val expandedChevronBounds = composeTestRule.onNodeWithTag("btn_pill_collapse_expand").fetchSemanticsNode().boundsInRoot
        val expandedModeBounds = composeTestRule.onNodeWithTag("btn_play_mode").fetchSemanticsNode().boundsInRoot
        composeTestRule.onNodeWithTag("bottom_now_playing_title", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_now_playing_queue", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(expandedPlayBounds.width > expandedPlayBounds.height)
        assertTrue(expandedInfoBounds.right <= expandedChevronBounds.left + 1f)
        assertTrue((expandedChevronBounds.left - expandedInfoBounds.right) <= 2f)
        assertEquals(expandedInfoBounds.right, expandedTitleBounds.right, 1f)
        assertEquals(expandedInfoBounds.right, expandedQueueBounds.right, 1f)
        assertTrue(expandedModeBounds.left >= expandedPillBounds.right)

        composeTestRule.onNodeWithTag("bottom_now_playing_pill").performTouchInput { swipeRight() }
        assertTrue(seekCalls > 0)
        seekCalls = 0

        // Collapse via chevron button -> Play/Pause morphs into circle, external Repeat tracks inward on the right & stays clickable
        composeTestRule.onNodeWithTag("btn_pill_collapse_expand").performClick()
        composeTestRule.waitForIdle()

        val compactPillBounds = composeTestRule.onNodeWithTag("bottom_now_playing_pill").fetchSemanticsNode().boundsInRoot
        val compactPlayBounds = composeTestRule.onNodeWithTag("btn_play_pause").fetchSemanticsNode().boundsInRoot
        val compactChevronBounds = composeTestRule.onNodeWithTag("btn_pill_collapse_expand").fetchSemanticsNode().boundsInRoot
        val compactModeBounds = composeTestRule.onNodeWithTag("btn_play_mode").fetchSemanticsNode().boundsInRoot
        assertTrue(compactPillBounds.width < expandedPillBounds.width * 0.5f)
        assertTrue(abs(compactPlayBounds.width - compactPlayBounds.height) < 2f)
        assertTrue(compactChevronBounds.left >= compactPlayBounds.right - 2f)
        assertTrue(compactModeBounds.left >= compactPillBounds.right)
        assertTrue(compactModeBounds.left < expandedModeBounds.left)
        assertEquals(
            expandedModeBounds.left - expandedPillBounds.right,
            compactModeBounds.left - compactPillBounds.right,
            2.5f
        )

        composeTestRule.onNodeWithTag("bottom_now_playing_title", useUnmergedTree = true).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("bottom_now_playing_queue", useUnmergedTree = true).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("btn_play_mode").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_play_pause").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_pill_collapse_expand").assertIsDisplayed()

        // External Repeat stays clickable even while pill is collapsed
        composeTestRule.onNodeWithTag("btn_play_mode").performClick()
        assertEquals(1, cycleModeCalls)

        // Scrubbing & tap-seeking are locked while collapsed
        composeTestRule.onNodeWithTag("bottom_now_playing_pill").performTouchInput {
            swipeRight()
            click(Offset(center.x, center.y))
        }
        assertEquals(0, seekCalls)

        // Expand back -> full width restored, Play/Pause returns to capsule, external Repeat glides back out to right edge
        composeTestRule.onNodeWithTag("btn_pill_collapse_expand").performClick()
        composeTestRule.waitForIdle()

        val reExpandedPillBounds = composeTestRule.onNodeWithTag("bottom_now_playing_pill").fetchSemanticsNode().boundsInRoot
        val reExpandedPlayBounds = composeTestRule.onNodeWithTag("btn_play_pause").fetchSemanticsNode().boundsInRoot
        val reExpandedInfoBounds = composeTestRule.onNodeWithTag("bottom_now_playing_info", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val reExpandedTitleBounds = composeTestRule.onNodeWithTag("bottom_now_playing_title", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val reExpandedQueueBounds = composeTestRule.onNodeWithTag("bottom_now_playing_queue", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val reExpandedChevronBounds = composeTestRule.onNodeWithTag("btn_pill_collapse_expand").fetchSemanticsNode().boundsInRoot
        val reExpandedModeBounds = composeTestRule.onNodeWithTag("btn_play_mode").fetchSemanticsNode().boundsInRoot
        assertEquals(expandedPillBounds.width, reExpandedPillBounds.width, 1f)
        assertEquals(expandedPlayBounds.width, reExpandedPlayBounds.width, 1f)
        assertTrue(reExpandedPlayBounds.width > reExpandedPlayBounds.height)
        assertEquals(expandedModeBounds.left, reExpandedModeBounds.left, 1.5f)
        assertEquals(expandedInfoBounds.width, reExpandedInfoBounds.width, 1f)
        assertTrue(reExpandedInfoBounds.right <= reExpandedChevronBounds.left + 1f)
        assertTrue((reExpandedChevronBounds.left - reExpandedInfoBounds.right) <= 2f)
        assertEquals(reExpandedInfoBounds.right, reExpandedTitleBounds.right, 1f)
        assertEquals(reExpandedInfoBounds.right, reExpandedQueueBounds.right, 1f)
        composeTestRule.onNodeWithTag("bottom_now_playing_title", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_now_playing_queue", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_play_mode").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_now_playing_pill").performTouchInput { swipeRight() }
        assertTrue(seekCalls > 0)
    }

    @Test
    fun `mainViewModel persists player pill collapsed state across restarts`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences("music_player_theme_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove(MainViewModel.KEY_PLAYER_PILL_COLLAPSED).commit()

        val vm1 = MainViewModel(app)
        assertFalse(vm1.isPlayerPillCollapsed.value)

        vm1.setPlayerPillCollapsed(true)
        assertTrue(vm1.isPlayerPillCollapsed.value)
        assertTrue(prefs.getBoolean(MainViewModel.KEY_PLAYER_PILL_COLLAPSED, false))

        val vm2 = MainViewModel(app)
        assertTrue(vm2.isPlayerPillCollapsed.value)

        vm2.setPlayerPillCollapsed(false)
        val vm3 = MainViewModel(app)
        assertFalse(vm3.isPlayerPillCollapsed.value)
    }

    @Test
    fun `selected song replaces leading track number with equalizer bars across SongList and PlaylistDetailScreen`() {
        val song1 = Song(1L, "Selected Track.mp3", Uri.parse("content://media/1"), 120_000L, 1L)
        val song2 = Song(2L, "Other Track.mp3", Uri.parse("content://media/2"), 150_000L, 2L)
        val playlist = Playlist(id = 33L, name = "Road Mix", createdAt = 1L, songs = listOf(song1, song2))
        var showPlaylistDetail by mutableStateOf(false)
        var isPlaying by mutableStateOf(true)

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!showPlaylistDetail) {
                        SongList(
                            songs = listOf(song1, song2),
                            playbackState = PlaybackState(
                                currentSong = song1,
                                isPlaying = isPlaying,
                                activePlaylistId = 33L
                            ),
                            onSongClick = {},
                            onAddToPlaylist = {},
                            onRenameSong = { _, _ -> },
                            onDeleteSong = {},
                            contentTopPadding = 12.dp,
                            contentBottomPadding = 90.dp,
                            isDarkTheme = false
                        )
                    } else {
                        PlaylistDetailScreen(
                            playlist = playlist,
                            playbackState = PlaybackState(
                                currentSong = song1,
                                isPlaying = isPlaying,
                                activePlaylistId = null
                            ),
                            onBack = {},
                            onPlayAll = {},
                            onAddSongs = {},
                            onRenamePlaylist = {},
                            onSongClick = {},
                            onRemoveSongFromPlaylist = {},
                            contentTopPadding = 12.dp,
                            contentBottomPadding = 90.dp,
                            isDarkTheme = false
                        )
                    }
                }
            }
        }

        // In SongList: song1 shows equalizer instead of "1" even when activePlaylistId != null; song2 shows "2"
        composeTestRule.onNodeWithTag("song_equalizer_1", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("song_equalizer_2", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNode(
            hasAnyAncestor(hasTestTag("song_index_1")) and hasText("1"),
            useUnmergedTree = true
        ).assertDoesNotExist()
        composeTestRule.onNode(
            hasAnyAncestor(hasTestTag("song_index_2")) and hasText("2"),
            useUnmergedTree = true
        ).assertIsDisplayed()

        // Pausing still retains the equalizer indicator on selected song1 (static bars)
        isPlaying = false
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("song_equalizer_1", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(
            hasAnyAncestor(hasTestTag("song_index_1")) and hasText("1"),
            useUnmergedTree = true
        ).assertDoesNotExist()

        // In PlaylistDetailScreen: song1 also shows equalizer instead of "1" across any list
        showPlaylistDetail = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("song_equalizer_1", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("song_equalizer_2", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNode(
            hasAnyAncestor(hasTestTag("song_index_1")) and hasText("1"),
            useUnmergedTree = true
        ).assertDoesNotExist()
        composeTestRule.onNode(
            hasAnyAncestor(hasTestTag("song_index_2")) and hasText("2"),
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun `selected song cards across all lists use rounded corners instead of pill-rounded ends`() {
        // UniformSongCard min height is 56dp (half = 28dp) -> 18dp gives rounded corners with straight vertical sides
        assertEquals(18.dp, resolveSongCardCornerTarget(isSelectedOrRevealed = true, isEdge = false))
        assertEquals(18.dp, resolveSongCardCornerTarget(isSelectedOrRevealed = true, isEdge = true))
        assertEquals(24.dp, resolveSongCardCornerTarget(isSelectedOrRevealed = false, isEdge = true))
        assertEquals(4.dp, resolveSongCardCornerTarget(isSelectedOrRevealed = false, isEdge = false))

        // CurrentQueueBottomSheet row min height is ~50dp (half = 25dp) -> 16dp keeps rounded corners without capsule ends
        assertEquals(16.dp, resolveQueueRowCornerTarget(isCurrent = true))
        assertEquals(10.dp, resolveQueueRowCornerTarget(isCurrent = false))
        assertTrue(resolveSongCardCornerTarget(isSelectedOrRevealed = true, isEdge = false) < 28.dp)
        assertTrue(resolveQueueRowCornerTarget(isCurrent = true) < 25.dp)

        // Verify selection transitions cleanly inside SongList & CurrentQueueBottomSheet UI
        val song1 = Song(1L, "Track One.mp3", Uri.parse("content://media/1"), 120_000L, 1L)
        val song2 = Song(2L, "Track Two.mp3", Uri.parse("content://media/2"), 150_000L, 2L)
        var currentSong by mutableStateOf<Song?>(song1)

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    SongList(
                        songs = listOf(song1, song2),
                        playbackState = PlaybackState(currentSong = currentSong, isPlaying = true),
                        onSongClick = { currentSong = it },
                        onAddToPlaylist = {},
                        onRenameSong = { _, _ -> },
                        onDeleteSong = {},
                        contentTopPadding = 12.dp,
                        contentBottomPadding = 90.dp,
                        isDarkTheme = false
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("song_card_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("song_equalizer_1", useUnmergedTree = true).assertIsDisplayed()

        // Switch selection smoothly to song 2
        composeTestRule.onNodeWithTag("song_card_2").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("song_equalizer_2", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("song_equalizer_1", useUnmergedTree = true).assertDoesNotExist()

        // Leading index/equalizer column is compact (22.dp) to maximize space for song title
        val card1Bounds = composeTestRule.onNodeWithTag("song_card_1").fetchSemanticsNode().boundsInRoot
        val index1Bounds = composeTestRule.onNodeWithTag("song_index_1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val title1Bounds = composeTestRule.onNodeWithText(song1.displayTitle, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val maxIndexWidthPx = with(composeTestRule.density) { 24.dp.toPx() }
        val maxTitleLeftInsetPx = with(composeTestRule.density) { 42.dp.toPx() }
        assertTrue(index1Bounds.width <= maxIndexWidthPx)
        assertTrue((title1Bounds.left - card1Bounds.left) <= maxTitleLeftInsetPx)
    }

    @Test
    fun `addSongsToPlaylistBottomSheet minimalist search bar filters songs and preserves selection`() {
        val song1 = Song(1L, "Mùa Xuân Đầu Tiên.mp3", Uri.parse("content://media/1"), 120_000L, 1L)
        val song2 = Song(2L, "Đêm Thành Phố Đầy Sao.mp3", Uri.parse("content://media/2"), 150_000L, 2L)
        val song3 = Song(3L, "Acoustic Chill.mp3", Uri.parse("content://media/3"), 180_000L, 3L)
        val playlist = Playlist(id = 55L, name = "Bolero & Chill", createdAt = 1L, songs = emptyList())
        var confirmedSongIds: List<Long> = emptyList()

        // Unit check for diacritic-insensitive and case-insensitive search helper
        assertTrue(matchesSongSearchQuery(song1, "mua xuan"))
        assertTrue(matchesSongSearchQuery(song2, "dem thanh pho"))
        assertFalse(matchesSongSearchQuery(song3, "mua xuan"))

        composeTestRule.setContent {
            MyApplicationTheme {
                AddSongsToPlaylistBottomSheet(
                    playlist = playlist,
                    allSongs = listOf(song1, song2, song3),
                    onConfirmAddSongs = { confirmedSongIds = it },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        // Initially search button is a circle on the right of the title
        val iconBounds = composeTestRule.onNodeWithTag("icon_playlist_add_songs").fetchSemanticsNode().boundsInRoot
        val titleBounds = composeTestRule.onNodeWithTag("title_add_songs_to_playlist", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val initialSearchBounds = composeTestRule.onNodeWithTag("btn_search_add_songs").fetchSemanticsNode().boundsInRoot
        assertEquals(initialSearchBounds.width, initialSearchBounds.height, 1.5f)
        assertTrue(initialSearchBounds.left > titleBounds.left)
        composeTestRule.onNodeWithTag("input_search_add_songs").assertDoesNotExist()

        composeTestRule.onNodeWithTag("pick_song_row_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pick_song_row_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pick_song_row_3").assertIsDisplayed()

        // Select song 1 first
        composeTestRule.onNodeWithTag("pick_song_row_1").performClick()

        // Tap circular search button -> expands to long capsule reaching playlist icon and overlapping title
        composeTestRule.onNodeWithTag("btn_search_add_songs").performClick()
        composeTestRule.waitForIdle()

        val expandedSearchBounds = composeTestRule.onNodeWithTag("btn_search_add_songs").fetchSemanticsNode().boundsInRoot
        assertTrue(expandedSearchBounds.width > initialSearchBounds.width * 2f)
        assertEquals(titleBounds.left, expandedSearchBounds.left, 2f)
        assertTrue(abs(expandedSearchBounds.left - iconBounds.right) <= with(composeTestRule.density) { 14.dp.toPx() })
        composeTestRule.onNodeWithTag("input_search_add_songs").assertIsDisplayed()

        // Filter using diacritic-free query "dem thanh" -> only song 2 is shown
        composeTestRule.onNodeWithTag("input_search_add_songs").performTextInput("dem thanh")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pick_song_row_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pick_song_row_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("pick_song_row_3").assertDoesNotExist()

        // Select song 2 while filtered
        composeTestRule.onNodeWithTag("pick_song_row_2").performClick()

        // Note: clicking pick_song_row_2 outside the search capsule also collapses the search capsule back to a circle!
        composeTestRule.waitForIdle()
        val collapsedAfterOutsideClickBounds = composeTestRule.onNodeWithTag("btn_search_add_songs").fetchSemanticsNode().boundsInRoot
        assertEquals(initialSearchBounds.width, collapsedAfterOutsideClickBounds.width, 1.5f)

        // Expand again and filter with non-matching query -> shows empty search state
        composeTestRule.onNodeWithTag("btn_search_add_songs").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("input_search_add_songs").performTextReplacement("khong co bai nay")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add_songs_no_search_results").assertIsDisplayed()

        // Clear/close search via X button -> collapses back to circle, all songs shown again and both song 1 & song 2 remain selected
        composeTestRule.onNodeWithTag("btn_clear_search_add_songs").performClick()
        composeTestRule.waitForIdle()
        val collapsedAfterXBounds = composeTestRule.onNodeWithTag("btn_search_add_songs").fetchSemanticsNode().boundsInRoot
        assertEquals(initialSearchBounds.width, collapsedAfterXBounds.width, 1.5f)
        composeTestRule.onNodeWithTag("pick_song_row_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pick_song_row_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pick_song_row_3").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_confirm_add_songs_to_playlist").performClick()
        assertEquals(listOf(1L, 2L), confirmedSongIds)
    }

    @Test
    fun `expanded NowPlayingPill inside CapsuleBottomToolbar supports horizontal drag scrubbing on title queue and pill background`() {
        val song = Song(
            id = 88L,
            title = "Bản Giao Hưởng Ánh Sáng Trải Dài.mp3",
            uri = Uri.parse("content://media/external/audio/media/88"),
            duration = 200_000L,
            dateAdded = 1L
        )
        val posFlow = MutableStateFlow(40_000L)
        var seekTarget: Long? = null
        var playToggled = 0
        var modeCycled = 0
        var isCollapsedState by mutableStateOf(false)

        composeTestRule.setContent {
            MyApplicationTheme {
                CapsuleBottomToolbar(
                    currentTab = MainTab.SONGS,
                    onSelectTab = {},
                    playbackState = PlaybackState(
                        currentSong = song,
                        isPlaying = true,
                        playMode = PlayMode.REPEAT_ALL,
                        durationMs = 200_000L
                    ),
                    activePlaylistName = "Acoustic Chill",
                    currentPositionMsFlow = posFlow,
                    isDarkTheme = false,
                    isPlayerPillCollapsed = isCollapsedState,
                    onTogglePlayerPillCollapsed = { isCollapsedState = !isCollapsedState },
                    onCyclePlayMode = { modeCycled++ },
                    onTogglePlayPause = { playToggled++ },
                    onSeek = { seekTarget = it },
                    onOpenQueue = {},
                    onOpenSettings = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // 1. Swiping horizontally on song title text seeks song progress
        composeTestRule.onNodeWithTag("bottom_now_playing_title", useUnmergedTree = true).performTouchInput {
            swipeRight()
        }
        composeTestRule.waitForIdle()
        assertTrue((seekTarget ?: 0L) > 50_000L)

        // 2. Swiping horizontally on time & playlist subtitle text also seeks song progress
        seekTarget = null
        composeTestRule.onNodeWithTag("bottom_now_playing_queue", useUnmergedTree = true).performTouchInput {
            swipeRight()
        }
        composeTestRule.waitForIdle()
        assertTrue((seekTarget ?: 0L) > 50_000L)

        // 3. Tapping the 3 buttons (Play/Pause, Repeat, Collapse) triggers their own actions and never triggers onSeek
        seekTarget = null
        composeTestRule.onNodeWithTag("btn_play_pause").performClick()
        assertEquals(1, playToggled)
        assertNull(seekTarget)

        composeTestRule.onNodeWithTag("btn_play_mode").performClick()
        assertEquals(1, modeCycled)
        assertNull(seekTarget)

        // Collapse and re-expand with elastic spring animation
        composeTestRule.onNodeWithTag("btn_pill_collapse_expand").performClick()
        composeTestRule.waitForIdle()
        assertTrue(isCollapsedState)
        assertNull(seekTarget)

        composeTestRule.onNodeWithTag("btn_pill_collapse_expand").performClick()
        composeTestRule.waitForIdle()
        assertFalse(isCollapsedState)

        // After elastic collapse & re-expand, horizontal drag scrubbing on title still works seamlessly
        seekTarget = null
        composeTestRule.onNodeWithTag("bottom_now_playing_title", useUnmergedTree = true).performTouchInput {
            swipeRight()
        }
        composeTestRule.waitForIdle()
        assertTrue((seekTarget ?: 0L) > 50_000L)
    }

    @Test
    fun `tapping Carousel card on Songs page opens only CarouselPlaylistBottomSheet without switching tabs`() {
        val song1 = Song(1L, "Giai Điệu Mùa Thu.mp3", Uri.parse("content://media/1"), 120_000L, 1L)
        val song2 = Song(2L, "Khúc Nhạc Đêm.mp3", Uri.parse("content://media/2"), 150_000L, 2L)
        val playlist = Playlist(id = 42L, name = "Acoustic Đêm Khuya", createdAt = 1L, songs = listOf(song1, song2))

        var currentTab by mutableStateOf(MainTab.SONGS)
        var sheetPlaylist by mutableStateOf<Playlist?>(null)
        var selectedSongFromSheet: Song? = null
        var playedPlaylistFromSheet: Playlist? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    SongList(
                        songs = listOf(song1, song2),
                        playlists = listOf(playlist),
                        playbackState = PlaybackState(currentSong = song1, activePlaylistId = 42L, isPlaying = true),
                        onSongClick = {},
                        onCarouselPlaylistClick = { clicked -> sheetPlaylist = clicked },
                        onCarouselPlayPlaylist = {},
                        onAddToPlaylist = {},
                        onRenameSong = { _, _ -> },
                        onDeleteSong = {},
                        contentTopPadding = 12.dp,
                        contentBottomPadding = 140.dp,
                        isDarkTheme = false
                    )

                    sheetPlaylist?.let { activeSheetPl ->
                        CarouselPlaylistBottomSheet(
                            playlist = activeSheetPl,
                            currentSong = song1,
                            activePlaylistId = 42L,
                            isPlaying = true,
                            onPlayPlaylist = { playedPlaylistFromSheet = it },
                            onSelectSong = { selectedSongFromSheet = it },
                            onDismiss = { sheetPlaylist = null }
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("carousel_playlist_card_42").performClick()
        composeTestRule.waitForIdle()

        // Remains on SONGS tab and shows bottom sheet for that playlist
        assertEquals(MainTab.SONGS, currentTab)
        composeTestRule.onNodeWithTag("sheet_carousel_playlist").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sheet_carousel_playlist_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_sheet_song_row_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_sheet_song_row_2").assertIsDisplayed()

        // Tapping a song inside the bottom sheet selects that song
        composeTestRule.onNodeWithTag("carousel_sheet_song_row_2").performClick()
        assertEquals(2L, selectedSongFromSheet?.id)

        // Tapping play button inside the bottom sheet header triggers playlist playback
        composeTestRule.onNodeWithTag("btn_sheet_play_playlist_42").performClick()
        assertEquals(42L, playedPlaylistFromSheet?.id)
    }

    @Test
    fun `progress pill automatically collapses after 1 minute of no touch anywhere on the app and resets timer on touch`() {
        assertEquals(60_000L, INACTIVITY_AUTO_COLLAPSE_TIMEOUT_MS)
        val song = Song(1L, "Serenade.mp3", Uri.parse("content://media/1"), 180_000L, 1L)
        val posFlow = MutableStateFlow(20_000L)
        var isCollapsed by mutableStateOf(false)

        composeTestRule.setContent {
            MyApplicationTheme {
                var userInteractionTick by remember { mutableLongStateOf(0L) }
                LaunchedEffect(isCollapsed, userInteractionTick) {
                    if (!isCollapsed) {
                        delay(INACTIVITY_AUTO_COLLAPSE_TIMEOUT_MS)
                        isCollapsed = true
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .observeUserTouch { userInteractionTick++ }
                        .testTag("app_touch_root")
                ) {
                    CapsuleBottomToolbar(
                        currentTab = MainTab.SONGS,
                        onSelectTab = {},
                        playbackState = PlaybackState(
                            currentSong = song,
                            isPlaying = true,
                            playMode = PlayMode.REPEAT_ALL,
                            durationMs = 180_000L
                        ),
                        activePlaylistName = "Chill",
                        currentPositionMsFlow = posFlow,
                        isDarkTheme = false,
                        isPlayerPillCollapsed = isCollapsed,
                        onTogglePlayerPillCollapsed = { isCollapsed = !isCollapsed },
                        onCyclePlayMode = {},
                        onTogglePlayPause = {},
                        onSeek = {},
                        onOpenQueue = {},
                        onOpenSettings = {},
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        assertFalse(isCollapsed)

        // Advance 40 seconds -> still expanded
        composeTestRule.mainClock.advanceTimeBy(40_000L)
        composeTestRule.waitForIdle()
        assertFalse(isCollapsed)

        // Touch anywhere on the app -> resets the 1-minute countdown
        composeTestRule.onNodeWithTag("app_touch_root").performTouchInput {
            click(center)
        }
        composeTestRule.waitForIdle()

        // Advance another 40 seconds (80s total, but only 40s since last touch) -> still expanded!
        composeTestRule.mainClock.advanceTimeBy(40_000L)
        composeTestRule.waitForIdle()
        assertFalse(isCollapsed)

        // Advance remaining 21 seconds (61s since last touch) -> automatically collapses!
        composeTestRule.mainClock.advanceTimeBy(21_000L)
        composeTestRule.waitForIdle()
        assertTrue(isCollapsed)
        composeTestRule.onNodeWithTag("bottom_now_playing_title", useUnmergedTree = true).assertIsNotDisplayed()
    }

    @Test
    fun `songs page and playlists page show capsule search bars only when non-empty and filter items accurately`() {
        val song1 = Song(1L, "Mùa Thu Lá Bay.mp3", Uri.parse("content://media/1"), 120_000L, 1L)
        val song2 = Song(2L, "Đêm Đông.mp3", Uri.parse("content://media/2"), 150_000L, 2L)
        val playlist1 = Playlist(id = 10L, name = "Nhạc Trữ Tình", createdAt = 1L, songs = listOf(song1))
        val playlist2 = Playlist(id = 20L, name = "Acoustic Buổi Sáng", createdAt = 2L, songs = listOf(song2))

        var currentSongs by mutableStateOf(emptyList<Song>())
        var currentPlaylists by mutableStateOf(emptyList<Playlist>())
        var showPlaylistsTab by mutableStateOf(false)

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!showPlaylistsTab) {
                        SongList(
                            songs = currentSongs,
                            playlists = currentPlaylists,
                            playbackState = PlaybackState(),
                            onSongClick = {},
                            onAddToPlaylist = {},
                            onRenameSong = { _, _ -> },
                            onDeleteSong = {},
                            contentTopPadding = 12.dp,
                            contentBottomPadding = 90.dp,
                            isDarkTheme = false
                        )
                    } else {
                        PlaylistsOverviewScreen(
                            playlists = currentPlaylists,
                            playbackState = PlaybackState(),
                            onCreatePlaylist = {},
                            onOpenPlaylist = {},
                            onPlayPlaylist = {},
                            onRequestDeletePlaylist = {},
                            contentTopPadding = 12.dp,
                            contentBottomPadding = 90.dp
                        )
                    }
                }
            }
        }

        // 1. Empty Songs page: search bar is NOT shown
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("songs_search_bar").assertDoesNotExist()

        // 2. Non-empty Songs page: capsule search bar is at the top spanning full width above carousel & songs
        currentSongs = listOf(song1, song2)
        currentPlaylists = listOf(playlist1, playlist2)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("songs_search_bar").assertIsDisplayed()

        val songsSearchBounds = composeTestRule.onNodeWithTag("songs_search_bar").fetchSemanticsNode().boundsInRoot
        val carouselBounds = composeTestRule.onNodeWithTag("songs_playlists_carousel").fetchSemanticsNode().boundsInRoot
        assertTrue(songsSearchBounds.bottom <= carouselBounds.top)
        assertTrue(songsSearchBounds.width > songsSearchBounds.height * 3f)

        // Filter songs with diacritic-free query "dem dong"
        composeTestRule.onNodeWithTag("input_search_songs").performTextInput("dem dong")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("song_card_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("song_card_1").assertDoesNotExist()

        // Clear search restores all songs
        composeTestRule.onNodeWithTag("btn_clear_search_songs").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("song_card_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("song_card_2").assertIsDisplayed()

        // 3. Switch to Playlists page when empty -> search bar is NOT shown, title is shown
        currentPlaylists = emptyList()
        showPlaylistsTab = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("playlists_search_bar").assertDoesNotExist()
        composeTestRule.onNodeWithText("Danh sách phát").assertIsDisplayed()

        // 4. Non-empty Playlists page -> capsule search bar replaces title on the left of create button
        currentPlaylists = listOf(playlist1, playlist2)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("playlists_search_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Danh sách phát").assertDoesNotExist()

        val plSearchBounds = composeTestRule.onNodeWithTag("playlists_search_bar").fetchSemanticsNode().boundsInRoot
        val createBtnBounds = composeTestRule.onNodeWithTag("btn_create_playlist").fetchSemanticsNode().boundsInRoot
        assertTrue(plSearchBounds.right <= createBtnBounds.left)
        assertEquals(plSearchBounds.center.y, createBtnBounds.center.y, 2f)

        // Filter playlists with diacritic-free query "tru tinh"
        composeTestRule.onNodeWithTag("input_search_playlists").performTextInput("tru tinh")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("playlist_card_10").assertIsDisplayed()
        composeTestRule.onNodeWithTag("playlist_card_20").assertDoesNotExist()

        // Clear playlist search restores all playlists
        composeTestRule.onNodeWithTag("btn_clear_search_playlists").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("playlist_card_10").assertIsDisplayed()
        composeTestRule.onNodeWithTag("playlist_card_20").assertIsDisplayed()
    }

    @Test
    fun `playlist detail screen search button starts as circle next to add button expands to back button overlapping title and collapses on X or outside tap`() {
        val song1 = Song(1L, "Hương Đêm Bay Xa.mp3", Uri.parse("content://media/1"), 120_000L, 1L)
        val song2 = Song(2L, "Nắng Ấm Xa Dần.mp3", Uri.parse("content://media/2"), 150_000L, 2L)
        var playlistState by mutableStateOf(
            Playlist(id = 88L, name = "Tình Khúc Vàng", createdAt = 1L, songs = emptyList())
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                PlaylistDetailScreen(
                    playlist = playlistState,
                    playbackState = PlaybackState(),
                    onBack = {},
                    onPlayAll = {},
                    onAddSongs = {},
                    onRenamePlaylist = {},
                    onSongClick = {},
                    onRemoveSongFromPlaylist = {},
                    contentTopPadding = 12.dp,
                    contentBottomPadding = 90.dp,
                    isDarkTheme = false
                )
            }
        }

        // 1. Empty playlist detail page: search button is NOT shown
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_search_playlist_detail").assertDoesNotExist()

        // 2. Non-empty playlist detail page: search button starts as a 48dp circle immediately left of Add button
        playlistState = playlistState.copy(songs = listOf(song1, song2))
        composeTestRule.waitForIdle()

        val backBounds = composeTestRule.onNodeWithTag("btn_back_playlists").fetchSemanticsNode().boundsInRoot
        val titleBounds = composeTestRule.onNodeWithTag("btn_rename_playlist").fetchSemanticsNode().boundsInRoot
        val addBounds = composeTestRule.onNodeWithTag("btn_add_songs_to_playlist").fetchSemanticsNode().boundsInRoot
        val initialSearchBounds = composeTestRule.onNodeWithTag("btn_search_playlist_detail").fetchSemanticsNode().boundsInRoot

        assertEquals(initialSearchBounds.width, initialSearchBounds.height, 1.5f)
        assertTrue(initialSearchBounds.right <= addBounds.left)
        assertTrue(initialSearchBounds.left > backBounds.right + 20f)
        composeTestRule.onNodeWithTag("input_search_playlist_detail").assertDoesNotExist()

        // 3. Tap circular search button -> animates into long capsule extending to Back button and overlapping title
        composeTestRule.onNodeWithTag("btn_search_playlist_detail").performClick()
        composeTestRule.waitForIdle()

        val expandedSearchBounds = composeTestRule.onNodeWithTag("btn_search_playlist_detail").fetchSemanticsNode().boundsInRoot
        assertTrue(expandedSearchBounds.width > initialSearchBounds.width * 2f)
        assertTrue(abs(expandedSearchBounds.left - backBounds.right) <= with(composeTestRule.density) { 6.dp.toPx() })
        assertEquals(titleBounds.left, expandedSearchBounds.left, 2f)
        composeTestRule.onNodeWithTag("input_search_playlist_detail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_close_search_playlist_detail").assertIsDisplayed()

        // Filter songs inside playlist detail
        composeTestRule.onNodeWithTag("input_search_playlist_detail").performTextInput("nang am")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("playlist_song_card_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("playlist_song_card_1").assertDoesNotExist()

        // 4. Tap X button -> clears query and collapses back to initial circle
        composeTestRule.onNodeWithTag("btn_close_search_playlist_detail").performClick()
        composeTestRule.waitForIdle()

        val collapsedAfterXBounds = composeTestRule.onNodeWithTag("btn_search_playlist_detail").fetchSemanticsNode().boundsInRoot
        assertEquals(initialSearchBounds.width, collapsedAfterXBounds.width, 1.5f)
        assertEquals(collapsedAfterXBounds.width, collapsedAfterXBounds.height, 1.5f)
        composeTestRule.onNodeWithTag("playlist_song_card_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("playlist_song_card_2").assertIsDisplayed()

        // 5. Expand again and tap outside (e.g., on the Play All button area below header) -> collapses back to circle
        composeTestRule.onNodeWithTag("btn_search_playlist_detail").performClick()
        composeTestRule.waitForIdle()
        assertTrue(
            composeTestRule.onNodeWithTag("btn_search_playlist_detail").fetchSemanticsNode().boundsInRoot.width >
                initialSearchBounds.width * 2f
        )

        composeTestRule.onNodeWithTag("btn_play_playlist_detail").performTouchInput {
            click(center)
        }
        composeTestRule.waitForIdle()

        val collapsedAfterOutsideBounds = composeTestRule.onNodeWithTag("btn_search_playlist_detail").fetchSemanticsNode().boundsInRoot
        assertEquals(initialSearchBounds.width, collapsedAfterOutsideBounds.width, 1.5f)
        assertEquals(collapsedAfterOutsideBounds.width, collapsedAfterOutsideBounds.height, 1.5f)
    }

    @Test
    fun `carousel largest card is square and stopped horizontal bounds match song list margins`() {
        val song1 = Song(1L, "Bản Tình Ca Mùa Đông.mp3", Uri.parse("content://media/1"), 180_000L, 1L)
        val song2 = Song(2L, "Khúc Giao Mùa.mp3", Uri.parse("content://media/2"), 200_000L, 2L)
        val playlists = (1L..5L).map { id ->
            Playlist(id = id, name = "Danh Sách $id", createdAt = id, songs = listOf(song1, song2))
        }

        composeTestRule.setContent {
            MyApplicationTheme {
                SongList(
                    songs = listOf(song1, song2),
                    playlists = playlists,
                    playbackState = PlaybackState(),
                    onSongClick = {},
                    onAddToPlaylist = {},
                    onRenameSong = { _, _ -> },
                    onDeleteSong = {},
                    contentTopPadding = 12.dp,
                    contentBottomPadding = 90.dp,
                    isDarkTheme = false
                )
            }
        }

        composeTestRule.waitForIdle()
        val carouselBoundsBefore = composeTestRule.onNodeWithTag("songs_playlists_carousel").fetchSemanticsNode().boundsInRoot
        val firstCardBounds = composeTestRule.onNodeWithTag("carousel_playlist_card_1").fetchSemanticsNode().boundsInRoot
        val songCardBounds = composeTestRule.onNodeWithTag("song_card_1").fetchSemanticsNode().boundsInRoot

        // 1. Largest card state is a 1:1 square (width == height)
        assertEquals(firstCardBounds.width, firstCardBounds.height, 2f)

        // 2. Carousel viewport spans full width and stopped first card is inset at ~16.dp matching song list
        assertEquals(0f, carouselBoundsBefore.left, 1f)
        assertEquals(songCardBounds.left, firstCardBounds.left, 4f)
    }

    @Test
    fun `carousel playlist bottom sheet and current queue bottom sheet support circular-to-capsule search morphing`() {
        val song1 = Song(1L, "Mưa Trên Phố Huế.mp3", Uri.parse("content://media/1"), 120_000L, 1L)
        val song2 = Song(2L, "Chiều Tím.mp3", Uri.parse("content://media/2"), 150_000L, 2L)
        val playlist = Playlist(id = 77L, name = "Nhạc Huế Xưa", createdAt = 1L, songs = listOf(song1, song2))
        var showQueueSheet by mutableStateOf(false)

        composeTestRule.setContent {
            MyApplicationTheme {
                if (!showQueueSheet) {
                    CarouselPlaylistBottomSheet(
                        playlist = playlist,
                        currentSong = song1,
                        activePlaylistId = 77L,
                        isPlaying = true,
                        onPlayPlaylist = {},
                        onSelectSong = {},
                        onDismiss = {}
                    )
                } else {
                    CurrentQueueBottomSheet(
                        queueSongs = listOf(song1, song2),
                        currentSong = song1,
                        isPlaying = true,
                        activePlaylistName = playlist.name,
                        onSelectSong = {},
                        onDismiss = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // A. CarouselPlaylistBottomSheet: initial circular button on the right of title
        val carouselIconBounds = composeTestRule.onNodeWithTag("icon_playlist_carousel_sheet").fetchSemanticsNode().boundsInRoot
        val carouselTitleBounds = composeTestRule.onNodeWithTag("sheet_carousel_playlist_title", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val carouselSearchInitial = composeTestRule.onNodeWithTag("btn_search_carousel_sheet").fetchSemanticsNode().boundsInRoot
        assertEquals(carouselSearchInitial.width, carouselSearchInitial.height, 1.5f)
        assertTrue(carouselSearchInitial.left > carouselTitleBounds.left)

        // Click to expand -> stretches to playlist icon and overlaps playlist name
        composeTestRule.onNodeWithTag("btn_search_carousel_sheet").performClick()
        composeTestRule.waitForIdle()
        val carouselSearchExpanded = composeTestRule.onNodeWithTag("btn_search_carousel_sheet").fetchSemanticsNode().boundsInRoot
        assertTrue(carouselSearchExpanded.width > carouselSearchInitial.width * 2f)
        assertEquals(carouselTitleBounds.left, carouselSearchExpanded.left, 2f)
        assertTrue(abs(carouselSearchExpanded.left - carouselIconBounds.right) <= with(composeTestRule.density) { 14.dp.toPx() })

        // Filter songs inside CarouselPlaylistBottomSheet
        composeTestRule.onNodeWithTag("input_search_carousel_sheet").performTextInput("chieu tim")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("carousel_sheet_song_row_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_sheet_song_row_1").assertDoesNotExist()

        // Click X -> collapses back to circle and restores all songs
        composeTestRule.onNodeWithTag("btn_close_search_carousel_sheet").performClick()
        composeTestRule.waitForIdle()
        val carouselSearchCollapsed = composeTestRule.onNodeWithTag("btn_search_carousel_sheet").fetchSemanticsNode().boundsInRoot
        assertEquals(carouselSearchInitial.width, carouselSearchCollapsed.width, 1.5f)
        composeTestRule.onNodeWithTag("carousel_sheet_song_row_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_sheet_song_row_2").assertIsDisplayed()

        // B. Switch to CurrentQueueBottomSheet (opened on long-pressing expanded progress bar)
        showQueueSheet = true
        composeTestRule.waitForIdle()

        val queueIconBounds = composeTestRule.onNodeWithTag("icon_playlist_current_queue").fetchSemanticsNode().boundsInRoot
        val queueTitleBounds = composeTestRule.onNodeWithTag("title_current_queue", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val queueSearchInitial = composeTestRule.onNodeWithTag("btn_search_current_queue").fetchSemanticsNode().boundsInRoot
        assertEquals(queueSearchInitial.width, queueSearchInitial.height, 1.5f)

        // Click to expand -> stretches to playlist icon and overlaps title/playlist name
        composeTestRule.onNodeWithTag("btn_search_current_queue").performClick()
        composeTestRule.waitForIdle()
        val queueSearchExpanded = composeTestRule.onNodeWithTag("btn_search_current_queue").fetchSemanticsNode().boundsInRoot
        assertTrue(queueSearchExpanded.width > queueSearchInitial.width * 2f)
        assertEquals(queueTitleBounds.left, queueSearchExpanded.left, 2f)
        assertTrue(abs(queueSearchExpanded.left - queueIconBounds.right) <= with(composeTestRule.density) { 14.dp.toPx() })

        // Filter songs inside CurrentQueueBottomSheet
        composeTestRule.onNodeWithTag("input_search_current_queue").performTextInput("pho hue")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("queue_song_row_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("queue_song_row_2").assertDoesNotExist()

        // Click X -> collapses back to circle
        composeTestRule.onNodeWithTag("btn_close_search_current_queue").performClick()
        composeTestRule.waitForIdle()
        val queueSearchCollapsed = composeTestRule.onNodeWithTag("btn_search_current_queue").fetchSemanticsNode().boundsInRoot
        assertEquals(queueSearchInitial.width, queueSearchCollapsed.width, 1.5f)
    }

    @Test
    fun `bottom menu displays Hoa ca tab button with icon when selected and opens Hoa ca page`() {
        val posFlow = MutableStateFlow(0L)
        var currentTab by mutableStateOf(MainTab.SONGS)

        composeTestRule.setContent {
            MyApplicationTheme {
                CapsuleBottomToolbar(
                    currentTab = currentTab,
                    onSelectTab = { currentTab = it },
                    playbackState = PlaybackState(),
                    currentPositionMsFlow = posFlow,
                    isDarkTheme = false,
                    onCyclePlayMode = {},
                    onTogglePlayPause = {},
                    onSeek = {},
                    onOpenQueue = {},
                    onOpenSettings = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_sing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hòa ca").assertIsDisplayed()

        // Click "Hòa ca" tab -> selects MainTab.SING and displays accompanying icon
        composeTestRule.onNodeWithTag("nav_tab_sing").performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainTab.SING, currentTab)
        composeTestRule.onNodeWithTag("nav_tab_icon_sing", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `carousel excludes playlists that have no songs`() {
        val sampleSong = Song(
            id = 1L,
            title = "Giai Điệu Mùa Thu",
            uri = Uri.parse("content://media/external/audio/media/1"),
            duration = 180_000L,
            dateAdded = 1L
        )
        val emptyPlaylist = Playlist(
            id = 10L,
            name = "Danh sách trống",
            createdAt = 1L,
            songs = emptyList()
        )
        val nonEmptyPlaylist = Playlist(
            id = 20L,
            name = "Danh sách có nhạc",
            createdAt = 2L,
            songs = listOf(sampleSong)
        )

        var playlistsState by mutableStateOf(listOf(emptyPlaylist))

        composeTestRule.setContent {
            MyApplicationTheme {
                SongList(
                    songs = listOf(sampleSong),
                    playlists = playlistsState,
                    playbackState = PlaybackState(),
                    onSongClick = {},
                    onCarouselPlaylistClick = {},
                    onCarouselPlayPlaylist = {},
                    onAddToPlaylist = {},
                    onRenameSong = { _, _ -> },
                    onDeleteSong = {},
                    contentTopPadding = 16.dp,
                    contentBottomPadding = 16.dp,
                    isDarkTheme = false
                )
            }
        }

        composeTestRule.waitForIdle()
        // When all playlists are empty, carousel does not render at all
        composeTestRule.onNodeWithTag("songs_playlists_carousel").assertDoesNotExist()
        composeTestRule.onNodeWithTag("carousel_playlist_card_10").assertDoesNotExist()

        // When mixed with a non-empty playlist, only the non-empty playlist appears in Carousel
        playlistsState = listOf(emptyPlaylist, nonEmptyPlaylist)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("songs_playlists_carousel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_playlist_card_20").assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_playlist_card_10").assertDoesNotExist()
    }

    @Test
    fun `three tier hybrid lyrics pipeline extracts ID3v2 USLT and LRCLIB synced lyrics and offline AI fallback and caches in Room`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.lyricsCacheDao()

        try {
            // 1. Verify filename cleaner strips noise tags like _ [320kbps] (Official MV)
            val (cleanTitle, cleanArtist) = LyricsRepository.parseTitleAndArtistFromFileName(
                "Son_Tung_MTP - Con_Mua_Ngang_Qua [320kbps] (Official MV).mp3"
            )
            assertEquals("Con Mua Ngang Qua", cleanTitle)
            assertEquals("Son Tung MTP", cleanArtist)

            // 2. Verify Tier 1 ID3v2 binary USLT extraction
            val rawId3Tag = Id3v2LyricsExtractor.buildTestId3v2TagWithUslt(
                "[00:05.00] Câu hát mở đầu từ thẻ ID3\n[00:10.00] Câu hát thứ hai nối tiếp"
            )
            val extractedId3 = Id3v2LyricsExtractor.extractFromStream(
                ByteArrayInputStream(rawId3Tag),
                totalDurationMs = 60_000L
            )
            assertTrue(extractedId3 != null && extractedId3.isSynced)
            assertEquals(2, extractedId3!!.lines.size)
            assertEquals(5_000L, extractedId3.lines[0].startTimeMs)
            assertEquals(10_000L, extractedId3.lines[0].endTimeMs)
            assertEquals(10_000L, extractedId3.lines[1].startTimeMs)
            assertEquals(60_000L, extractedId3.lines[1].endTimeMs)

            val tempMp3 = File(context.cacheDir, "test_embedded_id3.mp3")
            tempMp3.writeBytes(rawId3Tag)
            val songTier1 = Song(
                id = 501L,
                title = "Bai_Hat_ID3.mp3",
                uri = Uri.fromFile(tempMp3),
                duration = 60_000L,
                dateAdded = 1L,
                path = tempMp3.absolutePath
            )

            var remoteCallCount = 0
            val fakeRemote = LrcLibRemoteClient { _ ->
                remoteCallCount++
                RemoteLyricsResponse(
                    syncedLyrics = "[00:02.00] Dòng một từ LRCLIB\n[00:08.50] Dòng hai từ LRCLIB\n[00:15.00] Dòng cuối từ LRCLIB",
                    plainLyrics = null
                )
            }
            val offlineEngine = OfflineSpeechLyricsEngine(context)
            val repo = LyricsRepository(context, dao, offlineEngine, fakeRemote)

            // Load songTier1 -> hits Tier 1 (ID3_TAG) without calling LRCLIB
            repo.loadLyricsForSong(songTier1)
            val stateTier1 = repo.uiState.value as LyricsUiState.Success
            assertEquals(LyricsSource.ID3_TAG, stateTier1.source)
            assertTrue(stateTier1.isSynced)
            assertEquals(2, stateTier1.lines.size)
            assertEquals(0, remoteCallCount)

            // Load songTier2 (no embedded ID3 file) -> hits Tier 2 (LRCLIB) and saves to Room cache
            val songTier2 = Song(
                id = 502L,
                title = "Nghe_Si_A - Bai_Ca_Hy_Vong_[320kbps]_(Official_MV).mp3",
                uri = Uri.parse("content://media/external/audio/media/502"),
                duration = 30_000L,
                dateAdded = 2L
            )
            repo.loadLyricsForSong(songTier2)
            val stateTier2 = repo.uiState.value as LyricsUiState.Success
            assertEquals(LyricsSource.LRCLIB, stateTier2.source)
            assertEquals(1, remoteCallCount)
            assertEquals(3, stateTier2.lines.size)
            assertEquals(2_000L, stateTier2.lines[0].startTimeMs)
            assertEquals(8_500L, stateTier2.lines[0].endTimeMs)
            assertEquals(8_500L, stateTier2.lines[1].startTimeMs)
            assertEquals(15_000L, stateTier2.lines[1].endTimeMs)
            assertEquals(15_000L, stateTier2.lines[2].startTimeMs)
            assertEquals(30_000L, stateTier2.lines[2].endTimeMs)

            // Priority 0 Cache check: new repository instance loads songTier2 immediately from Room DB without calling LRCLIB again
            val repoFromCache = LyricsRepository(context, dao, offlineEngine, fakeRemote)
            repoFromCache.loadLyricsForSong(songTier2)
            val cachedState = repoFromCache.uiState.value as LyricsUiState.Success
            assertTrue(cachedState.fromCache)
            assertEquals(1, remoteCallCount)
            assertEquals(3, cachedState.lines.size)

            // Rescan clears cache for songTier2 and fetches fresh from Tier 2
            repoFromCache.rescanLyrics(songTier2)
            assertEquals(2, remoteCallCount)

            // Tier 3 Offline AI Speech-to-Text when LRCLIB returns null
            val failingRemote = LrcLibRemoteClient { null }
            offlineEngine.setModelDownloadedForTesting(false)
            val repoTier3 = LyricsRepository(context, dao, offlineEngine, failingRemote)
            val songTier3 = Song(
                id = 503L,
                title = "Ban_Thu_Am_Rieng_Remix.mp3",
                uri = Uri.parse("content://media/external/audio/media/503"),
                duration = 45_000L,
                dateAdded = 3L
            )

            // When offline model is not yet downloaded, returns Error with requiresOfflineModelDownload = true
            repoTier3.loadLyricsForSong(songTier3)
            val missingModelState = repoTier3.uiState.value as LyricsUiState.Error
            assertTrue(missingModelState.requiresOfflineModelDownload)

            // Once model is downloaded, Tier 3 transcribes audio and caches with AI_OFFLINE source
            offlineEngine.setModelDownloadedForTesting(true)
            repoTier3.rescanLyrics(songTier3)
            val stateTier3 = repoTier3.uiState.value as LyricsUiState.Success
            assertEquals(LyricsSource.AI_OFFLINE, stateTier3.source)
            assertTrue(stateTier3.isSynced)
            assertTrue(stateTier3.lines.isNotEmpty())
            tempMp3.delete()
            Unit
        } finally {
            db.close()
        }
    }

    @Test
    fun `HoaCaScreen renders continuous text progress bar keeping past lines 100 percent filled, non-shifting dots, swipe-to-refresh, and coordinated toolbar return button`() {
        val song = Song(
            id = 900L,
            title = "Khúc Hòa Ca Mùa Xuân.mp3",
            uri = Uri.parse("content://media/external/audio/media/900"),
            duration = 40_000L,
            dateAdded = 1L,
            artist = "Ban Nhạc Mùa Xuân"
        )
        val lines = listOf(
            LyricLine(0, 0L, 10_000L, "Câu thứ nhất đã hát qua giữ nguyên màu"),
            LyricLine(1, 10_000L, 20_000L, "Câu thứ hai cũng đã hát xong trọn vẹn"),
            LyricLine(2, 20_000L, 30_000L, "Câu thứ ba đang hát ở đúng giữa câu này"),
            LyricLine(3, 30_000L, 40_000L, "Câu thứ tư phía dưới chưa hát tới")
        )

        // Verify exact mathematical rules for calculateLineProgress at 25_000ms:
        // - Past lines (0, 1) -> 1.0f (100% retained!)
        // - Active line (2) -> 0.5f (50% swept)
        // - Upcoming line (3) -> 0.0f (0% uncolored)
        assertEquals(1.0f, calculateLineProgress(25_000L, lines[0]), 0.001f)
        assertEquals(1.0f, calculateLineProgress(25_000L, lines[1]), 0.001f)
        assertEquals(0.5f, calculateLineProgress(25_000L, lines[2]), 0.001f)
        assertEquals(0.0f, calculateLineProgress(25_000L, lines[3]), 0.001f)

        val positionFlow = MutableStateFlow(25_000L)
        var seekedPositionMs = -1L
        var rescanCount = 0

        val successState = LyricsUiState.Success(
            songId = song.id,
            cacheKey = "hash_900",
            songTitle = "Khúc Hòa Ca Mùa Xuân",
            artistName = "Ban Nhạc Mùa Xuân",
            lines = lines,
            source = LyricsSource.LRCLIB,
            isSynced = true,
            fromCache = true
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                var showReturnButton by remember { mutableStateOf(false) }
                var returnTick by remember { mutableIntStateOf(0) }
                var isCollapsed by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    HoaCaScreen(
                        currentSong = song,
                        isPlaying = false,
                        currentPositionMsFlow = positionFlow,
                        lyricsUiState = successState,
                        offlineModelState = OfflineModelState(isDownloaded = true),
                        onSeekTo = { targetMs ->
                            seekedPositionMs = targetMs
                            positionFlow.value = targetMs
                        },
                        onRescanLyrics = { rescanCount++ },
                        onDownloadOfflineModel = {},
                        returnToActiveRequestTick = returnTick,
                        onReturnToActiveVisibilityChanged = { visible ->
                            showReturnButton = visible
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    CapsuleBottomToolbar(
                        currentTab = MainTab.SING,
                        onSelectTab = {},
                        playbackState = PlaybackState(
                            currentSong = song,
                            isPlaying = false,
                            durationMs = 40_000L
                        ),
                        currentPositionMsFlow = positionFlow,
                        isDarkTheme = false,
                        isPlayerPillCollapsed = isCollapsed,
                        onTogglePlayerPillCollapsed = { isCollapsed = !isCollapsed },
                        onCyclePlayMode = {},
                        onTogglePlayPause = {},
                        onSeek = {},
                        onOpenQueue = {},
                        onOpenSettings = {},
                        showReturnToActiveButton = showReturnButton,
                        onReturnToActiveClick = { returnTick++ },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Minimal header: only song title on the left (no artist/subtitle), only sync icon badge on right (no text label, no refresh button)
        composeTestRule.onNodeWithTag("hoaca_song_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Khúc Hòa Ca Mùa Xuân").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ban Nhạc Mùa Xuân").assertDoesNotExist()
        composeTestRule.onNodeWithText("LRCLIB").assertDoesNotExist()
        composeTestRule.onNodeWithTag("btn_rescan_lyrics").assertDoesNotExist()
        composeTestRule.onNodeWithTag("lyrics_source_badge").assertIsDisplayed()

        // 2. Non-shifting dot indicator: text starts at exact same X coordinate for both active and inactive lines
        composeTestRule.onNodeWithTag("active_lyric_indicator_2", useUnmergedTree = true).assertIsDisplayed()
        val line0Left = composeTestRule.onNodeWithTag("lyric_text_0", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.left
        val line2Left = composeTestRule.onNodeWithTag("lyric_text_2", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.left
        assertTrue(abs(line0Left - line2Left) < 0.5f)

        // 3. Continuous Text Progress Bar states at 25_000ms
        assertEquals(
            "progress_100",
            composeTestRule.onNodeWithTag("lyric_line_0").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        )
        assertEquals(
            "progress_100",
            composeTestRule.onNodeWithTag("lyric_line_1").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        )
        assertEquals(
            "progress_50",
            composeTestRule.onNodeWithTag("lyric_line_2").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        )
        assertEquals(
            "progress_0",
            composeTestRule.onNodeWithTag("lyric_line_3").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        )

        // 4. Swipe-to-refresh triggers rescan callback
        composeTestRule.onNodeWithTag("lyrics_lazy_column").performTouchInput {
            swipeDown()
        }
        composeTestRule.waitForIdle()
        assertTrue(rescanCount >= 1)

        // 5. Tap-to-Seek: clicking line 3 seeks to 30_000ms and immediately fills lines 0, 1, 2 to 100%
        composeTestRule.onNodeWithTag("lyric_line_3").performClick()
        composeTestRule.waitForIdle()
        assertEquals(30_000L, seekedPositionMs)
        assertEquals(
            "progress_100",
            composeTestRule.onNodeWithTag("lyric_line_2").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        )

        // 6. Swiping manually up pauses auto-scroll and reveals circular btn_return_to_active_lyric to the right of progress bar
        composeTestRule.onNodeWithTag("lyrics_lazy_column").performTouchInput {
            swipeUp(
                startY = bottom * 0.55f,
                endY = bottom * 0.15f
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_return_to_active_lyric").assertIsDisplayed()

        val expandedPillBounds = composeTestRule.onNodeWithTag("bottom_now_playing_pill")
            .fetchSemanticsNode().boundsInRoot
        val expandedModeBounds = composeTestRule.onNodeWithTag("btn_play_mode")
            .fetchSemanticsNode().boundsInRoot
        val returnBtnBoundsExpanded = composeTestRule.onNodeWithTag("btn_return_to_active_lyric")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(expandedModeBounds.left >= expandedPillBounds.right)
        assertTrue(returnBtnBoundsExpanded.left >= expandedModeBounds.right)
        assertTrue(abs(returnBtnBoundsExpanded.height - expandedPillBounds.height) < 2.5f)
        assertTrue(abs(returnBtnBoundsExpanded.width - returnBtnBoundsExpanded.height) < 2.5f)

        // Collapsing the progress bar brings both external Repeat and btn_return_to_active_lyric inward alongside the collapsed pill
        composeTestRule.onNodeWithTag("btn_pill_collapse_expand").performClick()
        composeTestRule.waitForIdle()
        val collapsedPillBounds = composeTestRule.onNodeWithTag("bottom_now_playing_pill")
            .fetchSemanticsNode().boundsInRoot
        val collapsedModeBounds = composeTestRule.onNodeWithTag("btn_play_mode")
            .fetchSemanticsNode().boundsInRoot
        val returnBtnBoundsCollapsed = composeTestRule.onNodeWithTag("btn_return_to_active_lyric")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(collapsedPillBounds.width < expandedPillBounds.width)
        assertTrue(collapsedModeBounds.left < expandedModeBounds.left)
        assertTrue(collapsedModeBounds.left >= collapsedPillBounds.right)
        assertTrue(returnBtnBoundsCollapsed.left < returnBtnBoundsExpanded.left)
        assertTrue(returnBtnBoundsCollapsed.left >= collapsedModeBounds.right)

        // Clicking btn_return_to_active_lyric resumes auto-scroll and hides the circular button
        composeTestRule.onNodeWithTag("btn_return_to_active_lyric").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_return_to_active_lyric").assertDoesNotExist()
    }

    @Test
    fun `lyrics parser handles offset tags and enhanced word timestamps with calibrated pause clamping`() {
        val enhancedLrc = """
            [offset:+500]
            [00:10.00] <00:10.00>Một <00:11.00>hai <00:13.00>ba
            [00:30.00] Câu tiếp theo sau đoạn nghỉ dài
        """.trimIndent()

        val parsed = Id3v2LyricsExtractor.parseLrcText(enhancedLrc, totalDurationMs = 38_000L)
        assertEquals(2, parsed.size)

        // Positive offset +500 shifts timestamps earlier by 500ms (10_000 - 500 = 9_500)
        assertEquals(9_500L, parsed[0].startTimeMs)
        assertEquals("Một hai ba", parsed[0].text)
        assertEquals(3, parsed[0].segments.size)
        assertEquals(9_500L, parsed[0].segments[0].startTimeMs)
        assertEquals(10_500L, parsed[0].segments[1].startTimeMs)
        assertEquals(12_500L, parsed[0].segments[2].startTimeMs)

        // Word-level segment interpolation tracks exact word progress instead of stretching across the 20s gap
        val atWordTwoStart = calculateLineProgress(10_500L, parsed[0], calibrateLongPause = true)
        assertTrue(atWordTwoStart in 0.25f..0.45f)

        val afterLastWordSung = calculateLineProgress(15_500L, parsed[0], calibrateLongPause = true)
        assertEquals(1.0f, afterLastWordSung, 0.001f)

        // Unsegmented line with long pause (> 9.5s) clamps effective vocal span reasonably instead of dragging over 20s
        val plainLongGapLine = LyricLine(0, 0L, 25_000L, "Câu hát ngắn rồi nghỉ nhạc dài")
        val progressAt7s = calculateLineProgress(7_000L, plainLongGapLine, calibrateLongPause = true)
        assertTrue(progressAt7s > 0.75f)
    }

    @Test
    fun `lyrics extractor skips instrumental and credit sections while capping vocal endTimeMs and repository handles Vietnamese filenames`() {
        val rawLrcWithInstrumentals = """
            [00:00.50] Sáng tác: Nhạc Sĩ Trẻ
            [00:02.00] Ca sĩ: Ca Sĩ A
            [00:03.50] (Nhạc dạo mở đầu)
            [00:06.00] Câu hát đầu tiên ngân vang giữa đêm
            [00:11.00] [Giang tấu]
            [00:16.00] ♪ ♫ ♪
            [00:20.00] Câu hát thứ hai tiếp nối cảm xúc (Nhạc dạo)
            [00:26.00]
            [00:30.00] Câu hát kết thúc bài ca
            [00:35.00] [Outro]
        """.trimIndent()

        val parsedLines = Id3v2LyricsExtractor.parseLrcText(rawLrcWithInstrumentals, totalDurationMs = 42_000L)

        // Only the 3 real vocal lines are kept; credits, instrumental markers, musical notes, and blank timestamps are skipped
        assertEquals(3, parsedLines.size)
        assertEquals("Câu hát đầu tiên ngân vang giữa đêm", parsedLines[0].text)
        assertEquals("Câu hát thứ hai tiếp nối cảm xúc", parsedLines[1].text)
        assertEquals("Câu hát kết thúc bài ca", parsedLines[2].text)

        // Instrumental/blank markers at 11.00s, 26.00s, and 35.00s cap the endTimeMs of the preceding vocal lines
        assertEquals(6_000L, parsedLines[0].startTimeMs)
        assertEquals(11_000L, parsedLines[0].endTimeMs)

        assertEquals(20_000L, parsedLines[1].startTimeMs)
        assertEquals(26_000L, parsedLines[1].endTimeMs)

        assertEquals(30_000L, parsedLines[2].startTimeMs)
        assertEquals(35_000L, parsedLines[2].endTimeMs)

        // During the instrumental break at 15_000ms, line 0 stays 100% colored and line 1 is 0%
        assertEquals(1.0f, calculateLineProgress(15_000L, parsedLines[0]), 0.001f)
        assertEquals(0.0f, calculateLineProgress(15_000L, parsedLines[1]), 0.001f)

        // Hyphenated slug and bidirectional "Bài hát - Ca sĩ" filenames generate accurate search candidates
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            var capturedMetadata: com.example.data.CleanedTrackMetadata? = null
            val fakeRemote = LrcLibRemoteClient { metadata ->
                capturedMetadata = metadata
                RemoteLyricsResponse(
                    syncedLyrics = rawLrcWithInstrumentals,
                    plainLyrics = null
                )
            }
            val repo = LyricsRepository(
                context = context,
                lyricsCacheDao = db.lyricsCacheDao(),
                offlineEngine = OfflineSpeechLyricsEngine(context),
                remoteClient = fakeRemote
            )

            val hyphenSong = Song(
                id = 777L,
                title = "Cat-Doi-Noi-Sau - Tang-Duy-Tan (Official Remix 320kbps).mp3",
                uri = Uri.parse("content://media/external/audio/media/777"),
                duration = 42_000L,
                dateAdded = 1L
            )

            runBlocking {
                repo.loadLyricsForSong(hyphenSong)
            }

            assertTrue(capturedMetadata != null)
            val candidateTitles = capturedMetadata!!.candidatePairs.map { pair -> pair.first }
            // Bidirectional candidates include both "Cat Doi Noi Sau" and "Tang Duy Tan"
            assertTrue(candidateTitles.any { it.contains("Cat Doi Noi Sau", ignoreCase = true) })
            assertTrue(candidateTitles.any { it.contains("Tang Duy Tan", ignoreCase = true) })

            val state = repo.uiState.value as LyricsUiState.Success
            assertEquals(3, state.lines.size)
            assertEquals("Câu hát đầu tiên ngân vang giữa đêm", state.lines[0].text)
        } finally {
            db.close()
        }
    }
}

