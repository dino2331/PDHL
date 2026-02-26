package com.pdrehab.handwritinglab.data.db.dao

data class AdminSessionRow(
    val sessionId: String,
    val participantCode: String,
    val createdAtMs: Long,
    val endedAtMs: Long?
)