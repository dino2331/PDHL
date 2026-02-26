package com.pdrehab.handwritinglab.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "participants",
    indices = [
        Index(value = ["participantCode"], unique = true)
    ]
)
data class ParticipantEntity(
    @PrimaryKey val participantId: String, // UUID string
    val participantCode: String,           // ^[A-Z0-9]{8}
    val assignedAddressId: String,         // A001..A100
    val assignedAddressText: String,       // length=12
    val createdAtMs: Long,
    val notes: String? = null
)