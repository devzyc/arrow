@file:Suppress("unused")

package com.zyc.arrow

import android.util.Log

const val PAGE_LIMIT = 2000

object LogLong {

  fun d(tag: String, message: String) {
    for (i in 0..message.length / PAGE_LIMIT) {
      val start = i * PAGE_LIMIT
      var end = (i + 1) * PAGE_LIMIT
      end = end.coerceAtMost(message.length)
      Log.d(
        tag,
        message.substring(
          start,
          end
        )
      )
    }
  }
}