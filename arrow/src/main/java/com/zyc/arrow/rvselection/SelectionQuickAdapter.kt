package com.zyc.arrow.rvselection

import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

/** @author zeng_yong_chang@163.com
 */
abstract class SelectionQuickAdapter<T>(
    layoutResId: Int,
    data: MutableList<T>,
    private val selectionSupport: ItemSelectionSupport
) : BaseQuickAdapter<T, BaseViewHolder>(layoutResId, data) {
    override fun convert(
        helper: BaseViewHolder,
        item: T
    ) {
        helper.itemView.setOnClickListener { _: View? -> handleClicked(item) }
    }

    protected fun handleClicked(clickedItem: T) {
        notifyDataSetChanged()
    }

    fun checked(viewHolder: BaseViewHolder): Boolean {
        return selectionSupport.isItemChecked(viewHolder.adapterPosition)
    }
}