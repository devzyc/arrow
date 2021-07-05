package com.zyc.arrow.putbypackage.android.graphics

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.*

/**
 * @author zeng_yong_chang@163.com
 */
fun Bitmap.toByteArray(): ByteArray {
  val baos = ByteArrayOutputStream()
  compressAsPng(baos)
  return baos.toByteArray()
}

fun Bitmap.saveToSdcard(fileName: String, context: Context) {
  val targetFile: File = File(
    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!,
    fileName
  )
  if (!targetFile.exists()) {
    try {
      targetFile.createNewFile()
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }
  try {
    val fos = FileOutputStream(targetFile)
    compressAsPng(fos)
    fos.flush()
    fos.close()
  } catch (e: IOException) {
    e.printStackTrace()
  }
}

private fun Bitmap.compressAsPng(fos: OutputStream) {
  compress(
    Bitmap.CompressFormat.PNG,
    100,
    fos
  )
}