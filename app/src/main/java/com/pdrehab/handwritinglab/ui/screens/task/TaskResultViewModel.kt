package com.pdrehab.handwritinglab.ui.screens.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdrehab.handwritinglab.core.PdhlConstants
import com.pdrehab.handwritinglab.data.db.dao.MetricValueDao
import com.pdrehab.handwritinglab.data.db.dao.ParticipantDao
import com.pdrehab.handwritinglab.data.db.dao.SessionDao
import com.pdrehab.handwritinglab.data.db.dao.TaskInstanceDao
import com.pdrehab.handwritinglab.data.repo.ProtocolStore
import com.pdrehab.handwritinglab.feature.analysis.histogram.Hist
import com.pdrehab.handwritinglab.feature.analysis.histogram.computeHist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskResultUi(
    val taskId: String = "",
    val primaryMetricKey: String = "",
    val myValue: Double? = null,
    val unit: String = "",
    val deltaVsFirst: Double? = null,
    val n: Int = 0,
    val hist: Hist? = null,
    val hideChart: Boolean = true,
    val message: String? = null,
    val nextTaskInstanceId: String? = null
)

@HiltViewModel
class TaskResultViewModel @Inject constructor(
    private val protocol: ProtocolStore,
    private val sessionDao: SessionDao,
    private val participantDao: ParticipantDao,
    private val taskDao: TaskInstanceDao,
    private val metricDao: MetricValueDao
) : ViewModel() {

    private val _ui = MutableStateFlow(TaskResultUi())
    val ui: StateFlow<TaskResultUi> = _ui

    fun load(sessionId: String, taskId: String) {
        viewModelScope.launch {
            val spec = protocol.findTask(taskId)
            val primary = spec.result.primaryMetricKey

            val session = sessionDao.getById(sessionId) ?: return@launch
            val participant = participantDao.getById(session.participantId) ?: return@launch

            val my = metricDao.getAggregateValue(sessionId, taskId, primary)

            // Δ vs first session
            val firstSessionId = sessionDao.getFirstSessionId(participant.participantId)
            val firstVal = if (firstSessionId != null) {
                metricDao.getAggregateValue(firstSessionId, taskId, primary)
            } else null
            val delta = if (my != null && firstVal != null) my - firstVal else null

            // distribution
            val n = metricDao.getDistributionCountLatestSessionPerParticipant(
                selfParticipantId = participant.participantId,
                taskId = taskId,
                metricKey = primary
            )
            val hide = n < PdhlConstants.MIN_DISTRIBUTION_N
            val hist = if (!hide && my != null) {
                val dist = metricDao.getDistributionValuesLatestSessionPerParticipant(
                    selfParticipantId = participant.participantId,
                    taskId = taskId,
                    metricKey = primary
                )
                if (dist.isNotEmpty()) computeHist(dist, my, bins = PdhlConstants.HIST_BINS) else null
            } else null

            // next task instance id (trial2 order + 1)
            val t2 = taskDao.getBySessionTaskTrial(sessionId, taskId, 2)
            val next = if (t2 != null) taskDao.getByOrder(sessionId, t2.orderInSession + 1) else null

            _ui.value = TaskResultUi(
                taskId = taskId,
                primaryMetricKey = primary,
                myValue = my,
                unit = unitOf(primary),
                deltaVsFirst = delta,
                n = n,
                hist = hist,
                hideChart = hide || hist == null,
                message = if (hide) "비교 데이터가 부족합니다(n<${PdhlConstants.MIN_DISTRIBUTION_N})" else null,
                nextTaskInstanceId = next?.taskInstanceId
            )
        }
    }

    private fun unitOf(metricKey: String): String =
        when (metricKey) {
            "SIZE_REDUCTION_PCT" -> "%"
            "COMPLETION_TIME_MS" -> "ms"
            else -> ""
        }
}