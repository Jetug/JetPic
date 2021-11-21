package com.jetug.gallery.pro.extensions

import com.jetug.commons.helpers.SORT_DESCENDING

fun Int.isSortingAscending() = this and SORT_DESCENDING == 0
