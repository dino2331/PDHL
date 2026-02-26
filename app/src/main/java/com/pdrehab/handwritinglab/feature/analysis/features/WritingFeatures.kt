package com.pdrehab.handwritinglab.feature.analysis.features

import com.pdrehab.handwritinglab.feature.analysis.segmentation.RawSample

fun pathLengthMm(samples: List<RawSample>, conv: PxToMm): Double {
    if (samples.size < 2) return 0.0
    var sum = 0.0
    for (i in 1 until samples.size) {
        val a = samples[i - 1]
        val b = samples[i]
        val dx = b.xPx - a.xPx
        val dy = b.yPx - a.yPx
        sum += conv.distMm(dx, dy)
    }
    return sum
}

fun sizeReductionPct(unitPathLengthsMm: List<Double>, firstK: Int = 5, lastK: Int = 5): Double? {
    if (unitPathLengthsMm.size < firstK + lastK) return null // <10이면 NA
    val first = unitPathLengthsMm.take(firstK).average()
    val last = unitPathLengthsMm.takeLast(lastK).average()
    if (first == 0.0) return null
    return (first - last) / first * 100.0
}