package com.pdrehab.handwritinglab.domain.usecase

import com.pdrehab.handwritinglab.core.PdhlConstants

class AdminSession(private val pin: String) {
    private var failCount = 0
    private var lockUntilMs = 0L

    fun canTry(nowMs: Long): Boolean = nowMs >= lockUntilMs

    fun lockRemainingMs(nowMs: Long): Long = (lockUntilMs - nowMs).coerceAtLeast(0L)

    fun login(input: String, nowMs: Long): Boolean {
        if (!canTry(nowMs)) return false
        val ok = input == pin
        if (ok) {
            failCount = 0
            lockUntilMs = 0L
            return true
        }
        failCount += 1
        if (failCount >= PdhlConstants.ADMIN_MAX_FAIL) {
            lockUntilMs = nowMs + PdhlConstants.ADMIN_LOCK_MS
            failCount = 0
        }
        return false
    }
}