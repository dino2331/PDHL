package com.pdrehab.handwritinglab.feature.export

import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File

class ExcelSummaryWriter {

    private fun headerStyle(wb: Workbook): CellStyle {
        val style = wb.createCellStyle()
        val font = wb.createFont()
        // POI 3.17 호환: boldweight = 700
        font.bold = true
        style.setFont(font)
        return style
    }

    fun writeSummaryXlsx(
        outFile: File,
        sessionsRows: List<List<String>>,
        taskAggregatesRows: List<List<String>>,
        metricsLongRows: List<List<String>>
    ) {
        outFile.parentFile?.mkdirs()

        val wb = XSSFWorkbook()
        val hs = headerStyle(wb)

        fun writeSheet(name: String, header: List<String>, rows: List<List<String>>) {
            val sh = wb.createSheet(name)
            val hRow = sh.createRow(0)
            header.forEachIndexed { i, h ->
                val c = hRow.createCell(i)
                c.setCellValue(h)
                c.cellStyle = hs
            }
            rows.forEachIndexed { rIdx, row ->
                val rr = sh.createRow(rIdx + 1)
                row.forEachIndexed { cIdx, v ->
                    rr.createCell(cIdx).setCellValue(v)
                }
            }
            sh.createFreezePane(0, 1)
        }

        writeSheet(
            "Sessions",
            listOf("participantCode","participantId","sessionId","createdAtIso","endedAtIso","seed","addressText","pressureMvc","baselineSizeScore"),
            sessionsRows
        )
        writeSheet(
            "TaskAggregates",
            listOf("participantCode","sessionId","taskId","primaryMetricKey","primaryValue","primaryUnit","meanSpeedMmps","meanPressureNorm","inAirTimeMs","countNextScreen","pageCount"),
            taskAggregatesRows
        )
        writeSheet(
            "MetricsLong",
            listOf("createdAtIso","createdAtMs","participantCode","participantId","sessionId","taskId","trialIndex","metricKey","value","unit","direction"),
            metricsLongRows
        )

        outFile.outputStream().use { wb.write(it) }
        wb.close()
    }
}