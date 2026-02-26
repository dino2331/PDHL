package com.pdrehab.handwritinglab.domain.usecase

import com.pdrehab.handwritinglab.data.db.dao.MetricValueDao
import com.pdrehab.handwritinglab.data.db.dao.ParticipantDao
import com.pdrehab.handwritinglab.data.repo.AddressRepository
import com.pdrehab.handwritinglab.domain.model.MetricCatalog
import com.pdrehab.handwritinglab.data.db.entity.MetricValueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class PostBackfillUseCase @Inject constructor(
    private val participantDao: ParticipantDao,
    private val addressRepo: AddressRepository,
    private val metricDao: MetricValueDao
) {
    suspend fun runOnceBestEffort() = withContext(Dispatchers.IO) {
        // 1) participantCode normalize + unique + address 채움
        val ps = participantDao.getAll()
        val used = HashSet<String>(ps.size)

        for (p in ps) {
            var code = normalize(p.participantCode)
            if (!isValidCode(code)) {
                code = generateCodeFromId(p.participantId)
            }
            code = ensureUnique(code, used)
            used.add(code)

            val needAddr = p.assignedAddressId.isBlank() || p.assignedAddressText.isBlank()
            val addr = if (needAddr) addressRepo.assignByParticipantCode(code) else null

            participantDao.updateCore(
                participantId = p.participantId,
                participantCode = code,
                assignedAddressId = addr?.id ?: p.assignedAddressId,
                assignedAddressText = addr?.text ?: p.assignedAddressText
            )
        }

        // 2) aggregate(trialIndex=0) 없으면 생성(가능한 범위)
        val trial = metricDao.getAllTrialMetrics()
        val agg = metricDao.getAllAggregateMetrics()
        val aggKeySet = agg.map { keyOf(it) }.toHashSet()

        val grouped = trial
            .filter { it.taskId != null && it.trialIndex != null && (it.trialIndex == 1 || it.trialIndex == 2) }
            .groupBy { Triple(it.sessionId, it.taskId!!, it.metricKey) }

        val inserts = ArrayList<MetricValueEntity>()

        for ((k, rows) in grouped) {
            val (sessionId, taskId, metricKey) = k
            val aggKey = "$sessionId|$taskId|0|$metricKey"
            if (aggKeySet.contains(aggKey)) continue

            val def = MetricCatalog.def(metricKey)
            val op = def?.aggOp ?: MetricCatalog.AggOp.MEAN

            val vals = rows.mapNotNull { it.value }
            if (vals.isEmpty()) continue

            val vAgg = when (op) {
                MetricCatalog.AggOp.SUM -> vals.sum()
                MetricCatalog.AggOp.MAX -> vals.maxOrNull()!!
                MetricCatalog.AggOp.MEAN -> vals.average()
            }

            val base = rows.maxByOrNull { it.createdAtMs }!!
            inserts += MetricValueEntity(
                metricId = UUID.randomUUID().toString(),
                participantId = base.participantId,
                participantCode = base.participantCode,
                sessionId = sessionId,
                taskId = taskId,
                trialIndex = 0,
                metricKey = metricKey,
                value = vAgg,
                unit = base.unit,
                direction = base.direction,
                createdAtMs = base.createdAtMs
            )
        }

        if (inserts.isNotEmpty()) metricDao.upsertAll(inserts)
    }

    private fun normalize(s: String): String = s.trim().uppercase()

    private fun isValidCode(s: String): Boolean =
        s.length == 8 && s.all { it.isDigit() || (it in 'A'..'Z') }

    private fun generateCodeFromId(id: String): String {
        val raw = id.replace("-", "").uppercase()
        val base = (raw + "AAAAAAAA").take(8)
        return base.map { if (it.isDigit() || it in 'A'..'Z') it else 'A' }.joinToString("")
    }

    private fun ensureUnique(code: String, used: Set<String>): String {
        if (!used.contains(code)) return code
        val chars = code.toCharArray()
        for (i in 0 until 36) {
            val c = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"[i]
            chars[7] = c
            val candidate = String(chars)
            if (!used.contains(candidate)) return candidate
        }
        // 최후: 랜덤
        return UUID.randomUUID().toString().replace("-", "").uppercase().take(8)
    }

    private fun keyOf(m: MetricValueEntity): String =
        "${m.sessionId}|${m.taskId}|${m.trialIndex}|${m.metricKey}"
}