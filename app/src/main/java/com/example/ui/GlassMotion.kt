package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Thông số biên ngang của từng ô nút trong thanh viên nhộng.
 * Đánh dấu @Immutable để Compose bỏ qua recomposition không cần thiết.
 */
@Immutable
internal data class GlassTabSlotBounds(
    val leftPx: Float = 0f,
    val widthPx: Float = 0f
) {
    val rightPx: Float get() = leftPx + widthPx
    val centerX: Float get() = leftPx + widthPx * 0.5f
}

/**
 * Kết quả tính toán hình học của giọt nước thấu kính tối (Active Liquid Lens Pill)
 * dùng trực tiếp trong pha Draw/Layout mà không cấp phát đối tượng mới trên mỗi khung hình.
 */
@Stable
internal class LiquidLensFrameGeometry {
    var leftPx: Float = 0f
    var topPx: Float = 0f
    var rightPx: Float = 0f
    var bottomPx: Float = 0f
    var stretchFactor: Float = 0f
    var verticalSquashPx: Float = 0f
    var velocityPxPerSec: Float = 0f

    val widthPx: Float get() = (rightPx - leftPx).coerceAtLeast(0f)
    val heightPx: Float get() = (bottomPx - topPx).coerceAtLeast(0f)
    val centerX: Float get() = leftPx + widthPx * 0.5f
}

/**
 * Bộ điều khiển chuyển động vật lý Kính lỏng (`LiquidGlassMotionState`):
 * - Sử dụng hệ lò xo kép lệch pha (`headPosition` dẫn hướng + `tailPosition` bám đuôi)
 *   kết hợp sóng đàn hồi bề mặt (`bounceCarrier`) chạy song song trong CoroutineScope.
 * - Tự động kéo giãn chiều ngang theo `min(head, tail)` .. `max(head, tail)` và ép nhẹ chiều dọc
 *   (`verticalSquash` 2.dp - 2.5.dp) theo `stretchFactor = abs(head - tail).coerceIn(0f, 1f)`.
 * - An toàn tuyệt đối khi đổi hướng liên tục (Interruption-Safe): kế thừa vận tốc tức thời khi bấm
 *   chuyển nút liên tiếp mà không bao giờ nhảy khung hình.
 * - Toàn bộ thuộc tính `.value` chỉ được đọc ở pha Draw/Layout (0% Recomposition).
 */
