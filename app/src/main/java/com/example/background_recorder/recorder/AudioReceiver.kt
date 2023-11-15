package com.example.background_recorder.recorder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.example.background_recorder.R
import java.util.TimerTask

class AudioReceiver : BroadcastReceiver() {
    private val handler = Handler(Looper.getMainLooper())
    companion object {
        const val TAG = "RecordTest"
        internal const val ACTION_RECORD = "ACTION_RECORD"
        internal const val ACTION_STOP = "ACTION_STOP"
        internal const val ACTION_CANCEL = "ACTION_CANCEL"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(
            TAG,
            "지금 상태는 " + AudioRecorder.getInstance().recordingState + " 액션 : " + intent.action
        )
        when (intent.action) {
            ACTION_RECORD -> {
                when (AudioRecorder.getInstance().recordingState) {
                    RecordingState.BEFORE_RECORDING -> {
                        Log.d(TAG, "[리시버] 녹음 전 상태")
                        AudioTimer.startTimer(object : TimerTask() {
                            override fun run() {
                                //Log.d(TAG, "BTEST")
                                NotificationGenerator.notifyNotification(
                                    context, R.layout.custom_notification_recording
                                )
                            }
                        })
                        AudioRecorder.getInstance().startRecording()
                    }

                    RecordingState.ON_RECORDING, RecordingState.RESUME -> {

                        Log.d(TAG, "[리시버] 녹음 중지 상태")
                        NotificationGenerator.notifyNotification(
                            context, R.layout.custom_notification_pause
                        )
                        AudioTimer.pauseTimer()
                        AudioRecorder.getInstance().pauseRecording()
                    }

                    RecordingState.PAUSE -> {
                        val vibrator = context.getSystemService(VIBRATOR_SERVICE) as Vibrator
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(
                                    20,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                        }
                        //Log.d(TAG, "[리시버] 녹음 다시 시작")
                        AudioTimer.resumeTimer(object : TimerTask() {
                            override fun run() {
                               // Log.d(TAG, "PTEST")
                                NotificationGenerator.notifyNotification(
                                    context, R.layout.custom_notification_recording
                                )
                            }
                        })
                        AudioRecorder.getInstance().resumeRecording()
                    }
                }
            }

            ACTION_STOP -> {
                val vibrator = context.getSystemService(VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            20,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                }

                //Log.d(TAG, "[리시버] 녹음 저장")
                AudioTimer.stopTimer()
                NotificationGenerator.notifyNotification(context, R.layout.custom_notification)
                AudioRecorder.getInstance().stopRecording()
                context.stopService(Intent(context, AudioService::class.java))

            }

            ACTION_CANCEL -> {
                val vibrator = context.getSystemService(VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            20,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                }
                //Log.d(TAG, "[리시버] 녹음 삭제")
                AudioTimer.stopTimer()
                AudioRecorder.getInstance().cancelRecording()
                context.stopService(Intent(context, AudioService::class.java))
            }
            else -> {
                // Do nothing.
            }
        }
    }
}
