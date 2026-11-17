package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.PlaylistRepository
import com.example.data.analyzePcmSamplesToProfile
import com.example.data.local.AppDatabase
import com.example.model.PlayMode
import com.example.model.Song
import com.example.service.PlaybackManager
import com.example.ui.MainViewModel
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.ThemeMode
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppDataRobolectricTest {

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

            val playlistId = repo.createPlaylist("Nhạc Chill", iconId = "vinyl")
            val playlistId2 = repo.createPlaylist("Nhạc Lofi")
            repo.addSongToPlaylists(song1.id, listOf(playlistId))
            repo.addSongsToPlaylist(playlistId, listOf(song2.id))

            val resolved = repo.observePlaylistsWithSongs(songsFlow)
                .first { it.size == 2 && it.first().songs.size == 2 }
            assertEquals("Nhạc Chill", resolved.first().name)
            assertEquals("vinyl", resolved.first().iconId)
            assertNull(resolved[1].iconId)
            assertEquals(listOf(playlistId, playlistId2), resolved.map { it.id })
            assertEquals(listOf(10L, 20L), resolved.first().songs.map { it.id })
            assertEquals(300_000L, resolved.first().totalDurationMs)

            // Update playlist icon persistently
            repo.updatePlaylistIcon(playlistId, "coffee")
            val afterIconUpdate = repo.observePlaylistsWithSongs(songsFlow)
                .first { it.firstOrNull { p -> p.id == playlistId }?.iconId == "coffee" }
            assertEquals("coffee", afterIconUpdate.first { it.id == playlistId }.iconId)

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
    fun `mainViewModel persists app language preference across launches`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences("music_player_theme_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove(MainViewModel.KEY_APP_LANGUAGE).commit()

        val firstVm = MainViewModel(app)
        assertEquals(AppLanguage.VI, firstVm.appLanguage.value)

        firstVm.setAppLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, firstVm.appLanguage.value)
        assertEquals("en", prefs.getString(MainViewModel.KEY_APP_LANGUAGE, null))

        val reloadedVm = MainViewModel(app)
        assertEquals(AppLanguage.EN, reloadedVm.appLanguage.value)
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
    fun `SongAudioAnalyzer separates bass treble silence and real beat onset accurately`() {
        val sampleRate = 44_100

        // 1. Digital silence produces zero energy (no fake beats)
        val silenceSamples = FloatArray(sampleRate / 2) { 0f }
        val silenceProfile = analyzePcmSamplesToProfile(1L, silenceSamples, sampleRate)
        val silentFrame = silenceProfile.sampleAt(250L)
        assertEquals(0f, silentFrame.overall, 0.01f)
        assertEquals(0f, silentFrame.bass, 0.01f)
        assertEquals(0f, silentFrame.treble, 0.01f)
        assertEquals(0f, silentFrame.onset, 0.01f)

        // 2. 65Hz sub-bass signal activates bass band strongly over treble
        val bassSamples = FloatArray(sampleRate / 2) { i ->
            0.8f * sin(2f * PI.toFloat() * 65f * (i.toFloat() / sampleRate.toFloat()))
        }
        val bassProfile = analyzePcmSamplesToProfile(2L, bassSamples, sampleRate)
        val bassFrame = bassProfile.sampleAt(250L)
        assertTrue(bassFrame.bass > 0.5f)
        assertTrue(bassFrame.bass > bassFrame.treble + 0.35f)

        // 3. 5500Hz high-frequency signal activates treble band strongly over bass
        val trebleSamples = FloatArray(sampleRate / 2) { i ->
            0.8f * sin(2f * PI.toFloat() * 5500f * (i.toFloat() / sampleRate.toFloat()))
        }
        val trebleProfile = analyzePcmSamplesToProfile(3L, trebleSamples, sampleRate)
        val trebleFrame = trebleProfile.sampleAt(250L)
        assertTrue(trebleFrame.treble > 0.5f)
        assertTrue(trebleFrame.treble > trebleFrame.bass + 0.35f)

        // 4. Sudden bass kick at 300ms after quiet intro triggers genuine onset spike at 300ms
        val kickTrack = FloatArray(sampleRate) { i ->
            if (i >= (sampleRate * 0.3f).toInt() && i < (sampleRate * 0.5f).toInt()) {
                0.9f * sin(2f * PI.toFloat() * 75f * (i.toFloat() / sampleRate.toFloat()))
            } else {
                0f
            }
        }
        val kickProfile = analyzePcmSamplesToProfile(4L, kickTrack, sampleRate)
        assertTrue(kickProfile.sampleAt(100L).beatPulse < 0.05f)
        assertTrue(kickProfile.sampleAt(325L).onset > 0.30f)
        assertTrue(kickProfile.sampleAt(325L).beatPulse > 0.45f)
    }

    @Test
    fun `SPHERE_CLOUD_BUFFERS keeps all 3D sphere particles compact within unit radius without scattered dispersion`() {
        val buffers = com.example.ui.SPHERE_CLOUD_BUFFERS
        assertEquals(1860, buffers.count)
        assertEquals(1860, buffers.sin3Az.size)
        assertEquals(1860, buffers.quadrupole.size)
        for (i in 0 until buffers.count) {
            val r = buffers.baseR[i]
            assertTrue("Particle $i has dispersed radius $r > 1.0f", r in 0.60f..1.0001f)
            assertTrue("Particle $i sizeWeight ${buffers.sizeWeight[i]} out of compact micro-dot range", buffers.sizeWeight[i] in 0.40f..1.30f)
        }
    }

    @Test
    fun `playlistIconCatalog provides 30 distinct custom icons and Room MIGRATION_4_5 runs cleanly`() {
        val icons = com.example.ui.PlaylistIconCatalog.icons
        assertEquals(30, icons.size)
        assertEquals(30, icons.map { it.id }.distinct().size)
        assertEquals("vinyl", com.example.ui.PlaylistIconCatalog.findById("vinyl")?.id)
        assertNull(com.example.ui.PlaylistIconCatalog.findById("non_existent_icon"))

        val sqliteDb = android.database.sqlite.SQLiteDatabase.create(null)
        try {
            sqliteDb.execSQL(
                "CREATE TABLE playlists (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL DEFAULT 0, playCount INTEGER NOT NULL DEFAULT 0, createdAt INTEGER NOT NULL DEFAULT 0)"
            )
            sqliteDb.execSQL("INSERT INTO playlists (name, sortOrder, playCount, createdAt) VALUES ('Old Playlist', 0, 1, 1000)")
            val supportDb = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
            // Directly run ALTER statement equivalent to MIGRATION_4_5 SQL check
            sqliteDb.execSQL("ALTER TABLE playlists ADD COLUMN iconId TEXT DEFAULT NULL")
            val cursor = sqliteDb.rawQuery("SELECT name, iconId FROM playlists WHERE name = 'Old Playlist'", null)
            cursor.use {
                assertTrue(it.moveToFirst())
                assertEquals("Old Playlist", it.getString(0))
                assertTrue(it.isNull(1))
            }
        } finally {
            sqliteDb.close()
        }
    }

    @Test
    fun `reordering playlist songs persists position and updates live PlaybackManager queue without interrupting active song`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val manager = PlaybackManager.getInstance(context)
        try {
            val repo = PlaylistRepository(db.playlistDao())
            val song1 = Song(101L, "one.mp3", Uri.parse("content://media/101"), 120_000L, 1L)
            val song2 = Song(102L, "two.mp3", Uri.parse("content://media/102"), 150_000L, 2L)
            val song3 = Song(103L, "three.mp3", Uri.parse("content://media/103"), 180_000L, 3L)
            val librarySongs = listOf(song1, song2, song3)
            val songsFlow = MutableStateFlow(librarySongs)
            manager.setSongs(librarySongs)

            while (manager.playbackState.value.playMode != PlayMode.REPEAT_ALL) {
                manager.cyclePlayMode()
            }

            val playlistId = repo.createPlaylist("Mix")
            repo.addSongsToPlaylist(playlistId, listOf(song1.id, song2.id, song3.id))

            val initial = repo.observePlaylistsWithSongs(songsFlow)
                .first { it.firstOrNull()?.songs?.size == 3 }
            assertEquals(listOf(101L, 102L, 103L), initial.first().songs.map { it.id })

            // Start playing song2 (index 1) and seek/advance to 42_500ms
            manager.playSongFromPlaylist(playlistId, initial.first().songs, song2)
            manager.updateProgress(42_500L, 150_000L)
            assertEquals(102L, manager.playbackState.value.currentSong?.id)
            assertTrue(manager.playbackState.value.isPlaying)
            assertEquals(42_500L, manager.currentPositionMs.value)

            // Reorder playlist to [song3, song1, song2]
            val reordered = listOf(song3, song1, song2)
            manager.reorderActivePlaylistSongs(playlistId, reordered)
            repo.reorderPlaylistSongs(playlistId, reordered.map { it.id })

            // 1. Live queue updates immediately AND active song continues seamlessly at exact 42_500ms
            assertEquals(listOf(103L, 101L, 102L), manager.getOrderedSongs().map { it.id })
            assertEquals(listOf(103L, 101L, 102L), manager.orderedSongs.value.map { it.id })
            assertEquals(102L, manager.playbackState.value.currentSong?.id)
            assertTrue(manager.playbackState.value.isPlaying)
            assertEquals(42_500L, manager.currentPositionMs.value)

            // 2. Room emits persisted new order and syncPlaylists preserves playback seamlessly
            val persisted = repo.observePlaylistsWithSongs(songsFlow)
                .first { it.firstOrNull()?.songs?.map { s -> s.id } == listOf(103L, 101L, 102L) }
            manager.syncPlaylists(persisted)
            assertEquals(102L, manager.playbackState.value.currentSong?.id)
            assertEquals(42_500L, manager.currentPositionMs.value)

            // 3. Next/Previous now follow the updated queue order [song3, song1, song2]
            manager.playNext() // from song2 (last) wraps to song3 (first)
            assertEquals(103L, manager.playbackState.value.currentSong?.id)
            manager.playNext() // from song3 to song1
            assertEquals(101L, manager.playbackState.value.currentSong?.id)
            manager.playPrevious() // back to song3
            assertEquals(103L, manager.playbackState.value.currentSong?.id)
        } finally {
            manager.stopPlayback()
            db.close()
        }
    }
}
