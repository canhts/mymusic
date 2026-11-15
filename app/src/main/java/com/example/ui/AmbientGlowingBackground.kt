package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.theme.AppThemeColor

/**
 * Nền phẳng MD3 1 màu đơn cho toàn ứng dụng, loại bỏ hoàn toàn các vòng tròn màu sắc
 * để tối ưu hóa tối đa hiệu suất rendering và thời lượng pin.
 */
@Composable
fun AmbientGlowingBackground(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    themeColor: AppThemeColor = AppThemeColor.DEFAULT
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )
}
