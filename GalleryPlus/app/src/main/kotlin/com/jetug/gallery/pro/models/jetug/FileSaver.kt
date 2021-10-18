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

fun getCustomMediaList(source: ArrayList<Medium>): ArrayList<Medium>{
    if (source.isEmpty())
        return source

    val directoryPath = source[0].parentPath
    val customSortingFileName = "Sort.txt"
    val customSortingFile = File(File(directoryPath),customSortingFileName)
    val names = arrayListOf<String>()

    if (customSortingFile.exists()) {
        customSortingFile.bufferedReader().forEachLine {

            var offset = 0

            if (it != "" && File(directoryPath, it).exists()) {
                names.add(it)
                var flag = true

                for (i in offset until source.size){
                    val medium = source[i]
                    if(medium.name == it){
                        source.removeAt(i)
                        source.add(offset, medium)
                        offset += 1
                        break
                    }
                }
            }
        }
        source.reverse()
    }
    return source
}
