package com.simplemobiletools.gallery.pro.data.extensions.context

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.extensions.hasStoragePermission
import com.simplemobiletools.commons.helpers.FAVORITES
import com.simplemobiletools.gallery.pro.data.extensions.*
import com.simplemobiletools.gallery.pro.data.helpers.JET
import com.simplemobiletools.gallery.pro.data.helpers.NO_VALUE
import com.simplemobiletools.gallery.pro.data.helpers.RECYCLE_BIN
import com.simplemobiletools.gallery.pro.data.models.*
import kotlinx.coroutines.*
import java.io.File
import java.lang.reflect.Type
import kotlin.system.measureTimeMillis

const val SETTINGS_FILE_NAME = "settings.txt"
val systemPaths = arrayOf("", RECYCLE_BIN, FAVORITES)

class Synchronizer{
    private val pool: ArrayList<()->Any> = arrayListOf()

    var isAlreadyRunning: Boolean = false

    fun <T:Any> launch(block: ()->T){
        if(isAlreadyRunning){
            pool.add(block)
        }
        else{
            isAlreadyRunning = true
            block()
            isAlreadyRunning = false
            if (pool.isNotEmpty()){
                val savedBlock = pool.takeFirst()
                launch{savedBlock}
            }
        }
    }
}

private val sync = Synchronizer()

fun Context.startSettingsScanner() = launchIO{
    while (true){
        try {
            val directories = directoryDao.getAll() as ArrayList<Directory>
            directories.forEach{ dir ->
                val settings = readSettings(dir.path)
                val group = if(settings.group == NO_VALUE) "" else settings.group
                dir.apply {
                    groupName = group
                    customSorting = settings.sorting
                }

                directoryDao.update(dir)
                config.saveCustomSorting(dir.path, settings.sorting)
                folderSettingsDao.insert(settings)
            }
        } catch (e: Exception) {
            Log.e(JET, e.message, e)
        }

        delay(5000)
    }
}

fun Context.pinDir(dirGroup: DirectoryGroup, newName: String){

}

fun Context.renameGroup(dirGroup: DirectoryGroup, newName: String) = launchIO{
    val groups = dirGroup.innerDirs

    groups.forEach {
        it.groupName = newName
    }

    saveDirChanges(groups)
}

fun Context.getDirectoryGroup(path: String): String{
    val settings = getSettings(path)
    var group = settings.group

    if(group == NO_VALUE) group = ""

    return group
}

fun Context.saveDirChanges(directories: ArrayList<Directory>) = launchIO{
    directories.forEach{
        saveDirChanges(it)
    }
}

fun Context.saveDirChanges(directory: Directory) = launchIO{
    val settings = getSettings(directory.path)
    settings.addDirectoryData(directory)
    settings.sorting = config.getCustomFolderSorting(directory.path)
    updateDirectory(directory)
    saveSettings(settings)
}

fun Context.saveDirectoryGroup(path: String, groupName: String) = launchIO{
    val settings = getSettings(path)
    settings.group = groupName

    saveSettings(settings)
}

fun Context.getSorting(path: String): Int{
    var sorting: Int
    val time = measureTimeMillis {
        val settings = getSettings(path)

        sorting = if(settings.sorting != 0)
            settings.sorting
        else
            config.getCustomFolderSorting(path)
    }
    //Log.e(JET,"getSorting $time ms")
    return sorting
}

fun Context.saveSorting(path: String, sorting: Int){
    sync.launch {
        launchIO {
            val settings = getSettings(path)
            settings.sorting = sorting
            config.saveCustomSorting(path, sorting)
            saveSettings(settings)
        }
    }
}

fun Context.getCustomMediaOrder(source: ArrayList<Medium>){
    if (source.isEmpty()) return

    val path = source[0].parentPath
    val settings: FolderSettings = getSettings(path)
    sortAs(source, settings.order)
}

fun Context.saveCustomMediaOrder(medias:ArrayList<Medium>){
    sync.launch{
        launchIO {
            if (medias.isNotEmpty()) {
                val path = medias[0].parentPath
                val settings = getSettings(path)
                settings.order = medias.names

                saveSettings(settings)
            }
        }
    }
}

fun Context.getSettings(path: String): FolderSettings{
    var settings: FolderSettings? = folderSettingsDao.getByPath(path)

    if(settings == null) {
        if(hasStoragePermission)
            settings = readSettings(path)
        else
            settings = FolderSettings(null, path, "", arrayListOf())

        launchIO {
            folderSettingsDao.insert(settings)
        }
    }
    return settings
}

fun Context.saveSettings(settings: FolderSettings) = launchIO {
    folderSettingsDao.insert(settings)
    //GalleryDatabase.getInstance(applicationContext).isOpen
    writeSettings(settings)
}

////////////////////////////
private fun getOrCreateSettingsFile(path: String): File{
    val settingsFile = File(File(path), SETTINGS_FILE_NAME)
    if (!settingsFile.exists())
        settingsFile.createNewFile()
    return settingsFile
}

private fun readSettings(path: String): FolderSettings{
    val file = File(path, SETTINGS_FILE_NAME)

    var settings: FolderSettings? = null
    if(file.exists()) {
        val json = file.readText()
        val type: Type = object : TypeToken<FolderSettings?>() {}.type
        settings = Gson().fromJson(json, type)
        settings?.path = path
    }

    if(settings == null){
        settings = FolderSettings(null, path, "", arrayListOf())
    }

    return settings
}

private fun Context.writeSettings(settings: FolderSettings){
    if (File(settings.path).exists() && hasStoragePermission) {
        val json: String = Gson().toJson(settings)
        getOrCreateSettingsFile(settings.path).printWriter().use {
            it.println(json)
        }
    }
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
