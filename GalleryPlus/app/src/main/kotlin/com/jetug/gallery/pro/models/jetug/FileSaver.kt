package com.jetug.gallery.pro.models.jetug

import com.jetug.gallery.pro.models.Medium
import com.jetug.gallery.pro.models.ThumbnailItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

const val SORTING_FILE_NAME = "Sort.txt"

fun saveImagePositions(medias:ArrayList<Medium>){
    if(medias.isNotEmpty()) {
        val directoryPath: String = medias[0].parentPath
        val sortingFile = File(File(directoryPath), SORTING_FILE_NAME)

        if (!sortingFile.exists()) {
            sortingFile.createNewFile()
        }

        CoroutineScope(Dispatchers.IO).launch {
            var text = ""
            for (file in medias) {
                text += file.name
                text += "\n"
            }
            sortingFile.printWriter().use {
                it.println(text)
            }
        }
    }
}

fun getCustomMediaList(source: ArrayList<Medium>):ArrayList<Medium>{
    if(source.isNotEmpty()) {
        val directoryPath = source[0].parentPath
        val customSortingFile = File(File(directoryPath), SORTING_FILE_NAME)
        val files = arrayListOf<String>()
        val newOrdered = ArrayList<Medium>()
        val medias = source.clone() as ArrayList<Medium>

        if (customSortingFile.exists()) {
            customSortingFile.bufferedReader().forEachLine {
                if (it != "" && File(directoryPath, it).exists()) {
                    val file = it
                    files.add(file)
                }
            }

            files.forEach { path ->
                val index = medias.indexOfFirst { it.path == path }
                if (index != -1) {
                    val dir = medias.removeAt(index)
                    newOrdered.add(dir)
                }
            }
            medias.mapTo(newOrdered, { it })

            return newOrdered
        }
    }
    return source
}
