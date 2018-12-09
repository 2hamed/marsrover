package com.hmomeni.marsrover

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.hmomeni.marsrover.utils.dpToPx
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.min

const val UP = 0
const val RIGHT = 1
const val LEFT = 2
const val DOWN = 3

class RoverView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    private var roverUp: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.rover_up)
    private var roverRight: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.rover_right)
    private var roverLeft: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.rover_left)
    private var roverDown: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.rover_down)
    private var textBubble: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.bubble)

    private var cellWidth = dpToPx(24).toFloat()
    private val cellPadding = dpToPx(4).toFloat()
    private val cellPaint = Paint().apply {
        color = Color.GREEN
    }
    private val blockedCellPaint = Paint().apply {
        color = Color.RED
    }

    private val messagePaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textSize = dpToPx(14).toFloat()
    }

    var blockedCells = Array(20) {
        return@Array Array(10) {
            false
        }
    }


    var roverPosition = Point(0, 0)
    private lateinit var roverRect: RectF

    fun reset() {
        textBubbleRect = null
        direction = UP
        blockedCells = Array(20) {
            return@Array Array(10) {
                false
            }
        }
        invalidate()
    }

    fun updateLayout(startPoint: Point, blocks: List<Point>) {
        roverPosition = Point(startPoint)
        blocks.forEach {
            blockedCells[it.y][it.x] = true
        }
        calculateRoverRect()
        invalidate()
    }

    private fun calculateRoverRect() {
        roverRect = RectF(
            roverPosition.x * cellWidth + cellPadding,
            (19 - roverPosition.y) * cellWidth + cellPadding,
            roverPosition.x * cellWidth + cellWidth,
            (19 - roverPosition.y) * cellWidth + cellWidth
        )
    }

    private val viewRect: RectF = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (measuredWidth > 0 && measuredHeight > 0) {
            cellWidth = min((measuredWidth - 11 * cellPadding) / 9, (measuredHeight - 11 * cellPadding) / 19)
            setMeasuredDimension(measuredWidth, measuredHeight)
            viewRect.set(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
            calculateRoverRect()
        }
    }

    override fun onDraw(canvas: Canvas) {
        for (i in 0..19) {
            for (j in 0..9) {
                if (blockedCells[19 - i][j]) {
                    canvas.drawRect(
                        j * cellWidth + cellPadding,
                        i * cellWidth + cellPadding,
                        j * cellWidth + cellWidth,
                        i * cellWidth + cellWidth,
                        blockedCellPaint
                    )
                } else {
                    canvas.drawRect(
                        j * cellWidth + cellPadding,
                        i * cellWidth + cellPadding,
                        j * cellWidth + cellWidth,
                        i * cellWidth + cellWidth,
                        cellPaint
                    )
                }
                if (19 - i == roverPosition.y && j == roverPosition.x) {
                    canvas.drawBitmap(
                        getRoverBitmap(),
                        null,
                        roverRect,
                        null
                    )
                }
            }
        }
        textBubbleRect?.let {
            canvas.drawBitmap(textBubble, null, it, null)
            canvas.drawText(message, textPos!!.first, textPos!!.second, messagePaint)
        }

    }

    private var textBubbleRect: RectF? = null
    private var textPos: Pair<Float, Float>? = null
    private var message: String = ""


    fun showMessage(cellXY: Point, message: String) {
        val bounds = Rect()
        messagePaint.getTextBounds(message, 0, message.length, bounds)

        this.message = message
        var top: Float = (19 - cellXY.y) * cellWidth + cellPadding - 200
        var left: Float = (cellXY.x + 1) * cellWidth + cellPadding
        var right = left + (bounds.width() + 100)
        var bottom = top + 150

        if (top < 0) {
            top += abs(top)
            bottom = top + 200
        }

        if (right > measuredWidth) {
            left -= right - measuredWidth
            right = left + (bounds.width() + 100)
        }


        textBubbleRect = RectF(
            left,
            top,
            right,
            bottom
        )

        textPos = Pair(textBubbleRect!!.left + 50, textBubbleRect!!.centerY())

        invalidate()

    }

    private fun getRoverBitmap() = when (direction) {
        UP -> roverUp
        RIGHT -> roverRight
        LEFT -> roverLeft
        DOWN -> roverDown
        else -> throw RuntimeException("Invalid direction")
    }

    private var direction = UP
    private fun turnRight() {
        direction = when (direction) {
            UP -> RIGHT
            RIGHT -> DOWN
            DOWN -> LEFT
            LEFT -> UP
            else -> UP
        }
        invalidate()
    }


    private fun turnLeft() {
        direction = when (direction) {
            UP -> LEFT
            LEFT -> DOWN
            DOWN -> RIGHT
            RIGHT -> UP
            else -> UP
        }
        invalidate()
    }

    private fun moveOneCell() {
        if (!checkPath()) {
            cancelMovement = true
            showMessage(roverPosition, "Hey, I can't go that way!")
            return
        }
        when (direction) {
            UP -> roverPosition.y += 1
            RIGHT -> roverPosition.x += 1
            LEFT -> roverPosition.x -= 1
            DOWN -> roverPosition.y -= 1
            else -> throw RuntimeException("Invalid direction")
        }

        calculateRoverRect()
        invalidate()
    }

    private var cancelMovement = false
    private fun checkPath(): Boolean {
        val nextPos = Point(roverPosition)
        when (direction) {
            UP -> nextPos.y += 1
            RIGHT -> nextPos.x += 1
            LEFT -> nextPos.x -= 1
            DOWN -> nextPos.y -= 1
            else -> throw RuntimeException("Invalid direction")
        }

        return when {
            nextPos.y !in 0..19 -> false
            nextPos.x !in 0..9 -> false
            blockedCells[nextPos.y][nextPos.x] -> false
            else -> true
        }
    }

    fun processCommand(commands: String) {
        textBubbleRect = null
        thread {
            for (c in commands) {
                if (cancelMovement) {
                    cancelMovement = false
                    return@thread
                }
                if (handler == null) {
                    return@thread
                }
                when (c) {
                    'M' -> handler.post { moveOneCell() }
                    'R' -> handler.post { turnRight() }
                    'L' -> handler.post { turnLeft() }
                }
                Thread.sleep(300)
            }
        }
    }

}