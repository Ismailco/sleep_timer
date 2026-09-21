package com.anonymous.sleep_timer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
  primary = Cyan,
  onPrimary = DeepNavy,
  primaryContainer = SurfaceBlue,
  onPrimaryContainer = TextPrimary,
  secondary = Ice,
  onSecondary = DeepNavy,
  secondaryContainer = SurfaceBlue,
  onSecondaryContainer = TextPrimary,
  background = DeepNavy,
  onBackground = TextPrimary,
  surface = NightSurface,
  onSurface = TextPrimary,
  surfaceVariant = SurfaceBlue,
  onSurfaceVariant = TextMuted,
  outline = OutlineBlue
)

@Composable
fun SleepTimerTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = DarkColors,
    typography = MaterialTheme.typography,
    content = content
  )
}
