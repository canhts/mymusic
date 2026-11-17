package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hệ màu quang học Kính lỏng 4 Lớp (`LiquidGlassStyle`):
 * Đánh dấu @Immutable để toàn bộ cấu hình màu sắc không bao giờ gây Recomposition thừa.
 */
@Immutable
internal data class LiquidGlassPalette(
    val frostedTopColor: Color,
    val frostedMiddleColor: Color,
    val frostedBottomColor: Color,
    val frostedVeilColor: Color,
    val rimStartColor: Color,
    val rimMidColor: Color,
    val rimEndColor: Color,
    val rimPressedBoostColor: Color,
    val lensTopColor: Color,
    val lensBottomColor: Color,
    val lensAccentCoreColor: Color,
    val lensInnerRimTopColor: Color,
    val lensInnerRimBottomColor: Color
)

@Composable
internal fun rememberLiquidGlassPalette(
    isDarkTheme: Boolean,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    surfaceTint: Color = if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
): LiquidGlassPalette {
    return remember(isDarkTheme, primaryColor, surfaceTint) {
        if (!isDarkTheme) {
            // Chế độ Sáng:
            // Lớp 1 - Nền kính sương mờ (Frosted Glass Base) pha trắng sữa/xám nhẹ (alpha 0.28f -> 0.42f)
            // kết hợp lớp tán xạ sương mịn giúp chữ và icon luôn sắc nét trên mọi nền danh sách
            val frostTop = Color(0xFFFDFEFF).copy(alpha = 0.42f)
            val frostMid = Color(0xFFF1F5FA).copy(alpha = 0.34f)
            val frostBottom = Color(0xFFE4EAF2).copy(alpha = 0.28f)
            val frostVeil = surfaceTint.copy(alpha = 0.72f)

            // Lớp 2 - Viền khúc xạ ánh sáng 3D (Specular Rim Highlight: 0.85f -> 0.25f -> 0.55f)
            val rimStart = Color.White.copy(alpha = 0.85f)
            val rimMid = Color.White.copy(alpha = 0.25f)
            val rimEnd = Color.White.copy(alpha = 0.55f)
            val rimBoost = Color.White.copy(alpha = 0.96f)

            // Lớp 3 & 4 - Giọt nước thấu kính tối xuyên thấu (Active Liquid Lens Pill: alpha 0.36f -> 0.48f)
            // kết hợp lõi sắc độ chủ đạo để giữ độ tương phản cao cho chữ/icon sáng màu
            val deepNavyTop = lerp(Color(0xFF0B1324), primaryColor, 0.32f).copy(alpha = 0.48f)
            val deepNavyBottom = lerp(Color(0xFF070C18), primaryColor, 0.24f).copy(alpha = 0.36f)
            val lensCore = lerp(primaryColor, Color(0xFF0D1628), 0.38f).copy(alpha = 0.78f)
            val innerRimTop = Color.White.copy(alpha = 0.30f)
            val innerRimBottom = Color.White.copy(alpha = 0.08f)

            LiquidGlassPalette(
                frostedTopColor = frostTop,
                frostedMiddleColor = frostMid,
                frostedBottomColor = frostBottom,
                frostedVeilColor = frostVeil,
                rimStartColor = rimStart,
                rimMidColor = rimMid,
                rimEndColor = rimEnd,
                rimPressedBoostColor = rimBoost,
                lensTopColor = deepNavyTop,
                lensBottomColor = deepNavyBottom,
                lensAccentCoreColor = lensCore,
                lensInnerRimTopColor = innerRimTop,
                lensInnerRimBottomColor = innerRimBottom
            )
        } else {
            // Chế độ Tối:
            val frostTop = Color(0xFF3A4458).copy(alpha = 0.42f)
            val frostMid = Color(0xFF283040).copy(alpha = 0.35f)
            val frostBottom = Color(0xFF1B2230).copy(alpha = 0.28f)
            val frostVeil = surfaceTint.copy(alpha = 0.76f)

            val rimStart = Color.White.copy(alpha = 0.85f)
            val rimMid = Color.White.copy(alpha = 0.25f)
            val rimEnd = Color.White.copy(alpha = 0.55f)
            val rimBoost = Color.White.copy(alpha = 0.96f)

            val deepLensTop = lerp(Color(0xFF070C18), primaryColor, 0.36f).copy(alpha = 0.48f)
            val deepLensBottom = lerp(Color(0xFF040710), primaryColor, 0.24f).copy(alpha = 0.36f)
            val lensCore = lerp(primaryColor, Color(0xFF101B30), 0.30f).copy(alpha = 0.76f)
            val innerRimTop = Color.White.copy(alpha = 0.30f)
            val innerRimBottom = Color.White.copy(alpha = 0.08f)

            LiquidGlassPalette(
                frostedTopColor = frostTop,
                frostedMiddleColor = frostMid,
                frostedBottomColor = frostBottom,
                frostedVeilColor = frostVeil,
                rimStartColor = rimStart,
                rimMidColor = rimMid,
                rimEndColor = rimEnd,
                rimPressedBoostColor = rimBoost,
                lensTopColor = deepLensTop,
                lensBottomColor = deepLensBottom,
                lensAccentCoreColor = lensCore,
                lensInnerRimTopColor = innerRimTop,
                lensInnerRimBottomColor = innerRimBottom
            )
        }
    }
}

