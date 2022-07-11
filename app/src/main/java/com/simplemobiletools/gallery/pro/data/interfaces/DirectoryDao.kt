package com.simplemobiletools.gallery.pro.data.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.simplemobiletools.gallery.pro.data.helpers.RECYCLE_BIN
import com.simplemobiletools.gallery.pro.data.models.Directory

@Dao
interface DirectoryDao {
    @Query("SELECT *  FROM directories")
    fun getAll(): List<Directory>

    @Query("SELECT *  FROM directories WHERE path = :path")
    fun get(path: String): List<Directory>

    @Insert(onConflict = REPLACE)
    fun insert(directory: Directory)

    @Insert(onConflict = REPLACE)
    fun insertAll(directories: List<Directory>)

    @Query("DELETE FROM directories WHERE path = :path COLLATE NOCASE")
    fun deleteDirPath(path: String)

    //Jet
    @Update
    fun update(directory: Directory)

    @Query("UPDATE OR REPLACE directories SET thumbnail = :thumbnail, media_count = :mediaCnt, last_modified = :lastModified, date_taken = :dateTaken, size = :size, media_types = :mediaTypes, custom_sorting = :customSorting, group_name = :groupName WHERE path = :path COLLATE NOCASE")
    fun updateDirectory(path: String, thumbnail: String, mediaCnt: Int, lastModified: Long, dateTaken: Long, size: Long, mediaTypes: Int, customSorting: Int, groupName: String)

    @Query("UPDATE directories SET thumbnail = :thumbnail, filename = :name, path = :newPath WHERE path = :oldPath COLLATE NOCASE")
    fun updateDirectoryAfterRename(thumbnail: String, name: String, newPath: String, oldPath: String)

    @Query("DELETE FROM directories WHERE path = \'$RECYCLE_BIN\' COLLATE NOCASE")
    fun deleteRecycleBin()

    @Query("SELECT thumbnail FROM directories WHERE path = :path")
    fun getDirectoryThumbnail(path: String): String?
}
