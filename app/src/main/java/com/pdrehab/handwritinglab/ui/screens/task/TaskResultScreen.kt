package com.pdrehab.handwritinglab.ui.screens.task

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdrehab.handwritinglab.ui.components.GuideHeaderBar
import com.pdrehab.handwritinglab.ui.components.HistogramCanvas
import com.pdrehab.handwritinglab.ui.components.PrimaryButton

@Composable
fun TaskResultScreen(
    sessionId: String,
    taskId: String,
    onNextTask: (taskInstanceId: String) -> Unit,
    onSessionDone: (sessionId: String) -> Unit,
    vm: TaskResultViewModel = hiltViewModel()
) {
    LaunchedEffect(sessionId, taskId) {
        vm.load(sessionId, taskId)
    }
    val ui by vm.ui.collectAsState()

    Column(Modifier.fillMaxSize()) {
        GuideHeaderBar("결과", ui.taskId, "")
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("대표 지표: ${ui.primaryMetricKey}", fontSize = 24.sp)
            Spacer(Modifier.height(8.dp))
            Text("내 값: ${ui.myValue?.toString() ?: "NA"} ${ui.unit}", fontSize = 30.sp)

            ui.deltaVsFirst?.let {
                Spacer(Modifier.height(6.dp))
                Text("최초 세션 대비 Δ: ${"%.3f".format(it)}", fontSize = 22.sp)
            }

            Spacer(Modifier.height(10.dp))
            Text("비교 표본 수 n=${ui.n}", fontSize = 20.sp)

            Spacer(Modifier.height(10.dp))
            if (ui.hideChart) {
                Text(ui.message ?: "차트를 표시할 수 없습니다.", fontSize = 20.sp)
            } else {
                ui.hist?.let { h ->
                    HistogramCanvas(hist = h, modifier = Modifier.fillMaxWidth().height(220.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            PrimaryButton(
                text = "다음",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val next = ui.nextTaskInstanceId
                    if (next != null) onNextTask(next) else onSessionDone(sessionId)
                }
            )
        }
    }
}