package com.simplemobiletools.gallery.pro.data.models.tasks

import androidx.room.*

@Entity(tableName = "tasks")
class SimpleTask(
    @PrimaryKey(autoGenerate = true)  val id: Int,
    @ColumnInfo(name = "name")        val name: String,
    @ColumnInfo(name = "source_path") val sourcePath: String,
    @ColumnInfo(name = "target_path") val targetPath: String
) : AbstractTask()
