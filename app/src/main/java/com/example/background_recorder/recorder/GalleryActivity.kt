package com.example.background_recorder.recorder

import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.background_recorder.R
import com.example.background_recorder.databinding.ActivityGalleryBinding
import com.example.background_recorder.recorder.AudioReceiver.Companion.TAG
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.NumberFormat


class GalleryActivity : AppCompatActivity() , OnItemClickListener {
    private lateinit var runnable: Runnable
    private lateinit var handler : Handler
    private var delay = 10L
    private var jumpValue = 1000
    private var playbackSpeed = 1.0f
    private  lateinit var records : ArrayList<AudioRecord>
    private lateinit var mAdapter: Adapter
    private lateinit var db : AppDatabase
    private lateinit var mediaPlayer :MediaPlayer
    private var allChecked = false
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var binding : ActivityGalleryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())


        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        binding.toolbar.setNavigationIconTint(Color.BLACK)
        records = ArrayList()


        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        db = Room.databaseBuilder(
            this,AppDatabase::class.java, "audioRecords").build()


        mAdapter = Adapter(records,this)
        binding.recyclerview.apply{
            adapter = mAdapter
            layoutManager = LinearLayoutManager(context)
        }

        fetchAll()

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                var query = p0.toString()
                searchDatabase("%$query%")
            }
        })


        binding.btnClose.setOnClickListener {
            leaveEditMode()
            hideBottomSheet()
        }

        binding.btnSelectAll.setOnClickListener {
            allChecked = !allChecked
            records.map{it.isChecked = allChecked}
            mAdapter.notifyDataSetChanged()

            if(allChecked){
                disableRename()
                enableDelete()
            }
            else{
                disableDelete()
                disableRename()
            }
        }

        binding.btnOut.setOnClickListener {
            val record = records.filter { it.isChecked }.get(0)
            Log.d(TAG, record.filePath)
            val playIntent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.parse("content://com.example.mysololife${record.filePath}")
            Log.d(TAG, uri.toString())
            playIntent.setDataAndType(uri, "audio/mp3")
            playIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(playIntent)
        }

        binding.btnDelete.setOnClickListener {
            val builder = AlertDialog.Builder(this)

            builder.setTitle("강의 녹음 삭제")
            val nbRecords = records.count({it.isChecked})
            builder.setMessage("${nbRecords}개의 강의 녹음본을 삭제하시겠습니까?")

            builder.setPositiveButton("삭제"){_,_->
                val toDelete = records.filter{it.isChecked}.toTypedArray()
                GlobalScope.launch {
                    db.audioRecorDao().delete(toDelete)
                    runOnUiThread {
                        records.removeAll(toDelete)
                        mAdapter.notifyDataSetChanged()
                        leaveEditMode()
                        hideBottomSheet()
                    }
                }
            }
            builder.setNegativeButton("취소"){_,_->
            }
            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun hideBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.peekHeight = 0
        binding.recordConstraintLayout.visibility = View.VISIBLE
    }


    private fun fetchAll(){
        GlobalScope.launch {
            records.clear()
            var queryResult = db.audioRecorDao().getAll()
            records.addAll(queryResult)

            mAdapter.notifyDataSetChanged()
        }

        binding.btnEdit.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val inflater = LayoutInflater.from(this)
            val dialogView = inflater.inflate(R.layout.rename_layout,null)
            builder.setView(dialogView)
            val dialog = builder.create()

            val record = records.filter{it.isChecked}.get(0)
            val textInput = dialogView.findViewById<TextInputEditText>(R.id.filenameInput)
            textInput.setText(record.filename)
            builder.show()
            dialogView.findViewById<Button>(R.id.btnSave).setOnClickListener {
                val input = textInput.text.toString()
                if(input.isEmpty()){
                    Toast.makeText(this,"변경할 이름을 작성하세요.",Toast.LENGTH_SHORT).show()
                }
                else{
                    record.filename = input
                    GlobalScope.launch {
                        db.audioRecorDao().update(record)
                        runOnUiThread {
                            mAdapter.notifyItemChanged(records.indexOf(record))
                            dialog.dismiss()
                            onBackPressed()
                            leaveEditMode()
                            hideBottomSheet()
                        }
                    }
                }
            }

            val btnCanCel = dialogView.findViewById<Button>(R.id.btnCancel)

           btnCanCel.setOnClickListener {
               dialog.dismiss()
               onBackPressed()
           }

        }
    }

    override fun onItemClickListener(position: Int) {
        var audioRecord = records[position]

        if (mAdapter.isEditMode()) {
            records[position].isChecked = !records[position].isChecked
            mAdapter.notifyItemChanged(position)

            var nbSelected = records.count{it.isChecked}
                when(nbSelected) {
                    0 ->{
                        disableRename()
                        disableDelete()
                    }
                    1 ->{
                        enableRename()
                        enableDelete()
                    }
                    else ->{
                        disableRename()
                        enableDelete()
                    }
                }

        } else {
            if (::mediaPlayer.isInitialized && mediaPlayer != null && mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            binding.chip.text = "x 1.0"
            val filePath = audioRecord.filePath
            val fileName = audioRecord.filename
            binding.tvFilename.text = fileName
            mediaPlayer = MediaPlayer()
            try {
                mediaPlayer.setDataSource(filePath)
                mediaPlayer.prepare()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            handler = Handler(Looper.getMainLooper())
            runnable = Runnable {
                binding.seekbar.progress = mediaPlayer.currentPosition
                handler.postDelayed(runnable,delay)
                binding.tvTrackProgress.text = dateFormat(mediaPlayer.currentPosition)
            }
            mediaPlayer.setOnCompletionListener {
                binding.btnPlay.background = ResourcesCompat.getDrawable(resources,
                    R.drawable.ic_play_circle,theme)
                handler.removeCallbacks(runnable)
            }
            playPausePlayer()
            recordInit()
        }
    }

    private fun recordInit() {
        binding.tvTrackDuration.text = dateFormat(mediaPlayer.duration)
        binding.btnPlay.setOnClickListener{
            playPausePlayer()
        }

        binding.seekbar.max = mediaPlayer.duration


        binding.btnforward.setOnClickListener {
            mediaPlayer.seekTo(mediaPlayer.currentPosition+jumpValue)
            binding.seekbar.progress += jumpValue
        }
        binding.btnBackward.setOnClickListener {
            mediaPlayer.seekTo(mediaPlayer.currentPosition-jumpValue)
            binding.seekbar.progress -= jumpValue
        }
        binding.chip.setOnClickListener{
            if(playbackSpeed != 2f)
                playbackSpeed += 0.25f
            else
                playbackSpeed = 0.5f

            mediaPlayer.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
            binding.chip.text = "x $playbackSpeed"
        }

        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser)
                    mediaPlayer.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?){}

            override fun onStopTrackingTouch(seekBar: SeekBar?){}
        })
    }

    private fun playPausePlayer() {
        if(!mediaPlayer.isPlaying){
            mediaPlayer.start()
            binding.btnPlay.background = ResourcesCompat.getDrawable(resources,
                R.drawable.ic_pause_circle,theme)
            handler.postDelayed(runnable,delay)
        }else{
            mediaPlayer.pause()
            binding.btnPlay.background = ResourcesCompat.getDrawable(resources,
                R.drawable.ic_play_circle,theme)
            handler.removeCallbacks(runnable)
        }
    }

    private fun dateFormat(duration:Int):String{
        val d= duration/1000
        val s = d%60
        val m = (d/60 % 60)
        val h = ((d - m*60)/3600)
        Log.d("testplease",s.toString()+" "+m.toString()+" "+h.toString())
        val f : NumberFormat = DecimalFormat("00")
        var str = "$m:${f.format(s)}"
        if(h>0)
            str = "$h:$str"
        return str
    }

    private fun searchDatabase(query: String){
        GlobalScope.launch {
            records.clear()
            var queryResult = db.audioRecorDao().searchDatabase("%$query%")
            records.addAll(queryResult)

            runOnUiThread{
            mAdapter.notifyDataSetChanged()
        }}
    }

    private fun leaveEditMode(){
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        // show relative layout
        binding.editBar.visibility = View.GONE
        records.map{it.isChecked = false}
        mAdapter.setEditMode(false)
    }
    private fun disableRename(){
        binding.btnEdit.isClickable = false
        binding.btnEdit.backgroundTintList = ResourcesCompat.getColorStateList(resources,
            R.color.grayDarkDisabled,theme)
        binding.tvEdit.setTextColor(ResourcesCompat.getColorStateList(resources,
            R.color.grayDarkDisabled,theme))
    }

    private fun disableDelete(){
        binding.btnDelete.isClickable = false
        binding.btnDelete.backgroundTintList = ResourcesCompat.getColorStateList(resources,
            R.color.grayDarkDisabled,theme)
        binding.tvDelete.setTextColor(ResourcesCompat.getColorStateList(resources,
            R.color.grayDarkDisabled,theme))
    }

    private fun enableRename(){
        binding.btnEdit.isClickable = true
        binding.btnEdit.backgroundTintList = ResourcesCompat.getColorStateList(resources,
            R.color.grayDark,theme)
        binding.tvEdit.setTextColor(ResourcesCompat.getColorStateList(resources,
            R.color.grayDark,theme))
    }

    private fun enableDelete(){
        binding.btnDelete.isClickable = true
        binding.btnDelete.backgroundTintList = ResourcesCompat.getColorStateList(resources,
            R.color.grayDark,theme)
        binding.tvDelete.setTextColor(ResourcesCompat.getColorStateList(resources,
            R.color.grayDark,theme))
    }

    override fun onItemLongClickListener(position: Int) {
        mAdapter.setEditMode(true)
        binding.recordConstraintLayout.visibility = View.GONE
        if (::mediaPlayer.isInitialized && mediaPlayer != null && mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        records[position].isChecked = !records[position].isChecked
        mAdapter.notifyItemChanged(position)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        if(mAdapter.isEditMode() && binding.editBar.visibility == View.GONE){
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowHomeEnabled(false)
            binding.editBar.visibility = View.VISIBLE

            enableRename()
            enableDelete()
            enableOutApp()
        }
    }

    private fun enableOutApp() {
    }
}