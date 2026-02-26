package com.pdrehab.handwritinglab.core

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdhlPaths @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun rootDir(): File = File(context.filesDir, "pdhl")
    fun dbDir(): File = File(rootDir(), "db")
    fun dbFile(): File = File(dbDir(), "pdhl.db")

    fun sessionsDir(): File = File(rootDir(), "sessions")
    fun sessionDir(sessionId: String): File = File(sessionsDir(), sessionId)

    fun tasksDir(sessionId: String): File = File(sessionDir(sessionId), "tasks")
    fun taskDir(sessionId: String, taskId: String): File = File(tasksDir(sessionId), taskId)
    fun trialDir(sessionId: String, taskId: String, trialIndex: Int): File =
        File(taskDir(sessionId, taskId), "trial_$trialIndex")

    fun rawGz(sessionId: String, taskId: String, trialIndex: Int): File =
        File(trialDir(sessionId, taskId, trialIndex), "raw.jsonl.gz")

    fun exportsDir(): File = File(rootDir(), "exports")

    fun ensureBaseDirs() {
        rootDir().mkdirs()
        dbDir().mkdirs()
        sessionsDir().mkdirs()
        exportsDir().mkdirs()
    }
}