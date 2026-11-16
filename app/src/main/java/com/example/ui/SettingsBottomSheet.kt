package com.example.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.AppThemeColor
import com.example.ui.theme.LocalizedAppScope
import com.example.ui.theme.ThemeMode

/**
 * Bảng Cài đặt thiết kế theo chuẩn Google Material Design 3 ModalBottomSheet:
 * - Tiêu đề + nút Thoát ngang hàng bên phải
 * - Nhóm Giao diện & Hiển thị
 * - Bộ chỉnh âm DSP tích hợp trực tiếp bên dưới Giao diện & Hiển thị
 * - Thông tin ứng dụng
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    isDarkTheme: Boolean,
    selectedThemeMode: ThemeMode = ThemeMode.SYSTEM,
    selectedThemeColor: AppThemeColor,
    selectedLanguage: AppLanguage = AppLanguage.DEFAULT,
    onSelectThemeMode: (ThemeMode) -> Unit = {},
    onSelectThemeColor: (AppThemeColor) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onToggleTheme: () -> Unit = {},
    bassStrength: Int = 800,
    trebleStrength: Int = 600,
    vocalStrength: Int = 500,
    virtualizerStrength: Int = 400,
    onBassChange: (Int, Boolean) -> Unit = { _, _ -> },
    onTrebleChange: (Int, Boolean) -> Unit = { _, _ -> },
    onVocalChange: (Int, Boolean) -> Unit = { _, _ -> },
    onVirtualizerChange: (Int, Boolean) -> Unit = { _, _ -> },
    onExitApp: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current

    var showExitDialog by remember { mutableStateOf(false) }

    val sheetShape = remember { RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) }
    val cardGroupShape = remember { RoundedCornerShape(22.dp) }
    val debouncedOpenExitDialog = rememberDebouncedAction(debounceMs = 200L) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        showExitDialog = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        dragHandle = null,
        modifier = modifier
    ) {
        LocalizedAppScope(appLanguage = selectedLanguage) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            val maxSheetHeight = maxHeight * 0.75f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .heightIn(max = maxSheetHeight)
                    .testTag("panel_settings")
            ) {
                // Drag handle chuẩn MD3 gọn gàng trong giới hạn 1/2 màn hình
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(width = 32.dp, height = 4.dp)
                    ) {}
                }

                // Header MD3 cố định phía trên: Tiêu đề Cài đặt bên trái + Nút Thoát ngang hàng bên phải
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp)
                        .testTag("settings_header_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = stringResource(R.string.settings_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("settings_title_text")
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    FilledTonalButton(
                        onClick = debouncedOpenExitDialog,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.testTag("btn_setting_exit_app")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_exit),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp, bottom = 24.dp)
                ) {
                    // Nhóm 1: Giao diện & Hiển thị
                    Text(
                        text = stringResource(R.string.settings_appearance_section),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.4.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = cardGroupShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Hàng 1: Chế độ giao diện (Tự động theo hệ thống / Sáng / Tối)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                                    .testTag("card_setting_theme")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            AnimatedContent(
                                                targetState = selectedThemeMode,
                                                transitionSpec = {
                                                    (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn())
                                                        .togetherWith(scaleOut(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut())
                                                },
                                                label = "theme_icon_anim"
                                            ) { mode ->
                                                val modeIcon = when (mode) {
                                                    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                                }
                                                Icon(
                                                    imageVector = modeIcon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = stringResource(R.string.settings_dark_mode),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = when (selectedThemeMode) {
                                                ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_mode_system_desc)
                                                ThemeMode.LIGHT -> stringResource(R.string.settings_dark_mode_desc_off)
                                                ThemeMode.DARK -> stringResource(R.string.settings_dark_mode_desc_on)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                val systemDesc = stringResource(R.string.settings_theme_mode_system_desc)
                                val themeOptions = listOf(
                                    Triple(ThemeMode.SYSTEM, R.string.settings_theme_mode_system, Icons.Default.BrightnessAuto),
                                    Triple(ThemeMode.LIGHT, R.string.settings_theme_mode_light, Icons.Default.LightMode),
                                    Triple(ThemeMode.DARK, R.string.settings_theme_mode_dark, Icons.Default.DarkMode)
                                )

                                // Cụm chuyển giao diện thiết kế theo Split Buttons của Google Material Design 3 (không viền)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("theme_mode_split_button_group"),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    themeOptions.forEachIndexed { index, (mode, labelRes, iconVec) ->
                                        val isSelected = selectedThemeMode == mode
                                        val labelText = stringResource(labelRes)
                                        val isFirst = index == 0
                                        val isLast = index == themeOptions.lastIndex

                                        val startCorner by animateDpAsState(
                                            targetValue = if (isSelected || isFirst) 21.dp else 6.dp,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            ),
                                            label = "split_start_corner_${mode.id}"
                                        )
                                        val endCorner by animateDpAsState(
                                            targetValue = if (isSelected || isLast) 21.dp else 6.dp,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            ),
                                            label = "split_end_corner_${mode.id}"
                                        )
                                        val containerColor by animateColorAsState(
                                            targetValue = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerLow
                                            },
                                            animationSpec = tween(durationMillis = 180),
                                            label = "split_bg_${mode.id}"
                                        )
                                        val contentColor by animateColorAsState(
                                            targetValue = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            animationSpec = tween(durationMillis = 180),
                                            label = "split_fg_${mode.id}"
                                        )

                                        val splitShape = remember(startCorner, endCorner) {
                                            RoundedCornerShape(
                                                topStart = startCorner,
                                                bottomStart = startCorner,
                                                topEnd = endCorner,
                                                bottomEnd = endCorner
                                            )
                                        }

                                        Surface(
                                            onClick = {
                                                if (selectedThemeMode != mode) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onSelectThemeMode(mode)
                                                }
                                            },
                                            shape = splitShape,
                                            color = containerColor,
                                            contentColor = contentColor,
                                            border = null,
                                            tonalElevation = 0.dp,
                                            shadowElevation = 0.dp,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .semantics {
                                                    if (mode == ThemeMode.SYSTEM) {
                                                        contentDescription = systemDesc
                                                    }
                                                }
                                                .testTag("theme_mode_${mode.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = iconVec,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = labelText,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Hàng 2: 5 màu sắc chủ đạo MD3
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                                    .testTag("card_setting_theme_color")
                            ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = stringResource(R.string.settings_theme_color_title),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    AnimatedContent(
                                        targetState = selectedThemeColor,
                                        transitionSpec = {
                                            (fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                                                .togetherWith(fadeOut() + scaleOut())
                                        },
                                        label = "color_name_anim"
                                    ) { color ->
                                        Text(
                                            text = stringResource(color.nameResId),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppThemeColor.entries.forEach { color ->
                                    val isSelected = color == selectedThemeColor
                                    val interactionSource = remember { MutableInteractionSource() }

                                    val ringScale by animateFloatAsState(
                                        targetValue = if (isSelected) 1f else 0.4f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "ring_scale_${color.id}"
                                    )
                                    val ringAlpha by animateFloatAsState(
                                        targetValue = if (isSelected) 1f else 0f,
                                        animationSpec = tween(durationMillis = 150),
                                        label = "ring_alpha_${color.id}"
                                    )
                                    val swatchScale by animateFloatAsState(
                                        targetValue = if (isSelected) 0.80f else 0.72f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "swatch_scale_${color.id}"
                                    )

                                    val colorName = stringResource(color.nameResId)
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .springPress(scaleDown = 0.88f, interactionSource = interactionSource)
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null,
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onSelectThemeColor(color)
                                                }
                                            )
                                            .semantics {
                                                contentDescription = colorName
                                            }
                                            .testTag("color_swatch_${color.id}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .graphicsLayer {
                                                    scaleX = ringScale
                                                    scaleY = ringScale
                                                    alpha = ringAlpha
                                                }
                                                .border(
                                                    width = 2.dp,
                                                    color = color.previewColor,
                                                    shape = CircleShape
                                                )
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .graphicsLayer {
                                                    scaleX = swatchScale
                                                    scaleY = swatchScale
                                                }
                                                .clip(CircleShape)
                                                .background(color.previewColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Nhóm 2: Hệ thống & Ứng dụng
                    Text(
                        text = stringResource(R.string.settings_system_section),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.4.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = cardGroupShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Hàng 1 nhóm Hệ thống: Chuyển đổi ngôn ngữ Việt Nam (VN) / English (US)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .testTag("card_setting_language")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Language,
                                            contentDescription = null,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = stringResource(R.string.settings_language_title),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    AnimatedContent(
                                        targetState = selectedLanguage,
                                        transitionSpec = {
                                            (fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                                                .togetherWith(fadeOut() + scaleOut())
                                        },
                                        label = "language_subtitle_anim"
                                    ) { language ->
                                        Text(
                                            text = language.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val languageOptions = listOf(
                                AppLanguage.VI to R.string.settings_language_vi,
                                AppLanguage.EN to R.string.settings_language_en
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("language_split_button_group"),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                languageOptions.forEachIndexed { index, (language, labelRes) ->
                                    val isSelected = selectedLanguage == language
                                    val labelText = stringResource(labelRes)
                                    val isFirst = index == 0
                                    val isLast = index == languageOptions.lastIndex

                                    val startCorner by animateDpAsState(
                                        targetValue = if (isSelected || isFirst) 24.dp else 6.dp,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "lang_start_corner_${language.id}"
                                    )
                                    val endCorner by animateDpAsState(
                                        targetValue = if (isSelected || isLast) 24.dp else 6.dp,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "lang_end_corner_${language.id}"
                                    )
                                    val containerColor by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerLow
                                        },
                                        animationSpec = tween(durationMillis = 180),
                                        label = "lang_bg_${language.id}"
                                    )
                                    val contentColor by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        animationSpec = tween(durationMillis = 180),
                                        label = "lang_fg_${language.id}"
                                    )

                                    val splitShape = remember(startCorner, endCorner) {
                                        RoundedCornerShape(
                                            topStart = startCorner,
                                            bottomStart = startCorner,
                                            topEnd = endCorner,
                                            bottomEnd = endCorner
                                        )
                                    }

                                    Surface(
                                        onClick = {
                                            if (selectedLanguage != language) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onSelectLanguage(language)
                                            }
                                        },
                                        shape = splitShape,
                                        color = containerColor,
                                        contentColor = contentColor,
                                        border = null,
                                        tonalElevation = 0.dp,
                                        shadowElevation = 0.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .semantics {
                                                contentDescription = labelText
                                            }
                                            .testTag("lang_mode_${language.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) {
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                        } else {
                                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                                        }
                                                    )
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = language.shortCode,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    ),
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        contentColor
                                                    }
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = labelText,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Hàng 2 nhóm Hệ thống: Thông tin ứng dụng
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("card_setting_about"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_app_logo),
                                    contentDescription = stringResource(R.string.settings_about_title),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = stringResource(R.string.settings_about_title),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.settings_about_desc, BuildConfig.VERSION_NAME),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

    if (showExitDialog) {
        val confirmSource = remember { MutableInteractionSource() }
        val cancelSource = remember { MutableInteractionSource() }

        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                LocalizedAppScope(appLanguage = selectedLanguage) {
                    Text(
                        text = stringResource(R.string.exit_app_dialog_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                LocalizedAppScope(appLanguage = selectedLanguage) {
                    Text(
                        text = stringResource(R.string.exit_app_dialog_desc),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                LocalizedAppScope(appLanguage = selectedLanguage) {
                    val debouncedConfirmExit = rememberDebouncedAction(debounceMs = 300L) {
                        showExitDialog = false
                        onExitApp()
                    }
                    Button(
                        onClick = debouncedConfirmExit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp),
                        interactionSource = confirmSource,
                        modifier = Modifier
                            .springPress(scaleDown = 0.92f, interactionSource = confirmSource)
                            .testTag("btn_confirm_exit_app")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_exit),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                LocalizedAppScope(appLanguage = selectedLanguage) {
                    TextButton(
                        onClick = { showExitDialog = false },
                        shape = RoundedCornerShape(12.dp),
                        interactionSource = cancelSource,
                        modifier = Modifier
                            .springPress(scaleDown = 0.92f, interactionSource = cancelSource)
                            .testTag("btn_cancel_exit_app")
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}
