package com.anonymous.sleep_timer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anonymous.sleep_timer.R
import com.anonymous.sleep_timer.sleep.SleepTimerService
import com.anonymous.sleep_timer.ui.theme.Cyan
import com.anonymous.sleep_timer.ui.theme.DeepNavy
import com.anonymous.sleep_timer.ui.theme.Ice
import com.anonymous.sleep_timer.ui.theme.NightSurface
import com.anonymous.sleep_timer.ui.theme.OutlineBlue
import com.anonymous.sleep_timer.ui.theme.TextMuted
import com.anonymous.sleep_timer.ui.theme.TextPrimary
import com.anonymous.sleep_timer.ui.theme.TrackBlue
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MAX_MINUTES = 120
private const val MAX_SECONDS = MAX_MINUTES * 60
private val QUICK_PRESETS = listOf(15, 30, 45, 60)

@Composable
fun SleepTimerScreen() {
  var showSplash by remember { mutableStateOf(true) }

  if (showSplash) {
    SleepTimerSplash(onFinished = { showSplash = false })
  } else {
    SleepTimerContent()
  }
}

@Composable
private fun SleepTimerSplash(onFinished: () -> Unit) {
  LaunchedEffect(Unit) {
    delay(1000)
    onFinished()
  }

  Image(
    painter = painterResource(R.drawable.sleep_timer_splash),
    contentDescription = null,
    modifier = Modifier.fillMaxSize(),
    contentScale = ContentScale.Crop
  )
}

@Composable
private fun SleepTimerContent() {
  val context = LocalContext.current
  var selectedSeconds by rememberSaveable { mutableIntStateOf(0) }
  var remainingSeconds by rememberSaveable { mutableIntStateOf(0) }
  var isRunning by rememberSaveable { mutableStateOf(false) }

  fun chooseDuration(seconds: Int) {
    if (isRunning) {
      isRunning = false
      SleepTimerService.stop(context)
    }
    selectedSeconds = seconds
    remainingSeconds = seconds
  }

  LaunchedEffect(isRunning) {
    if (!isRunning) return@LaunchedEffect
    while (remainingSeconds > 0) {
      delay(1000)
      remainingSeconds = (remainingSeconds - 1).coerceAtLeast(0)
      if (remainingSeconds == 0) {
        isRunning = false
        selectedSeconds = 0
        SleepTimerService.stop(context)
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(DeepNavy, Color(0xFF071B3D), DeepNavy)
        )
      )
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding(),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      item { Header() }
      item {
        TimerDialCard(
          selectedSeconds = selectedSeconds,
          remainingSeconds = remainingSeconds,
          isRunning = isRunning,
          onDurationChange = ::chooseDuration
        )
      }
      item {
        QuickPresets(
          selectedSeconds = selectedSeconds,
          onPresetSelected = { chooseDuration(it * 60) }
        )
      }
      item {
        TimerAction(
          isRunning = isRunning,
          canStart = selectedSeconds > 0,
          onStart = {
            val duration = remainingSeconds.takeIf { it > 0 } ?: selectedSeconds
            if (duration > 0) {
              selectedSeconds = duration
              remainingSeconds = duration
              isRunning = true
              SleepTimerService.start(context, duration * 1000L)
            }
          },
          onPause = {
            isRunning = false
            SleepTimerService.stop(context)
          }
        )
      }
      item { CompletionBehaviorCard() }
    }
  }
}

@Composable
private fun Header() {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(56.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(Color(0xFF0B2A58)),
      contentAlignment = Alignment.Center
    ) {
      androidx.compose.foundation.Image(
        painter = painterResource(R.drawable.sleep_timer_logo),
        contentDescription = "Sleep Timer logo",
        modifier = Modifier.size(48.dp)
      )
    }
    Spacer(modifier = Modifier.width(14.dp))
    Column {
      Text(
        text = "Sleep Timer",
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Fall asleep, worry less.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted
      )
    }
  }
}

