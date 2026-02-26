package com.pdrehab.handwritinglab.ui.screens.admin

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
fun AdminLoginScreen(
    onSuccess: () -> Unit,
    vm: AdminLoginViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("관리자 PIN", fontSize = 34.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = ui.pin,
            onValueChange = vm::onPinChanged,
            label = { Text("PIN (기본 1234)") },
            modifier = Modifier.fillMaxWidth()
        )
        ui.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 20.sp)
        }
        if (ui.lockedRemainingMs > 0) {
            Spacer(Modifier.height(6.dp))
            Text("잠금 해제까지 ${(ui.lockedRemainingMs / 1000)}초", fontSize = 20.sp)
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("로그인", modifier = Modifier.fillMaxWidth(), onClick = { vm.submit(onSuccess) })
    }
}