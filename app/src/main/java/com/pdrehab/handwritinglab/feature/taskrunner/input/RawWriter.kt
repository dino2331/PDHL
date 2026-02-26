package com.pdrehab.handwritinglab.feature.taskrunner.input

import com.pdrehab.handwritinglab.core.JsonUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.*
import java.util.zip.GZIPOutputStream

@Serializable
data class RawMarker(
    val recordType: String = "MARKER",
    val marker: String,
    val tMs: Long,
    val taskId: String,
    val trialIndex: Int
)

@Serializable
data class RawSample(
    val recordType: String = "SAMPLE",
    val tNs: Long,
    val tMs: Long,
    val xPx: Double,
    val yPx: Double,
    val pressure: Double,
    val tilt: Double,
    val orientation: Double,
    val distance: Double,
    val action: String,
    val toolType: String,
    val pointerId: Int,
    val isDown: Boolean,
    val pageIndex: Int,
    val boxId: Int,
    val strokeId: Int,
    val clusterId: Int
)

class RawWriter(
    private val file: File,
    scope: CoroutineScope
) {
    private val ch = Channel<String>(capacity = Channel.BUFFERED)
    private val job = scope.launch(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        GZIPOutputStream(BufferedOutputStream(FileOutputStream(file))).bufferedWriter().use { w ->
            for (line in ch) {
                w.write(line); w.newLine()
            }
            w.flush()
        }
    }

    fun writeMarker(taskId: String, trialIndex: Int) {
        val m = RawMarker(marker = "TASK_STARTED", tMs = System.currentTimeMillis(), taskId = taskId, trialIndex = trialIndex)
        ch.trySend(JsonUtil.json.encodeToString(m))
    }

    fun writeSample(s: MotionSample) {
        val r = RawSample(
            tNs = s.tNs,
            tMs = s.tMs,
            xPx = s.xPx.toDouble(),
            yPx = s.yPx.toDouble(),
            pressure = s.pressure.toDouble(),
            tilt = s.tilt.toDouble(),
            orientation = s.orientation.toDouble(),
            distance = s.distance.toDouble(),
            action = s.action,
            toolType = s.toolType,
            pointerId = s.pointerId,
            isDown = s.isDown,
            pageIndex = s.pageIndex,
            boxId = s.boxId,
            strokeId = s.strokeId,
            clusterId = s.clusterId
        )
        ch.trySend(JsonUtil.json.encodeToString(r))
    }

    suspend fun close() {
        ch.close()
        job.join()
    }
}