@Composable
private fun TimerDialCard(
  selectedSeconds: Int,
  remainingSeconds: Int,
  isRunning: Boolean,
  onDurationChange: (Int) -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(30.dp),
    colors = CardDefaults.cardColors(containerColor = NightSurface.copy(alpha = 0.88f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = if (isRunning) "Timer running" else "Set your sleep timer",
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = if (isRunning) "Playback will fade out when time is up" else "Drag around the dial to choose a duration",
        style = MaterialTheme.typography.bodySmall,
        color = TextMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp)
      )
      Spacer(modifier = Modifier.height(14.dp))
      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        TimerDial(
          selectedSeconds = selectedSeconds,
          remainingSeconds = remainingSeconds,
          isRunning = isRunning,
          dialSize = if (maxWidth < 312.dp) maxWidth else 312.dp,
          onDurationChange = onDurationChange
        )
      }
    }
  }
}

@Composable
private fun TimerDial(
  selectedSeconds: Int,
  remainingSeconds: Int,
  isRunning: Boolean,
  dialSize: Dp,
  onDurationChange: (Int) -> Unit
) {
  var boxSize by remember { mutableIntStateOf(0) }
  val selectedProgress = selectedSeconds / MAX_SECONDS.toFloat()
  val remainingProgress = if (selectedSeconds == 0) 0f else remainingSeconds / selectedSeconds.toFloat()
  val knobAngle = selectedProgress * 360f

  Box(
    modifier = Modifier
      .size(dialSize)
      .clip(CircleShape)
      .semantics {
        role = Role.Button
        contentDescription = if (isRunning) {
          "Timer running for ${formatDurationLabel(remainingSeconds)}"
        } else {
          "Timer dial, ${formatDurationLabel(selectedSeconds)} selected"
        }
      }
      .pointerInput(isRunning, boxSize) {
        if (isRunning) return@pointerInput
        detectDragGestures { change, _ ->
          change.consume()
          if (boxSize == 0) return@detectDragGestures
          val center = Offset(boxSize / 2f, boxSize / 2f)
          val vector = change.position - center
          val angle = ((Math.toDegrees(atan2(vector.y.toDouble(), vector.x.toDouble())) + 450) % 360)
            .toFloat()
          val snappedAngle = (angle / 5f).roundToInt() * 5f
          val seconds = ((snappedAngle / 360f) * MAX_SECONDS)
            .roundToInt()
            .coerceIn(0, MAX_SECONDS)
          onDurationChange(seconds)
        }
      },
    contentAlignment = Alignment.Center
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .onSizeChanged { boxSize = it.width }
    ) {
      val diameter = size.minDimension
      val strokeWidth = 12.dp.toPx()
      val radius = (diameter - strokeWidth) / 2f
      val center = Offset(size.width / 2f, size.height / 2f)
      val knobRadius = 14.dp.toPx()
      val knobCenter = polarToCartesian(radius, knobAngle - 90f) + center

      drawCircle(
        color = TrackBlue,
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth)
      )
      if (selectedProgress > 0f) {
        drawArc(
          brush = Brush.sweepGradient(colors = listOf(Ice, Cyan, Color(0xFF4E9BFF), Ice)),
          startAngle = -90f,
          sweepAngle = if (selectedProgress >= 1f) 359.9f else selectedProgress * 360f,
          useCenter = false,
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
      }
      if (isRunning && remainingProgress > 0f) {
        drawArc(
          color = Color.White.copy(alpha = 0.65f),
          startAngle = -90f,
          sweepAngle = remainingProgress * 360f,
          useCenter = false,
          style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
      }
      drawCircle(
        color = Cyan.copy(alpha = 0.18f),
        radius = knobRadius + 10.dp.toPx(),
        center = knobCenter
      )
      drawCircle(color = Ice, radius = knobRadius, center = knobCenter)
      drawCircle(color = Color(0xFF4A9DFF), radius = 5.dp.toPx(), center = knobCenter)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = formatClock(remainingSeconds),
        style = MaterialTheme.typography.displayMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
      Text(
        text = formatDurationLabel(if (remainingSeconds > 0) remainingSeconds else selectedSeconds),
        style = MaterialTheme.typography.titleMedium,
        color = Ice,
        modifier = Modifier.padding(top = 2.dp)
      )
    }
  }
}

