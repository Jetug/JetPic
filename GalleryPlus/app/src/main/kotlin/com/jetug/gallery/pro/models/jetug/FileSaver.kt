package com.jetug.gallery.pro.models.jetug

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetug.gallery.pro.BuildConfig
import com.jetug.gallery.pro.extensions.*
import com.jetug.gallery.pro.models.Medium
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.net.URI
import java.net.URL


data class DirectoryConfig(@SerializedName("order")val order: ArrayList<String>, @SerializedName("group")val group: String){}

const val SORTING_FILE_NAME = "Sort.txt"
const val GROUPING_FILE_NAME = "Group.txt"

class FileSaver(val activity: AppCompatActivity){

}

fun saveDirectoryGroup(directoryPath: String, groupName: String){
    val groupFile = File(File(directoryPath), GROUPING_FILE_NAME)
    //val uri2 = directoryPath.toUri()

    if (!groupFile.exists()) {
        //activity.createFile(uri2, GROUPING_FILE_NAME)
        groupFile.createNewFile()
    }

    groupFile.printWriter().use {
        it.println(groupName)
    }
}

fun getDirectoryGroup(directoryPath: String): String{
    val groupFile = File(File(directoryPath), GROUPING_FILE_NAME)
    if (!groupFile.exists())  {
        return ""
    }

    var line = ""
     groupFile.bufferedReader().forEachLine {
         line = it
    }

    return line
}




fun saveImagePositions(medias:ArrayList<Medium>){
    if(medias.isNotEmpty()) {
        CoroutineScope(Dispatchers.IO).launch {
            val directoryPath: String = medias[0].parentPath
            val sortingFile = File(File(directoryPath), SORTING_FILE_NAME)

            if (!sortingFile.exists()) {
                sortingFile.createNewFile()
            }

            writePositionsToFile(sortingFile, medias)
        }
    }
}

fun getCustomMediaList(source: ArrayList<Medium>): ArrayList<Medium>{

    if (source.isEmpty())
        return source

    val directoryPath = source[0].parentPath
    val customSortingFileName = "Sort.txt"
    val customSortingFile = File(File(directoryPath),customSortingFileName)

    if (customSortingFile.exists()) {
        customSortingFile.bufferedReader().forEachLine {
            var offset = 0
            if (it != "" && File(directoryPath, it).exists()) {
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

private fun readPositionsFromFile(customSortingFile:File, source: ArrayList<Medium>, directoryPath: String): ArrayList<Medium> {
    customSortingFile.bufferedReader().forEachLine {
        var offset = 0
        val names = arrayListOf<String>()

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
    return source
}

private fun writePositionsToFile(sortingFile: File, medias:ArrayList<Medium>){
    var text = ""
    for (m in medias) {
        text += m.name
        text += "\n"
    }
    sortingFile.printWriter().use {
        it.println(text)
    }
}

private fun writePositionsToFile2(sortingFile: File, medias:ArrayList<Medium>){
    var text = ""
    for (m in medias) {
        text += m.name
        text += "\n"
    }

    val fileNames = ArrayList<String>(medias.map { it.name })
    val json: String = Gson().toJson(DirectoryConfig(fileNames, ""))


    sortingFile.printWriter().use {
        it.println(text)
    }
}
