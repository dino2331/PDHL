package com.pdrehab.handwritinglab.feature.taskrunner.input

data class MotionSample(
    val tNs: Long,
    val tMs: Long,
    val xPx: Float,
    val yPx: Float,
    val pressure: Float,
    val tilt: Float,
    val orientation: Float,
    val distance: Float,
    val action: String,     // DOWN|MOVE|UP|HOVER_MOVE|CANCEL
    val toolType: String,   // STYLUS|ERASER
    val pointerId: Int,
    val isDown: Boolean,
    val pageIndex: Int,
    val boxId: Int,
    val strokeId: Int,
    val clusterId: Int
)