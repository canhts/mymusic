package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.CleanedTrackMetadata
import com.example.data.Id3v2LyricsExtractor
import com.example.data.LrcLibRemoteClient
import com.example.data.LyricsRepository
import com.example.data.OfflineSpeechLyricsEngine
import com.example.data.PlaylistRepository
import com.example.data.RemoteLyricsResponse
import com.example.data.analyzePcmSamplesToProfile
import com.example.data.local.AppDatabase
import com.example.model.LyricLine
import com.example.model.LyricsSource
import com.example.model.LyricsUiState
import com.example.model.PlayMode
import com.example.model.Song
import com.example.model.calculateLineProgress
import com.example.service.PlaybackManager
import com.example.ui.MainViewModel
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.ThemeMode
import java.io.ByteArrayInputStream
import java.io.File
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
            var capturedMetadata: CleanedTrackMetadata? = null
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
        assertEquals(1020, buffers.count)
        for (i in 0 until buffers.count) {
            val r = buffers.baseR[i]
            assertTrue("Particle $i has dispersed radius $r > 1.0f", r in 0.60f..1.0001f)
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
}
