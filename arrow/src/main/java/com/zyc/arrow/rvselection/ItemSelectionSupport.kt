package com.zyc.arrow.rvselection

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.LongSparseArray
import android.util.SparseBooleanArray
import android.view.View
import android.widget.Checkable
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min

class ItemSelectionSupport(
    private val mRecyclerView: RecyclerView,
    enableTouch: Boolean
) {
    enum class ChoiceMode {
        NONE, SINGLE, MULTIPLE
    }

    private val mTouchListener: TouchListener
    private var mAllSelectedListener: OnAllSelectedListener? = null
    private var mChoiceMode = ChoiceMode.NONE
    private var mCheckedStates: CheckedStates? = null
    private var mCheckedIdStates: CheckedIdStates? = null

    /**
     * Returns the number of items currently selected. This will only be valid
     * if the choice mode is not [ChoiceMode.NONE] (default).
     *
     *
     *
     * To determine the specific items that are currently selected, use one of
     * the `getChecked*` methods.
     *
     * @return The number of items currently selected
     * @see .getCheckedItemPosition
     * @see .getCheckedItemPositions
     * @see .getCheckedItemIds
     */
    var checkedItemCount = 0
        private set
    private var mEnableTouch = false

    init {
        mTouchListener = TouchListener(mRecyclerView)
        setTouchEnabled(enableTouch)
    }

    fun enableTouch(): Boolean {
        return mEnableTouch
    }

    fun setTouchEnabled(enabled: Boolean) {
        mEnableTouch = enabled
        if (enabled) {
            mRecyclerView.addOnItemTouchListener(mTouchListener)
        } else {
            mRecyclerView.removeOnItemTouchListener(mTouchListener)
        }
    }

    private fun updateOnScreenCheckedViews() {
        /*
        final int count = mRecyclerView.getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = mRecyclerView.getChildAt(i);
            final int position = mRecyclerView.getChildPosition(child);
            setViewChecked(child, mCheckedStates.get(position));
        }
        */
    }

    /**
     * Returns the checked state of the specified position. The result is only
     * valid if the choice mode has been set to [ChoiceMode.SINGLE]
     * or [ChoiceMode.MULTIPLE].
     *
     * @param position The item whose checked state to return
     * @return The item's checked state or `false` if choice mode
     * is invalid
     * @see .setChoiceMode
     */
    fun isItemChecked(position: Int): Boolean {
        return if (mChoiceMode != ChoiceMode.NONE && mCheckedStates != null) {
            mCheckedStates!![position]
        } else false
    }

    /**
     * Returns the currently checked item. The result is only valid if the choice
     * mode has been set to [ChoiceMode.SINGLE].
     *
     * @return The position of the currently checked item or
     * [.INVALID_POSITION] if nothing is selected
     * @see .setChoiceMode
     */
    val checkedItemPosition: Int
        get() = if (mChoiceMode == ChoiceMode.SINGLE && mCheckedStates != null && mCheckedStates!!.size() == 1) {
            mCheckedStates!!.keyAt(0)
        } else INVALID_POSITION

    /**
     * Returns the set of checked items in the list. The result is only valid if
     * the choice mode has not been set to [ChoiceMode.NONE].
     *
     * @return A SparseBooleanArray which will return true for each call to
     * get(int position) where position is a position in the list,
     * or `null` if the choice mode is set to
     * [ChoiceMode.NONE].
     */
    val checkedItemPositions: SparseBooleanArray?
        get() = if (mChoiceMode != ChoiceMode.NONE) {
            mCheckedStates
        } else null

    /**
     * Returns the set of checked items ids. The result is only valid if the
     * choice mode has not been set to [ChoiceMode.NONE] and the adapter
     * has stable IDs.
     *
     * @return A new array which contains the id of each checked item in the
     * list.
     * @see Adapter.hasStableIds
     */
    val checkedItemIds: LongArray
        get() {
            if (mChoiceMode == ChoiceMode.NONE || mCheckedIdStates == null || mRecyclerView.adapter == null) {
                return LongArray(0)
            }
            val count = mCheckedIdStates!!.size()
            val ids = LongArray(count)
            for (i in 0 until count) {
                ids[i] = mCheckedIdStates!!.keyAt(i)
            }
            return ids
        }

    /**
     * Sets the checked state of the specified position. The is only valid if
     * the choice mode has been set to [ChoiceMode.SINGLE] or
     * [ChoiceMode.MULTIPLE].
     *
     * @param position The item whose checked state is to be checked
     * @param checked  The new checked state for the item
     */
    fun setItemChecked(
        position: Int,
        checked: Boolean
    ) {
        if (mChoiceMode == ChoiceMode.NONE) {
            return
        }
        val adapter = mRecyclerView.adapter
        if (mChoiceMode == ChoiceMode.MULTIPLE) {
            val oldValue = mCheckedStates!![position]
            mCheckedStates!!.put(position, checked)
            if (mCheckedIdStates != null && adapter!!.hasStableIds()) {
                if (checked) {
                    mCheckedIdStates!!.put(adapter.getItemId(position), position)
                } else {
                    mCheckedIdStates!!.delete(adapter.getItemId(position))
                }
            }
            val itemCount = mRecyclerView.adapter!!.itemCount
            if (oldValue != checked) {
                if (checked) {
                    checkedItemCount++
                    if (checkedItemCount == itemCount && mAllSelectedListener != null) {
                        mAllSelectedListener!!.onChanged(true)
                    }
                } else {
                    checkedItemCount--
                    // 仅适用于mCheckedCount变化量为1的情况，下面的判断是为了减少mAllSelectedListener频繁地回调
                    if (checkedItemCount == itemCount - 1 && mAllSelectedListener != null) {
                        mAllSelectedListener!!.onChanged(false)
                    }
                }
            }
        } else {
            val updateIds = mCheckedIdStates != null && adapter!!.hasStableIds()

            // Clear all values if we're checking something, or unchecking the currently
            // selected item
            if (checked || isItemChecked(position)) {
                mCheckedStates!!.clear()
                if (updateIds) {
                    mCheckedIdStates!!.clear()
                }
            }

            // This may end up selecting the checked we just cleared but this way
            // we ensure length of mCheckStates is 1, a fact getCheckedItemPosition relies on
            if (checked) {
                mCheckedStates!!.put(position, true)
                if (updateIds) {
                    mCheckedIdStates!!.put(adapter!!.getItemId(position), position)
                }
                checkedItemCount = 1
            } else if (mCheckedStates!!.size() == 0 || !mCheckedStates!!.valueAt(0)) {
                checkedItemCount = 0
            }
        }
        updateOnScreenCheckedViews()
    }

    fun setViewChecked(
        view: View,
        checked: Boolean
    ) {
        if (view is Checkable) {
            (view as Checkable).isChecked = checked
        } else
            view.isActivated = checked
    }

    /**
     * Clears any choices previously set.
     */
    fun clearChoices() {
        if (mCheckedStates != null) {
            mCheckedStates!!.clear()
        }
        if (mCheckedIdStates != null) {
            mCheckedIdStates!!.clear()
        }
        checkedItemCount = 0
        updateOnScreenCheckedViews()
    }
    /**
     * Returns the current choice mode.
     *
     * @see .setChoiceMode
     */
    /**
     * Defines the choice behavior for the List. By default, Lists do not have any choice behavior
     * ([ChoiceMode.NONE]). By setting the choiceMode to [ChoiceMode.SINGLE], the
     * List allows up to one item to  be in a chosen state. By setting the choiceMode to
     * [ChoiceMode.MULTIPLE], the list allows any number of items to be chosen.
     *
     * @param choiceMode One of [ChoiceMode.NONE], [ChoiceMode.SINGLE], or
     * [ChoiceMode.MULTIPLE]
     */
    var choiceMode: ChoiceMode
        get() = mChoiceMode
        set(choiceMode) {
            if (mChoiceMode == choiceMode) {
                return
            }
            mChoiceMode = choiceMode
            if (mChoiceMode != ChoiceMode.NONE) {
                if (mCheckedStates == null) {
                    mCheckedStates = CheckedStates()
                }
                val adapter = mRecyclerView.adapter
                if (mCheckedIdStates == null && adapter != null && adapter.hasStableIds()) {
                    mCheckedIdStates = CheckedIdStates()
                }
            }
        }

    fun setChoiceModeMultiple(listener: OnAllSelectedListener?) {
        mAllSelectedListener = listener
        choiceMode = ChoiceMode.MULTIPLE
    }

    fun onAdapterDataChanged() {
        val adapter = mRecyclerView.adapter
        if (mChoiceMode == ChoiceMode.NONE || adapter == null || !adapter.hasStableIds()) {
            return
        }
        val itemCount = adapter.itemCount

        // Clear out the positional check states, we'll rebuild it below from IDs.
        mCheckedStates!!.clear()
        var checkedIndex = 0
        while (checkedIndex < mCheckedIdStates!!.size()) {
            val currentId = mCheckedIdStates!!.keyAt(checkedIndex)
            val currentPosition = mCheckedIdStates!!.valueAt(checkedIndex)!!
            val newPositionId = adapter.getItemId(currentPosition)
            if (currentId != newPositionId) {
                // Look around to see if the ID is nearby. If not, uncheck it.
                val start = 0.coerceAtLeast(currentPosition - CHECK_POSITION_SEARCH_DISTANCE)
                val end = min(currentPosition + CHECK_POSITION_SEARCH_DISTANCE, itemCount)
                var found = false
                for (searchPos in start until end) {
                    val searchId = adapter.getItemId(searchPos)
                    if (currentId == searchId) {
                        found = true
                        mCheckedStates!!.put(searchPos, true)
                        mCheckedIdStates!!.setValueAt(checkedIndex, searchPos)
                        break
                    }
                }
                if (!found) {
                    mCheckedIdStates!!.delete(currentId)
                    checkedItemCount--
                    checkedIndex--
                }
            } else {
                mCheckedStates!!.put(currentPosition, true)
            }
            checkedIndex++
        }
    }

    fun onSaveInstanceState(): Bundle {
        val state = Bundle()
        state.putInt(STATE_KEY_CHOICE_MODE, mChoiceMode.ordinal)
        state.putParcelable(STATE_KEY_CHECKED_STATES, mCheckedStates)
        state.putParcelable(STATE_KEY_CHECKED_ID_STATES, mCheckedIdStates)
        state.putInt(STATE_KEY_CHECKED_COUNT, checkedItemCount)
        return state
    }

    fun onRestoreInstanceState(state: Bundle) {
        mChoiceMode = ChoiceMode.values()[state.getInt(STATE_KEY_CHOICE_MODE)]
        mCheckedStates = state.getParcelable(STATE_KEY_CHECKED_STATES)
        mCheckedIdStates = state.getParcelable(STATE_KEY_CHECKED_ID_STATES)
        checkedItemCount = state.getInt(STATE_KEY_CHECKED_COUNT)

        // TODO confirm ids here
    }

    private class CheckedStates : SparseBooleanArray, Parcelable {
        constructor() : super()
        private constructor(`in`: Parcel) {
            val size = `in`.readInt()
            if (size > 0) {
                for (i in 0 until size) {
                    val key = `in`.readInt()
                    val value = `in`.readInt() == TRUE
                    put(key, value)
                }
            }
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(
            parcel: Parcel,
            flags: Int
        ) {
            val size = size()
            parcel.writeInt(size)
            for (i in 0 until size) {
                parcel.writeInt(keyAt(i))
                parcel.writeInt(if (valueAt(i)) TRUE else FALSE)
            }
        }

        companion object {
            private const val FALSE = 0
            private const val TRUE = 1
            @JvmField
            val CREATOR: Parcelable.Creator<CheckedStates?> = object : Parcelable.Creator<CheckedStates?> {
                override fun createFromParcel(`in`: Parcel): CheckedStates? {
                    return CheckedStates(`in`)
                }

                override fun newArray(size: Int): Array<CheckedStates?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    private class CheckedIdStates : LongSparseArray<Int?>, Parcelable {
        constructor() : super()
        private constructor(`in`: Parcel) {
            val size = `in`.readInt()
            if (size > 0) {
                for (i in 0 until size) {
                    val key = `in`.readLong()
                    val value = `in`.readInt()
                    put(key, value)
                }
            }
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(
            parcel: Parcel,
            flags: Int
        ) {
            val size = size()
            parcel.writeInt(size)
            for (i in 0 until size) {
                parcel.writeLong(keyAt(i))
                parcel.writeInt(valueAt(i)!!)
            }
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<CheckedIdStates?> = object : Parcelable.Creator<CheckedIdStates?> {
                override fun createFromParcel(`in`: Parcel): CheckedIdStates? {
                    return CheckedIdStates(`in`)
                }

                override fun newArray(size: Int): Array<CheckedIdStates?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    private inner class TouchListener internal constructor(recyclerView: RecyclerView?) : ClickItemTouchListener(recyclerView!!) {
        override fun performItemClick(
            parent: RecyclerView?,
            view: View?,
            position: Int,
            id: Long
        ): Boolean {
            val adapter = mRecyclerView.adapter
            var checkedStateChanged = false
            if (mChoiceMode == ChoiceMode.MULTIPLE) {
                val checked = !mCheckedStates!![position, false]
                mCheckedStates!!.put(position, checked)
                if (mCheckedIdStates != null && adapter!!.hasStableIds()) {
                    if (checked) {
                        mCheckedIdStates!!.put(adapter.getItemId(position), position)
                    } else {
                        mCheckedIdStates!!.delete(adapter.getItemId(position))
                    }
                }
                val itemCount = mRecyclerView.adapter!!.itemCount
                if (checked) {
                    checkedItemCount++
                    if (checkedItemCount == itemCount && mAllSelectedListener != null) {
                        mAllSelectedListener!!.onChanged(true)
                    }
                } else {
                    checkedItemCount--
                    // 仅适用于mCheckedCount变化量为1的情况，下面的判断是为了减少mAllSelectedListener频繁地回调
                    if (checkedItemCount == itemCount - 1 && mAllSelectedListener != null) {
                        mAllSelectedListener!!.onChanged(false)
                    }
                }
                checkedStateChanged = true
            } else if (mChoiceMode == ChoiceMode.SINGLE) {
                val checked = !mCheckedStates!![position, false]
                if (checked) {
                    mCheckedStates!!.clear()
                    mCheckedStates!!.put(position, true)
                    if (mCheckedIdStates != null && adapter!!.hasStableIds()) {
                        mCheckedIdStates!!.clear()
                        mCheckedIdStates!!.put(adapter.getItemId(position), position)
                    }
                    checkedItemCount = 1
                } else if (mCheckedStates!!.size() == 0 || !mCheckedStates!!.valueAt(0)) {
                    checkedItemCount = 0
                }
                checkedStateChanged = true
            }
            if (checkedStateChanged) {
                updateOnScreenCheckedViews()
            }
            return false
        }

        override fun performItemLongClick(
            parent: RecyclerView?,
            view: View?,
            position: Int,
            id: Long
        ): Boolean {
            return true
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
    }

    interface OnAllSelectedListener {
        fun onChanged(allSelected: Boolean)
    }

    companion object {
        const val INVALID_POSITION = -1
        private const val STATE_KEY_CHOICE_MODE = "choiceMode"
        private const val STATE_KEY_CHECKED_STATES = "checkedStates"
        private const val STATE_KEY_CHECKED_ID_STATES = "checkedIdStates"
        private const val STATE_KEY_CHECKED_COUNT = "checkedCount"
        private const val CHECK_POSITION_SEARCH_DISTANCE = 20
    }
}