package app.owlle.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * The golden Cupertino palettes. Light is warm paper + gold; dark is a
 * strictly NEUTRAL gray ground — warm-tinted text on dark reads as gold
 * sludge, so in dark mode gold appears only as the accent (selection,
 * unread, links) and never in body text or grounds.
 */
data class OwllePalette(
    val gold: Color,
    val goldDeep: Color,
    val goldWash: Color,
    val paper: Color,
    val sidebar: Color,
    val sidebarIcon: Color,
    val ink: Color,
    val inkMuted: Color,
    val hairline: Color,
    val unread: Color,
    val selection: Color,
    val onSelection: Color,
    val danger: Color,
    val isDark: Boolean,
)

/** The brand default. Any accent from the profile picker reshapes both palettes. */
val GoldAccent = Color(0xFFEFAF1C)

fun lightPalette(accent: Color): OwllePalette = OwllePalette(
    gold = accent,
    goldDeep = lerp(accent, Color.Black, 0.30f),
    goldWash = lerp(accent, Color.White, 0.86f),
    paper = Color(0xFFFCFAF4),
    sidebar = Color(0xFFF4F0E4),
    sidebarIcon = lerp(accent, Color.Black, 0.30f),
    ink = Color(0xFF272217),
    inkMuted = Color(0xFF6E6656),
    hairline = Color(0xFFE5DEC9),
    unread = accent,
    selection = lerp(accent, Color.White, 0.70f),
    onSelection = Color(0xFF272217),
    danger = Color(0xFFA3492E),
    isDark = false,
)

fun darkPalette(accent: Color): OwllePalette = OwllePalette(
    gold = lerp(accent, Color.White, 0.06f),
    goldDeep = lerp(accent, Color.White, 0.10f),
    goldWash = lerp(accent, Color.Black, 0.72f),
    paper = Color(0xFF191919),
    sidebar = Color(0xFF212121),
    sidebarIcon = Color(0xFF908D87),
    ink = Color(0xFFE9E8E6),
    inkMuted = Color(0xFF9A9791),
    hairline = Color(0xFF333230),
    unread = lerp(accent, Color.White, 0.10f),
    selection = lerp(accent, Color.White, 0.10f),
    onSelection = Color(0xFF191919),
    danger = Color(0xFFE08A6D),
    isDark = true,
)

val LightPalette = lightPalette(GoldAccent)
val DarkPalette = darkPalette(GoldAccent)

private val LocalOwllePalette = staticCompositionLocalOf { LightPalette }

/**
 * Call-site facade: `OwlleColors.gold` etc. resolve against whichever
 * palette the active theme provides.
 */
object OwlleColors {
    val gold: Color @Composable get() = LocalOwllePalette.current.gold
    val goldDeep: Color @Composable get() = LocalOwllePalette.current.goldDeep
    val goldWash: Color @Composable get() = LocalOwllePalette.current.goldWash
    val paper: Color @Composable get() = LocalOwllePalette.current.paper
    val sidebar: Color @Composable get() = LocalOwllePalette.current.sidebar
    val sidebarIcon: Color @Composable get() = LocalOwllePalette.current.sidebarIcon
    val ink: Color @Composable get() = LocalOwllePalette.current.ink
    val inkMuted: Color @Composable get() = LocalOwllePalette.current.inkMuted
    val hairline: Color @Composable get() = LocalOwllePalette.current.hairline
    val unread: Color @Composable get() = LocalOwllePalette.current.unread
    val selection: Color @Composable get() = LocalOwllePalette.current.selection
    val onSelection: Color @Composable get() = LocalOwllePalette.current.onSelection
    val danger: Color @Composable get() = LocalOwllePalette.current.danger
    val isDark: Boolean @Composable get() = LocalOwllePalette.current.isDark
}

private fun lightScheme(p: OwllePalette) = lightColorScheme(
    primary = p.goldDeep,
    onPrimary = Color.White,
    primaryContainer = p.goldWash,
    onPrimaryContainer = p.ink,
    secondary = p.gold,
    background = p.paper,
    onBackground = p.ink,
    surface = p.paper,
    onSurface = p.ink,
    surfaceVariant = p.sidebar,
    onSurfaceVariant = p.inkMuted,
    outline = p.hairline,
    outlineVariant = p.hairline,
    error = p.danger,
)

private fun darkScheme(p: OwllePalette) = darkColorScheme(
    primary = p.goldDeep,
    onPrimary = p.onSelection,
    primaryContainer = p.goldWash,
    onPrimaryContainer = p.ink,
    secondary = p.gold,
    background = p.paper,
    onBackground = p.ink,
    surface = p.sidebar,
    onSurface = p.ink,
    surfaceVariant = p.sidebar,
    onSurfaceVariant = p.inkMuted,
    outline = p.hairline,
    outlineVariant = p.hairline,
    error = p.danger,
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
)

@Composable
fun OwlleTheme(
    dark: Boolean = false,
    accent: Color = GoldAccent,
    content: @Composable () -> Unit,
) {
    val palette = if (dark) darkPalette(accent) else lightPalette(accent)
    CompositionLocalProvider(LocalOwllePalette provides palette) {
        MaterialTheme(
            colorScheme = if (dark) darkScheme(palette) else lightScheme(palette),
            shapes = shapes,
        ) {
            // Default content color for every Text/Icon that doesn't set one —
            // without this, unset text falls back to black and vanishes in dark mode.
            CompositionLocalProvider(LocalContentColor provides palette.ink, content = content)
        }
    }
}
