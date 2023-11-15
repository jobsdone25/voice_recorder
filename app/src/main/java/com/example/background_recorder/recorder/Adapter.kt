package com.example.background_recorder.recorder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.background_recorder.R



import java.text.SimpleDateFormat
import java.util.Date

class Adapter(var records:ArrayList<AudioRecord>,var listener: OnItemClickListener):RecyclerView.Adapter<Adapter.ViewHolder>() {
    private var editMode = false

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),View.OnClickListener, View.OnLongClickListener{
        var tvFilename : TextView = itemView.findViewById(R.id.tvFilename)
        var tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        var checkBox:CheckBox = itemView.findViewById(R.id.checkbox)

        init{
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }
        override fun onClick(v: View?) {
           val position = adapterPosition
            if(position != RecyclerView.NO_POSITION) listener.onItemClickListener(position)
        }

        override fun onLongClick(v: View?): Boolean {
            val position = adapterPosition
            if(position != RecyclerView.NO_POSITION) listener.onItemLongClickListener(position)
            return true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.itemview_layout,parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return records.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(position != RecyclerView.NO_POSITION){
            var record = records[position]

            var sdf = SimpleDateFormat("yyyy년 MM월 dd일 a h:mm")
            var date = Date(record.timestamp)
            var strDate = sdf.format(date)

            holder.tvFilename.text = record.filename
            holder.tvMeta.text = "${record.duration } $strDate"

            if(editMode) {
                holder.checkBox.visibility = View.VISIBLE
                holder.checkBox.isChecked = record.isChecked
            }else {
                holder.checkBox.visibility = View.GONE
                holder.checkBox.isChecked = false
            }
        }
    }

    fun setEditMode(mode: Boolean){
        if(editMode != mode) {
            editMode = mode
            notifyDataSetChanged()
        }
    }
    fun isEditMode():Boolean{
        return editMode
    }
}