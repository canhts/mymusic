package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Trang giới thiệu ứng dụng (Welcome / Onboarding Screen) khi cài mới ứng dụng:
 * - Phong cách siêu tối giản (Ultra-Minimalist), tinh tế và nghệ thuật.
 * - Hoa văn chìm chuyển động mượt mà (Harmonic Silk Waves & Orbital Acoustic Rings) trên toàn màn hình.
 * - Cụm hoạt hình trung tâm (Kinetic Vinyl & Soundwave Sculpture) sống động, kết hợp nhịp sóng âm.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "welcome_artistic_motion")

    // Pha chuyển động của các dải sóng lụa nghệ thuật dưới nền
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "welcome_wave_phase"
    )

    // Góc xoay chậm của hoa văn quỹ đạo âm thanh
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "welcome_orbit_angle"
    )

    // Nhịp thở mượt mà của đĩa tâm điểm và hào quang
    val breathProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome_breath_progress"
    )

    // Chuyển động bồng bềnh của biểu tượng trung tâm
    val floatOffsetDp by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome_float_offset"
    )

    // Độ dịch chuyển nhẹ của mũi tên trên nút Bắt đầu
    val arrowNudgeDp by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome_arrow_nudge"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceColor)
            .testTag("welcome_screen"),
        contentAlignment = Alignment.Center
    ) {
        // 1. Lớp nền hoa văn nghệ thuật siêu tối giản & hoạt hình sóng lụa (Submerged Kinetic Art)
        WelcomeArtisticBackgroundCanvas(
            wavePhase = wavePhase,
            orbitAngle = orbitAngle,
            breathProgress = breathProgress,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            tertiaryColor = tertiaryColor
        )

        // 2. Nội dung chính cân đối theo bố cục tối giản
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 460.dp)
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // Cụm điêu khắc âm thanh hoạt hình + Tiêu đề tối giản ở trung tâm
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(650)) + scaleIn(
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = Spring.StiffnessLow
                    ),
                    initialScale = 0.86f
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Tác phẩm hoạt hình tâm điểm (Kinetic Sound Sculpture)
                    WelcomeKineticHeroSculpture(
                        orbitAngle = orbitAngle,
                        breathProgress = breathProgress,
                        floatOffsetDp = floatOffsetDp,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        tertiaryColor = tertiaryColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Trình phát nhạc",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = onSurfaceColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("welcome_title")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Trải nghiệm nghe nhạc ngoại tuyến mượt mà, tối giản và sống động trên thiết bị của bạn.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 19.sp
                        ),
                        color = onSurfaceVariantColor.copy(alpha = 0.88f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .testTag("welcome_subtitle")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cụm 3 thẻ tính năng siêu gọn gàng dạng viên nhộng kính mờ tự động căn dòng
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WelcomeMinimalFeatureChip(
                            icon = Icons.Rounded.GraphicEq,
                            label = "Nghe ngoại tuyến"
                        )
                        WelcomeMinimalFeatureChip(
                            icon = Icons.Rounded.LibraryMusic,
                            label = "Danh sách phát"
                        )
                        WelcomeMinimalFeatureChip(
                            icon = Icons.Rounded.Tune,
                            label = "Bộ chỉnh âm"
                        )
                    }
                }
            }

            // Nút hành động chính (Primary CTA) tối giản với hiệu ứng nhấn lò xo đàn hồi
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(750)) + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessLow
                    ),
                    initialOffsetY = { it / 2 }
                )
            ) {
                val startInteractionSource = remember { MutableInteractionSource() }
                Button(
                    onClick = onStartClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp),
                    interactionSource = startInteractionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .springPress(scaleDown = 0.95f, interactionSource = startInteractionSource)
                        .radiantShadow(
                            color = primaryColor.copy(alpha = 0.32f),
                            borderRadius = 28.dp,
                            blurRadius = 18.dp,
                            offsetY = 5.dp
                        )
                        .testTag("btn_welcome_start")
                ) {
                    Text(
                        text = "Bắt đầu nghe nhạc",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .offset(x = arrowNudgeDp.dp)
                    )
                }
            }
        }
    }
}

/**
 * Hoa văn nền toàn màn hình siêu tối giản:
 * - Kết hợp quầng sáng cực quang mềm mại, vòng cung quỹ đạo đồng tâm mảnh mai,
 *   và những dải sóng lụa (Harmonic Silk Ribbons) chuyển động êm ái theo thời gian thực.
 */
