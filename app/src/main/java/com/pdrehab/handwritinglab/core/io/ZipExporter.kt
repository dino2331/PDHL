package com.pdrehab.handwritinglab.core.io

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipExporter {
    fun zipDirectory(inputDir: File, outZip: File) {
        outZip.parentFile?.mkdirs()
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outZip))).use { zos ->
            inputDir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val rel = inputDir.toPath().relativize(file.toPath()).toString().replace("\\", "/")
                    zos.putNextEntry(ZipEntry(rel))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
        }
    }
}