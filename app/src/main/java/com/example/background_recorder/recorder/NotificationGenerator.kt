package com.example.background_recorder.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.background_recorder.R

internal object NotificationGenerator {
    private const val NOTIFICATION_ID = 9999
    private const val ANDROID_CHANNEL_ID = "foreground_audiorecorder"
    internal fun generateNotification(context: Context, layout: Int): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ANDROID_CHANNEL_ID,
                "AudioService",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                lightColor = Color.BLUE
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notificationLayout = RemoteViews(context.packageName, layout).apply {
            setTextViewText(R.id.timeTextView, AudioTimer.getTimeStamp())
            setOnClickPendingIntent(
                R.id.stopButton,
                PendingIntent.getActivity(
                    context,
                    1000,
                    Intent(context, RecorderMainActivity::class.java).apply {
                        putExtra("EXTRA_FUNCTION_TO_CALL", "save")
                        setAction("CLICKED_SAVEBTN_ACTION")
                      },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            setOnClickPendingIntent(
                R.id.recordButton,
                PendingIntent.getBroadcast(
                    context,
                    1000,
                    Intent(context, AudioReceiver::class.java)
                        .apply {
                            action = AudioReceiver.ACTION_RECORD
                        },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            setOnClickPendingIntent(
                R.id.cancelButton,
                PendingIntent.getBroadcast(
                    context,
                    1000,
                    Intent(context, AudioReceiver::class.java)
                        .apply {
                            action = AudioReceiver.ACTION_CANCEL
                        },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            setOnClickPendingIntent(
                R.id.timeTextView,
                PendingIntent.getActivity(
                    context,
                    1000,
                    Intent(context, RecorderMainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        return NotificationCompat.Builder(context, ANDROID_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_mic_24)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCustomContentView(notificationLayout)
            .build()
    }

     internal fun notifyNotification(context: Context, layout: Int) {
         val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
         val notification = generateNotification(context, layout)
         manager.notify(NOTIFICATION_ID, notification)
     }
}