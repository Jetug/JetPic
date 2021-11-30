package com.jetug.gallery.pro.jetug

import android.content.Context
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.jetug.commons.extensions.hasStoragePermission
import com.jetug.gallery.pro.extensions.*
import com.jetug.gallery.pro.models.*
import kotlinx.coroutines.*
import java.io.File
import java.lang.reflect.Type


data class DirectoryConfig(@SerializedName("order")val order: ArrayList<String>, @SerializedName("group")val group: String){}

const val SORTING_FILE_NAME = "Sort.txt"
const val GROUPING_FILE_NAME = "Group.txt"
const val SETTINGS_FILE_NAME = "settings.txt"

val IOScope = CoroutineScope(Dispatchers.IO)
val DefaultScope = CoroutineScope(Dispatchers.Default)
val MainScope = CoroutineScope(Dispatchers.Main)

fun launchIO(block: suspend CoroutineScope.() -> Unit) = IOScope.launch { block()}
fun launchDefault(block: suspend CoroutineScope.() -> Unit) = DefaultScope.launch { block()}
fun launchMain(block: suspend CoroutineScope.() -> Unit) = MainScope.launch { block()}


class FileSaver(val activity: AppCompatActivity){}

fun Context.saveDirectoryGroup(path: String, groupName: String) = IOScope.launch {
    val settings = IOScope.async { getFolderSettings(path) }.await()
    settings.group = groupName
    folderSettingsDao.insert(settings)

    if(hasStoragePermission){
        saveDirectoryGroupToFile(path, groupName)
    }
}

fun Context.getDirectoryGroup(path: String): String{
    val settings: FolderSettings = runBlocking { IOScope.async { getFolderSettings(path) }.await() }
    var groupName = settings.group

    if(settings.group == "" && hasStoragePermission){
        groupName = getDirectoryGroupFromFile(path)
        IOScope.launch {
            if(groupName != "") {
                settings.group = groupName
                folderSettingsDao.insert(settings)
            }
        }
    }

    return groupName
}

fun Context.saveCustomMediaOrder(medias:ArrayList<Medium>) = IOScope.launch{
    if(medias.isNotEmpty()) {
        val path: String = medias[0].parentPath
        val settings = getFolderSettings(path)
        val names = medias.names
        settings.order = names
        folderSettingsDao.insert(settings)

        if(hasStoragePermission)
            writeSettingsToFile(path, settings)

    }
}

fun Context.getCustomMediaOrder(source: ArrayList<Medium>){
    if (source.isEmpty())
        return

    val path = source[0].parentPath
    val settings: FolderSettings = runBlocking { IOScope.async { getFolderSettings(path) }.await() }
    var order = settings.order

    if(order.isEmpty() && hasStoragePermission){
        order = getCustomMediaListFromFile(path)
        IOScope.launch {
            if(order.isNotEmpty()) {
                settings.order = order
                folderSettingsDao.insert(settings)
            }
        }
    }
    sortAs(source, order)
}

fun Context.saveCustomSorting(path: String, sorting: Int) = launchIO{
    val settings = getFolderSettings(path)
    settings.sorting = sorting
    config.saveCustomSorting(path, sorting)
    folderSettingsDao.insert(settings)
    if(hasStoragePermission) writeSettingsToFile(path, settings)
}

fun Context.getFolderSorting(path: String): Int{
    //val settings = runBlocking { IOScope.async { getFolderSettings(path) }.await() }
    var sorting = config.getFolderSorting(path)
    if(sorting == 0 && hasStoragePermission){
        val settings = readSettingsFromFile(path)
        if(settings != null) {
            sorting = settings.sorting
            IOScope.launch {
                if(sorting != 0) {
                    settings.sorting = sorting
                    config.saveCustomSorting(path, sorting)
                    folderSettingsDao.insert(settings)
                }
            }
        }
    }
    return sorting
}

////////////////////////////


private fun Context.getFolderSettings(path: String): FolderSettings{
    var settings: FolderSettings? = folderSettingsDao.getByPath(path)
    if(settings == null)
        settings = FolderSettings(null, path, "", arrayListOf())
    return settings
}

////

private fun saveDirectoryGroupToFile(directoryPath: String, groupName: String){
    val groupFile = File(File(directoryPath), GROUPING_FILE_NAME)
    if (!groupFile.exists()) groupFile.createNewFile()
    groupFile.printWriter().use {
        it.println(groupName)
    }
}


private fun getDirectoryGroupFromFile(directoryPath: String): String{
    val groupFile = File(File(directoryPath), GROUPING_FILE_NAME)
    if (!groupFile.exists()) return ""

    var line = ""
     groupFile.bufferedReader().forEachLine {
         line = it
    }

    return line
}



private fun getCreateSettingsFile(path: String): File{
    val settingsFile = File(File(path), SETTINGS_FILE_NAME)
    if (!settingsFile.exists())
        settingsFile.createNewFile()
    return settingsFile
}

private fun getSettingsFile(path: String) = File(path, SETTINGS_FILE_NAME)

private fun saveMediaOrderToFile(path: String, medias:ArrayList<String>){
    val sortingFile = File(File(path), SORTING_FILE_NAME)
    if (!sortingFile.exists())
        sortingFile.createNewFile()
    writePositionsToFile(sortingFile, medias)


}

private fun getCustomMediaListFromFile(path: String): ArrayList<String>{
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

private fun writePositionsToFile(sortingFile: File, medias:ArrayList<String>){
    var text = ""
    for (m in medias)
        text += m + "\n"

    sortingFile.printWriter().use {
        it.println(text)
    }
}

private fun writeSettingsToFile(path: String, settings: FolderSettings){
    val json: String = Gson().toJson(settings)
    getCreateSettingsFile(path).printWriter().use {
        it.println(json)
    }
}

private fun readSettingsFromFile(path: String): FolderSettings?{
    val file = getSettingsFile(path)
    var settings: FolderSettings? = null
    if(file.exists()) {
        val json = file.readText()
        val type: Type = object : TypeToken<Settings?>() {}.type
        settings = Gson().fromJson(json, type)
        settings?.path = path
    }
    return settings
}

private fun sortAs(source: ArrayList<Medium>, sample: ArrayList<String>){
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
