package com.pdrehab.handwritinglab.domain.usecase

import com.pdrehab.handwritinglab.data.db.dao.MetricValueDao
import com.pdrehab.handwritinglab.data.db.entity.MetricValueEntity
import com.pdrehab.handwritinglab.domain.model.MetricCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class ComputeTaskAggregateUseCase @Inject constructor(
    private val metricDao: MetricValueDao
) {
    suspend fun compute(sessionId: String, taskId: String) = withContext(Dispatchers.IO) {
        val all = metricDao.getMetricsForTask(sessionId, taskId)
            .filter { it.trialIndex == 1 || it.trialIndex == 2 }

        val grouped = all.groupBy { it.metricKey }

        val inserts = ArrayList<MetricValueEntity>()
        for ((key, rows) in grouped) {
            val vals = rows.mapNotNull { it.value }
            if (vals.isEmpty()) continue

            val def = MetricCatalog.def(key)
            val op = def?.aggOp ?: guessOp(key)

            val aggV = when (op) {
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
                metricKey = key,
                value = aggV,
                unit = base.unit,
                direction = base.direction,
                createdAtMs = base.createdAtMs
            )
        }
        if (inserts.isNotEmpty()) metricDao.upsertAll(inserts)
    }

    private fun guessOp(key: String): MetricCatalog.AggOp {
        if (key.endsWith("_COUNT")) return MetricCatalog.AggOp.SUM
        if (key.contains("MAX_")) return MetricCatalog.AggOp.MAX
        if (key.endsWith("_MS")) return MetricCatalog.AggOp.MEAN
        return MetricCatalog.AggOp.MEAN
    }
}