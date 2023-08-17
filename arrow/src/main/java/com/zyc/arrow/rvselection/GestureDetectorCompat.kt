/*
     * Copyright (C) 2012 The Android Open Source Project
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
package com.zyc.arrow.rvselection

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Message
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import androidx.core.view.MotionEventCompat
import androidx.core.view.VelocityTrackerCompat

/**
 * Detects various gestures and events using the supplied [MotionEvent]s.
 * The [OnGestureListener] callback will notify users when a particular
 * motion event has occurred. This class should only be used with [MotionEvent]s
 * reported via touch (don't use for trackball events).
 *
 *
 * This compatibility implementation of the framework's GestureDetector guarantees
 * the newer focal point scrolling behavior from Jellybean MR1 on all platform versions.
 *
 * To use this class:
 *
 *  * Create an instance of the `GestureDetectorCompat` for your [View]
 *  * In the [View.onTouchEvent] method ensure you call
 * [.onTouchEvent]. The methods defined in your callback
 * will be executed when the events occur.
 *
 */
open class GestureDetectorCompat @JvmOverloads constructor(
    context: Context?,
    listener: GestureDetector.OnGestureListener?,
    handler: Handler? = null
) {
    internal interface GestureDetectorCompatImpl {
        var isLongPressEnabled: Boolean
        fun onTouchEvent(ev: MotionEvent): Boolean
        fun setOnDoubleTapListener(listener: GestureDetector.OnDoubleTapListener?)
    }

    internal class GestureDetectorCompatImplBase(
        context: Context?,
        listener: GestureDetector.OnGestureListener?,
        handler: Handler?
    ) : GestureDetectorCompatImpl {
        private var mTouchSlopSquare = 0
        private var mDoubleTapSlopSquare = 0
        private var mMinimumFlingVelocity = 0
        private var mMaximumFlingVelocity = 0
        private var mHandler: Handler? = null
        private val mListener: GestureDetector.OnGestureListener?
        private var mDoubleTapListener: GestureDetector.OnDoubleTapListener? = null
        private var mStillDown = false
        private var mDeferConfirmSingleTap = false
        private var mInLongPress = false
        private var mAlwaysInTapRegion = false
        private var mAlwaysInBiggerTapRegion = false
        private var mCurrentDownEvent: MotionEvent? = null
        private var mPreviousUpEvent: MotionEvent? = null

        /**
         * True when the user is still touching for the second tap (down, move, and
         * up events). Can only be true if there is a double tap listener attached.
         */
        private var mIsDoubleTapping = false
        private var mLastFocusX = 0f
        private var mLastFocusY = 0f
        private var mDownFocusX = 0f
        private var mDownFocusY = 0f
        /**
         * @return true if longpress is enabled, else false.
         */
        /**
         * Set whether longpress is enabled, if this is enabled when a user
         * presses and holds down you get a longpress event and nothing further.
         * If it's disabled the user can press and hold down and then later
         * moved their finger and you will get scroll events. By default
         * longpress is enabled.
         *
         * @param isLongpressEnabled whether longpress should be enabled.
         */
        override var isLongPressEnabled = false

        /**
         * Determines speed during touch scrolling
         */
        private var mVelocityTracker: VelocityTracker? = null

        private inner class GestureHandler : Handler {
            internal constructor() : super() {}
            internal constructor(handler: Handler) : super(handler.looper) {}

            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    SHOW_PRESS -> mListener!!.onShowPress(mCurrentDownEvent)
                    LONG_PRESS -> dispatchLongPress()
                    TAP ->                 // If the user's finger is still down, do not count it as a tap
                        if (mDoubleTapListener != null) {
                            if (!mStillDown) {
                                mDoubleTapListener!!.onSingleTapConfirmed(mCurrentDownEvent)
                            } else {
                                mDeferConfirmSingleTap = true
                            }
                        }

                    else -> throw RuntimeException("Unknown message $msg") //never
                }
            }
        }

        /**
         * Creates a GestureDetector with the supplied listener.
         * You may only use this constructor from a UI thread (this is the usual situation).
         *
         * @param context the application's context
         * @param listener the listener invoked for all the callbacks, this must
         * not be null.
         * @param handler the handler to use
         * @throws NullPointerException if `listener` is null.
         * @see Handler.Handler
         */
        init {
            if (handler != null) {
                mHandler = GestureHandler(handler)
            } else {
                mHandler = GestureHandler()
            }
            mListener = listener
            if (listener is GestureDetector.OnDoubleTapListener) {
                setOnDoubleTapListener(listener as GestureDetector.OnDoubleTapListener?)
            }
            init(context)
        }

        private fun init(context: Context?) {
            requireNotNull(context) { "Context must not be null" }
            requireNotNull(mListener) { "OnGestureListener must not be null" }
            isLongPressEnabled = true
            val configuration = ViewConfiguration.get(context)
            val touchSlop = configuration.scaledTouchSlop
            val doubleTapSlop = configuration.scaledDoubleTapSlop
            mMinimumFlingVelocity = configuration.scaledMinimumFlingVelocity
            mMaximumFlingVelocity = configuration.scaledMaximumFlingVelocity
            mTouchSlopSquare = touchSlop * touchSlop
            mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop
        }

        /**
         * Sets the listener which will be called for double-tap and related
         * gestures.
         *
         * @param onDoubleTapListener the listener invoked for all the callbacks, or
         * null to stop listening for double-tap gestures.
         */
        override fun setOnDoubleTapListener(onDoubleTapListener: GestureDetector.OnDoubleTapListener?) {
            mDoubleTapListener = onDoubleTapListener
        }

        /**
         * Analyzes the given motion event and if applicable triggers the
         * appropriate callbacks on the [OnGestureListener] supplied.
         *
         * @param ev The current motion event.
         * @return true if the [OnGestureListener] consumed the event,
         * else false.
         */
        override fun onTouchEvent(ev: MotionEvent): Boolean {
            val action = ev.action
            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain()
            }
            mVelocityTracker!!.addMovement(ev)
            val pointerUp = action and MotionEventCompat.ACTION_MASK == MotionEventCompat.ACTION_POINTER_UP
            val skipIndex = if (pointerUp) MotionEventCompat.getActionIndex(ev) else -1

            // Determine focal point
            var sumX = 0f
            var sumY = 0f
            val count = MotionEventCompat.getPointerCount(ev)
            for (i in 0 until count) {
                if (skipIndex == i) continue
                sumX += MotionEventCompat.getX(ev, i)
                sumY += MotionEventCompat.getY(ev, i)
            }
            val div = if (pointerUp) count - 1 else count
            val focusX = sumX / div
            val focusY = sumY / div
            var handled = false
            when (action and MotionEventCompat.ACTION_MASK) {
                MotionEventCompat.ACTION_POINTER_DOWN -> {
                    run {
                        mLastFocusX = focusX
                        mDownFocusX = mLastFocusX
                    }
                    run {
                        mLastFocusY = focusY
                        mDownFocusY = mLastFocusY
                    }
                    // Cancel long press and taps
                    cancelTaps()
                }

                MotionEventCompat.ACTION_POINTER_UP -> {
                    run {
                        mLastFocusX = focusX
                        mDownFocusX = mLastFocusX
                    }
                    run {
                        mLastFocusY = focusY
                        mDownFocusY = mLastFocusY
                    }

                    // Check the dot product of current velocities.
                    // If the pointer that left was opposing another velocity vector, clear.
                    mVelocityTracker!!.computeCurrentVelocity(1000, mMaximumFlingVelocity.toFloat())
                    val upIndex = MotionEventCompat.getActionIndex(ev)
                    val id1 = MotionEventCompat.getPointerId(ev, upIndex)
                    val x1 = VelocityTrackerCompat.getXVelocity(mVelocityTracker, id1)
                    val y1 = VelocityTrackerCompat.getYVelocity(mVelocityTracker, id1)
                    var i = 0
                    while (i < count) {
                        if (i == upIndex) {
                            i++
                            continue
                        }
                        val id2 = MotionEventCompat.getPointerId(ev, i)
                        val x = x1 * VelocityTrackerCompat.getXVelocity(mVelocityTracker, id2)
                        val y = y1 * VelocityTrackerCompat.getYVelocity(mVelocityTracker, id2)
                        val dot = x + y
                        if (dot < 0) {
                            mVelocityTracker!!.clear()
                            break
                        }
                        i++
                    }
                }

                MotionEvent.ACTION_DOWN -> {
                    if (mDoubleTapListener != null) {
                        val hadTapMessage = mHandler!!.hasMessages(TAP)
                        if (hadTapMessage) mHandler!!.removeMessages(TAP)
                        if (mCurrentDownEvent != null && mPreviousUpEvent != null && hadTapMessage &&
                            isConsideredDoubleTap(mCurrentDownEvent!!, mPreviousUpEvent!!, ev)
                        ) {
                            // This is a second tap
                            mIsDoubleTapping = true
                            // Give a callback with the first tap of the double-tap
                            handled = handled or mDoubleTapListener!!.onDoubleTap(mCurrentDownEvent)
                            // Give a callback with down event of the double-tap
                            handled = handled or mDoubleTapListener!!.onDoubleTapEvent(ev)
                        } else {
                            // This is a first tap
                            mHandler!!.sendEmptyMessageDelayed(TAP, DOUBLE_TAP_TIMEOUT.toLong())
                        }
                    }
                    run {
                        mLastFocusX = focusX
                        mDownFocusX = mLastFocusX
                    }
                    run {
                        mLastFocusY = focusY
                        mDownFocusY = mLastFocusY
                    }
                    if (mCurrentDownEvent != null) {
                        mCurrentDownEvent!!.recycle()
                    }
                    mCurrentDownEvent = MotionEvent.obtain(ev)
                    mAlwaysInTapRegion = true
                    mAlwaysInBiggerTapRegion = true
                    mStillDown = true
                    mInLongPress = false
                    mDeferConfirmSingleTap = false
                    if (isLongPressEnabled) {
                        mHandler!!.removeMessages(LONG_PRESS)
                        mHandler!!.sendEmptyMessageAtTime(
                            LONG_PRESS, mCurrentDownEvent!!.downTime
                                    + TAP_TIMEOUT + LONGPRESS_TIMEOUT
                        )
                    }
                    mHandler!!.sendEmptyMessageAtTime(
                        SHOW_PRESS,
                        mCurrentDownEvent!!.downTime + TAP_TIMEOUT
                    )
                    handled = handled or mListener!!.onDown(ev)
                }

                MotionEvent.ACTION_MOVE -> {
                    if (mInLongPress) {
                        return handled
                    }
                    val scrollX = mLastFocusX - focusX
                    val scrollY = mLastFocusY - focusY
                    if (mIsDoubleTapping) {
                        // Give the move events of the double-tap
                        handled = handled or mDoubleTapListener!!.onDoubleTapEvent(ev)
                    } else if (mAlwaysInTapRegion) {
                        val deltaX = (focusX - mDownFocusX).toInt()
                        val deltaY = (focusY - mDownFocusY).toInt()
                        val distance = deltaX * deltaX + deltaY * deltaY
                        if (distance > mTouchSlopSquare) {
                            handled = mListener!!.onScroll(mCurrentDownEvent, ev, scrollX, scrollY)
                            mLastFocusX = focusX
                            mLastFocusY = focusY
                            mAlwaysInTapRegion = false
                            mHandler!!.removeMessages(TAP)
                            mHandler!!.removeMessages(SHOW_PRESS)
                            mHandler!!.removeMessages(LONG_PRESS)
                        }
                        if (distance > mTouchSlopSquare) {
                            mAlwaysInBiggerTapRegion = false
                        }
                    } else if (Math.abs(scrollX) >= 1 || Math.abs(scrollY) >= 1) {
                        handled = mListener!!.onScroll(mCurrentDownEvent, ev, scrollX, scrollY)
                        mLastFocusX = focusX
                        mLastFocusY = focusY
                    }
                }

                MotionEvent.ACTION_UP -> {
                    mStillDown = false
                    val currentUpEvent = MotionEvent.obtain(ev)
                    if (mIsDoubleTapping) {
                        // Finally, give the up event of the double-tap
                        handled = handled or mDoubleTapListener!!.onDoubleTapEvent(ev)
                    } else if (mInLongPress) {
                        mHandler!!.removeMessages(TAP)
                        mInLongPress = false
                    } else if (mAlwaysInTapRegion) {
                        handled = mListener!!.onSingleTapUp(ev)
                        if (mDeferConfirmSingleTap && mDoubleTapListener != null) {
                            mDoubleTapListener!!.onSingleTapConfirmed(ev)
                        }
                    } else {
                        // A fling must travel the minimum tap distance
                        val velocityTracker = mVelocityTracker
                        val pointerId = MotionEventCompat.getPointerId(ev, 0)
                        velocityTracker!!.computeCurrentVelocity(1000, mMaximumFlingVelocity.toFloat())
                        val velocityY = VelocityTrackerCompat.getYVelocity(
                            velocityTracker, pointerId
                        )
                        val velocityX = VelocityTrackerCompat.getXVelocity(
                            velocityTracker, pointerId
                        )
                        if (Math.abs(velocityY) > mMinimumFlingVelocity || Math.abs(velocityX) > mMinimumFlingVelocity) {
                            handled = mListener!!.onFling(mCurrentDownEvent, ev, velocityX, velocityY)
                        }
                    }
                    if (mPreviousUpEvent != null) {
                        mPreviousUpEvent!!.recycle()
                    }
                    // Hold the event we obtained above - listeners may have changed the original.
                    mPreviousUpEvent = currentUpEvent
                    if (mVelocityTracker != null) {
                        // This may have been cleared when we called out to the
                        // application above.
                        mVelocityTracker!!.recycle()
                        mVelocityTracker = null
                    }
                    mIsDoubleTapping = false
                    mDeferConfirmSingleTap = false
                    mHandler!!.removeMessages(SHOW_PRESS)
                    mHandler!!.removeMessages(LONG_PRESS)
                }

                MotionEvent.ACTION_CANCEL -> cancel()
            }
            return handled
        }

        private fun cancel() {
            mHandler!!.removeMessages(SHOW_PRESS)
            mHandler!!.removeMessages(LONG_PRESS)
            mHandler!!.removeMessages(TAP)
            mVelocityTracker!!.recycle()
            mVelocityTracker = null
            mIsDoubleTapping = false
            mStillDown = false
            mAlwaysInTapRegion = false
            mAlwaysInBiggerTapRegion = false
            mDeferConfirmSingleTap = false
            if (mInLongPress) {
                mInLongPress = false
            }
        }

        private fun cancelTaps() {
            mHandler!!.removeMessages(SHOW_PRESS)
            mHandler!!.removeMessages(LONG_PRESS)
            mHandler!!.removeMessages(TAP)
            mIsDoubleTapping = false
            mAlwaysInTapRegion = false
            mAlwaysInBiggerTapRegion = false
            mDeferConfirmSingleTap = false
            if (mInLongPress) {
                mInLongPress = false
            }
        }

        private fun isConsideredDoubleTap(
            firstDown: MotionEvent,
            firstUp: MotionEvent,
            secondDown: MotionEvent
        ): Boolean {
            if (!mAlwaysInBiggerTapRegion) {
                return false
            }
            if (secondDown.eventTime - firstUp.eventTime > DOUBLE_TAP_TIMEOUT) {
                return false
            }
            val deltaX = firstDown.x.toInt() - secondDown.x.toInt()
            val deltaY = firstDown.y.toInt() - secondDown.y.toInt()
            return deltaX * deltaX + deltaY * deltaY < mDoubleTapSlopSquare
        }

        private fun dispatchLongPress() {
            mHandler!!.removeMessages(TAP)
            mDeferConfirmSingleTap = false
            mInLongPress = true
            mListener!!.onLongPress(mCurrentDownEvent)
        }

        companion object {
            private val LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout()
            private val TAP_TIMEOUT = ViewConfiguration.getTapTimeout()
            private val DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout()

            // constants for Message.what used by GestureHandler below
            private const val SHOW_PRESS = 1
            private const val LONG_PRESS = 2
            private const val TAP = 3
        }
    }

    internal class GestureDetectorCompatImplJellybeanMr2(
        context: Context?,
        listener: GestureDetector.OnGestureListener?,
        handler: Handler?
    ) : GestureDetectorCompatImpl {
        private val mDetector: GestureDetector

        init {
            mDetector = GestureDetector(context, listener, handler)
        }

        override var isLongPressEnabled: Boolean
            get() = mDetector.isLongpressEnabled
            set(enabled) {
                mDetector.setIsLongpressEnabled(enabled)
            }

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            return mDetector.onTouchEvent(ev)
        }

        override fun setOnDoubleTapListener(listener: GestureDetector.OnDoubleTapListener?) {
            mDetector.setOnDoubleTapListener(listener)
        }
    }

    private var mImpl: GestureDetectorCompatImpl? = null
    /**
     * Creates a GestureDetectorCompat with the supplied listener.
     * As usual, you may only use this constructor from a UI thread.
     *
     * @param context the application's context
     * @param listener the listener invoked for all the callbacks, this must
     * not be null.
     * @param handler the handler that will be used for posting deferred messages
     * @see Handler.Handler
     */
    /**
     * Creates a GestureDetectorCompat with the supplied listener.
     * As usual, you may only use this constructor from a UI thread.
     *
     * @param context the application's context
     * @param listener the listener invoked for all the callbacks, this must
     * not be null.
     * @see Handler.Handler
     */
    init {
        mImpl = if (Build.VERSION.SDK_INT > 17) {
            GestureDetectorCompatImplJellybeanMr2(context, listener, handler)
        } else {
            GestureDetectorCompatImplBase(context, listener, handler)
        }
    }
    /**
     * @return true if longpress is enabled, else false.
     */
    /**
     * Set whether longpress is enabled, if this is enabled when a user
     * presses and holds down you get a longpress event and nothing further.
     * If it's disabled the user can press and hold down and then later
     * moved their finger and you will get scroll events. By default
     * longpress is enabled.
     *
     * @param enabled whether longpress should be enabled.
     */
    var isLongpressEnabled: Boolean
        get() = mImpl!!.isLongPressEnabled
        set(enabled) {
            mImpl!!.isLongPressEnabled = enabled
        }

    /**
     * Analyzes the given motion event and if applicable triggers the
     * appropriate callbacks on the [OnGestureListener] supplied.
     *
     * @param event The current motion event.
     * @return true if the [OnGestureListener] consumed the event,
     * else false.
     */
    open fun onTouchEvent(event: MotionEvent): Boolean {
        return mImpl!!.onTouchEvent(event)
    }

    /**
     * Sets the listener which will be called for double-tap and related
     * gestures.
     *
     * @param listener the listener invoked for all the callbacks, or
     * null to stop listening for double-tap gestures.
     */
    fun setOnDoubleTapListener(listener: GestureDetector.OnDoubleTapListener?) {
        mImpl!!.setOnDoubleTapListener(listener)
    }
}