package com.pdrehab.handwritinglab.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pdrehab.handwritinglab.data.db.entity.SessionEntity

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: SessionEntity)

    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getById(sessionId: String): SessionEntity?

    @Query("""
        UPDATE sessions
        SET endedAtMs = :endedAtMs,
            pressureMvc = :pressureMvc,
            baselineSizeScore = :baselineSizeScore
        WHERE sessionId = :sessionId
    """)
    suspend fun updateCalibsAndEnd(
        sessionId: String,
        endedAtMs: Long?,
        pressureMvc: Double?,
        baselineSizeScore: Double?
    )

    @Query("""
        SELECT sessionId FROM sessions
        WHERE participantId = :participantId
        ORDER BY createdAtMs ASC, sessionId ASC
        LIMIT 1
    """)
    suspend fun getFirstSessionId(participantId: String): String?

    @Query("""
        SELECT * FROM sessions
        WHERE participantId = :participantId
        ORDER BY createdAtMs DESC, sessionId DESC
    """)
    suspend fun getSessionsForParticipant(participantId: String): List<SessionEntity>

    @Query("""
        SELECT s.sessionId AS sessionId,
               p.participantCode AS participantCode,
               s.createdAtMs AS createdAtMs,
               s.endedAtMs AS endedAtMs
        FROM sessions s
        JOIN participants p ON p.participantId = s.participantId
        ORDER BY s.createdAtMs DESC, s.sessionId DESC
        LIMIT :limit
    """)
    suspend fun getRecentSessions(limit: Int = 30): List<AdminSessionRow>
}