package com.simplemobiletools.gallery.pro.data.jetug

import android.app.Activity
import android.content.Context
import com.google.android.exoplayer2.util.Log
import com.simplemobiletools.commons.extensions.launchViewIntent
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.GITHUB_LINK
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.extensions.launchIO
import com.simplemobiletools.gallery.pro.data.helpers.JET
import com.simplemobiletools.gallery.pro.data.helpers.khttp.post
import com.simplemobiletools.gallery.pro.data.helpers.khttp.structures.files.FileLike
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

const val searchUrl = "https://yandex.ru/images/search"

fun Activity.searchInYandex(filePath: String) = launchIO{
    try {
        toast(R.string.loading)
        val file = File(filePath)
        val fileLike = FileLike("upfile", file.name, file.readBytes())

        val values = mapOf(
            "rpt" to "imageview",
            "format" to "json",
            "request" to "{\"blocks\":[{\"block\":\"b-page_type_search-by-image__link\"}]}",
        )

        val result = post(
            url = searchUrl,
            params = values,
            files = listOf(fileLike)
        )

        val obj: JSONObject = result.jsonObject
        val url = (((obj["blocks"] as JSONArray)[0] as JSONObject)["params"] as JSONObject)["url"]
        val resultUrl = "$searchUrl?$url"

        launchViewIntent(resultUrl)
    }
    catch (e: Exception) {
        showErrorToast(e)
        Log.e(JET, e.message, e)
    }
}
