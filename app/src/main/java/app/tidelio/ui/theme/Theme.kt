package app.tidelio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Calm coastal palette. */
object TideColors {
    val Background = Color(0xFFFAF6EF) // warm off-white
    val Surface = Color(0xFFFFFDF9)
    val SurfaceVariant = Color(0xFFF1ECE3)
    val Teal = Color(0xFF3F7F7A) // muted teal active controls (4.6:1 on white)
    val TealDark = Color(0xFF2F6460)
    val TealContainer = Color(0xFFD3EBE8)
    val WaveFill = Color(0xFFBFE6E2) // pale aqua
    val WaveEdge = Color(0xFF4E9A93)
    val Morning = Color(0xFFDCE8F5) // soft blue
    val Day = Color(0xFFD3F1EC) // light turquoise
    val Evening = Color(0xFFE6E0F2) // muted lavender
    val Slate = Color(0xFF2E3A44) // dark slate text
    val SlateMuted = Color(0xFF55626C)
    val Outline = Color(0xFFB9C2C6)
    val Error = Color(0xFFB3261E)
}

private val Scheme = lightColorScheme(
    primary = TideColors.Teal,
    onPrimary = Color.White,
    primaryContainer = TideColors.TealContainer,
    onPrimaryContainer = TideColors.TealDark,
    secondary = TideColors.WaveEdge,
    onSecondary = Color.White,
    secondaryContainer = TideColors.TealContainer,
    onSecondaryContainer = TideColors.Slate,
    background = TideColors.Background,
    onBackground = TideColors.Slate,
    surface = TideColors.Surface,
    onSurface = TideColors.Slate,
    surfaceVariant = TideColors.SurfaceVariant,
    onSurfaceVariant = TideColors.SlateMuted,
    surfaceContainer = TideColors.Surface,
    surfaceContainerLow = TideColors.Surface,
    surfaceContainerHigh = TideColors.SurfaceVariant,
    surfaceContainerHighest = TideColors.SurfaceVariant,
    outline = TideColors.Outline,
    outlineVariant = TideColors.SurfaceVariant,
    error = TideColors.Error,
)

private val base = Typography()
private val TideTypography = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

val NumberStyle = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.SemiBold)

@Composable
fun TidelioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = TideTypography, content = content)
}
