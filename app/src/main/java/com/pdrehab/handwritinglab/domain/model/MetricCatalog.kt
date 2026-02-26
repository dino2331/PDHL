package com.pdrehab.handwritinglab.domain.model

object MetricCatalog {
    enum class AggOp { MEAN, SUM, MAX }

    data class Def(
        val key: String,
        val unit: String,
        val direction: MetricDirection,
        val aggOp: AggOp
    )

    // PRD v1.4: 집계 규칙 요약(필요한 것만 우선)
    private val defs: Map<String, Def> = listOf(
        Def("SIZE_REDUCTION_PCT", "%", MetricDirection.LOWER_BETTER, AggOp.MEAN),
        Def("MEAN_SPEED_MMPS", "mm/s", MetricDirection.NEUTRAL, AggOp.MEAN),
        Def("MEAN_PRESSURE_NORM", "ratio", MetricDirection.NEUTRAL, AggOp.MEAN),
        Def("IN_AIR_TIME_MS", "ms", MetricDirection.LOWER_BETTER, AggOp.MEAN),
        Def("ON_SURFACE_TIME_MS", "ms", MetricDirection.NEUTRAL, AggOp.MEAN),

        Def("STROKE_COUNT", "count", MetricDirection.NEUTRAL, AggOp.SUM),
        Def("COUNT_NEXT_SCREEN", "count", MetricDirection.NEUTRAL, AggOp.SUM),
        Def("PAGE_COUNT", "count", MetricDirection.NEUTRAL, AggOp.SUM),

        Def("COMPLETION_TIME_MS", "ms", MetricDirection.LOWER_BETTER, AggOp.MEAN),
        Def("SUCCESS_COUNT", "count", MetricDirection.NEUTRAL, AggOp.SUM),
        Def("ORDER_ERROR_COUNT", "count", MetricDirection.NEUTRAL, AggOp.SUM),
        Def("MAX_DEVIATION_TO_PATH_MM", "mm", MetricDirection.LOWER_BETTER, AggOp.MAX)
    ).associateBy { it.key }

    fun def(key: String): Def? = defs[key]
}