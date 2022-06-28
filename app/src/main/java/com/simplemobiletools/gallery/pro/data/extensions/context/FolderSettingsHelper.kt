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

class Synchronisator{
    private val pool: ArrayList<()->Any> = arrayListOf()

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

//if(group == null && hasStoragePermission){
//    runBlocking { IOScope.async { readSettingsFromFile(path) }.await() }.also { settings = it }
//
//    if(settings != null){
//        group = if (settings!!.group != null) settings!!.group else ""
//
//        IOScope.launch {
//            folderSettingsDao.insert(settings!!)
//        }
//    }
//}

//var i = 0
//if(path.endsWith("(A) ONI")) {
//    i = 1
//}


fun Context.getDirectoryGroup(path: String): String{
    val settingsDb: FolderSettings = runBlocking { IOScope.async { getSettings(path) }.await() }
    var group = settingsDb.group
    if(group == "" && hasStoragePermission){
        val settingsFile = runBlocking {IOScope.async {readSettingsFromFile(path) }.await() }

        if(settingsFile.group == ""){
            settingsFile.group = NO_VALUE
            group = ""
        }
        else{
            group = settingsFile.group
        }
        IOScope.launch {
            folderSettingsDao.insert(settingsFile)
        }
    }

    if(group == NO_VALUE) group = ""

    return group
}

fun Context.saveDirectoryGroup(path: String, groupName: String) = launchIO{
    val settings = IOScope.async { getSettings(path) }.await()
    settings.group = groupName

    saveSettings(settings)
}

fun Context.getCustomMediaOrder(source: ArrayList<Medium>){
    if (source.isEmpty()) return

    val path = source[0].parentPath
    var settings: FolderSettings? = runBlocking { IOScope.async { getSettings(path) }.await() }
    var order = settings!!.order

    if (order.isNotEmpty())

        if(order.isEmpty() && hasStoragePermission){
            settings = runBlocking { IOScope.async { readSettingsFromFile(path) }.await() }
            if(settings.order.isNotEmpty()) {
                order = settings.order
                IOScope.launch {
                    folderSettingsDao.insert(settings)
                }
            }
        }

    sortAs(source, order)
}

fun Context.saveCustomMediaOrder(medias:ArrayList<Medium>){
    sync.launch{
        launchIO {
            if (medias.isNotEmpty()) {
                val path: String = medias[0].parentPath
                val settings = getSettings(path)
                val names = medias.names
                settings.order = names

                saveSettings(settings)

                Log.e("Jet", "save ${settings.order[0]}; ${settings.order[1]}; ${settings.order[2]},")
            }
        }
    }
}

fun Context.getCustomSorting(path: String): Int{
    var sorting: Int
    val time = measureTimeMillis {
        val settings = getSettings(path)

        if(settings.sorting != 0){
            sorting = settings.sorting
        }
        else{
            sorting = config.getCustomFolderSorting(path)
        }
    }
    Log.e(JET,"getCustomSorting $time ms")
    return sorting
}

//fun Context.getCustomSorting(path: String): Int{
//    val time = measureTimeMillis {
//        var sorting =  config.getRealFolderSorting(path)
//
//        if (sorting == 0) {
//            sorting = config.getCustomFolderSorting(path)
//
//            val settings = getSettings(path)
//
//
//            if (hasStoragePermission) {
//                val settings = readSettingsFromFile(path)
//                sorting = settings.sorting
//                IOScope.launch {
//                    if (sorting != 0) {
//                        settings.sorting = sorting
//                        config.saveCustomSorting(path, sorting)
//                        folderSettingsDao.insert(settings)
//                    }
//                }
//            }
//        }
//    }
//    Log.e(JET,"getCustomSorting $time ms")
//    return sorting
//}

fun Context.saveCustomSorting(path: String, sorting: Int){
    sync.launch {
        launchIO {
            val settings = getSettings(path)
            settings.sorting = sorting
            config.saveCustomSorting(path, sorting)
            saveSettings(settings)
        }
    }
}

fun Context.getSettings(path: String): FolderSettings{
    var settings: FolderSettings? = folderSettingsDao.getByPath(path)

    if(settings == null) {
        if(hasStoragePermission)
            settings = readSettingsFromFile(path)
        else
            settings = FolderSettings(null, path, "", arrayListOf())

        saveSettings(settings)
    }
    return settings
}

fun Context.saveSettings(settings: FolderSettings) = launchIO {
    folderSettingsDao.insert(settings)
    if (hasStoragePermission)
        writeSettingsToFile(settings)
}

////////////////////////////

private fun getSettingsFile(path: String) = File(path, SETTINGS_FILE_NAME)

private fun getOrCreateSettingsFile(path: String): File{
    val settingsFile = File(File(path), SETTINGS_FILE_NAME)
    if (!settingsFile.exists())
        settingsFile.createNewFile()
    return settingsFile
}

//private fun Context.getSettingsFromFile(path: String): FolderSettings{
//    var settings: FolderSettings? = null
//    if(hasStoragePermission){
//        settings = readSettingsFromFile(path)
//    }
//    if(settings == null)
//        settings = FolderSettings(null, path, "", arrayListOf())
//    return settings
//}

private fun readSettingsFromFile(path: String): FolderSettings{
    val file = getSettingsFile(path)

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

private fun Context.writeSettingsToFile(settings: FolderSettings){
    if (File(settings.path).exists()) {
        if (settings.order.size > 2)
            Log.e("Jet", "write ${settings.order[0]}; ${settings.order[1]}; ${settings.order[2]},")

        val json: String = Gson().toJson(settings)
        getOrCreateSettingsFile(settings.path).printWriter().use {
            it.println(json)
        }

        Log.e("Jet", json)
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
