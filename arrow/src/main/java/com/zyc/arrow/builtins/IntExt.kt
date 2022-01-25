package com.zyc.arrow.builtins

import android.content.res.Resources

/**
 * @author zeng_yong_chang@163.com
 */

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
