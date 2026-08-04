package com.samroid.wled.presentation.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7C5CFF),
    onPrimary = Color.White,

    secondary = Color(0xFF57C8FF),
    onSecondary = Color.White,

    tertiary = Color(0xFF39D98A),

    background = Color(0xFF090B12),
    onBackground = Color(0xFFF5F7FA),

    surface = Color(0xFF121826),
    surfaceBright = Color(0xFF1A2235),
    surfaceContainer = Color(0xFF1B2437),
    surfaceContainerHigh = Color(0xFF202B41),
    surfaceContainerHighest = Color(0xFF29354D),

    onSurface = Color(0xFFF5F7FA),
    onSurfaceVariant = Color(0xFF98A4C0),

    outline = Color(0xFF38445C),

    error = Color(0xFFFF5D73),
    onError = Color.White,

    inversePrimary = Color(0xFFA08CFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6650FF),
    onPrimary = Color.White,

    secondary = Color(0xFF0B8DFF),
    onSecondary = Color.White,

    tertiary = Color(0xFF14A86C),

    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF0F172A),

    surface = Color.White,
    surfaceBright = Color(0xFFF7F8FC),
    surfaceContainer = Color(0xFFF2F4F9),
    surfaceContainerHigh = Color(0xFFECEFF7),
    surfaceContainerHighest = Color(0xFFE5EAF5),

    onSurface = Color(0xFF1C2333),
    onSurfaceVariant = Color(0xFF6D7891),

    outline = Color(0xFFD3DAE8),

    error = Color(0xFFE5485C),
    onError = Color.White
)

@Composable
fun WLEDTheme(
    darkTheme: Boolean = true,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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

    MaterialTheme(

        colorScheme =
            if (darkTheme)
                DarkColorScheme
            else
                LightColorScheme,


        typography = AppTypography,


        shapes = AppShapes,


        content = content
    )
}