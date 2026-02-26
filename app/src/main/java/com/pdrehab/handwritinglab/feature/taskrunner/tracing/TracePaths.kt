package com.pdrehab.handwritinglab.feature.taskrunner.tracing

data class NormPath(val pts: List<Pair<Float, Float>>) // 0..1 normalized

object TracePaths {
    val easy = NormPath(
        listOf(
            0.10f to 0.55f,
            0.25f to 0.40f,
            0.45f to 0.60f,
            0.65f to 0.45f,
            0.85f to 0.55f
        )
    )
    val hard = NormPath(
        listOf(
            0.10f to 0.55f,
            0.20f to 0.35f,
            0.30f to 0.70f,
            0.40f to 0.30f,
            0.50f to 0.75f,
            0.60f to 0.35f,
            0.70f to 0.70f,
            0.80f to 0.45f,
            0.90f to 0.55f
        )
    )
}