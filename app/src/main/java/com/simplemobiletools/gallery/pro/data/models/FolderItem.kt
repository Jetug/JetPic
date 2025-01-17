package com.simplemobiletools.gallery.pro.data.models

import android.content.Context
import androidx.room.Ignore
import com.bumptech.glide.signature.ObjectKey
import com.simplemobiletools.commons.extensions.formatDate
import com.simplemobiletools.commons.extensions.formatSize
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.gallery.pro.data.helpers.RECYCLE_BIN

abstract class FolderItem(open var id: Long?,
                          open var path: String,
                          open var tmb: String,
                          open var name: String,
                          open var mediaCnt: Int,
                          open var modified: Long,
                          open var taken: Long,
                          open var size: Long,
                          open var location: Int,

                          @Ignore open var subfoldersCount: Int = 0,
                          @Ignore open var subfoldersMediaCount: Int = 0,
                          @Ignore open var containsMediaFilesDirectly: Boolean = true
) {


    @Ignore var isHidden: Boolean = false

    fun areFavorites() = path == FAVORITES
    fun isRecycleBin() = path == RECYCLE_BIN
    fun getKey() = ObjectKey("$path-$modified")

    fun getBubbleText(sorting: Int, context: Context, dateFormat: String? = null, timeFormat: String? = null) = when {
        sorting and SORT_BY_NAME != 0 -> name
        sorting and SORT_BY_PATH != 0 -> path
        sorting and SORT_BY_SIZE != 0 -> size.formatSize()
        sorting and SORT_BY_DATE_MODIFIED != 0 -> modified.formatDate(context, dateFormat, timeFormat)
        else -> taken.formatDate(context)
    }
}
