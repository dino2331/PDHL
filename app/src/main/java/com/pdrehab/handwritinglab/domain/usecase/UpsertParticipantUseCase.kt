package com.pdrehab.handwritinglab.domain.usecase

import com.pdrehab.handwritinglab.core.Ids
import com.pdrehab.handwritinglab.data.db.dao.ParticipantDao
import com.pdrehab.handwritinglab.data.db.entity.ParticipantEntity
import com.pdrehab.handwritinglab.data.repo.AddressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class UpsertParticipantUseCase @Inject constructor(
    private val participantDao: ParticipantDao,
    private val addressRepo: AddressRepository
) {
    suspend fun upsertByCode(rawInput: String): ParticipantEntity = withContext(Dispatchers.IO) {
        val code = Ids.normalizeParticipantCode(rawInput)
        require(Ids.isValidParticipantCode(code)) { "participantCode must be 8 alnum" }

        val existing = participantDao.getByCode(code)
        if (existing != null) return@withContext existing

        val addr = addressRepo.assignByParticipantCode(code)
        val p = ParticipantEntity(
            participantId = UUID.randomUUID().toString(),
            participantCode = code,
            assignedAddressId = addr.id,
            assignedAddressText = addr.text,
            createdAtMs = System.currentTimeMillis()
        )
        participantDao.upsert(p)
        p
    }
}