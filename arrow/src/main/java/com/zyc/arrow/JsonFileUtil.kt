@file:Suppress("unused")

package com.zyc.arrow

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.lang.reflect.Type

/**
 * @author zeng_yong_chang@163.com
 */
object JsonFileUtil {

  private const val TAG = "JsonFileUtil"

  private fun cacheDir(context: Context): File {
    return context.cacheDir
  }

  private fun getFile(name: String, context: Context): File {
    return File(cacheDir(context), name)
  }

  fun <T> loadObject(classOfT: Class<T>, fileName: String, context: Context): T? {
    var obj: T? = null
    try {
      val reader = JsonReader(FileReader(getFile(fileName, context)))
      obj = Gson().fromJson(reader, classOfT)
      reader.close()
    } catch (e: IOException) {
      Log.e(TAG, String.format("failed on reading json from file: %s", fileName), e)
    } catch (e: JsonIOException) {
      Log.e(TAG, String.format("failed on reading json from file: %s", fileName), e)
    } catch (e: JsonSyntaxException) {
      Log.e(TAG, String.format("failed on reading json from file: %s", fileName), e)
    }
    return obj
  }

  fun saveToJson(src: Any, typeOfSrc: Type, fileName: String, context: Context) {
    val writer: JsonWriter
    try {
      writer = JsonWriter(FileWriter(getFile(fileName, context), false))
      writer.setIndent("  ")
      GsonBuilder().setPrettyPrinting()
        .create()
        .toJson(src, typeOfSrc, writer)
      writer.close()
    } catch (e: IOException) {
      Log.e(TAG, String.format("failed on writing json to file: %s", fileName), e)
    }
  }
}