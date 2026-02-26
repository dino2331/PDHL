package com.pdrehab.handwritinglab.ui.screens.task

import android.media.ToneGenerator
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdrehab.handwritinglab.core.JsonUtil
import com.pdrehab.handwritinglab.core.PdhlConstants
import com.pdrehab.handwritinglab.core.PdhlPaths
import com.pdrehab.handwritinglab.data.db.dao.MetricValueDao
import com.pdrehab.handwritinglab.data.db.dao.ParticipantDao
import com.pdrehab.handwritinglab.data.db.dao.SessionDao
import com.pdrehab.handwritinglab.data.db.dao.TaskInstanceDao
import com.pdrehab.handwritinglab.data.db.entity.MetricValueEntity
import com.pdrehab.handwritinglab.data.storage.EventLogger
import com.pdrehab.handwritinglab.data.repo.ProtocolStore
import com.pdrehab.handwritinglab.domain.usecase.ComputeTaskAggregateUseCase
import com.pdrehab.handwritinglab.feature.analysis.features.FeatureExtractor
import com.pdrehab.handwritinglab.feature.analysis.micrographia.MicrographiaDetector
import com.pdrehab.handwritinglab.feature.taskrunner.guides.GuideType
import com.pdrehab.handwritinglab.feature.taskrunner.guides.GuideGeometry
import com.pdrehab.handwritinglab.feature.taskrunner.input.MotionSample
import com.pdrehab.handwritinglab.feature.taskrunner.input.RawWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.math.min

data class TaskRunUi(
    val taskId: String = "",
    val taskName: String = "",
    val prompt: String = "",
    val trialIndex: Int = 1,
    val rightInfo: String = "",
    val durationMs: Long = 30_000L,
    val remainingMs: Long = 30_000L,

    val showInstruction: Boolean = true,
    val instructionTitle: String = "",
    val instructionBody: List<String> = emptyList(),

    val guideType: GuideType = GuideType.NONE,
    val nextVisible: Boolean = false,
    val nextEnabled: Boolean = false,
    val completeVisible: Boolean = false,
    val completeEnabled: Boolean = false,

    val feedbackBanner: String? = null,
    val restOverlay: Boolean = false
)

sealed class TaskRunNav {
    data class ToTrial(val nextTaskInstanceId: String) : TaskRunNav()
    data class ToResult(val sessionId: String, val taskId: String) : TaskRunNav()
}

@Serializable
data class FeaturesSummaryFile(
    val taskId: String,
    val trialIndex: Int,
    val durationMs: Long,
    val completionTimeMs: Long,
    val metrics: Map<String, Double?>,
    val notes: Map<String, String> = emptyMap()
)

@Serializable
data class UnitFeatFile(
    val kind: String,
    val unitId: Int,
    val startMs: Long,
    val endMs: Long,
    val pathLengthMm: Double,
    val widthMm: Double,
    val heightMm: Double
)

