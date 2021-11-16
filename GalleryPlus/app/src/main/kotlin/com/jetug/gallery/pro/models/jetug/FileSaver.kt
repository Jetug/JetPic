package com.jetug.gallery.pro.models.jetug

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetug.gallery.pro.models.Medium
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File


data class DirectoryConfig(@SerializedName("order")val order: ArrayList<String>, @SerializedName("group")val group: String){}

const val SORTING_FILE_NAME = "Sort.txt"
const val GROUPING_FILE_NAME = "Group.txt"

fun saveDirectoryGroup(directoryPath: String, groupName: String){
    val groupFile = File(File(directoryPath), GROUPING_FILE_NAME)

    if (!groupFile.exists()) {
        groupFile.createNewFile()
    }

    groupFile.printWriter().use {
        it.println(groupName)
    }
}

fun getDirectoryGroup(directoryPath: String): String{
    val groupFile = File(File(directoryPath), GROUPING_FILE_NAME)
    if (!groupFile.exists()) {
        return ""
    }

    return groupFile.bufferedReader().readLine()
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

//    val jsonObject = JSONTokener(response).nextValue() as JSONObject
//
//// ID
//    val id = jsonObject.getString("id")
//    Log.i("ID: ", id)
//
//// Employee Name
//    val employeeName = jsonObject.getString("employee_name")
//    Log.i("Employee Name: ", employeeName)
//
//// Employee Salary
//    val employeeSalary = jsonObject.getString("employee_salary")
//    Log.i("Employee Salary: ", employeeSalary)
//
//// Employee Age
//    val employeeAge = jsonObject.getString("employee_age")
//    Log.i("Employee Age: ", employeeAge)
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
