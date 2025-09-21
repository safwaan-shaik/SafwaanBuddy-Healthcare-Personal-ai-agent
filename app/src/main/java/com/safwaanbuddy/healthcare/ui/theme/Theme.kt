package com.safwaanbuddy.healthcare.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SafwaanBuddyColors.HolographicBlue,
    secondary = SafwaanBuddyColors.BioGreen,
    tertiary = SafwaanBuddyColors.ElectricBlue,
    background = SafwaanBuddyColors.DarkBackground,
    surface = SafwaanBuddyColors.CardBackground,
    onPrimary = SafwaanBuddyColors.DarkBackground,
    onSecondary = SafwaanBuddyColors.DarkBackground,
    onTertiary = SafwaanBuddyColors.DarkBackground,
    onBackground = SafwaanBuddyColors.PrimaryText,
    onSurface = SafwaanBuddyColors.PrimaryText,
    error = SafwaanBuddyColors.CriticalRed
)

@Composable
fun SafwaanBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkColorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = SafwaanBuddyTypography,
        content = content
    )
}