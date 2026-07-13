package com.example.ui.theme

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
    primary = NeonCyan,
    secondary = ElectricTeal,
    tertiary = SlateGray,
    background = DeepNavy,
    surface = SpaceNavy,
    onPrimary = DeepNavy,
    onSecondary = DeepNavy,
    onBackground = Color.White,
    onSurface = Color.White
  )

private val LightColorScheme = DarkColorScheme // Keep it consistently Deep Tech even in light mode for extreme brand identity

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark mode for Deep Tech corporate aesthetic
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve our intentional Agritech palette
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
