package com.pdrehab.handwritinglab.domain.usecase

import com.pdrehab.handwritinglab.core.JsonUtil
import com.pdrehab.handwritinglab.core.PdhlPaths
import com.pdrehab.handwritinglab.data.db.dao.TaskInstanceDao
import com.pdrehab.handwritinglab.data.db.entity.TaskInstanceEntity
import com.pdrehab.handwritinglab.data.repo.ProtocolStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.UUID
import kotlin.random.Random
import javax.inject.Inject

@Serializable
data class TaskPlanFile(
    val sessionId: String,
    val seed: Long,
    val orderedTaskIds: List<String>
)

class CreateTaskPlanUseCase @Inject constructor(
    private val protocol: ProtocolStore,
    private val taskDao: TaskInstanceDao,
    private val paths: PdhlPaths
) {
    suspend fun create(sessionId: String, seed: Long): String = withContext(Dispatchers.IO) {
        val p = protocol.load()
        val rnd = Random(seed)

        val groups = p.taskGroups.toMutableList()
        if (p.randomization.shuffleTaskGroups) groups.shuffle(rnd)

        val taskIds = ArrayList<String>()
        for (g in groups) {
            val tasks = g.tasks.toMutableList()
            if (p.randomization.shuffleTasksWithinGroup) tasks.shuffle(rnd)
            taskIds += tasks.map { it.id }
        }

        var order = 0
        val now = System.currentTimeMillis()
        for (tid in taskIds) {
            for (trial in 1..2) {
                val ti = TaskInstanceEntity(
                    taskInstanceId = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    taskId = tid,
                    trialIndex = trial,
                    orderInSession = order++,
                    startedAtMs = 0L,
                    endedAtMs = 0L,
                    countNextScreen = 0,
                    pageCount = 1
                )
                taskDao.upsert(ti)
            }
        }

        // randomization.json 저장
        val plan = TaskPlanFile(sessionId = sessionId, seed = seed, orderedTaskIds = taskIds)
        val out = File(paths.sessionDir(sessionId), "randomization.json")
        out.writeText(JsonUtil.json.encodeToString(plan), Charsets.UTF_8)

        // 첫 taskInstanceId 반환
        val first = taskDao.getFirstByOrder(sessionId)
        require(first != null) { "task plan empty" }
        first.taskInstanceId
    }
}