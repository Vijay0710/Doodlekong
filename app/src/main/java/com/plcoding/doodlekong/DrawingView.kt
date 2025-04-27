package com.plcoding.doodlekong

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import androidx.annotation.ColorInt
import com.plcoding.doodlekong.data.remote.ws.models.DrawData
import com.plcoding.doodlekong.utils.Constants
import java.util.Stack
import kotlin.math.abs

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private var viewWidth: Int? = null
    private var viewHeight: Int? = null
    private var bmp: Bitmap? = null
    private var canvas: Canvas? = null

    private var curX: Float? = null
    private var curY: Float? = null

    var smoothness = 5
    var isDrawing = false

    private var paint = Paint(Paint.DITHER_FLAG).apply {
        isDither = true
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = Constants.DEFAULT_PAINT_THICKNESS
    }

    private var path = Path()
    private var paths = Stack<PathData>()
    private var pathDataChangedListener: ((Stack<PathData>) -> Unit)? = null

    private var onDrawListener: ((DrawData) -> Unit)? = null
    var roomName: String? = null
    var isUserDrawing = false
        set(value) {
            isEnabled = value
            field = value
        }

    private var startedTouch = false

    fun setOnDrawListener(listener: (DrawData) -> Unit) {
        onDrawListener = listener
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        path.reset()
        invalidate()
    }

    fun undo() {
        if (paths.isNotEmpty()) {
            paths.pop()
            pathDataChangedListener?.let { change ->
                change(paths)
            }
            invalidate()
        }
    }


    fun setPathChangedListener(listener: (Stack<PathData>) -> Unit) {
        pathDataChangedListener = listener
    }

    data class PathData(
        val path: Path,
        val color: Int,
        val thickness: Float,
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bmp!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val initialColor = paint.color
        val initialThickness = paint.strokeWidth

        paths.forEach { pathData ->
            paint.apply {
                color = pathData.color
                strokeWidth = pathData.thickness
            }
            canvas.drawPath(pathData.path, paint)
        }

        paint.apply {
            color = initialColor
            strokeWidth = initialThickness
        }
        canvas.drawPath(path, paint)
    }

    fun startedTouchExternally(drawData: DrawData) {
        parseDrawData(drawData).apply {
            paint.color = color
            paint.strokeWidth = thickness
            path.reset()
            path.moveTo(fromX, fromY)
            invalidate()
            startedTouch = true
        }
    }

    fun movedTouchExternally(drawData: DrawData) {
        parseDrawData(drawData).apply {
            val dx = abs(toX - fromX)
            val dy = abs(toY - fromY)

            if (!startedTouch) {
                startedTouchExternally(drawData)
            }

            if (dx >= smoothness || dy >= smoothness) {
                isDrawing = true
                path.quadTo(fromX, fromY, (fromX + toX) / 2F, (fromY + toY) / 2F)
                invalidate()
            }
        }
    }

    fun releaseTouchExternally(drawData: DrawData) {
        parseDrawData(drawData).apply {
            path.lineTo(fromX, fromY)
            canvas?.drawPath(path, paint)

            // When user undoes we need to remove the top path so we push the path that the player drawn
            paths.push(
                PathData(
                    path = Path(path),
                    color = paint.color,
                    thickness = paint.strokeWidth
                )
            )

            pathDataChangedListener?.let { change ->
                change(paths)
            }

            path = Path()
            invalidate()
            startedTouch = false
        }
    }

    private fun startedTouch(x: Float, y: Float) {
        path.reset()
        path.moveTo(x, y)
        curX = x; curY = y
        onDrawListener?.let { draw ->
            val drawData = createDrawData(
                x, y, x, y, ACTION_DOWN
            )
            draw(drawData)
        }
        invalidate()
    }

    private fun movedTouch(toX: Float, toY: Float) {
        val dx = abs(toX - (curX ?: return))
        val dy = abs(toY - (curY ?: return))
        if (dx >= smoothness || dy >= smoothness) {
            isDrawing = true
            path.quadTo(curX!!, curY!!, (curX!! + toX) / 2F, (curY!! + toY) / 2F)

            onDrawListener?.let { draw ->
                val drawData = createDrawData(
                    curX!!, curY!!, toX, toY, ACTION_MOVE
                )
                draw(drawData)
            }

            curX = toX
            curY = toY
            invalidate()
        }
    }

    fun finishOffDrawing() {
        isDrawing = false
        path.lineTo(curX ?: return, curY ?: return)
        canvas?.drawPath(path, paint)
        paths.push(
            PathData(
                path,
                paint.color,
                paint.strokeWidth
            )
        )

        pathDataChangedListener?.let { change ->
            change(paths)
        }
        path = Path()
        invalidate()
    }

    private fun releasedTouch() {
        isDrawing = false
        path.lineTo(curX ?: return, curY ?: return)
        paths.push(
            PathData(
                path = path,
                color = paint.color,
                thickness = paint.strokeWidth
            )
        )
        pathDataChangedListener?.let { change ->
            change(paths)
        }

        onDrawListener?.let { draw ->
            val drawData = createDrawData(
                curX!!, curY!!, curX!!, curY!!, ACTION_UP
            )
            draw(drawData)
        }

        path = Path()
        path.reset()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!isEnabled) {
            return false
        }
        val newX = event?.x
        val newY = event?.y

        when (event?.action) {
            ACTION_DOWN -> {
                startedTouch(newX ?: return false, newY ?: return false)
            }

            ACTION_MOVE -> {
                movedTouch(newX ?: return false, newY ?: return false)
            }

            ACTION_UP -> {
                releasedTouch()
            }
        }
        return true
    }

    private fun createDrawData(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        motionEvent: Int
    ): DrawData {
        return DrawData(
            roomName ?: throw IllegalStateException("Must set the roomName in drawing view"),
            paint.color,
            paint.strokeWidth,
            fromX = fromX / viewWidth!!,
            fromY = fromY / viewHeight!!,
            toX = toX / viewWidth!!,
            toY = toY / viewHeight!!,
            motionEvent = motionEvent
        )
    }

    private fun parseDrawData(
        drawData: DrawData
    ): DrawData {
        return drawData.copy(
            fromX = drawData.fromX * viewWidth!!,
            fromY = drawData.fromY * viewHeight!!,
            toX = drawData.toX * viewWidth!!,
            toY = drawData.toY * viewHeight!!
        )
    }

    fun setThickness(thickness: Float) {
        paint.strokeWidth = thickness
    }

    fun setColor(@ColorInt color: Int) {
        paint.color = color
    }

    fun clear() {
        canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY)
        paths.clear()
    }


}