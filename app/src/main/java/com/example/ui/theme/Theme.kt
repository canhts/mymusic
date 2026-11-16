package com.example.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

// ==========================================
// 1. OCEAN BLUE (Mặc định - Google Material Blue)
// ==========================================
private val OceanBlueDarkColorScheme = darkColorScheme(
    primary = OceanBluePrimaryDark,
    onPrimary = OceanBlueOnPrimaryDark,
    primaryContainer = OceanBluePrimaryContainerDark,
    onPrimaryContainer = OceanBlueOnPrimaryContainerDark,
    secondary = OceanBlueSecondaryDark,
    onSecondary = OceanBlueOnSecondaryDark,
    secondaryContainer = OceanBlueSecondaryContainerDark,
    onSecondaryContainer = OceanBlueOnSecondaryContainerDark,
    tertiary = OceanBlueTertiaryDark,
    onTertiary = OceanBlueOnTertiaryDark,
    tertiaryContainer = OceanBlueTertiaryContainerDark,
    onTertiaryContainer = OceanBlueOnTertiaryContainerDark,
    background = AppBackgroundDark,
    onBackground = AppOnBackgroundDark,
    surface = AppSurfaceDark,
    onSurface = AppOnSurfaceDark,
    surfaceVariant = AppSurfaceVariantDark,
    onSurfaceVariant = AppOnSurfaceVariantDark,
    surfaceContainerLowest = AppSurfaceContainerLowestDark,
    surfaceContainerLow = AppSurfaceContainerLowDark,
    surfaceContainer = AppSurfaceContainerDark,
    surfaceContainerHigh = OceanBlueSurfaceContainerHighDark,
    surfaceContainerHighest = OceanBlueSurfaceContainerHighestDark
)

private val OceanBlueLightColorScheme = lightColorScheme(
    primary = OceanBluePrimaryLight,
    onPrimary = OceanBlueOnPrimaryLight,
    primaryContainer = OceanBluePrimaryContainerLight,
    onPrimaryContainer = OceanBlueOnPrimaryContainerLight,
    secondary = OceanBlueSecondaryLight,
    onSecondary = OceanBlueOnSecondaryLight,
    secondaryContainer = OceanBlueSecondaryContainerLight,
    onSecondaryContainer = OceanBlueOnSecondaryContainerLight,
    tertiary = OceanBlueTertiaryLight,
    onTertiary = OceanBlueOnTertiaryLight,
    tertiaryContainer = OceanBlueTertiaryContainerLight,
    onTertiaryContainer = OceanBlueOnTertiaryContainerLight,
    background = AppBackgroundLight,
    onBackground = AppOnBackgroundLight,
    surface = AppSurfaceLight,
    onSurface = AppOnSurfaceLight,
    surfaceVariant = AppSurfaceVariantLight,
    onSurfaceVariant = AppOnSurfaceVariantLight,
    surfaceContainerLowest = AppSurfaceContainerLowestLight,
    surfaceContainerLow = AppSurfaceContainerLowLight,
    surfaceContainer = AppSurfaceContainerLight,
    surfaceContainerHigh = OceanBlueSurfaceContainerHighLight,
    surfaceContainerHighest = OceanBlueSurfaceContainerHighestLight
)

// ==========================================
// 2. EMERALD GREEN (Ngọc Lục Bảo)
// ==========================================
private val EmeraldGreenLightColorScheme = lightColorScheme(
    primary = EmeraldGreenPrimaryLight,
    onPrimary = EmeraldGreenOnPrimaryLight,
    primaryContainer = EmeraldGreenPrimaryContainerLight,
    onPrimaryContainer = EmeraldGreenOnPrimaryContainerLight,
    secondary = EmeraldGreenSecondaryLight,
    onSecondary = EmeraldGreenOnSecondaryLight,
    secondaryContainer = EmeraldGreenSecondaryContainerLight,
    onSecondaryContainer = EmeraldGreenOnSecondaryContainerLight,
    tertiary = EmeraldGreenTertiaryLight,
    onTertiary = EmeraldGreenOnTertiaryLight,
    tertiaryContainer = EmeraldGreenTertiaryContainerLight,
    onTertiaryContainer = EmeraldGreenOnTertiaryContainerLight,
    background = AppBackgroundLight,
    onBackground = AppOnBackgroundLight,
    surface = AppSurfaceLight,
    onSurface = AppOnSurfaceLight,
    surfaceVariant = AppSurfaceVariantLight,
    onSurfaceVariant = AppOnSurfaceVariantLight,
    surfaceContainerLowest = AppSurfaceContainerLowestLight,
    surfaceContainerLow = AppSurfaceContainerLowLight,
    surfaceContainer = AppSurfaceContainerLight,
    surfaceContainerHigh = EmeraldGreenSurfaceContainerHighLight,
    surfaceContainerHighest = EmeraldGreenSurfaceContainerHighestLight
)

