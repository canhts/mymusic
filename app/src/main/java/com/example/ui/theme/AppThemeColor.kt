package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.R

/**
 * 5 màu sắc chủ đạo chuẩn Material Design 3 (MD3):
 * - Không trùng lặp, độ tương phản và hài hòa cao trên cả giao diện sáng & tối.
 * - OCEAN_BLUE: Xanh Đại Dương (mặc định hiện tại của app).
 * - EMERALD_GREEN: Ngọc Lục Bảo tươi mát, tràn đầy năng lượng tự nhiên.
 * - ROYAL_VIOLET: Tím Hoàng Gia quý phái, sáng tạo và cá tính.
 * - SUNSET_AMBER: Cam Hổ Phách ấm áp, rực rỡ và cuốn hút.
 * - RUBY_ROSE: Hồng Ruby ngọt ngào, hiện đại và trẻ trung.
 */
enum class AppThemeColor(
    val id: String,
    val nameResId: Int,
    val seedColor: Color,
    val previewColor: Color
) {
    OCEAN_BLUE(
        id = "ocean_blue",
        nameResId = R.string.theme_color_ocean_blue,
        seedColor = Color(0xFF2196F3),
        previewColor = Color(0xFF0077C2)
    ),
    EMERALD_GREEN(
        id = "emerald_green",
        nameResId = R.string.theme_color_emerald_green,
        seedColor = Color(0xFF00897B),
        previewColor = Color(0xFF00897B)
    ),
    ROYAL_VIOLET(
        id = "royal_violet",
        nameResId = R.string.theme_color_royal_violet,
        seedColor = Color(0xFF7C4DFF),
        previewColor = Color(0xFF7C4DFF)
    ),
    SUNSET_AMBER(
        id = "sunset_amber",
        nameResId = R.string.theme_color_sunset_amber,
        seedColor = Color(0xFFFF6F00),
        previewColor = Color(0xFFFF6F00)
    ),
    RUBY_ROSE(
        id = "ruby_rose",
        nameResId = R.string.theme_color_ruby_rose,
        seedColor = Color(0xFFE91E63),
        previewColor = Color(0xFFE91E63)
    );

    companion object {
        val DEFAULT = OCEAN_BLUE

        fun fromId(id: String?): AppThemeColor {
            return entries.firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}
