package com.pdrehab.handwritinglab.data.repo

import android.content.Context
import com.pdrehab.handwritinglab.core.JsonUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class Protocol(
    val version: String,
    val defaults: Defaults,
    val randomization: Randomization,
    val taskGroups: List<TaskGroup>
)

@Serializable
data class Defaults(
    val trialCount: Int = 2,
    val gapThresholdMs: Long = 350,
    val nextButton: NextButtonDefaults,
    val distribution: DistributionDefaults,
    val micrographia: MicrographiaDefaults,
    val appleGame: AppleGameDefaults,
    val restFeedback: RestFeedbackDefaults,
    val addressPolicy: AddressPolicyDefaults
)

@Serializable
data class NextButtonDefaults(
    val visibility: String = "ALWAYS",
    val rightEdgeThresholdRatio: Double = 0.92,
    val boxAllCount: Int = 12,
    val latchEnabledPerPage: Boolean = true
)

@Serializable
data class DistributionDefaults(
    val minN: Int = 20,
    val latestSessionPerParticipant: Boolean = true,
    val excludeSelf: Boolean = true,
    val chart: DistributionChart
)

@Serializable
data class DistributionChart(
    val type: String = "HISTOGRAM",
    val bins: Int = 10,
    val range: String = "P5_P95_CLAMP"
)

@Serializable
data class MicrographiaDefaults(
    val alpha: Double = 0.75,
    val beta: Double = 0.30,
    val baselineClusters: Int = 3,
    val rollingWindow: Int = 3,
    val thresholdRatio: Double = 0.75,
    val confirmConsecutive: Int = 3
)

@Serializable
data class AppleGameDefaults(
    val successHoldMs: Long = 300,
    val successTarget: Int = 10
)

@Serializable
data class RestFeedbackDefaults(
    val restDurationSec: Int = 10,
    val pauseTimerDuringRest: Boolean = true,
    val lockInputDuringRest: Boolean = true
)

@Serializable
data class AddressPolicyDefaults(
    val mode: String = "PARTICIPANT_FIXED",
    val writeUnits: Int = 12,
    val assetFile: String = "addresses_ko_12_nospace_100.json"
)

@Serializable
data class Randomization(
    val shuffleTaskGroups: Boolean = true,
    val shuffleTasksWithinGroup: Boolean = true
)

@Serializable
data class TaskGroup(
    val id: String,
    val name: String,
    val tasks: List<TaskSpec>
)

@Serializable
data class TaskSpec(
    val id: String,
    val name: String,
    val durationSec: Int,
    val trialCount: Int = 2,
    val ui: UiSpec,
    val analysis: AnalysisSpec,
    val result: ResultSpec,
    val difficultyByTrial: List<DifficultyByTrial>? = null
)

@Serializable
data class DifficultyByTrial(
    val trial: Int,
    val level: String
)

@Serializable
data class UiSpec(
    val guide: String = "NONE",
    val promptDisplay: String,
    val nextRule: String? = null,
    val instructionTitle: String,
    val instructionBody: List<String>
)

@Serializable
data class AnalysisSpec(
    val unit: String = "CLUSTER", // CLUSTER|BOX|TASK
    val computeBaselineDeviation: Boolean = false,
    val realtimeMicrographia: Boolean = false,
    val feedbackMode: String? = null
)

@Serializable
data class ResultSpec(
    val primaryMetricKey: String,
    val secondaryMetricKeys: List<String> = emptyList()
)

@Singleton
class ProtocolStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile private var cached: Protocol? = null

    fun load(): Protocol {
        val c = cached
        if (c != null) return c
        val json = context.assets.open("protocol_v1_4_final.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val p = JsonUtil.json.decodeFromString<Protocol>(json)
        cached = p
        return p
    }

    fun findTask(taskId: String): TaskSpec {
        val p = load()
        for (g in p.taskGroups) {
            for (t in g.tasks) if (t.id == taskId) return t
        }
        error("Task not found: $taskId")
    }

    fun allTaskIds(): List<String> =
        load().taskGroups.flatMap { it.tasks }.map { it.id }
}