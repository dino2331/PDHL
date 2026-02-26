package com.pdrehab.handwritinglab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Composable
fun LineChartCanvas(values: List<Double?>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        val xs = values.mapIndexedNotNull { i, v -> if (v != null && v.isFinite()) i to v else null }
        if (xs.size < 2) return@Canvas

        val padL = 48f
        val padR = 16f
        val padT = 16f
        val padB = 36f

        val left = padL
        val top = padT
        val right = size.width - padR
        val bottom = size.height - padB

        val w = (right - left).coerceAtLeast(1f)
        val h = (bottom - top).coerceAtLeast(1f)

        val minV = xs.minOf { it.second }
        val maxV = xs.maxOf { it.second }.let { if (it == minV) it + 1.0 else it }

        fun xOf(i: Int): Float {
            val t = i.toFloat() / (values.size - 1).coerceAtLeast(1)
            return left + t * w
        }
        fun yOf(v: Double): Float {
            val t = ((v - minV) / (maxV - minV)).toFloat().coerceIn(0f, 1f)
            return bottom - t * h
        }

        // axes
        drawLine(Color.Black, Offset(left, bottom), Offset(right, bottom), 2f)
        drawLine(Color.Black, Offset(left, top), Offset(left, bottom), 2f)

        // polyline
        for (k in 1 until xs.size) {
            val (i0, v0) = xs[k - 1]
            val (i1, v1) = xs[k]
            drawLine(Color.Black, Offset(xOf(i0), yOf(v0)), Offset(xOf(i1), yOf(v1)), 3f)
        }
        // points
        xs.forEach { (i, v) ->
            drawCircle(Color.Black, radius = 5f, center = Offset(xOf(i), yOf(v)))
        }
    }
}