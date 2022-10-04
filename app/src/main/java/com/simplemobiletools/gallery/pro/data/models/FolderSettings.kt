package com.simplemobiletools.gallery.pro.data.models

import androidx.room.*

@Entity(tableName = "settings", indices = [Index(value = ["path"], unique = true)])
data class FolderSettings (
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    @ColumnInfo(name = "path"      ) var path: String = "",
    @ColumnInfo(name = "group"     ) var group: String = "",
    @ColumnInfo(name = "order"     ) var order: ArrayList<String> = arrayListOf(),
    @ColumnInfo(name = "sorting"   ) var sorting: Int = 0,
    @Ignore var pinned: Boolean = false,
    @Ignore var excluded: Boolean = false
) {
    constructor() : this(null, "", "", arrayListOf(), 0)

    fun addDirectoryData(directory: Directory){
        if(directory.path == path){
            group = directory.groupName
            if(directory.customSorting > 0)
                sorting = directory.customSorting
        }
    }
}
