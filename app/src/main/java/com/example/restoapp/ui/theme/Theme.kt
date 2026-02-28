package com.example.restoapp.ui.theme

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

/**
 * Skema warna untuk mode gelap.
 * Menggunakan warna oranye sebagai warna utama.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    secondary = SecondaryGold,
    tertiary = PrimaryOrangeLight,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = TextOnPrimary,
    onSecondary = TextOnPrimary,
    onTertiary = TextOnPrimary,
    onBackground = Color.White,
    onSurface = Color.White
)

/**
 * Skema warna untuk mode terang.
 * Ini adalah skema default yang dipakai aplikasi.
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    secondary = SecondaryGold,
    tertiary = PrimaryOrangeLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = TextOnPrimary,
    onSecondary = TextOnPrimary,
    onTertiary = TextOnPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

/**
 * Tema utama aplikasi RestoApp.
 *
 * Dynamic color dinonaktifkan agar warna tema kustom tetap konsisten
 * di semua perangkat, tidak mengikuti wallpaper pengguna.
 */
@Composable
fun RestoAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Nonaktifkan dynamic color agar tema kustom tetap dipakai
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

    // Atur warna status bar agar transparan
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

