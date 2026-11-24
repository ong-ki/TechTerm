package com.techterms.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
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

val LocalTechTermsColors = staticCompositionLocalOf { LightExtendedColors }

object TechTermsThemeTokens {
    val extendedColors: TechTermsExtendedColors
        @Composable
        get() = LocalTechTermsColors.current
}

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
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalTechTermsColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TechTermsTypography,
            shapes = TechTermsShapes,
            content = content
        )
    }
}
