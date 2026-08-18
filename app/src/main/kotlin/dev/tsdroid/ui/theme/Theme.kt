package dev.tsdroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Normal, lineHeight = 64.sp),
    displayMedium = TextStyle(fontSize = 45.sp, fontWeight = FontWeight.Normal, lineHeight = 52.sp),
    displaySmall = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Normal, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    // CJK text reads better without the extra letter spacing that Latin
    // Material typography applies.
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
)

private fun Color.hsl(): Triple<Float, Float, Float> {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val h: Float
    val s: Float
    if (max == min) {
        h = 0f; s = 0f
    } else {
        val d = max - min
        s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        h = when (max) {
            r -> (g - b) / d + (if (g < b) 6f else 0f)
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } / 6f
    }
    return Triple(h * 360f, s, l)
}

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(((r + m) * 255).roundToInt(), ((g + m) * 255).roundToInt(), ((b + m) * 255).roundToInt())
}

fun generateColorScheme(seed: Color, isDark: Boolean): androidx.compose.material3.ColorScheme {
    val (h, s, l) = seed.hsl()
    val primary = seed
    val onPrimary = if (l > 0.5f) Color.Black else Color.White
    val primaryContainer = hslToColor(h, (s * 0.7f).coerceAtMost(1f), if (isDark) 0.3f else 0.85f)
    val onPrimaryContainer = if (isDark) hslToColor(h, 0.6f, 0.9f) else hslToColor(h, 0.8f, 0.15f)

    val secH = (h + 30f) % 360f
    val secondary = hslToColor(secH, (s * 0.6f).coerceAtMost(1f), if (isDark) 0.7f else 0.45f)
    val onSecondary = if (isDark) Color.Black else Color.White
    val secondaryContainer = hslToColor(secH, (s * 0.5f).coerceAtMost(1f), if (isDark) 0.25f else 0.9f)
    val onSecondaryContainer = if (isDark) hslToColor(secH, 0.5f, 0.85f) else hslToColor(secH, 0.7f, 0.15f)

    val terH = (h + 60f) % 360f
    val tertiary = hslToColor(terH, (s * 0.5f).coerceAtMost(1f), if (isDark) 0.7f else 0.5f)
    val onTertiary = if (isDark) Color.Black else Color.White
    val tertiaryContainer = hslToColor(terH, (s * 0.4f).coerceAtMost(1f), if (isDark) 0.25f else 0.9f)
    val onTertiaryContainer = if (isDark) hslToColor(terH, 0.4f, 0.85f) else hslToColor(terH, 0.6f, 0.15f)

    val surface = if (isDark) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)
    val onSurface = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
    val surfaceVariant = if (isDark) Color(0xFF49454F) else Color(0xFFE7E0EC)
    val onSurfaceVariant = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)

    return if (isDark) {
        darkColorScheme(
            primary = hslToColor(h, (s * 0.8f).coerceAtMost(1f), 0.8f),
            onPrimary = hslToColor(h, 0.6f, 0.2f),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC),
            background = surface,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = Color(0xFF938F99),
            outlineVariant = Color(0xFF49454F),
            inverseSurface = Color(0xFFE6E1E5),
            inverseOnSurface = Color(0xFF313033),
            inversePrimary = hslToColor(h, (s * 0.8f).coerceAtMost(1f), 0.8f),
            surfaceDim = Color(0xFF141218),
            surfaceBright = Color(0xFF3B383E),
            surfaceContainerLowest = Color(0xFF0F0D13),
            surfaceContainerLow = Color(0xFF1D1B20),
            surfaceContainer = Color(0xFF211F26),
            surfaceContainerHigh = Color(0xFF2B2930),
            surfaceContainerHighest = Color(0xFF36343B),
        )
    } else {
        lightColorScheme(
            primary = hslToColor(h, (s * 0.8f).coerceAtMost(1f), 0.4f),
            onPrimary = Color.White,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            error = Color(0xFFB3261E),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            background = surface,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = Color(0xFF79747E),
            outlineVariant = Color(0xFFCAC4D0),
            inverseSurface = Color(0xFF313033),
            inverseOnSurface = Color(0xFFF4EFF4),
            inversePrimary = hslToColor(h, (s * 0.8f).coerceAtMost(1f), 0.8f),
            surfaceDim = Color(0xFFDED8E1),
            surfaceBright = Color(0xFFFFFBFE),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF7F2FA),
            surfaceContainer = Color(0xFFF3EDF7),
            surfaceContainerHigh = Color(0xFFECE6F0),
            surfaceContainerHighest = Color(0xFFE6E0E9),
        )
    }
}

enum class AppThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    AMOLED("amoled");

    companion object {
        fun fromStorage(value: String?): AppThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

@Composable
fun TsDroidTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    seedColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
        AppThemeMode.SYSTEM -> systemDark
    }

    val baseSeed = seedColor ?: Color(0xFF6750A4)
    val colorScheme = if (themeMode == AppThemeMode.AMOLED) {
        pureBlackColorScheme(baseSeed)
    } else {
        generateColorScheme(baseSeed, darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

/**
 * AMOLED-friendly scheme: true black surfaces so OLED pixels turn off,
 * with only the hierarchy tones slightly lifted to keep cards readable.
 */
private fun pureBlackColorScheme(seed: Color): androidx.compose.material3.ColorScheme {
    val (h, s, _) = seed.hsl()
    fun tone(lightness: Float) = hslToColor(h, (s * 0.8f).coerceAtMost(1f), lightness)
    fun secTone(lightness: Float) = hslToColor((h + 30f) % 360f, (s * 0.6f).coerceAtMost(1f), lightness)
    fun terTone(lightness: Float) = hslToColor((h + 60f) % 360f, (s * 0.5f).coerceAtMost(1f), lightness)

    return darkColorScheme(
        primary = tone(0.82f),
        onPrimary = hslToColor(h, 0.6f, 0.12f),
        primaryContainer = tone(0.22f),
        onPrimaryContainer = hslToColor(h, 0.6f, 0.9f),
        secondary = secTone(0.75f),
        onSecondary = Color.Black,
        secondaryContainer = secTone(0.2f),
        onSecondaryContainer = hslToColor((h + 30f) % 360f, 0.5f, 0.85f),
        tertiary = terTone(0.75f),
        onTertiary = Color.Black,
        tertiaryContainer = terTone(0.2f),
        onTertiaryContainer = hslToColor((h + 60f) % 360f, 0.4f, 0.85f),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color.Black,
        onBackground = Color(0xFFE6E1E5),
        surface = Color.Black,
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = Color(0xFF111111),
        onSurfaceVariant = Color(0xFFCAC4D0),
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF252525),
        inverseSurface = Color(0xFFE6E1E5),
        inverseOnSurface = Color(0xFF1C1B1F),
        inversePrimary = tone(0.4f),
        surfaceDim = Color.Black,
        surfaceBright = Color(0xFF2A2A2A),
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0A0A0A),
        surfaceContainer = Color(0xFF111111),
        surfaceContainerHigh = Color(0xFF1A1A1A),
        surfaceContainerHighest = Color(0xFF232323),
    )
}
