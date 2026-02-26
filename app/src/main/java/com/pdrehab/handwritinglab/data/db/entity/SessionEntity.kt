package com.pdrehab.handwritinglab.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = ParticipantEntity::class,
            parentColumns = ["participantId"],
            childColumns = ["participantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["participantId"]),
        Index(value = ["createdAtMs"])
    ]
)
data class SessionEntity(
    @PrimaryKey val sessionId: String, // UUID string
    val participantId: String,
    val randomSeed: Long,
    val createdAtMs: Long,
    val endedAtMs: Long? = null,
    val pressureMvc: Double? = null,
    val baselineSizeScore: Double? = null,
    val assignedAddressText: String // denormalized copy
)