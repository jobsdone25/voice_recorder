package com.example.background_recorder.recorder

import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

interface OnRecordingStateChangeListener {
    fun onRecordingStateChanged(newState: RecordingState)
}

class AudioRecorder private constructor() : AppCompatActivity() {
    var recorder: MediaRecorder? = null
    var filename: String = ""
    var recordingState = RecordingState.BEFORE_RECORDING
    var permissionGranted = false
    var dirPath = ""
    private var recordingStateChangeListener: OnRecordingStateChangeListener? = null

    companion object {
        private var instance: AudioRecorder? = null
        fun getInstance(): AudioRecorder {
            if (instance == null) {
                instance = AudioRecorder()
            }
            return instance!!
        }
    }

    fun setOnRecordingStateChangeListener(listener: OnRecordingStateChangeListener) {
        recordingStateChangeListener = listener
    }


    fun startRecording() {
        recorder = MediaRecorder()
        Log.e("RecordTest", dirPath)
        var simpleDateFormat = SimpleDateFormat("[MM월 dd일]")
        var date = simpleDateFormat.format(Date())
        filename = "과목 이름_$date"

        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$filename.mp3")
            try {
                prepare()
            } catch (e: IOException) {
                Log.e("startRecoring()", "prepare() failed")
            }
            start()
        }
        recordingState = RecordingState.ON_RECORDING
        recordingStateChangeListener?.onRecordingStateChanged(recordingState)
    }

    fun resumeRecording() {
        recorder?.resume()
        recordingState = RecordingState.ON_RECORDING
        recordingStateChangeListener?.onRecordingStateChanged(recordingState)
    }

    fun pauseRecording() {
        recorder?.pause()
        recordingState = RecordingState.PAUSE
        recordingStateChangeListener?.onRecordingStateChanged(recordingState)
    }

    fun stopRecording() {
        recorder?.run {
            stop()
            reset()
            release()
        }

        recordingState = RecordingState.BEFORE_RECORDING
        recorder = null
        recordingStateChangeListener?.onRecordingStateChanged(recordingState)
    }
    fun cancelRecording() {
        stopRecording()
        File("$dirPath$filename.mp3").delete()
        recordingStateChangeListener?.onRecordingStateChanged(recordingState)
    }

}