package com.jetug.gallery.pro.models.jetug

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetug.commons.extensions.hasStoragePermission
import com.jetug.gallery.pro.extensions.*
import com.jetug.gallery.pro.models.*
import kotlinx.coroutines.*
import java.io.File


data class DirectoryConfig(@SerializedName("order")val order: ArrayList<String>, @SerializedName("group")val group: String){}

const val SORTING_FILE_NAME = "Sort.txt"
const val GROUPING_FILE_NAME = "Group.txt"

val IOScope = CoroutineScope(Dispatchers.IO)
val DefaultScope = CoroutineScope(Dispatchers.Default)
val MainScope = CoroutineScope(Dispatchers.Main)

class FileSaver(val activity: AppCompatActivity){}

fun Context.saveDirectoryGroup(path: String, groupName: String) = IOScope.launch {
    var settings = folderSettingsDao.getByPath(path)
    if(settings != null)
        settings.group = groupName
    else
        settings = FolderSettings(null, path, groupName, arrayListOf())
    folderSettingsDao.insert(settings)

    if(hasStoragePermission){
        saveDirectoryGroupToFile(path, groupName)
    }
}

fun saveDirectoryGroupToFile(directoryPath: String, groupName: String){
    val groupFile = File(File(directoryPath), GROUPING_FILE_NAME)

    if (!groupFile.exists()) {
        groupFile.createNewFile()
    }

    groupFile.printWriter().use {
        it.println(groupName)
    }
}

fun Context.getOrCreateFolderSettings(path: String): FolderSettings{
    var settings: FolderSettings? = folderSettingsDao.getByPath(path)
    if(settings == null)
        settings = FolderSettings(null, path, "", arrayListOf())
    return settings
}

fun Context.getDirectoryGroup(path: String): String{
    val settings: FolderSettings = runBlocking { IOScope.async { getOrCreateFolderSettings(path) }.await() }
    var groupName = settings.group

    if(settings.group == "" && hasStoragePermission){
        groupName = getDirectoryGroupFromFile(path)
        IOScope.launch {
            if(groupName != "") folderSettingsDao.insert(settings)
        }
    }

    return groupName
}

fun getDirectoryGroupFromFile(directoryPath: String): String{
    val groupFile = File(File(directoryPath), GROUPING_FILE_NAME)
    if (!groupFile.exists()) return ""

    var line = ""
     groupFile.bufferedReader().forEachLine {
         line = it
    }

    return line
}

fun Context.saveImagePositions(medias:ArrayList<Medium>) = IOScope.launch{
    if(medias.isNotEmpty()) {
        val path: String = medias[0].parentPath
        val settings = getOrCreateFolderSettings(path)
        val names = medias.names
        settings.order = names

        if(hasStoragePermission){
            saveImagePositionsToFile(path, names)
        }

    }
}

private fun saveImagePositionsToFile(path: String, medias:ArrayList<String>){
    val sortingFile = File(File(path), SORTING_FILE_NAME)
    if (!sortingFile.exists())
        sortingFile.createNewFile()
    writePositionsToFile(sortingFile, medias)
}

fun Context.getCustomMediaList(source: ArrayList<Medium>){
    if (source.isEmpty())
        return

    val path = source[0].parentPath
    val settings: FolderSettings = runBlocking { IOScope.async { getOrCreateFolderSettings(path) }.await() }
    var order = settings.order
    if(order.isEmpty()){
        order = getCustomMediaListFromFile(path)
    }

    sortAs(source, order)
}

fun getCustomMediaListFromFile(path: String): ArrayList<String>{
    val customSortingFileName = "Sort.txt"
    val customSortingFile = File(File(path),customSortingFileName)
    val order = arrayListOf<String>()
    if (customSortingFile.exists()) {
        customSortingFile.bufferedReader().forEachLine {
            if (it != "" && File(path, it).exists()) {
                order.add(it)
            }
        }
    }
    return order
}

fun sortAs(source: ArrayList<Medium>, sample: ArrayList<String>){
    if (source.isEmpty())
        return

    val path = source[0].parentPath
    sample.forEach {
        var offset = 0
        if (it != "" && File(path, it).exists()) {
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

private fun writePositionsToFile(sortingFile: File, medias:ArrayList<String>){
    var text = ""
    for (m in medias) {
        text += m + "\n"
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
