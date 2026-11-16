package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.State
import com.example.R

private val WELCOME_CTA_HALO_BASE_COLOR = Color(0xFFFF5C8A)
private val WELCOME_CTA_BUTTON_BRUSH = Brush.linearGradient(
    colors = listOf(Color(0xFFFF5E84), Color(0xFFF63C6B))
)
private val WELCOME_BG_GLOW_BRUSH = Brush.radialGradient(
    colors = listOf(Color(0xFF8347FF).copy(alpha = 0.28f), Color.Transparent),
    center = Offset.Zero,
    radius = 100f
)
private val WELCOME_BACK_SPHERE_BRUSH = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFB29D),
        Color(0xFFF3636D),
        Color(0xFFD63E7A),
        Color(0xFF6B28D9)
    ),
    start = Offset(-20f, -100f),
    end = Offset(40f, 100f)
)
private val WELCOME_BACK_HIGHLIGHT_BRUSH = Brush.radialGradient(
    colors = listOf(Color(0xFFFFC8B4).copy(alpha = 0.55f), Color.Transparent),
    center = Offset.Zero,
    radius = 100f
)
private val WELCOME_BACK_SHADOW_BRUSH = Brush.radialGradient(
    colors = listOf(Color(0xFF4718B8).copy(alpha = 0.65f), Color.Transparent),
    center = Offset.Zero,
    radius = 100f
)
private val WELCOME_FRONT_SPHERE_BRUSH = Brush.linearGradient(
    colors = listOf(
        Color(0xFF6759D9),
        Color(0xFF7B93E8),
        Color(0xFF5CF6FF)
    ),
    start = Offset(0f, -100f),
    end = Offset(-40f, 85f)
)
private val WELCOME_FRONT_PEACH_BRUSH = Brush.radialGradient(
    colors = listOf(
        Color(0xFFFF8F73).copy(alpha = 0.92f),
        Color(0xFFEA7B7E).copy(alpha = 0.52f),
        Color.Transparent
    ),
    center = Offset.Zero,
    radius = 100f
)
private val WELCOME_FRONT_CYAN_BRUSH = Brush.radialGradient(
    colors = listOf(
        Color(0xFF9EFFFF).copy(alpha = 0.96f),
        Color(0xFF4BEBFF).copy(alpha = 0.65f),
        Color.Transparent
    ),
    center = Offset.Zero,
    radius = 100f
)
private val WELCOME_FRONT_RIM_BRUSH = Brush.radialGradient(
    colors = listOf(Color.Transparent, Color(0xFFB388FF).copy(alpha = 0.28f)),
    center = Offset.Zero,
    radius = 100f
)

/**
 * Trang khởi động khi mới cài đặt ứng dụng (Onboarding / Start Screen):
 * - Nền chuyển sắc tím hoàng gia sống động hòa quyện cùng hai khối cầu màu 3D mượt mà ở nửa trên.
 * - Nội dung giới thiệu trực tiếp bằng ngôn ngữ của ứng dụng (Thư viện MP3, Danh sách phát, Hòa ca, Nhịp nhạc).
 * - Thanh hành động dưới đáy gồm nhãn "Bắt đầu" bên trái và nút tròn san hô kèm quầng hào quang bên phải.
 */
