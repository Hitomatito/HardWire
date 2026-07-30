package com.hitomatito.hardwire.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = HardwirePrimary,
    onPrimary = HardwireOnPrimary,
    secondary = HardwireSecondary,
    background = HardwireBackground,
    surface = HardwireSurface,
    onBackground = HardwireOnBackground,
    onSurface = HardwireOnSurface,
    error = HardwireError
)

private val LightColorScheme = lightColorScheme(
    primary = HardwirePrimary,
    onPrimary = HardwireOnPrimary,
    secondary = HardwireSecondary,
    background = HardwireBackgroundLight,
    surface = HardwireSurfaceLight,
    onBackground = HardwireOnBackgroundLight,
    onSurface = HardwireOnSurfaceLight,
    error = HardwireErrorLight
)

@Composable
fun HardwireTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else null

    val colorScheme = dynamicColor ?: if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
