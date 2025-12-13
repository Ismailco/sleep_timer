package com.anonymous.sleep_timer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anonymous.sleep_timer.sleep.SleepTimerService
import com.anonymous.sleep_timer.ui.theme.SleepTimerTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SleepTimerTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          SleepTimerScreen()
        }
      }
    }
  }
}

@Composable
fun SleepTimerScreen() {
  val context = LocalContext.current
  val maxMinutes = 120
  val maxSeconds = maxMinutes * 60
  var totalSeconds by rememberSaveable { mutableIntStateOf(0) }
  var remainingSeconds by rememberSaveable { mutableIntStateOf(0) }
  var isRunning by rememberSaveable { mutableStateOf(false) }
  var knobAngle by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(totalSeconds) {
    knobAngle = if (totalSeconds == 0) 0f else (totalSeconds / maxSeconds.toFloat()) * 360f
  }

  LaunchedEffect(isRunning) {
    if (!isRunning) return@LaunchedEffect
    while (isRunning && remainingSeconds > 0) {
      delay(1000)
      remainingSeconds = (remainingSeconds - 1).coerceAtLeast(0)
      if (remainingSeconds == 0) {
        isRunning = false
        totalSeconds = 0
        SleepTimerService.stop(context)
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 24.dp, vertical = 40.dp),
    verticalArrangement = Arrangement.SpaceBetween,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "Sleep Timer",
        style = MaterialTheme.typography.headlineLarge,
        color = Color.White
      )
      Text(
        text = "Scroll around the circle to set time",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSecondary,
        modifier = Modifier.padding(top = 8.dp)
      )
      Text(
        text = "When the timer ends, media fades out smoothly and playback pauses automatically.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 4.dp),
        textAlign = TextAlign.Center
      )
    }

    TimerDial(
      totalSeconds = totalSeconds,
      remainingSeconds = remainingSeconds,
      knobAngle = knobAngle,
      isRunning = isRunning,
      onAngleChange = { angle ->
        val snappedAngle = (angle / 5).roundToInt() * 5f
        knobAngle = snappedAngle
        val seconds = ((snappedAngle / 360f) * maxSeconds).roundToInt().coerceIn(0, maxSeconds)
        totalSeconds = seconds
        if (!isRunning) {
          remainingSeconds = seconds
        } else {
          isRunning = false
          remainingSeconds = seconds
          SleepTimerService.stop(context)
        }
      }
    )

    ControlButtons(
      isRunning = isRunning,
      canStart = totalSeconds > 0,
      modifier = Modifier.padding(bottom = 32.dp),
      onStart = {
        if (remainingSeconds == 0) remainingSeconds = totalSeconds
        isRunning = true
        SleepTimerService.start(context, remainingSeconds * 1000L)
      },
      onPause = {
        isRunning = false
        SleepTimerService.stop(context)
      }
    )
  }
}

@Composable
fun TimerDial(
  totalSeconds: Int,
  remainingSeconds: Int,
  knobAngle: Float,
  isRunning: Boolean,
  onAngleChange: (Float) -> Unit,
  dialSize: Dp = 280.dp
) {
  val progress = if (totalSeconds == 0) 0f else remainingSeconds / totalSeconds.toFloat()
  var boxSize by remember { mutableStateOf(0f) }

  Box(
    modifier = Modifier
      .size(dialSize)
      .clip(CircleShape)
      .background(Color(0x1AFFFFFF))
      .then(
        if (isRunning) {
          Modifier
        } else {
          Modifier.pointerInput(Unit) {
            detectDragGestures { change, _ ->
              val center = Offset(boxSize / 2f, boxSize / 2f)
              val vector = change.position - center
              val angle =
                (Math.toDegrees(atan2(vector.y.toDouble(), vector.x.toDouble())) + 450) % 360
              onAngleChange(angle.toFloat())
            }
          }
        }
      ),
    contentAlignment = Alignment.Center
  ) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
      val canvasDiameter = this.size.minDimension
      if (boxSize == 0f) boxSize = canvasDiameter
      val strokeWidth = 12.dp.toPx()
      val radius = (canvasDiameter - strokeWidth) / 2

      drawCircle(
        color = Color(0x33FFFFFF),
        radius = radius,
        style = Stroke(width = strokeWidth)
      )

      drawArc(
        brush = Brush.sweepGradient(
          colors = listOf(
            Color(0xFF6C63FF),
            Color(0xFF8A7BFF),
            Color(0xFF6C63FF)
          )
        ),
        startAngle = -90f,
        sweepAngle = (totalSeconds / (120f * 60f)) * 360f,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )

      drawArc(
        color = Color(0x55FFFFFF),
        startAngle = -90f,
        sweepAngle = progress * 360f,
        useCenter = false,
        style = Stroke(width = strokeWidth / 2, cap = StrokeCap.Round)
      )

      val knobRadius = 14.dp.toPx()
      val knobCenter = polarToCartesian(radius, knobAngle - 90f) + center
      drawCircle(
        color = Color(0xFF6C63FF),
        radius = knobRadius,
        center = knobCenter
      )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      val minutes = remainingSeconds / 60
      val seconds = remainingSeconds % 60
      Text(
        text = "%02d:%02d".format(minutes, seconds),
        style = MaterialTheme.typography.displayMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "${totalSeconds / 60} min",
        color = MaterialTheme.colorScheme.onSecondary,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
  }
}

@Composable
fun ControlButtons(
  isRunning: Boolean,
  canStart: Boolean,
  modifier: Modifier = Modifier,
  onStart: () -> Unit,
  onPause: () -> Unit
) {
  Row(
    modifier = modifier
      .fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    TextButton(
      onClick = onPause,
      enabled = isRunning,
      modifier = Modifier
        .weight(1f)
        .height(56.dp),
      colors = ButtonDefaults.textButtonColors(
        containerColor = Color.Transparent,
        contentColor = if (isRunning) Color.White else Color(0x66FFFFFF)
      )
    ) {
      Text(text = "Pause", fontSize = 16.sp)
    }

    TextButton(
      onClick = onStart,
      enabled = !isRunning && canStart,
      modifier = Modifier
        .weight(1f)
        .height(56.dp),
      colors = ButtonDefaults.textButtonColors(
        containerColor = if (!isRunning && canStart) Color(0xFF6C63FF) else Color(0x336C63FF),
        contentColor = if (!isRunning && canStart) Color(0xFF0E0F1C) else Color.White
      )
    ) {
      Text(text = if (isRunning) "Running" else "Start", fontSize = 16.sp)
    }
  }
}

private fun polarToCartesian(radius: Float, angle: Float): Offset {
  val rad = angle * PI.toFloat() / 180f
  return Offset(
    radius * cos(rad),
    radius * sin(rad)
  )
}
