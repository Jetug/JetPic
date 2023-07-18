package com.simplemobiletools.gallery.pro.data.databases.dao

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.simplemobiletools.gallery.pro.data.helpers.RECYCLE_BIN
import com.simplemobiletools.gallery.pro.data.models.Directory
import com.simplemobiletools.gallery.pro.data.models.tasks.SimpleTask

@Dao
interface TaskDao {
    @Query("SELECT *  FROM tasks")
    fun getAll(): List<SimpleTask>

    @Insert(onConflict = REPLACE)
    fun insert(task: SimpleTask)

    @Delete
    fun delete(task: SimpleTask)
}
