package com.anonymous.sleep_timer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
  primary = Lilac,
  onPrimary = Midnight,
  primaryContainer = SoftLilac,
  onPrimaryContainer = Color.White,
  secondary = Mist,
  onSecondary = Color.White,
  background = Midnight,
  onBackground = Color.White,
  surface = Midnight,
  onSurface = Color.White
)

private val LightColors = lightColorScheme(
  primary = Lilac,
  onPrimary = Midnight,
  primaryContainer = SoftLilac,
  onPrimaryContainer = Color.White,
  secondary = Mist,
  onSecondary = Color.White,
  background = Midnight,
  onBackground = Color.White,
  surface = Midnight,
  onSurface = Color.White
)

@Composable
fun SleepTimerTheme(
  useDarkTheme: Boolean = !isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colors = if (useDarkTheme) DarkColors else LightColors

  MaterialTheme(
    colorScheme = colors,
    typography = MaterialTheme.typography,
    content = content
  )
}
