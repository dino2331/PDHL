package com.pdrehab.handwritinglab.ui.screens.participant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdrehab.handwritinglab.data.db.dao.MetricValueDao
import com.pdrehab.handwritinglab.data.db.dao.SessionDao
import com.pdrehab.handwritinglab.data.db.dao.TaskInstanceDao
import com.pdrehab.handwritinglab.data.repo.ProtocolStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistorySessionRow(val taskId: String, val primaryKey: String, val value: Double?)

data class HistorySessionUiState(
    val loading: Boolean = true,
    val sessionId: String = "",
    val addressText: String = "",
    val rows: List<HistorySessionRow> = emptyList()
)

@HiltViewModel
class HistorySessionViewModel @Inject constructor(
    private val protocolStore: ProtocolStore,
    private val sessionDao: SessionDao,
    private val taskDao: TaskInstanceDao,
    private val metricDao: MetricValueDao
) : ViewModel() {

    private val _ui = MutableStateFlow(HistorySessionUiState())
    val ui: StateFlow<HistorySessionUiState> = _ui

    fun load(sessionId: String) {
        viewModelScope.launch {
            val s = sessionDao.getById(sessionId) ?: return@launch
            val protocol = protocolStore.load()
            val taskIds = taskDao.getDistinctTaskIdsOrdered(sessionId)
            val agg = metricDao.getAllTaskAggregateMetrics(sessionId)
                .groupBy { it.taskId!! }
                .mapValues { it.value.associateBy { m -> m.metricKey } }

            val rows = taskIds.map { tid ->
                val spec = protocol.findTask(tid)
                val pk = spec.result.primaryMetricKey
                val v = agg[tid]?.get(pk)?.value
                HistorySessionRow(tid, pk, v)
            }
            _ui.value = HistorySessionUiState(
                loading = false,
                sessionId = sessionId,
                addressText = s.assignedAddressText,
                rows = rows
            )
        }
    }
}