package com.pdrehab.handwritinglab

import com.pdrehab.handwritinglab.feature.analysis.features.PxToMm
import com.pdrehab.handwritinglab.feature.analysis.features.TrialMetricsAccumulator
import com.pdrehab.handwritinglab.feature.analysis.features.UnitKind
import com.pdrehab.handwritinglab.feature.analysis.micrographia.MicrographiaDetector
import com.pdrehab.handwritinglab.feature.taskrunner.input.MotionSample
import org.junit.Assert.*
import org.junit.Test

class MicrographiaAndSizeReductionTest {

    @Test fun micrographia_baseline_then_active() {
        val det = MicrographiaDetector()
        // baseline 3 clusters
        assertNull(det.onCluster(10.0, 100))
        assertNull(det.onCluster(10.0, 100))
        assertNull(det.onCluster(10.0, 100)) // baseline formed here

        // rolling window needs 3 entries
        assertNull(det.onCluster(7.0, 100))
        assertNull(det.onCluster(7.0, 100))
        val upd = det.onCluster(7.0, 100)
        assertNotNull(upd)
        // baseline 10, threshold 7.5 => 7.0 should be active
        assertTrue(upd!!.active)
    }

    @Test fun micrographia_confirm_trigger_after_3_active_clusters() {
        val det = MicrographiaDetector()
        det.onCluster(10.0, 100); det.onCluster(10.0, 100); det.onCluster(10.0, 100)
        det.onCluster(7.0, 100); det.onCluster(7.0, 100)
        val u1 = det.onCluster(7.0, 100)!! // active #1
        det.onCluster(7.0, 100) // active #2
        val u3 = det.onCluster(7.0, 100)!! // active #3 => confirmed
        assertTrue(u1.active)
        assertTrue(u3.confirmedTriggered)
        assertTrue(u3.confirmedCount >= 1)
    }

    @Test fun size_reduction_na_if_units_lt_10() {
        val conv = PxToMm(160f, 160f)
        val acc = TrialMetricsAccumulator(UnitKind.CLUSTER, conv, pressureMvc = null)
        // 9 clusters only -> NA
        for (cid in 1..9) {
            acc.onSample(sampleDown(clusterId = cid, x = 10f * cid, y = 10f))
            acc.onSample(sampleMove(clusterId = cid, x = 10f * cid + 5f, y = 10f))
        }
        acc.finalizeNow()
        val m = acc.buildWritingMetrics(0, 1)
        assertNull(m["SIZE_REDUCTION_PCT"])
    }

    @Test fun size_reduction_computable_if_units_ge_10() {
        val conv = PxToMm(160f, 160f)
        val acc = TrialMetricsAccumulator(UnitKind.CLUSTER, conv, pressureMvc = null)
        // 10 clusters -> enough
        for (cid in 1..10) {
            acc.onSample(sampleDown(clusterId = cid, x = 10f * cid, y = 10f))
            acc.onSample(sampleMove(clusterId = cid, x = 10f * cid + 20f, y = 10f)) // longer strokes
        }
        acc.finalizeNow()
        val m = acc.buildWritingMetrics(0, 1)
        // 값이 null이 아니면 통과
        assertNotNull(m["SIZE_REDUCTION_PCT"])
    }

    // helper
    private fun sampleDown(clusterId: Int, x: Float, y: Float) = MotionSample(
        tNs = 0, tMs = 0,
        xPx = x, yPx = y,
        pressure = 1f, tilt = 0f, orientation = 0f, distance = 0f,
        action = "DOWN", toolType = "STYLUS", pointerId = 0,
        isDown = true, pageIndex = 0, boxId = -1, strokeId = 1, clusterId = clusterId
    )

    private fun sampleMove(clusterId: Int, x: Float, y: Float) = MotionSample(
        tNs = 0, tMs = 10,
        xPx = x, yPx = y,
        pressure = 1f, tilt = 0f, orientation = 0f, distance = 0f,
        action = "MOVE", toolType = "STYLUS", pointerId = 0,
        isDown = true, pageIndex = 0, boxId = -1, strokeId = 1, clusterId = clusterId
    )
}