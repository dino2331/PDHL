package com.pdrehab.handwritinglab

import android.content.Context
import androidx.room.Room
import com.pdrehab.handwritinglab.data.db.AppDatabase
import com.pdrehab.handwritinglab.data.db.entity.MetricValueEntity
import com.pdrehab.handwritinglab.data.db.entity.ParticipantEntity
import com.pdrehab.handwritinglab.data.db.entity.SessionEntity
import com.pdrehab.handwritinglab.feature.analysis.histogram.computeHist
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.collections.plusAssign

@RunWith(AndroidJUnit4::class)
class DistributionSmokeTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun latestSessionPerParticipant_distributionQuery_works() = runBlocking {
        val pDao = db.participantDao()
        val sDao = db.sessionDao()
        val mDao = db.metricValueDao()

        val now = System.currentTimeMillis()

        // self
        val selfP = ParticipantEntity(
            participantId = UUID.randomUUID().toString(),
            participantCode = "AB12CD34",
            assignedAddressId = "A001",
            assignedAddressText = "경기도수원시산업로101",
            createdAtMs = now
        )
        pDao.upsert(selfP)

        val selfS = SessionEntity(
            sessionId = UUID.randomUUID().toString(),
            participantId = selfP.participantId,
            randomSeed = 1L,
            createdAtMs = now,
            endedAtMs = now + 1000,
            assignedAddressText = selfP.assignedAddressText
        )
        sDao.upsert(selfS)

        // self aggregate metric (trialIndex=0)
        mDao.upsertAll(
            listOf(
                MetricValueEntity(
                    metricId = UUID.randomUUID().toString(),
                    participantId = selfP.participantId,
                    participantCode = selfP.participantCode,
                    sessionId = selfS.sessionId,
                    taskId = "T01",
                    trialIndex = 0,
                    metricKey = "SIZE_REDUCTION_PCT",
                    value = 10.0,
                    unit = "%",
                    direction = "LOWER_BETTER",
                    createdAtMs = now + 10
                )
            )
        )

        // other participants: 각자 2개 세션(최신만 뽑히는지 검증)
        val others = 25
        val rows = ArrayList<MetricValueEntity>()
        for (i in 1..others) {
            val code = "AA" + i.toString().padStart(6, '0') // AA000001.. (8자)
            val pid = UUID.randomUUID().toString()
            pDao.upsert(
                ParticipantEntity(
                    participantId = pid,
                    participantCode = code,
                    assignedAddressId = "A001",
                    assignedAddressText = "경기도수원시산업로101",
                    createdAtMs = now - i
                )
            )

            val oldSession = SessionEntity(
                sessionId = UUID.randomUUID().toString(),
                participantId = pid,
                randomSeed = i.toLong(),
                createdAtMs = now - 10_000 - i,
                endedAtMs = now - 9_000 - i,
                assignedAddressText = "경기도수원시산업로101"
            )
            val newSession = SessionEntity(
                sessionId = UUID.randomUUID().toString(),
                participantId = pid,
                randomSeed = (i + 100).toLong(),
                createdAtMs = now - 1_000 - i,
                endedAtMs = now - 900 - i,
                assignedAddressText = "경기도수원시산업로101"
            )
            sDao.upsert(oldSession)
            sDao.upsert(newSession)

            // old 값은 1000+i (뽑히면 안 됨), new 값은 i (뽑혀야 함)
            rows plusAssign MetricValueEntity(
                metricId = UUID.randomUUID().toString(),
                participantId = pid,
                participantCode = code,
                sessionId = oldSession.sessionId,
                taskId = "T01",
                trialIndex = 0,
                metricKey = "SIZE_REDUCTION_PCT",
                value = 1000.0 + i,
                unit = "%",
                direction = "LOWER_BETTER",
                createdAtMs = oldSession.createdAtMs + 1
            )
            rows plusAssign MetricValueEntity(
                metricId = UUID.randomUUID().toString(),
                participantId = pid,
                participantCode = code,
                sessionId = newSession.sessionId,
                taskId = "T01",
                trialIndex = 0,
                metricKey = "SIZE_REDUCTION_PCT",
                value = i.toDouble(),
                unit = "%",
                direction = "LOWER_BETTER",
                createdAtMs = newSession.createdAtMs + 1
            )
        }
        mDao.upsertAll(rows)

        val n = mDao.getDistributionCountLatestSessionPerParticipant(
            selfParticipantId = selfP.participantId,
            taskId = "T01",
            metricKey = "SIZE_REDUCTION_PCT"
        )
        assertEquals("n은 other participants 수와 같아야 함", others, n)

        val dist = mDao.getDistributionValuesLatestSessionPerParticipant(
            selfParticipantId = selfP.participantId,
            taskId = "T01",
            metricKey = "SIZE_REDUCTION_PCT"
        )
        assertEquals(others, dist.size)

        // 최신 세션 값(i)만 존재해야 함 → 1000대 값이 있으면 실패
        assertTrue(dist.all { it < 100.0 })

        // 히스토그램 계산 가능해야 함
        val hist = computeHist(dist, myValue = 10.0, bins = 10)
        assertTrue(hist.counts.sum() >= others)
        assertTrue(hist.min <= hist.myClamped && hist.myClamped <= hist.max)
    }
}