private val EmeraldGreenDarkColorScheme = darkColorScheme(
    primary = EmeraldGreenPrimaryDark,
    onPrimary = EmeraldGreenOnPrimaryDark,
    primaryContainer = EmeraldGreenPrimaryContainerDark,
    onPrimaryContainer = EmeraldGreenOnPrimaryContainerDark,
    secondary = EmeraldGreenSecondaryDark,
    onSecondary = EmeraldGreenOnSecondaryDark,
    secondaryContainer = EmeraldGreenSecondaryContainerDark,
    onSecondaryContainer = EmeraldGreenOnSecondaryContainerDark,
    tertiary = EmeraldGreenTertiaryDark,
    onTertiary = EmeraldGreenOnTertiaryDark,
    tertiaryContainer = EmeraldGreenTertiaryContainerDark,
    onTertiaryContainer = EmeraldGreenOnTertiaryContainerDark,
    background = AppBackgroundDark,
    onBackground = AppOnBackgroundDark,
    surface = AppSurfaceDark,
    onSurface = AppOnSurfaceDark,
    surfaceVariant = AppSurfaceVariantDark,
    onSurfaceVariant = AppOnSurfaceVariantDark,
    surfaceContainerLowest = AppSurfaceContainerLowestDark,
    surfaceContainerLow = AppSurfaceContainerLowDark,
    surfaceContainer = AppSurfaceContainerDark,
    surfaceContainerHigh = EmeraldGreenSurfaceContainerHighDark,
    surfaceContainerHighest = EmeraldGreenSurfaceContainerHighestDark
)

// ==========================================
// 3. ROYAL VIOLET (Tím Hoàng Gia)
// ==========================================
private val RoyalVioletLightColorScheme = lightColorScheme(
    primary = RoyalVioletPrimaryLight,
    onPrimary = RoyalVioletOnPrimaryLight,
    primaryContainer = RoyalVioletPrimaryContainerLight,
    onPrimaryContainer = RoyalVioletOnPrimaryContainerLight,
    secondary = RoyalVioletSecondaryLight,
    onSecondary = RoyalVioletOnSecondaryLight,
    secondaryContainer = RoyalVioletSecondaryContainerLight,
    onSecondaryContainer = RoyalVioletOnSecondaryContainerLight,
    tertiary = RoyalVioletTertiaryLight,
    onTertiary = RoyalVioletOnTertiaryLight,
    tertiaryContainer = RoyalVioletTertiaryContainerLight,
    onTertiaryContainer = RoyalVioletOnTertiaryContainerLight,
    background = AppBackgroundLight,
    onBackground = AppOnBackgroundLight,
    surface = AppSurfaceLight,
    onSurface = AppOnSurfaceLight,
    surfaceVariant = AppSurfaceVariantLight,
    onSurfaceVariant = AppOnSurfaceVariantLight,
    surfaceContainerLowest = AppSurfaceContainerLowestLight,
    surfaceContainerLow = AppSurfaceContainerLowLight,
    surfaceContainer = AppSurfaceContainerLight,
    surfaceContainerHigh = RoyalVioletSurfaceContainerHighLight,
    surfaceContainerHighest = RoyalVioletSurfaceContainerHighestLight
)

private val RoyalVioletDarkColorScheme = darkColorScheme(
    primary = RoyalVioletPrimaryDark,
    onPrimary = RoyalVioletOnPrimaryDark,
    primaryContainer = RoyalVioletPrimaryContainerDark,
    onPrimaryContainer = RoyalVioletOnPrimaryContainerDark,
    secondary = RoyalVioletSecondaryDark,
    onSecondary = RoyalVioletOnSecondaryDark,
    secondaryContainer = RoyalVioletSecondaryContainerDark,
    onSecondaryContainer = RoyalVioletOnSecondaryContainerDark,
    tertiary = RoyalVioletTertiaryDark,
    onTertiary = RoyalVioletOnTertiaryDark,
    tertiaryContainer = RoyalVioletTertiaryContainerDark,
    onTertiaryContainer = RoyalVioletOnTertiaryContainerDark,
    background = AppBackgroundDark,
    onBackground = AppOnBackgroundDark,
    surface = AppSurfaceDark,
    onSurface = AppOnSurfaceDark,
    surfaceVariant = AppSurfaceVariantDark,
    onSurfaceVariant = AppOnSurfaceVariantDark,
    surfaceContainerLowest = AppSurfaceContainerLowestDark,
    surfaceContainerLow = AppSurfaceContainerLowDark,
    surfaceContainer = AppSurfaceContainerDark,
    surfaceContainerHigh = RoyalVioletSurfaceContainerHighDark,
    surfaceContainerHighest = RoyalVioletSurfaceContainerHighestDark
)

