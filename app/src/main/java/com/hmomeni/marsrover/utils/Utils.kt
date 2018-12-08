package com.hmomeni.marsrover.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.view.WindowManager

private var screenDimension: Dimension? = null
fun getScreenDimensions(context: Context): Dimension {
    if (screenDimension != null) {
        return screenDimension as Dimension
    }
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = wm.defaultDisplay
    val size = Point()
    display.getSize(size)
    screenDimension = Dimension(size.x, size.y)
    return screenDimension as Dimension
}

fun dpToPx(dp: Int) = (dp * Resources.getSystem().displayMetrics.density).toInt()