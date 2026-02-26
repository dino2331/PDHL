package com.pdrehab.handwritinglab.feature.taskrunner.input

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class PressureTargetView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.LTGRAY }
    private val paintProg = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 18f; color = Color.DKGRAY }

    private var collecting = false
    private var startMs: Long = 0L
    private val pressures = ArrayList<Float>()
    private var progress01: Float = 0f

    var onDone: ((List<Float>) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)
        val r = 0.22f * min(width.toFloat(), height.toFloat())
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, r, paint)

        // progress arc
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(rect, -90f, 360f * progress01, false, paintProg)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val tool = event.getToolType(0)
        val isStylus = tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER
        if (!isStylus) return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                collecting = true
                startMs = SystemClock.uptimeMillis()
                pressures.clear()
                progress01 = 0f
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!collecting) return true
                pressures.add(event.pressure)
                val elapsed = SystemClock.uptimeMillis() - startMs
                progress01 = (elapsed / 2000f).coerceIn(0f, 1f)
                invalidate()
                if (elapsed >= 2000L) {
                    collecting = false
                    onDone?.invoke(pressures.toList())
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 손 떼면 중단(재시도 유도)
                collecting = false
                progress01 = 0f
                invalidate()
                return true
            }
        }
        return true
    }
}