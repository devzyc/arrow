package com.zyc.arrow

import android.view.View

/**
 * @author zeng_yong_chang@163.com
 */
public fun View.nameOfId() = resources.getResourceEntryName(id)!!