@Stable
internal class LiquidGlassMotionState(
    initialIndex: Int,
    val tabCount: Int = 3
) {
    private val clampedInitial = initialIndex.coerceIn(0, (tabCount - 1).coerceAtLeast(0)).toFloat()

    // 1. Đầu dẫn hướng (Leading Edge): vọt nhanh tới nút mới ngay khi chạm
    val headPosition: Animatable<Float, AnimationVector1D> = Animatable(clampedInitial)

    // 2. Đuôi bám theo (Trailing Edge): kéo theo sau với độ trễ mềm mại tạo hình giọt chất lỏng
    val tailPosition: Animatable<Float, AnimationVector1D> = Animatable(clampedInitial)

    // 3. Dao động đàn hồi bề mặt khi giọt chất lỏng hội tụ tại đích
    val bounceCarrier: Animatable<Float, AnimationVector1D> = Animatable(clampedInitial)

    var targetIndex by mutableIntStateOf(initialIndex.coerceIn(0, (tabCount - 1).coerceAtLeast(0)))
        private set

    // Lưu biên đo thực tế của từng tab (theo chỉ số 0, 1, 2) để nội suy chính xác cả khi độ rộng chữ khác nhau
    private val slotLefts = FloatArray(tabCount)
    private val slotWidths = FloatArray(tabCount)
    private val slotMeasured = BooleanArray(tabCount)
    var measuredSlotCount by mutableIntStateOf(0)
        private set

    var isInitialized by mutableStateOf(false)
        private set

    private val reusableGeometry = LiquidLensFrameGeometry()

    fun updateSlotBounds(index: Int, leftPx: Float, widthPx: Float) {
        if (index !in 0 until tabCount || widthPx <= 0.5f) return
        val prevLeft = slotLefts[index]
        val prevWidth = slotWidths[index]
        val wasMeasured = slotMeasured[index]
        if (!wasMeasured || abs(prevLeft - leftPx) > 0.25f || abs(prevWidth - widthPx) > 0.25f) {
            slotLefts[index] = leftPx
            slotWidths[index] = widthPx
            if (!wasMeasured) {
                slotMeasured[index] = true
                var count = 0
                for (i in 0 until tabCount) {
                    if (slotMeasured[i]) count++
                }
                measuredSlotCount = count
            }
            if (!isInitialized && index == targetIndex) {
                isInitialized = true
            }
        }
    }

    fun getSlotBounds(index: Int): GlassTabSlotBounds? {
        if (index !in 0 until tabCount || !slotMeasured[index]) return null
        return GlassTabSlotBounds(leftPx = slotLefts[index], widthPx = slotWidths[index])
    }

    suspend fun snapToTab(index: Int) {
        val clamped = index.coerceIn(0, (tabCount - 1).coerceAtLeast(0))
        targetIndex = clamped
        val targetF = clamped.toFloat()
        headPosition.snapTo(targetF)
        tailPosition.snapTo(targetF)
        bounceCarrier.snapTo(targetF)
        isInitialized = true
    }

    fun animateToTab(index: Int, scope: CoroutineScope) {
        val clamped = index.coerceIn(0, (tabCount - 1).coerceAtLeast(0))
        targetIndex = clamped
        val targetF = clamped.toFloat()

        scope.launch {
            headPosition.animateTo(
                targetValue = targetF,
                initialVelocity = headPosition.velocity,
                animationSpec = HeadSpringSpec
            )
        }
        scope.launch {
            tailPosition.animateTo(
                targetValue = targetF,
                initialVelocity = tailPosition.velocity,
                animationSpec = TailSpringSpec
            )
        }
        scope.launch {
            bounceCarrier.animateTo(
                targetValue = targetF,
                initialVelocity = bounceCarrier.velocity,
                animationSpec = SurfaceBounceSpringSpec
            )
        }
    }

    /**
     * Tính toán hệ số biến dạng kéo giãn tức thời `stretchFactor` trong khoảng `[0f, 1f]`.
     * Chỉ gọi trong pha Draw hoặc Layout.
     */
    fun currentStretchFactor(): Float {
        return (abs(headPosition.value - tailPosition.value) * 3.6f).coerceIn(0f, 1f)
    }

    /**
     * Tính tọa độ khung giọt nước thấu kính tối tại khung hình hiện tại mà không cấp phát bộ nhớ.
     * Chỉ gọi trong pha Draw (`drawWithCache` -> `onDrawWithContent`) hoặc pha Layout.
     */
    fun computeLensGeometry(
        innerWidthPx: Float,
        innerHeightPx: Float,
        maxVerticalSquashPx: Float
    ): LiquidLensFrameGeometry {
        val out = reusableGeometry
        val safeW = innerWidthPx.coerceAtLeast(1f)
        val safeH = innerHeightPx.coerceAtLeast(1f)

        val head = headPosition.value
        val tail = tailPosition.value
        val carrier = bounceCarrier.value

        val minPos = min(head, tail)
        val maxPos = max(head, tail)
        val rawSpan = abs(head - tail)
        val stretch = (rawSpan * 3.6f).coerceIn(0f, 1f)

        // Khi đầu và đuôi hội tụ gần đích, truyền dao động đàn hồi bề mặt để tạo độ nảy tự nhiên
        val convergence = (1f - rawSpan * 2.15f).coerceIn(0f, 1f)
        val carrierShift = (carrier - head) * convergence

        val effectiveMinPos = minPos + carrierShift
        val effectiveMaxPos = maxPos + carrierShift

        val baseLeft = interpolateSlotLeft(effectiveMinPos, safeW)
        val baseRight = interpolateSlotRight(effectiveMaxPos, safeW)

        // Mở rộng thêm biên độ giọt chất lỏng khi đang lướt nhanh giữa các nút
        val avgSlotWidth = (safeW / tabCount.coerceAtLeast(1)).coerceAtLeast(1f)
        val liquidBridgeExpand = stretch * avgSlotWidth * 0.16f

        val rawLeft = baseLeft - liquidBridgeExpand * 0.5f
        val rawRight = baseRight + liquidBridgeExpand * 0.5f
        val rawWidth = (rawRight - rawLeft).coerceAtLeast(avgSlotWidth * 0.5f)

        val clampedBaseWidth = rawWidth.coerceAtMost(safeW)
        val leftOvershoot = (-rawLeft).coerceAtLeast(0f)
        val rightOvershoot = (rawLeft + clampedBaseWidth - safeW).coerceAtLeast(0f)
        val wallBleedPx = min(4f, safeW * 0.015f)

        val visualWidth: Float
        val visualLeft: Float
        when {
            leftOvershoot > 0f -> {
                visualWidth = (clampedBaseWidth - leftOvershoot * 0.35f)
                    .coerceAtLeast(clampedBaseWidth * 0.84f)
                visualLeft = (-leftOvershoot * 0.18f).coerceAtLeast(-wallBleedPx)
            }
            rightOvershoot > 0f -> {
                visualWidth = (clampedBaseWidth - rightOvershoot * 0.35f)
                    .coerceAtLeast(clampedBaseWidth * 0.84f)
                val maxRight = safeW + (rightOvershoot * 0.18f).coerceAtMost(wallBleedPx)
                visualLeft = (maxRight - visualWidth).coerceAtLeast(0f)
            }
            else -> {
                visualWidth = clampedBaseWidth
                visualLeft = rawLeft.coerceIn(0f, (safeW - visualWidth).coerceAtLeast(0f))
            }
        }

        val squashPx = maxVerticalSquashPx * stretch
        val topPx = (squashPx * 0.5f).coerceAtMost(safeH * 0.2f)
        val bottomPx = (safeH - squashPx * 0.5f).coerceAtLeast(topPx + 1f)

        val avgVelocityIndexPerSec = (headPosition.velocity + tailPosition.velocity) * 0.5f
        out.leftPx = visualLeft
        out.topPx = topPx
        out.rightPx = visualLeft + visualWidth
        out.bottomPx = bottomPx
        out.stretchFactor = stretch
        out.verticalSquashPx = squashPx
        out.velocityPxPerSec = avgVelocityIndexPerSec * avgSlotWidth
        return out
    }

    private fun interpolateSlotLeft(position: Float, fallbackTotalWidth: Float): Float {
        if (tabCount <= 0) return 0f
        if (measuredSlotCount < tabCount) {
            val slotW = fallbackTotalWidth / tabCount
            return position * slotW
        }
        val lastIdx = tabCount - 1
        return when {
            position <= 0f -> {
                val step = if (tabCount > 1) (slotLefts[1] - slotLefts[0]) else slotWidths[0]
                slotLefts[0] + position * step
            }
            position >= lastIdx.toFloat() -> {
                val step = if (tabCount > 1) {
                    slotLefts[lastIdx] - slotLefts[lastIdx - 1]
                } else {
                    slotWidths[lastIdx]
                }
                slotLefts[lastIdx] + (position - lastIdx.toFloat()) * step
            }
            else -> {
                val lower = position.toInt().coerceIn(0, lastIdx - 1)
                val upper = lower + 1
                val frac = position - lower.toFloat()
                slotLefts[lower] + (slotLefts[upper] - slotLefts[lower]) * frac
            }
        }
    }

    private fun interpolateSlotRight(position: Float, fallbackTotalWidth: Float): Float {
        if (tabCount <= 0) return fallbackTotalWidth
        if (measuredSlotCount < tabCount) {
            val slotW = fallbackTotalWidth / tabCount
            return (position + 1f) * slotW
        }
        val lastIdx = tabCount - 1
        return when {
            position <= 0f -> {
                val right0 = slotLefts[0] + slotWidths[0]
                val right1 = if (tabCount > 1) slotLefts[1] + slotWidths[1] else right0 + slotWidths[0]
                right0 + position * (right1 - right0)
            }
            position >= lastIdx.toFloat() -> {
                val rightLast = slotLefts[lastIdx] + slotWidths[lastIdx]
                val rightPrev = if (tabCount > 1) {
                    slotLefts[lastIdx - 1] + slotWidths[lastIdx - 1]
                } else {
                    0f
                }
                rightLast + (position - lastIdx.toFloat()) * (rightLast - rightPrev)
            }
            else -> {
                val lower = position.toInt().coerceIn(0, lastIdx - 1)
                val upper = lower + 1
                val frac = position - lower.toFloat()
                val rightLower = slotLefts[lower] + slotWidths[lower]
                val rightUpper = slotLefts[upper] + slotWidths[upper]
                rightLower + (rightUpper - rightLower) * frac
            }
        }
    }

    companion object {
        val HeadSpringSpec: SpringSpec<Float> = spring(
            dampingRatio = 0.78f,
            stiffness = 420f
        )
        val TailSpringSpec: SpringSpec<Float> = spring(
            dampingRatio = 0.82f,
            stiffness = 290f
        )
        val SurfaceBounceSpringSpec: SpringSpec<Float> = spring(
            dampingRatio = 0.52f,
            stiffness = 360f
        )
    }
}

