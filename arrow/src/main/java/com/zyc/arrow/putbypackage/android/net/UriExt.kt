package com.zyc.arrow.putbypackage.android.net

import android.content.Context
import android.net.Uri
import android.provider.MediaStore

/**
 * @author zeng_yong_chang@163.com
 */
fun Uri.toPathForPhoto(context: Context): String {
  val c = context.contentResolver.query(
    this,
    null,
    null,
    null,
    null
  )!!
  c.moveToFirst()
  val path = c.getString(c.getColumnIndex(MediaStore.Images.Media.DATA))
  c.close()
  return path
}