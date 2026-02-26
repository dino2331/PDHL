package com.pdrehab.handwritinglab.domain.model

enum class MetricDirection { LOWER_BETTER, HIGHER_BETTER, NEUTRAL }

data class MetricSpec(
    val key: String,
    val unit: String,
    val direction: MetricDirection
)

object MetricCatalog {
    val SIZE_REDUCTION_PCT = MetricSpec("SIZE_REDUCTION_PCT", "%", MetricDirection.LOWER_BETTER)
    val COMPLETION_TIME_MS = MetricSpec("COMPLETION_TIME_MS", "ms", MetricDirection.LOWER_BETTER)

    fun byKey(key: String): MetricSpec = when (key) {
        SIZE_REDUCTION_PCT.key -> SIZE_REDUCTION_PCT
        COMPLETION_TIME_MS.key -> COMPLETION_TIME_MS
        else -> MetricSpec(key, "", MetricDirection.NEUTRAL)
    }
}