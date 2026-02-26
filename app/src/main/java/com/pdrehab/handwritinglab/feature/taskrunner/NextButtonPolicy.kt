package com.pdrehab.handwritinglab.feature.taskrunner

class NextButtonPolicy(
    private val rule: Rule,
    private val canvasWidthPx: Int,
    private val rightEdgeRatio: Double = 0.92,
    private val boxCount: Int = 12
) {
    enum class Rule { RIGHT_EDGE, ALL_BOXES_12 }

    private val filled = BooleanArray(boxCount)
    var enabled: Boolean = false
        private set

    fun onPenDown(xPx: Float, boxId: Int) {
        if (enabled) return
        when (rule) {
            Rule.RIGHT_EDGE -> {
                val thr = rightEdgeRatio * canvasWidthPx.toDouble()
                if (xPx.toDouble() >= thr) enabled = true
            }
            Rule.ALL_BOXES_12 -> {
                if (boxId in 0 until boxCount) filled[boxId] = true
                if (filled.all { it }) enabled = true
            }
        }
    }

    fun resetForNewPage() {
        enabled = false
        if (rule == Rule.ALL_BOXES_12) {
            for (i in filled.indices) filled[i] = false
        }
    }
}