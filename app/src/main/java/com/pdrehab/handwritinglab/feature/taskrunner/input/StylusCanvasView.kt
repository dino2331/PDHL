package com.pdrehab.handwritinglab.feature.taskrunner.input

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.pdrehab.handwritinglab.feature.taskrunner.guides.GuideGeometry
import com.pdrehab.handwritinglab.feature.taskrunner.guides.GuideType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import android.os.SystemClock

class StylusCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var guideType: GuideType = GuideType.NONE
    private var hard: Boolean = false
    private var inkColor: Int = Color.BLACK
    private var pageIndex: Int = 0
    private var inputEnabled: Boolean = true

    private var onSample: ((MotionSample) -> Unit)? = null
    private var onNonStylus: (() -> Unit)? = null
    private var onSize: ((Int, Int) -> Unit)? = null

    private val paintInk = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = inkColor
    }
    private val paintGuide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.GRAY
    }
    private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(30, 0, 0, 255)
    }
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 36f
    }

    private val paths = ArrayList<Path>()
    private var curPath: Path? = null

    // stroke/cluster
    private var strokeId = 0
    private var clusterId = 0
    private var lastUpMs: Long = 0L

    private var clusterMinX = Float.POSITIVE_INFINITY
    private var clusterMinY = Float.POSITIVE_INFINITY
    private var clusterMaxX = Float.NEGATIVE_INFINITY
    private var clusterMaxY = Float.NEGATIVE_INFINITY
    private var lastDownY = 0f

    // cached geometry
    private var rectsBoxes12: List<com.pdrehab.handwritinglab.feature.taskrunner.guides.RectF> = emptyList()
    private var rectsVariable: List<com.pdrehab.handwritinglab.feature.taskrunner.guides.RectF> = emptyList()
    private var rectsNumbered: List<com.pdrehab.handwritinglab.feature.taskrunner.guides.RectF> = emptyList()
    private var tracePath: List<com.pdrehab.handwritinglab.feature.taskrunner.guides.Pt> = emptyList()
    private var appleTarget: com.pdrehab.handwritinglab.feature.taskrunner.guides.RectF? = null
    private var appleRadius: Float = 0f
    private var appleCenterX: Float = -1f
    private var appleCenterY: Float = -1f

    private var arrowUntilUptimeMs: Long = 0L

    fun showStartArrow(durationMs: Long = 1000L) {
        arrowUntilUptimeMs = SystemClock.uptimeMillis() + durationMs
        invalidate()
    }

    fun setGuideType(t: GuideType) {
        if (guideType != t) {
            guideType = t
            recomputeGeometry()
            invalidate()
        }
    }

    fun setHard(v: Boolean) {
        if (hard != v) {
            hard = v
            recomputeGeometry()
            invalidate()
        }
    }

    fun setInkColorArgb(argb: Int) {
        inkColor = argb
        paintInk.color = inkColor
        invalidate()
    }

    fun setPageIndex(v: Int) { pageIndex = v }
    fun setInputEnabled(v: Boolean) { inputEnabled = v }

    fun clearCanvas() {
        paths.clear()
        curPath = null
        invalidate()
    }

    fun setAppleCenter(x: Float, y: Float) {
        appleCenterX = x
        appleCenterY = y
        invalidate()
    }

    fun setOnSampleListener(cb: (MotionSample) -> Unit) { onSample = cb }
    fun setOnNonStylusListener(cb: () -> Unit) { onNonStylus = cb }
    fun setOnSizeListener(cb: (Int, Int) -> Unit) { onSize = cb }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recomputeGeometry()
        onSize?.invoke(w, h)
    }

    private fun recomputeGeometry() {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        rectsBoxes12 = GuideGeometry.boxesRow12(w, h)
        rectsVariable = GuideGeometry.variableBoxes(w, h, hard)
        rectsNumbered = GuideGeometry.numberedBoxes(w, h)
        tracePath = GuideGeometry.tracePath(w, h, hard)
        appleTarget = GuideGeometry.appleTargetRect(w, h, hard)
        appleRadius = GuideGeometry.appleRadius(w, h, hard)

        if (appleCenterX < 0f || appleCenterY < 0f) {
            val p = GuideGeometry.appleStartCenter(w, h)
            appleCenterX = p.x
            appleCenterY = p.y
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)

        // guides
        when (guideType) {
            GuideType.TWO_HORIZONTAL_LINES -> {
                val (y1, y2) = GuideGeometry.twoLinesY(height.toFloat())
                canvas.drawLine(0f, y1, width.toFloat(), y1, paintGuide)
                canvas.drawLine(0f, y2, width.toFloat(), y2, paintGuide)
            }
            GuideType.TOP_DOTS -> {
                val dots = GuideGeometry.topDots(width.toFloat(), height.toFloat())
                val r = 8f
                for (p in dots) canvas.drawCircle(p.x, p.y, r, paintGuide)
            }
            GuideType.BOXES_ROW_12 -> {
                for (r in rectsBoxes12) {
                    canvas.drawRect(RectF(r.left, r.top, r.right, r.bottom), paintGuide)
                }
            }
            GuideType.VARIABLE_BOXES_LINE -> {
                for (r in rectsVariable) {
                    canvas.drawRect(RectF(r.left, r.top, r.right, r.bottom), paintGuide)
                }
            }
            GuideType.NUMBERED_BOXES -> {
                for (i in rectsNumbered.indices) {
                    val r = rectsNumbered[i]
                    canvas.drawRect(RectF(r.left, r.top, r.right, r.bottom), paintGuide)
                    canvas.drawText((i + 1).toString(), r.left + 12f, r.top + 42f, paintText)
                }
            }
            GuideType.TRACE_PATH -> {
                val p = Paint(paintGuide).apply { strokeWidth = if (!hard) 10f else 6f }
                for (i in 0 until tracePath.size - 1) {
                    val a = tracePath[i]
                    val b = tracePath[i + 1]
                    canvas.drawLine(a.x, a.y, b.x, b.y, p)
                }
                if (tracePath.isNotEmpty()) {
                    val s = tracePath.first()
                    val e = tracePath.last()
                    val r = 14f
                    val paintS = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.BLUE }
                    val paintE = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.RED }
                    canvas.drawCircle(s.x, s.y, r, paintS)
                    canvas.drawCircle(e.x, e.y, r, paintE)
                }
            }
            GuideType.GAME_APPLE -> {
                appleTarget?.let { t ->
                    val rr = RectF(t.left, t.top, t.right, t.bottom)
                    canvas.drawRect(rr, paintFill)
                    canvas.drawRect(rr, paintGuide)
                }
                val paintApple = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.rgb(200, 30, 30) }
                canvas.drawCircle(appleCenterX, appleCenterY, appleRadius, paintApple)
            }
            else -> Unit
        }

        // ink
        for (p in paths) canvas.drawPath(p, paintInk)
        curPath?.let { canvas.drawPath(it, paintInk) }

        val now = SystemClock.uptimeMillis()
        if (now < arrowUntilUptimeMs) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.DKGRAY }
            val x = 0.10f * width.toFloat()
            val y = 0.50f * height.toFloat()
            val path = Path().apply {
                moveTo(x, y)
                lineTo(x + 40f, y - 24f)
                lineTo(x + 40f, y + 24f)
                close()
            }
            canvas.drawPath(path, p)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!inputEnabled) return true

        val tool = event.getToolType(0)
        val isStylus = tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER
        if (!isStylus) {
            onNonStylus?.invoke()
            return true
        }

        val actionMasked = event.actionMasked
        val pointerId = event.getPointerId(0)
        val toolName = if (tool == MotionEvent.TOOL_TYPE_ERASER) "ERASER" else "STYLUS"

        // history 먼저
        val historySize = event.historySize
        for (i in 0 until historySize) {
            val tMs = event.getHistoricalEventTime(i)
            val x = event.getHistoricalX(0, i)
            val y = event.getHistoricalY(0, i)
            val pr = event.getHistoricalPressure(0, i)
            val tilt = histAxis(event, MotionEvent.AXIS_TILT, 0, i)
            val ori = histAxis(event, MotionEvent.AXIS_ORIENTATION, 0, i)
            val dist = histAxis(event, MotionEvent.AXIS_DISTANCE, 0, i)

            emitSample(tMs, x, y, pr, tilt, ori, dist, actionMasked, toolName, pointerId)

            if (actionMasked == MotionEvent.ACTION_MOVE) {
                curPath?.lineTo(x, y)
                invalidate()
            }
        }

        val tMs = event.eventTime
        val x = event.getX(0)
        val y = event.getY(0)
        val pr = event.getPressure(0)
        val tilt = axis(event, MotionEvent.AXIS_TILT, 0)
        val ori = axis(event, MotionEvent.AXIS_ORIENTATION, 0)
        val dist = axis(event, MotionEvent.AXIS_DISTANCE, 0)

        emitSample(tMs, x, y, pr, tilt, ori, dist, actionMasked, toolName, pointerId)

        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                strokeId += 1
                if (shouldStartNewCluster(tMs, x, y)) {
                    clusterId += 1
                    resetClusterBbox(x, y)
                } else {
                    extendClusterBbox(x, y)
                }
                lastDownY = y

                curPath = Path().apply { moveTo(x, y) }
                paths.add(curPath!!)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                extendClusterBbox(x, y)
                curPath?.lineTo(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                extendClusterBbox(x, y)
                curPath?.lineTo(x, y)
                curPath = null
                lastUpMs = tMs
                invalidate()
            }
        }

        return true
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        if (!inputEnabled) return true

        val tool = event.getToolType(0)
        val isStylus = tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER
        if (!isStylus) return true

        val pointerId = event.getPointerId(0)
        val toolName = if (tool == MotionEvent.TOOL_TYPE_ERASER) "ERASER" else "STYLUS"

        val historySize = event.historySize
        for (i in 0 until historySize) {
            val tMs = event.getHistoricalEventTime(i)
            val x = event.getHistoricalX(0, i)
            val y = event.getHistoricalY(0, i)
            val pr = event.getHistoricalPressure(0, i)
            val tilt = histAxis(event, MotionEvent.AXIS_TILT, 0, i)
            val ori = histAxis(event, MotionEvent.AXIS_ORIENTATION, 0, i)
            val dist = histAxis(event, MotionEvent.AXIS_DISTANCE, 0, i)
            emitHoverSample(tMs, x, y, pr, tilt, ori, dist, toolName, pointerId)
        }

        val tMs = event.eventTime
        val x = event.getX(0)
        val y = event.getY(0)
        val pr = event.getPressure(0)
        val tilt = axis(event, MotionEvent.AXIS_TILT, 0)
        val ori = axis(event, MotionEvent.AXIS_ORIENTATION, 0)
        val dist = axis(event, MotionEvent.AXIS_DISTANCE, 0)
        emitHoverSample(tMs, x, y, pr, tilt, ori, dist, toolName, pointerId)

        return true
    }

    private fun emitSample(
        tMs: Long,
        x: Float,
        y: Float,
        pressure: Float,
        tilt: Float,
        orientation: Float,
        distance: Float,
        actionMasked: Int,
        toolName: String,
        pointerId: Int
    ) {
        val action = when (actionMasked) {
            MotionEvent.ACTION_DOWN -> "DOWN"
            MotionEvent.ACTION_MOVE -> "MOVE"
            MotionEvent.ACTION_UP -> "UP"
            MotionEvent.ACTION_CANCEL -> "CANCEL"
            else -> "MOVE"
        }
        val isDown = action == "DOWN" || action == "MOVE"

        val boxId = when (guideType) {
            GuideType.BOXES_ROW_12 -> GuideGeometry.boxIdFromRects(rectsBoxes12, x, y)
            GuideType.VARIABLE_BOXES_LINE -> GuideGeometry.boxIdFromRects(rectsVariable, x, y)
            GuideType.NUMBERED_BOXES -> GuideGeometry.boxIdFromRects(rectsNumbered, x, y)
            else -> -1
        }

        val sample = MotionSample(
            tNs = System.nanoTime(),
            tMs = tMs,
            xPx = x,
            yPx = y,
            pressure = pressure,
            tilt = tilt,
            orientation = orientation,
            distance = distance,
            action = action,
            toolType = toolName,
            pointerId = pointerId,
            isDown = isDown,
            pageIndex = pageIndex,
            boxId = boxId,
            strokeId = strokeId,
            clusterId = max(clusterId, 1)
        )
        onSample?.invoke(sample)
    }

    private fun emitHoverSample(
        tMs: Long,
        x: Float,
        y: Float,
        pressure: Float,
        tilt: Float,
        orientation: Float,
        distance: Float,
        toolName: String,
        pointerId: Int
    ) {
        val boxId = when (guideType) {
            GuideType.BOXES_ROW_12 -> GuideGeometry.boxIdFromRects(rectsBoxes12, x, y)
            GuideType.VARIABLE_BOXES_LINE -> GuideGeometry.boxIdFromRects(rectsVariable, x, y)
            GuideType.NUMBERED_BOXES -> GuideGeometry.boxIdFromRects(rectsNumbered, x, y)
            else -> -1
        }
        val sample = MotionSample(
            tNs = System.nanoTime(),
            tMs = tMs,
            xPx = x,
            yPx = y,
            pressure = pressure,
            tilt = tilt,
            orientation = orientation,
            distance = distance,
            action = "HOVER_MOVE",
            toolType = toolName,
            pointerId = pointerId,
            isDown = false,
            pageIndex = pageIndex,
            boxId = boxId,
            strokeId = strokeId,
            clusterId = max(clusterId, 1)
        )
        onSample?.invoke(sample)
    }

    private fun axis(e: MotionEvent, axis: Int, pointerIndex: Int): Float =
        try { e.getAxisValue(axis, pointerIndex) } catch (_: Throwable) { 0f }

    private fun histAxis(e: MotionEvent, axis: Int, pointerIndex: Int, histIndex: Int): Float =
        try { e.getHistoricalAxisValue(axis, pointerIndex, histIndex) } catch (_: Throwable) { 0f }

    private fun shouldStartNewCluster(tMs: Long, x: Float, y: Float): Boolean {
        val gapThresholdMs = 350L
        if (clusterId <= 0) return true
        if (lastUpMs > 0 && (tMs - lastUpMs) > gapThresholdMs) return true

        val w = (clusterMaxX - clusterMinX).takeIf { it.isFinite() } ?: 0f
        val h = (clusterMaxY - clusterMinY).takeIf { it.isFinite() } ?: 0f
        val margin = 0.35f * max(w, h)

        val inInflated =
            x >= (clusterMinX - margin) && x <= (clusterMaxX + margin) &&
                    y >= (clusterMinY - margin) && y <= (clusterMaxY + margin)

        if (!inInflated) return true

        if (h > 1f && abs(y - lastDownY) > 1.5f * h) return true
        return false
    }

    private fun resetClusterBbox(x: Float, y: Float) {
        clusterMinX = x
        clusterMaxX = x
        clusterMinY = y
        clusterMaxY = y
    }

    private fun extendClusterBbox(x: Float, y: Float) {
        clusterMinX = min(clusterMinX, x)
        clusterMaxX = max(clusterMaxX, x)
        clusterMinY = min(clusterMinY, y)
        clusterMaxY = max(clusterMaxY, y)
    }


}