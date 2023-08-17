package com.zyc.arrow.rvselection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import java.util.List;

/** @author zeng_yong_chang@163.com */
public abstract class SelectionQuickAdapter<T> extends BaseQuickAdapter<T, BaseViewHolder> {
  private ItemSelectionSupport selectionSupport;

  public SelectionQuickAdapter(int layoutResId, @Nullable List<T> data, ItemSelectionSupport support) {
    super(layoutResId, data);
    selectionSupport = support;
  }

  @Override protected void convert(@NonNull BaseViewHolder helper, T item) {
    helper.itemView.setOnClickListener(__ -> handleClicked(item));
  }

  protected void handleClicked(T clickedItem) {
    notifyDataSetChanged();
  }

  public boolean checked(BaseViewHolder viewHolder) {
    return selectionSupport.isItemChecked(viewHolder.getAdapterPosition());
  }
}
