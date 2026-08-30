package app.olwle.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The golden Cupertino palette. Compose draws every pixel itself,
 * so this theme renders identically on every target we add later.
 */
object OlwleColors {
    val gold = Color(0xFFEFAF1C)
    val goldDeep = Color(0xFFB07C08)
    val goldWash = Color(0xFFFBF3DC)
    val paper = Color(0xFFFCFAF4)
    val sidebar = Color(0xFFF4F0E4)
    val ink = Color(0xFF272217)
    val inkMuted = Color(0xFF6E6656)
    val hairline = Color(0xFFE5DEC9)
    val unread = Color(0xFFEFAF1C)
    val selection = Color(0xFFF6E7BC)
    val danger = Color(0xFFA3492E)
}

private val colorScheme = lightColorScheme(
    primary = OlwleColors.goldDeep,
    onPrimary = Color.White,
    primaryContainer = OlwleColors.goldWash,
    onPrimaryContainer = OlwleColors.ink,
    secondary = OlwleColors.gold,
    background = OlwleColors.paper,
    onBackground = OlwleColors.ink,
    surface = OlwleColors.paper,
    onSurface = OlwleColors.ink,
    surfaceVariant = OlwleColors.sidebar,
    onSurfaceVariant = OlwleColors.inkMuted,
    outline = OlwleColors.hairline,
    outlineVariant = OlwleColors.hairline,
    error = OlwleColors.danger,
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
)

@Composable
fun OlwleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        content = content,
    )
}
