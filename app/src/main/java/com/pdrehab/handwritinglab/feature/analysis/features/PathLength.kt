package com.pdrehab.handwritinglab.feature.analysis.features

import com.pdrehab.handwritinglab.feature.taskrunner.input.MotionSample
import kotlin.math.hypot

fun pathLengthPx(samples: List<MotionSample>): Double {
    if (samples.size < 2) return 0.0
    var sum = 0.0
    var prev = samples[0]
    for (i in 1 until samples.size) {
        val cur = samples[i]
        sum += hypot((cur.xPx - prev.xPx).toDouble(), (cur.yPx - prev.yPx).toDouble())
        prev = cur
    }
    return sum
}