@HiltViewModel
class TaskRunViewModel @Inject constructor(
    private val taskDao: TaskInstanceDao,
    private val sessionDao: SessionDao,
    private val participantDao: ParticipantDao,
    private val metricDao: MetricValueDao,
    private val protocol: ProtocolStore,
    private val paths: PdhlPaths,
    private val events: EventLogger,
    private val aggregate: ComputeTaskAggregateUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(TaskRunUi())
    val ui: StateFlow<TaskRunUi> = _ui

    private val _nav = MutableSharedFlow<TaskRunNav>(extraBufferCapacity = 1)
    val nav = _nav

    private var taskInstanceId: String = ""
    private var sessionId: String = ""
    private var participantCode: String = ""
    private var participantId: String = ""
    private var taskId: String = ""
    private var trialIndex: Int = 1

    private var durationMs: Long = 30_000L
    private var running = false
    private var trialStartMs: Long = 0L

    private var restUntilMs: Long = 0L
    private var restTotalMs: Long = 0L
    private var restShownCount: Int = 0

    private var canvasW = 1
    private var canvasH = 1

    private val samples = ArrayList<MotionSample>(12000)
    private var rawWriter: RawWriter? = null

    // Next 정책
    private var nextRule: String? = null
    private var nextLatched = false
    private var pageIndex = 0
    private var countNext = 0
    private var pageCount = 1
    private val filled12 = BooleanArray(12) { false }

    // TG3 상태(최소 구현)
    private var filledBoxesSet = HashSet<Int>()
    private var orderExpected = 0
    private var orderErrorCount = 0
    private var completionAchieved = false
    private var completionTimeMs: Long = 0L

    private var successCount = 0
    private var dropCount = 0
    private var firstSuccessAt: Long? = null
    private var lastSuccessAt: Long? = null
    private var successHoldStart: Long? = null

    private var appleDragging = false
    private var appleX = -1f
    private var appleY = -1f
    private var applePathMm = 0.0
    private var prevAppleX = -1f
    private var prevAppleY = -1f

    // Micrographia
    private var micro: MicrographiaDetector? = null
    private var curClusterId = -1
    private var clusterMinX = Float.POSITIVE_INFINITY
    private var clusterMaxX = Float.NEGATIVE_INFINITY
    private var clusterMinY = Float.POSITIVE_INFINITY
    private var clusterMaxY = Float.NEGATIVE_INFINITY
    private var clusterStartMs = 0L
    private var clusterEndMs = 0L
    private var visualShown = 0
    private var audioCount = 0

    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)

    fun load(taskInstanceId: String, assignedAddressText: String) {
        this.taskInstanceId = taskInstanceId

        viewModelScope.launch(Dispatchers.IO) {
            val ti = taskDao.getById(taskInstanceId) ?: error("taskInstance not found")
            sessionId = ti.sessionId
            taskId = ti.taskId
            trialIndex = ti.trialIndex

            val session = sessionDao.getById(sessionId) ?: error("session not found")
            val participant = participantDao.getById(session.participantId) ?: error("participant not found")
            participantId = participant.participantId
            participantCode = participant.participantCode

            val spec = protocol.findTask(taskId)
            durationMs = spec.durationSec * 1000L

            val prompt = spec.ui.promptDisplay.replace("{{assignedAddressText}}", session.assignedAddressText)

            nextRule = spec.ui.nextRule
            val guideType = mapGuide(spec.ui.guide)

            // micrographia detector only for realtimeMicrographia tasks
            micro = if (spec.analysis.realtimeMicrographia) {
                val m = protocol.load().defaults.micrographia
                MicrographiaDetector(
                    alpha = m.alpha, beta = m.beta,
                    baselineClusters = m.baselineClusters,
                    rollingWindow = m.rollingWindow,
                    thresholdRatio = m.thresholdRatio,
                    confirmConsecutive = m.confirmConsecutive
                )
            } else null

            resetPerTrial()

            _ui.value = TaskRunUi(
                taskId = taskId,
                taskName = spec.name,
                prompt = prompt,
                trialIndex = trialIndex,
                rightInfo = "${spec.durationSec}s  ${trialIndex}/2",
                durationMs = durationMs,
                remainingMs = durationMs,
                showInstruction = true,
                instructionTitle = spec.ui.instructionTitle,
                instructionBody = spec.ui.instructionBody,
                guideType = guideType,
                nextVisible = isWritingTask(taskId),
                nextEnabled = false,
                completeVisible = taskId == "T10",
                completeEnabled = false
            )
        }
    }

    fun onCanvasSize(w: Int, h: Int) { canvasW = w; canvasH = h }

    fun onStartClicked(openRawFile: (File) -> Unit) {
        if (running) return
        running = true
        trialStartMs = System.currentTimeMillis()
        completionAchieved = false
        completionTimeMs = durationMs

        viewModelScope.launch(Dispatchers.IO) {
            // task_instances startedAt
            val ti = taskDao.getById(taskInstanceId) ?: return@launch
            taskDao.upsert(ti.copy(startedAtMs = trialStartMs))

            // raw writer open
            val rawFile = paths.rawGz(sessionId, taskId, trialIndex)
            rawWriter = RawWriter(rawFile, viewModelScope)
            rawWriter?.writeMarker(taskId, trialIndex)
            openRawFile(rawFile)

            events.log(sessionId, participantCode, "TASK_STARTED", trialStartMs, taskId = taskId, trialIndex = trialIndex)
        }

        _ui.value = _ui.value.copy(showInstruction = false)
        startTimerLoop()
    }

    private fun startTimerLoop() {
        viewModelScope.launch {
            var last = System.currentTimeMillis()
            while (running) {
                delay(50)
                val now = System.currentTimeMillis()
                val dt = now - last
                last = now

                val inRest = now < restUntilMs
                val pauseTimer = protocol.load().defaults.restFeedback.pauseTimerDuringRest
                val dec = if (inRest && pauseTimer) 0L else dt

                val rem = (_ui.value.remainingMs - dec).coerceAtLeast(0L)
                _ui.value = _ui.value.copy(
                    remainingMs = rem,
                    restOverlay = inRest
                )

                if (rem <= 0L) {
                    finishTrial()
                    break
                }
            }
        }
    }

    fun onSample(s: MotionSample) {
        if (!running) return

        // rest 중 입력 잠금(스펙)
        if (System.currentTimeMillis() < restUntilMs && protocol.load().defaults.restFeedback.lockInputDuringRest) {
            return
        }

        samples.add(s)
        rawWriter?.writeSample(s)

        // cluster boundary 감지(현재 clusterId 변화)
        if (micro != null) {
            if (curClusterId < 0) {
                startCluster(s)
            } else if (s.clusterId != curClusterId && s.action == "DOWN") {
                finalizeCluster()
                startCluster(s)
            } else {
                extendCluster(s)
            }
        }

        // Next 정책 업데이트(쓰기 tasks)
        if (isWritingTask(taskId) && s.action == "DOWN") {
            when (nextRule) {
                "RIGHT_EDGE" -> {
                    val thr = (PdhlConstants.NEXT_RIGHT_EDGE_RATIO * canvasW.toFloat())
                    if (s.xPx >= thr) nextLatched = true
                }
                "ALL_BOXES_12" -> {
                    if (s.boxId in 0..11) filled12[s.boxId] = true
                    if (filled12.all { it }) nextLatched = true
                }
            }
            _ui.value = _ui.value.copy(nextEnabled = nextLatched)
        }

        // TG3 최소 로직
        when (taskId) {
            "T10" -> { // filledBoxes >=10 -> complete enabled
                if (s.action == "DOWN" && s.boxId >= 0) {
                    filledBoxesSet.add(s.boxId)
                    _ui.value = _ui.value.copy(completeEnabled = filledBoxesSet.size >= 10)
                    if (filledBoxesSet.size >= 10 && !completionAchieved) {
                        // 자동 완료는 하지 않고 버튼 활성만
                    }
                }
            }
            "T12" -> { // numbered order error
                if (s.action == "DOWN" && s.boxId in 0..11) {
                    val firstTime = filledBoxesSet.add(s.boxId)
                    if (firstTime) {
                        if (s.boxId != orderExpected) orderErrorCount += 1
                        orderExpected = (orderExpected + 1).coerceAtMost(12)
                        if (filledBoxesSet.size == 12 && !completionAchieved) {
                            completionAchieved = true
                            completionTimeMs = (System.currentTimeMillis() - trialStartMs).coerceAtLeast(0L)
                            finishTrial()
                        }
                    }
                }
            }
            else -> Unit
        }
    }

    fun onNextPage(clearCanvas: () -> Unit, showArrow: () -> Unit) {
        if (!running) return
        if (!nextLatched) return

        // clear + pageIndex++ + countNext++ + pageCount++
        pageIndex += 1
        countNext += 1
        pageCount += 1
        nextLatched = false
        filled12.fill(false)
        _ui.value = _ui.value.copy(nextEnabled = false)

        clearCanvas()
        showArrow()

        viewModelScope.launch(Dispatchers.IO) {
            events.log(sessionId, participantCode, "NEXT_PAGE_CLICKED", System.currentTimeMillis(),
                taskId = taskId, trialIndex = trialIndex,
                payload = mapOf("pageIndex" to pageIndex.toString())
            )
        }
    }

    fun onCompleteClicked() {
        if (!running) return
        if (taskId != "T10") return
        if (!_ui.value.completeEnabled) return
        completionAchieved = true
        completionTimeMs = (System.currentTimeMillis() - trialStartMs).coerceAtLeast(0L)
        finishTrial()
    }

    private fun finishTrial() {
        if (!running) return
        running = false
        val endMs = System.currentTimeMillis()

        viewModelScope.launch(Dispatchers.IO) {
            // cluster finalize
            if (micro != null) finalizeCluster()

            rawWriter?.close()
            rawWriter = null

            // metrics compute + files save + DB insert
            val session = sessionDao.getById(sessionId) ?: return@launch

            val spec = protocol.findTask(taskId)
            val unitMode = spec.analysis.unit

            val writing = if (unitMode == "CLUSTER" || unitMode == "BOX") {
                FeatureExtractor.computeWriting(
                    samples = samples,
                    unitMode = unitMode,
                    pressureMvc = session.pressureMvc,
                    canvasW = canvasW,
                    canvasH = canvasH,
                    computeBaselineDeviation = spec.analysis.computeBaselineDeviation
                )
            } else null

            val metrics = LinkedHashMap<String, Double?>()

            if (spec.result.primaryMetricKey == "SIZE_REDUCTION_PCT") {
                metrics["SIZE_REDUCTION_PCT"] = writing?.sizeReductionPct
                metrics["MEAN_SPEED_MMPS"] = writing?.meanSpeedMmps
                metrics["MEAN_PRESSURE_NORM"] = writing?.meanPressureNorm
                metrics["IN_AIR_TIME_MS"] = writing?.inAirTimeMs
                metrics["ON_SURFACE_TIME_MS"] = writing?.onSurfaceTimeMs
                metrics["ON_SURFACE_TO_INAIR_RATIO"] = writing?.onSurfaceToInAirRatio
                metrics["STROKE_COUNT"] = writing?.strokeCount
                metrics["HOVER_RATIO_PCT"] = writing?.hoverRatioPct
                if (spec.analysis.computeBaselineDeviation) {
                    metrics["BASELINE_DEV_RMS_MM"] = writing?.baselineDevRmsMm
                    metrics["INSIDE_GUIDE_RATIO_PCT"] = writing?.insideGuideRatioPct
                }
                // Next/Page
                metrics["COUNT_NEXT_SCREEN"] = countNext.toDouble()
                metrics["PAGE_COUNT"] = pageCount.toDouble()

                // Micrographia tasks(T13~T15)
                micro?.let { m ->
                    metrics["MICROGRAPHIA_CONFIRMED_COUNT"] = m.confirmedCount.toDouble()
                    metrics["MICROGRAPHIA_ACTIVE_SECONDS"] = (m.activeMs / 1000.0)
                    metrics["VISUAL_FEEDBACK_SHOWN_COUNT"] = visualShown.toDouble()
                    metrics["AUDIO_FEEDBACK_TRIGGER_COUNT"] = audioCount.toDouble()
                    metrics["REST_SHOWN_COUNT"] = restShownCount.toDouble()
                    metrics["REST_TOTAL_TIME_MS"] = restTotalMs.toDouble()
                }
            } else {
                // TG3 primary: COMPLETION_TIME_MS
                metrics["COMPLETION_TIME_MS"] = completionTimeMs.toDouble()
                if (taskId == "T09") {
                    metrics["SUCCESS_COUNT"] = successCount.toDouble()
                    metrics["DROP_COUNT"] = dropCount.toDouble()
                    val meanPer = if (successCount > 0 && firstSuccessAt != null && lastSuccessAt != null) {
                        (lastSuccessAt!! - firstSuccessAt!!).toDouble() / successCount.toDouble()
                    } else null
                    metrics["MEAN_TIME_PER_SUCCESS_MS"] = meanPer
                    metrics["DRAG_PATH_LENGTH_MM"] = applePathMm
                }
                if (taskId == "T10") metrics["FILLED_BOX_COUNT"] = filledBoxesSet.size.toDouble()
                if (taskId == "T12") metrics["ORDER_ERROR_COUNT"] = orderErrorCount.toDouble()
            }

            // features files
            val trialDir = paths.trialDir(sessionId, taskId, trialIndex).apply { mkdirs() }
            val summaryFile = File(trialDir, "features_summary.json")
            summaryFile.writeText(
                JsonUtil.json.encodeToString(
                    FeaturesSummaryFile(
                        taskId = taskId,
                        trialIndex = trialIndex,
                        durationMs = durationMs,
                        completionTimeMs = completionTimeMs,
                        metrics = metrics
                    )
                ),
                Charsets.UTF_8
            )
            val unitsFile = File(trialDir, "features_units.json")
            val unitRows = writing?.units?.map {
                UnitFeatFile(it.kind, it.unitId, it.startMs, it.endMs, it.pathLengthMm, it.widthMm, it.heightMm)
            } ?: emptyList()
            unitsFile.writeText(JsonUtil.json.encodeToString(unitRows), Charsets.UTF_8)

            // MetricValueEntity insert(trial)
            val createdAt = endMs
            val rows = metrics.entries.map { (k, v) ->
                val (unit, dir) = unitAndDir(k)
                MetricValueEntity(
                    metricId = UUID.randomUUID().toString(),
                    participantId = participantId,
                    participantCode = participantCode,
                    sessionId = sessionId,
                    taskId = taskId,
                    trialIndex = trialIndex,
                    metricKey = k,
                    value = v,
                    unit = unit,
                    direction = dir,
                    createdAtMs = createdAt
                )
            }
            metricDao.upsertAll(rows)

            // task_instances update (endedAt + paging)
            val ti = taskDao.getById(taskInstanceId) ?: return@launch
            taskDao.upsert(
                ti.copy(
                    endedAtMs = endMs,
                    countNextScreen = countNext,
                    pageCount = pageCount
                )
            )

            events.log(sessionId, participantCode, "TASK_ENDED", endMs, taskId = taskId, trialIndex = trialIndex)

            // next routing
            val next = taskDao.getNextByOrder(sessionId, ti.orderInSession)
            if (next != null && next.taskId == taskId) {
                // trial1 -> trial2
                _nav.tryEmit(TaskRunNav.ToTrial(next.taskInstanceId))
            } else {
                // trial2 끝: aggregate 생성 후 result로
                aggregate.compute(sessionId, taskId)
                _nav.tryEmit(TaskRunNav.ToResult(sessionId, taskId))
            }
        }
    }

    // --- micrographia cluster bbox update
    private fun startCluster(s: MotionSample) {
        curClusterId = s.clusterId
        clusterMinX = s.xPx; clusterMaxX = s.xPx
        clusterMinY = s.yPx; clusterMaxY = s.yPx
        clusterStartMs = s.tMs
        clusterEndMs = s.tMs
    }
    private fun extendCluster(s: MotionSample) {
        clusterMinX = min(clusterMinX, s.xPx); clusterMaxX = maxOf(clusterMaxX, s.xPx)
        clusterMinY = min(clusterMinY, s.yPx); clusterMaxY = maxOf(clusterMaxY, s.yPx)
        clusterEndMs = s.tMs
    }
    private fun finalizeCluster() {
        val m = micro ?: return
        if (curClusterId < 0) return
        val hMm = pxToMmY(clusterMaxY - clusterMinY)
        val wMm = pxToMmX(clusterMaxX - clusterMinX)
        val dur = (clusterEndMs - clusterStartMs).coerceAtLeast(0L)
        val r = m.onCluster(heightMm = hMm, widthMm = wMm, durationMs = dur)

        if (r.confirmed) {
            val spec = protocol.findTask(taskId)
            when (spec.analysis.feedbackMode) {
                "VISUAL" -> {
                    visualShown += 1
                    _ui.value = _ui.value.copy(feedbackBanner = "글자 크기가 작아졌습니다!")
                }
                "AUDIO" -> {
                    audioCount += 1
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                }
                "REST" -> {
                    restShownCount += 1
                    val restMs = protocol.load().defaults.restFeedback.restDurationSec * 1000L
                    val now = System.currentTimeMillis()
                    restUntilMs = now + restMs
                    restTotalMs += restMs
                    _ui.value = _ui.value.copy(feedbackBanner = "잠시 쉬어가세요", restOverlay = true)
                }
            }
        }
    }

    private fun pxToMmX(px: Float): Double {
        val dm = android.content.res.Resources.getSystem().displayMetrics
        return px.toDouble() / dm.xdpi.toDouble() * 25.4
    }
    private fun pxToMmY(px: Float): Double {
        val dm = android.content.res.Resources.getSystem().displayMetrics
        return px.toDouble() / dm.ydpi.toDouble() * 25.4
    }

    private fun resetPerTrial() {
        samples.clear()
        rawWriter = null

        nextLatched = false
        pageIndex = 0
        countNext = 0
        pageCount = 1
        filled12.fill(false)

        filledBoxesSet.clear()
        orderExpected = 0
        orderErrorCount = 0

        completionAchieved = false
        completionTimeMs = durationMs

        successCount = 0
        dropCount = 0
        firstSuccessAt = null
        lastSuccessAt = null
        successHoldStart = null

        appleDragging = false
        appleX = -1f; appleY = -1f
        applePathMm = 0.0
        prevAppleX = -1f; prevAppleY = -1f

        curClusterId = -1
        visualShown = 0
        audioCount = 0
        restUntilMs = 0L
        restTotalMs = 0L
        restShownCount = 0
    }

    private fun mapGuide(s: String): GuideType = when (s) {
        "TWO_HORIZONTAL_LINES" -> GuideType.TWO_HORIZONTAL_LINES
        "TOP_DOTS" -> GuideType.TOP_DOTS
        "BOXES_ROW_12" -> GuideType.BOXES_ROW_12
        "GAME_APPLE" -> GuideType.GAME_APPLE
        "VARIABLE_BOXES_LINE" -> GuideType.VARIABLE_BOXES_LINE
        "TRACE_PATH" -> GuideType.TRACE_PATH
        "NUMBERED_BOXES" -> GuideType.NUMBERED_BOXES
        else -> GuideType.NONE
    }

    private fun isWritingTask(taskId: String): Boolean {
        // PRD: T01~T08, T13~T15
        return taskId in setOf(
            "T01","T02","T03","T04","T05","T06","T07","T08",
            "T13","T14","T15"
        )
    }

    private fun unitAndDir(key: String): Pair<String, String> {
        return when (key) {
            "SIZE_REDUCTION_PCT" -> "%" to "LOWER_BETTER"
            "MEAN_SPEED_MMPS" -> "mm/s" to "NEUTRAL"
            "MEAN_PRESSURE_NORM" -> "ratio" to "NEUTRAL"
            "IN_AIR_TIME_MS" -> "ms" to "LOWER_BETTER"
            "ON_SURFACE_TIME_MS" -> "ms" to "NEUTRAL"
            "ON_SURFACE_TO_INAIR_RATIO" -> "ratio" to "HIGHER_BETTER"
            "STROKE_COUNT" -> "count" to "NEUTRAL"
            "HOVER_RATIO_PCT" -> "%" to "NEUTRAL"
            "COUNT_NEXT_SCREEN" -> "count" to "NEUTRAL"
            "PAGE_COUNT" -> "count" to "NEUTRAL"
            "BASELINE_DEV_RMS_MM" -> "mm" to "LOWER_BETTER"
            "INSIDE_GUIDE_RATIO_PCT" -> "%" to "HIGHER_BETTER"

            "COMPLETION_TIME_MS" -> "ms" to "LOWER_BETTER"
            "SUCCESS_COUNT" -> "count" to "NEUTRAL"
            "DROP_COUNT" -> "count" to "NEUTRAL"
            "MEAN_TIME_PER_SUCCESS_MS" -> "ms" to "NEUTRAL"
            "DRAG_PATH_LENGTH_MM" -> "mm" to "NEUTRAL"
            "FILLED_BOX_COUNT" -> "count" to "NEUTRAL"
            "ORDER_ERROR_COUNT" -> "count" to "LOWER_BETTER"

            "MICROGRAPHIA_CONFIRMED_COUNT" -> "count" to "LOWER_BETTER"
            "MICROGRAPHIA_ACTIVE_SECONDS" -> "sec" to "LOWER_BETTER"
            "VISUAL_FEEDBACK_SHOWN_COUNT" -> "count" to "LOWER_BETTER"
            "AUDIO_FEEDBACK_TRIGGER_COUNT" -> "count" to "LOWER_BETTER"
            "REST_SHOWN_COUNT" -> "count" to "LOWER_BETTER"
            "REST_TOTAL_TIME_MS" -> "ms" to "LOWER_BETTER"
            else -> "" to "NEUTRAL"
        }
    }
}