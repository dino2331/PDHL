package com.pdrehab.handwritinglab.ui.screens.admin

import androidx.lifecycle.ViewModel
import com.pdrehab.handwritinglab.domain.usecase.AdminSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class AdminLoginUiState(
    val pin: String = "",
    val error: String? = null,
    val lockedMs: Long = 0L
)

@HiltViewModel
class AdminLoginViewModel @Inject constructor(
    private val admin: AdminSession
) : ViewModel() {

    private val _ui = MutableStateFlow(AdminLoginUiState())
    val ui: StateFlow<AdminLoginUiState> = _ui

    fun onPinChange(s: String) {
        _ui.value = _ui.value.copy(pin = s.filter { it.isDigit() }.take(8), error = null)
    }

    fun tryLogin(onSuccess: () -> Unit) {
        val now = System.currentTimeMillis()
        if (!admin.canTry(now)) {
            _ui.value = _ui.value.copy(lockedMs = admin.lockRemainingMs(now), error = "잠금: ${admin.lockRemainingMs(now)/1000}s")
            return
        }
        val ok = admin.login(_ui.value.pin, now)
        if (ok) onSuccess()
        else _ui.value = _ui.value.copy(error = "PIN이 틀렸습니다.")
    }
}