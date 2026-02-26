package com.pdrehab.handwritinglab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class TrendPointUi(
    val xIndex: Int,          // 1..N
    val value: Double?
)

@Composable
fun TrendChartCanvas(
    points: List<TrendPointUi>,
    modifier: Modifier = Modifier.fillMaxWidth().height(260.dp)
) {
    Canvas(modifier = modifier) {
        val W = size.width
        val H = size.height
        val pad = 24.dp.toPx()

        val left = pad
        val right = W - pad
        val top = pad
        val bottom = H - pad

        val xs = points.mapNotNull { it.value }
        if (xs.isEmpty() || points.size < 2) return@Canvas

        val minV = xs.minOrNull()!!
        val maxV = xs.maxOrNull()!!.let { if (it == minV) it + 1.0 else it }

        fun xOf(i: Int): Float {
            val t = (i - 1).toFloat() / (points.size - 1).toFloat()
            return left + t * (right - left)
        }
        fun yOf(v: Double): Float {
            val t = ((v - minV) / (maxV - minV)).toFloat().coerceIn(0f, 1f)
            return bottom - t * (bottom - top)
        }

        // axes
        drawLine(Color.Black, Offset(left, bottom), Offset(right, bottom), 2f)
        drawLine(Color.Black, Offset(left, top), Offset(left, bottom), 2f)

        var last: Offset? = null
        points.forEachIndexed { idx, p ->
            val v = p.value ?: return@forEachIndexed
            val o = Offset(xOf(idx + 1), yOf(v))
            last?.let { drawLine(Color.Black, it, o, 3f) }
            drawCircle(Color.Black, radius = 6.dp.toPx(), center = o)
            last = o
        }
    }
}