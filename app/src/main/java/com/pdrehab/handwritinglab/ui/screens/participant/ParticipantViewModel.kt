package com.pdrehab.handwritinglab.ui.screens.participant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdrehab.handwritinglab.domain.usecase.CurrentSessionStore
import com.pdrehab.handwritinglab.domain.usecase.UpsertParticipantUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EntryUiState(
    val code: String = "",
    val error: String? = null,
    val busy: Boolean = false
)

@HiltViewModel
class ParticipantEntryViewModel @Inject constructor(
    private val upsert: UpsertParticipantUseCase,
    private val store: CurrentSessionStore
) : ViewModel() {

    private val _ui = MutableStateFlow(EntryUiState())
    val ui: StateFlow<EntryUiState> = _ui

    fun onCodeChange(v: String) {
        _ui.value = _ui.value.copy(code = v)
    }

    fun confirm(onGoHome: (String) -> Unit) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, error = null)
            try {
                val p = upsert(_ui.value.code, System.currentTimeMillis())
                store.set(p, session = null)
                onGoHome(p.participantCode)
            } catch (e: Throwable) {
                _ui.value = _ui.value.copy(error = "ID는 8글자 영숫자입니다.")
            } finally {
                _ui.value = _ui.value.copy(busy = false)
            }
        }
    }
}