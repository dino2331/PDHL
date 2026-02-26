package com.pdrehab.handwritinglab.data.storage

import com.pdrehab.handwritinglab.core.JsonUtil
import com.pdrehab.handwritinglab.core.PdhlPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class EventRecord(
    val tMs: Long,
    val sessionId: String,
    val participantCode: String,
    val taskId: String? = null,
    val trialIndex: Int? = null,
    val eventType: String,
    val payload: Map<String, String> = emptyMap()
)

@Singleton
class EventLogger @Inject constructor(
    private val paths: PdhlPaths
) {
    private fun file(sessionId: String): File =
        File(paths.sessionDir(sessionId), "logs/events.jsonl")

    suspend fun log(
        sessionId: String,
        participantCode: String,
        eventType: String,
        tMs: Long,
        taskId: String? = null,
        trialIndex: Int? = null,
        payload: Map<String, String> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        val f = file(sessionId)
        f.parentFile?.mkdirs()
        val line = JsonUtil.json.encodeToString(
            EventRecord(tMs, sessionId, participantCode, taskId, trialIndex, eventType, payload)
        )
        f.appendText(line + "\n", Charsets.UTF_8)
    }
}