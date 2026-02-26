package com.pdrehab.handwritinglab.ui.screens.participant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HistorySessionScreen(
    vm: HistorySessionViewModel,
    sessionId: String
) {
    val ui by vm.ui.collectAsState()
    LaunchedEffect(sessionId) { vm.load(sessionId) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("세션 상세", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("sessionId: ${ui.sessionId}", style = MaterialTheme.typography.bodyLarge)
        Text("address: ${ui.addressText}", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        ui.rows.forEach {
            Text("${it.taskId}  ${it.primaryKey} = ${it.value ?: "NA"}", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
        }
    }
}