package com.example.background_recorder.recorder

import java.util.Timer
import java.util.TimerTask


object AudioTimer {
    private var startTimeStamp: Long = 0L
    private var duration: Long = 0L
    private var timer = Timer()
    private var currentRecordingDuration: String = ""

    fun startTimer(timerTask: TimerTask) {
        startTimeStamp = System.currentTimeMillis()
        duration = 0L
        timer = Timer()
        timer.schedule(timerTask, 1L, 40L)
    }

    fun pauseTimer() {
        duration = System.currentTimeMillis() - startTimeStamp
        timer.cancel()
    }

   fun resumeTimer(timerTask: TimerTask) {
        startTimeStamp = System.currentTimeMillis() - duration
        timer = Timer()
        timer.schedule(timerTask, 1L, 40L)
    }

    fun stopTimer() {
        startTimeStamp = 0L
        duration = 0L
        timer.cancel()
    }

    fun getTimeStamp(): String {
        if (startTimeStamp == 0L) {
            return "00:00:00"
        }

        val currentTimeStamp = System.currentTimeMillis()
        val milisecond = ((currentTimeStamp - startTimeStamp) %1000/ 10).toInt()
        val countTimeSeconds = ((currentTimeStamp - startTimeStamp) / 1000L).toInt()
        val hour = countTimeSeconds / 3600
        val minute = (countTimeSeconds % 3600) / 60
        val second = countTimeSeconds % 60
        var duration : String
        if(hour>0) duration = "%02d:%02d:%02d".format(hour, minute, second)
        else{
             duration = "%02d:%02d.%02d".format(minute, second,milisecond)
        }

        currentRecordingDuration = duration
        return "%02d:%02d:%02d".format(hour, minute, second)
    }
    fun getCurrentRecordingDuration(): String {
        return currentRecordingDuration
    }
}
