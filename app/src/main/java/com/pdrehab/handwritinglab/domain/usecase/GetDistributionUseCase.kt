package com.pdrehab.handwritinglab.domain.usecase

import com.pdrehab.handwritinglab.core.PdhlConstants
import com.pdrehab.handwritinglab.data.db.dao.MetricValueDao
import com.pdrehab.handwritinglab.feature.analysis.histogram.Hist
import com.pdrehab.handwritinglab.feature.analysis.histogram.computeHist
import javax.inject.Inject

data class DistResult(val n: Int, val hist: Hist?)

class GetDistributionUseCase @Inject constructor(
    private val metricDao: MetricValueDao
) {
    suspend operator fun invoke(
        selfParticipantId: String,
        sessionId: String,
        taskId: String,
        metricKey: String
    ): DistResult {
        val n = metricDao.getDistributionCountLatestSessionPerParticipant(selfParticipantId, taskId, metricKey)
        if (n < PdhlConstants.MIN_DISTRIBUTION_N) return DistResult(n, null)
        val values = metricDao.getDistributionValuesLatestSessionPerParticipant(selfParticipantId, taskId, metricKey)
        // my value는 별도 쿼리에서 가져오므로 여기선 hist만 계산할 때 입력 필요 -> 호출측에서 myValue 제공하는 구조가 더 맞지만
        // 현재는 화면에서 myValue를 별도 제공하므로, 여기서는 hist 계산을 하지 않고 values만 반환해도 됨.
        // 단, TaskResultViewModel에서 computeHist를 직접 호출하면 더 깔끔하지만, 일단 요구 충족을 위해 임시로 myValue=values.median 근처로 둠.
        // => 실제 hist는 TaskResultViewModel에서 myValue로 다시 computeHist 하도록 아래처럼 nullable 유지.
        return DistResult(n, null)
    }

    suspend fun computeHistogram(values: List<Double>, myValue: Double): Hist {
        return computeHist(values, myValue, bins = PdhlConstants.HIST_BINS)
    }
}