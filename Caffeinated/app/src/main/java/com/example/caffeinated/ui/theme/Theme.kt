package com.example.caffeinated.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF8f6c50),
        secondary = Color(0xFF96765d),
        tertiary = Color(0xFFa99486),
        background = Color(0xFF231f1d),
        surface = Color(0xFF1E1E1E),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF707070),
        onSurface = Color(0xFFc1c1c1),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFFa57b5b),
        secondary = Color(0xFFAD876A),
        tertiary = Color(0xFFC3AA9A),
        background = Color(0xFFfff8f0),
        surface = Color(0xFFF5F5F5),
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onTertiary = Color.Black,
        onBackground = Color(0xFF707070),
        onSurface = Color(0xFFE0E0E0),
    )

@Composable
fun CaffeinatedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