@Composable
private fun WelcomeArtisticBackgroundCanvas(
    wavePhase: Float,
    orbitAngle: Float,
    breathProgress: Float,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color
) {
    val ribbonPath = remember { Path() }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("welcome_ambient_art")
    ) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)

        // 1. Hào quang cực quang mờ ảo phía trên và phía dưới
        val topGlowCenter = Offset(w * 0.5f, h * 0.34f)
        val topGlowRadius = minDim * (0.72f + 0.08f * breathProgress)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.14f),
                    tertiaryColor.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = topGlowCenter,
                radius = topGlowRadius
            ),
            radius = topGlowRadius,
            center = topGlowCenter
        )

        val bottomGlowCenter = Offset(w * 0.82f, h * 0.86f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = bottomGlowCenter,
                radius = minDim * 0.65f
            ),
            radius = minDim * 0.65f,
            center = bottomGlowCenter
        )

        // 2. Hoa văn vòng cung đồng tâm siêu mảnh ở góc trên phải và góc dưới trái
        val topCornerAnchor = Offset(w * 0.92f, h * 0.08f)
        for (i in 0..2) {
            val r = minDim * (0.28f + i * 0.14f)
            drawArc(
                color = primaryColor.copy(alpha = 0.09f - i * 0.02f),
                startAngle = 115f + sin(wavePhase + i) * 6f,
                sweepAngle = 135f,
                useCenter = false,
                topLeft = Offset(topCornerAnchor.x - r, topCornerAnchor.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = (1.1f - i * 0.2f).dp.toPx(), cap = StrokeCap.Round)
            )
        }

        val bottomCornerAnchor = Offset(w * 0.08f, h * 0.92f)
        for (i in 0..2) {
            val r = minDim * (0.24f + i * 0.13f)
            drawArc(
                color = tertiaryColor.copy(alpha = 0.08f - i * 0.018f),
                startAngle = -65f + cos(wavePhase + i) * 6f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(bottomCornerAnchor.x - r, bottomCornerAnchor.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = (1.0f - i * 0.18f).dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 3. Các dải lụa sóng âm (Harmonic Silk Waves) uốn lượn mềm mại ngang qua không gian
        val ribbonCount = 3
        val steps = 40
        for (ribbonIndex in 0 until ribbonCount) {
            ribbonPath.reset()
            val baseY = h * (0.31f + ribbonIndex * 0.045f)
            val amplitude = minDim * (0.045f + ribbonIndex * 0.014f)
            val phaseShift = wavePhase + ribbonIndex * 0.85f
            for (step in 0..steps) {
                val t = step / steps.toFloat()
                val x = w * t
                val envelope = sin(t * PI).toFloat()
                val y = baseY +
                    sin(t * 2.2f * PI + phaseShift).toFloat() * amplitude * envelope +
                    cos(t * 1.1f * PI - phaseShift * 0.6f).toFloat() * (amplitude * 0.35f)
                if (step == 0) ribbonPath.moveTo(x, y) else ribbonPath.lineTo(x, y)
            }
            val ribbonColor = when (ribbonIndex) {
                0 -> primaryColor
                1 -> tertiaryColor
                else -> secondaryColor
            }
            val ribbonAlpha = 0.14f - ribbonIndex * 0.03f
            drawPath(
                path = ribbonPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        ribbonColor.copy(alpha = ribbonAlpha),
                        ribbonColor.copy(alpha = ribbonAlpha * 1.25f),
                        ribbonColor.copy(alpha = ribbonAlpha),
                        Color.Transparent
                    )
                ),
                style = Stroke(
                    width = (1.35f - ribbonIndex * 0.25f).dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        // 4. Vòng quỹ đạo nghệ thuật xoay chậm quanh tâm điểm
        val sculptureCenter = Offset(w * 0.5f, h * 0.34f)
        val outerOrbitRadius = minDim * 0.33f
        rotate(degrees = orbitAngle * 0.5f, pivot = sculptureCenter) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.07f),
                radius = outerOrbitRadius,
                center = sculptureCenter,
                style = Stroke(width = 0.8.dp.toPx())
            )
        }
    }
}

