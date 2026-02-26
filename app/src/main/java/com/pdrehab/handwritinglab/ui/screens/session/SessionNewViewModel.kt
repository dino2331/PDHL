package com.pdrehab.handwritinglab.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdrehab.handwritinglab.domain.usecase.CreateSessionUseCase
import com.pdrehab.handwritinglab.domain.usecase.CreateTaskPlanUseCase
import com.pdrehab.handwritinglab.domain.usecase.CurrentSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import javax.inject.Inject

data class SessionNewUi(
    val addressText: String = "",
    val creating: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SessionNewViewModel @Inject constructor(
    private val store: CurrentSessionStore,
    private val createSession: CreateSessionUseCase,
    private val createPlan: CreateTaskPlanUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(SessionNewUi())
    val ui: StateFlow<SessionNewUi> = _ui

    fun load() {
        val p = store.participant.value
        _ui.value = _ui.value.copy(addressText = p?.assignedAddressText ?: "")
    }

    fun startSession(onGoPressure: () -> Unit) {
        val p = store.participant.value ?: return
        if (_ui.value.creating) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(creating = true, error = null)
            try {
                val seed = Random(System.currentTimeMillis()).nextLong()
                val s = createSession.create(p, seed)
                store.setSession(s)
                createPlan.create(s.sessionId, seed) // firstTaskInstanceId는 size calibration에서 사용
                _ui.value = _ui.value.copy(creating = false)
                onGoPressure()
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(creating = false, error = t.message ?: "오류")
            }
        }
    }
}