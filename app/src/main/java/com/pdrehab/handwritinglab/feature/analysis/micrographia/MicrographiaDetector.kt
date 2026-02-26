package com.pdrehab.handwritinglab.feature.analysis.micrographia

data class MicroResult(
    val active: Boolean,
    val confirmed: Boolean
)

class MicrographiaDetector(
    private val alpha: Double = 0.75,
    private val beta: Double = 0.30,
    private val baselineClusters: Int = 3,
    private val rollingWindow: Int = 3,
    private val thresholdRatio: Double = 0.75,
    private val confirmConsecutive: Int = 3
) {
    private val sizes = ArrayList<Double>()
    private var baseline: Double? = null
    private var consecutiveActive = 0
    private var confirmedAlready = false

    var confirmedCount: Int = 0
        private set
    var activeMs: Long = 0
        private set

    fun onCluster(
        heightMm: Double,
        widthMm: Double,
        durationMs: Long
    ): MicroResult {
        val sizeScore = alpha * heightMm + beta * widthMm
        sizes.add(sizeScore)

        if (sizes.size == baselineClusters) {
            baseline = sizes.take(baselineClusters).average()
        }

        val b = baseline
        if (b == null || sizes.size < baselineClusters + rollingWindow) {
            return MicroResult(active = false, confirmed = false)
        }

        val last = sizes.takeLast(rollingWindow).average()
        val activeNow = last < thresholdRatio * b

        if (activeNow) {
            activeMs += durationMs
            consecutiveActive += 1
        } else {
            consecutiveActive = 0
            confirmedAlready = false
        }

        val confirmedNow = consecutiveActive >= confirmConsecutive
        if (confirmedNow && !confirmedAlready) {
            confirmedCount += 1
            confirmedAlready = true
        }

        return MicroResult(active = activeNow, confirmed = confirmedNow)
    }
}