/**
 * Modifier vẽ toàn bộ 3 lớp quang học của Thanh viên nhộng 3 nút (Capsule Pill Container)
 * hoàn toàn ở pha Draw (`drawWithCache` -> `onDrawWithContent`) với Zero Object Allocation mỗi khung hình:
 * - Lớp 1: Nền kính sương mờ (`Brush.verticalGradient` pha trắng sữa/xám nhẹ alpha 0.28f..0.42f).
 * - Lớp 2: Viền khúc xạ ánh sáng 3D (`1.25.dp`, `Brush.linearGradient` chéo góc trên-trái -> dưới-phải `0.85f -> 0.25f -> 0.55f`).
 * - Lớp 3: Giọt nước thấu kính tối (`Brush.verticalGradient` alpha `0.36f..0.48f` + viền sáng nội bộ `1.dp` alpha `0.08f..0.30f`)
 *   thụt vào `insetDp` (4.dp) đồng tâm với khung ngoài, tự động kéo giãn ngang & ép dọc theo `motionState`.
 */
internal fun Modifier.liquidGlassPillSurface(
    palette: LiquidGlassPalette,
    motionState: LiquidGlassMotionState,
    insetDp: Dp = 4.dp,
    maxVerticalSquashDp: Dp = 2.25.dp
): Modifier = this.drawWithCache {
    val w = size.width.coerceAtLeast(1f)
    val h = size.height.coerceAtLeast(1f)
    val outerRadius = h * 0.5f
    val outerCornerRadius = CornerRadius(outerRadius, outerRadius)

    val rimWidthPx = 1.25.dp.toPx()
    val halfRimPx = rimWidthPx * 0.5f
    val rimTopLeft = Offset(halfRimPx, halfRimPx)
    val rimSize = Size((w - rimWidthPx).coerceAtLeast(1f), (h - rimWidthPx).coerceAtLeast(1f))
    val rimCornerRadius = CornerRadius(
        (outerRadius - halfRimPx).coerceAtLeast(1f),
        (outerRadius - halfRimPx).coerceAtLeast(1f)
    )
    val rimStroke = Stroke(width = rimWidthPx)

    val innerLensStrokePx = 1.dp.toPx()
    val halfInnerStrokePx = innerLensStrokePx * 0.5f
    val innerLensStroke = Stroke(width = innerLensStrokePx)

    val insetPx = insetDp.toPx()
    val maxSquashPx = maxVerticalSquashDp.toPx()
    val innerContainerWidth = (w - insetPx * 2f).coerceAtLeast(1f)
    val innerContainerHeight = (h - insetPx * 2f).coerceAtLeast(1f)

    // Lưu đệm (Cache) toàn bộ Brush một lần duy nhất khi kích thước hoặc bảng màu thay đổi -> Zero Allocation khi chạy 120Hz
    val frostedBaseBrush = Brush.verticalGradient(
        colors = listOf(
            palette.frostedTopColor,
            palette.frostedMiddleColor,
            palette.frostedBottomColor
        ),
        startY = 0f,
        endY = h
    )

    val specularRimBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to palette.rimStartColor,
            0.52f to palette.rimMidColor,
            1.0f to palette.rimEndColor
        ),
        start = Offset.Zero,
        end = Offset(w, h)
    )

    val liquidLensGradientBrush = Brush.verticalGradient(
        colors = listOf(
            palette.lensTopColor,
            palette.lensBottomColor
        ),
        startY = insetPx,
        endY = h - insetPx
    )

    val innerLensRimBrush = Brush.verticalGradient(
        colors = listOf(
            palette.lensInnerRimTopColor,
            palette.lensInnerRimBottomColor
        ),
        startY = insetPx,
        endY = h - insetPx
    )

    onDrawWithContent {
        // Lớp 1: Nền kính sương mờ (Frosted Glass Base)
        drawRoundRect(
            color = palette.frostedVeilColor,
            cornerRadius = outerCornerRadius
        )
        drawRoundRect(
            brush = frostedBaseBrush,
            cornerRadius = outerCornerRadius
        )

        // Lớp 3: Giọt nước thấu kính tối cho nút đang chọn (Active Liquid Lens Pill)
        val geom = motionState.computeLensGeometry(
            innerWidthPx = innerContainerWidth,
            innerHeightPx = innerContainerHeight,
            maxVerticalSquashPx = maxSquashPx
        )
        val lensW = geom.widthPx
        val lensH = geom.heightPx
        if (lensW > 1f && lensH > 1f) {
            val lensLeft = insetPx + geom.leftPx
            val lensTop = insetPx + geom.topPx
            val lensRadius = lensH * 0.5f
            val lensCorner = CornerRadius(lensRadius, lensRadius)
            val lensTopLeft = Offset(lensLeft, lensTop)
            val lensSize = Size(lensW, lensH)

            drawRoundRect(
                color = palette.lensAccentCoreColor,
                topLeft = lensTopLeft,
                size = lensSize,
                cornerRadius = lensCorner
            )
            drawRoundRect(
                brush = liquidLensGradientBrush,
                topLeft = lensTopLeft,
                size = lensSize,
                cornerRadius = lensCorner
            )

            // Viền sáng nội bộ mảnh (1.dp, alpha 0.08f -> 0.30f) ôm quanh giọt nước đang chọn
            val strokeW = (lensW - innerLensStrokePx).coerceAtLeast(1f)
            val strokeH = (lensH - innerLensStrokePx).coerceAtLeast(1f)
            val strokeRadius = (lensRadius - halfInnerStrokePx).coerceAtLeast(1f)
            drawRoundRect(
                brush = innerLensRimBrush,
                topLeft = Offset(lensLeft + halfInnerStrokePx, lensTop + halfInnerStrokePx),
                size = Size(strokeW, strokeH),
                cornerRadius = CornerRadius(strokeRadius, strokeRadius),
                style = innerLensStroke
            )
        }

        // Vẽ nội dung Icon + Text của 3 nút viên nhộng
        drawContent()

        // Lớp 2: Viền khúc xạ ánh sáng 3D (Specular Rim Highlight 1.25.dp) bao quanh mép ngoài
        drawRoundRect(
            brush = specularRimBrush,
            topLeft = rimTopLeft,
            size = rimSize,
            cornerRadius = rimCornerRadius,
            style = rimStroke
        )
    }
}

