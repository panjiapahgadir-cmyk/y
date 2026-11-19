package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val PerangHantuColorScheme =
  darkColorScheme(
    primary = SpectralViolet,
    onPrimary = ObsidianNight,
    primaryContainer = SpectralVioletDeep,
    onPrimaryContainer = InkPrimary,
    secondary = SpiritGold,
    onSecondary = ObsidianNight,
    secondaryContainer = MysticalSurfaceHigh,
    onSecondaryContainer = SpiritGold,
    tertiary = SpiritCyan,
    onTertiary = ObsidianNight,
    background = ObsidianNight,
    onBackground = InkPrimary,
    surface = MidnightIndigo,
    onSurface = InkPrimary,
    surfaceVariant = MysticalSurface,
    onSurfaceVariant = InkMuted,
    outline = MysticalBorder,
    error = CursedCrimson,
    onError = ObsidianNight,
  )

val PerangHantuShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
  )

@Composable
fun PerangHantuTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = PerangHantuColorScheme,
    typography = AppTypography,
    shapes = PerangHantuShapes,
    content = content,
  )
}
