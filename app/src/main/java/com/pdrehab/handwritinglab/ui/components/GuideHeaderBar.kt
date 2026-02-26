package com.pdrehab.handwritinglab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MintHeader = Color(0xFF8DAA9D)
private val Black = Color(0xFF000000)

@Composable
fun GuideHeaderBar(
    leftTitle: String,
    centerPrompt: String,
    rightInfo: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MintHeader)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(leftTitle, fontSize = 30.sp, color = Black)
        Spacer(Modifier.weight(1f))
        Text(centerPrompt, fontSize = 30.sp, color = Black, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text(rightInfo, fontSize = 26.sp, color = Black)
    }
}