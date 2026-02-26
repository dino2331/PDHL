package com.pdrehab.handwritinglab.ui.screens.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdrehab.handwritinglab.core.JsonUtil
import com.pdrehab.handwritinglab.core.PdhlPaths
import com.pdrehab.handwritinglab.data.db.dao.SessionDao
import com.pdrehab.handwritinglab.data.storage.EventLogger
import com.pdrehab.handwritinglab.domain.usecase.CurrentSessionStore
import com.pdrehab.handwritinglab.feature.analysis.features.FeatureExtractor
import com.pdrehab.handwritinglab.feature.analysis.micrographia.MicrographiaDetector
import com.pdrehab.handwritinglab.feature.taskrunner.input.MotionSample
import com.pdrehab.handwritinglab.data.repo.ProtocolStore
import com.pdrehab.handwritinglab.data.db.dao.TaskInstanceDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import javax.inject.Inject

@Serializable
data class SizeCalibFile(
    val baselineSizeScore: Double,
    val clustersUsed: Int
)

data class SizeUi(
    val message: String = "“강강강”을 편한 크기로 써주세요",
    val error: String? = null
)

@HiltViewModel
class SizeCalibrationViewModel @Inject constructor(
    private val store: CurrentSessionStore,
    private val sessionDao: SessionDao,
    private val taskDao: TaskInstanceDao,
    private val protocol: ProtocolStore,
    private val paths: PdhlPaths,
    private val events: EventLogger
) : ViewModel() {

    private val _ui = MutableStateFlow(SizeUi())
    val ui: StateFlow<SizeUi> = _ui

    private val samples = ArrayList<MotionSample>()
    private var canvasW: Int = 1
    private var canvasH: Int = 1

    fun onCanvasSize(w: Int, h: Int) { canvasW = w; canvasH = h }

    fun onSample(s: MotionSample) {
        samples.add(s)
    }

    fun onComplete(onStartFirstTask: (String) -> Unit) {
        val session = store.session.value ?: return
        val participant = store.participant.value ?: return

        viewModelScope.launch {
            // cluster 기반으로 3개 음절 추정(가장 단순: clusterId 그룹)
            val units = FeatureExtractor.computeWriting(
                samples = samples,
                unitMode = "CLUSTER",
                pressureMvc = session.pressureMvc,
                canvasW = canvasW,
                canvasH = canvasH,
                computeBaselineDeviation = false
            ).units.sortedBy { it.startMs }

            if (units.size < 3) {
                _ui.value = _ui.value.copy(error = "3개 음절이 인식되지 않았습니다. 재시도 해주세요.")
                return@launch
            }

            val alpha = protocol.load().defaults.micrographia.alpha
            val beta = protocol.load().defaults.micrographia.beta
            val first3 = units.take(3)
            val score = first3.map { alpha * it.heightMm + beta * it.widthMm }.average()

            val s2 = session.copy(baselineSizeScore = score)
            sessionDao.upsert(s2)
            store.setSession(s2)

            val out = File(paths.sessionDir(session.sessionId), "calibration_size.json")
            out.writeText(JsonUtil.json.encodeToString(SizeCalibFile(score, 3)), Charsets.UTF_8)

            events.log(session.sessionId, participant.participantCode, "CALIB_SIZE_DONE", System.currentTimeMillis(),
                payload = mapOf("baselineSizeScore" to score.toString())
            )

            // 첫 taskInstanceId 찾아서 시작
            val first = taskDao.getFirstByOrder(session.sessionId) ?: error("no task plan")
            onStartFirstTask(first.taskInstanceId)
        }
    }

    fun onRetry() {
        samples.clear()
        _ui.value = _ui.value.copy(error = null)
    }
}