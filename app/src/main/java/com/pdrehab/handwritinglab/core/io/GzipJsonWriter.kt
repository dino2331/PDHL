package com.pdrehab.handwritinglab.core.io

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

class GzipJsonlWriter(
    private val file: File,
    scope: CoroutineScope
) {
    private val ch = Channel<String>(capacity = Channel.BUFFERED)
    private val job = scope.launch(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        GZIPOutputStream(BufferedOutputStream(FileOutputStream(file))).bufferedWriter().use { w ->
            for (line in ch) {
                w.write(line)
                w.newLine()
            }
            w.flush()
        }
    }

    fun tryWrite(line: String) {
        ch.trySend(line)
    }

    suspend fun close() {
        ch.close()
        job.join()
    }
}