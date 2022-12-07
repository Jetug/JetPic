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

suspend fun <A, B> Iterable<A>.pmap(action: suspend (A) -> B): List<B> = coroutineScope {
    map { async { action(it) } }.awaitAll()
}

fun Context.startSettingsScanner() = launchIO{
    while (true){
        try {
            val directories = directoryDao.getAll() as ArrayList<Directory>

            directories.pmap{ dir ->
                val path = dir.path
                val settings = readSettings(path)
                dir.apply {
                    val group = if(settings.group == NO_VALUE) "" else settings.group
                    groupName = group
                    customSorting = settings.sorting
                }

                if(settings.pinned)
                    config.addPinnedFolders(setOf(path))
                config.saveCustomSorting(path, settings.sorting)
                folderSettingsDao.insert(settings)
                directoryDao.update(dir)
            }
        }
        catch (e: Exception) { Log.e(JET, e.message, e) }

        delay(10000)
    }
}

fun Context.saveIsPinned(paths: ArrayList<String>, pin: Boolean) {
    if (pin)
        config.addPinnedFolders(paths.toHashSet())
    else
        config.removePinnedFolders(paths.toHashSet())

    paths.forEach { path -> saveIsPinned(path, pin) }
}

fun Context.renameGroup(dirGroup: DirectoryGroup, newName: String){
    val groups = dirGroup.innerDirs
    groups.forEach { it.groupName = newName }
    saveDirChanges(groups)
}

fun Context.saveDirChanges(directories: ArrayList<Directory>) = launchIO{
    directories.forEach{ saveDirChanges(it) }
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

fun Context.getFolderSorting(path: String): Int{
    return config.getCustomFolderSorting(path)
}

fun Context.saveSorting(path: String, sorting: Int) {
    config.saveCustomSorting(path, sorting)
    launchIO {
        val settings = getSettings(path)
        settings.sorting = sorting
        saveSettings(settings)
    }
}

fun Context.getCustomMediaOrder(source: ArrayList<Medium>){
    sortAs(source, getSettings(source[0].parentPath).order)
}

fun Context.saveCustomMediaOrder(medias:ArrayList<Medium>) = launchIO {
    if (medias.isNotEmpty()) {
        val path = medias[0].parentPath
        val settings = getSettings(path)
        settings.order = medias.names

        saveSettings(settings)
    }
}

fun Context.getSettings(path: String): FolderSettings{
    var settings: FolderSettings? = folderSettingsDao.getByPath(path)

    if(settings == null) {
        settings = if(hasStoragePermission)
            readSettings(path)
        else
            FolderSettings(null, path, "", arrayListOf())

        launchIO {
            folderSettingsDao.insert(settings)
        }
    }
    return settings
}

fun Context.saveSettings(settings: FolderSettings) = launchIO {
    folderSettingsDao.insert(settings)
    writeSettings(settings)
}

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
    if (source.isEmpty()) return

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

private fun Context.saveIsPinned(path: String, pin: Boolean) = launchIO{
    val settings = getSettings(path)
    settings.pinned = pin
    saveSettings(settings)
}
