package com.simplemobiletools.gallery.pro.data.helpers.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class JsonToStringConverter {
    @TypeConverter
    fun fromArrayList(list: ArrayList<String>): String {
        val gson = Gson()
        return gson.toJson(list)
    }
    @TypeConverter
    fun fromString(value: String): ArrayList<String> {
        val listType: Type = object : TypeToken<ArrayList<String?>?>(){}.type
        return Gson().fromJson(value, listType)
    }
}
