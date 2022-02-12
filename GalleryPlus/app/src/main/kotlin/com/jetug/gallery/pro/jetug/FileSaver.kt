package com.jetug.gallery.pro.jetug

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jetug.commons.extensions.hasStoragePermission
import com.jetug.gallery.pro.extensions.*
import com.jetug.gallery.pro.models.*
import kotlinx.coroutines.*
import java.io.File
import java.lang.reflect.Type

const val SETTINGS_FILE_NAME = "settings.txt"

class Synchronisator{
    val pool: ArrayList<()->Any> = arrayListOf()

    var isAlreadyRunning: Boolean = false

    fun <T:Any> launch(block: ()->T)/*: T*/ {
        //lateinit var res: T
        if(isAlreadyRunning){
            pool.add(block)
        }
        else{
            isAlreadyRunning = true
            block()
            isAlreadyRunning = false
            if (pool.isNotEmpty()){
                val savedBlock = pool.takeLast()
                launch{savedBlock}
            }
        }
        //return res
    }
}

private val sync = Synchronisator()

fun readZip(){

}

fun Context.saveDirectoryGroup(path: String, groupName: String) = IOScope.launch {
    //sync.launch{
        val settings = IOScope.async { getFolderSettings(path) }.await()
        settings.group = groupName
        folderSettingsDao.insert(settings)

        if (hasStoragePermission) {
            writeSettingsToFile(path, settings)
        }
    //}
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

fun Context.saveCustomMediaOrder(medias:ArrayList<Medium>){
    sync.launch{
        if (medias.isNotEmpty()) {
            val path: String = medias[0].parentPath
            val settings = getFolderSettingsFromFile(path)
            val names = medias.names
            settings.order = names
            folderSettingsDao.insert(settings)

            if (hasStoragePermission)
                writeSettingsToFile(path, settings)

            Log.e("Jet", "save ${settings.order[0]}; ${settings.order[1]}; ${settings.order[2]},")
        }
    }
}

fun Context.getCustomMediaOrder(source: ArrayList<Medium>){
    if (source.isEmpty()) return

    val path = source[0].parentPath
    var settings: FolderSettings? = runBlocking { IOScope.async { getFolderSettings(path) }.await() }
    var order = settings!!.order

    if (order.isNotEmpty())

    if(order.isEmpty() && hasStoragePermission){
        settings = runBlocking { IOScope.async { readSettingsFromFile(path) }.await() }
        if(settings != null && settings.order.isNotEmpty()) {
            order = settings.order
            IOScope.launch {
                folderSettingsDao.insert(settings)
            }
        }
    }

//    val settings = readSettingsFromFile(path)
//
//    if (settings != null) {
//        val order = settings.order
//        sortAs(source, order)
//        if(order.isNotEmpty())
//            Log.e("Jet", "get ${order[0]}; ${order[1]}; ${order[2]}; ")
//        else
//            Log.e("Jet", "get empty")
//
//    }


    sortAs(source, order)

}

fun Context.saveCustomSorting(path: String, sorting: Int){
    sync.launch {
        val settings = getFolderSettingsFromFile(path)
        settings.sorting = sorting
        config.saveCustomSorting(path, sorting)
        folderSettingsDao.insert(settings)

        Log.e("Jet", "sorting ${settings.order.toString()}")
        if (hasStoragePermission) writeSettingsToFile(path, settings)
    }
}

fun Context.getFolderSorting(path: String): Int{
    var sorting = runBlocking { IOScope.async { config.getRealFolderSorting(path) }.await() }
    if(sorting == 0){
        sorting = runBlocking { IOScope.async { config.getCustomFolderSorting(path) }.await() }
        if(hasStoragePermission) {
            val settings = runBlocking { IOScope.async { readSettingsFromFile(path) }.await() }
            if (settings != null) {
                sorting = settings.sorting
                IOScope.launch {
                    if (sorting != 0) {
                        settings.sorting = sorting
                        config.saveCustomSorting(path, sorting)
                        folderSettingsDao.insert(settings)
                    }
                }
            }
        }
    }
    return sorting
}

////////////////////////////

private fun Context.getFolderSettingsFromFile(path: String): FolderSettings{
    var settings: FolderSettings? = null
    if(hasStoragePermission){
        settings = readSettingsFromFile(path)
    }
    if(settings == null)
        settings = getFolderSettings(path)
    return settings
}

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
    if(settings.order.size > 2)
        Log.e("Jet", "write ${settings.order[0]}; ${settings.order[1]}; ${settings.order[2]},")

    val json: String = Gson().toJson(settings)
    getCreateSettingsFile(path).printWriter().use {
        it.println(json)
    }

    Log.e("Jet", json)

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