@Composable
internal fun rememberLiquidGlassMotionState(
    selectedIndex: Int,
    tabCount: Int = 3
): LiquidGlassMotionState {
    val scope = rememberCoroutineScope()
    val state = remember(tabCount) {
        LiquidGlassMotionState(initialIndex = selectedIndex, tabCount = tabCount)
    }
    var isFirstComposition by remember { mutableStateOf(true) }

    LaunchedEffect(selectedIndex) {
        if (isFirstComposition) {
            isFirstComposition = false
            state.snapToTab(selectedIndex)
        } else {
            state.animateToTab(selectedIndex, scope)
        }
    }
    return state
}

/**
 * Trạng thái phản hồi chạm đàn hồi cho các nút tròn Liquid Glass (`scale = 0.92f` + tăng sáng viền kính).
 * Toàn bộ giá trị `.value` chỉ được đọc trong `graphicsLayer` và `drawWithCache` -> `onDrawWithContent`
 * để đảm bảo 0% Recomposition khi nhấn/thả.
 */
@Stable
internal class LiquidGlassPressState {
    val scaleAnim = Animatable(1f)
    val pressProgressAnim = Animatable(0f)
}

@Composable
internal fun rememberLiquidGlassPressState(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.92f
): LiquidGlassPressState {
    val state = remember { LiquidGlassPressState() }

    LaunchedEffect(interactionSource, pressedScale) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    launch {
                        state.scaleAnim.animateTo(
                            targetValue = pressedScale,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessHigh
                            )
                        )
                    }
                    launch {
                        state.pressProgressAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessHigh
                            )
                        )
                    }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    launch {
                        state.scaleAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    launch {
                        state.pressProgressAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            }
        }
    }
    return state
}

/**
 * Modifier áp dụng hiệu ứng nhấn đàn hồi Liquid Bounce (`scale = 0.92f` -> `1.0f` với
 * `Spring.DampingRatioMediumBouncy` & `Spring.StiffnessMedium`) hoàn toàn trong pha GPU (`graphicsLayer`).
 */
internal fun Modifier.liquidGlassBounce(
    pressState: LiquidGlassPressState,
    extraScaleProvider: (() -> Float)? = null,
    rotationZProvider: (() -> Float)? = null
): Modifier = this.graphicsLayer {
    val baseScale = pressState.scaleAnim.value
    val extraScale = extraScaleProvider?.invoke() ?: 1f
    val combinedScale = baseScale * extraScale
    scaleX = combinedScale
    scaleY = combinedScale
    if (rotationZProvider != null) {
        rotationZ = rotationZProvider()
    }
}
