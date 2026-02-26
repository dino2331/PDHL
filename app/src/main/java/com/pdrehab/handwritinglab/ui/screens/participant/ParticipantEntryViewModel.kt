package com.pdrehab.handwritinglab.ui.screens.participant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdrehab.handwritinglab.core.Ids
import com.pdrehab.handwritinglab.domain.usecase.CurrentSessionStore
import com.pdrehab.handwritinglab.domain.usecase.UpsertParticipantUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParticipantEntryUi(
    val code: String = "",
    val error: String? = null,
    val loading: Boolean = false
)

@HiltViewModel
class ParticipantEntryViewModel @Inject constructor(
    private val upsert: UpsertParticipantUseCase,
    private val store: CurrentSessionStore
) : ViewModel() {
    private val _ui = MutableStateFlow(ParticipantEntryUi())
    val ui: StateFlow<ParticipantEntryUi> = _ui

    fun onCodeChanged(v: String) {
        val filtered = v.uppercase().filter { it.isDigit() || (it in 'A'..'Z') }.take(8)
        _ui.value = _ui.value.copy(code = filtered, error = null)
    }

    fun confirm(onDone: (String) -> Unit) {
        val code = Ids.normalizeParticipantCode(_ui.value.code)
        if (!Ids.isValidParticipantCode(code)) {
            _ui.value = _ui.value.copy(error = "ID는 8글자 영숫자입니다.")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            try {
                val p = upsert.upsertByCode(code)
                store.setParticipant(p)
                store.clearSession()
                _ui.value = _ui.value.copy(loading = false)
                onDone(p.participantCode)
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(loading = false, error = t.message ?: "오류")
            }
        }
    }
}