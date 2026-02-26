package com.pdrehab.handwritinglab.ui.screens.session

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdrehab.handwritinglab.data.db.dao.SessionDao
import com.pdrehab.handwritinglab.domain.usecase.CurrentSessionStore
import com.pdrehab.handwritinglab.ui.components.GuideHeaderBar
import com.pdrehab.handwritinglab.ui.components.PrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    private val store: CurrentSessionStore,
    private val sessionDao: SessionDao
) : ViewModel() {
    fun markEnded() {
        val s = store.session.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            sessionDao.updateCalibsAndEnd(s.sessionId, now, s.pressureMvc, s.baselineSizeScore)
        }
    }
}

@Composable
fun SessionSummaryScreen(
    sessionId: String,
    onDone: () -> Unit,
    onAdminExport: () -> Unit,
    vm: SessionSummaryViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { vm.markEnded() }

    Column(Modifier.fillMaxSize()) {
        GuideHeaderBar("세션 요약", sessionId.take(8), "")
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("세션이 완료되었습니다.", fontSize = 28.sp)
            Spacer(Modifier.weight(1f))
            PrimaryButton("완료(홈)", modifier = Modifier.fillMaxWidth(), onClick = onDone)
            Spacer(Modifier.height(12.dp))
            PrimaryButton("관리자 Export", modifier = Modifier.fillMaxWidth(), onClick = onAdminExport)
        }
    }
}