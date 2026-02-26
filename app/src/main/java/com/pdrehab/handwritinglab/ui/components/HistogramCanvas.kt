package com.pdrehab.handwritinglab.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pdrehab.handwritinglab.feature.analysis.histogram.Hist
import kotlin.math.floor
import androidx.compose.ui.graphics.nativeCanvas
private val Purple = Color(0xFF522B47)
private val Axis = Color(0xFF222222)
private val Bar = Color(0xFF666666)

@Composable
fun HistogramCanvas(hist: Hist, modifier: Modifier = Modifier.fillMaxWidth().height(240.dp)) {
    Canvas(modifier = modifier) {
        val W = size.width
        val H = size.height

        val padL = 26.dp.toPx()
        val padR = 26.dp.toPx()
        val padT = 16.dp.toPx()
        val padB = 46.dp.toPx()

        val chartLeft = padL
        val chartRight = W - padR
        val chartTop = padT
        val chartBottom = H - padB

        val chartW = (chartRight - chartLeft).coerceAtLeast(1f)
        val chartH = (chartBottom - chartTop).coerceAtLeast(1f)

        val bins = hist.counts.size
        val slotW = chartW / bins.toFloat()
        val gapPx = 4.dp.toPx()
        val barW = (slotW - gapPx).coerceAtLeast(1f)

        // axis
        drawLine(Axis, start = Offset(chartLeft, chartBottom), end = Offset(chartRight, chartBottom), strokeWidth = 2f)

        // bars
        for (i in 0 until bins) {
            val c = hist.counts[i]
            val h = (c.toFloat() / hist.maxCount.toFloat()) * chartH
            val x = chartLeft + i * slotW + (gapPx / 2f)
            val y = chartBottom - h
            drawRect(
                color = Bar,
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barW, h)
            )
        }

        fun xOf(v: Double): Float {
            val t = ((v - hist.min) / (hist.max - hist.min)).toFloat().coerceIn(0f, 1f)
            return chartLeft + t * chartW
        }

        // my marker
        val mx = xOf(hist.myClamped)
        drawLine(Purple, Offset(mx, chartTop), Offset(mx, chartBottom), strokeWidth = 3f)
        drawCircle(Purple, radius = 6.dp.toPx(), center = Offset(mx, chartTop + 10.dp.toPx()))

        // labels (native canvas)
        val nc = drawContext.canvas.nativeCanvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 32f
            color = android.graphics.Color.DKGRAY
        }
        val paintPurple = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 34f
            color = android.graphics.Color.rgb(0x52, 0x2B, 0x47) // PRD purple
            isFakeBoldText = true
        }

        fun clampTextX(x: Float, text: String, p: Paint): Float {
            val w = p.measureText(text)
            val minX = chartLeft
            val maxX = chartRight - w
            return x.coerceIn(minX, maxX)
        }

        val minText = "min ${"%.2f".format(hist.min)}"
        val medText = "med ${"%.2f".format(hist.median)}"
        val maxText = "max ${"%.2f".format(hist.max)}"

        val minX = clampTextX(xOf(hist.min), minText, paint)
        val medX = clampTextX(xOf(hist.median), medText, paint)
        val maxX = clampTextX(xOf(hist.max), maxText, paint)

        val yLabel = H - 14.dp.toPx()

        nc.drawText(minText, minX, yLabel, paint)
        nc.drawText(medText, medX, yLabel, paint)
        nc.drawText(maxText, maxX, yLabel, paint)

        val myText = "내 값"
        val myX = clampTextX(mx, myText, paintPurple)
        val myY = chartTop + 34.dp.toPx()
        nc.drawText(myText, myX, myY, paintPurple)
    }
}