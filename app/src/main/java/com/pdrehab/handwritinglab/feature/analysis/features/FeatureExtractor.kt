package com.pdrehab.handwritinglab.feature.analysis.features

import android.content.res.Resources
import com.pdrehab.handwritinglab.feature.taskrunner.guides.GuideGeometry
import com.pdrehab.handwritinglab.feature.taskrunner.input.MotionSample
import kotlin.math.*

data class UnitFeat(
    val kind: String,   // CLUSTER|BOX
    val unitId: Int,    // clusterId or (pageIndex*100+boxId)
    val startMs: Long,
    val endMs: Long,
    val pathLengthMm: Double,
    val widthMm: Double,
    val heightMm: Double
)

data class WritingMetrics(
    val sizeReductionPct: Double?,
    val meanSpeedMmps: Double?,
    val meanPressureNorm: Double?,
    val inAirTimeMs: Double?,
    val onSurfaceTimeMs: Double?,
    val onSurfaceToInAirRatio: Double?,
    val strokeCount: Double?,
    val hoverRatioPct: Double?,
    val baselineDevRmsMm: Double?,
    val insideGuideRatioPct: Double?,
    val units: List<UnitFeat>
)

object FeatureExtractor {

    private fun pxToMmX(px: Float): Double {
        val dm = Resources.getSystem().displayMetrics
        return px.toDouble() / dm.xdpi.toDouble() * 25.4
    }
    private fun pxToMmY(px: Float): Double {
        val dm = Resources.getSystem().displayMetrics
        return px.toDouble() / dm.ydpi.toDouble() * 25.4
    }

    fun computeWriting(
        samples: List<MotionSample>,
        unitMode: String, // CLUSTER|BOX
        pressureMvc: Double?,
        canvasW: Int,
        canvasH: Int,
        computeBaselineDeviation: Boolean
    ): WritingMetrics {

        val sorted = samples.sortedBy { it.tMs }
        if (sorted.isEmpty()) {
            return WritingMetrics(null,null,null,null,null,null,null,null,null,null, emptyList())
        }

        // --- time split
        var onSurfaceMs = 0.0
        var inAirMs = 0.0
        for (i in 0 until sorted.size - 1) {
            val a = sorted[i]
            val b = sorted[i + 1]
            val dt = (b.tMs - a.tMs).coerceAtLeast(0L).toDouble()
            if (a.isDown) onSurfaceMs += dt else inAirMs += dt
        }
        val totalMs = onSurfaceMs + inAirMs
        val hoverRatioPct = if (totalMs > 0) (inAirMs / totalMs * 100.0) else null

        // --- path length + speed
        var pathMm = 0.0
        var prev: MotionSample? = null
        for (s in sorted) {
            if (prev != null && s.isDown && prev!!.isDown) {
                val dxMm = pxToMmX(s.xPx - prev!!.xPx)
                val dyMm = pxToMmY(s.yPx - prev!!.yPx)
                pathMm += sqrt(dxMm * dxMm + dyMm * dyMm)
            }
            prev = s
        }
        val meanSpeed = if (onSurfaceMs > 0) (pathMm / (onSurfaceMs / 1000.0)) else null

        // --- pressure norm
        val pressureNorm = if (pressureMvc != null && pressureMvc > 0) {
            val ps = sorted.filter { it.isDown }.map { it.pressure.toDouble() / pressureMvc }
            if (ps.isNotEmpty()) ps.average() else null
        } else null

        // --- stroke count
        val strokeCount = sorted.maxOfOrNull { it.strokeId }?.toDouble()

        // --- units
        val units = buildUnits(sorted, unitMode)

        // --- SIZE_REDUCTION_PCT
        val sizeReduction = run {
            if (units.size < 10) null
            else {
                val ordered = units.sortedBy { it.startMs }
                val first = ordered.take(5).map { it.pathLengthMm }
                val last = ordered.takeLast(5).map { it.pathLengthMm }
                val meanFirst = first.average()
                val meanLast = last.average()
                if (meanFirst <= 0) null else ((meanFirst - meanLast) / meanFirst * 100.0)
            }
        }

        // --- baseline deviation / inside guide
        var baselineDevRmsMm: Double? = null
        var insideRatio: Double? = null
        if (computeBaselineDeviation) {
            val (y1, y2) = GuideGeometry.twoLinesY(canvasH.toFloat())
            val yMid = (y1 + y2) / 2f
            val downs = sorted.filter { it.isDown }
            if (downs.isNotEmpty()) {
                val diffs = downs.map { pxToMmY(it.yPx - yMid).toDouble() }
                baselineDevRmsMm = sqrt(diffs.map { it * it }.average())
                val inside = downs.count { it.yPx in y1..y2 }.toDouble()
                insideRatio = inside / downs.size.toDouble() * 100.0
            }
        }

        return WritingMetrics(
            sizeReductionPct = sizeReduction,
            meanSpeedMmps = meanSpeed,
            meanPressureNorm = pressureNorm,
            inAirTimeMs = inAirMs,
            onSurfaceTimeMs = onSurfaceMs,
            onSurfaceToInAirRatio = if (inAirMs > 0) (onSurfaceMs / inAirMs) else null,
            strokeCount = strokeCount,
            hoverRatioPct = hoverRatioPct,
            baselineDevRmsMm = baselineDevRmsMm,
            insideGuideRatioPct = insideRatio,
            units = units
        )
    }

    private fun buildUnits(sorted: List<MotionSample>, unitMode: String): List<UnitFeat> {
        val downs = sorted.filter { it.isDown }
        if (downs.isEmpty()) return emptyList()

        return if (unitMode == "BOX") {
            val groups = downs.groupBy { it.pageIndex * 100 + it.boxId }
                .filterKeys { it % 100 in 0..11 }
            groups.map { (unitId, ss) -> unitFeat("BOX", unitId, ss) }
        } else {
            val groups = downs.groupBy { it.clusterId }
            groups.map { (cid, ss) -> unitFeat("CLUSTER", cid, ss) }
        }
    }

    private fun unitFeat(kind: String, unitId: Int, ss: List<MotionSample>): UnitFeat {
        val ordered = ss.sortedBy { it.tMs }
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var path = 0.0
        var prev: MotionSample? = null
        for (s in ordered) {
            minX = min(minX, s.xPx); maxX = max(maxX, s.xPx)
            minY = min(minY, s.yPx); maxY = max(maxY, s.yPx)
            if (prev != null) {
                val dx = pxToMmX(s.xPx - prev!!.xPx)
                val dy = pxToMmY(s.yPx - prev!!.yPx)
                path += sqrt(dx*dx + dy*dy)
            }
            prev = s
        }
        val wMm = pxToMmX(maxX - minX)
        val hMm = pxToMmY(maxY - minY)
        return UnitFeat(
            kind = kind,
            unitId = unitId,
            startMs = ordered.first().tMs,
            endMs = ordered.last().tMs,
            pathLengthMm = path,
            widthMm = wMm,
            heightMm = hMm
        )
    }
}