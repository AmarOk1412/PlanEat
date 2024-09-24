package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.compose.onSurfaceVariantLight
import com.planeat.planeat.R

val UbuntuFontFamily = FontFamily(
    Font(R.font.ubuntu_bold, FontWeight.Bold),
    Font(R.font.ubuntu_medium, FontWeight.Medium),
    Font(R.font.ubuntu_regular, FontWeight.Normal)
)
val RobotoFontFamily = FontFamily(
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_regular, FontWeight.Normal)
)
val RobotoFlexFontFamily = FontFamily(
    Font(R.font.roboto_flex, FontWeight.Normal)
)

val factor = 1.5

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = UbuntuFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp*factor,
        lineHeight = 64.sp*factor,
        letterSpacing = (-0.25*factor).sp
    ),
    displayMedium = TextStyle(
        fontFamily = RobotoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp*factor,
        lineHeight = 52.sp*factor
    ),
    displaySmall = TextStyle(
        fontFamily = RobotoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp*factor,
        lineHeight = 44.sp*factor
    ),
    headlineLarge = TextStyle(
        fontFamily = UbuntuFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp*factor,
        lineHeight = 36.sp*factor
    ),
    headlineMedium = TextStyle(
        fontFamily = UbuntuFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp*factor,
        lineHeight = 28.sp*factor
    ),
    headlineSmall = TextStyle(
        fontFamily = UbuntuFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp*factor,
        lineHeight = 22.sp*factor
    ),
    titleLarge = TextStyle(
        fontFamily = UbuntuFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp*factor,
        lineHeight = 28.sp*factor
    ),
    titleMedium = TextStyle(
        fontFamily = UbuntuFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp*factor,
        lineHeight = 24.sp*factor,
        letterSpacing = 0.15.sp*factor
    ),
    titleSmall = TextStyle(
        fontFamily = UbuntuFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp*factor,
        lineHeight = 16.sp*factor
    ),
    labelLarge = TextStyle(
        fontFamily = RobotoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp*factor,
        lineHeight = 20.sp*factor
    ),
    labelMedium = TextStyle(
        fontFamily = RobotoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp*factor,
        lineHeight = 16.sp*factor,
        letterSpacing = 0.5.sp*factor
    ),
    labelSmall = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp*factor,
        lineHeight = 12.sp*factor
    ),
    bodyLarge = TextStyle(
        fontFamily = RobotoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp*factor,
        lineHeight = 24.sp*factor,
        letterSpacing = 0.5.sp*factor
    ),
    bodyMedium = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp*factor,
        lineHeight = 20.sp*factor,
        letterSpacing = 0.1.sp*factor
    ),
    bodySmall = TextStyle(
        fontFamily = RobotoFlexFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp*factor,
        lineHeight = 16.sp*factor,
        color = onSurfaceVariantLight
    )
)