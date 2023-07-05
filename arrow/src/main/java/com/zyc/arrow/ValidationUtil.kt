package com.zyc.arrow

import android.util.Patterns

/**
 * @author zeng_yong_chang@163.com
 */
object ValidationUtil {
    fun isEmail(input: String?): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(input).matches()
    }
}