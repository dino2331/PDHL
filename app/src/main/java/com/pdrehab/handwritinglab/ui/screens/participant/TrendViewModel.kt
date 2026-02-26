package com.pdrehab.handwritinglab.ui.screens.participant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdrehab.handwritinglab.data.db.dao.MetricValueDao
import com.pdrehab.handwritinglab.data.db.dao.SessionDao
import com.pdrehab.handwritinglab.data.repo.ProtocolStore
import com.pdrehab.handwritinglab.domain.model.MetricCatalog
import com.pdrehab.handwritinglab.domain.usecase.CurrentSessionStore
import com.pdrehab.handwritinglab.ui.components.TrendPointUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskOption(
    val taskId: String,
    val primaryMetricKey: String
)

data class TrendUi(
    val options: List<TaskOption> = emptyList(),
    val selected: TaskOption? = null,
    val direction: MetricDirection = MetricDirection.NEUTRAL,
    val unit: String = "",
    val baselineValue: Double? = null,
    val points: List<TrendPointUi> = emptyList(),
    val deltas: List<Double?> = emptyList(),
    val improvedFlags: List<Boolean?> = emptyList()
)

@HiltViewModel
class TrendViewModel @Inject constructor(
    private val store: CurrentSessionStore,
    private val protocol: ProtocolStore,
    private val sessionDao: SessionDao,
    private val metricDao: MetricValueDao
) : ViewModel() {

    private val _ui = MutableStateFlow(TrendUi())
    val ui: StateFlow<TrendUi> = _ui

    fun init() {
        val options = (1..15).map { i ->
            val id = "T" + i.toString().padStart(2, '0')
            val spec = protocol.findTask(id)
            TaskOption(taskId = id, primaryMetricKey = spec.result.primaryMetricKey)
        }
        _ui.value = _ui.value.copy(options = options, selected = options.firstOrNull())
        _ui.value.selected?.let { load(it) }
    }

    fun select(opt: TaskOption) {
        _ui.value = _ui.value.copy(selected = opt)
        load(opt)
    }

    private fun load(opt: TaskOption) {
        viewModelScope.launch {
            val participant = store.participant.value ?: error("participant not set")
            val participantId = participant.participantId

            val dir = MetricCatalog.def(opt.primaryMetricKey)?.direction ?: MetricDirection.NEUTRAL
            val unit = MetricCatalog.def(opt.primaryMetricKey)?.unit ?: ""

            val series = metricDao.getTrendSeries(participantId, opt.taskId, opt.primaryMetricKey)
            val points = series.mapIndexed { idx, row ->
                TrendPointUi(xIndex = idx + 1, value = row.value)
            }

            val baselineSessionId = sessionDao.getFirstSessionId(participantId)
            val baselineValue = series.firstOrNull { it.sessionId == baselineSessionId }?.value
                ?: series.firstOrNull()?.value

            val deltas = series.map { row ->
                val v = row.value
                if (v == null || baselineValue == null) null else (v - baselineValue)
            }

            val improvedFlags = series.mapIndexed { idx, row ->
                val d = deltas[idx] ?: return@mapIndexed null
                when (dir) {
                    MetricDirection.LOWER_BETTER -> d < 0.0
                    MetricDirection.HIGHER_BETTER -> d > 0.0
                    MetricDirection.NEUTRAL -> null
                }
            }

            _ui.value = TrendUi(
                options = _ui.value.options,
                selected = opt,
                direction = dir,
                unit = unit,
                baselineValue = baselineValue,
                points = points,
                deltas = deltas,
                improvedFlags = improvedFlags
            )
        }
    }
}