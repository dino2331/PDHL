package com.pdrehab.handwritinglab

import com.pdrehab.handwritinglab.core.io.ZipExporter
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class ZipExporterTest {

    @Test fun zip_contains_file() {
        val dir = File("build/tmp/zip_in").apply { deleteRecursively(); mkdirs() }
        val f = File(dir, "a.txt").apply { writeText("hi") }
        val out = File("build/tmp/out.zip").apply { parentFile?.mkdirs() }
        if (out.exists()) out.delete()

        ZipExporter.zipDirectory(dir, out)
        assertTrue(out.exists())

        ZipFile(out).use { z ->
            val e = z.getEntry("a.txt")
            assertNotNull(e)
        }
    }
}