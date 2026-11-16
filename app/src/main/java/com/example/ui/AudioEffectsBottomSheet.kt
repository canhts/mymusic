package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.IconButton
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalizedAppScope
import kotlin.math.roundToInt

private data class AudioPreset(
    val name: String,
    val bass: Int,
    val treble: Int,
    val vocal: Int,
    val spatial: Int
)

private val AUDIO_PRESETS = listOf(
    AudioPreset("Siêu trầm", 950, 650, 450, 600),
    AudioPreset("Giọng ca", 400, 750, 900, 350),
    AudioPreset("Sôi động", 750, 800, 600, 500),
    AudioPreset("Vòm 3D", 700, 750, 650, 950),
    AudioPreset("Cân bằng", 500, 500, 500, 0),
    AudioPreset("Mộc", 450, 700, 750, 300)
)

internal fun resolveActiveAudioPresetLabel(
    bassStrength: Int,
    trebleStrength: Int,
    vocalStrength: Int,
    virtualizerStrength: Int
): String {
    if (bassStrength == 800 && trebleStrength == 600 && vocalStrength == 500 && virtualizerStrength == 400) {
        return "Mặc định"
    }
    return AUDIO_PRESETS.firstOrNull { preset ->
        bassStrength == preset.bass &&
            trebleStrength == preset.treble &&
            vocalStrength == preset.vocal &&
            virtualizerStrength == preset.spatial
    }?.name ?: "Tùy chỉnh"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsBottomSheet(
    bassStrength: Int,
    trebleStrength: Int,
    vocalStrength: Int,
    virtualizerStrength: Int,
    onBassChange: (Int, Boolean) -> Unit,
    onTrebleChange: (Int, Boolean) -> Unit,
    onVocalChange: (Int, Boolean) -> Unit,
    onVirtualizerChange: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetShape = remember { RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) }

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
                AudioEffectsContent(
                    bassStrength = bassStrength,
                    trebleStrength = trebleStrength,
                    vocalStrength = vocalStrength,
                    virtualizerStrength = virtualizerStrength,
                    onBassChange = onBassChange,
                    onTrebleChange = onTrebleChange,
                    onVocalChange = onVocalChange,
                    onVirtualizerChange = onVirtualizerChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 18.dp)
                )
            }
        }
    }
}

@Composable
internal fun AudioEffectsContent(
    bassStrength: Int,
    trebleStrength: Int,
    vocalStrength: Int,
    virtualizerStrength: Int,
    onBassChange: (Int, Boolean) -> Unit,
    onTrebleChange: (Int, Boolean) -> Unit,
    onVocalChange: (Int, Boolean) -> Unit,
    onVirtualizerChange: (Int, Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
    inlineSection: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val cardShape = remember { RoundedCornerShape(22.dp) }

    val isDefault = bassStrength == 800 &&
        trebleStrength == 600 &&
        vocalStrength == 500 &&
        virtualizerStrength == 400

    val debouncedReset = rememberDebouncedAction(debounceMs = 200L) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (bassStrength != 800) onBassChange(800, true)
        if (trebleStrength != 600) onTrebleChange(600, true)
        if (vocalStrength != 500) onVocalChange(500, true)
        if (virtualizerStrength != 400) onVirtualizerChange(400, true)
    }

    Column(
        modifier = modifier.testTag("panel_audio_effects")
    ) {
        // Header: đồng bộ tiêu đề nhóm MD3 khi nhúng chung trong Cài đặt, hoặc tiêu đề sheet riêng
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (inlineSection) {
                Text(
                    text = stringResource(R.string.audio_effects_sheet_title),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("btn_equalizer_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.settings_equalizer_back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Text(
                        text = stringResource(R.string.audio_effects_sheet_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            TextButton(
                onClick = debouncedReset,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDefault) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ),
                modifier = Modifier.testTag("btn_reset_audio_state")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = stringResource(R.string.audio_effects_reset),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.audio_effects_reset),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(if (inlineSection) 6.dp else 10.dp))

        // Dải preset tối giản dạng chip không viền
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AUDIO_PRESETS.forEach { preset ->
                val isSelected = bassStrength == preset.bass &&
                    trebleStrength == preset.treble &&
                    vocalStrength == preset.vocal &&
                    virtualizerStrength == preset.spatial

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (bassStrength != preset.bass) onBassChange(preset.bass, true)
                        if (trebleStrength != preset.treble) onTrebleChange(preset.treble, true)
                        if (vocalStrength != preset.vocal) onVocalChange(preset.vocal, true)
                        if (virtualizerStrength != preset.spatial) onVirtualizerChange(preset.spatial, true)
                    },
                    shape = CircleShape,
                    label = {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = null,
                    modifier = Modifier.testTag("preset_${preset.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Khối 4 thanh chỉnh âm gom gọn vào một thẻ duy nhất
        Surface(
            shape = cardShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                CompactEffectSliderRow(
                    title = stringResource(R.string.audio_effects_bass),
                    icon = Icons.Default.GraphicEq,
                    value = bassStrength,
                    onValueChange = onBassChange,
                    testTag = "slider_bass"
                )

                CompactEffectSliderRow(
                    title = stringResource(R.string.audio_effects_treble),
                    icon = Icons.Default.MusicNote,
                    value = trebleStrength,
                    onValueChange = onTrebleChange,
                    testTag = "slider_treble"
                )

                CompactEffectSliderRow(
                    title = stringResource(R.string.audio_effects_vocal),
                    icon = Icons.Default.RecordVoiceOver,
                    value = vocalStrength,
                    onValueChange = onVocalChange,
                    testTag = "slider_vocal"
                )

                CompactEffectSliderRow(
                    title = stringResource(R.string.audio_effects_3d),
                    icon = Icons.Default.SurroundSound,
                    value = virtualizerStrength,
                    onValueChange = onVirtualizerChange,
                    testTag = "slider_spatial"
                )
            }
        }
    }
}

@Composable
private fun CompactEffectSliderRow(
    title: String,
    icon: ImageVector,
    value: Int,
    onValueChange: (Int, Boolean) -> Unit,
    testTag: String
) {
    var sliderValue by remember { mutableFloatStateOf(value.toFloat()) }
    var lastSentInt by remember { mutableIntStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (!isDragging) {
            sliderValue = value.toFloat()
            lastSentInt = value
        }
    }

    val isActive = sliderValue > 5f
    val accentTint by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 160),
        label = "slider_accent_$testTag"
    )

    val dbTenths = remember(sliderValue) {
        val dbVal = sliderValue / 100f
        if (dbVal <= 0.05f) 0 else (dbVal * 10f).roundToInt()
    }
    val formattedDb = remember(dbTenths) {
        if (dbTenths <= 0) {
            "0.0 dB"
        } else {
            val whole = dbTenths / 10
            val frac = dbTenths % 10
            "+$whole.$frac dB"
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = formattedDb,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = accentTint
            )
        }

        Slider(
            value = sliderValue,
            onValueChange = { newVal ->
                isDragging = true
                sliderValue = newVal
                val rounded = newVal.roundToInt()
                if (rounded != lastSentInt) {
                    lastSentInt = rounded
                    onValueChange(rounded, false)
                }
            },
            onValueChangeFinished = {
                isDragging = false
                val rounded = sliderValue.roundToInt()
                lastSentInt = rounded
                onValueChange(rounded, true)
            },
            valueRange = 0f..1000f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}