/**
 * Modifier vẽ mặt kính lỏng 4 lớp cho Nút tròn (`CircleShape`) hoàn toàn ở pha Draw (`drawWithCache`)
 * với Zero Object Allocation mỗi khung hình:
 * - Trạng thái bình thường: Mặt kính sương mờ + viền sáng khúc xạ 3D `1.25.dp` đồng bộ với thanh viên nhộng.
 * - Khi nhấn giữ (`pressState`) hoặc kích hoạt (`isActive = true`): Phủ lớp thấu kính tối xuyên thấu
 *   (`alpha 0.36f..0.48f`) kèm viền sáng nội bộ `1.dp` và tăng nhẹ độ sáng viền kính ngoài.
 */
internal fun Modifier.liquidGlassCircleSurface(
    palette: LiquidGlassPalette,
    pressState: LiquidGlassPressState,
    isActive: Boolean = false
): Modifier = this.drawWithCache {
    val w = size.width.coerceAtLeast(1f)
    val h = size.height.coerceAtLeast(1f)
    val center = Offset(w * 0.5f, h * 0.5f)
    val outerRadius = minOf(w, h) * 0.5f

    val rimWidthPx = 1.25.dp.toPx()
    val rimRadius = (outerRadius - rimWidthPx * 0.5f).coerceAtLeast(1f)
    val rimStroke = Stroke(width = rimWidthPx)

    val innerLensInsetPx = 3.5.dp.toPx()
    val lensRadius = (outerRadius - innerLensInsetPx).coerceAtLeast(1f)
    val innerLensStrokePx = 1.dp.toPx()
    val innerLensRimRadius = (lensRadius - innerLensStrokePx * 0.5f).coerceAtLeast(1f)
    val innerLensStroke = Stroke(width = innerLensStrokePx)

    val frostedBaseBrush = Brush.verticalGradient(
        colors = listOf(
            palette.frostedTopColor,
            palette.frostedMiddleColor,
            palette.frostedBottomColor
        ),
        startY = 0f,
        endY = h
    )

    val specularRimBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to palette.rimStartColor,
            0.52f to palette.rimMidColor,
            1.0f to palette.rimEndColor
        ),
        start = Offset.Zero,
        end = Offset(w, h)
    )

    val liquidLensGradientBrush = Brush.verticalGradient(
        colors = listOf(
            palette.lensTopColor,
            palette.lensBottomColor
        ),
        startY = innerLensInsetPx,
        endY = h - innerLensInsetPx
    )

    val innerLensRimBrush = Brush.verticalGradient(
        colors = listOf(
            palette.lensInnerRimTopColor,
            palette.lensInnerRimBottomColor
        ),
        startY = innerLensInsetPx,
        endY = h - innerLensInsetPx
    )

    onDrawWithContent {
        val pressProgress = pressState.pressProgressAnim.value.coerceIn(0f, 1f)
        val lensOverlayFraction = if (isActive) {
            1f
        } else {
            (pressProgress * 0.88f).coerceIn(0f, 1f)
        }

        // Lớp 1: Nền kính sương mờ
        drawCircle(
            color = palette.frostedVeilColor,
            radius = outerRadius,
            center = center
        )
        drawCircle(
            brush = frostedBaseBrush,
            radius = outerRadius,
            center = center
        )

        // Lớp 4: Phủ lớp thấu kính tối xuyên thấu khi nút tròn được nhấn giữ hoặc đang kích hoạt (Active/Toggled)
        if (lensOverlayFraction > 0.01f) {
            val activeRadius = if (isActive) outerRadius else lensRadius
            val activeRimRadius = if (isActive) rimRadius else innerLensRimRadius
            drawCircle(
                color = palette.lensAccentCoreColor,
                radius = activeRadius,
                center = center,
                alpha = lensOverlayFraction
            )
            drawCircle(
                brush = liquidLensGradientBrush,
                radius = activeRadius,
                center = center,
                alpha = lensOverlayFraction
            )
            drawCircle(
                brush = innerLensRimBrush,
                radius = activeRimRadius,
                center = center,
                alpha = lensOverlayFraction,
                style = innerLensStroke
            )
        }

        // Vẽ Icon bên trong nút tròn
        drawContent()

        // Lớp 2: Viền khúc xạ ánh sáng 3D (1.25.dp) + tăng nhẹ độ sáng viền khi chạm
        drawCircle(
            brush = specularRimBrush,
            radius = rimRadius,
            center = center,
            style = rimStroke
        )
        if (pressProgress > 0.01f) {
            drawCircle(
                color = palette.rimPressedBoostColor,
                radius = rimRadius,
                center = center,
                alpha = (pressProgress * 0.42f).coerceIn(0f, 1f),
                style = rimStroke
            )
        }
    }
}

