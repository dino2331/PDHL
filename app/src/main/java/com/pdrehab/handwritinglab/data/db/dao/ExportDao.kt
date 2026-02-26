package com.pdrehab.handwritinglab.data.db.dao

import androidx.room.Dao
import androidx.room.Query

data class MetricLongRow(
    val createdAtMs: Long,
    val participantCode: String,
    val participantId: String,
    val sessionId: String,
    val taskId: String?,
    val trialIndex: Int?,
    val metricKey: String,
    val value: Double?,
    val unit: String,
    val direction: String
)

@Dao
interface ExportDao {

    @Query("""
        SELECT mv.createdAtMs AS createdAtMs,
               p.participantCode AS participantCode,
               mv.participantId AS participantId,
               mv.sessionId AS sessionId,
               mv.taskId AS taskId,
               mv.trialIndex AS trialIndex,
               mv.metricKey AS metricKey,
               mv.value AS value,
               mv.unit AS unit,
               mv.direction AS direction
        FROM metric_values mv
        JOIN participants p ON p.participantId = mv.participantId
        WHERE mv.sessionId = :sessionId
        ORDER BY mv.createdAtMs ASC, mv.taskId ASC, mv.trialIndex ASC, mv.metricKey ASC
    """)
    suspend fun getMetricLongRowsForSession(sessionId: String): List<MetricLongRow>
}