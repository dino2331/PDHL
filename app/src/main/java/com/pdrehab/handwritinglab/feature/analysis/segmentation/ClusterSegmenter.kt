package com.pdrehab.handwritinglab.feature.analysis.segmentation

data class Cluster(
    val id: Int,
    val samples: List<RawSample>,
    val bbox: BBoxPx
)

class ClusterSegmenter(
    private val gapThresholdMs: Long = 350L,
    private val marginRatio: Float = 0.35f
) {
    fun segment(samples: List<RawSample>): List<Cluster> {
        val down = samples.filter { it.isDown && (it.action == "DOWN" || it.action == "MOVE") }
        if (down.isEmpty()) return emptyList()

        val out = ArrayList<Cluster>()
        var curId = 1
        var cur = ArrayList<RawSample>()
        cur.add(down[0])
        var bbox = BBoxPx.from(cur)!!

        for (i in 1 until down.size) {
            val prev = down[i - 1]
            val s = down[i]
            val dt = s.tMs - prev.tMs

            val inflated = bbox.inflate(marginRatio)
            val spatialBreak = !inflated.contains(s.xPx, s.yPx)
            val bboxH = bbox.height.coerceAtLeast(1f)
            val lineBreak = kotlin.math.abs(s.yPx - prev.yPx) > 1.5f * bboxH

            val newCluster = (dt > gapThresholdMs) || spatialBreak || lineBreak

            if (newCluster) {
                out.add(Cluster(curId, cur.toList(), bbox))
                curId += 1
                cur = ArrayList()
                cur.add(s)
                bbox = BBoxPx.from(cur)!!
            } else {
                cur.add(s)
                bbox = BBoxPx.from(cur)!!
            }
        }

        out.add(Cluster(curId, cur.toList(), bbox))
        return out
    }
}