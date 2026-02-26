package com.pdrehab.handwritinglab.ui.screens.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdrehab.handwritinglab.core.JsonUtil
import com.pdrehab.handwritinglab.core.PdhlPaths
import com.pdrehab.handwritinglab.data.db.dao.SessionDao
import com.pdrehab.handwritinglab.data.storage.EventLogger
import com.pdrehab.handwritinglab.domain.usecase.CurrentSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.floor

@Serializable
data class PressureCalibFile(
    val pressureMvc: Double?,
    val sampleCount: Int,
    val note: String? = null
)

data class PressureUi(
    val status: String = "가장 강한 힘으로 2초간 누르세요",
    val pressureMvc: Double? = null,
    val warning: String? = null
)

@HiltViewModel
class PressureCalibrationViewModel @Inject constructor(
    private val store: CurrentSessionStore,
    private val sessionDao: SessionDao,
    private val paths: PdhlPaths,
    private val events: EventLogger
) : ViewModel() {

    private val _ui = MutableStateFlow(PressureUi())
    val ui: StateFlow<PressureUi> = _ui

    fun onDoneSamples(pressures: List<Float>, onNext: () -> Unit) {
        val session = store.session.value ?: return
        val participant = store.participant.value ?: return

        viewModelScope.launch {
            val nonZero = pressures.any { it > 0f }
            val mvc = if (!nonZero) null else p95(pressures.map { it.toDouble() })

            val s2 = session.copy(pressureMvc = mvc)
            sessionDao.upsert(s2)
            store.setSession(s2)

            val out = File(paths.sessionDir(session.sessionId), "calibration_pressure.json")
            out.writeText(
                JsonUtil.json.encodeToString(
                    PressureCalibFile(
                        pressureMvc = mvc,
                        sampleCount = pressures.size,
                        note = if (!nonZero) "pressure axis unsupported" else null
                    )
                ),
                Charsets.UTF_8
            )

            events.log(session.sessionId, participant.participantCode, "CALIB_PRESSURE_DONE", System.currentTimeMillis(),
                payload = mapOf("pressureMvc" to (mvc?.toString() ?: "NA"))
            )

            _ui.value = _ui.value.copy(
                pressureMvc = mvc,
                warning = if (!nonZero) "압력 축이 지원되지 않습니다(NA 처리). 계속 진행합니다." else null,
                status = "완료"
            )
            onNext()
        }
    }

    private fun p95(xs: List<Double>): Double {
        val s = xs.sorted()
        val n = s.size
        if (n == 1) return s[0]
        val pos = 0.95 * (n - 1)
        val lo = floor(pos).toInt()
        val hi = ceil(pos).toInt()
        val t = pos - lo
        return if (lo == hi) s[lo] else s[lo] * (1.0 - t) + s[hi] * t
    }
}