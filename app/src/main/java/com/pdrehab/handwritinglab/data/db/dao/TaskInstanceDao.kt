package com.pdrehab.handwritinglab.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pdrehab.handwritinglab.data.db.entity.TaskInstanceEntity

@Dao
interface TaskInstanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(ti: TaskInstanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<TaskInstanceEntity>)

    @Query("SELECT * FROM task_instances WHERE taskInstanceId = :taskInstanceId LIMIT 1")
    suspend fun getById(taskInstanceId: String): TaskInstanceEntity?

    @Query("""
        SELECT * FROM task_instances
        WHERE sessionId = :sessionId
        ORDER BY orderInSession ASC
        LIMIT 1
    """)
    suspend fun getFirstByOrder(sessionId: String): TaskInstanceEntity?

    @Query("""
        SELECT * FROM task_instances
        WHERE sessionId = :sessionId AND orderInSession = :order
        LIMIT 1
    """)
    suspend fun getByOrder(sessionId: String, order: Int): TaskInstanceEntity?

    @Query("""
        SELECT * FROM task_instances
        WHERE sessionId = :sessionId AND taskId = :taskId AND trialIndex = :trialIndex
        LIMIT 1
    """)
    suspend fun getBySessionTaskTrial(sessionId: String, taskId: String, trialIndex: Int): TaskInstanceEntity?

    @Query("""
        SELECT taskId FROM task_instances
        WHERE sessionId = :sessionId
        GROUP BY taskId
        ORDER BY MIN(orderInSession) ASC
    """)
    suspend fun getDistinctTaskIdsOrdered(sessionId: String): List<String>
}