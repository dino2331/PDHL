package com.pdrehab.handwritinglab.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pdrehab.handwritinglab.data.db.dao.*
import com.pdrehab.handwritinglab.data.db.entity.*

@Database(
    entities = [
        ParticipantEntity::class,
        SessionEntity::class,
        TaskInstanceEntity::class,
        MetricValueEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun participantDao(): ParticipantDao
    abstract fun sessionDao(): SessionDao
    abstract fun taskInstanceDao(): TaskInstanceDao
    abstract fun metricValueDao(): MetricValueDao
    abstract fun exportDao(): ExportDao
}