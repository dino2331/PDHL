package com.pdrehab.handwritinglab.ui.screens.admin

import androidx.lifecycle.ViewModel
import com.pdrehab.handwritinglab.feature.admin.AdminAuthStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class AdminLoginUi(
    val pin: String = "",
    val error: String? = null,
    val lockedSec: Int = 0
)

@HiltViewModel
class AdminLoginViewModel @Inject constructor(
    private val auth: AdminAuthStore
) : ViewModel() {
    private val _ui = MutableStateFlow(AdminLoginUi())
    val ui: StateFlow<AdminLoginUi> = _ui

    fun onPinChanged(v: String) {
        _ui.value = _ui.value.copy(pin = v.take(8), error = null)
    }

    fun tryLogin(nowMs: Long): Boolean {
        val locked = auth.remainingLockSec(nowMs)
        if (locked > 0) {
            _ui.value = _ui.value.copy(lockedSec = locked, error = "잠금 상태입니다(${locked}초)")
            return false
        }
        val ok = auth.tryLogin(_ui.value.pin, nowMs)
        _ui.value = if (ok) _ui.value.copy(error = null, lockedSec = 0) else {
            val l2 = auth.remainingLockSec(nowMs)
            _ui.value.copy(error = if (l2 > 0) "5회 실패로 30초 잠금" else "PIN이 올바르지 않습니다", lockedSec = l2)
        }
        return ok
    }
}