package com.pdrehab.handwritinglab.core

object Ids {
    fun normalizeParticipantCode(input: String): String =
        input.trim().uppercase()

    fun isValidParticipantCode(code: String): Boolean =
        Regex("^[A-Z0-9]{8}$").matches(code)
}