// ==========================================
// 4. SUNSET AMBER (Cam Hổ Phách)
// ==========================================
private val SunsetAmberLightColorScheme = lightColorScheme(
    primary = SunsetAmberPrimaryLight,
    onPrimary = SunsetAmberOnPrimaryLight,
    primaryContainer = SunsetAmberPrimaryContainerLight,
    onPrimaryContainer = SunsetAmberOnPrimaryContainerLight,
    secondary = SunsetAmberSecondaryLight,
    onSecondary = SunsetAmberOnSecondaryLight,
    secondaryContainer = SunsetAmberSecondaryContainerLight,
    onSecondaryContainer = SunsetAmberOnSecondaryContainerLight,
    tertiary = SunsetAmberTertiaryLight,
    onTertiary = SunsetAmberOnTertiaryLight,
    tertiaryContainer = SunsetAmberTertiaryContainerLight,
    onTertiaryContainer = SunsetAmberOnTertiaryContainerLight,
    background = AppBackgroundLight,
    onBackground = AppOnBackgroundLight,
    surface = AppSurfaceLight,
    onSurface = AppOnSurfaceLight,
    surfaceVariant = AppSurfaceVariantLight,
    onSurfaceVariant = AppOnSurfaceVariantLight,
    surfaceContainerLowest = AppSurfaceContainerLowestLight,
    surfaceContainerLow = AppSurfaceContainerLowLight,
    surfaceContainer = AppSurfaceContainerLight,
    surfaceContainerHigh = SunsetAmberSurfaceContainerHighLight,
    surfaceContainerHighest = SunsetAmberSurfaceContainerHighestLight
)

private val SunsetAmberDarkColorScheme = darkColorScheme(
    primary = SunsetAmberPrimaryDark,
    onPrimary = SunsetAmberOnPrimaryDark,
    primaryContainer = SunsetAmberPrimaryContainerDark,
    onPrimaryContainer = SunsetAmberOnPrimaryContainerDark,
    secondary = SunsetAmberSecondaryDark,
    onSecondary = SunsetAmberOnSecondaryDark,
    secondaryContainer = SunsetAmberSecondaryContainerDark,
    onSecondaryContainer = SunsetAmberOnSecondaryContainerDark,
    tertiary = SunsetAmberTertiaryDark,
    onTertiary = SunsetAmberOnTertiaryDark,
    tertiaryContainer = SunsetAmberTertiaryContainerDark,
    onTertiaryContainer = SunsetAmberOnTertiaryContainerDark,
    background = AppBackgroundDark,
    onBackground = AppOnBackgroundDark,
    surface = AppSurfaceDark,
    onSurface = AppOnSurfaceDark,
    surfaceVariant = AppSurfaceVariantDark,
    onSurfaceVariant = AppOnSurfaceVariantDark,
    surfaceContainerLowest = AppSurfaceContainerLowestDark,
    surfaceContainerLow = AppSurfaceContainerLowDark,
    surfaceContainer = AppSurfaceContainerDark,
    surfaceContainerHigh = SunsetAmberSurfaceContainerHighDark,
    surfaceContainerHighest = SunsetAmberSurfaceContainerHighestDark
)

// ==========================================
// 5. RUBY ROSE (Hồng Ruby)
// ==========================================
private val RubyRoseLightColorScheme = lightColorScheme(
    primary = RubyRosePrimaryLight,
    onPrimary = RubyRoseOnPrimaryLight,
    primaryContainer = RubyRosePrimaryContainerLight,
    onPrimaryContainer = RubyRoseOnPrimaryContainerLight,
    secondary = RubyRoseSecondaryLight,
    onSecondary = RubyRoseOnSecondaryLight,
    secondaryContainer = RubyRoseSecondaryContainerLight,
    onSecondaryContainer = RubyRoseOnSecondaryContainerLight,
    tertiary = RubyRoseTertiaryLight,
    onTertiary = RubyRoseOnTertiaryLight,
    tertiaryContainer = RubyRoseTertiaryContainerLight,
    onTertiaryContainer = RubyRoseOnTertiaryContainerLight,
    background = AppBackgroundLight,
    onBackground = AppOnBackgroundLight,
    surface = AppSurfaceLight,
    onSurface = AppOnSurfaceLight,
    surfaceVariant = AppSurfaceVariantLight,
    onSurfaceVariant = AppOnSurfaceVariantLight,
    surfaceContainerLowest = AppSurfaceContainerLowestLight,
    surfaceContainerLow = AppSurfaceContainerLowLight,
    surfaceContainer = AppSurfaceContainerLight,
    surfaceContainerHigh = RubyRoseSurfaceContainerHighLight,
    surfaceContainerHighest = RubyRoseSurfaceContainerHighestLight
)

