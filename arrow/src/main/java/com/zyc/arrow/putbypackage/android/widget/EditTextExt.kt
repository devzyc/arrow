package com.zyc.arrow.putbypackage.android.widget

import android.widget.EditText

/**
 * @author zengyongchang_2010@163.com
 */
val EditText.content get() = text.toString().trim()