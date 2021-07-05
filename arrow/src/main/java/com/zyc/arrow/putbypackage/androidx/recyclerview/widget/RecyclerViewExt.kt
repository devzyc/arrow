package com.zyc.arrow.putbypackage.androidx.recyclerview.widget

import android.widget.Toast
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView

/**
 * @author zeng_yong_chang@163.com
 */
class RecyclerViewExt {

  fun RecyclerView.click(position: Int, @IdRes clickableAreaResId: Int) {
    postDelayed({
      val viewHolder = findViewHolderForAdapterPosition(position)
      if (viewHolder == null) {
        Toast.makeText(context, "切换列表项失败", Toast.LENGTH_SHORT)
          .show()
      } else {
        val v =
          if (clickableAreaResId == 0) viewHolder.itemView
          else viewHolder.itemView.findViewById(clickableAreaResId)
        v.performClick()
      }
    }, 500)
  }
}