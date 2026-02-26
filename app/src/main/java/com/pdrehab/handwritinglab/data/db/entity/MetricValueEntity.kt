package com.pdrehab.handwritinglab.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "metric_values",
    indices = [
        Index(value = ["sessionId", "taskId", "trialIndex", "metricKey"], unique = true),
        Index(value = ["taskId", "metricKey", "trialIndex"]),
        Index(value = ["participantId", "taskId", "metricKey", "trialIndex"]),
        Index(value = ["sessionId"])
    ]
)
data class MetricValueEntity(
    @PrimaryKey val metricId: String,  // UUID
    val participantId: String,
    val participantCode: String? = null, // optional denorm
    val sessionId: String,
    val taskId: String? = null,
    val trialIndex: Int? = null, // 1/2 trial, 0 aggregate, null session-level
    val metricKey: String,
    val value: Double? = null,
    val unit: String,
    val direction: String, // LOWER_BETTER|HIGHER_BETTER|NEUTRAL
    val createdAtMs: Long
)