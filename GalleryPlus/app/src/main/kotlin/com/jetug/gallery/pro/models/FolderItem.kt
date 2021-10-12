package com.jetug.gallery.pro.models

import androidx.room.ColumnInfo
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.bumptech.glide.signature.ObjectKey
import com.jetug.commons.helpers.FAVORITES
import com.jetug.gallery.pro.helpers.RECYCLE_BIN

abstract class FolderItem(open var id: Long?,
                          open var path: String,
                          open var tmb: String,
                          open var name: String,
                          open var mediaCnt: Int,
                          open var modified: Long,
                          open var taken: Long,
                          open var size: Long,
                          open var location: Int,
                          open var types: Int,
                          open var sortValue: String,

                          @Ignore open var subfoldersCount: Int = 0,
                          @Ignore open var subfoldersMediaCount: Int = 0,
                          @Ignore open var containsMediaFilesDirectly: Boolean = true,) {

    fun areFavorites() = path == FAVORITES
    fun isRecycleBin() = path == RECYCLE_BIN
    fun getKey() = ObjectKey("$path-$modified")
}
