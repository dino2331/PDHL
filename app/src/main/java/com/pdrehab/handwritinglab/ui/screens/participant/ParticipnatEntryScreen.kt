package com.pdrehab.handwritinglab.ui.screens.participant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdrehab.handwritinglab.ui.components.PrimaryButton

@Composable
fun ParticipantEntryScreen(
    onDone: (participantCode: String) -> Unit,
    vm: ParticipantEntryViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("참가자 ID", fontSize = 34.sp)
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = ui.code,
            onValueChange = vm::onCodeChanged,
            label = { Text("8글자 영숫자") },
            modifier = Modifier.fillMaxWidth()
        )
        ui.error?.let { Spacer(Modifier.height(10.dp)); Text(it, fontSize = 20.sp) }
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = if (ui.loading) "처리 중..." else "확인",
            enabled = !ui.loading,
            modifier = Modifier.fillMaxWidth(),
            onClick = { vm.confirm(onDone) }
        )
    }
}