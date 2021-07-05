@file:Suppress("unused")

package com.zyc.arrow

import android.util.Log

/** @author zeng_yong_chang@163.com
 */
object LogUtil {

  private val TAG = LogUtil::class.java.getPackage()!!.name

  /**
   * Define your log level for printing out detail here. *
   */
  private const val PRINT_DETAIL_LOG_LEVEL = Log.ERROR
  fun v(`object`: Any) {
    Log.v(TAG, buildMsg(Log.INFO, `object`))
  }

  fun d(`object`: Any) {
    Log.d(TAG, buildMsg(Log.INFO, `object`))
  }

  fun i(`object`: Any) {
    Log.i(TAG, buildMsg(Log.INFO, `object`))
  }

  fun w(`object`: Any) {
    Log.w(TAG, buildMsg(Log.INFO, `object`))
  }

  fun e(`object`: Any) {
    Log.e(TAG, buildMsg(Log.ERROR, `object`))
  }

  private fun buildMsg(logLevel: Int, `object`: Any): String {
    val buffer = StringBuilder()
    if (logLevel >= PRINT_DETAIL_LOG_LEVEL) {
      val stackTraceElement = Thread.currentThread().stackTrace[4]
      buffer.append("[ thread name is ")
      buffer.append(Thread.currentThread().name)
      buffer.append(", ")
      buffer.append(stackTraceElement.fileName)
      buffer.append(", method is ")
      buffer.append(stackTraceElement.methodName)
      buffer.append("(), at line ")
      buffer.append(stackTraceElement.lineNumber)
      buffer.append(" ]")
      buffer.append("\n")
    }
    buffer.append("___")
    buffer.append(`object`)
    return buffer.toString()
  }

  // Log.v()
  fun lv(content: String?) {
    Log.v(TAG, content!!)
  }

  // Log.d()
  fun ld(content: String?) {
    Log.d(TAG, content!!)
  }

  // Log.i()
  fun li(content: String?) {
    Log.i(TAG, content!!)
  }

  // Log.w()
  fun lw(content: String?) {
    Log.w(TAG, content!!)
  }

  // Log.e()
  fun le(content: String) {
    Log.e(TAG, "wta $content")
  }
}