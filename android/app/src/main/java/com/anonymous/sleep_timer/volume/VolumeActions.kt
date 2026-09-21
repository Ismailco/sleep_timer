package com.anonymous.sleep_timer.volume

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.view.KeyEvent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.anonymous.sleep_timer.R

object VolumeActions {

  const val CHANNEL_ID = "sleep_timer_channel"
  const val RUNNING_NOTIFICATION_ID = 2001
  const val COMPLETED_NOTIFICATION_ID = 2002

  fun fadeOutAndPause(
    context: Context,
    durationMs: Long = 4000L,
    steps: Int = 20,
    onComplete: () -> Unit = {}
  ) {
    val audioManager =
      context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    if (startVolume == 0 || steps == 0 || durationMs == 0L) {
      audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
      sendMediaPause(context)
      onComplete()
      return
    }

    val stepDelay = durationMs / steps
    for (i in 0..steps) {
      val delay = i * stepDelay
      Handler(Looper.getMainLooper()).postDelayed({
        val fraction = i.toFloat() / steps
        val newVolume = (startVolume * (1f - fraction)).toInt().coerceAtLeast(0)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        if (i == steps) {
          sendMediaPause(context)
          onComplete()
        }
      }, delay)
    }
  }

  fun requestAudioFocus(context: Context) {
    val audioManager =
      context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setOnAudioFocusChangeListener { }
        .build()
      audioManager.requestAudioFocus(focusRequest)
      Handler(Looper.getMainLooper()).postDelayed({
        audioManager.abandonAudioFocusRequest(focusRequest)
      }, 1500)
    } else {
      @Suppress("DEPRECATION")
      audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
      @Suppress("DEPRECATION")
      audioManager.abandonAudioFocus(null)
    }
  }

  private fun sendMediaPause(context: Context) {
    val audioManager =
      context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.dispatchMediaKeyEvent(
      KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)
    )
    audioManager.dispatchMediaKeyEvent(
      KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE)
    )
  }

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val channel = NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT
      )
      channel.description = context.getString(R.string.notification_channel_description)
      manager.createNotificationChannel(channel)
    }
  }

  fun showCompletionNotification(context: Context) {
    ensureChannel(context)
    val manager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle("Sleep timer finished")
      .setContentText("Media volume muted.")
      .setAutoCancel(true)
      .build()
    manager.notify(COMPLETED_NOTIFICATION_ID, notification)
  }

  fun showCompletionToast(context: Context) {
    Handler(Looper.getMainLooper()).post {
      Toast.makeText(context, "Sleep timer completed", Toast.LENGTH_SHORT).show()
    }
  }
}
