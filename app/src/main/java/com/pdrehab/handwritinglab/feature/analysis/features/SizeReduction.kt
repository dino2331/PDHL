package com.pdrehab.handwritinglab.feature.analysis.features

fun sizeReductionPct(unitSizeMetric: List<Double>, firstK: Int = 5, lastK: Int = 5): Double? {
    if (unitSizeMetric.size < firstK + lastK) return null
    val first = unitSizeMetric.take(firstK).average()
    val last = unitSizeMetric.takeLast(lastK).average()
    if (first == 0.0) return null
    return (first - last) / first * 100.0
}