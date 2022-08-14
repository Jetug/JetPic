package com.simplemobiletools.gallery.pro.data.databases.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simplemobiletools.gallery.pro.data.models.FolderSettings

@Dao
interface FolderSettingsDao {
    @Query("SELECT * FROM settings")
    fun getAll(): List<FolderSettings>

    @Query("SELECT * FROM settings WHERE path = :path")
    fun getByPath(path: String): FolderSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(settings: FolderSettings)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(settings: List<FolderSettings>)
}

