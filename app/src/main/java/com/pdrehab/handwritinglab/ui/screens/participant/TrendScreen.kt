package com.pdrehab.handwritinglab.ui.screens.participant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdrehab.handwritinglab.ui.components.GuideHeaderBar
import com.pdrehab.handwritinglab.ui.components.TrendChartCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendScreen(
    participantCode: String,
    vm: TrendViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { vm.init() }
    val ui by vm.ui.collectAsState()

    Column(Modifier.fillMaxSize()) {
        GuideHeaderBar(
            leftTitle = "향상 보기",
            centerPrompt = participantCode,
            rightInfo = ""
        )

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            // task selector
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                val selected = ui.selected?.taskId ?: ""
                TextField(
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    readOnly = true,
                    value = selected,
                    onValueChange = {},
                    label = { Text("과제 선택") }
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ui.options.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text("${opt.taskId} (${opt.primaryMetricKey})") },
                            onClick = {
                                expanded = false
                                vm.select(opt)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "baseline(첫 세션): ${ui.baselineValue?.let { "%.2f".format(it) } ?: "NA"} ${ui.unit}",
                fontSize = 22.sp
            )
            Spacer(Modifier.height(12.dp))

            TrendChartCanvas(points = ui.points)

            Spacer(Modifier.height(12.dp))

            // delta summary
            val lastDelta = ui.deltas.lastOrNull()
            val improved = ui.improvedFlags.lastOrNull()
            Text(
                text = buildString {
                    append("최근 Δ vs first: ")
                    append(lastDelta?.let { "%.2f".format(it) } ?: "NA")
                    append(" ${ui.unit}  ")
                    when (ui.direction.name) {
                        "LOWER_BETTER" -> append(if (improved == true) "(개선)" else if (improved == false) "(악화)" else "")
                        "HIGHER_BETTER" -> append(if (improved == true) "(개선)" else if (improved == false) "(악화)" else "")
                        else -> append("")
                    }
                },
                fontSize = 26.sp
            )
        }
    }
}