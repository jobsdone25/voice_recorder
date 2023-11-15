package com.example.background_recorder.recorder

import android.Manifest
import android.Manifest.permission.RECORD_AUDIO
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.background_recorder.R
import com.example.background_recorder.databinding.ActivityRecorderMainBinding
import com.example.background_recorder.recorder.AudioReceiver.Companion.TAG
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.util.Date

const val REQUEST_CODE = 200

class RecorderMainActivity : AppCompatActivity(), OnRecordingStateChangeListener {
    private lateinit var amplitudes: ArrayList<Float>
    private var permissions = arrayOf(RECORD_AUDIO)
    private lateinit var binding: ActivityRecorderMainBinding
    private var duration = ""
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var vibrator: Vibrator
    private lateinit var btnCancel: Button
    private lateinit var btnOk: Button
    private lateinit var filenameInput: EditText
    private lateinit var db: AppDatabase
    private val handler = Handler(Looper.getMainLooper())

    private val updateUITask: Runnable = object : Runnable {
        override fun run() {
            // UI 업데이트 작업 수행
            val currentRecordingDuration = AudioTimer.getCurrentRecordingDuration()
            // 예: TextView 등을 사용하여 UI 업데이트
            binding.tvTimer.text = currentRecordingDuration
            duration = currentRecordingDuration
            if (audioRecorder.recorder != null && (audioRecorder.recordingState == RecordingState.ON_RECORDING || audioRecorder.recordingState == RecordingState.RESUME))
                binding.waveformView.addAmplitude(audioRecorder.recorder!!.maxAmplitude.toFloat())
            // 주기적으로 업데이트
            handler.postDelayed(this, 40) // 1초마다 업데이트
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecorderMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        Log.d(TAG,"ONCREATE")
        val functionName = intent.getStringExtra("EXTRA_FUNCTION_TO_CALL")
        Log.d(TAG, functionName.toString())

        audioRecorder = AudioRecorder.getInstance()
        audioRecorder.dirPath = "${externalCacheDir?.absolutePath}/"
        audioRecorder.permissionGranted = (ActivityCompat.checkSelfPermission(
            this,
            permissions[0]
        ) == PackageManager.PERMISSION_GRANTED)
        if (!audioRecorder.permissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
        }
        audioRecorder.setOnRecordingStateChangeListener(this)

        //알림설정
        alarmPermission()
        //초기화 해주기
        val bottomSheet = findViewById<LinearLayout>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        btnCancel = findViewById<Button>(R.id.btnCancel)
        btnOk = findViewById<Button>(R.id.btnOk)

        filenameInput = findViewById<EditText>(R.id.filenameInput)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        db = Room.databaseBuilder(
            this, AppDatabase::class.java, "audioRecords"
        ).build()

        binding.btnRecord.setOnClickListener {
            when (audioRecorder.recordingState) {
                RecordingState.PAUSE -> resumeRecorder()
                RecordingState.ON_RECORDING, RecordingState.RESUME -> pauseRecorder()
                RecordingState.BEFORE_RECORDING -> startRecording()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        50,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            }
        }

        binding.btnList.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        binding.btnDone.setOnClickListener {
            stopRecorder()
            Toast.makeText(this, "Record saved", Toast.LENGTH_SHORT).show()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.bottomSheetBG.visibility = View.VISIBLE
            filenameInput.setText(audioRecorder.filename)
        }

        //bottom sheet view 버튼 설정
        btnCancel.setOnClickListener {
            File("${audioRecorder.dirPath}${audioRecorder.filename}.mp3").delete()
            dismiss()
        }

        btnOk.setOnClickListener {
            save()
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            stopRecorder()
            File("${audioRecorder.dirPath}${audioRecorder.filename}.mp3")
            Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show()
        }
        binding.btnDelete.isClickable = false

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val functionName = intent.getStringExtra("EXTRA_FUNCTION_TO_CALL")
        Log.d(TAG, functionName.toString())
        if (functionName == "save") {
            try {
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            20,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                }

                val temp = binding.tvTimer.text.toString()
                stopRecorder()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                binding.bottomSheetBG.visibility = View.VISIBLE
                filenameInput.setText(audioRecorder.filename)
                duration=temp

            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(updateUITask)
    }

    @Override
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent!!)
    }
    override fun onResume() {
        super.onResume()
        audioRecorder = AudioRecorder.getInstance()
        if (audioRecorder.recordingState == RecordingState.ON_RECORDING || audioRecorder.recordingState == RecordingState.RESUME) {
            handler.post(updateUITask)
            binding.btnRecord.setImageResource(R.drawable.baseline_pause_24)
            binding.btnDelete.isClickable = true
            binding.btnDelete.setImageResource(R.drawable.baseline_clear_24)
            binding.btnList.visibility = View.GONE
            binding.btnDone.visibility = View.VISIBLE
        }
        if (audioRecorder.recordingState == RecordingState.PAUSE) {
            handler.post(updateUITask)
            binding.btnRecord.setImageResource(R.drawable.baseline_record)
            binding.btnDelete.isClickable = true
            binding.btnDelete.setImageResource(R.drawable.baseline_clear_24)
            binding.btnList.visibility = View.GONE
            binding.btnDone.visibility = View.VISIBLE
        }
    }

