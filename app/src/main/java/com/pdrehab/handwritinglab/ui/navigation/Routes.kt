package com.pdrehab.handwritinglab.ui.navigation

object Routes {
    const val Splash = "splash"
    const val ParticipantEntry = "participant/entry"
    const val ParticipantHome = "participant/home/{participantCode}"

    const val SessionNew = "session/new"
    const val CalibrationPressure = "calibration/pressure"
    const val CalibrationSize = "calibration/size"

    const val TaskRun = "task/run/{taskInstanceId}"
    const val TaskResult = "task/result/{sessionId}/{taskId}"
    const val SessionSummary = "session/summary/{sessionId}"

    const val HistoryList = "participant/history/{participantCode}"
    const val Trend = "participant/history/trend/{participantCode}"

    const val AdminLogin = "admin/login"
    const val AdminTools = "admin/tools"

    fun home(participantCode: String) = "participant/home/$participantCode"
    fun run(taskInstanceId: String) = "task/run/$taskInstanceId"
    fun result(sessionId: String, taskId: String) = "task/result/$sessionId/$taskId"
    fun summary(sessionId: String) = "session/summary/$sessionId"
    fun history(participantCode: String) = "participant/history/$participantCode"
    fun trend(participantCode: String) = "participant/history/trend/$participantCode"
}