package com.jetug.gallery.pro.models

import android.content.Context
import androidx.room.*
import com.bumptech.glide.signature.ObjectKey
import com.jetug.commons.extensions.formatDate
import com.jetug.commons.extensions.formatSize
import com.jetug.commons.helpers.*
import com.jetug.gallery.pro.helpers.RECYCLE_BIN

@Entity(tableName = "directories", indices = [Index(value = ["path"], unique = true)])
data class Directory(
    @PrimaryKey(autoGenerate = true) override var id: Long?,
    @ColumnInfo(name = "path") override var path: String,
    @ColumnInfo(name = "thumbnail") override var tmb: String,
    @ColumnInfo(name = "filename") override var name: String,
    @ColumnInfo(name = "media_count") override var mediaCnt: Int,
    @ColumnInfo(name = "last_modified") override var modified: Long,
    @ColumnInfo(name = "date_taken") override var taken: Long,
    @ColumnInfo(name = "size") override var size: Long,
    @ColumnInfo(name = "location") override var location: Int,
    @ColumnInfo(name = "media_types") override var types: Int,
    @ColumnInfo(name = "sort_value") override var sortValue: String,

    // used with "Group direct subfolders" enabled
    @Ignore override var subfoldersCount: Int = 0,
    @Ignore override var subfoldersMediaCount: Int = 0,
    @Ignore override var containsMediaFilesDirectly: Boolean = true,

    @ColumnInfo(name = "group_name") var groupName: String = "") : FolderItem(id, path, tmb, name, mediaCnt, modified, taken, size, location, types, sortValue) {

    constructor() : this(null, "", "", "", 0, 0L, 0L, 0L,
        0, 0, "", 0, 0, true
    )
}
