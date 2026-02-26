package com.pdrehab.handwritinglab.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pdrehab.handwritinglab.data.db.entity.MetricValueEntity

data class TrendPointRow(
    val sessionId: String,
    val createdAtMs: Long,
    val value: Double?
)

@Dao
interface MetricValueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<MetricValueEntity>)

    @Query("""
        SELECT value FROM metric_values
        WHERE sessionId = :sessionId AND taskId = :taskId AND trialIndex = 0 AND metricKey = :metricKey
        LIMIT 1
    """)
    suspend fun getAggregateValue(sessionId: String, taskId: String, metricKey: String): Double?

    // --- Distribution: other participants + latest session per participant (no window func)
    @Query("""
        SELECT mv.value
        FROM metric_values mv
        JOIN sessions s ON s.sessionId = mv.sessionId
        WHERE mv.taskId = :taskId
          AND mv.trialIndex = 0
          AND mv.metricKey = :metricKey
          AND mv.value IS NOT NULL
          AND s.participantId != :selfParticipantId
          AND s.sessionId = (
              SELECT s2.sessionId
              FROM sessions s2
              WHERE s2.participantId = s.participantId
              ORDER BY s2.createdAtMs DESC, s2.sessionId DESC
              LIMIT 1
          )
        ORDER BY mv.value ASC
    """)
    suspend fun getDistributionValuesLatestSessionPerParticipant(
        selfParticipantId: String,
        taskId: String,
        metricKey: String
    ): List<Double>

    @Query("""
        SELECT COUNT(1)
        FROM metric_values mv
        JOIN sessions s ON s.sessionId = mv.sessionId
        WHERE mv.taskId = :taskId
          AND mv.trialIndex = 0
          AND mv.metricKey = :metricKey
          AND mv.value IS NOT NULL
          AND s.participantId != :selfParticipantId
          AND s.sessionId = (
              SELECT s2.sessionId
              FROM sessions s2
              WHERE s2.participantId = s.participantId
              ORDER BY s2.createdAtMs DESC, s2.sessionId DESC
              LIMIT 1
          )
    """)
    suspend fun getDistributionCountLatestSessionPerParticipant(
        selfParticipantId: String,
        taskId: String,
        metricKey: String
    ): Int

    // --- Trend series (participant timeline)
    @Query("""
        SELECT s.sessionId AS sessionId,
               s.createdAtMs AS createdAtMs,
               mv.value AS value
        FROM sessions s
        LEFT JOIN metric_values mv
          ON mv.sessionId = s.sessionId
         AND mv.taskId = :taskId
         AND mv.trialIndex = 0
         AND mv.metricKey = :metricKey
        WHERE s.participantId = :participantId
        ORDER BY s.createdAtMs ASC, s.sessionId ASC
    """)
    suspend fun getTrendSeries(
        participantId: String,
        taskId: String,
        metricKey: String
    ): List<TrendPointRow>

    @Query("""
        SELECT * FROM metric_values
        WHERE sessionId = :sessionId AND trialIndex = 0 AND taskId IS NOT NULL
    """)
    suspend fun getAllTaskAggregateMetrics(sessionId: String): List<MetricValueEntity>

    @Query("""
        SELECT * FROM metric_values
        WHERE sessionId = :sessionId AND taskId = :taskId
    """)
    suspend fun getMetricsForTask(sessionId: String, taskId: String): List<MetricValueEntity>

    @Query("SELECT * FROM metric_values WHERE trialIndex IN (1,2)")
    suspend fun getAllTrialMetrics(): List<MetricValueEntity>

    @Query("SELECT * FROM metric_values WHERE trialIndex = 0")
    suspend fun getAllAggregateMetrics(): List<MetricValueEntity>
}