package com.pdrehab.handwritinglab.ui.screens.task

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdrehab.handwritinglab.domain.usecase.CurrentSessionStore
import com.pdrehab.handwritinglab.feature.taskrunner.input.StylusCanvasView
import com.pdrehab.handwritinglab.ui.components.GuideHeaderBar
import com.pdrehab.handwritinglab.ui.components.InstructionOverlay
import com.pdrehab.handwritinglab.ui.components.PrimaryButton
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TaskRunScreen(
    taskInstanceId: String,
    onNavigateToTrial: (nextTaskInstanceId: String) -> Unit,
    onNavigateToResult: (sessionId: String, taskId: String) -> Unit,
    vm: TaskRunViewModel = hiltViewModel(),
    store: CurrentSessionStore = hiltViewModel<com.pdrehab.handwritinglab.ui.screens.participant.ParticipantHomeViewModel>().store // 간단 주입
) {
    val session = store.session.collectAsState().value
    val assignedAddress = session?.assignedAddressText ?: ""

    var viewRef: StylusCanvasView? by remember { mutableStateOf(null) }

    LaunchedEffect(taskInstanceId) {
        vm.load(taskInstanceId, assignedAddress)
    }

    LaunchedEffect(Unit) {
        vm.nav.collectLatest { nav ->
            when (nav) {
                is TaskRunNav.ToTrial -> onNavigateToTrial(nav.nextTaskInstanceId)
                is TaskRunNav.ToResult -> onNavigateToResult(nav.sessionId, nav.taskId)
            }
        }
    }

    val ui by vm.ui.collectAsState()

    Column(Modifier.fillMaxSize()) {
        GuideHeaderBar(
            leftTitle = "${ui.taskId}",
            centerPrompt = ui.prompt,
            rightInfo = ui.rightInfo
        )

        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {

                Text("남은 시간: ${(ui.remainingMs / 1000.0).toString().take(4)}s", fontSize = 22.sp)
                ui.feedbackBanner?.let { Spacer(Modifier.height(4.dp)); Text(it, fontSize = 22.sp) }

                Spacer(Modifier.height(8.dp))

                AndroidView(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    factory = { ctx ->
                        StylusCanvasView(ctx).apply {
                            setGuideType(ui.guideType)
                            setOnSizeListener { w, h -> vm.onCanvasSize(w, h) }
                            setOnSampleListener { s -> vm.onSample(s) }
                        }.also { viewRef = it }
                    },
                    update = { v ->
                        v.setGuideType(ui.guideType)
                        v.setPageIndex(0) // 실제 pageIndex는 VM이 관리(샘플에 pageIndex가 들어가므로 필요 시 확장)
                        v.setInputEnabled(!ui.restOverlay)
                    }
                )

                Spacer(Modifier.height(10.dp))

                Row(Modifier.fillMaxWidth()) {
                    if (ui.nextVisible) {
                        PrimaryButton(
                            text = "다음 쪽",
                            enabled = ui.nextEnabled,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                vm.onNextPage(
                                    clearCanvas = { viewRef?.clearCanvas() },
                                    showArrow = { viewRef?.showStartArrow() }
                                )
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    val rightText = if (ui.completeVisible) "완료" else " "
                    PrimaryButton(
                        text = rightText,
                        enabled = if (ui.completeVisible) ui.completeEnabled else false,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.onCompleteClicked() }
                    )
                }
            }

            if (ui.showInstruction) {
                InstructionOverlay(
                    title = ui.instructionTitle,
                    body = ui.instructionBody,
                    onStart = {
                        vm.onStartClicked { /* raw file path available if needed */ }
                    }
                )
            }

            if (ui.restOverlay) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("휴식 중...", fontSize = 38.sp)
                }
            }
        }
    }
}