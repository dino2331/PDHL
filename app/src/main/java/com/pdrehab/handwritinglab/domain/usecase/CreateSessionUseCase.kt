package com.pdrehab.handwritinglab.domain.usecase

import android.os.Build
import com.pdrehab.handwritinglab.core.JsonUtil
import com.pdrehab.handwritinglab.core.PdhlPaths
import com.pdrehab.handwritinglab.data.db.dao.SessionDao
import com.pdrehab.handwritinglab.data.db.entity.ParticipantEntity
import com.pdrehab.handwritinglab.data.db.entity.SessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.UUID
import javax.inject.Inject

@Serializable
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val sdkInt: Int
)

class CreateSessionUseCase @Inject constructor(
    private val sessionDao: SessionDao,
    private val paths: PdhlPaths
) {
    suspend fun create(participant: ParticipantEntity, seed: Long): SessionEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val s = SessionEntity(
            sessionId = UUID.randomUUID().toString(),
            participantId = participant.participantId,
            randomSeed = seed,
            createdAtMs = now,
            endedAtMs = null,
            pressureMvc = null,
            baselineSizeScore = null,
            assignedAddressText = participant.assignedAddressText
        )
        sessionDao.upsert(s)

        // session folder + meta files
        val dir = paths.sessionDir(s.sessionId).apply { mkdirs() }
        File(dir, "tasks").mkdirs()
        File(dir, "logs").mkdirs()

        File(dir, "session.json").writeText(JsonUtil.json.encodeToString(s), Charsets.UTF_8)
        File(dir, "participant.json").writeText(JsonUtil.json.encodeToString(participant), Charsets.UTF_8)

        val device = DeviceInfo(Build.MANUFACTURER, Build.MODEL, Build.VERSION.SDK_INT)
        File(dir, "device.json").writeText(JsonUtil.json.encodeToString(device), Charsets.UTF_8)

        s
    }
}