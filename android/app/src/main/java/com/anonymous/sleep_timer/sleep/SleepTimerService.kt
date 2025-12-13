package com.anonymous.sleep_timer.sleep

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.anonymous.sleep_timer.R
import com.anonymous.sleep_timer.volume.VolumeActions

class SleepTimerService : Service() {

  private var countDownTimer: CountDownTimer? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> {
        val durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 0L)
        if (durationMs > 0L) {
          startTimer(durationMs)
        } else {
          stopSelf()
        }
      }
      ACTION_STOP -> {
        stopTimer()
      }
    }
    return START_NOT_STICKY
  }

  private fun startTimer(durationMs: Long) {
    VolumeActions.ensureChannel(this)
    startForeground(
      VolumeActions.RUNNING_NOTIFICATION_ID,
      buildRunningNotification(durationMs)
    )
    countDownTimer?.cancel()
    countDownTimer = object : CountDownTimer(durationMs, 1000L) {
      override fun onTick(millisUntilFinished: Long) {
        updateRunningNotification(millisUntilFinished)
      }

      override fun onFinish() {
        completeTimer()
      }
    }.start()
  }

  private fun stopTimer() {
    countDownTimer?.cancel()
    countDownTimer = null
    val manager =
      getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.cancel(VolumeActions.RUNNING_NOTIFICATION_ID)
    stopForeground(true)
    stopSelf()
  }

  private fun completeTimer() {
    VolumeActions.fadeOutAndPause(
      context = this,
      durationMs = 6000L,
      steps = 24
    ) {
      VolumeActions.requestAudioFocus(this)
      VolumeActions.showCompletionNotification(this)
      VolumeActions.showCompletionToast(this)
      stopForeground(true)
      stopSelf()
    }
  }

  private fun buildRunningNotification(remainingMs: Long): Notification {
    val formatted = formatRemaining(remainingMs)
    return NotificationCompat.Builder(this, VolumeActions.CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("Sleep timer running")
      .setContentText("$formatted remaining")
      .setOnlyAlertOnce(true)
      .setOngoing(true)
      .build()
  }

  private fun updateRunningNotification(remainingMs: Long) {
    val manager =
      getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(
      VolumeActions.RUNNING_NOTIFICATION_ID,
      buildRunningNotification(remainingMs)
    )
  }

  private fun formatRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
  }

  override fun onDestroy() {
    countDownTimer?.cancel()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  companion object {
    const val ACTION_START = "com.anonymous.sleep_timer.sleep.START"
    const val ACTION_STOP = "com.anonymous.sleep_timer.sleep.STOP"
    const val EXTRA_DURATION_MS = "EXTRA_DURATION_MS"

    fun start(context: Context, durationMs: Long) {
      val intent = Intent(context, SleepTimerService::class.java).apply {
        action = ACTION_START
        putExtra(EXTRA_DURATION_MS, durationMs)
      }
      context.startForegroundService(intent)
    }

    fun stop(context: Context) {
      val intent = Intent(context, SleepTimerService::class.java).apply {
        action = ACTION_STOP
      }
      context.startService(intent)
    }
  }
}
