package com.pdrehab.handwritinglab.data.storage

import com.pdrehab.handwritinglab.core.PdhlConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AdminAuthState(
    val isAdmin: Boolean = false,
    val failCount: Int = 0,
    val lockedUntilMs: Long = 0L
)

data class AdminLoginResult(
    val success: Boolean,
    val message: String,
    val lockedRemainingMs: Long = 0L
)

class AdminSession(
    private val pin: String = PdhlConstants.ADMIN_PIN_DEFAULT
) {
    private val _state = MutableStateFlow(AdminAuthState())
    val state: StateFlow<AdminAuthState> = _state

    fun logout() {
        _state.value = _state.value.copy(isAdmin = false)
    }

    fun tryLogin(input: String, nowMs: Long = System.currentTimeMillis()): AdminLoginResult {
        val s = _state.value
        if (nowMs < s.lockedUntilMs) {
            return AdminLoginResult(
                success = false,
                message = "잠금 상태입니다.",
                lockedRemainingMs = s.lockedUntilMs - nowMs
            )
        }

        if (input == pin) {
            _state.value = AdminAuthState(isAdmin = true, failCount = 0, lockedUntilMs = 0L)
            return AdminLoginResult(success = true, message = "성공")
        }

        val newFail = s.failCount + 1
        val lockedUntil = if (newFail >= PdhlConstants.ADMIN_FAIL_LIMIT) nowMs + PdhlConstants.ADMIN_LOCK_MS else 0L
        _state.value = AdminAuthState(isAdmin = false, failCount = newFail, lockedUntilMs = lockedUntil)

        return if (lockedUntil > 0L) {
            AdminLoginResult(false, "5회 실패: 30초 잠금", lockedUntil - nowMs)
        } else {
            AdminLoginResult(false, "PIN이 틀렸습니다. (${newFail}/${PdhlConstants.ADMIN_FAIL_LIMIT})")
        }
    }
}