package com.pdrehab.handwritinglab

import com.pdrehab.handwritinglab.core.io.CsvWriter
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CsvWriterTest {

    @Test fun csv_escape_quotes_commas() {
        val f = File("build/tmp/test.csv").apply { parentFile?.mkdirs() }
        CsvWriter.writeCsv(
            f,
            header = listOf("a","b"),
            rows = listOf(listOf("hello,world", "he said \"ok\""))
        )
        val t = f.readText(Charsets.UTF_8)
        assertTrue(t.contains("\"hello,world\""))
        assertTrue(t.contains("\"he said \"\"ok\"\"\""))
    }
}