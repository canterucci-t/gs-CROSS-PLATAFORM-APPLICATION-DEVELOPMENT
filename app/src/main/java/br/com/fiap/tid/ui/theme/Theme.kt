package br.com.fiap.tid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp

/**
 * IDENTIDADE VISUAL - fonte unica de verdade.
 *
 * Conceito: "territorio vivo". Verde-petroleo transmite ambiente e saneamento;
 * o ambar e o vermelho-telha marcam os niveis de risco. Nenhum valor de cor,
 * espacamento ou raio aparece solto nas telas: tudo vem deste arquivo.
 *
 * Acessibilidade: as cores semanticas foram escolhidas com contraste >= 4.5:1
 * sobre o fundo correspondente, e o nivel do alerta NUNCA e comunicado so pela
 * cor - sempre acompanha icone e rotulo em texto.
 */

// ------------------------------------------------------------------- paleta
private val Petroleo = Color(0xFF0E5C56)
private val PetroleoClaro = Color(0xFF7BD3C7)
private val PetroleoEscuro = Color(0xFF00312D)
private val Terra = Color(0xFF5A4634)
private val TerraClara = Color(0xFFE2C9A8)

/** Cores semanticas por nivel de alerta (claro / escuro). */
data class StatusColors(
    val normal: Color,
    val onNormal: Color,
    val info: Color,
    val onInfo: Color,
    val attention: Color,
    val onAttention: Color,
    val critical: Color,
    val onCritical: Color,
    val forecast: Color,
    val threshold: Color
)

private val StatusLight = StatusColors(
    normal = Color(0xFFD7F0E4), onNormal = Color(0xFF0B4A32),
    info = Color(0xFFD8E6F7), onInfo = Color(0xFF0B3C6B),
    attention = Color(0xFFFCE8C2), onAttention = Color(0xFF6B4300),
    critical = Color(0xFFF9D9D4), onCritical = Color(0xFF8A1B0C),
    forecast = Color(0xFF6D5BA8), threshold = Color(0xFFB3261E)
)

private val StatusDark = StatusColors(
    normal = Color(0xFF14362A), onNormal = Color(0xFF9CE8C4),
    info = Color(0xFF142E47), onInfo = Color(0xFFAFD2F7),
    attention = Color(0xFF3E2E0A), onAttention = Color(0xFFF7CE85),
    critical = Color(0xFF47130C), onCritical = Color(0xFFFFB4A6),
    forecast = Color(0xFFC3B4F5), threshold = Color(0xFFFF8A80)
)

private val LightScheme = lightColorScheme(
    primary = Petroleo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCEDE6),
    onPrimaryContainer = PetroleoEscuro,
    secondary = Terra,
    onSecondary = Color.White,
    secondaryContainer = TerraClara,
    onSecondaryContainer = Color(0xFF2B1F10),
    background = Color(0xFFF7FAF8),
    onBackground = Color(0xFF17201D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17201D),
    surfaceVariant = Color(0xFFE2EAE6),
    onSurfaceVariant = Color(0xFF3F4945),
    outline = Color(0xFF6F7975),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = PetroleoClaro,
    onPrimary = PetroleoEscuro,
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFCCEDE6),
    secondary = TerraClara,
    onSecondary = Color(0xFF2B1F10),
    secondaryContainer = Color(0xFF42301C),
    onSecondaryContainer = TerraClara,
    background = Color(0xFF0E1513),
    onBackground = Color(0xFFDDE5E1),
    surface = Color(0xFF141C19),
    onSurface = Color(0xFFDDE5E1),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBEC9C4),
    outline = Color(0xFF899390),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

// -------------------------------------------------------------- tipografia
// Escala enxuta: 6 niveis, todos em sp para crescer com a fonte do sistema.
private val TidTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp
    )
)

// ------------------------------------------------------- formas e espacamento
private val TidShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Grade de espacamento (multiplos de 4dp) e alvo minimo de toque. */
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    /** Alvo minimo de toque exigido pela acessibilidade. */
    val touchTarget: Dp = 48.dp
)

private val LocalStatusColors = staticCompositionLocalOf { StatusLight }
private val LocalSpacing = staticCompositionLocalOf { Spacing() }

/** Acesso aos tokens proprios do app a partir de qualquer Composable. */
object TidTheme {
    val status: StatusColors
        @Composable @ReadOnlyComposable get() = LocalStatusColors.current
    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
}

@Composable
fun TidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) StatusDark else StatusLight,
        LocalSpacing provides Spacing()
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = TidTypography,
            shapes = TidShapes,
            content = content
        )
    }
}
