package com.pdrehab.handwritinglab.feature.analysis.features

class PxToMm(
    private val xdpi: Float,
    private val ydpi: Float
) {
    fun wMm(px: Float): Double = (px / xdpi) * 25.4
    fun hMm(px: Float): Double = (px / ydpi) * 25.4
    fun distMm(dxPx: Float, dyPx: Float): Double {
        val dx = wMm(dxPx)
        val dy = hMm(dyPx)
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}