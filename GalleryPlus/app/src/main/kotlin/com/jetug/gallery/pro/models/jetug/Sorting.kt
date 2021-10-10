package com.jetug.gallery.pro.models.jetug

import com.jetug.commons.helpers.*
import com.jetug.gallery.pro.models.Medium

fun sortMedia(media: ArrayList<Medium>, sorting: Int): ArrayList<Medium> {
    if (sorting and SORT_BY_RANDOM != 0) {
        media.shuffle()
        return media
    }

    media.sortWith { o1, o2 ->
        o1 as Medium
        o2 as Medium
        var result = when {
            sorting and SORT_BY_NAME != 0 -> {
                if (sorting and SORT_USE_NUMERIC_VALUE != 0) {
                    AlphanumericComparator().compare(o1.name.toLowerCase(), o2.name.toLowerCase())
                } else {
                    o1.name.toLowerCase().compareTo(o2.name.toLowerCase())
                }
            }
            sorting and SORT_BY_PATH != 0 -> {
                if (sorting and SORT_USE_NUMERIC_VALUE != 0) {
                    AlphanumericComparator().compare(o1.path.toLowerCase(), o2.path.toLowerCase())
                } else {
                    o1.path.toLowerCase().compareTo(o2.path.toLowerCase())
                }
            }
            sorting and SORT_BY_SIZE != 0 -> o1.size.compareTo(o2.size)
            sorting and SORT_BY_DATE_MODIFIED != 0 -> o1.modified.compareTo(o2.modified)
            else -> o1.taken.compareTo(o2.taken)
        }

        if (sorting and SORT_DESCENDING != 0) {
            result *= -1
        }
        result
    }

    return media
}
