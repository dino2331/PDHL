package com.pdrehab.handwritinglab.ui.screens.participant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdrehab.handwritinglab.ui.components.GuideHeaderBar
import com.pdrehab.handwritinglab.ui.components.PrimaryButton

@Composable
fun HistoryListScreen(
    participantCode: String,
    onTrend: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        GuideHeaderBar("내 기록", participantCode, "")
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("세션 리스트 화면(최소 구현)", fontSize = 24.sp)
            Spacer(Modifier.weight(1f))
            PrimaryButton("향상 보기", modifier = Modifier.fillMaxWidth(), onClick = onTrend)
        }
    }
}