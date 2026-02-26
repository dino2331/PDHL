package com.pdrehab.handwritinglab.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdrehab.handwritinglab.data.db.dao.MetricValueDao
import com.pdrehab.handwritinglab.data.db.dao.ParticipantDao
import com.pdrehab.handwritinglab.data.db.dao.SessionDao
import com.pdrehab.handwritinglab.data.db.dao.TaskInstanceDao
import com.pdrehab.handwritinglab.data.repo.ProtocolStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskSummaryRow(
    val taskId: String,
    val primaryKey: String,
    val value: Double?
)

data class SessionSummaryUiState(
    val loading: Boolean = true,
    val participantCode: String = "",
    val sessionId: String = "",
    val addressText: String = "",
    val rows: List<TaskSummaryRow> = emptyList()
)

@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    private val protocolStore: ProtocolStore,
    private val sessionDao: SessionDao,
    private val participantDao: ParticipantDao,
    private val taskDao: TaskInstanceDao,
    private val metricDao: MetricValueDao
) : ViewModel() {

    private val _ui = MutableStateFlow(SessionSummaryUiState())
    val ui: StateFlow<SessionSummaryUiState> = _ui

    fun load(sessionId: String) {
        viewModelScope.launch {
            val session = sessionDao.getById(sessionId) ?: return@launch
            if (session.endedAtMs == null) {
                sessionDao.updateEndedAt(sessionId, System.currentTimeMillis())
            }
            val p = participantDao.getById(session.participantId) ?: return@launch

            val protocol = protocolStore.load()
            val taskIds = taskDao.getDistinctTaskIdsOrdered(sessionId)
            val agg = metricDao.getAllTaskAggregateMetrics(sessionId)
                .groupBy { it.taskId!! }
                .mapValues { it.value.associateBy { m -> m.metricKey } }

            val rows = taskIds.map { tid ->
                val spec = protocol.findTask(tid)
                val pk = spec.result.primaryMetricKey
                val v = agg[tid]?.get(pk)?.value
                TaskSummaryRow(tid, pk, v)
            }

            _ui.value = SessionSummaryUiState(
                loading = false,
                participantCode = p.participantCode,
                sessionId = sessionId,
                addressText = session.assignedAddressText,
                rows = rows
            )
        }
    }
}