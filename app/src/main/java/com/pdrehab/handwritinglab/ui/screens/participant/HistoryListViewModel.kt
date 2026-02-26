package com.pdrehab.handwritinglab.ui.screens.participant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdrehab.handwritinglab.data.db.dao.ParticipantDao
import com.pdrehab.handwritinglab.data.db.dao.SessionDao
import com.pdrehab.handwritinglab.data.db.entity.SessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryListViewModel @Inject constructor(
    private val participantDao: ParticipantDao,
    private val sessionDao: SessionDao
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val sessions: StateFlow<List<SessionEntity>> = _sessions

    fun load(participantCode: String) {
        viewModelScope.launch {
            val p = participantDao.getByCode(participantCode) ?: return@launch
            _sessions.value = sessionDao.listByParticipant(p.participantId)
        }
    }
}