/**
 * Nút tròn Liquid Glass độc lập (`LiquidGlassCircleButton`):
 * - Kích thước vuông vắn đồng tâm với chiều cao của thanh viên nhộng (`52.dp`), cắt bo tròn tuyệt đối (`CircleShape`).
 * - Tắt hoàn toàn Ripple mặc định (`indication = null`) để mặt kính luôn trong trẻo.
 * - Tích hợp phản hồi đàn hồi `liquidGlassBounce` (`scale = 0.92f` -> `1.0f`) và 4 lớp kính lỏng ở pha Draw.
 */
@Composable
internal fun LiquidGlassCircleButton(
    onClick: () -> Unit,
    palette: LiquidGlassPalette,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    enabled: Boolean = true,
    isActive: Boolean = false,
    shadowColor: Color = Color.Transparent,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    extraScaleProvider: (() -> Float)? = null,
    rotationZProvider: (() -> Float)? = null,
    testTag: String? = null,
    content: @Composable () -> Unit
) {
    val pressState = rememberLiquidGlassPressState(
        interactionSource = interactionSource,
        pressedScale = 0.92f
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .liquidGlassBounce(
                pressState = pressState,
                extraScaleProvider = extraScaleProvider,
                rotationZProvider = rotationZProvider
            )
            .radiantShadow(
                color = shadowColor,
                blurRadius = 14.dp,
                offsetY = 3.dp,
                isCircle = true
            )
            .clip(CircleShape)
            .liquidGlassCircleSurface(
                palette = palette,
                pressState = pressState,
                isActive = isActive
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    }
}

/**
 * Thanh công cụ kép Liquid Glass (`GlassToolbar`):
 * - Bố cục chia đôi nằm ngang (`Row` với `Alignment.CenterVertically`):
 *   1. Bên trái (Khối chính): Thanh viên nhộng 3 nút (`nav_pill_menu`) với hiệu ứng giọt nước thấu kính tối
 *      trượt đàn hồi bằng lò xo kép lệch pha (`LiquidGlassMotionState`).
 *   2. Bên phải (Khối phụ): Các nút tròn Liquid Glass độc lập xếp ngang hàng và đồng tâm theo chiều dọc
 *      với thanh viên nhộng, cách nhau một khoảng đệm `gap` (`8.dp - 10.dp`).
 */
@Composable
internal fun GlassToolbar(
    palette: LiquidGlassPalette,
    motionState: LiquidGlassMotionState,
    shadowColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    gap: Dp = 10.dp,
    pillInset: Dp = 4.dp,
    pillTestTag: String = "nav_pill_menu",
    pillContent: @Composable BoxScopeWithInset.() -> Unit,
    trailingCirclesContent: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Phần bên trái (Khối chính): Thanh viên nhộng 3 nút Liquid Glass
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .radiantShadow(
                    color = shadowColor,
                    borderRadius = height / 2,
                    blurRadius = 14.dp,
                    offsetY = 3.dp
                )
                .clip(CircleShape)
                .liquidGlassPillSurface(
                    palette = palette,
                    motionState = motionState,
                    insetDp = pillInset
                )
                .testTag(pillTestTag)
                .padding(pillInset)
        ) {
            BoxScopeWithInsetInstance.pillContent()
        }

        // Phần bên phải (Khối phụ): Các nút tròn Liquid Glass đồng tâm
        trailingCirclesContent()
    }
}

internal object BoxScopeWithInsetInstance : BoxScopeWithInset

internal interface BoxScopeWithInset
