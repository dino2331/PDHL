package com.pdrehab.handwritinglab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InstructionOverlay(
    title: String,
    body: List<String>,
    onStart: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(Color(0xAA000000)),
        contentAlignment = Alignment.Center
    ) {
        Card(Modifier.fillMaxWidth(0.88f).padding(12.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text(title, fontSize = 30.sp)
                Spacer(Modifier.height(12.dp))
                body.take(3).forEach {
                    Text("• $it", fontSize = 24.sp)
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(14.dp))
                PrimaryButton(text = "시작하기", modifier = Modifier.fillMaxWidth(), onClick = onStart)
            }
        }
    }
}