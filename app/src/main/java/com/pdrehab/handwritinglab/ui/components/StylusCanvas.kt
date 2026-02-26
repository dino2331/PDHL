package com.pdrehab.handwritinglab.ui.components

import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pdrehab.handwritinglab.feature.taskrunner.guides.GuideType
import com.pdrehab.handwritinglab.feature.taskrunner.input.MotionSample
import com.pdrehab.handwritinglab.feature.taskrunner.input.StylusCanvasView

@Composable
fun StylusCanvas(
    modifier: Modifier,
    guideType: GuideType,
    hard: Boolean,
    inkColorArgb: Int,
    pageIndex: Int,
    inputEnabled: Boolean,
    clearToken: Int,
    onSizePx: (w: Int, h: Int) -> Unit,
    onSample: (MotionSample) -> Unit,
    onNonStylus: () -> Unit
) {
    var lastClear by remember { mutableIntStateOf(clearToken) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            StylusCanvasView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setOnSizeListener(onSizePx)
                setOnSampleListener(onSample)
                setOnNonStylusListener { onNonStylus() }
            }
        },
        update = { v ->
            v.setGuideType(guideType)
            v.setHard(hard)
            v.setInkColorArgb(inkColorArgb)
            v.setPageIndex(pageIndex)
            v.setInputEnabled(inputEnabled)

            if (clearToken != lastClear) {
                v.clearCanvas()
                lastClear = clearToken
            }
        }
    )
}