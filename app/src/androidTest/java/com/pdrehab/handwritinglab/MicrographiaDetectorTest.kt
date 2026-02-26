package com.pdrehab.handwritinglab

import com.pdrehab.handwritinglab.feature.analysis.micrographia.MicroParams
import com.pdrehab.handwritinglab.feature.analysis.micrographia.MicrographiaDetector
import org.junit.Assert.*
import org.junit.Test

class MicrographiaDetectorTest {

    @Test fun baseline_not_ready_before_3() {
        val d = MicrographiaDetector(MicroParams())
        assertFalse(d.onClusterSizeScore(10.0).active)
        assertFalse(d.onClusterSizeScore(10.0).active)
        assertFalse(d.isBaselineReady)
    }

    @Test fun becomes_active_when_rolling_mean_below_threshold() {
        val d = MicrographiaDetector(MicroParams())
        // baseline 3 clusters
        d.onClusterSizeScore(10.0)
        d.onClusterSizeScore(10.0)
        d.onClusterSizeScore(10.0)
        assertTrue(d.isBaselineReady)

        // rolling window 3 clusters: 7,7,7 -> mean=7 < 7.5 -> active
        d.onClusterSizeScore(7.0)
        d.onClusterSizeScore(7.0)
        val s = d.onClusterSizeScore(7.0)
        assertTrue(s.active)
    }

    @Test fun confirmed_after_3_consecutive_active_clusters() {
        val d = MicrographiaDetector(MicroParams())
        d.onClusterSizeScore(10.0)
        d.onClusterSizeScore(10.0)
        d.onClusterSizeScore(10.0)
        // active window
        d.onClusterSizeScore(7.0)
        d.onClusterSizeScore(7.0)
        val s3 = d.onClusterSizeScore(7.0)
        assertTrue(s3.confirmedThisCluster)
        assertEquals(1, d.confirmedCount)
    }

    @Test fun confirmed_not_double_counted_until_active_ends() {
        val d = MicrographiaDetector(MicroParams())
        d.onClusterSizeScore(10.0); d.onClusterSizeScore(10.0); d.onClusterSizeScore(10.0)
        d.onClusterSizeScore(7.0); d.onClusterSizeScore(7.0); d.onClusterSizeScore(7.0) // confirmed 1
        assertEquals(1, d.confirmedCount)
        d.onClusterSizeScore(7.0) // still active, should not increment
        assertEquals(1, d.confirmedCount)

        // exit active (bigger size)
        d.onClusterSizeScore(12.0); d.onClusterSizeScore(12.0); d.onClusterSizeScore(12.0)
        // active again 3 clusters
        d.onClusterSizeScore(7.0); d.onClusterSizeScore(7.0); d.onClusterSizeScore(7.0)
        assertEquals(2, d.confirmedCount)
    }
}