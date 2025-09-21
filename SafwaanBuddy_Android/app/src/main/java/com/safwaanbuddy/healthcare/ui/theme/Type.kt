package com.safwaanbuddy.healthcare.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.safwaanbuddy.healthcare.R

// IMPORTANT: Download the Orbitron and Exo font families from Google Fonts
// and place the .ttf files in the `app/src/main/res/font` directory.
// https://fonts.google.com/specimen/Orbitron
// https://fonts.google.com/specimen/Exo

val OrbitronFontFamily = FontFamily(
    Font(R.font.orbitron_regular, FontWeight.Normal),
    Font(R.font.orbitron_medium, FontWeight.Medium),
    Font(R.font.orbitron_bold, FontWeight.Bold),
    Font(R.font.orbitron_black, FontWeight.Black)
)

val ExoFontFamily = FontFamily(
    Font(R.font.exo_light, FontWeight.Light),
    Font(R.font.exo_regular, FontWeight.Normal),
    Font(R.font.exo_medium, FontWeight.Medium),
    Font(R.font.exo_semibold, FontWeight.SemiBold),
    Font(R.font.exo_bold, FontWeight.Bold)
)

val SafwaanBuddyTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 57.sp,
        letterSpacing = (-0.25).sp,
        color = SafwaanBuddyColors.HolographicBlue
    ),
    displayMedium = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        color = SafwaanBuddyColors.AccentText
    ),
    headlineSmall = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = SafwaanBuddyColors.GlowText
    ),
    bodyLarge = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        color = SafwaanBuddyColors.PrimaryText
    ),
    labelLarge = TextStyle(
        fontFamily = ExoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = SafwaanBuddyColors.AccentText
    )
)