package com.example.background_recorder.recorder

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AudioRecordDao {
    @Query("SELECT * FROM audioRecords")
    fun getAll(): List<AudioRecord>
    @Query("SELECT * FROM audioRecords WHERE filename LIKE :query")
    fun searchDatabase(query: String): List<AudioRecord>

    @Insert
    fun insert(vararg audioRecord: AudioRecord)

    @Delete
    fun delete(audioRecords: Array<AudioRecord>)
    @Delete
    fun delete(audioRecord: AudioRecord)
    @Delete
    fun update(audioRecords: Array<AudioRecord>)

    @Update
    fun update(audioRecord: AudioRecord)
}