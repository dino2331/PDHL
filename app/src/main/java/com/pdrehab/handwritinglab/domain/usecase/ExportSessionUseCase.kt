package com.pdrehab.handwritinglab.domain.usecase

import android.content.Context
import com.pdrehab.handwritinglab.core.PdhlPaths
import com.pdrehab.handwritinglab.core.TimeFormat
import com.pdrehab.handwritinglab.core.io.CsvWriter
import com.pdrehab.handwritinglab.core.io.ZipExporter
import com.pdrehab.handwritinglab.data.db.dao.ExportDao
import com.pdrehab.handwritinglab.data.db.dao.MetricValueDao
import com.pdrehab.handwritinglab.data.db.dao.ParticipantDao
import com.pdrehab.handwritinglab.data.db.dao.SessionDao
import com.pdrehab.handwritinglab.data.db.dao.TaskInstanceDao
import com.pdrehab.handwritinglab.data.repo.ProtocolStore
import com.pdrehab.handwritinglab.data.storage.EventLogger
import com.pdrehab.handwritinglab.feature.export.ExcelSummaryWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class ExportSessionUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val paths: PdhlPaths,
    private val participantDao: ParticipantDao,
    private val sessionDao: SessionDao,
    private val taskDao: TaskInstanceDao,
    private val metricDao: MetricValueDao,
    private val exportDao: ExportDao,
    private val protocol: ProtocolStore,
    private val excel: ExcelSummaryWriter,
    private val events: EventLogger
) {
    suspend fun export(sessionId: String): File = withContext(Dispatchers.IO) {
        val session = sessionDao.getById(sessionId) ?: error("session not found")
        val participant = participantDao.getById(session.participantId) ?: error("participant not found")

        val day = TimeFormat.yyyyMMdd(session.createdAtMs)
        val baseName = "PDHL_export_${participant.participantCode}_${day}_${session.sessionId}"
        val exportRoot = paths.exportsDir()
        val tempDir = File(exportRoot, baseName).apply { mkdirs() }

        events.log(sessionId, participant.participantCode, "EXPORT_STARTED", System.currentTimeMillis())

        // metrics_long.csv
        val metricRows = exportDao.getMetricLongRowsForSession(sessionId)
        val metricsLongCsv = File(tempDir, "metrics_long.csv")
        CsvWriter.writeCsv(
            metricsLongCsv,
            header = listOf("createdAtIso","createdAtMs","participantCode","participantId","sessionId","taskId","trialIndex","metricKey","value","unit","direction"),
            rows = metricRows.map { m ->
                listOf(
                    TimeFormat.iso(m.createdAtMs),
                    m.createdAtMs.toString(),
                    m.participantCode,
                    m.participantId,
                    m.sessionId,
                    m.taskId ?: "",
                    m.trialIndex?.toString() ?: "",
                    m.metricKey,
                    m.value?.toString() ?: "",
                    m.unit,
                    m.direction
                )
            }
        )

        // summary.csv (TaskAggregates 중심)
        val performedTaskIds = taskDao.getDistinctTaskIdsOrdered(sessionId)
        val agg = metricDao.getAllTaskAggregateMetrics(sessionId)
        val aggMap = agg.groupBy { it.taskId!! }.mapValues { e -> e.value.associateBy { it.metricKey } }

        val taskAggRows = performedTaskIds.map { taskId ->
            val spec = protocol.findTask(taskId)
            val primaryKey = spec.result.primaryMetricKey
            val m = aggMap[taskId]

            fun s(key: String) = m?.get(key)?.value?.toString() ?: ""
            fun u(key: String) = m?.get(key)?.unit ?: ""

            listOf(
                participant.participantCode, session.sessionId, taskId,
                primaryKey, s(primaryKey), u(primaryKey),
                s("MEAN_SPEED_MMPS"),
                s("MEAN_PRESSURE_NORM"),
                s("IN_AIR_TIME_MS"),
                s("COUNT_NEXT_SCREEN"),
                s("PAGE_COUNT"),
                s("COMPLETION_TIME_MS"),
                s("SUCCESS_COUNT"),
                s("ORDER_ERROR_COUNT")
            )
        }

        val summaryCsv = File(tempDir, "summary.csv")
        CsvWriter.writeCsv(
            summaryCsv,
            header = listOf(
                "participantCode","sessionId","taskId",
                "primaryMetricKey","primaryValue","primaryUnit",
                "meanSpeedMmps","meanPressureNorm","inAirTimeMs",
                "countNextScreen","pageCount",
                "tg3CompletionTimeMs","tg3SuccessCount","tg3OrderErrorCount"
            ),
            rows = taskAggRows
        )

        // summary.xlsx (실패해도 csv는 남김)
        val summaryXlsx = File(tempDir, "summary.xlsx")
        runCatching {
            val sessionRow = listOf(
                participant.participantCode,
                participant.participantId,
                session.sessionId,
                TimeFormat.iso(session.createdAtMs),
                session.endedAtMs?.let { TimeFormat.iso(it) } ?: "",
                session.randomSeed.toString(),
                session.assignedAddressText,
                session.pressureMvc?.toString() ?: "",
                session.baselineSizeScore?.toString() ?: ""
            )
            excel.writeSummaryXlsx(
                outFile = summaryXlsx,
                sessionRow = sessionRow,
                taskAggregateRows = taskAggRows,
                metricLongRows = metricRows
            )
        }.onFailure {
            events.log(sessionId, participant.participantCode, "EXPORT_XLSX_FAILED", System.currentTimeMillis(),
                payload = mapOf("error" to (it.message ?: it.javaClass.simpleName))
            )
        }

        // db copy
        val dbCopyDir = File(tempDir, "db").apply { mkdirs() }
        paths.dbFile().copyTo(File(dbCopyDir, "pdhl.db"), overwrite = true)

        // sessions copy
        val sessOut = File(tempDir, "sessions").apply { mkdirs() }
        val srcSessDir = paths.sessionDir(sessionId)
        srcSessDir.copyRecursively(File(sessOut, sessionId), overwrite = true)

        // assets copy
        listOf("protocol_v1_4_final.json", "addresses_ko_12_nospace_100.json").forEach { name ->
            val out = File(tempDir, name)
            context.assets.open(name).use { ins ->
                out.outputStream().use { ins.copyTo(it) }
            }
        }

        // zip
        val zipFile = File(exportRoot, "$baseName.zip")
        ZipExporter.zipDirectory(tempDir, zipFile)

        // cleanup
        tempDir.deleteRecursively()

        events.log(sessionId, participant.participantCode, "EXPORT_DONE", System.currentTimeMillis(),
            payload = mapOf("zip" to zipFile.name)
        )

        zipFile
    }
}