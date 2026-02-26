package com.pdrehab.handwritinglab.domain.usecase

import com.pdrehab.handwritinglab.core.PdhlPaths
import com.pdrehab.handwritinglab.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class InitializeAppUseCase @Inject constructor(
    private val paths: PdhlPaths,
    private val db: AppDatabase,
    private val backfill: PostBackfillUseCase
) {
    suspend fun initAppBestEffort() = withContext(Dispatchers.IO) {
        // 디렉토리 생성
        paths.rootDir().mkdirs()
        paths.dbFile().parentFile?.mkdirs()
        paths.sessionsDir().mkdirs()
        paths.exportsDir().mkdirs()

        // DB open (migration 수행)
        db.openHelper.writableDatabase

        // backfill (best-effort)
        backfill.runOnceBestEffort()
    }
}