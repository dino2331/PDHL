package com.pdrehab.handwritinglab.di

import android.content.Context
import androidx.room.Room
import com.pdrehab.handwritinglab.core.PdhlPaths
import com.pdrehab.handwritinglab.data.db.AppDatabase
import com.pdrehab.handwritinglab.data.db.Migrations
import com.pdrehab.handwritinglab.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext context: Context, paths: PdhlPaths): AppDatabase {
        paths.ensureBaseDirs()
        val dbFile = paths.dbFile()
        dbFile.parentFile?.mkdirs()

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            dbFile.absolutePath
        )
            .addMigrations(*Migrations.ALL)
            .build()
    }

    @Provides fun participantDao(db: AppDatabase): ParticipantDao = db.participantDao()
    @Provides fun sessionDao(db: AppDatabase): SessionDao = db.sessionDao()
    @Provides fun taskInstanceDao(db: AppDatabase): TaskInstanceDao = db.taskInstanceDao()
    @Provides fun metricValueDao(db: AppDatabase): MetricValueDao = db.metricValueDao()
    @Provides fun exportDao(db: AppDatabase): ExportDao = db.exportDao()
}