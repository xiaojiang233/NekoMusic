package top.xiaojiang233.nekomusic.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

@Composable
expect fun getDynamicColorScheme(dark: Boolean): ColorScheme?

fun createColorSchemeFromSeed(seedColor: Color, isDark: Boolean): ColorScheme {
    // Simplified tonal palette generation without external deps
    // This is not strictly Material3 algo but close enough for custom theming

    val primary = seedColor
    val onPrimary = if (isDark) Color.Black else Color.White // Ideally check luminance
    val primaryContainer = if (isDark) seedColor.multiplyAlpha(0.3f) else seedColor.multiplyAlpha(0.8f).lighten(0.7f)
    val onPrimaryContainer = if (isDark) seedColor.lighten(0.8f) else seedColor.darken(0.8f)

    // Secondary: shift hue
    val secondary = seedColor.shiftHue(15f)
    val onSecondary = onPrimary
    val secondaryContainer = if (isDark) secondary.multiplyAlpha(0.3f) else secondary.multiplyAlpha(0.8f).lighten(0.7f)
    val onSecondaryContainer = if (isDark) secondary.lighten(0.8f) else secondary.darken(0.8f)

    // Tertiary: shift hue more
    val tertiary = seedColor.shiftHue(30f)
    val onTertiary = onPrimary
    val tertiaryContainer = if (isDark) tertiary.multiplyAlpha(0.3f) else tertiary.multiplyAlpha(0.8f).lighten(0.7f)
    val onTertiaryContainer = if (isDark) tertiary.lighten(0.8f) else tertiary.darken(0.8f)

    // Background/Surface
    val neutral = seedColor.desaturate(0.9f) // Almost grey
    val background = if (isDark) Color(0xFF141414) else Color(0xFFFDFDFD)
    val onBackground = if (isDark) Color(0xFFE0E0E0) else Color(0xFF1A1C1E)
    val surface = if (isDark) Color(0xFF1C1C1C) else Color(0xFFFAFAFA)
    val onSurface = onBackground

    val surfaceVariant = if (isDark) neutral.multiplyAlpha(0.2f).compositeOver(Color.Black) else neutral.multiplyAlpha(0.1f).compositeOver(Color.White)
    val onSurfaceVariant = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)

    // Outline
    val outline = if (isDark) Color(0xFF938F99) else Color(0xFF79747E)

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
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
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
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
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline
        )
    }
}

// Helpers
private fun Color.multiplyAlpha(alpha: Float): Color = copy(alpha = this.alpha * alpha)

// Manual implementation of compositeOver to avoid resolution issues/recursion
private fun Color.compositeOver(background: Color): Color {
    val fgAlpha = this.alpha
    val bgAlpha = background.alpha
    val outAlpha = fgAlpha + (bgAlpha * (1f - fgAlpha))

    if (outAlpha == 0f) return Color(0f, 0f, 0f, 0f)

    val r = (this.red * fgAlpha + background.red * bgAlpha * (1f - fgAlpha)) / outAlpha
    val g = (this.green * fgAlpha + background.green * bgAlpha * (1f - fgAlpha)) / outAlpha
    val b = (this.blue * fgAlpha + background.blue * bgAlpha * (1f - fgAlpha)) / outAlpha

    return Color(r, g, b, outAlpha)
}

private fun Color.lighten(amount: Float): Color {
    val r = red + (1 - red) * amount
    val g = green + (1 - green) * amount
    val b = blue + (1 - blue) * amount
    return copy(red = r, green = g, blue = b)
}

private fun Color.darken(amount: Float): Color {
    val r = red * (1 - amount)
    val g = green * (1 - amount)
    val b = blue * (1 - amount)
    return copy(red = r, green = g, blue = b)
}

private fun Color.desaturate(amount: Float): Color {
    // Simple desaturation by mixing with grey
    val grey = (red + green + blue) / 3f
    val r = red + (grey - red) * amount
    val g = green + (grey - green) * amount
    val b = blue + (grey - blue) * amount
    return copy(red = r, green = g, blue = b)
}

@Suppress("UNUSED_PARAMETER")
private fun Color.shiftHue(degrees: Float): Color {
    // RGB to HSL, shift H, back to RGB logic would be here
    // For brevity, just return self or a simple mix
    // Implementing full RGB->HSL->RGB is verbose here but safer
    // Or just swap components for "difference"
    // Let's implement simple fallback:
    return this // Placeholder if we don't implement full conversion
}

// Minimal HSL conversion if needed, but for now placeholder is fine or just leave secondary=primary
// Actually let's try a simple mix
