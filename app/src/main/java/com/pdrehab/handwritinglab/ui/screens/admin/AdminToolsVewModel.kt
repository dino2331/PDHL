package com.pdrehab.handwritinglab.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdrehab.handwritinglab.data.db.dao.AdminSessionRow
import com.pdrehab.handwritinglab.data.db.dao.SessionDao
import com.pdrehab.handwritinglab.data.storage.AdminSession
import com.pdrehab.handwritinglab.domain.usecase.ExportSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class AdminToolsUi(
    val isAdmin: Boolean = false,
    val sessions: List<AdminSessionRow> = emptyList(),
    val selectedSessionId: String? = null,
    val exporting: Boolean = false,
    val message: String? = null,
    val exportedZip: File? = null
)

@HiltViewModel
class AdminToolsViewModel @Inject constructor(
    private val admin: AdminSession,
    private val sessionDao: SessionDao,
    private val export: ExportSessionUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(AdminToolsUi())
    val ui: StateFlow<AdminToolsUi> = _ui

    fun load() {
        viewModelScope.launch {
            val isAdmin = admin.state.value.isAdmin
            val list = sessionDao.getRecentSessions(30)
            _ui.value = _ui.value.copy(isAdmin = isAdmin, sessions = list)
        }
    }

    fun select(sessionId: String) {
        _ui.value = _ui.value.copy(selectedSessionId = sessionId, message = null, exportedZip = null)
    }

    fun runExport() {
        val sid = _ui.value.selectedSessionId ?: return
        if (_ui.value.exporting) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(exporting = true, message = null, exportedZip = null)
            runCatching { export.export(sid) }
                .onSuccess { zip ->
                    _ui.value = _ui.value.copy(exporting = false, exportedZip = zip, message = "완료: ${zip.name}")
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(exporting = false, message = "실패: ${e.message ?: e.javaClass.simpleName}")
                }
        }
    }
}