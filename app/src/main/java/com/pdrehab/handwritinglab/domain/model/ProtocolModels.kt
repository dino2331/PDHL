package com.pdrehab.handwritinglab.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Protocol(
    val version: String,
    val defaults: Defaults? = null,
    val randomization: Randomization? = null,
    val taskGroups: List<TaskGroup>
) {
    fun findTask(taskId: String): TaskSpec {
        for (g in taskGroups) for (t in g.tasks) if (t.id == taskId) return t
        error("Task not found: $taskId")
    }
}

@Serializable
data class Defaults(
    val trialCount: Int = 2,
    val gapThresholdMs: Int = 350,
    val nextButton: NextButtonDefaults? = null,
    val distribution: DistributionDefaults? = null,
    val micrographia: MicroDefaults? = null,
    val appleGame: AppleDefaults? = null,
    val restFeedback: RestDefaults? = null,
    val addressPolicy: AddressPolicyDefaults? = null
)

@Serializable data class NextButtonDefaults(
    val visibility: String = "ALWAYS",
    val rightEdgeThresholdRatio: Double = 0.92,
    val boxAllCount: Int = 12,
    val latchEnabledPerPage: Boolean = true
)

@Serializable data class DistributionDefaults(
    val minN: Int = 20,
    val latestSessionPerParticipant: Boolean = true,
    val excludeSelf: Boolean = true,
    val chart: DistributionChart? = null
)

@Serializable data class DistributionChart(
    val type: String = "HISTOGRAM",
    val bins: Int = 10,
    val range: String = "P5_P95_CLAMP"
)

@Serializable data class MicroDefaults(
    val alpha: Double = 0.75,
    val beta: Double = 0.30,
    val baselineClusters: Int = 3,
    val rollingWindow: Int = 3,
    val thresholdRatio: Double = 0.75,
    val confirmConsecutive: Int = 3
)

@Serializable data class AppleDefaults(
    val successHoldMs: Int = 300,
    val successTarget: Int = 10
)

@Serializable data class RestDefaults(
    val restDurationSec: Int = 10,
    val pauseTimerDuringRest: Boolean = true,
    val lockInputDuringRest: Boolean = true
)

@Serializable data class AddressPolicyDefaults(
    val mode: String = "PARTICIPANT_FIXED",
    val writeUnits: Int = 12,
    val assetFile: String = "addresses_ko_12_nospace_100.json"
)

@Serializable data class Randomization(
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
data class UiSpec(
    val guide: String = "NONE",
    val promptDisplay: String,
    val nextRule: String? = null,
    val instructionTitle: String,
    val instructionBody: List<String>
)

@Serializable
data class AnalysisSpec(
    val unit: String = "CLUSTER", // CLUSTER/BOX/TASK
    val computeBaselineDeviation: Boolean = false,
    val realtimeMicrographia: Boolean = false,
    val feedbackMode: String? = null
)

@Serializable
data class ResultSpec(
    val primaryMetricKey: String,
    val secondaryMetricKeys: List<String> = emptyList()
)

@Serializable
data class DifficultyByTrial(
    val trial: Int,
    val level: String // EASY/HARD
)