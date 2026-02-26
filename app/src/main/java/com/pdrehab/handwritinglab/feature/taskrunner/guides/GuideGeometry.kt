package com.pdrehab.handwritinglab.feature.taskrunner.guides

import kotlin.math.min

data class Pt(val x: Float, val y: Float)
data class RectF(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun contains(x: Float, y: Float): Boolean =
        x >= left && x <= right && y >= top && y <= bottom
}

object GuideGeometry {

    fun twoLinesY(H: Float): Pair<Float, Float> = Pair(0.35f * H, 0.65f * H)

    fun topDots(W: Float, H: Float): List<Pt> {
        val dotCount = 12
        val marginX = 0.06f * W
        val usableW = W - 2f * marginX
        val y = 0.30f * H
        return (0 until dotCount).map { i ->
            val x = marginX + i * (usableW / (dotCount - 1).toFloat())
            Pt(x, y)
        }
    }

    fun boxesRow12(W: Float, H: Float): List<RectF> {
        val n = 12
        val marginX = 0.04f * W
        val gap = 0.008f * W
        val maxBoxW = (W - 2f * marginX - gap * (n - 1)) / n.toFloat()
        val boxSize = min(maxBoxW, 0.55f * H)
        val rowW = n * boxSize + (n - 1) * gap
        val left = (W - rowW) / 2f
        val top = (H - boxSize) / 2f
        return (0 until n).map { i ->
            val x0 = left + i * (boxSize + gap)
            RectF(x0, top, x0 + boxSize, top + boxSize)
        }
    }

    fun variableBoxes(W: Float, H: Float, hard: Boolean): List<RectF> {
        // 쉬움/어려움: 폭/높이 변화량만 다르게
        val n = 12
        val marginX = 0.04f * W
        val gap = 0.008f * W
        val baseH = 0.45f * H
        val top = 0.30f * H
        val usableW = W - 2f * marginX - gap * (n - 1)
        val avgW = usableW / n
        val amp = if (!hard) 0.25f else 0.40f
        var x = marginX
        val rects = ArrayList<RectF>(n)
        for (i in 0 until n) {
            val w = avgW * (1f + amp * ((i % 3) - 1)) // -amp, 0, +amp 반복
            rects += RectF(x, top, x + w, top + baseH)
            x += w + gap
        }
        return rects
    }

    fun numberedBoxes(W: Float, H: Float): List<RectF> {
        // 3x4 grid, 12개
        val cols = 4
        val rows = 3
        val marginX = 0.06f * W
        val marginY = 0.18f * H
        val gapX = 0.02f * W
        val gapY = 0.04f * H
        val cellW = (W - 2f * marginX - gapX * (cols - 1)) / cols
        val cellH = (H - 2f * marginY - gapY * (rows - 1)) / rows
        val rects = ArrayList<RectF>(12)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x0 = marginX + c * (cellW + gapX)
                val y0 = marginY + r * (cellH + gapY)
                rects += RectF(x0, y0, x0 + cellW, y0 + cellH)
            }
        }
        return rects
    }

    fun tracePath(W: Float, H: Float, hard: Boolean): List<Pt> {
        // 간단한 polyline (easy는 완만, hard는 굴곡 추가)
        val left = 0.10f * W
        val right = 0.90f * W
        val midY = 0.55f * H
        val amp = if (!hard) 0.10f * H else 0.18f * H

        val pts = mutableListOf(
            Pt(left, midY),
            Pt(left + 0.20f * W, midY - amp),
            Pt(left + 0.40f * W, midY + amp),
            Pt(left + 0.60f * W, midY - amp),
            Pt(right, midY)
        )
        if (hard) {
            pts.add(3, Pt(left + 0.50f * W, midY + 1.2f * amp))
        }
        return pts
    }

    fun appleTargetRect(W: Float, H: Float, hard: Boolean): RectF {
        val w = if (!hard) 0.22f * W else 0.18f * W
        val h = if (!hard) 0.22f * H else 0.18f * H
        val left = 0.70f * W
        val top = 0.20f * H
        return RectF(left, top, left + w, top + h)
    }

    fun appleRadius(W: Float, H: Float, hard: Boolean): Float =
        if (!hard) 0.04f * min(W, H) else 0.032f * min(W, H)

    fun appleStartCenter(W: Float, H: Float): Pt = Pt(0.20f * W, 0.75f * H)

    fun boxIdFromRects(rects: List<RectF>, x: Float, y: Float): Int {
        for (i in rects.indices) if (rects[i].contains(x, y)) return i
        return -1
    }
}