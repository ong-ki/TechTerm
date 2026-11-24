package com.techterms.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.techterms.app.R

val NotoSansMyanmarFontFamily = FontFamily(
    Font(R.font.noto_sans_myanmar_regular, FontWeight.Normal),
    Font(R.font.noto_sans_myanmar_medium, FontWeight.Medium),
    Font(R.font.noto_sans_myanmar_semibold, FontWeight.SemiBold),
    Font(R.font.noto_sans_myanmar_bold, FontWeight.Bold)
)

val EnglishSansFontFamily = FontFamily.SansSerif
val CodeMonoFontFamily = FontFamily.Monospace

// 1.6x lineHeight + Trim.None prevents stacked Myanmar diacritics and subscripts from clipping.
private val MyanmarLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

val MyanmarBodyLarge = TextStyle(
    fontFamily = NotoSansMyanmarFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 26.sp,
    lineHeightStyle = MyanmarLineHeightStyle
)

val MyanmarBodyMedium = TextStyle(
    fontFamily = NotoSansMyanmarFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 23.sp,
    lineHeightStyle = MyanmarLineHeightStyle
)

val TechTermsTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = EnglishSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = EnglishSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = EnglishSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = EnglishSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = EnglishSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = EnglishSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontFamily = EnglishSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = EnglishSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = EnglishSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)
