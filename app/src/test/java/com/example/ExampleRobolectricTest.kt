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
import androidx.compose.ui.test.assert
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
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.PlaylistRepository
import com.example.data.analyzePcmSamplesToProfile
import com.example.data.local.AppDatabase
import com.example.model.PlayMode
import com.example.model.PlaybackState
import com.example.model.Playlist
import com.example.model.Song
import com.example.service.PlaybackManager
import com.example.ui.AddSongsToPlaylistBottomSheet
import com.example.ui.AddToPlaylistBottomSheet
import com.example.ui.AppPreloadScreen
import com.example.ui.AudioEffectsBottomSheet
import com.example.ui.BottomNowPlayingPill
import com.example.ui.CapsuleBottomToolbar
import com.example.ui.CarouselPlaylistBottomSheet
import com.example.ui.CreatePlaylistDialog
import com.example.ui.CurrentQueueBottomSheet
import com.example.ui.EdgeToEdgeGradientScrims
import com.example.ui.INACTIVITY_AUTO_COLLAPSE_TIMEOUT_MS
import com.example.ui.KeepAliveMainTabHost
import com.example.ui.MainScreen
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.NowPlayingScreen
import com.example.ui.PlaylistCarouselCard
import com.example.ui.PlaylistDetailScreen
import com.example.ui.PlaylistIconCatalog
import com.example.ui.PlaylistsOverviewScreen
import com.example.ui.SettingsBottomSheet
import com.example.ui.SongList
import com.example.ui.WelcomeScreen
import com.example.ui.matchesSongSearchQuery
import com.example.ui.observeUserTouch
import com.example.ui.resolveCarouselWatermarkCenterXFraction
import com.example.ui.resolveQueueRowCornerTarget
import com.example.ui.resolveSongCardCornerTarget
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import java.io.ByteArrayInputStream
import java.io.File
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.AppThemeColor
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
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

        // Title sits below playlist icon and above play button; top-left circular icon matches delete button circle size and alignment
        val iconBounds1 = composeTestRule.onNodeWithTag("playlist_card_icon_10", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val deleteCircleBounds1 = composeTestRule.onNodeWithTag("playlist_delete_circle_10", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val titleBounds1 = composeTestRule.onNodeWithTag("playlist_card_title_10", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val playBounds1 = composeTestRule.onNodeWithTag("btn_play_playlist_10", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(iconBounds1.width, iconBounds1.height, 1.5f)
        assertEquals(deleteCircleBounds1.width, iconBounds1.width, 1.5f)
        assertEquals(deleteCircleBounds1.height, iconBounds1.height, 1.5f)
        assertEquals(deleteCircleBounds1.center.y, iconBounds1.center.y, 1.5f)
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
        var startedCount = 0
        var languageState by mutableStateOf(AppLanguage.VI)

        composeTestRule.setContent {
            MyApplicationTheme(appLanguage = languageState) {
                WelcomeScreen(onStartClick = { startedCount++ })
            }
        }

        composeTestRule.onNodeWithTag("welcome_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_hero_visual").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_subtitle").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_start_label").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bắt đầu").assertIsDisplayed()

        val ctaBounds = composeTestRule.onNodeWithTag("btn_welcome_start").fetchSemanticsNode().boundsInRoot
        assertTrue(ctaBounds.height >= 48f)
        assertEquals(ctaBounds.width, ctaBounds.height, 2.0f)

        composeTestRule.onNodeWithTag("btn_welcome_start").performClick()
        assertEquals(1, startedCount)

        // Switching language to EN updates welcome editorial copy cleanly
        languageState = AppLanguage.EN
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(350))
        composeTestRule.mainClock.advanceTimeBy(350L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_start_label").performClick()
        assertEquals(2, startedCount)
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
    fun `settings bottom sheet switches between Vietnamese and English dynamically`() {
        var languageState by mutableStateOf(AppLanguage.VI)

        composeTestRule.setContent {
            MyApplicationTheme(appLanguage = languageState) {
                SettingsBottomSheet(
                    isDarkTheme = false,
                    selectedThemeMode = ThemeMode.SYSTEM,
                    selectedThemeColor = AppThemeColor.DEFAULT,
                    selectedLanguage = languageState,
                    onSelectThemeColor = {},
                    onSelectLanguage = { languageState = it },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Cài đặt").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_setting_language").performScrollTo()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("language_split_button_group").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lang_mode_vi").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lang_mode_en").assertIsDisplayed()

        // Switch to English (US)
        composeTestRule.onNodeWithTag("lang_mode_en").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppLanguage.EN, languageState)
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Language").assertIsDisplayed()

        // Switch back to Việt Nam (VN)
        composeTestRule.onNodeWithTag("lang_mode_vi").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppLanguage.VI, languageState)
        composeTestRule.onNodeWithText("Cài đặt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ngôn ngữ").assertIsDisplayed()
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
    fun `playlist detail screen positions icon button between search and add and expands search full width over back button`() {
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
                    onUpdatePlaylistIcon = { iconId ->
                        playlistState = playlistState.copy(iconId = iconId)
                    },
                    onSongClick = {},
                    onRemoveSongFromPlaylist = {},
                    contentTopPadding = 12.dp,
                    contentBottomPadding = 90.dp,
                    isDarkTheme = false
                )
            }
        }

        // 1. Empty playlist detail page: search button is NOT shown, icon picker & add buttons ARE shown
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_search_playlist_detail").assertDoesNotExist()
        composeTestRule.onNodeWithTag("btn_choose_playlist_icon").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_add_songs_to_playlist").assertIsDisplayed()

        // 2. Non-empty playlist detail page: [Search] -> [Choose Icon] -> [Add] strictly ordered
        playlistState = playlistState.copy(songs = listOf(song1, song2))
        composeTestRule.waitForIdle()

        val backBounds = composeTestRule.onNodeWithTag("btn_back_playlists").fetchSemanticsNode().boundsInRoot
        val iconPickerBounds = composeTestRule.onNodeWithTag("btn_choose_playlist_icon").fetchSemanticsNode().boundsInRoot
        val addBounds = composeTestRule.onNodeWithTag("btn_add_songs_to_playlist").fetchSemanticsNode().boundsInRoot
        val initialSearchBounds = composeTestRule.onNodeWithTag("btn_search_playlist_detail").fetchSemanticsNode().boundsInRoot

        assertEquals(initialSearchBounds.width, initialSearchBounds.height, 1.5f)
        assertTrue(initialSearchBounds.right <= iconPickerBounds.left + 1f)
        assertTrue(iconPickerBounds.right <= addBounds.left + 1f)
        assertTrue(initialSearchBounds.left > backBounds.right + 20f)
        composeTestRule.onNodeWithTag("input_search_playlist_detail").assertDoesNotExist()

        // 3. Tap circular search button -> bounces and expands full header width covering Back button on the left
        composeTestRule.onNodeWithTag("btn_search_playlist_detail").performClick()
        composeTestRule.mainClock.advanceTimeBy(700L)
        composeTestRule.waitForIdle()

        val expandedSearchBounds = composeTestRule.onNodeWithTag("btn_search_playlist_detail").fetchSemanticsNode().boundsInRoot
        assertTrue(expandedSearchBounds.width > initialSearchBounds.width * 3f)
        assertTrue(
            "Expected expanded search left (${expandedSearchBounds.left}) <= back left (${backBounds.left})",
            expandedSearchBounds.left <= backBounds.left + 2f
        )
        assertTrue(
            "Expected expanded search right (${expandedSearchBounds.right}) >= add right (${addBounds.right})",
            expandedSearchBounds.right >= addBounds.right - 2f
        )
        composeTestRule.onNodeWithTag("input_search_playlist_detail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_close_search_playlist_detail").assertIsDisplayed()

        // Filter songs inside playlist detail
        composeTestRule.onNodeWithTag("input_search_playlist_detail").performTextInput("nang am")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("playlist_song_card_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("playlist_song_card_1").assertDoesNotExist()

        // 4. Tap X button -> clears query and collapses back to initial circle, restoring Back/Icon/Add
        composeTestRule.onNodeWithTag("btn_close_search_playlist_detail").performClick()
        composeTestRule.mainClock.advanceTimeBy(700L)
        composeTestRule.waitForIdle()

        val collapsedAfterXBounds = composeTestRule.onNodeWithTag("btn_search_playlist_detail").fetchSemanticsNode().boundsInRoot
        assertEquals(initialSearchBounds.width, collapsedAfterXBounds.width, 2f)
        assertEquals(collapsedAfterXBounds.width, collapsedAfterXBounds.height, 2f)
        composeTestRule.onNodeWithTag("btn_back_playlists").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_choose_playlist_icon").assertIsDisplayed()
        composeTestRule.onNodeWithTag("playlist_song_card_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("playlist_song_card_2").assertIsDisplayed()

        // 5. Expand again and tap outside (e.g., on Play All button area below header) -> collapses back to circle
        composeTestRule.onNodeWithTag("btn_search_playlist_detail").performClick()
        composeTestRule.mainClock.advanceTimeBy(700L)
        composeTestRule.waitForIdle()
        assertTrue(
            composeTestRule.onNodeWithTag("btn_search_playlist_detail").fetchSemanticsNode().boundsInRoot.width >
                initialSearchBounds.width * 3f
        )

        composeTestRule.onNodeWithTag("btn_play_playlist_detail").performTouchInput {
            click(center)
        }
        composeTestRule.mainClock.advanceTimeBy(700L)
        composeTestRule.waitForIdle()

        val collapsedAfterOutsideBounds = composeTestRule.onNodeWithTag("btn_search_playlist_detail").fetchSemanticsNode().boundsInRoot
        assertEquals(initialSearchBounds.width, collapsedAfterOutsideBounds.width, 2f)
        assertEquals(collapsedAfterOutsideBounds.width, collapsedAfterOutsideBounds.height, 2f)
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
    fun `bottom menu displays only two tabs Songs and Playlists`() {
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
        composeTestRule.onNodeWithTag("nav_tab_songs").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_playlists").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_sing").assertDoesNotExist()
        composeTestRule.onNodeWithText("Hòa ca").assertDoesNotExist()

        composeTestRule.onNodeWithTag("nav_tab_playlists").performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainTab.PLAYLISTS, currentTab)
        composeTestRule.onNodeWithTag("nav_tab_icon_playlists", useUnmergedTree = true).assertIsDisplayed()
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
    fun `selected song card replaces duration with open-player icon across lists and sheets while AddSongs keeps duration`() {
        val song1 = Song(1L, "Selected Song.mp3", Uri.parse("content://media/1"), 120_000L, 1L)
        val song2 = Song(2L, "Other Song.mp3", Uri.parse("content://media/2"), 150_000L, 2L)
        val song3 = Song(3L, "Addable Song.mp3", Uri.parse("content://media/3"), 150_000L, 3L)
        val playlist = Playlist(id = 10L, name = "Road Mix", createdAt = 1L, songs = listOf(song1, song2))

        var stage by mutableIntStateOf(0)
        var openPlayerCount = 0
        var songClickCount = 0
        var queueOpenPlayerCalls = 0
        var queueDismissCalls = 0
        var carouselOpenPlayerCalls = 0
        var carouselDismissCalls = 0

        composeTestRule.setContent {
            MyApplicationTheme {
                when (stage) {
                    0 -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            SongList(
                                songs = listOf(song1, song2),
                                playbackState = PlaybackState(currentSong = song1, isPlaying = true, durationMs = 120_000L),
                                onSongClick = { songClickCount++ },
                                onAddToPlaylist = {},
                                onRenameSong = { _, _ -> },
                                onDeleteSong = {},
                                onOpenPlayer = { openPlayerCount++ },
                                contentTopPadding = 12.dp,
                                contentBottomPadding = 144.dp,
                                isDarkTheme = false
                            )
                        }
                    }
                    1 -> {
                        CurrentQueueBottomSheet(
                            queueSongs = listOf(song1, song2),
                            currentSong = song1,
                            isPlaying = true,
                            activePlaylistName = null,
                            onSelectSong = {},
                            onOpenPlayer = { queueOpenPlayerCalls++ },
                            onDismiss = { queueDismissCalls++ }
                        )
                    }
                    2 -> {
                        CarouselPlaylistBottomSheet(
                            playlist = playlist,
                            currentSong = song1,
                            activePlaylistId = playlist.id,
                            isPlaying = true,
                            onPlayPlaylist = {},
                            onSelectSong = {},
                            onOpenPlayer = { carouselOpenPlayerCalls++ },
                            onDismiss = { carouselDismissCalls++ }
                        )
                    }
                    3 -> {
                        CurrentQueueBottomSheet(
                            queueSongs = listOf(song1, song2),
                            currentSong = song1,
                            isPlaying = true,
                            activePlaylistName = null,
                            onSelectSong = {},
                            onOpenPlayer = null,
                            onDismiss = { queueDismissCalls++ }
                        )
                    }
                    else -> {
                        AddSongsToPlaylistBottomSheet(
                            playlist = playlist,
                            allSongs = listOf(song1, song2, song3),
                            onConfirmAddSongs = {},
                            onDismiss = {}
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. SongList: selected song displays open-player icon instead of duration; unselected keeps duration & same card height
        composeTestRule.onNodeWithTag("btn_open_player_1", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_open_player_2", useUnmergedTree = true).assertDoesNotExist()
        assertEquals(
            composeTestRule.onNodeWithTag("song_card_2").fetchSemanticsNode().size.height,
            composeTestRule.onNodeWithTag("song_card_1").fetchSemanticsNode().size.height
        )

        composeTestRule.onNodeWithTag("btn_open_player_1", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, openPlayerCount)
        assertEquals(0, songClickCount)

        // 2. CurrentQueueBottomSheet: shows open-player icon on active row
        stage = 1
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_open_player_queue_1", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_open_player_queue_2", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("btn_open_player_queue_1", useUnmergedTree = true).performClick()
        assertEquals(1, queueOpenPlayerCalls)
        assertEquals(1, queueDismissCalls)

        // 3. CarouselPlaylistBottomSheet: shows open-player icon on active row
        stage = 2
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_open_player_carousel_1", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_open_player_carousel_2", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("btn_open_player_carousel_1", useUnmergedTree = true).performClick()
        assertEquals(1, carouselOpenPlayerCalls)
        assertEquals(1, carouselDismissCalls)

        // 4. CurrentQueueBottomSheet opened from NowPlayingScreen (onOpenPlayer = null): active song shows duration, no open-player button
        stage = 3
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_open_player_queue_1", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("02:00", useUnmergedTree = true).assertIsDisplayed()

        // 5. AddSongsToPlaylistBottomSheet: excluded and keeps duration text
        stage = 4
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pick_song_row_3").assertIsDisplayed()
        composeTestRule.onNodeWithText("02:30", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_open_player_3", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `NowPlayingScreen renders ultra minimalist full screen with only back button and music controls`() {
        val song = Song(
            id = 55L,
            title = "Giai Điệu Ánh Trăng.mp3",
            uri = Uri.parse("content://media/55"),
            duration = 180_000L,
            dateAdded = 1L,
            artist = "Hidden Artist Name"
        )
        val posFlow = MutableStateFlow(500L)
        val sampleRate = 44_100
        val samples = FloatArray(sampleRate) { i ->
            val t = i.toFloat() / sampleRate.toFloat()
            0.75f * sin(2f * PI.toFloat() * 75f * t)
        }
        val spectrumProfile = analyzePcmSamplesToProfile(
            songId = song.id,
            samples = samples,
            sampleRateHz = sampleRate
        )

        var isPlayingState by mutableStateOf(true)
        var playModeState by mutableStateOf(PlayMode.REPEAT_ALL)
        var themeModeState by mutableStateOf(ThemeMode.SYSTEM)
        var backCalls = 0
        var nextCalls = 0
        var prevCalls = 0
        var queueOpenCalls = 0
        var seekTargetMs = -1L

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = themeModeState == ThemeMode.DARK) {
                NowPlayingScreen(
                    song = song,
                    isPlaying = isPlayingState,
                    playMode = playModeState,
                    themeMode = themeModeState,
                    durationMs = 180_000L,
                    currentPositionMsFlow = posFlow,
                    audioSpectrumProfile = spectrumProfile,
                    isDarkTheme = themeModeState == ThemeMode.DARK,
                    onBack = { backCalls++ },
                    onTogglePlayPause = { isPlayingState = !isPlayingState },
                    onPlayNext = { nextCalls++ },
                    onPlayPrevious = { prevCalls++ },
                    onSeekTo = { seekTargetMs = it },
                    onCyclePlayMode = {
                        playModeState = PlayMode.REPEAT_ONE
                    },
                    onCycleThemeMode = {
                        themeModeState = themeModeState.next()
                    },
                    onOpenQueue = { queueOpenCalls++ }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Strictly contains back button & music controller elements, zero artist/extra UI
        composeTestRule.onNodeWithTag("now_playing_full_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_player_back").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_kinetic_artwork").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_song_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Giai điệu ánh trăng").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_wavy_scrubber").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_current_time").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_total_time").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_player_previous").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_player_play_pause").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_player_next").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_player_play_mode").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_bottom_utility_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_player_theme_mode").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_player_queue").assertIsDisplayed()

        // Verify all 4 utility buttons (Quay lại, Danh sách, Giao diện, Lặp lại) sit in 1 single row,
        // evenly spaced horizontally, directly below the Play/Pause control cluster
        val backBounds = composeTestRule.onNodeWithTag("btn_player_back").fetchSemanticsNode().boundsInRoot
        val queueBtnBounds = composeTestRule.onNodeWithTag("btn_player_queue").fetchSemanticsNode().boundsInRoot
        val themeBtnBounds = composeTestRule.onNodeWithTag("btn_player_theme_mode").fetchSemanticsNode().boundsInRoot
        val modeBounds = composeTestRule.onNodeWithTag("btn_player_play_mode").fetchSemanticsNode().boundsInRoot
        val playBoundsPlaying = composeTestRule.onNodeWithTag("btn_player_play_pause").fetchSemanticsNode().boundsInRoot
        val artworkBounds = composeTestRule.onNodeWithTag("player_kinetic_artwork").fetchSemanticsNode().boundsInRoot
        val titleBounds = composeTestRule.onNodeWithTag("player_song_title").fetchSemanticsNode().boundsInRoot

        assertTrue(titleBounds.top >= artworkBounds.bottom)
        assertTrue(backBounds.top >= playBoundsPlaying.bottom)
        assertTrue(abs(backBounds.center.y - queueBtnBounds.center.y) < 2f)
        assertTrue(abs(queueBtnBounds.center.y - themeBtnBounds.center.y) < 2f)
        assertTrue(abs(themeBtnBounds.center.y - modeBounds.center.y) < 2f)
        assertTrue(backBounds.center.x < queueBtnBounds.center.x)
        assertTrue(queueBtnBounds.center.x < themeBtnBounds.center.x)
        assertTrue(themeBtnBounds.center.x < modeBounds.center.x)

        val step1 = queueBtnBounds.center.x - backBounds.center.x
        val step2 = themeBtnBounds.center.x - queueBtnBounds.center.x
        val step3 = modeBounds.center.x - themeBtnBounds.center.x
        assertTrue(abs(step1 - step2) < 4f)
        assertTrue(abs(step2 - step3) < 4f)
        assertTrue(abs(themeBtnBounds.width - themeBtnBounds.height) <= 2f)
        assertTrue(abs(queueBtnBounds.width - queueBtnBounds.height) <= 2f)

        // When playing: Play/Pause button morphs into Capsule (width > height) with outer beat halo
        assertTrue(playBoundsPlaying.width > playBoundsPlaying.height)
        assertEquals(
            "capsule_playing",
            composeTestRule.onNodeWithTag("btn_player_play_pause").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        )
        composeTestRule.onNodeWithTag("player_play_outer_halo").assertExists()

        // Ensure no extra/clutter text or unrelated toolbar items appear
        composeTestRule.onNodeWithText("Hidden Artist Name").assertDoesNotExist()
        composeTestRule.onNodeWithTag("nav_pill_menu").assertDoesNotExist()
        composeTestRule.onNodeWithTag("btn_settings").assertDoesNotExist()

        // Quick 3-theme switcher cycles SYSTEM -> LIGHT -> DARK -> SYSTEM
        assertEquals(ThemeMode.SYSTEM, themeModeState)
        composeTestRule.onNodeWithTag("btn_player_theme_mode").performClick()
        assertEquals(ThemeMode.LIGHT, themeModeState)
        composeTestRule.onNodeWithTag("btn_player_theme_mode").performClick()
        assertEquals(ThemeMode.DARK, themeModeState)
        composeTestRule.onNodeWithTag("btn_player_theme_mode").performClick()
        assertEquals(ThemeMode.SYSTEM, themeModeState)
        composeTestRule.waitForIdle()

        // Toggle Pause -> Play/Pause button morphs cleanly to Circle (width == height) and outer halo disappears
        composeTestRule.onNodeWithTag("btn_player_play_pause").performClick()
        assertFalse(isPlayingState)
        composeTestRule.waitForIdle()
        val playBoundsPaused = composeTestRule.onNodeWithTag("btn_player_play_pause").fetchSemanticsNode().boundsInRoot
        assertTrue(abs(playBoundsPaused.width - playBoundsPaused.height) <= 2f)
        assertEquals(
            "circle_paused",
            composeTestRule.onNodeWithTag("btn_player_play_pause").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        )
        composeTestRule.onNodeWithTag("player_play_outer_halo").assertDoesNotExist()

        composeTestRule.onNodeWithTag("btn_player_previous").performClick()
        assertEquals(1, prevCalls)

        composeTestRule.onNodeWithTag("btn_player_next").performClick()
        assertEquals(1, nextCalls)

        composeTestRule.onNodeWithTag("btn_player_play_mode").performClick()
        assertEquals(PlayMode.REPEAT_ONE, playModeState)

        composeTestRule.onNodeWithTag("player_wavy_scrubber").performTouchInput { swipeRight() }
        assertTrue(seekTargetMs > 0L)

        composeTestRule.onNodeWithTag("btn_player_queue").performClick()
        assertEquals(1, queueOpenCalls)

        composeTestRule.onNodeWithTag("btn_player_back").performClick()
        assertEquals(1, backCalls)
    }

    @Test
    fun `NowPlayingScreen floating control card hides via V button and 1-minute inactivity while centering interactive 3D sphere`() {
        val song = Song(
            id = 88L,
            title = "Vũ Điệu Vũ Trụ.mp3",
            uri = Uri.parse("content://media/88"),
            duration = 200_000L,
            dateAdded = 1L,
            artist = "Hidden Artist"
        )
        val posFlow = MutableStateFlow(1_200L)
        val sampleRate = 44_100
        val samples = FloatArray(sampleRate) { i ->
            val t = i.toFloat() / sampleRate.toFloat()
            0.8f * sin(2f * PI.toFloat() * 70f * t)
        }
        val spectrumProfile = analyzePcmSamplesToProfile(
            songId = song.id,
            samples = samples,
            sampleRateHz = sampleRate
        )

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                NowPlayingScreen(
                    song = song,
                    isPlaying = true,
                    playMode = PlayMode.REPEAT_ALL,
                    themeMode = ThemeMode.DARK,
                    durationMs = 200_000L,
                    currentPositionMsFlow = posFlow,
                    audioSpectrumProfile = spectrumProfile,
                    isDarkTheme = true,
                    onBack = {},
                    onTogglePlayPause = {},
                    onPlayNext = {},
                    onPlayPrevious = {},
                    onSeekTo = {},
                    onCyclePlayMode = {},
                    onCycleThemeMode = {},
                    onOpenQueue = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // 1. Floating control card is visible with centered top chevron-down button
        composeTestRule.onNodeWithTag("player_control_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_player_hide_control_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_player_show_control_card").assertDoesNotExist()

        val screenBounds = composeTestRule.onNodeWithTag("now_playing_full_screen").fetchSemanticsNode().boundsInRoot
        val cardBounds = composeTestRule.onNodeWithTag("player_control_card").fetchSemanticsNode().boundsInRoot
        val hideBtnBounds = composeTestRule.onNodeWithTag("btn_player_hide_control_card").fetchSemanticsNode().boundsInRoot
        val titleBounds = composeTestRule.onNodeWithTag("player_song_title").fetchSemanticsNode().boundsInRoot
        val sphereBoundsVisible = composeTestRule.onNodeWithTag("player_kinetic_artwork").fetchSemanticsNode().boundsInRoot

        assertTrue(abs(hideBtnBounds.center.x - cardBounds.center.x) < 4f)
        assertTrue(hideBtnBounds.top >= cardBounds.top)
        assertTrue(titleBounds.top >= hideBtnBounds.bottom - 2f)
        assertTrue(sphereBoundsVisible.center.y < screenBounds.center.y)

        // 2. Click V-down button -> control card slides down, V-up button appears, 3D sphere moves down to center
        composeTestRule.onNodeWithTag("btn_player_hide_control_card").performClick()
        composeTestRule.mainClock.advanceTimeBy(800L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("player_control_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("btn_player_show_control_card").assertIsDisplayed()

        val sphereBoundsCentered = composeTestRule.onNodeWithTag("player_kinetic_artwork").fetchSemanticsNode().boundsInRoot
        assertTrue(sphereBoundsCentered.center.y > sphereBoundsVisible.center.y + 40f)
        assertTrue(abs(sphereBoundsCentered.center.y - screenBounds.center.y) <= 24f)

        // 3. Interactive 3D sphere supports free multi-directional drag rotation while remaining responsive
        val initialOrientation = composeTestRule
            .onNodeWithTag("player_kinetic_artwork")
            .fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription]
        composeTestRule.onNodeWithTag("player_kinetic_artwork").performTouchInput {
            swipe(
                start = center,
                end = Offset(center.x + 80f, center.y - 65f),
                durationMillis = 150L
            )
        }
        composeTestRule.mainClock.advanceTimeBy(120L)
        composeTestRule.waitForIdle()
        val draggedOrientation = composeTestRule
            .onNodeWithTag("player_kinetic_artwork")
            .fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription]
        assertTrue(
            "Expected draggedOrientation != initialOrientation, initial=$initialOrientation dragged=$draggedOrientation bounds=$sphereBoundsCentered screen=$screenBounds",
            draggedOrientation != initialOrientation
        )

        // 4. Click V-up button -> control card restores and sphere moves back up above card
        composeTestRule.onNodeWithTag("btn_player_show_control_card").performClick()
        composeTestRule.mainClock.advanceTimeBy(800L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("player_control_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_player_show_control_card").assertDoesNotExist()
        val sphereBoundsRestored = composeTestRule.onNodeWithTag("player_kinetic_artwork").fetchSemanticsNode().boundsInRoot
        assertTrue(sphereBoundsRestored.center.y < sphereBoundsCentered.center.y - 40f)

        // 5. After 1 minute (60s) of no touch, control card hides automatically; touching sphere resets the timer
        composeTestRule.mainClock.advanceTimeBy(40_000L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("player_control_card").assertIsDisplayed()

        // Dragging the 3D sphere resets the 1-minute countdown and updates rotation
        val orientationBeforeSecondDrag = composeTestRule
            .onNodeWithTag("player_kinetic_artwork")
            .fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription]
        composeTestRule.onNodeWithTag("player_kinetic_artwork").performTouchInput {
            swipe(
                start = center,
                end = Offset(center.x - 50f, center.y + 55f),
                durationMillis = 120L
            )
        }
        composeTestRule.waitForIdle()
        val orientationAfterSecondDrag = composeTestRule
            .onNodeWithTag("player_kinetic_artwork")
            .fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription]
        assertTrue(orientationAfterSecondDrag != orientationBeforeSecondDrag)

        // 40s after touch -> still visible!
        composeTestRule.mainClock.advanceTimeBy(40_000L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("player_control_card").assertIsDisplayed()

        // Remaining 21s (61s since touch) + animation -> card automatically hides to bottom
        composeTestRule.mainClock.advanceTimeBy(21_000L)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(600L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("player_control_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("btn_player_show_control_card").assertIsDisplayed()
        val showChevronBounds = composeTestRule.onNodeWithTag("btn_player_show_control_card").fetchSemanticsNode().boundsInRoot
        val minTouchPx = with(composeTestRule.density) { 48.dp.toPx() }
        assertTrue(showChevronBounds.width >= minTouchPx - 1f)
        assertTrue(showChevronBounds.height >= minTouchPx - 1f)
    }

    @Test
    fun `createPlaylistDialog places icon picker bottom-left aligned with cancel and create and 5x6 grid picks icon cleanly`() {
        var createdName: String? = null
        var createdIconId: String? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                CreatePlaylistDialog(
                    onConfirmCreate = { name, iconId ->
                        createdName = name
                        createdIconId = iconId
                    },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        val iconBtnBounds = composeTestRule.onNodeWithTag("btn_create_playlist_choose_icon").fetchSemanticsNode().boundsInRoot
        val cancelBounds = composeTestRule.onNodeWithTag("btn_cancel_create_playlist").fetchSemanticsNode().boundsInRoot
        val createBounds = composeTestRule.onNodeWithTag("btn_confirm_create_playlist").fetchSemanticsNode().boundsInRoot

        // Bottom-left icon picker sits strictly left of Cancel and Create and aligns horizontally on the same row
        assertTrue(iconBtnBounds.right < cancelBounds.left)
        assertTrue(cancelBounds.right <= createBounds.left + 2f)
        assertTrue(abs(iconBtnBounds.center.y - cancelBounds.center.y) <= 12f)
        assertTrue(abs(iconBtnBounds.center.y - createBounds.center.y) <= 12f)

        // Tap bottom-left icon button -> opens 5x6 MD3 icon picker popup with 30 icons and NO labels below
        composeTestRule.onNodeWithTag("btn_create_playlist_choose_icon").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dialog_playlist_icon_picker").assertIsDisplayed()
        composeTestRule.onNodeWithTag("playlist_icon_picker_grid").assertIsDisplayed()

        // Verify all 30 icon options exist in a 5-column x 6-row layout, and first music icon is pre-selected by default
        val allIcons = PlaylistIconCatalog.icons
        assertEquals(30, allIcons.size)
        assertEquals(PlaylistIconCatalog.DEFAULT_ICON_ID, allIcons.first().id)
        assertEquals(
            true,
            composeTestRule.onNodeWithTag("playlist_icon_option_${allIcons.first().id}")
                .fetchSemanticsNode().config[SemanticsProperties.Selected]
        )
        val firstRowFirst = composeTestRule.onNodeWithTag("playlist_icon_option_${allIcons[0].id}").fetchSemanticsNode().boundsInRoot
        val firstRowFifth = composeTestRule.onNodeWithTag("playlist_icon_option_${allIcons[4].id}").fetchSemanticsNode().boundsInRoot
        val secondRowFirst = composeTestRule.onNodeWithTag("playlist_icon_option_${allIcons[5].id}").fetchSemanticsNode().boundsInRoot
        assertEquals(firstRowFirst.center.y, firstRowFifth.center.y, 4f)
        assertTrue(secondRowFirst.top >= firstRowFirst.bottom - 1f)
        assertEquals(firstRowFirst.center.x, secondRowFirst.center.x, 4f)

        // Tap "coffee" -> immediately saves & closes picker dialog
        composeTestRule.onNodeWithTag("playlist_icon_option_coffee").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dialog_playlist_icon_picker").assertDoesNotExist()

        // Enter playlist name and submit
        composeTestRule.onNodeWithTag("input_playlist_name").performTextInput("Lofi Cafe")
        composeTestRule.onNodeWithTag("btn_confirm_create_playlist").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Lofi Cafe", createdName)
        assertEquals("coffee", createdIconId)
    }

    @Test
    fun `choosing playlist icon in detail screen updates oversized watermark across playlists overview and songs carousel`() {
        val song1 = Song(1L, "Đêm Mưa.mp3", Uri.parse("content://media/1"), 180_000L, 1L)
        var sharedPlaylist by mutableStateOf(
            Playlist(id = 88L, name = "Đêm Khuya", createdAt = 1L, songs = listOf(song1), iconId = null)
        )
        var screenMode by mutableStateOf("overview")

        composeTestRule.setContent {
            MyApplicationTheme {
                when (screenMode) {
                    "detail" -> PlaylistDetailScreen(
                        playlist = sharedPlaylist,
                        playbackState = PlaybackState(),
                        onBack = {},
                        onPlayAll = {},
                        onAddSongs = {},
                        onUpdatePlaylistIcon = { newIconId ->
                            sharedPlaylist = sharedPlaylist.copy(iconId = newIconId)
                        },
                        onSongClick = {},
                        onRemoveSongFromPlaylist = {},
                        contentTopPadding = 12.dp,
                        contentBottomPadding = 90.dp,
                        isDarkTheme = false
                    )
                    "overview" -> PlaylistsOverviewScreen(
                        playlists = listOf(sharedPlaylist),
                        playbackState = PlaybackState(),
                        onCreatePlaylist = {},
                        onOpenPlaylist = {},
                        onPlayPlaylist = {},
                        onRequestDeletePlaylist = {},
                        contentTopPadding = 12.dp,
                        contentBottomPadding = 90.dp
                    )
                    "songs" -> SongList(
                        songs = listOf(song1),
                        playlists = listOf(sharedPlaylist),
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
        }

        // 1. Default playlist without custom icon uses first music icon ("music_note") as watermark
        composeTestRule.waitForIdle()
        assertEquals(
            PlaylistIconCatalog.DEFAULT_ICON_ID,
            composeTestRule.onNodeWithTag("playlist_card_watermark_88", useUnmergedTree = true)
                .fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        )

        // 2. Open icon picker in PlaylistDetailScreen and select "vinyl"
        screenMode = "detail"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_choose_playlist_icon").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            true,
            composeTestRule.onNodeWithTag("playlist_icon_option_${PlaylistIconCatalog.DEFAULT_ICON_ID}")
                .fetchSemanticsNode().config[SemanticsProperties.Selected]
        )
        composeTestRule.onNodeWithTag("playlist_icon_option_vinyl").performClick()
        composeTestRule.waitForIdle()
        assertEquals("vinyl", sharedPlaylist.iconId)

        // 3. Verify PlaylistsOverviewScreen displays oversized soft "vinyl" watermark for playlist 88
        screenMode = "overview"
        composeTestRule.waitForIdle()
        assertEquals(
            "vinyl",
            composeTestRule.onNodeWithTag("playlist_card_watermark_88", useUnmergedTree = true)
                .fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        )

        // 4. Verify Songs screen Carousel card displays synchronized oversized soft "vinyl" watermark for playlist 88
        screenMode = "songs"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("carousel_playlist_card_88").assertIsDisplayed()
        assertEquals(
            "vinyl",
            composeTestRule.onNodeWithTag("carousel_playlist_watermark_88", useUnmergedTree = true)
                .fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        )
    }

    @Test
    fun `carousel watermark centers horizontally on smallest card size while preserving larger card placement`() {
        // Smallest carousel item sizes (~44..56dp) center the watermark at 0.50f
        assertEquals(0.50f, resolveCarouselWatermarkCenterXFraction(44f), 0.001f)
        assertEquals(0.50f, resolveCarouselWatermarkCenterXFraction(52f), 0.001f)
        assertEquals(0.50f, resolveCarouselWatermarkCenterXFraction(56f), 0.001f)
        // Medium & Large carousel cards keep 0.65f unchanged
        assertEquals(0.65f, resolveCarouselWatermarkCenterXFraction(112f), 0.001f)
        assertEquals(0.65f, resolveCarouselWatermarkCenterXFraction(186f), 0.001f)
    }

    @Test
    fun `bottom toolbar pill scales up to midpoint and shrinks back while tab buttons bounce in place`() {
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

        val songsBounds = composeTestRule.onNodeWithTag("nav_tab_songs").fetchSemanticsNode().boundsInRoot
        val initialIndicator = composeTestRule.onNodeWithTag("nav_pill_active_indicator", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertEquals(songsBounds.left, initialIndicator.left, 2f)
        assertEquals(songsBounds.width, initialIndicator.width, 2f)
        val defaultPillHeight = initialIndicator.height

        val initialPlaylistsContentHeight = composeTestRule.onNodeWithTag("nav_tab_playlists_content", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.height

        // Click PLAYLISTS and pause autoAdvance to observe midpoint pill enlargement and selected tab pop-enlarge + elastic return
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("nav_tab_playlists").performClick()

        var maxPillHeightDuringTravel = defaultPillHeight
        var maxPlaylistsContentHeight = initialPlaylistsContentHeight
        var minPlaylistsContentHeightAfterPeak = initialPlaylistsContentHeight
        var sawEnlargePeak = false

        repeat(35) {
            composeTestRule.mainClock.advanceTimeBy(16L)
            val playlistsBoundsNow = composeTestRule.onNodeWithTag("nav_tab_playlists").fetchSemanticsNode().boundsInRoot
            val playlistsContentNow = composeTestRule.onNodeWithTag("nav_tab_playlists_content", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            val songsBoundsNow = composeTestRule.onNodeWithTag("nav_tab_songs").fetchSemanticsNode().boundsInRoot
            val songsContentNow = composeTestRule.onNodeWithTag("nav_tab_songs_content", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            val indicatorNow = composeTestRule.onNodeWithTag("nav_pill_active_indicator", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot

            if (indicatorNow.height > maxPillHeightDuringTravel) {
                maxPillHeightDuringTravel = indicatorNow.height
            }

            // Tab content stays centered in place inside its button (never chases the pill horizontally)
            assertEquals(playlistsBoundsNow.center.x, playlistsContentNow.center.x, 1.5f)
            assertEquals(songsBoundsNow.center.x, songsContentNow.center.x, 1.5f)

            if (playlistsContentNow.height > maxPlaylistsContentHeight) {
                maxPlaylistsContentHeight = playlistsContentNow.height
            }
            if (maxPlaylistsContentHeight > initialPlaylistsContentHeight * 1.12f) {
                sawEnlargePeak = true
            }
            if (sawEnlargePeak && playlistsContentNow.height < minPlaylistsContentHeightAfterPeak) {
                minPlaylistsContentHeightAfterPeak = playlistsContentNow.height
            }
        }

        assertTrue(
            "Expected dark pill to enlarge moderately around travel midpoint (peak=$maxPillHeightDuringTravel, default=$defaultPillHeight)",
            maxPillHeightDuringTravel > defaultPillHeight * 1.06f
        )
        assertTrue(
            "Expected selected tab icon+label to enlarge slightly on tap (peak=$maxPlaylistsContentHeight, initial=$initialPlaylistsContentHeight)",
            sawEnlargePeak
        )
        assertTrue(
            "Expected elastic spring undershoot below 1.0x after enlarge peak before settling (min=$minPlaylistsContentHeightAfterPeak, initial=$initialPlaylistsContentHeight)",
            minPlaylistsContentHeightAfterPeak < initialPlaylistsContentHeight * 0.98f
        )

        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        val playlistsFinalBounds = composeTestRule.onNodeWithTag("nav_tab_playlists").fetchSemanticsNode().boundsInRoot
        val indicatorAtPlaylists = composeTestRule.onNodeWithTag("nav_pill_active_indicator", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val contentAtPlaylists = composeTestRule.onNodeWithTag("nav_tab_playlists_content", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertEquals(playlistsFinalBounds.left, indicatorAtPlaylists.left, 2.5f)
        assertEquals(playlistsFinalBounds.width, indicatorAtPlaylists.width, 2.5f)
        assertEquals(defaultPillHeight, indicatorAtPlaylists.height, 1.5f)
        assertEquals(playlistsFinalBounds.center.x, contentAtPlaylists.center.x, 1.5f)
        assertEquals(initialPlaylistsContentHeight, contentAtPlaylists.height, 1.5f)

        // Switch back to SONGS tab and verify pill shrinks back to default and content stays centered
        composeTestRule.onNodeWithTag("nav_tab_songs").performClick()
        composeTestRule.waitForIdle()
        val songsFinalBounds = composeTestRule.onNodeWithTag("nav_tab_songs").fetchSemanticsNode().boundsInRoot
        val indicatorAtSongs = composeTestRule.onNodeWithTag("nav_pill_active_indicator", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val contentAtSongs = composeTestRule.onNodeWithTag("nav_tab_songs_content", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertEquals(songsFinalBounds.left, indicatorAtSongs.left, 2.5f)
        assertEquals(songsFinalBounds.width, indicatorAtSongs.width, 2.5f)
        assertEquals(defaultPillHeight, indicatorAtSongs.height, 1.5f)
        assertEquals(songsFinalBounds.center.x, contentAtSongs.center.x, 1.5f)
    }

    @Test
    fun `playlist detail 4-dot drag handle reorders songs strictly vertically within container bounds and updates indices`() {
        val song1 = Song(1L, "Track One.mp3", Uri.parse("content://media/1"), 120_000L, 1L)
        val song2 = Song(2L, "Track Two.mp3", Uri.parse("content://media/2"), 150_000L, 2L)
        val song3 = Song(3L, "Track Three.mp3", Uri.parse("content://media/3"), 180_000L, 3L)
        var playlistState by mutableStateOf(
            Playlist(id = 77L, name = "Top Hits", createdAt = 1L, songs = listOf(song1, song2, song3))
        )
        var lastReorderedIds by mutableStateOf<List<Long>>(emptyList())

        composeTestRule.setContent {
            MyApplicationTheme {
                PlaylistDetailScreen(
                    playlist = playlistState,
                    playbackState = PlaybackState(),
                    onBack = {},
                    onPlayAll = {},
                    onAddSongs = {},
                    onReorderSongs = { reordered ->
                        lastReorderedIds = reordered.map { it.id }
                        playlistState = playlistState.copy(songs = reordered)
                    },
                    onSongClick = {},
                    onRemoveSongFromPlaylist = {},
                    contentTopPadding = 12.dp,
                    contentBottomPadding = 90.dp,
                    isDarkTheme = false
                )
            }
        }

        composeTestRule.waitForIdle()

        val card1InitialBounds = composeTestRule.onNodeWithTag("playlist_song_card_1").fetchSemanticsNode().boundsInRoot
        val card3InitialBounds = composeTestRule.onNodeWithTag("playlist_song_card_3").fetchSemanticsNode().boundsInRoot
        val drag1Bounds = composeTestRule.onNodeWithTag("btn_drag_song_1", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val listBounds = composeTestRule.onNodeWithTag("playlist_detail_lazy_column").fetchSemanticsNode().boundsInRoot

        // 4-dot handle sits at the trailing right edge of the song card
        assertTrue(drag1Bounds.left > card1InitialBounds.center.x)
        assertTrue(drag1Bounds.right <= card1InitialBounds.right + 1f)

        // Long-press the 4-dot handle of song 1 and drag diagonally (large X drift + vertical past song 3 bottom)
        val dragDownDistance = (card3InitialBounds.bottom - card1InitialBounds.center.y) + 120f
        composeTestRule.onNodeWithTag("btn_drag_song_1", useUnmergedTree = true).performTouchInput {
            down(center)
            advanceEventTime(650L)
            moveBy(Offset(140f, dragDownDistance / 2f))
            advanceEventTime(50L)
            moveBy(Offset(-90f, dragDownDistance / 2f))
            advanceEventTime(50L)
        }
        composeTestRule.waitForIdle()

        // Strictly locked to vertical axis (no horizontal drift) and clamped within playlist list bounds
        val card1DuringDrag = composeTestRule.onNodeWithTag("playlist_song_card_1").fetchSemanticsNode().boundsInRoot
        assertEquals(card1InitialBounds.left, card1DuringDrag.left, 2.5f)
        assertTrue(
            "Dragged card bottom (${card1DuringDrag.bottom}) escaped list bounds (${listBounds.bottom})",
            card1DuringDrag.bottom <= listBounds.bottom + 4f
        )
        // Dragging the 4-dot handle must NOT reveal delete button
        composeTestRule.onNodeWithTag("action_remove_from_playlist_1", useUnmergedTree = true).assertDoesNotExist()

        // Release finger -> snaps cleanly and updates order & numbering 1, 2, 3
        composeTestRule.onNodeWithTag("btn_drag_song_1", useUnmergedTree = true).performTouchInput {
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf(2L, 3L, 1L), lastReorderedIds)
        composeTestRule.onNode(
            hasText("1") and hasAnyAncestor(hasTestTag("song_index_2")),
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeTestRule.onNode(
            hasText("2") and hasAnyAncestor(hasTestTag("song_index_3")),
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeTestRule.onNode(
            hasText("3") and hasAnyAncestor(hasTestTag("song_index_1")),
            useUnmergedTree = true
        ).assertIsDisplayed()

        val card2After = composeTestRule.onNodeWithTag("playlist_song_card_2").fetchSemanticsNode().boundsInRoot
        val card3After = composeTestRule.onNodeWithTag("playlist_song_card_3").fetchSemanticsNode().boundsInRoot
        val card1After = composeTestRule.onNodeWithTag("playlist_song_card_1").fetchSemanticsNode().boundsInRoot
        assertTrue(card2After.top < card3After.top)
        assertTrue(card3After.top < card1After.top)
    }

    @Test
    fun `playlist detail removes swipe-left delete gesture and isolates card body long-press reveal from 4-dot drag handle`() {
        val song1 = Song(1L, "Bài Hát A.mp3", Uri.parse("content://media/1"), 120_000L, 1L)
        val song2 = Song(2L, "Bài Hát B.mp3", Uri.parse("content://media/2"), 150_000L, 2L)
        var removedSongId: Long? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                PlaylistDetailScreen(
                    playlist = Playlist(id = 91L, name = "Chill", createdAt = 1L, songs = listOf(song1, song2)),
                    playbackState = PlaybackState(),
                    onBack = {},
                    onPlayAll = {},
                    onAddSongs = {},
                    onSongClick = {},
                    onRemoveSongFromPlaylist = { removedSongId = it.id },
                    contentTopPadding = 12.dp,
                    contentBottomPadding = 90.dp,
                    isDarkTheme = false
                )
            }
        }

        composeTestRule.waitForIdle()

        // 1. Horizontal swipe-left across song card must NOT reveal remove/delete button
        composeTestRule.onNodeWithTag("playlist_song_card_1").performTouchInput {
            swipeLeft()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("action_remove_from_playlist_1", useUnmergedTree = true).assertDoesNotExist()

        // 2. Long-pressing the 4-dot drag handle must NOT reveal remove/delete button
        composeTestRule.onNodeWithTag("btn_drag_song_1", useUnmergedTree = true).performTouchInput {
            longClick()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("action_remove_from_playlist_1", useUnmergedTree = true).assertDoesNotExist()

        // 3. Long-pressing the song card body (outside drag handle) opens and keeps remove button visible after lift
        composeTestRule.onNodeWithTag("playlist_song_card_1").performTouchInput {
            longClick(centerLeft + Offset(40f, 0f))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("action_remove_from_playlist_1", useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag("action_remove_from_playlist_1", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        assertEquals(1L, removedSongId)
    }

    @Test
    fun `bottom toolbar handles rapid 2-tab switching without desyncing active indicator and tab content`() {
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

        // Rapid multi-tap sequence across Songs and Playlists tabs
        composeTestRule.onNodeWithTag("nav_tab_playlists").performClick()
        composeTestRule.mainClock.advanceTimeBy(32L)
        composeTestRule.onNodeWithTag("nav_tab_songs").performClick()
        composeTestRule.mainClock.advanceTimeBy(32L)
        composeTestRule.onNodeWithTag("nav_tab_playlists").performClick()

        composeTestRule.waitForIdle()

        assertEquals(MainTab.PLAYLISTS, currentTab)
        composeTestRule.onNodeWithTag("nav_tab_icon_playlists", useUnmergedTree = true).assertIsDisplayed()
        val playlistsBounds = composeTestRule.onNodeWithTag("nav_tab_playlists").fetchSemanticsNode().boundsInRoot
        val indicatorBounds = composeTestRule.onNodeWithTag("nav_pill_active_indicator", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val contentBounds = composeTestRule.onNodeWithTag("nav_tab_playlists_content", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        assertEquals(playlistsBounds.left, indicatorBounds.left, 2.5f)
        assertEquals(playlistsBounds.width, indicatorBounds.width, 2.5f)
        assertEquals(playlistsBounds.center.x, contentBounds.center.x, 1.5f)
    }

    @Test
    fun `quick return floating header hides on swipe up and immediately floats back on swipe down across Songs and Playlists`() {
        val manySongs = (1..25).map { idx ->
            Song(
                id = idx.toLong(),
                title = "Track Number $idx.mp3",
                uri = Uri.parse("content://media/$idx"),
                duration = 180_000L,
                dateAdded = idx.toLong()
            )
        }
        val manyPlaylists = (1..16).map { idx ->
            Playlist(
                id = (idx * 10).toLong(),
                name = "Playlist $idx",
                createdAt = idx.toLong(),
                songs = listOf(manySongs.first())
            )
        }
        var activeTab by mutableStateOf(MainTab.SONGS)

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (activeTab) {
                        MainTab.SONGS -> {
                            SongList(
                                songs = manySongs,
                                playlists = manyPlaylists.take(4),
                                playbackState = PlaybackState(),
                                onSongClick = {},
                                onAddToPlaylist = {},
                                onRenameSong = { _, _ -> },
                                onDeleteSong = {},
                                contentTopPadding = 12.dp,
                                contentBottomPadding = 90.dp,
                                isDarkTheme = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        MainTab.PLAYLISTS -> {
                            PlaylistsOverviewScreen(
                                playlists = manyPlaylists,
                                playbackState = PlaybackState(),
                                onCreatePlaylist = {},
                                onOpenPlaylist = {},
                                onPlayPlaylist = {},
                                onRequestDeletePlaylist = {},
                                contentTopPadding = 12.dp,
                                contentBottomPadding = 90.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // 1. SONGS TAB: Capsule search bar starts above carousel, hides on swipe up, and slides down proportionally with finger swipe (not all at once)
        composeTestRule.waitForIdle()
        val songsInitialBounds = composeTestRule.onNodeWithTag("songs_search_bar").fetchSemanticsNode().boundsInRoot
        val carouselInitialBounds = composeTestRule.onNodeWithTag("songs_playlists_carousel").fetchSemanticsNode().boundsInRoot
        assertTrue(songsInitialBounds.bottom <= carouselInitialBounds.top)

        composeTestRule.onNodeWithTag("songs_lazy_column").performTouchInput {
            swipeUp(startY = bottom * 0.8f, endY = top + 40f)
        }
        composeTestRule.waitForIdle()
        val songsHiddenBounds = composeTestRule.onNodeWithTag("songs_search_bar").fetchSemanticsNode().boundsInRoot
        assertTrue(songsHiddenBounds.top < songsInitialBounds.top - 30f)

        // Partial downward drag while still scrolled deep in list -> search capsule slides down partially following the finger (does NOT drop all at once)
        composeTestRule.onNodeWithTag("songs_lazy_column").performTouchInput {
            down(center)
            moveBy(Offset(0f, 36f))
            up()
        }
        composeTestRule.waitForIdle()
        val songsPartialBounds = composeTestRule.onNodeWithTag("songs_search_bar").fetchSemanticsNode().boundsInRoot
        assertTrue(songsPartialBounds.top > songsHiddenBounds.top + 8f)
        assertTrue(songsPartialBounds.top < songsInitialBounds.top - 8f)

        // Continue downward drag -> search capsule completes sliding down to full visible position
        composeTestRule.onNodeWithTag("songs_lazy_column").performTouchInput {
            down(center)
            moveBy(Offset(0f, 80f))
            up()
        }
        composeTestRule.waitForIdle()
        val songsRevealedBounds = composeTestRule.onNodeWithTag("songs_search_bar").fetchSemanticsNode().boundsInRoot
        assertEquals(songsInitialBounds.top, songsRevealedBounds.top, 1.5f)

        // 2. PLAYLISTS TAB: Search + Add (+) row starts above first row of playlists, hides on swipe up, and slides down proportionally with finger swipe
        activeTab = MainTab.PLAYLISTS
        composeTestRule.waitForIdle()
        val plHeaderInitial = composeTestRule.onNodeWithTag("playlists_floating_header").fetchSemanticsNode().boundsInRoot
        val plCard1Initial = composeTestRule.onNodeWithTag("playlist_card_10").fetchSemanticsNode().boundsInRoot
        assertTrue(plHeaderInitial.bottom <= plCard1Initial.top)

        composeTestRule.onNodeWithTag("playlists_lazy_grid").performTouchInput {
            swipeUp(startY = bottom * 0.8f, endY = top + 40f)
        }
        composeTestRule.waitForIdle()
        val plHeaderHidden = composeTestRule.onNodeWithTag("playlists_floating_header").fetchSemanticsNode().boundsInRoot
        assertTrue(plHeaderHidden.top < plHeaderInitial.top - 30f)

        composeTestRule.onNodeWithTag("playlists_lazy_grid").performTouchInput {
            down(center)
            moveBy(Offset(0f, 36f))
            up()
        }
        composeTestRule.waitForIdle()
        val plHeaderPartial = composeTestRule.onNodeWithTag("playlists_floating_header").fetchSemanticsNode().boundsInRoot
        assertTrue(plHeaderPartial.top > plHeaderHidden.top + 8f)
        assertTrue(plHeaderPartial.top < plHeaderInitial.top - 8f)

        composeTestRule.onNodeWithTag("playlists_lazy_grid").performTouchInput {
            down(center)
            moveBy(Offset(0f, 80f))
            up()
        }
        composeTestRule.waitForIdle()
        val plHeaderRevealed = composeTestRule.onNodeWithTag("playlists_floating_header").fetchSemanticsNode().boundsInRoot
        assertEquals(plHeaderInitial.top, plHeaderRevealed.top, 1.5f)
    }

    @Test
    fun `app preload screen centers single MD3 indicator until all 2 tabs and data are pre-rendered then fades out and per-page loading keeps toolbar interactive`() {
        val songs = (1L..25L).map { id ->
            Song(id, "Bài Hát $id.mp3", Uri.parse("content://media/$id"), 180_000L, id)
        }
        val playlists = (10L..15L).map { id ->
            Playlist(id = id, name = "Playlist $id", createdAt = id, songs = songs.take(2))
        }
        val posFlow = MutableStateFlow(1_000L)

        var isInitialDataReady by mutableStateOf(false)
        var isSongsLoading by mutableStateOf(true)
        var isPlaylistsLoading by mutableStateOf(true)
        var currentTab by mutableStateOf(MainTab.SONGS)

        composeTestRule.setContent {
            MyApplicationTheme {
                var allTabsPreRendered by remember { mutableStateOf(false) }
                var isToolbarPreRendered by remember { mutableStateOf(false) }
                var hasCompletedAppPreload by remember { mutableStateOf(false) }

                if (!hasCompletedAppPreload && isInitialDataReady && allTabsPreRendered && isToolbarPreRendered) {
                    hasCompletedAppPreload = true
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("preload_test_root")
                ) {
                    KeepAliveMainTabHost(
                        currentTab = currentTab,
                        isInitialDataReady = isInitialDataReady,
                        onAllTabsPreRendered = { allTabsPreRendered = true },
                        modifier = Modifier.fillMaxSize(),
                        songsContent = {
                            SongList(
                                songs = songs,
                                playlists = playlists,
                                playbackState = PlaybackState(currentSong = songs.first(), isPlaying = true, durationMs = 180_000L),
                                onSongClick = {},
                                onAddToPlaylist = {},
                                onRenameSong = { _, _ -> },
                                onDeleteSong = {},
                                contentTopPadding = 36.dp,
                                contentBottomPadding = 144.dp,
                                isDarkTheme = false,
                                isLoading = isSongsLoading,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        playlistsContent = {
                            PlaylistsOverviewScreen(
                                playlists = playlists,
                                playbackState = PlaybackState(currentSong = songs.first(), isPlaying = true, durationMs = 180_000L),
                                onCreatePlaylist = {},
                                onOpenPlaylist = {},
                                onPlayPlaylist = {},
                                onRequestDeletePlaylist = {},
                                contentTopPadding = 36.dp,
                                contentBottomPadding = 144.dp,
                                isLoading = isPlaylistsLoading,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    )

                    EdgeToEdgeGradientScrims(
                        statusBarHeight = 24.dp,
                        navigationBarHeight = 0.dp
                    )

                    CapsuleBottomToolbar(
                        currentTab = currentTab,
                        onSelectTab = { currentTab = it },
                        playbackState = PlaybackState(currentSong = songs.first(), isPlaying = true, durationMs = 180_000L),
                        activePlaylistName = "Playlist 10",
                        currentPositionMsFlow = posFlow,
                        isDarkTheme = false,
                        onCyclePlayMode = {},
                        onTogglePlayPause = {},
                        onSeek = {},
                        onOpenQueue = {},
                        onOpenSettings = {},
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .also { isToolbarPreRendered = true }
                    )

                    if (!hasCompletedAppPreload) {
                        AppPreloadScreen(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. While initial data is not ready, AppPreloadScreen covers full screen with centered MD3 indicator
        val rootBounds = composeTestRule.onNodeWithTag("preload_test_root").fetchSemanticsNode().boundsInRoot
        val preloadBounds = composeTestRule.onNodeWithTag("app_preload_screen").fetchSemanticsNode().boundsInRoot
        val preloadIndicatorBounds = composeTestRule.onNodeWithTag("app_preload_loading_indicator").fetchSemanticsNode().boundsInRoot
        assertEquals(rootBounds.width, preloadBounds.width, 1f)
        assertEquals(rootBounds.height, preloadBounds.height, 1f)
        assertEquals(rootBounds.center.x, preloadIndicatorBounds.center.x, 1.5f)
        assertEquals(rootBounds.center.y, preloadIndicatorBounds.center.y, 1.5f)

        // 2. Mark initial data ready -> all 2 pages pre-compose & measure, and AppPreloadScreen disappears
        isSongsLoading = false
        isPlaylistsLoading = false
        isInitialDataReady = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("app_preload_screen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("songs_lazy_column").assertIsDisplayed()

        // 3. Per-Page Loading on Playlists tab: shows centered MD3 indicator while keeping toolbar and NowPlayingPill interactive
        isPlaylistsLoading = true
        composeTestRule.onNodeWithTag("nav_tab_playlists").performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainTab.PLAYLISTS, currentTab)
        composeTestRule.onNodeWithTag("playlists_page_loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_pill_menu").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_now_playing_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_settings").assertIsDisplayed()

        // User can switch back to Songs tab immediately even while Playlists page is still loading
        isSongsLoading = true
        composeTestRule.onNodeWithTag("nav_tab_songs").performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainTab.SONGS, currentTab)
        composeTestRule.onNodeWithTag("songs_page_loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_pill_menu").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_now_playing_pill").assertIsDisplayed()

        // Complete per-page loading on both Songs and Playlists -> content appears and stays cached across tab switches
        isSongsLoading = false
        isPlaylistsLoading = false
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("songs_page_loading_indicator").assertDoesNotExist()
        composeTestRule.onNodeWithTag("songs_lazy_column").assertIsDisplayed()

        // 4. Verify Edge-to-Edge Vertical Gradient Scrims at top and bottom (minimum 28.dp height when navBar is 0.dp)
        val topScrimBounds = composeTestRule.onNodeWithTag("top_edge_gradient_scrim").fetchSemanticsNode().boundsInRoot
        val bottomScrimBounds = composeTestRule.onNodeWithTag("bottom_edge_gradient_scrim").fetchSemanticsNode().boundsInRoot
        assertEquals(0f, topScrimBounds.top, 1f)
        assertEquals(rootBounds.bottom, bottomScrimBounds.bottom, 1f)
        assertTrue(topScrimBounds.height > 10f)
        assertTrue(bottomScrimBounds.height >= 24f)

        // Verify scrolling works unimpeded through scrims and scroll state is preserved across tab switches
        val initialHeaderTop = composeTestRule.onNodeWithTag("songs_floating_header").fetchSemanticsNode().boundsInRoot.top
        composeTestRule.onNodeWithTag("songs_lazy_column").performTouchInput {
            swipeUp(startY = centerY, endY = top + 20f)
        }
        composeTestRule.waitForIdle()
        val scrolledHeaderTop = composeTestRule.onNodeWithTag("songs_floating_header").fetchSemanticsNode().boundsInRoot.top
        assertTrue(scrolledHeaderTop < initialHeaderTop - 15f)

        composeTestRule.onNodeWithTag("nav_tab_playlists").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("playlist_card_10").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_songs").performClick()
        composeTestRule.waitForIdle()
        // Songs tab kept its scrolled state in memory without resetting to initial top
        val headerTopAfterReturn = composeTestRule.onNodeWithTag("songs_floating_header").fetchSemanticsNode().boundsInRoot.top
        assertEquals(scrolledHeaderTop, headerTopAfterReturn, 1.5f)
    }

    @Test
    fun `headers stay fixed when page content cannot scroll across Songs and Playlists`() {
        val twoSongs = listOf(
            Song(1L, "Short Song 1.mp3", Uri.parse("content://media/1"), 120_000L, 1L),
            Song(2L, "Short Song 2.mp3", Uri.parse("content://media/2"), 120_000L, 2L)
        )
        val twoPlaylists = listOf(
            Playlist(id = 10L, name = "Playlist A", createdAt = 1L, songs = twoSongs),
            Playlist(id = 20L, name = "Playlist B", createdAt = 2L, songs = twoSongs)
        )
        var activeTab by mutableStateOf(MainTab.SONGS)

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (activeTab) {
                        MainTab.SONGS -> {
                            SongList(
                                songs = twoSongs,
                                playlists = emptyList(),
                                playbackState = PlaybackState(),
                                onSongClick = {},
                                onAddToPlaylist = {},
                                onRenameSong = { _, _ -> },
                                onDeleteSong = {},
                                contentTopPadding = 12.dp,
                                contentBottomPadding = 90.dp,
                                isDarkTheme = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        MainTab.PLAYLISTS -> {
                            PlaylistsOverviewScreen(
                                playlists = twoPlaylists,
                                playbackState = PlaybackState(),
                                onCreatePlaylist = {},
                                onOpenPlaylist = {},
                                onPlayPlaylist = {},
                                onRequestDeletePlaylist = {},
                                contentTopPadding = 12.dp,
                                contentBottomPadding = 90.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // 1. SONGS TAB with short content -> swiping up/down does NOT move songs_floating_header
        composeTestRule.waitForIdle()
        val songsHeaderBefore = composeTestRule.onNodeWithTag("songs_floating_header").fetchSemanticsNode().boundsInRoot.top
        composeTestRule.onNodeWithTag("songs_lazy_column").performTouchInput {
            swipeUp(startY = bottom * 0.7f, endY = top + 40f)
        }
        composeTestRule.waitForIdle()
        val songsHeaderAfterSwipeUp = composeTestRule.onNodeWithTag("songs_floating_header").fetchSemanticsNode().boundsInRoot.top
        assertEquals(songsHeaderBefore, songsHeaderAfterSwipeUp, 0.5f)

        // 2. PLAYLISTS TAB with short content -> swiping up/down does NOT move playlists_floating_header
        activeTab = MainTab.PLAYLISTS
        composeTestRule.waitForIdle()
        val playlistsHeaderBefore = composeTestRule.onNodeWithTag("playlists_floating_header").fetchSemanticsNode().boundsInRoot.top
        composeTestRule.onNodeWithTag("playlists_lazy_grid").performTouchInput {
            swipeUp(startY = bottom * 0.7f, endY = top + 40f)
        }
        composeTestRule.waitForIdle()
        val playlistsHeaderAfterSwipeUp = composeTestRule.onNodeWithTag("playlists_floating_header").fetchSemanticsNode().boundsInRoot.top
        assertEquals(playlistsHeaderBefore, playlistsHeaderAfterSwipeUp, 0.5f)
    }
}



