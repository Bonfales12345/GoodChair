package com.goodchair.launcher.view

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout

class GestureFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var gestureDetector: GestureDetector? = null
    private var startY = 0f
    private var startX = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    fun setGestureDetector(detector: GestureDetector) {
        this.gestureDetector = detector
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startY = ev.y
                startX = ev.x
                gestureDetector?.onTouchEvent(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                val diffY = startY - ev.y
                val diffX = Math.abs(startX - ev.x)
                // If swiping up and vertical movement is significant
                if (diffY > touchSlop && diffY > diffX) {
                    return true // Intercept to handle swipe up
                }
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector?.onTouchEvent(event) ?: super.onTouchEvent(event)
    }
}