private val RubyRoseDarkColorScheme = darkColorScheme(
    primary = RubyRosePrimaryDark,
    onPrimary = RubyRoseOnPrimaryDark,
    primaryContainer = RubyRosePrimaryContainerDark,
    onPrimaryContainer = RubyRoseOnPrimaryContainerDark,
    secondary = RubyRoseSecondaryDark,
    onSecondary = RubyRoseOnSecondaryDark,
    secondaryContainer = RubyRoseSecondaryContainerDark,
    onSecondaryContainer = RubyRoseOnSecondaryContainerDark,
    tertiary = RubyRoseTertiaryDark,
    onTertiary = RubyRoseOnTertiaryDark,
    tertiaryContainer = RubyRoseTertiaryContainerDark,
    onTertiaryContainer = RubyRoseOnTertiaryContainerDark,
    background = AppBackgroundDark,
    onBackground = AppOnBackgroundDark,
    surface = AppSurfaceDark,
    onSurface = AppOnSurfaceDark,
    surfaceVariant = AppSurfaceVariantDark,
    onSurfaceVariant = AppOnSurfaceVariantDark,
    surfaceContainerLowest = AppSurfaceContainerLowestDark,
    surfaceContainerLow = AppSurfaceContainerLowDark,
    surfaceContainer = AppSurfaceContainerDark,
    surfaceContainerHigh = RubyRoseSurfaceContainerHighDark,
    surfaceContainerHighest = RubyRoseSurfaceContainerHighestDark
)

/**
 * Trả về ColorScheme chuẩn MD3 dựa trên AppThemeColor và Dark/Light mode
 */
fun getAppColorScheme(themeColor: AppThemeColor, darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        when (themeColor) {
            AppThemeColor.OCEAN_BLUE -> OceanBlueDarkColorScheme
            AppThemeColor.EMERALD_GREEN -> EmeraldGreenDarkColorScheme
            AppThemeColor.ROYAL_VIOLET -> RoyalVioletDarkColorScheme
            AppThemeColor.SUNSET_AMBER -> SunsetAmberDarkColorScheme
            AppThemeColor.RUBY_ROSE -> RubyRoseDarkColorScheme
        }
    } else {
        when (themeColor) {
            AppThemeColor.OCEAN_BLUE -> OceanBlueLightColorScheme
            AppThemeColor.EMERALD_GREEN -> EmeraldGreenLightColorScheme
            AppThemeColor.ROYAL_VIOLET -> RoyalVioletLightColorScheme
            AppThemeColor.SUNSET_AMBER -> SunsetAmberLightColorScheme
            AppThemeColor.RUBY_ROSE -> RubyRoseLightColorScheme
        }
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.DEFAULT }

/**
 * Cung cấp [LocalContext] và [LocalConfiguration] đã gắn [appLanguage] cho toàn bộ cây con,
 * kể cả các cửa sổ con như ModalBottomSheet hoặc AlertDialog vốn tự tạo AndroidComposeView riêng.
 */
@Composable
fun LocalizedAppScope(
    appLanguage: AppLanguage = LocalAppLanguage.current,
    content: @Composable () -> Unit
) {
    val currentContext = LocalContext.current
    val unwrappedBase = remember(currentContext) {
        if (currentContext is LocalizedContextWrapper) currentContext.baseContext else currentContext
    }
    val baseConfiguration = LocalConfiguration.current

    val localizedConfig = remember(baseConfiguration, appLanguage) {
        Configuration(baseConfiguration).apply {
            setLocale(appLanguage.toLocale())
        }
    }
    val localizedContext = remember(unwrappedBase, localizedConfig) {
        val configContext = unwrappedBase.createConfigurationContext(localizedConfig)
        LocalizedContextWrapper(unwrappedBase, configContext.resources)
    }

    CompositionLocalProvider(
        LocalAppLanguage provides appLanguage,
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfig,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: AppThemeColor = AppThemeColor.DEFAULT,
    appLanguage: AppLanguage = AppLanguage.DEFAULT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(baseContext) else dynamicLightColorScheme(baseContext)
        }
        else -> getAppColorScheme(themeColor = themeColor, darkTheme = darkTheme)
    }

    LocalizedAppScope(appLanguage = appLanguage) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
