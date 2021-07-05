@file:Suppress("unused")

package com.zyc.arrow

import android.text.TextUtils
import android.widget.EditText
import android.widget.TextView

/** @author zeng_yong_chang@163.com
 */
object TextUtil {

  fun textOf(textView: TextView): String {
    val got = textView.text
    return got?.toString() ?: ""
  }

  fun textOf(editText: EditText): String {
    return editText.text.toString()
  }

  fun showNullableText(textView: TextView, value: String?) {
    textView.text = if (TextUtils.isEmpty(value)) "无" else value
  }

  fun isChinese(c: Char): Boolean {
    val ub = Character.UnicodeBlock.of(c)
    return ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || ub === Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || ub === Character.UnicodeBlock.GENERAL_PUNCTUATION
            || ub === Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
            || ub === Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
  }

  fun isEnglish(c: Char): Boolean {
    return c in 'a'..'z' || c in 'A'..'Z'
  }
}