package com.hmomeni.marsrover.utils

/**
 * Created by hamed on 4/24/16.in neshan
 */
class Dimension : Cloneable {
    var width: Int = 0
    var height: Int = 0

    val isEitherZero: Boolean
        get() = width == 0 || height == 0

    constructor(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    constructor(widthXHeight: String) {
        try {
            val arr = widthXHeight.split("x".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            width = java.lang.Double.parseDouble(arr[0]).toInt()
            height = java.lang.Double.parseDouble(arr[1]).toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            width = 0
            height = 0
        }

    }

    public override fun clone(): Dimension {
        return Dimension(width, height)
    }

    override fun toString(): String {
        return "${width}x$height"
    }

    override fun equals(other: Any?): Boolean {
        return (other is Dimension && other.toString() == toString())
    }

    fun ratio(): Float = width.toFloat() / height.toFloat()

}
