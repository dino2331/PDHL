package com.pdrehab.handwritinglab.core.io

import java.io.File

object CsvWriter {
    private const val BOM = "\uFEFF"

    private fun esc(v: String): String {
        val needsQuote = v.contains(',') || v.contains('"') || v.contains('\n') || v.contains('\r')
        if (!needsQuote) return v
        val vv = v.replace("\"", "\"\"")
        return "\"$vv\""
    }

    fun writeCsv(file: File, header: List<String>, rows: List<List<String>>) {
        file.parentFile?.mkdirs()
        file.outputStream().buffered().use { os ->
            os.writer(Charsets.UTF_8).use { w ->
                w.write(BOM)
                w.write(header.joinToString(",") { esc(it) })
                w.write("\n")
                for (r in rows) {
                    w.write(r.joinToString(",") { esc(it) })
                    w.write("\n")
                }
            }
        }
    }
}