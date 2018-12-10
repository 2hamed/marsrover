package com.hmomeni.marsrover

import android.content.Context
import android.graphics.*
import android.media.MediaPlayer
import android.text.TextPaint
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

const val SOUND_MOVE = 1
const val SOUND_BLOCK = 2
const val SOUND_LAZER = 3

class RoverView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    var roverListener: RoverListener? = null
    var roverPosition = Point(0, 0)
    var blockedCells = Array(20) {
        return@Array Array(10) {
            false
        }
    }

    private val mediaPlayer = MediaPlayer()

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

    private val messagePaint = TextPaint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textSize = dpToPx(14).toFloat()
    }

    private val lazerPaint = Paint().apply {
        color = Color.BLUE
    }
    private val lazerRect = RectF()
    private var showLazer = false

    private val viewRect: RectF = RectF()

    private lateinit var roverRect: RectF

    private var direction = UP
    private var textBubbleRect: RectF? = null
    private var textPos: Pair<Float, Float>? = null
    private var message: String = ""
    private var cancelMovement = false
    private var isBoulder = false
    private var boulderPosition: Point? = null


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


    fun showMessage(cellXY: Point, message: String) {
        val bounds = Rect()
        messagePaint.getTextBounds(message, 0, message.length, bounds)

        this.message = message
        var top: Float = (19 - cellXY.y) * cellWidth + cellPadding - (bounds.height() + 50)
        var left: Float = (cellXY.x + 1) * cellWidth + cellPadding
        var right = left + (bounds.width() + 100)
        var bottom = top + bounds.height() + 50

        // let's check that our message bubble doesn't exceeds the view boundaries
        // vertically
        if (top < 0) {
            top += abs(top)
            bottom = top + 200
        }

        // horizontally
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

    fun processCommand(commands: String) {
        textBubbleRect = null

        // we don't want to freeze the UI while waiting for our timeout
        thread {
            for (c in commands) {
                if (cancelMovement) {
                    cancelMovement = false
                    return@thread
                }

                // let's check that our view hasn't gone away while we were waiting
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

    private var lastPlayedSound = -1

    private fun playMoveSound() {
        if (lastPlayedSound == SOUND_MOVE) {
            mediaPlayer.seekTo(0)
        } else {
            val afd = context.resources.openRawResourceFd(R.raw.beep_short_on)
            mediaPlayer.reset()
            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            mediaPlayer.prepare()
        }
        mediaPlayer.start()
        lastPlayedSound = SOUND_MOVE
    }

    private fun playBlockSound() {
        if (lastPlayedSound == SOUND_BLOCK) {
            mediaPlayer.seekTo(0)
        } else {
            val afd = context.resources.openRawResourceFd(R.raw.zap_digi_up)
            mediaPlayer.reset()
            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            mediaPlayer.prepare()
        }
        mediaPlayer.start()
        lastPlayedSound = SOUND_BLOCK
    }

    private fun playLazerSound() {
        if (lastPlayedSound == SOUND_LAZER) {
            mediaPlayer.seekTo(0)
        } else {
            val afd = context.resources.openRawResourceFd(R.raw.zap_clang)
            mediaPlayer.reset()
            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            mediaPlayer.prepare()
        }
        mediaPlayer.start()
        lastPlayedSound = SOUND_LAZER
    }

    private fun calculateRoverRect() {
        roverRect = RectF(
            roverPosition.x * cellWidth + cellPadding,
            (19 - roverPosition.y) * cellWidth + cellPadding,
            roverPosition.x * cellWidth + cellWidth,
            (19 - roverPosition.y) * cellWidth + cellWidth
        )
    }

    private fun getRoverBitmap() = when (direction) {
        UP -> roverUp
        RIGHT -> roverRight
        LEFT -> roverLeft
        DOWN -> roverDown
        else -> throw RuntimeException("Invalid direction")
    }


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
            playBlockSound()
            cancelMovement = true
            showMessage(roverPosition, "Hey, I can't go that way!")
            if (isBoulder && roverListener != null) {
                postDelayed({
                    showMessage(roverPosition, "Wait! I can destroy this boulder with my laser!")
                    roverListener?.showLazerButton()
                }, 2000)
            }
            return
        }
        playMoveSound()
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

    private fun checkPath(): Boolean {
        val nextPos = Point(roverPosition)
        when (direction) {
            UP -> nextPos.y += 1
            RIGHT -> nextPos.x += 1
            LEFT -> nextPos.x -= 1
            DOWN -> nextPos.y -= 1
            else -> throw RuntimeException("Invalid direction")
        }
        isBoulder = false
        return when {
            nextPos.y !in 0..19 -> false
            nextPos.x !in 0..9 -> false
            blockedCells[nextPos.y][nextPos.x] -> {
                boulderPosition = nextPos
                isBoulder = true
                false
            }
            else -> true
        }
    }

    fun useLazer() {
        prepareLazer()
    }

    private fun prepareLazer() {
        val left: Float
        val top: Float
        val right: Float
        val bottom: Float
        when (direction) {
            RIGHT -> {
                left = roverRect.right
                top = roverRect.centerY() - 10
                right = left + viewRect.width()
                bottom = top + 10
            }
            LEFT -> {
                left = roverRect.left
                top = roverRect.centerY() - 10
                right = left - viewRect.width()
                bottom = top + 10
            }
            DOWN -> {
                left = roverRect.centerX() + 10
                top = roverRect.bottom
                right = left - 10
                bottom = top + viewRect.height()
            }
            UP -> {
                left = roverRect.centerX() - 10
                top = roverRect.top
                right = left + 10
                bottom = top - viewRect.height()
            }
            else -> throw RuntimeException()
        }

        lazerRect.set(left, top, right, bottom)
        showLazer = true

        blockedCells[boulderPosition!!.y][boulderPosition!!.x] = false

        invalidate()

        playLazerSound()

        postDelayed({
            showLazer = false
            invalidate()
        }, 300)

        roverListener?.hideLazerButton()
    }

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
        // let's create a 10x20 grid
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
                // here we detect if should draw the rover
                // and our origin is not the true graphical origin, hence the 19 - x
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

        if (showLazer) {
            canvas.drawRect(lazerRect, lazerPaint)
        }
    }

    interface RoverListener {
        fun showLazerButton()
        fun hideLazerButton()
    }
}