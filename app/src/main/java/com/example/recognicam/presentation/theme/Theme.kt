package com.example.recognicam.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light color scheme - more white with green/brown accents
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = LightGreen.copy(alpha = 0.7f), // Softer green for containers
    onPrimaryContainer = OnBackground,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = Tertiary.copy(alpha = 0.5f), // Lighter container
    onSecondaryContainer = OnBackground,
    tertiary = Tertiary,
    onTertiary = OnBackground,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error,
    onError = Color(0xFFFFFFFF)
)

// Dark color scheme - keep the existing dark theme
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = OnDarkPrimary,
    primaryContainer = DarkPrimaryVariant,
    onPrimaryContainer = OnDarkPrimary,
    secondary = DarkSecondary,
    onSecondary = OnDarkSecondary,
    secondaryContainer = DarkSecondaryVariant,
    onSecondaryContainer = OnDarkSecondary,
    tertiary = DarkSecondary,
    onTertiary = OnDarkSecondary,
    background = DarkBackground,
    onBackground = OnDarkBackground,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,
    error = Error,
    onError = Color(0xFFFFFFFF)
)

@Composable
fun ReCogniCamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,  // Set to false to use our custom colors by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use a color that matches the theme better
            val statusBarColor = if (darkTheme) DarkBackground.toArgb() else Background.toArgb()
            window.statusBarColor = statusBarColor

            // Set status bar text/icons to be dark in light mode, light in dark mode
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}