    private fun save() {
        val newFilename = filenameInput.text.toString()
        if (newFilename != audioRecorder.filename) {
            var newFile = File("${audioRecorder.dirPath}$newFilename.mp3")
            File("${audioRecorder.dirPath}${audioRecorder.filename}.mp3").renameTo(newFile)
        }

        var filePath = "${audioRecorder.dirPath}$newFilename.mp3"
        var timestamp = Date().time
        var ampsPath = "${audioRecorder.dirPath}$newFilename"

        try {
            var fos = FileOutputStream(ampsPath)
            var out = ObjectOutputStream(fos)
            out.writeObject(amplitudes)
            fos.close()
            out.close()
        } catch (e: IOException) {
        }
        if(duration.length==8) duration = "00:"+duration
        var record = AudioRecord(newFilename, filePath, timestamp, duration.dropLast(3), ampsPath)

        GlobalScope.launch {
            db.audioRecorDao().insert(record)
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            audioRecorder.permissionGranted = (grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }
    }

    private fun startRecording() {
        val intent = Intent(applicationContext, AudioService::class.java)
        startService(intent)
        handler.post(updateUITask)
        Intent(this, AudioReceiver::class.java)
            .apply {
                action = AudioReceiver.ACTION_RECORD
                sendBroadcast(this)
            }
    }

    private fun dismiss() {
        binding.bottomSheetBG.visibility = View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        hideKeyboard(filenameInput)
    }

    private fun stopRecorder() {
        AudioRecorder.getInstance().stopRecording()
        AudioTimer.stopTimer()
        val intent = Intent(applicationContext, AudioService::class.java)
        stopService(intent)
        handler.removeCallbacks(updateUITask)
        binding.tvTimer.text = "00:00:00"

    }

    private fun pauseRecorder() {
        Intent(this, AudioReceiver::class.java)
            .apply {
                action = AudioReceiver.ACTION_RECORD
                sendBroadcast(this)
            }
    }

    private fun resumeRecorder() {
        Intent(this, AudioReceiver::class.java)
            .apply {
                action = AudioReceiver.ACTION_RECORD
                sendBroadcast(this)
            }
    }

    private fun alarmPermission() {
        val isTiramisuOrHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
        var hasNotificationPermission =
            if (isTiramisuOrHigher)
                ContextCompat.checkSelfPermission(
                    this,
                    notificationPermission
                ) == PackageManager.PERMISSION_GRANTED
            else true

        val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            hasNotificationPermission = it

        }
        if (!hasNotificationPermission) {
            launcher.launch(notificationPermission)
        }
    }

    override fun onRecordingStateChanged(newState: RecordingState) {
        runOnUiThread {
            when (newState) {
                RecordingState.BEFORE_RECORDING -> {
                    handler.removeCallbacks(updateUITask)
                    binding.btnList.visibility = View.VISIBLE
                    binding.btnDone.visibility = View.GONE
                    binding.btnDelete.isClickable = false
                    binding.btnDelete.setImageResource(R.drawable.baseline_clear_disabled_24)
                    binding.btnRecord.setImageResource(R.drawable.baseline_record)
                    binding.tvTimer.text = "00:00:00"
                    binding.waveformView.clear()
                    amplitudes = binding.waveformView.clear()
                }

                RecordingState.PAUSE -> {
                    binding.btnRecord.setImageResource(R.drawable.baseline_record)
                    NotificationGenerator.notifyNotification(
                        this, R.layout.custom_notification_pause
                    )
                }

                RecordingState.RESUME -> {
                    binding.btnRecord.setImageResource(R.drawable.baseline_pause_24)
                }

                RecordingState.ON_RECORDING -> {
                    binding.btnRecord.setImageResource(R.drawable.baseline_pause_24)
                    binding.btnDelete.isClickable = true
                    binding.btnDelete.setImageResource(R.drawable.baseline_clear_24)
                    binding.btnList.visibility = View.GONE
                    binding.btnDone.visibility = View.VISIBLE
                }
            }
        }
    }
}