@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "welcome_orb_motion")

    val orbDriftState = infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome_orb_drift"
    )

    val haloPulseState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome_halo_pulse"
    )

    val arrowNudgeState = infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome_arrow_nudge"
    )

    val themePrimary = MaterialTheme.colorScheme.primary
    val backgroundBrush = remember(themePrimary) {
        val deepVioletTop = lerp(Color(0xFF4712D4), themePrimary, 0.14f)
        val deepVioletMid = lerp(Color(0xFF5A21E6), themePrimary, 0.16f)
        val deepVioletBottom = lerp(Color(0xFF4514C7), themePrimary, 0.12f)
        Brush.verticalGradient(
            colors = listOf(deepVioletTop, deepVioletMid, deepVioletBottom)
        )
    }

    val triggerStart = rememberDebouncedAction(debounceMs = 300L) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onStartClick()
    }

    val startActionLabel = stringResource(R.string.welcome_start_action)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
            .testTag("welcome_screen"),
        contentAlignment = Alignment.Center
    ) {
        val isCompactHeight = maxHeight < 580.dp
        val isTabletOrWide = maxWidth >= 600.dp

        // 1. Nền minh họa 2 khối cầu 3D đa sắc + kết cấu hạt mịn (đọc State chỉ ở Draw Phase)
        WelcomeSpheresHeroCanvas(
            orbDriftState = orbDriftState,
            haloPulseState = haloPulseState,
            isCompactHeight = isCompactHeight,
            modifier = Modifier
                .fillMaxSize()
                .testTag("welcome_hero_visual")
        )

        // 2. Bố cục nội dung chính ở nửa dưới và hàng CTA ở đáy
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .systemBarsPadding()
                .padding(
                    horizontal = if (isTabletOrWide) 36.dp else 28.dp,
                    vertical = if (isCompactHeight) 16.dp else 24.dp
                ),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(580)) + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessLow
                    ),
                    initialOffsetY = { it / 5 }
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.welcome_title),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = if (isCompactHeight) 28.sp else 38.sp,
                            lineHeight = if (isCompactHeight) 32.sp else 43.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.9).sp
                        ),
                        color = Color.White,
                        modifier = Modifier.testTag("welcome_title")
                    )

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 10.dp else 16.dp))

                    Text(
                        text = stringResource(R.string.welcome_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = if (isCompactHeight) 13.sp else 14.5.sp,
                            lineHeight = if (isCompactHeight) 18.sp else 21.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = if (isCompactHeight) 3 else 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .testTag("welcome_subtitle")
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompactHeight) 18.dp else 34.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700)) + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessLow
                    ),
                    initialOffsetY = { it / 3 }
                )
            ) {
                val labelInteractionSource = remember { MutableInteractionSource() }
                val buttonInteractionSource = remember { MutableInteractionSource() }

                val buttonDiameter = if (isCompactHeight) 52.dp else 60.dp
                val haloDiameter = if (isCompactHeight) 68.dp else 78.dp

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (isCompactHeight) 4.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = startActionLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = if (isCompactHeight) 16.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.1.sp
                        ),
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .heightIn(min = 48.dp)
                            .clickable(
                                interactionSource = labelInteractionSource,
                                indication = null,
                                role = Role.Button,
                                onClick = triggerStart
                            )
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                            .testTag("welcome_start_label")
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(haloDiameter)
                    ) {
                        // Vòng hào quang đồng tâm mờ nhẹ tỏa ra xung quanh nút tròn (GPU drawBehind + graphicsLayer)
                        Box(
                            modifier = Modifier
                                .size(haloDiameter)
                                .graphicsLayer {
                                    val pulseScale = 0.96f + 0.08f * haloPulseState.value
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                                .drawBehind {
                                    drawCircle(
                                        color = WELCOME_CTA_HALO_BASE_COLOR.copy(
                                            alpha = 0.22f + 0.08f * haloPulseState.value
                                        )
                                    )
                                }
                        )

                        Surface(
                            onClick = triggerStart,
                            interactionSource = buttonInteractionSource,
                            shape = CircleShape,
                            color = Color.Transparent,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .size(buttonDiameter)
                                .springPress(scaleDown = 0.90f, interactionSource = buttonInteractionSource)
                                .semantics {
                                    contentDescription = startActionLabel
                                }
                                .testTag("btn_welcome_start")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(brush = WELCOME_CTA_BUTTON_BRUSH)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer {
                                            translationX = arrowNudgeState.value.dp.toPx()
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vẽ 2 khối cầu 3D đa sắc đặc trưng ở nửa trên:
 * - Quả cầu san hô - hồng đậm lớn tràn khỏi mép trên phải.
 * - Quả cầu lam ngọc - oải hương - đào dịu nằm chồng phía trước lệch về mép trái trên.
 * - Sử dụng các Brush chuẩn hóa tĩnh + transform GPU để không cấp phát bộ nhớ ở mỗi khung hình.
 */
@Composable
private fun WelcomeSpheresHeroCanvas(
    orbDriftState: State<Float>,
    haloPulseState: State<Float>,
    isCompactHeight: Boolean,
    modifier: Modifier = Modifier
) {
    val backSphereClip = remember { Path() }
    val frontSphereClip = remember { Path() }

    val grainPoints = remember {
        buildList {
            var seed = 42917L
            repeat(90) {
                seed = (seed * 6364136223846793005L + 1442695040888963407L)
                val rx = ((seed ushr 16) and 0xFFFFL) / 65535f
                seed = (seed * 6364136223846793005L + 1442695040888963407L)
                val ry = ((seed ushr 16) and 0xFFFFL) / 65535f
                add(Offset(rx, ry))
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val orbDrift = orbDriftState.value
        val haloPulse = haloPulseState.value
        val scaleFactor = if (isCompactHeight) 0.80f else 1.0f

        // 1. Ánh sáng nền nhẹ sau các khối cầu
        val bgGlowRadius = minDim * 0.85f
        val bgGlowScale = bgGlowRadius / 100f
        withTransform({
            translate(left = w * 0.42f, top = h * 0.24f)
            scale(scaleX = bgGlowScale, scaleY = bgGlowScale, pivot = Offset.Zero)
        }) {
            drawCircle(
                brush = WELCOME_BG_GLOW_BRUSH,
                radius = 100f,
                center = Offset.Zero
            )
        }

        // 2. Quả cầu sau (Coral - Rose - Violet) tràn mép trên phải
        val backRadius = (minDim * 0.54f * scaleFactor) * (1f + 0.014f * haloPulse)
        val backCenterX = w * 0.58f + orbDrift * 6.dp.toPx()
        val backCenterY = h * (if (isCompactHeight) 0.02f else 0.05f) - orbDrift * 4.dp.toPx()
        val backScale = backRadius / 100f

        backSphereClip.reset()
        backSphereClip.addOval(
            Rect(
                left = backCenterX - backRadius,
                top = backCenterY - backRadius,
                right = backCenterX + backRadius,
                bottom = backCenterY + backRadius
            )
        )
        clipPath(backSphereClip) {
            withTransform({
                translate(left = backCenterX, top = backCenterY)
                scale(scaleX = backScale, scaleY = backScale, pivot = Offset.Zero)
            }) {
                drawRect(
                    brush = WELCOME_BACK_SPHERE_BRUSH,
                    topLeft = Offset(-102f, -102f),
                    size = Size(204f, 204f)
                )
            }
            // Điểm sáng dịu góc trên trái quả cầu lớn
            val backHiScale = (backRadius * 0.9f) / 100f
            withTransform({
                translate(left = backCenterX - backRadius * 0.35f, top = backCenterY - backRadius * 0.45f)
                scale(scaleX = backHiScale, scaleY = backHiScale, pivot = Offset.Zero)
            }) {
                drawCircle(
                    brush = WELCOME_BACK_HIGHLIGHT_BRUSH,
                    radius = 100f,
                    center = Offset.Zero
                )
            }
            // Bóng tối mềm phần dưới trái quả cầu lớn để hòa vào nền tím
            val backShadowScale = (backRadius * 0.95f) / 100f
            withTransform({
                translate(left = backCenterX - backRadius * 0.55f, top = backCenterY + backRadius * 0.58f)
                scale(scaleX = backShadowScale, scaleY = backShadowScale, pivot = Offset.Zero)
            }) {
                drawCircle(
                    brush = WELCOME_BACK_SHADOW_BRUSH,
                    radius = 100f,
                    center = Offset.Zero
                )
            }
        }

        // 3. Quả cầu trước (Cyan - Lavender - Peach) lệch góc trên trái và đè lên mép cầu sau
        val frontRadius = (minDim * 0.39f * scaleFactor) * (1f + 0.018f * (1f - haloPulse))
        val frontCenterX = w * 0.17f - orbDrift * 5.dp.toPx()
        val frontCenterY = h * (if (isCompactHeight) 0.29f else 0.33f) + orbDrift * 5.dp.toPx()
        val frontScale = frontRadius / 100f

        frontSphereClip.reset()
        frontSphereClip.addOval(
            Rect(
                left = frontCenterX - frontRadius,
                top = frontCenterY - frontRadius,
                right = frontCenterX + frontRadius,
                bottom = frontCenterY + frontRadius
            )
        )
        clipPath(frontSphereClip) {
            withTransform({
                translate(left = frontCenterX, top = frontCenterY)
                scale(scaleX = frontScale, scaleY = frontScale, pivot = Offset.Zero)
            }) {
                drawRect(
                    brush = WELCOME_FRONT_SPHERE_BRUSH,
                    topLeft = Offset(-102f, -102f),
                    size = Size(204f, 204f)
                )
                drawCircle(
                    brush = WELCOME_FRONT_RIM_BRUSH,
                    radius = 100f,
                    center = Offset.Zero
                )
            }
            // Quầng đào ấm áp phía trên phải của quả cầu trước
            val frontSpotScale = (frontRadius * 0.95f) / 100f
            withTransform({
                translate(left = frontCenterX + frontRadius * 0.42f, top = frontCenterY - frontRadius * 0.14f)
                scale(scaleX = frontSpotScale, scaleY = frontSpotScale, pivot = Offset.Zero)
            }) {
                drawCircle(
                    brush = WELCOME_FRONT_PEACH_BRUSH,
                    radius = 100f,
                    center = Offset.Zero
                )
            }
            // Điểm sáng lam ngọc rực rỡ ở phần dưới trái của quả cầu trước
            withTransform({
                translate(left = frontCenterX - frontRadius * 0.28f, top = frontCenterY + frontRadius * 0.42f)
                scale(scaleX = frontSpotScale, scaleY = frontSpotScale, pivot = Offset.Zero)
            }) {
                drawCircle(
                    brush = WELCOME_FRONT_CYAN_BRUSH,
                    radius = 100f,
                    center = Offset.Zero
                )
            }
        }

        // 4. Lớp hạt mịn nhẹ giúp các dải chuyển màu có chiều sâu mượt mà
        val grainColor = Color.White.copy(alpha = 0.032f)
        val dotRadius = 0.9.dp.toPx()
        for (i in grainPoints.indices) {
            val normOffset = grainPoints[i]
            drawCircle(
                color = grainColor,
                radius = dotRadius,
                center = Offset(normOffset.x * w, normOffset.y * h)
            )
        }
    }
}
