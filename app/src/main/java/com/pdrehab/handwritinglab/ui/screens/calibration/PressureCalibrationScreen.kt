package com.pdrehab.handwritinglab.ui.screens.calibration

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdrehab.handwritinglab.feature.taskrunner.input.PressureTargetView
import com.pdrehab.handwritinglab.ui.components.GuideHeaderBar

@Composable
fun PressureCalibrationScreen(
    onDone: () -> Unit,
    vm: PressureCalibrationViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    Column(Modifier.fillMaxSize()) {
        GuideHeaderBar("압력 캘리브", "가장 강한 힘", "1/2")

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(ui.status, fontSize = 26.sp)
            ui.warning?.let { Spacer(Modifier.height(6.dp)); Text(it, fontSize = 20.sp) }
            Spacer(Modifier.height(12.dp))

            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = { ctx ->
                    PressureTargetView(ctx).apply {
                        onDone = { pressures -> vm.onDoneSamples(pressures, onDone) }
                    }
                }
            )
        }
    }
}