package com.zyc.arrow.rvselection

import android.content.Context
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import androidx.core.view.MotionEventCompat
import androidx.recyclerview.widget.RecyclerView

internal abstract class ClickItemTouchListener(hostView: RecyclerView) : RecyclerView.OnItemTouchListener {
    private val mGestureDetector: GestureDetectorCompat

    init {
        mGestureDetector = ItemClickGestureDetector(
            hostView.context,
            ItemClickGestureListener(hostView)
        )
    }

    private fun isAttachedToWindow(hostView: RecyclerView): Boolean {
        return hostView.isAttachedToWindow
    }

    private fun hasAdapter(hostView: RecyclerView): Boolean {
        return hostView.adapter != null
    }

    override fun onInterceptTouchEvent(
        recyclerView: RecyclerView,
        event: MotionEvent
    ): Boolean {
        if (!isAttachedToWindow(recyclerView) || !hasAdapter(recyclerView)) {
            return false
        }
        mGestureDetector.onTouchEvent(event)
        return false
    }

    override fun onTouchEvent(
        recyclerView: RecyclerView,
        event: MotionEvent
    ) {
        // We can silently track tap and and long presses by silently
        // intercepting touch events in the host RecyclerView.
    }

    abstract fun performItemClick(
        parent: RecyclerView?,
        view: View?,
        position: Int,
        id: Long
    ): Boolean

    abstract fun performItemLongClick(
        parent: RecyclerView?,
        view: View?,
        position: Int,
        id: Long
    ): Boolean

    private inner class ItemClickGestureDetector(
        context: Context?,
        private val mGestureListener: ItemClickGestureListener
    ) : GestureDetectorCompat(context, mGestureListener) {
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val handled = super.onTouchEvent(event)
            val action = event.action and MotionEventCompat.ACTION_MASK
            if (action == MotionEvent.ACTION_UP) {
                mGestureListener.dispatchSingleTapUpIfNeeded(event)
            }
            return handled
        }
    }

    private inner class ItemClickGestureListener(private val mHostView: RecyclerView) : SimpleOnGestureListener() {
        private var mTargetChild: View? = null
        fun dispatchSingleTapUpIfNeeded(event: MotionEvent) {
            // When the long press hook is called but the long press listener
            // returns false, the target child will be left around to be
            // handled later. In this case, we should still treat the gesture
            // as potential item click.
            if (mTargetChild != null) {
                onSingleTapUp(event)
            }
        }

        override fun onDown(event: MotionEvent): Boolean {
            val x = event.x.toInt()
            val y = event.y.toInt()
            mTargetChild = mHostView.findChildViewUnder(x.toFloat(), y.toFloat())
            return mTargetChild != null
        }

        override fun onShowPress(event: MotionEvent) {
            if (mTargetChild != null) {
                mTargetChild!!.isPressed = true
            }
        }

        override fun onSingleTapUp(event: MotionEvent): Boolean {
            var handled = false
            if (mTargetChild != null) {
                mTargetChild!!.isPressed = false
                val position = mHostView.getChildPosition(mTargetChild!!)
                val id = mHostView.adapter!!.getItemId(position)
                handled = performItemClick(mHostView, mTargetChild, position, id)
                mTargetChild = null
            }
            return handled
        }

        override fun onScroll(
            event: MotionEvent,
            event2: MotionEvent,
            v: Float,
            v2: Float
        ): Boolean {
            if (mTargetChild != null) {
                mTargetChild!!.isPressed = false
                mTargetChild = null
                return true
            }
            return false
        }

        override fun onLongPress(event: MotionEvent) {
            if (mTargetChild == null) {
                return
            }
            val position = mHostView.getChildPosition(mTargetChild!!)
            val id = mHostView.adapter!!.getItemId(position)
            val handled = performItemLongClick(mHostView, mTargetChild, position, id)
            if (handled) {
                mTargetChild!!.isPressed = false
                mTargetChild = null
            }
        }
    }
}