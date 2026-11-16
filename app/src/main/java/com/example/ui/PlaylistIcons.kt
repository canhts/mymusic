package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.theme.LocalizedAppScope

@Immutable
data class PlaylistIconItem(
    val id: String,
    val contentDescription: String,
    val vector: ImageVector
)

object PlaylistIconCatalog {

    val icons: List<PlaylistIconItem> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        listOf(
            // Row 1: Music & Sound
            PlaylistIconItem("music_note", "Music Note", buildMusicNoteIcon()),
            PlaylistIconItem("vinyl", "Vinyl Record", buildVinylIcon()),
            PlaylistIconItem("headphones", "Headphones", buildHeadphonesIcon()),
            PlaylistIconItem("waveform", "Waveform", buildWaveformIcon()),
            PlaylistIconItem("chill_wave", "Chill Wave", buildChillWaveIcon()),
            // Row 2: Cozy & Night
            PlaylistIconItem("coffee", "Coffee Cup", buildCoffeeIcon()),
            PlaylistIconItem("rain", "Rain Cloud", buildRainIcon()),
            PlaylistIconItem("night_moon", "Night Moon", buildNightMoonIcon()),
            PlaylistIconItem("stars", "Sparkling Stars", buildStarsIcon()),
            PlaylistIconItem("heart", "Heart", buildHeartIcon()),
            // Row 3: Energy & Nature
            PlaylistIconItem("flame", "Flame", buildFlameIcon()),
            PlaylistIconItem("leaf", "Leaf", buildLeafIcon()),
            PlaylistIconItem("mountain", "Mountain", buildMountainIcon()),
            PlaylistIconItem("dumbbell", "Workout", buildDumbbellIcon()),
            PlaylistIconItem("gamepad", "Gaming", buildGamepadIcon()),
            // Row 4: Culture & Mood
            PlaylistIconItem("book", "Book", buildBookIcon()),
            PlaylistIconItem("classical_lyre", "Classical Lyre", buildClassicalLyreIcon()),
            PlaylistIconItem("lightning", "Lightning", buildLightningIcon()),
            PlaylistIconItem("sun", "Sun", buildSunIcon()),
            PlaylistIconItem("campfire", "Campfire", buildCampfireIcon()),
            // Row 5: Travel & Lifestyle
            PlaylistIconItem("plane", "Travel Plane", buildPlaneIcon()),
            PlaylistIconItem("bicycle", "Bicycle", buildBicycleIcon()),
            PlaylistIconItem("camera", "Camera", buildCameraIcon()),
            PlaylistIconItem("diamond", "Diamond", buildDiamondIcon()),
            PlaylistIconItem("infinity", "Infinity", buildInfinityIcon()),
            // Row 6: Retro & Studio
            PlaylistIconItem("radio", "Radio", buildRadioIcon()),
            PlaylistIconItem("cassette", "Cassette Tape", buildCassetteIcon()),
            PlaylistIconItem("microphone", "Microphone", buildMicrophoneIcon()),
            PlaylistIconItem("compass", "Compass", buildCompassIcon()),
            PlaylistIconItem("crown", "Crown", buildCrownIcon())
        )
    }

    const val DEFAULT_ICON_ID = "music_note"

    private val iconsById: Map<String, PlaylistIconItem> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        icons.associateBy { it.id }
    }

    val defaultIcon: PlaylistIconItem
        get() = icons.first()

    fun findById(id: String?): PlaylistIconItem? = id?.let { iconsById[it] }

    fun resolveOrDefault(id: String?): PlaylistIconItem = findById(id) ?: defaultIcon

    private fun strokeIcon(
        name: String,
        strokeWidth: Float = 1.85f,
        block: PathBuilder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = "playlist_icon_$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block
        )
    }.build()

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        val k = r * 0.5522848f
        moveTo(cx, cy - r)
        curveTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy)
        curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r)
        curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy)
        curveTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r)
        close()
    }

    private fun buildMusicNoteIcon(): ImageVector = strokeIcon("music_note") {
        circle(6.8f, 17.5f, 2.5f)
        circle(17.2f, 15.5f, 2.5f)
        moveTo(9.3f, 17.5f)
        lineTo(9.3f, 6.2f)
        lineTo(19.7f, 4.3f)
        lineTo(19.7f, 15.5f)
        moveTo(9.3f, 9.6f)
        lineTo(19.7f, 7.7f)
    }

    private fun buildVinylIcon(): ImageVector = strokeIcon("vinyl") {
        circle(12f, 12f, 9f)
        circle(12f, 12f, 2.3f)
        moveTo(6.6f, 8.8f)
        curveTo(7.4f, 7.4f, 8.8f, 6.4f, 10.4f, 6f)
        moveTo(17.4f, 15.2f)
        curveTo(16.6f, 16.6f, 15.2f, 17.6f, 13.6f, 18f)
    }

    private fun buildHeadphonesIcon(): ImageVector = strokeIcon("headphones") {
        moveTo(4f, 14f)
        lineTo(4f, 12f)
        curveTo(4f, 7.58f, 7.58f, 4f, 12f, 4f)
        curveTo(16.42f, 4f, 20f, 7.58f, 20f, 12f)
        lineTo(20f, 14f)
        moveTo(4f, 13.5f)
        lineTo(6.2f, 13.5f)
        curveTo(7.2f, 13.5f, 8f, 14.3f, 8f, 15.3f)
        lineTo(8f, 18.2f)
        curveTo(8f, 19.2f, 7.2f, 20f, 6.2f, 20f)
        lineTo(5.2f, 20f)
        curveTo(4.5f, 20f, 4f, 19.4f, 4f, 18.7f)
        close()
        moveTo(20f, 13.5f)
        lineTo(17.8f, 13.5f)
        curveTo(16.8f, 13.5f, 16f, 14.3f, 16f, 15.3f)
        lineTo(16f, 18.2f)
        curveTo(16f, 19.2f, 16.8f, 20f, 17.8f, 20f)
        lineTo(18.8f, 20f)
        curveTo(19.5f, 20f, 20f, 19.4f, 20f, 18.7f)
        close()
    }

    private fun buildWaveformIcon(): ImageVector = strokeIcon("waveform") {
        moveTo(4f, 10f); lineTo(4f, 14f)
        moveTo(8f, 7f); lineTo(8f, 17f)
        moveTo(12f, 4f); lineTo(12f, 20f)
        moveTo(16f, 8f); lineTo(16f, 16f)
        moveTo(20f, 10.5f); lineTo(20f, 13.5f)
    }

    private fun buildChillWaveIcon(): ImageVector = strokeIcon("chill_wave") {
        moveTo(3f, 9f)
        curveTo(5.5f, 6.5f, 8.5f, 6.5f, 11f, 9f)
        curveTo(13.5f, 11.5f, 16.5f, 11.5f, 21f, 8.5f)
        moveTo(3f, 14.5f)
        curveTo(5.5f, 12f, 8.5f, 12f, 11f, 14.5f)
        curveTo(13.5f, 17f, 16.5f, 17f, 21f, 14f)
        moveTo(6f, 19f)
        curveTo(8.5f, 17.5f, 11.5f, 17.5f, 14f, 19f)
    }

    private fun buildCoffeeIcon(): ImageVector = strokeIcon("coffee") {
        moveTo(5f, 10f)
        lineTo(16.5f, 10f)
        lineTo(16.5f, 14.2f)
        curveTo(16.5f, 17.1f, 14.1f, 19.3f, 10.75f, 19.3f)
        curveTo(7.4f, 19.3f, 5f, 17.1f, 5f, 14.2f)
        close()
        moveTo(16.5f, 11.2f)
        lineTo(18.2f, 11.2f)
        curveTo(19.6f, 11.2f, 20.5f, 12.1f, 20.5f, 13.4f)
        curveTo(20.5f, 14.7f, 19.6f, 15.6f, 18.2f, 15.6f)
        lineTo(16.5f, 15.6f)
        moveTo(8.5f, 6.8f)
        curveTo(8.5f, 5.5f, 9.6f, 5.2f, 9.4f, 3.8f)
        moveTo(12.6f, 6.8f)
        curveTo(12.6f, 5.5f, 13.7f, 5.2f, 13.5f, 3.8f)
        moveTo(4f, 20.8f)
        lineTo(17.5f, 20.8f)
    }

    private fun buildRainIcon(): ImageVector = strokeIcon("rain") {
        moveTo(7.2f, 14.5f)
        lineTo(17.4f, 14.5f)
        curveTo(19.4f, 14.5f, 20.8f, 13.1f, 20.8f, 11.3f)
        curveTo(20.8f, 9.5f, 19.3f, 8.2f, 17.5f, 8.2f)
        curveTo(16.9f, 5.6f, 14.6f, 4f, 11.9f, 4f)
        curveTo(8.9f, 4f, 6.6f, 6.1f, 6.3f, 8.8f)
        curveTo(4.5f, 9f, 3.2f, 10.3f, 3.2f, 11.8f)
        curveTo(3.2f, 13.4f, 4.9f, 14.5f, 7.2f, 14.5f)
        close()
        moveTo(8f, 17.5f); lineTo(7f, 20f)
        moveTo(12.5f, 17.5f); lineTo(11.5f, 20f)
        moveTo(17f, 17.5f); lineTo(16f, 20f)
    }

    private fun buildNightMoonIcon(): ImageVector = strokeIcon("night_moon") {
        moveTo(15.2f, 4.3f)
        curveTo(10.5f, 4.7f, 6.8f, 8.6f, 6.8f, 13.4f)
        curveTo(6.8f, 18.4f, 10.8f, 21.3f, 15.6f, 20.6f)
        curveTo(17.7f, 20.3f, 19.6f, 19.1f, 20.8f, 17.3f)
        curveTo(17.1f, 17.9f, 13.8f, 15.1f, 13.8f, 11.3f)
        curveTo(13.8f, 8.5f, 14.4f, 6.1f, 15.2f, 4.3f)
        close()
        moveTo(18.5f, 6f); lineTo(18.5f, 9f)
        moveTo(17f, 7.5f); lineTo(20f, 7.5f)
    }

    private fun buildStarsIcon(): ImageVector = strokeIcon("stars") {
        moveTo(11f, 3.5f)
        lineTo(12.8f, 8.8f)
        lineTo(18f, 10.5f)
        lineTo(12.8f, 12.2f)
        lineTo(11f, 17.5f)
        lineTo(9.2f, 12.2f)
        lineTo(4f, 10.5f)
        lineTo(9.2f, 8.8f)
        close()
        moveTo(18.2f, 15f); lineTo(18.2f, 19.5f)
        moveTo(16f, 17.25f); lineTo(20.4f, 17.25f)
    }

    private fun buildHeartIcon(): ImageVector = strokeIcon("heart") {
        moveTo(12f, 20.2f)
        curveTo(12f, 20.2f, 4f, 15.2f, 4f, 9.4f)
        curveTo(4f, 6.6f, 6.1f, 4.6f, 8.6f, 4.6f)
        curveTo(10.2f, 4.6f, 11.4f, 5.4f, 12f, 6.7f)
        curveTo(12.6f, 5.4f, 13.8f, 4.6f, 15.4f, 4.6f)
        curveTo(17.9f, 4.6f, 20f, 6.6f, 20f, 9.4f)
        curveTo(20f, 15.2f, 12f, 20.2f, 12f, 20.2f)
        close()
    }

    private fun buildFlameIcon(): ImageVector = strokeIcon("flame") {
        moveTo(12.2f, 3.4f)
        curveTo(14.8f, 6.3f, 18.2f, 9.5f, 18.2f, 13.8f)
        curveTo(18.2f, 17.5f, 15.5f, 20.4f, 12f, 20.4f)
        curveTo(8.5f, 20.4f, 5.8f, 17.6f, 5.8f, 14.1f)
        curveTo(5.8f, 11.4f, 7.4f, 9.2f, 8.8f, 7.6f)
        curveTo(9f, 9.5f, 9.9f, 10.6f, 11.2f, 10.8f)
        curveTo(10.8f, 8.2f, 11.3f, 5.7f, 12.2f, 3.4f)
        close()
    }

    private fun buildLeafIcon(): ImageVector = strokeIcon("leaf") {
        moveTo(19.5f, 4.5f)
        curveTo(11.5f, 4.5f, 5.5f, 9.2f, 5.5f, 16.5f)
        lineTo(5.5f, 18.5f)
        lineTo(7.5f, 18.5f)
        curveTo(14.8f, 18.5f, 19.5f, 12.5f, 19.5f, 4.5f)
        close()
        moveTo(4.5f, 19.5f)
        lineTo(14.2f, 9.8f)
    }

    private fun buildMountainIcon(): ImageVector = strokeIcon("mountain") {
        moveTo(3f, 19f)
        lineTo(9.2f, 7.2f)
        lineTo(14.5f, 16.5f)
        lineTo(17f, 12.4f)
        lineTo(21f, 19f)
        close()
        circle(17.5f, 6.5f, 2f)
    }

    private fun buildDumbbellIcon(): ImageVector = strokeIcon("dumbbell") {
        moveTo(7.5f, 12f); lineTo(16.5f, 12f)
        moveTo(6f, 7.5f); lineTo(6f, 16.5f)
        moveTo(3.5f, 9.3f); lineTo(3.5f, 14.7f)
        moveTo(18f, 7.5f); lineTo(18f, 16.5f)
        moveTo(20.5f, 9.3f); lineTo(20.5f, 14.7f)
    }

    private fun buildGamepadIcon(): ImageVector = strokeIcon("gamepad") {
        moveTo(6.5f, 8.5f)
        lineTo(17.5f, 8.5f)
        curveTo(19.8f, 8.5f, 21.2f, 10.5f, 20.8f, 13.4f)
        lineTo(20.1f, 17f)
        curveTo(19.8f, 18.3f, 18.3f, 18.7f, 17.3f, 17.8f)
        lineTo(15.2f, 15.8f)
        lineTo(8.8f, 15.8f)
        lineTo(6.7f, 17.8f)
        curveTo(5.7f, 18.7f, 4.2f, 18.3f, 3.9f, 17f)
        lineTo(3.2f, 13.4f)
        curveTo(2.8f, 10.5f, 4.2f, 8.5f, 6.5f, 8.5f)
        close()
        moveTo(8f, 10.8f); lineTo(8f, 13.6f)
        moveTo(6.6f, 12.2f); lineTo(9.4f, 12.2f)
        circle(15.2f, 11.6f, 0.7f)
        circle(17.2f, 13.2f, 0.7f)
    }

    private fun buildBookIcon(): ImageVector = strokeIcon("book") {
        moveTo(12f, 6.8f)
        curveTo(10f, 5.2f, 6.6f, 4.8f, 3.8f, 5.4f)
        lineTo(3.8f, 18.2f)
        curveTo(6.6f, 17.6f, 10f, 18f, 12f, 19.6f)
        curveTo(14f, 18f, 17.4f, 17.6f, 20.2f, 18.2f)
        lineTo(20.2f, 5.4f)
        curveTo(17.4f, 4.8f, 14f, 5.2f, 12f, 6.8f)
        close()
        moveTo(12f, 6.8f); lineTo(12f, 19.6f)
    }

    private fun buildClassicalLyreIcon(): ImageVector = strokeIcon("classical_lyre") {
        moveTo(7f, 4.5f)
        curveTo(5.3f, 6.6f, 5.3f, 11.4f, 7.6f, 14.5f)
        curveTo(9f, 16.3f, 10.5f, 17.2f, 12f, 17.2f)
        curveTo(13.5f, 17.2f, 15f, 16.3f, 16.4f, 14.5f)
        curveTo(18.7f, 11.4f, 18.7f, 6.6f, 17f, 4.5f)
        moveTo(6.6f, 5.5f); lineTo(17.4f, 5.5f)
        moveTo(10f, 5.5f); lineTo(10f, 16.8f)
        moveTo(12f, 5.5f); lineTo(12f, 20f)
        moveTo(14f, 5.5f); lineTo(14f, 16.8f)
        moveTo(8.5f, 20f); lineTo(15.5f, 20f)
    }

    private fun buildLightningIcon(): ImageVector = strokeIcon("lightning") {
        moveTo(13.2f, 3.5f)
        lineTo(5.8f, 13.2f)
        lineTo(11.6f, 13.2f)
        lineTo(10.5f, 20.5f)
        lineTo(18.2f, 10.6f)
        lineTo(12.4f, 10.6f)
        close()
    }

    private fun buildSunIcon(): ImageVector = strokeIcon("sun") {
        circle(12f, 12f, 4.2f)
        moveTo(12f, 3f); lineTo(12f, 5.2f)
        moveTo(12f, 18.8f); lineTo(12f, 21f)
        moveTo(3f, 12f); lineTo(5.2f, 12f)
        moveTo(18.8f, 12f); lineTo(21f, 12f)
        moveTo(5.6f, 5.6f); lineTo(7.2f, 7.2f)
        moveTo(16.8f, 16.8f); lineTo(18.4f, 18.4f)
        moveTo(18.4f, 5.6f); lineTo(16.8f, 7.2f)
        moveTo(7.2f, 16.8f); lineTo(5.6f, 18.4f)
    }

    private fun buildCampfireIcon(): ImageVector = strokeIcon("campfire") {
        moveTo(12f, 3.8f)
        curveTo(14.2f, 6.2f, 16.4f, 8.6f, 16.4f, 11.4f)
        curveTo(16.4f, 13.9f, 14.5f, 15.8f, 12f, 15.8f)
        curveTo(9.5f, 15.8f, 7.6f, 13.9f, 7.6f, 11.4f)
        curveTo(7.6f, 8.6f, 9.8f, 6.2f, 12f, 3.8f)
        close()
        moveTo(4.5f, 20.2f); lineTo(19.5f, 16.8f)
        moveTo(4.5f, 16.8f); lineTo(19.5f, 20.2f)
    }

    private fun buildPlaneIcon(): ImageVector = strokeIcon("plane") {
        moveTo(20.5f, 3.8f)
        lineTo(3.5f, 10.6f)
        lineTo(10.2f, 13.4f)
        lineTo(13f, 20.2f)
        close()
        moveTo(10.2f, 13.4f); lineTo(20.5f, 3.8f)
    }

    private fun buildBicycleIcon(): ImageVector = strokeIcon("bicycle") {
        circle(6f, 16.5f, 3.3f)
        circle(18f, 16.5f, 3.3f)
        moveTo(6f, 16.5f)
        lineTo(10.4f, 10f)
        lineTo(15.8f, 10f)
        lineTo(12f, 16.5f)
        close()
        moveTo(18f, 16.5f); lineTo(14.8f, 7.5f); lineTo(17.2f, 7.5f)
        moveTo(9f, 10f); lineTo(11.4f, 10f)
    }

    private fun buildCameraIcon(): ImageVector = strokeIcon("camera") {
        moveTo(4f, 8.5f)
        lineTo(7.4f, 8.5f)
        lineTo(9.1f, 6f)
        lineTo(14.9f, 6f)
        lineTo(16.6f, 8.5f)
        lineTo(20f, 8.5f)
        curveTo(20.6f, 8.5f, 21f, 8.9f, 21f, 9.5f)
        lineTo(21f, 18f)
        curveTo(21f, 18.6f, 20.6f, 19f, 20f, 19f)
        lineTo(4f, 19f)
        curveTo(3.4f, 19f, 3f, 18.6f, 3f, 18f)
        lineTo(3f, 9.5f)
        curveTo(3f, 8.9f, 3.4f, 8.5f, 4f, 8.5f)
        close()
        circle(12f, 13.6f, 3.2f)
    }

    private fun buildDiamondIcon(): ImageVector = strokeIcon("diamond") {
        moveTo(6.5f, 5f)
        lineTo(17.5f, 5f)
        lineTo(21f, 10f)
        lineTo(12f, 20f)
        lineTo(3f, 10f)
        close()
        moveTo(3f, 10f); lineTo(21f, 10f)
        moveTo(9.2f, 5f); lineTo(12f, 20f); lineTo(14.8f, 5f)
    }

    private fun buildInfinityIcon(): ImageVector = strokeIcon("infinity") {
        moveTo(12f, 12f)
        curveTo(10.2f, 9f, 8f, 7.8f, 6f, 7.8f)
        curveTo(4f, 7.8f, 2.8f, 9.6f, 2.8f, 12f)
        curveTo(2.8f, 14.4f, 4f, 16.2f, 6f, 16.2f)
        curveTo(8f, 16.2f, 10.2f, 15f, 12f, 12f)
        curveTo(13.8f, 9f, 16f, 7.8f, 18f, 7.8f)
        curveTo(20f, 7.8f, 21.2f, 9.6f, 21.2f, 12f)
        curveTo(21.2f, 14.4f, 20f, 16.2f, 18f, 16.2f)
        curveTo(16f, 16.2f, 13.8f, 15f, 12f, 12f)
        close()
    }

    private fun buildRadioIcon(): ImageVector = strokeIcon("radio") {
        moveTo(6f, 8.5f); lineTo(18f, 4f)
        moveTo(4f, 9f)
        lineTo(20f, 9f)
        curveTo(20.6f, 9f, 21f, 9.4f, 21f, 10f)
        lineTo(21f, 18.5f)
        curveTo(21f, 19.1f, 20.6f, 19.5f, 20f, 19.5f)
        lineTo(4f, 19.5f)
        curveTo(3.4f, 19.5f, 3f, 19.1f, 3f, 18.5f)
        lineTo(3f, 10f)
        curveTo(3f, 9.4f, 3.4f, 9f, 4f, 9f)
        close()
        circle(8.2f, 14.4f, 2.4f)
        moveTo(13.8f, 12.6f); lineTo(18f, 12.6f)
        moveTo(13.8f, 16.2f); lineTo(18f, 16.2f)
    }

    private fun buildCassetteIcon(): ImageVector = strokeIcon("cassette") {
        moveTo(4f, 6.5f)
        lineTo(20f, 6.5f)
        curveTo(20.6f, 6.5f, 21f, 6.9f, 21f, 7.5f)
        lineTo(21f, 16.5f)
        curveTo(21f, 17.1f, 20.6f, 17.5f, 20f, 17.5f)
        lineTo(4f, 17.5f)
        curveTo(3.4f, 17.5f, 3f, 17.1f, 3f, 16.5f)
        lineTo(3f, 7.5f)
        curveTo(3f, 6.9f, 3.4f, 6.5f, 4f, 6.5f)
        close()
        circle(8f, 11.6f, 1.8f)
        circle(16f, 11.6f, 1.8f)
        moveTo(8f, 13.4f); lineTo(16f, 13.4f)
        moveTo(6.5f, 17.5f); lineTo(8f, 15.2f); lineTo(16f, 15.2f); lineTo(17.5f, 17.5f)
    }

    private fun buildMicrophoneIcon(): ImageVector = strokeIcon("microphone") {
        moveTo(12f, 3.5f)
        curveTo(10.3f, 3.5f, 9f, 4.8f, 9f, 6.5f)
        lineTo(9f, 11.5f)
        curveTo(9f, 13.2f, 10.3f, 14.5f, 12f, 14.5f)
        curveTo(13.7f, 14.5f, 15f, 13.2f, 15f, 11.5f)
        lineTo(15f, 6.5f)
        curveTo(15f, 4.8f, 13.7f, 3.5f, 12f, 3.5f)
        close()
        moveTo(6.2f, 11.2f)
        curveTo(6.2f, 14.5f, 8.8f, 17.2f, 12f, 17.2f)
        curveTo(15.2f, 17.2f, 17.8f, 14.5f, 17.8f, 11.2f)
        moveTo(12f, 17.2f); lineTo(12f, 20.5f)
        moveTo(8.8f, 20.5f); lineTo(15.2f, 20.5f)
    }

    private fun buildCompassIcon(): ImageVector = strokeIcon("compass") {
        circle(12f, 12f, 9f)
        moveTo(15.4f, 8.6f)
        lineTo(13.3f, 13.3f)
        lineTo(8.6f, 15.4f)
        lineTo(10.7f, 10.7f)
        close()
    }

    private fun buildCrownIcon(): ImageVector = strokeIcon("crown") {
        moveTo(3.8f, 17.5f)
        lineTo(4.8f, 7.5f)
        lineTo(9.2f, 11.8f)
        lineTo(12f, 5.8f)
        lineTo(14.8f, 11.8f)
        lineTo(19.2f, 7.5f)
        lineTo(20.2f, 17.5f)
        close()
        moveTo(4.2f, 20f); lineTo(19.8f, 20f)
    }
}

