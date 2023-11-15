package com.example.background_recorder.recorder
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.example.background_recorder.R
import com.example.background_recorder.recorder.AudioReceiver.Companion.TAG

class AudioService : Service() {
    private var audioReceiver: AudioReceiver? = null
    private var audioRecorder: AudioRecorder? = null
    companion object {
        private const val NOTIFICATION_ID = 9999
    }
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG,"서비스 진입")
        if (audioReceiver == null) {
            audioReceiver = AudioReceiver()
            val filter = IntentFilter()
            registerReceiver(audioReceiver, filter)
        }
        audioRecorder = AudioRecorder.getInstance()
        val notification = NotificationGenerator.generateNotification(
            this, R.layout.custom_notification_recording
        )
        startForeground(NOTIFICATION_ID, notification)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_REDELIVER_INTENT
    }
    override fun onDestroy() {
        super.onDestroy()
        if (audioRecorder?.recordingState != RecordingState.BEFORE_RECORDING) {
            audioRecorder?.stopRecording()
            AudioTimer.stopTimer()
        }
        audioReceiver?.let {
            unregisterReceiver(it)
        }
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}