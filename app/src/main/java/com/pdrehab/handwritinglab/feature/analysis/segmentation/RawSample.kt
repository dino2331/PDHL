package com.pdrehab.handwritinglab.feature.analysis.segmentation

data class RawSample(
    val tMs: Long,
    val xPx: Float,
    val yPx: Float,
    val pressure: Float,
    val action: String,
    val toolType: String,
    val pointerId: Int,
    val isDown: Boolean,
    val pageIndex: Int,
    val boxId: Int,
    val strokeId: Int,
    val clusterId: Int
)

data class BBoxPx(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = (maxX - minX).coerceAtLeast(0f)
    val height: Float get() = (maxY - minY).coerceAtLeast(0f)

    fun contains(x: Float, y: Float): Boolean = x in minX..maxX && y in minY..maxY

    fun inflate(marginRatio: Float): BBoxPx {
        val mx = width * marginRatio
        val my = height * marginRatio
        return BBoxPx(minX - mx, minY - my, maxX + mx, maxY + my)
    }

    companion object {
        fun from(samples: List<RawSample>): BBoxPx? {
            if (samples.isEmpty()) return null
            var minX = samples[0].xPx
            var maxX = samples[0].xPx
            var minY = samples[0].yPx
            var maxY = samples[0].yPx
            for (s in samples) {
                if (s.xPx < minX) minX = s.xPx
                if (s.xPx > maxX) maxX = s.xPx
                if (s.yPx < minY) minY = s.yPx
                if (s.yPx > maxY) maxY = s.yPx
            }
            return BBoxPx(minX, minY, maxX, maxY)
        }
    }
}