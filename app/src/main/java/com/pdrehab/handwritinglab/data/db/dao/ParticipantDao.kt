package com.pdrehab.handwritinglab.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pdrehab.handwritinglab.data.db.entity.ParticipantEntity

@Dao
interface ParticipantDao {

    @Query("SELECT * FROM participants WHERE participantCode = :code LIMIT 1")
    suspend fun getByCode(code: String): ParticipantEntity?

    @Query("SELECT * FROM participants WHERE participantId = :participantId LIMIT 1")
    suspend fun getById(participantId: String): ParticipantEntity?

    @Query("SELECT * FROM participants")
    suspend fun getAll(): List<ParticipantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: ParticipantEntity)

    @Query("""
        UPDATE participants
        SET participantCode = :participantCode,
            assignedAddressId = :assignedAddressId,
            assignedAddressText = :assignedAddressText
        WHERE participantId = :participantId
    """)
    suspend fun updateCore(
        participantId: String,
        participantCode: String,
        assignedAddressId: String,
        assignedAddressText: String
    )
}