package com.example.ui

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Chống bấm đúp / nhấn liên tiếp (Rapid-tap debounce) cho các nút mở sheet, hộp thoại hoặc thao tác nặng.
 */
@Composable
internal fun rememberDebouncedAction(
    debounceMs: Long = 240L,
    onClick: () -> Unit
): () -> Unit {
    val latestOnClick by rememberUpdatedState(onClick)
    val lastClickHolder = remember(debounceMs) { longArrayOf(-debounceMs) }
    return remember(debounceMs) {
        {
            val now = SystemClock.elapsedRealtime()
            if (now - lastClickHolder[0] >= debounceMs) {
                lastClickHolder[0] = now
                latestOnClick()
            }
        }
    }
}

/**
 * Hiệu ứng chạm nhún vật lý (Spring Press Feedback):
 * - Tối ưu 60/120fps trên GPU qua lambda graphicsLayer, không gây recompose hay re-layout.
 */
@Composable
internal fun Modifier.springPress(
    scaleDown: Float = 0.92f,
    shiftX: Dp = 0.dp,
    interactionSource: MutableInteractionSource
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "spring_press_scale"
    )

    val currentShiftX by if (shiftX != 0.dp) {
        val density = LocalDensity.current
        val shiftPx = remember(density, shiftX) { with(density) { shiftX.toPx() } }
        animateFloatAsState(
            targetValue = if (isPressed) shiftPx else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "spring_press_shift"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        if (shiftX != 0.dp) {
            translationX = currentShiftX
        }
    }
}

/**
 * Animation nhấn nẩy đàn hồi chuyên biệt cho các nút cụm điều khiển dưới (Bottom Control Cluster).
 * Đọc giá trị animation thuần túy bên trong lambda của graphicsLayer (Draw Phase).
 */
@Composable
internal fun Modifier.bottomControlBounce(
    scaleDown: Float = 0.84f,
    dipY: Dp = 2.5.dp,
    interactionSource: MutableInteractionSource
): Modifier {
    val density = LocalDensity.current
    val dipYPx = remember(density, dipY) { with(density) { dipY.toPx() } }

    val animScale = remember { Animatable(1f) }
    val animTranslationY = remember { Animatable(0f) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    launch {
                        animScale.animateTo(
                            targetValue = scaleDown,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessHigh
                            )
                        )
                    }
                    launch {
                        animTranslationY.animateTo(
                            targetValue = dipYPx,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessHigh
                            )
                        )
                    }
                }
                is PressInteraction.Release -> {
                    launch {
                        if (animScale.value > scaleDown + 0.04f) {
                            animScale.animateTo(
                                targetValue = scaleDown,
                                animationSpec = tween(durationMillis = 60, easing = FastOutLinearInEasing)
                            )
                        }
                        animScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.38f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    launch {
                        if (animTranslationY.value < dipYPx * 0.7f) {
                            animTranslationY.animateTo(
                                targetValue = dipYPx,
                                animationSpec = tween(durationMillis = 60, easing = FastOutLinearInEasing)
                            )
                        }
                        animTranslationY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.40f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
                is PressInteraction.Cancel -> {
                    launch {
                        animScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    launch {
                        animTranslationY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            }
        }
    }

    return this.graphicsLayer {
        scaleX = animScale.value
        scaleY = animScale.value
        translationY = animTranslationY.value
    }
}

/**
 * Đổ bóng tỏa lan tự nhiên (Soft Radiating Shadow) chuẩn MD3:
 * Sử dụng drawWithCache để khởi tạo Paint 1 lần duy nhất (Zero Allocation khi cuộn/animation).
 */
internal fun Modifier.radiantShadow(
    color: Color,
    borderRadius: Dp = 24.dp,
    blurRadius: Dp = 16.dp,
    offsetY: Dp = 4.dp,
    offsetX: Dp = 0.dp,
    spread: Dp = 0.dp,
    isCircle: Boolean = false
): Modifier = this.drawWithCache {
    if (color.alpha <= 0.01f || blurRadius <= 0.dp) {
        onDrawBehind {}
    } else {
        val transparentColor = color.copy(alpha = 0f).toArgb()
        val shadowColor = color.toArgb()
        val blurPx = blurRadius.toPx()
        val offsetXPx = offsetX.toPx()
        val offsetYPx = offsetY.toPx()
        val spreadPx = spread.toPx()
        val radiusPx = borderRadius.toPx()

        val paint = Paint().apply {
            val frameworkPaint = asFrameworkPaint()
            frameworkPaint.color = transparentColor
            frameworkPaint.setShadowLayer(blurPx, offsetXPx, offsetYPx, shadowColor)
        }

        val left = -spreadPx
        val top = -spreadPx
        val right = size.width + spreadPx
        val bottom = size.height + spreadPx
        val circleCenter = Offset((left + right) / 2f, (top + bottom) / 2f)
        val circleRadius = (right - left) / 2f

        onDrawBehind {
            drawIntoCanvas { canvas ->
                if (isCircle) {
                    canvas.drawCircle(circleCenter, circleRadius, paint)
                } else {
                    canvas.drawRoundRect(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        radiusX = radiusPx,
                        radiusY = radiusPx,
                        paint = paint
                    )
                }
            }
        }
    }
}
