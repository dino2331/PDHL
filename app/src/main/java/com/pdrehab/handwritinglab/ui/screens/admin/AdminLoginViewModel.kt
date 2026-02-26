package com.pdrehab.handwritinglab.ui.screens.admin

import androidx.lifecycle.ViewModel
import com.pdrehab.handwritinglab.data.storage.AdminSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class AdminLoginUi(
    val pin: String = "",
    val message: String? = null,
    val lockedRemainingMs: Long = 0L
)

@HiltViewModel
class AdminLoginViewModel @Inject constructor(
    private val admin: AdminSession
) : ViewModel() {

    private val _ui = MutableStateFlow(AdminLoginUi())
    val ui: StateFlow<AdminLoginUi> = _ui

    fun onPinChanged(v: String) {
        _ui.value = _ui.value.copy(pin = v.filter { it.isDigit() }.take(4), message = null)
    }

    fun submit(onSuccess: () -> Unit) {
        val res = admin.tryLogin(_ui.value.pin)
        _ui.value = _ui.value.copy(
            message = res.message,
            lockedRemainingMs = res.lockedRemainingMs
        )
        if (res.success) onSuccess()
    }
}