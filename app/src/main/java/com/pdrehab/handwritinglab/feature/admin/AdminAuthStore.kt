package com.pdrehab.handwritinglab.feature.admin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

@Singleton
class AdminAuthStore @Inject constructor() {
    private val _authed = MutableStateFlow(false)
    val authed: StateFlow<Boolean> = _authed

    private var failCount = 0
    private var lockUntilMs: Long = 0L

    fun remainingLockSec(nowMs: Long): Int {
        val rem = (lockUntilMs - nowMs).coerceAtLeast(0L)
        return ceil(rem / 1000.0).toInt()
    }

    fun tryLogin(pin: String, nowMs: Long): Boolean {
        if (nowMs < lockUntilMs) return false
        if (pin == "1234") {
            _authed.value = true
            failCount = 0
            return true
        }
        failCount += 1
        if (failCount >= 5) {
            lockUntilMs = nowMs + 30_000L
            failCount = 0
        }
        return false
    }

    fun logout() { _authed.value = false }
}