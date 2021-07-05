package com.zyc.arrow.builtins

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * @author zeng_yong_chang@163.com
 */
fun ByteArray.toBitmap(): Bitmap {
  return BitmapFactory.decodeByteArray(this, 0, size)
}