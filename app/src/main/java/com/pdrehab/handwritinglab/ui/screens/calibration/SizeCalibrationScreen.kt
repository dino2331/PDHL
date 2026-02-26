package com.pdrehab.handwritinglab.ui.screens.calibration

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdrehab.handwritinglab.feature.taskrunner.guides.GuideType
import com.pdrehab.handwritinglab.feature.taskrunner.input.StylusCanvasView
import com.pdrehab.handwritinglab.ui.components.GuideHeaderBar
import com.pdrehab.handwritinglab.ui.components.PrimaryButton

@Composable
fun SizeCalibrationScreen(
    onDoneStartFirstTask: (firstTaskInstanceId: String) -> Unit,
    vm: SizeCalibrationViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    var viewRef: StylusCanvasView? by remember { mutableStateOf(null) }

    Column(Modifier.fillMaxSize()) {
        GuideHeaderBar("크기 캘리브", "강강강", "2/2")

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(ui.message, fontSize = 26.sp)
            ui.error?.let { Spacer(Modifier.height(6.dp)); Text(it, fontSize = 20.sp) }
            Spacer(Modifier.height(10.dp))

            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = { ctx ->
                    StylusCanvasView(ctx).apply {
                        setGuideType(GuideType.NONE)
                        setOnSizeListener { w, h -> vm.onCanvasSize(w, h) }
                        setOnSampleListener { s -> vm.onSample(s) }
                    }.also { viewRef = it }
                }
            )

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                PrimaryButton(
                    text = "재시도",
                    modifier = Modifier.weight(1f),
                    onClick = { viewRef?.clearCanvas(); vm.onRetry() }
                )
                Spacer(Modifier.width(12.dp))
                PrimaryButton(
                    text = "완료",
                    modifier = Modifier.weight(1f),
                    onClick = { vm.onComplete(onDoneStartFirstTask) }
                )
            }
        }
    }
}