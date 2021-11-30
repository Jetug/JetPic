package com.jetug.gallery.pro.jetug

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jetug.commons.extensions.hasStoragePermission
import com.jetug.gallery.pro.extensions.*
import com.jetug.gallery.pro.models.*
import kotlinx.coroutines.*
import java.io.File
import java.lang.reflect.Type

const val SETTINGS_FILE_NAME = "settings.txt"

fun Context.saveDirectoryGroup(path: String, groupName: String) = IOScope.launch {
    val settings = IOScope.async { getFolderSettings(path) }.await()
    settings.group = groupName
    folderSettingsDao.insert(settings)

    if(hasStoragePermission){
        writeSettingsToFile(path, settings)
    }
}

fun Context.getDirectoryGroup(path: String): String{
    var settings: FolderSettings? = runBlocking { IOScope.async { getFolderSettings(path) }.await() }
    var group = settings!!.group

    if(settings.group == "" && hasStoragePermission){
        settings = runBlocking {IOScope.async {readSettingsFromFile(path) }.await() }
        if(settings != null && settings.group != "") {
            group = settings.group
            IOScope.launch {
                folderSettingsDao.insert(settings)
            }
        }
    }
    return group
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
    if (source.isEmpty()) return

    val path = source[0].parentPath
    var settings: FolderSettings? = runBlocking { IOScope.async { getFolderSettings(path) }.await() }
    var order = settings!!.order

    if(order.isEmpty() && hasStoragePermission){
        settings = runBlocking { IOScope.async {readSettingsFromFile(path) }.await() }
        if(settings != null && settings.order.isNotEmpty()) {
            order = settings.order
            IOScope.launch {
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
    var sorting = runBlocking { IOScope.async { config.getFolderSorting(path) }.await() }
    if(sorting == 0 && hasStoragePermission){
        val settings = runBlocking { IOScope.async {readSettingsFromFile(path) }.await() }
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

private fun getCreateSettingsFile(path: String): File{
    val settingsFile = File(File(path), SETTINGS_FILE_NAME)
    if (!settingsFile.exists())
        settingsFile.createNewFile()
    return settingsFile
}

private fun getSettingsFile(path: String) = File(path, SETTINGS_FILE_NAME)
////

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
        val type: Type = object : TypeToken<FolderSettings?>() {}.type
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
