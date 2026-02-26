package com.pdrehab.handwritinglab.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_instances",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "taskId", "trialIndex"], unique = true)
    ]
)
data class TaskInstanceEntity(
    @PrimaryKey val taskInstanceId: String, // UUID
    val sessionId: String,
    val taskId: String,     // T01..T15
    val trialIndex: Int,    // 1..2
    val orderInSession: Int,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val countNextScreen: Int = 0,
    val pageCount: Int = 1
)