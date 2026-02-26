package com.pdrehab.handwritinglab.feature.analysis.micrographia

data class MicroParams(
    val alpha: Double = 0.75,
    val beta: Double = 0.30,
    val baselineClusters: Int = 3,
    val rollingWindow: Int = 3,
    val thresholdRatio: Double = 0.75,
    val confirmConsecutive: Int = 3
)

data class MicroState(
    val active: Boolean,
    val confirmed: Boolean,
    val confirmedThisCluster: Boolean,
    val rollingMean: Double?,
    val baselineMean: Double?
)