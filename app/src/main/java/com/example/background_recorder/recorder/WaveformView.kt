package com.example.background_recorder.recorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.example.background_recorder.R
import com.example.background_recorder.recorder.AudioReceiver.Companion.TAG

class WaveformView(context: Context?, attrs: AttributeSet?) : View(context, attrs){

    private var paint = Paint()
    private val amplitudes = ArrayList<Float>()
    private val spikes = ArrayList<RectF>()
    private var radius = 6f
    private var w = 9f
    private var d = 6f
    private var sw = 0f
    private var sh = 600f

    private var maxSpikes = 0
    init{
        paint.color = ContextCompat.getColor(context!!, R.color.maincolor_seven)
        sw = resources.displayMetrics.widthPixels.toFloat()
        maxSpikes = (sw/(w+d)).toInt()
    }

    fun addAmplitude(amp: Float) {
        var norm = Math.min(amp.toInt()/30,600).toFloat()
        amplitudes.add(norm)

        spikes.clear()
        var amps = amplitudes.takeLast(maxSpikes)
        for (i in amps.indices) {
            var left = sw - i*(w+d)
            var top = sh/2-amps[i]/2
            var right = left + w
            var bottom = top+amps[i]
            spikes.add(RectF(left, top, right, bottom))
        }

        invalidate()
    }

    fun clear():ArrayList<Float>{
        var amps = amplitudes.clone() as ArrayList<Float>
        amplitudes.clear()
        spikes.clear()
        invalidate()
        return amps
    }
    private fun updateSpikes() {
        spikes.clear()  // 이전에 저장된 spikes를 지웁니다.

        // 최근에 추가된 amplitudes를 가져옵니다.
        var amps = amplitudes.takeLast(maxSpikes)

        // 각 amplitude에 대해 RectF를 생성하여 spikes 리스트에 추가합니다.
        for (i in amps.indices) {
            Log.d(TAG,i.toString())
            var left = sw - i * (w + d)
            var top = sh / 2 - amps[i] / 2
            var right = left + w
            var bottom = top + amps[i]
            spikes.add(RectF(left, top, right, bottom))
        }
    }
    fun setAmplitudes(amps: ArrayList<Float>) {
        amplitudes.clear()
        amplitudes.addAll(amps)
        updateSpikes()
        invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        spikes.forEach {
            canvas?.drawRoundRect(it, radius, radius, paint)
        }
    }
}