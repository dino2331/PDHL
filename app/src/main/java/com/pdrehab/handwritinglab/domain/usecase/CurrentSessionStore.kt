package com.pdrehab.handwritinglab.domain.usecase

import com.pdrehab.handwritinglab.data.db.entity.ParticipantEntity
import com.pdrehab.handwritinglab.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentSessionStore @Inject constructor() {
    private val _participant = MutableStateFlow<ParticipantEntity?>(null)
    val participant: StateFlow<ParticipantEntity?> = _participant

    private val _session = MutableStateFlow<SessionEntity?>(null)
    val session: StateFlow<SessionEntity?> = _session

    fun setParticipant(p: ParticipantEntity) { _participant.value = p }
    fun setSession(s: SessionEntity) { _session.value = s }

    fun clearSession() { _session.value = null }
}