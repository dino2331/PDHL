package com.pdrehab.handwritinglab.feature.analysis.histogram

import kotlin.math.ceil
import kotlin.math.floor

fun percentile(sorted: List<Double>, p: Double): Double {
    val n = sorted.size
    if (n == 1) return sorted[0]
    val pos = (p / 100.0) * (n - 1)
    val lo = floor(pos).toInt()
    val hi = ceil(pos).toInt()
    val t = pos - lo
    if (lo == hi) return sorted[lo]
    return sorted[lo] * (1.0 - t) + sorted[hi] * t
}

data class Hist(
    val min: Double,
    val max: Double,
    val median: Double,
    val counts: IntArray,
    val maxCount: Int,
    val myClamped: Double
)

fun computeHist(values: List<Double>, myValue: Double, bins: Int = 10): Hist {
    val xs = values.filter { it.isFinite() }.sorted()
    require(xs.isNotEmpty())

    var min = percentile(xs, 5.0)
    var max = percentile(xs, 95.0)
    val med = percentile(xs, 50.0)

    if (min == max) {
        min = xs.first()
        max = xs.last()
        if (min == max) max = min + 1.0
    }

    val w = (max - min) / bins.toDouble()
    val counts = IntArray(bins)
    for (x in xs) {
        val cx = x.coerceIn(min, max)
        var idx = floor((cx - min) / w).toInt()
        if (idx == bins) idx = bins - 1
        counts[idx.coerceIn(0, bins - 1)]++
    }
    val maxCount = (counts.maxOrNull() ?: 1).coerceAtLeast(1)

    return Hist(
        min = min, max = max, median = med,
        counts = counts, maxCount = maxCount,
        myClamped = myValue.coerceIn(min, max)
    )
}