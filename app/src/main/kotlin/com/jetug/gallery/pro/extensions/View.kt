package com.jetug.gallery.pro.extensions

import android.app.Activity
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.jetug.commons.extensions.actionBarHeight

fun View.sendFakeClick(x: Float, y: Float) {
    val uptime = SystemClock.uptimeMillis()
    val event = MotionEvent.obtain(uptime, uptime, MotionEvent.ACTION_DOWN, x, y, 0)
    dispatchTouchEvent(event)
    event.action = MotionEvent.ACTION_UP
    dispatchTouchEvent(event)
}

fun View.setMargin(value: Int){
    setMargin(Rect(value,value,value,value))
}

fun View.setMargin(rect: Rect){
    val param = this.layoutParams as ViewGroup.MarginLayoutParams
    param.setMargins(rect.left, rect.top, rect.right, rect.bottom)
    this.layoutParams = param
}
