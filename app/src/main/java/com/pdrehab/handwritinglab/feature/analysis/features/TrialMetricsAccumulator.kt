package com.pdrehab.handwritinglab.feature.analysis.features

import com.pdrehab.handwritinglab.feature.taskrunner.input.MotionSample
import kotlin.math.sqrt

enum class UnitKind { CLUSTER, BOX, TASK }

data class BBoxPx(var minX: Float, var minY: Float, var maxX: Float, var maxY: Float) {
    fun w(): Float = (maxX - minX).coerceAtLeast(0f)
    fun h(): Float = (maxY - minY).coerceAtLeast(0f)
    fun update(x: Float, y: Float) {
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
    }
}

class TrialMetricsAccumulator(
    private val unitKind: UnitKind,
    private val conv: PxToMm,
    private val pressureMvc: Double?,
    private val baselineLinesY: Pair<Float, Float>? = null,
    private val onClusterClosed: ((clusterId: Int, bbox: BBoxPx, durationMs: Long) -> Unit)? = null
) {
    private var lastSample: MotionSample? = null
    private var lastTimeMs: Long? = null
    private var lastIsDown: Boolean = false

    var onSurfaceTimeMs: Long = 0
        private set
    var inAirTimeMs: Long = 0
        private set

    private var totalPathMm: Double = 0.0
    private var strokeCount: Int = 0

    private var pressureSum: Double = 0.0
    private var pressureN: Long = 0

    private val unitOrder = ArrayList<Int>()
    private val unitPath = HashMap<Int, Double>()

    private val clusterBbox = HashMap<Int, BBoxPx>()
    private val clusterStartMs = HashMap<Int, Long>()
    private val clusterEndMs = HashMap<Int, Long>()
    private var currentClusterId: Int? = null

    // baseline deviation
    private var devSqSum: Double = 0.0
    private var devN: Long = 0
    private var insideN: Long = 0
    private var guideTotalN: Long = 0

    fun onSample(s: MotionSample) {
        // time split
        val lt = lastTimeMs
        if (lt != null) {
            val dt = (s.tMs - lt).coerceAtLeast(0L)
            if (lastIsDown) onSurfaceTimeMs += dt else inAirTimeMs += dt
        }
        lastTimeMs = s.tMs
        lastIsDown = s.isDown

        // stroke count
        if (s.action == "DOWN") strokeCount += 1

        // pressure
        if (s.isDown) {
            val norm = if (pressureMvc != null && pressureMvc > 0.0) (s.pressure.toDouble() / pressureMvc) else s.pressure.toDouble()
            pressureSum += norm
            pressureN += 1
        }

        // unit id
        val uid = when (unitKind) {
            UnitKind.CLUSTER -> s.clusterId
            UnitKind.BOX -> s.boxId
            UnitKind.TASK -> 0
        }

        if (unitKind != UnitKind.TASK && uid >= 0) {
            if (!unitPath.containsKey(uid)) {
                unitPath[uid] = 0.0
                unitOrder.add(uid)
            }
        }

        // path length
        val prev = lastSample
        if (prev != null && prev.isDown && s.isDown) {
            val prevUid = when (unitKind) {
                UnitKind.CLUSTER -> prev.clusterId
                UnitKind.BOX -> prev.boxId
                UnitKind.TASK -> 0
            }
            if (prevUid == uid) {
                val d = conv.distMm(s.xPx - prev.xPx, s.yPx - prev.yPx)
                totalPathMm += d
                if (unitKind != UnitKind.TASK && uid >= 0) {
                    unitPath[uid] = (unitPath[uid] ?: 0.0) + d
                }
            }
        }

        // baseline deviation & inside ratio
        baselineLinesY?.let { (y1, y2) ->
            if (s.isDown) {
                guideTotalN += 1
                if (s.yPx in y1..y2) {
                    insideN += 1
                } else {
                    val dy = if (s.yPx < y1) (y1 - s.yPx) else (s.yPx - y2)
                    val mm = conv.hMm(dy)
                    devSqSum += mm * mm
                    devN += 1
                }
            }
        }

        // cluster bbox tracking (micrographia)
        if (unitKind == UnitKind.CLUSTER && s.isDown) {
            val cid = s.clusterId
            // cluster transition close
            val cur = currentClusterId
            if (cur != null && cid != cur) {
                closeCluster(cur)
            }
            currentClusterId = cid

            val bb = clusterBbox.getOrPut(cid) { BBoxPx(s.xPx, s.yPx, s.xPx, s.yPx) }
            bb.update(s.xPx, s.yPx)

            clusterStartMs.putIfAbsent(cid, s.tMs)
            clusterEndMs[cid] = s.tMs
        }

        lastSample = s
    }

    fun finalizeNow() {
        // close last cluster
        currentClusterId?.let { closeCluster(it) }
        currentClusterId = null
    }

    private fun closeCluster(cid: Int) {
        val bb = clusterBbox[cid] ?: return
        val st = clusterStartMs[cid] ?: return
        val et = clusterEndMs[cid] ?: st
        val dur = (et - st).coerceAtLeast(0L)
        onClusterClosed?.invoke(cid, bb, dur)
    }

    fun buildWritingMetrics(countNextScreen: Int, pageCount: Int): Map<String, Double?> {
        val out = LinkedHashMap<String, Double?>()

        // SIZE_REDUCTION_PCT
        val sr = computeSizeReductionPct()
        out["SIZE_REDUCTION_PCT"] = sr

        // speed
        val speed = if (onSurfaceTimeMs > 0) totalPathMm / (onSurfaceTimeMs.toDouble() / 1000.0) else null
        out["MEAN_SPEED_MMPS"] = speed

        // pressure norm
        out["MEAN_PRESSURE_NORM"] = if (pressureN > 0) (pressureSum / pressureN.toDouble()) else null

        out["IN_AIR_TIME_MS"] = inAirTimeMs.toDouble()
        out["ON_SURFACE_TIME_MS"] = onSurfaceTimeMs.toDouble()
        out["ON_SURFACE_TO_INAIR_RATIO"] =
            if (inAirTimeMs > 0) onSurfaceTimeMs.toDouble() / inAirTimeMs.toDouble() else null

        val total = onSurfaceTimeMs + inAirTimeMs
        out["HOVER_RATIO_PCT"] = if (total > 0) (inAirTimeMs.toDouble() / total.toDouble()) * 100.0 else null

        out["STROKE_COUNT"] = strokeCount.toDouble()
        out["COUNT_NEXT_SCREEN"] = countNextScreen.toDouble()
        out["PAGE_COUNT"] = pageCount.toDouble()

        // baseline deviation
        if (baselineLinesY != null) {
            out["BASELINE_DEV_RMS_MM"] = if (devN > 0) sqrt(devSqSum / devN.toDouble()) else 0.0
            out["INSIDE_GUIDE_RATIO_PCT"] =
                if (guideTotalN > 0) (insideN.toDouble() / guideTotalN.toDouble()) * 100.0 else null
        }

        return out
    }

    private fun computeSizeReductionPct(): Double? {
        // firstK=5 lastK=5, unitCount < 10 => NA
        if (unitKind == UnitKind.TASK) return null
        val values = unitOrder.mapNotNull { unitPath[it] }
        if (values.size < 10) return null
        val first = values.take(5).average()
        val last = values.takeLast(5).average()
        if (first == 0.0) return null
        return ((first - last) / first) * 100.0
    }
}