/**
 * Popup chọn biểu tượng Playlist theo phong cách Material Design 3:
 * Bố cục lưới cố định 5 cột x 6 hàng (30 icon tối giản), không hiển thị chữ dưới icon.
 */
@Composable
internal fun PlaylistCustomIconPickerDialog(
    selectedIconId: String?,
    onSelectIcon: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val rows = remember { PlaylistIconCatalog.icons.chunked(5) }

    Dialog(onDismissRequest = onDismiss) {
        LocalizedAppScope {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .testTag("dialog_playlist_icon_picker")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.playlist_icon_picker_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AlertDialogDefaults.titleContentColor,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_icon_picker_grid")
                    ) {
                        rows.forEach { rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowItems.forEach { item ->
                                    val isSelected = item.id == selectedIconId
                                    val cellShape = remember { RoundedCornerShape(14.dp) }
                                    val interactionSource = remember { MutableInteractionSource() }
                                    Surface(
                                        shape = cellShape,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                        contentColor = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        border = if (isSelected) {
                                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                        } else {
                                            null
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                            .aspectRatio(1f)
                                            .springPress(scaleDown = 0.88f, interactionSource = interactionSource)
                                            .clip(cellShape)
                                            .semantics { selected = isSelected }
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null,
                                                onClick = {
                                                    onSelectIcon(item.id)
                                                    onDismiss()
                                                }
                                            )
                                            .testTag("playlist_icon_option_${item.id}")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = item.vector,
                                                contentDescription = item.contentDescription,
                                                modifier = Modifier.size(22.dp)
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

private val UNIT_WATERMARK_GLOW_BRUSH: Brush by lazy {
    Brush.radialGradient(
        colors = listOf(Color.White, Color.Transparent),
        center = Offset.Zero,
        radius = 100f
    )
}

/**
 * Vẽ hình nền hoa văn mờ phóng to từ biểu tượng đại diện của danh sách phát:
 * Kết hợp hào quang dịu + lớp làm mềm đa hướng (soft bloom) ở alpha 0.08f..0.14f
 * để tạo chiều sâu thẩm mỹ mà vẫn giữ nguyên độ tương phản của tiêu đề & thông số.
 */
internal fun DrawScope.drawSoftPlaylistWatermark(
    painter: VectorPainter,
    center: Offset,
    iconSizePx: Float,
    tint: Color,
    alpha: Float,
    rotationDegrees: Float = -14f
) {
    if (iconSizePx <= 1f || alpha <= 0.001f) return

    val glowRadius = iconSizePx * 0.68f
    val glowAlpha = (alpha * 0.65f).coerceIn(0f, 0.10f)
    val colorFilter = ColorFilter.tint(tint)
    if (glowAlpha > 0.001f) {
        val glowScale = glowRadius / 100f
        withTransform({
            translate(left = center.x, top = center.y)
            scale(scaleX = glowScale, scaleY = glowScale, pivot = Offset.Zero)
        }) {
            drawCircle(
                brush = UNIT_WATERMARK_GLOW_BRUSH,
                radius = 100f,
                center = Offset.Zero,
                alpha = glowAlpha,
                colorFilter = colorFilter
            )
        }
    }

    val softBlurOffset = (iconSizePx * 0.020f).coerceIn(1.5f, 5.0f)
    val bloomAlpha = (alpha * 0.30f).coerceIn(0f, 0.045f)
    val drawSize = Size(iconSizePx, iconSizePx)
    val leftX = center.x - iconSizePx * 0.5f
    val topY = center.y - iconSizePx * 0.5f

    rotate(degrees = rotationDegrees, pivot = center) {
        translate(left = leftX - softBlurOffset, top = topY) {
            with(painter) { draw(size = drawSize, alpha = bloomAlpha, colorFilter = colorFilter) }
        }
        translate(left = leftX + softBlurOffset, top = topY) {
            with(painter) { draw(size = drawSize, alpha = bloomAlpha, colorFilter = colorFilter) }
        }
        translate(left = leftX, top = topY - softBlurOffset) {
            with(painter) { draw(size = drawSize, alpha = bloomAlpha, colorFilter = colorFilter) }
        }
        translate(left = leftX, top = topY + softBlurOffset) {
            with(painter) { draw(size = drawSize, alpha = bloomAlpha, colorFilter = colorFilter) }
        }
        translate(left = leftX, top = topY) {
            with(painter) {
                draw(
                    size = drawSize,
                    alpha = alpha.coerceIn(0.03f, 0.12f),
                    colorFilter = colorFilter
                )
            }
        }
    }
}
