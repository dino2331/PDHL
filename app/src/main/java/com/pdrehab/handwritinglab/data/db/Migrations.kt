package com.pdrehab.handwritinglab.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    // 1 -> 2: participants/task_instances 컬럼 추가(최소)
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // participants: assignedAddressId/text + notes
            db.execSQL("ALTER TABLE participants ADD COLUMN assignedAddressId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE participants ADD COLUMN assignedAddressText TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE participants ADD COLUMN notes TEXT")

            // task_instances: paging fields
            db.execSQL("ALTER TABLE task_instances ADD COLUMN countNextScreen INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE task_instances ADD COLUMN pageCount INTEGER NOT NULL DEFAULT 1")
        }
    }

    // 2 -> 3: metric_values 테이블 생성 + 인덱스
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS metric_values (
                    metricId TEXT NOT NULL PRIMARY KEY,
                    participantId TEXT NOT NULL,
                    participantCode TEXT,
                    sessionId TEXT NOT NULL,
                    taskId TEXT,
                    trialIndex INTEGER,
                    metricKey TEXT NOT NULL,
                    value REAL,
                    unit TEXT NOT NULL,
                    direction TEXT NOT NULL,
                    createdAtMs INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE UNIQUE INDEX IF NOT EXISTS index_metric_values_unique
                ON metric_values(sessionId, taskId, trialIndex, metricKey)
            """.trimIndent())

            db.execSQL("CREATE INDEX IF NOT EXISTS index_metric_values_task_key_trial ON metric_values(taskId, metricKey, trialIndex)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_metric_values_participant_task_key_trial ON metric_values(participantId, taskId, metricKey, trialIndex)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_metric_values_sessionId ON metric_values(sessionId)")
        }
    }

    // 3 -> 4: participantCode unique 강화(인덱스 보장)
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_participants_participantCode ON participants(participantCode)")
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}