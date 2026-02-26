package com.pdrehab.handwritinglab.ui.screens.splash

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdrehab.handwritinglab.ui.components.PrimaryButton

@Composable
fun SplashScreen(
    onStart: () -> Unit,
    vm: SplashViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { vm.startInit() }
    val ui by vm.ui.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("PDHL", fontSize = 36.sp)
        Spacer(Modifier.height(16.dp))
        Text(ui.status, fontSize = 24.sp)
        ui.error?.let {
            Spacer(Modifier.height(8.dp))
            Text("오류: $it", fontSize = 20.sp)
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = "시작",
            modifier = Modifier.fillMaxWidth(),
            onClick = onStart
        )
    }
}