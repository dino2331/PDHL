package com.pdrehab.handwritinglab

import com.pdrehab.handwritinglab.feature.analysis.histogram.computeHist
import com.pdrehab.handwritinglab.feature.analysis.histogram.percentile
import org.junit.Assert.*
import org.junit.Test

class HistogramTest {

    @Test fun percentile_linear_interpolation() {
        val xs = (0..9).map { it.toDouble() }
        assertEquals(0.45, percentile(xs, 5.0), 1e-6)
        assertEquals(4.5, percentile(xs, 50.0), 1e-6)
        assertEquals(8.55, percentile(xs, 95.0), 1e-6)
    }

    @Test fun hist_counts_sum() {
        val values = (0..99).map { it.toDouble() }
        val h = computeHist(values, myValue = 50.0, bins = 10)
        assertEquals(10, h.counts.size)
        assertEquals(100, h.counts.sum())
        assertTrue(h.myClamped in h.min..h.max)
    }

    @Test fun hist_min_max_not_equal() {
        val values = List(30) { 1.0 }
        val h = computeHist(values, myValue = 1.0, bins = 10)
        assertTrue(h.max > h.min)
    }
}