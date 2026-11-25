package com.techterms.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

@Immutable
data class TechTermsExtendedColors(
    val quoteSurface: Color,
    val codeSurface: Color,
    val codeKeyword: Color,
    val codeString: Color,
    val codeComment: Color,
    val codeNumber: Color,
    val difficultyBeginner: Color,
    val difficultyIntermediate: Color,
    val difficultyAdvanced: Color
)

private val LightExtendedColors = TechTermsExtendedColors(
    quoteSurface = QuoteSurfaceLight,
    codeSurface = CodeSurfaceLight,
    codeKeyword = CodeKeywordLight,
    codeString = CodeStringLight,
    codeComment = CodeCommentLight,
    codeNumber = CodeNumberLight,
    difficultyBeginner = DifficultyBeginnerLight,
    difficultyIntermediate = DifficultyIntermediateLight,
    difficultyAdvanced = DifficultyAdvancedLight
)

private val DarkExtendedColors = TechTermsExtendedColors(
    quoteSurface = QuoteSurfaceDark,
    codeSurface = CodeSurfaceDark,
    codeKeyword = CodeKeywordDark,
    codeString = CodeStringDark,
    codeComment = CodeCommentDark,
    codeNumber = CodeNumberDark,
    difficultyBeginner = DifficultyBeginnerDark,
    difficultyIntermediate = DifficultyIntermediateDark,
    difficultyAdvanced = DifficultyAdvancedDark
)

private val PaperExtendedColors = LightExtendedColors.copy(
    quoteSurface = Color(0xFFEFE4CB),
    codeSurface = Color(0xFFE8DCC0),
    codeKeyword = Color(0xFF8B3A2B),
    codeString = Color(0xFF5E6B2E),
    codeComment = Color(0xFF8C7B66),
    codeNumber = Color(0xFF7A5230)
)

val LocalTechTermsColors = staticCompositionLocalOf { LightExtendedColors }
val LocalPaperTheme = staticCompositionLocalOf { false }

object TechTermsThemeTokens {
    val extendedColors: TechTermsExtendedColors
        @Composable
        get() = LocalTechTermsColors.current

    /** Serif for term titles in the Paper theme, sans-serif otherwise. */
    val termFontFamily: FontFamily
        @Composable
        get() = if (LocalPaperTheme.current) FontFamily.Serif else EnglishSansFontFamily
}

private val PaperColorScheme = lightColorScheme(
    primary = Color(0xFF7A5230),
    onPrimary = Color(0xFFFBF5E6),
    primaryContainer = Color(0xFFE6D5B8),
    onPrimaryContainer = Color(0xFF3A2A1A),
    secondary = Color(0xFF8A6F4E),
    onSecondary = Color(0xFFFBF5E6),
    secondaryContainer = Color(0xFFEADFC6),
    onSecondaryContainer = Color(0xFF2B2118),
    background = Color(0xFFF4ECD8),
    onBackground = Color(0xFF2B2118),
    surface = Color(0xFFFAF3E1),
    onSurface = Color(0xFF2B2118),
    surfaceVariant = Color(0xFFEADFC6),
    onSurfaceVariant = Color(0xFF6B5A48),
    outline = Color(0xFF9C8468),
    outlineVariant = Color(0xFFD4C4A4)
)

private fun Typography.toPaper(): Typography {
    fun TextStyle.serif(body: Boolean = false): TextStyle = copy(
        fontFamily = FontFamily.Serif,
        lineHeight = if (body && fontSize.isSp) (fontSize.value * 1.5f).sp else lineHeight
    )
    return copy(
        headlineLarge = headlineLarge.serif(),
        headlineMedium = headlineMedium.serif(),
        headlineSmall = headlineSmall.serif(),
        titleLarge = titleLarge.serif(),
        titleMedium = titleMedium.serif(),
        bodyLarge = bodyLarge.serif(true),
        bodyMedium = bodyMedium.serif(true),
        bodySmall = bodySmall.serif(true)
    )
}

private val PaperTypography = TechTermsTypography.toPaper()

private val LightColorScheme = lightColorScheme(
    primary = IndigoAccent,
    onPrimary = SurfaceLight,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = OnIndigoContainerLight,
    secondary = OnSurfaceVariantLight,
    onSecondary = SurfaceLight,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = OnSurfaceLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = IndigoAccentDark,
    onPrimary = BackgroundDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = OnIndigoContainerDark,
    secondary = OnSurfaceVariantDark,
    onSecondary = BackgroundDark,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = OnSurfaceDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

val TechTermsShapes = Shapes(
    small = RoundedCornerShape(999.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun TechTermsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    paper: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        paper -> PaperColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val extendedColors = when {
        paper -> PaperExtendedColors
        darkTheme -> DarkExtendedColors
        else -> LightExtendedColors
    }

    CompositionLocalProvider(
        LocalTechTermsColors provides extendedColors,
        LocalPaperTheme provides paper
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = if (paper) PaperTypography else TechTermsTypography,
            shapes = TechTermsShapes,
            content = content
        )
    }
}
