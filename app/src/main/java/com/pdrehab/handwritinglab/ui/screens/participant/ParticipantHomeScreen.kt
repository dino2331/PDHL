package com.pdrehab.handwritinglab.ui.screens.participant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdrehab.handwritinglab.domain.usecase.CurrentSessionStore
import com.pdrehab.handwritinglab.ui.components.PrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel

@HiltViewModel
class ParticipantHomeViewModel @Inject constructor(
    val store: CurrentSessionStore
) : androidx.lifecycle.ViewModel()

@Composable
fun ParticipantHomeScreen(
    participantCode: String,
    onStartSession: () -> Unit,
    onHistory: () -> Unit,
    onAdmin: () -> Unit,
    vm: ParticipantHomeViewModel = hiltViewModel()
) {
    val p by vm.store.participant.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("내 ID: $participantCode", fontSize = 26.sp)
        Spacer(Modifier.height(8.dp))
        Text("내 고정 주소: ${p?.assignedAddressText ?: ""}", fontSize = 22.sp)

        Spacer(Modifier.weight(1f))

        PrimaryButton("과제하기", modifier = Modifier.fillMaxWidth(), onClick = onStartSession)
        Spacer(Modifier.height(12.dp))
        PrimaryButton("내 기록 보기", modifier = Modifier.fillMaxWidth(), onClick = onHistory)

        Spacer(Modifier.height(14.dp))
        PrimaryButton("관리자", modifier = Modifier.fillMaxWidth(), onClick = onAdmin)
    }
}