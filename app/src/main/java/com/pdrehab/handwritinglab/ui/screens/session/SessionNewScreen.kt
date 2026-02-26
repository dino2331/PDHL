package com.pdrehab.handwritinglab.ui.screens.session

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdrehab.handwritinglab.ui.components.PrimaryButton

@Composable
fun SessionNewScreen(
    onGoPressure: () -> Unit,
    vm: SessionNewViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { vm.load() }
    val ui by vm.ui.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("새 세션", fontSize = 34.sp)
        Spacer(Modifier.height(10.dp))
        Text("이번 세션 주소(고정): ${ui.addressText}", fontSize = 22.sp)
        ui.error?.let { Spacer(Modifier.height(8.dp)); Text("오류: $it", fontSize = 20.sp) }
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = if (ui.creating) "생성 중..." else "세션 시작",
            enabled = !ui.creating,
            modifier = Modifier.fillMaxWidth(),
            onClick = { vm.startSession(onGoPressure) }
        )
    }
}