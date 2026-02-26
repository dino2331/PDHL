package com.pdrehab.handwritinglab.domain.usecase

import com.pdrehab.handwritinglab.data.db.dao.MetricValueDao
import com.pdrehab.handwritinglab.data.db.dao.TrendPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetTrendUseCase @Inject constructor(
    private val metricDao: MetricValueDao
) {
    suspend operator fun invoke(participantId: String, taskId: String, metricKey: String): List<TrendPoint> =
        withContext(Dispatchers.IO) { metricDao.getTrendSeries(participantId, taskId, metricKey) }
}