@Composable
private fun QuickPresets(
  selectedSeconds: Int,
  onPresetSelected: (Int) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "Quick presets",
      style = MaterialTheme.typography.labelLarge,
      color = TextMuted,
      modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      QUICK_PRESETS.forEach { minutes ->
        val presetSeconds = minutes * 60
        OutlinedButton(
          onClick = { onPresetSelected(minutes) },
          modifier = Modifier
            .weight(1f)
            .height(64.dp),
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(
            width = if (selectedSeconds == presetSeconds) 2.dp else 1.dp,
            color = if (selectedSeconds == presetSeconds) Ice else OutlineBlue
          ),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selectedSeconds == presetSeconds) Color(0xFF0C467D) else Color(0x331A3760),
            contentColor = TextPrimary
          ),
          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = minutes.toString(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(text = "min", style = MaterialTheme.typography.labelSmall, color = TextMuted)
          }
        }
      }
    }
  }
}

@Composable
private fun TimerAction(
  isRunning: Boolean,
  canStart: Boolean,
  onStart: () -> Unit,
  onPause: () -> Unit
) {
  if (isRunning) {
    OutlinedButton(
      onClick = onPause,
      modifier = Modifier
        .fillMaxWidth()
        .height(60.dp),
      shape = RoundedCornerShape(22.dp),
      border = BorderStroke(2.dp, Ice),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
    ) {
      Icon(Icons.Outlined.Pause, contentDescription = null)
      Spacer(modifier = Modifier.width(10.dp))
      Text("Pause timer", fontWeight = FontWeight.Bold)
    }
  } else {
    Button(
      onClick = onStart,
      enabled = canStart,
      modifier = Modifier
        .fillMaxWidth()
        .height(60.dp),
      shape = RoundedCornerShape(22.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = Ice,
        contentColor = Color(0xFF05254A),
        disabledContainerColor = Color(0x334A7299),
        disabledContentColor = TextMuted
      )
    ) {
      Icon(Icons.Outlined.PlayArrow, contentDescription = null)
      Spacer(modifier = Modifier.width(10.dp))
      Text("Start timer", fontWeight = FontWeight.Bold)
    }
  }
}

@Suppress("DEPRECATION")
@Composable
private fun CompletionBehaviorCard() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0x331A3760))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(Color(0xFF123A67)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Outlined.VolumeOff, contentDescription = null, tint = Ice)
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(
          text = "When the timer ends",
          style = MaterialTheme.typography.labelMedium,
          color = TextMuted
        )
        Text(
          text = "Fade media out and pause playback",
          style = MaterialTheme.typography.bodyLarge,
          color = TextPrimary,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

private fun formatClock(totalSeconds: Int): String {
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return "%02d:%02d".format(minutes, seconds)
}

private fun formatDurationLabel(totalSeconds: Int): String {
  if (totalSeconds <= 0) return "Choose a duration"
  val totalMinutes = totalSeconds / 60
  val hours = totalMinutes / 60
  val minutes = totalMinutes % 60
  val seconds = totalSeconds % 60
  return when {
    hours > 0 && minutes > 0 -> "${hours}h ${minutes} min"
    hours > 0 -> "${hours}h"
    minutes > 0 && seconds > 0 -> "${minutes} min ${seconds} sec"
    minutes > 0 -> "${minutes} min"
    else -> "${seconds} sec"
  }
}

private fun polarToCartesian(radius: Float, angle: Float): Offset {
  val radians = angle * PI.toFloat() / 180f
  return Offset(radius * cos(radians), radius * sin(radians))
}
