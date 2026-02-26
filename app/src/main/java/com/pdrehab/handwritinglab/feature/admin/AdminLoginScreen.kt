package com.pdrehab.handwritinglab.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdrehab.handwritinglab.ui.components.GuideHeaderBar
import com.pdrehab.handwritinglab.ui.components.PrimaryButton

@Composable
fun AdminLoginScreen(
    onSuccess: () -> Unit,
    vm: AdminLoginViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    Column(Modifier.fillMaxSize()) {
        GuideHeaderBar("관리자", "PIN 로그인", "")
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = ui.pin,
                onValueChange = vm::onPinChanged,
                label = { Text("PIN") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            ui.error?.let { Text(it, fontSize = 22.sp) }
            Spacer(Modifier.height(12.dp))

            PrimaryButton(
                text = "로그인",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val ok = vm.tryLogin(System.currentTimeMillis())
                    if (ok) onSuccess()
                }
            )
        }
    }
}