/**
 * Tác phẩm điêu khắc âm thanh hoạt hình ở trung tâm (Kinetic Vinyl & Soundwave Sculpture):
 * - Các đường gợn sóng đồng tâm co giãn theo nhịp thở.
 * - Vòng quỹ đạo nghệ thuật xoay tròn cùng các hạt vệ tinh âm nhạc.
 * - Đĩa trung tâm lơ lửng kết hợp biểu tượng nốt nhạc và bộ sóng nhạc (Equalizer) đang nhịp.
 */
@Composable
private fun WelcomeKineticHeroSculpture(
    orbitAngle: Float,
    breathProgress: Float,
    floatOffsetDp: Float,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(148.dp)
            .testTag("welcome_hero_visual")
    ) {
        // Lớp hoa văn quỹ đạo & vòng sóng âm đồng tâm xoay mượt
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f

            // Vòng gợn sóng lan tỏa theo nhịp thở
            val pulseRadius1 = maxR * (0.82f + 0.14f * breathProgress)
            drawCircle(
                color = primaryColor.copy(alpha = 0.14f * (1f - breathProgress * 0.5f)),
                radius = pulseRadius1,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )

            val pulseRadius2 = maxR * (0.68f + 0.10f * (1f - breathProgress))
            drawCircle(
                color = tertiaryColor.copy(alpha = 0.16f),
                radius = pulseRadius2,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Vòng cung quỹ đạo xoay thuận chiều kim đồng hồ
            rotate(degrees = orbitAngle, pivot = center) {
                val orbitR = maxR * 0.86f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.45f),
                            tertiaryColor.copy(alpha = 0.55f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 15f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(center.x - orbitR, center.y - orbitR),
                    size = Size(orbitR * 2f, orbitR * 2f),
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            secondaryColor.copy(alpha = 0.42f),
                            primaryColor.copy(alpha = 0.48f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 195f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(center.x - orbitR, center.y - orbitR),
                    size = Size(orbitR * 2f, orbitR * 2f),
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )

                // 2 hạt vệ tinh nhỏ trên vành quỹ đạo
                val satAngle1 = Math.toRadians(145.0)
                val sat1 = Offset(
                    x = center.x + (orbitR * cos(satAngle1)).toFloat(),
                    y = center.y + (orbitR * sin(satAngle1)).toFloat()
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.85f),
                    radius = 3.2.dp.toPx(),
                    center = sat1
                )

                val satAngle2 = Math.toRadians(325.0)
                val sat2 = Offset(
                    x = center.x + (orbitR * cos(satAngle2)).toFloat(),
                    y = center.y + (orbitR * sin(satAngle2)).toFloat()
                )
                drawCircle(
                    color = tertiaryColor.copy(alpha = 0.85f),
                    radius = 3.0.dp.toPx(),
                    center = sat2
                )
            }

            // Vòng rãnh đĩa than (Vinyl Micro-grooves) xoay ngược chiều nhẹ nhàng
            rotate(degrees = -orbitAngle * 0.65f, pivot = center) {
                val innerOrbitR = maxR * 0.62f
                for (arcIndex in 0..3) {
                    drawArc(
                        color = primaryColor.copy(alpha = 0.22f),
                        startAngle = arcIndex * 90f + 12f,
                        sweepAngle = 54f,
                        useCenter = false,
                        topLeft = Offset(center.x - innerOrbitR, center.y - innerOrbitR),
                        size = Size(innerOrbitR * 2f, innerOrbitR * 2f),
                        style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Đĩa nghệ thuật trung tâm nổi khối với hiệu ứng lơ lửng
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 6.dp,
            border = BorderStroke(
                width = 1.2.dp,
                color = primaryColor.copy(alpha = 0.25f)
            ),
            modifier = Modifier
                .size(92.dp)
                .offset(y = floatOffsetDp.dp)
                .graphicsLayer {
                    val scale = 0.98f + 0.04f * breathProgress
                    scaleX = scale
                    scaleY = scale
                }
                .radiantShadow(
                    color = primaryColor.copy(alpha = 0.28f),
                    borderRadius = 46.dp,
                    blurRadius = 20.dp,
                    offsetY = 5.dp
                )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.16f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(38.dp)
                            .graphicsLayer {
                                rotationZ = sin(breathProgress * PI).toFloat() * 6f - 3f
                            }
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    AudioWaveformEqualizer(
                        isPlaying = true,
                        tint = primaryColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(width = 20.dp, height = 11.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeMinimalFeatureChip(
    icon: ImageVector,
    label: String
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 0.8.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            )
        }
    }
}

