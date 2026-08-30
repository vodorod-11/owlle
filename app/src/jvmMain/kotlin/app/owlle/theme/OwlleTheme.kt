package app.owlle.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The golden Cupertino palettes. Compose draws every pixel itself,
 * so these render identically on every target we add later.
 */
data class OwllePalette(
    val gold: Color,
    val goldDeep: Color,
    val goldWash: Color,
    val paper: Color,
    val sidebar: Color,
    val ink: Color,
    val inkMuted: Color,
    val hairline: Color,
    val unread: Color,
    val selection: Color,
    val danger: Color,
    val isDark: Boolean,
)

val LightPalette = OwllePalette(
    gold = Color(0xFFEFAF1C),
    goldDeep = Color(0xFFB07C08),
    goldWash = Color(0xFFFBF3DC),
    paper = Color(0xFFFCFAF4),
    sidebar = Color(0xFFF4F0E4),
    ink = Color(0xFF272217),
    inkMuted = Color(0xFF6E6656),
    hairline = Color(0xFFE5DEC9),
    unread = Color(0xFFEFAF1C),
    selection = Color(0xFFF6E7BC),
    danger = Color(0xFFA3492E),
    isDark = false,
)

val DarkPalette = OwllePalette(
    gold = Color(0xFFEFAF1C),
    goldDeep = Color(0xFFE9B23A),
    goldWash = Color(0xFF2A2413),
    paper = Color(0xFF1A1712),
    sidebar = Color(0xFF221E16),
    ink = Color(0xFFEDE6D4),
    inkMuted = Color(0xFFA79C82),
    hairline = Color(0xFF37311F),
    unread = Color(0xFFE9B23A),
    selection = Color(0xFF3A3115),
    danger = Color(0xFFD97B5D),
    isDark = true,
)

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
    val ink: Color @Composable get() = LocalOwllePalette.current.ink
    val inkMuted: Color @Composable get() = LocalOwllePalette.current.inkMuted
    val hairline: Color @Composable get() = LocalOwllePalette.current.hairline
    val unread: Color @Composable get() = LocalOwllePalette.current.unread
    val selection: Color @Composable get() = LocalOwllePalette.current.selection
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
    onPrimary = Color(0xFF1A1712),
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
fun OwlleTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalOwllePalette provides palette) {
        MaterialTheme(
            colorScheme = if (dark) darkScheme(palette) else lightScheme(palette),
            shapes = shapes,
            content = content,
        )
    }
}
