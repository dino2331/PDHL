package com.pdrehab.handwritinglab.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdrehab.handwritinglab.domain.usecase.InitializeAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashUi(
    val status: String = "초기화 중...",
    val ready: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val init: InitializeAppUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(SplashUi())
    val ui: StateFlow<SplashUi> = _ui

    fun startInit() {
        if (_ui.value.ready) return
        viewModelScope.launch {
            try {
                _ui.value = SplashUi(status = "DB 준비 중...", ready = false)
                init.initAppBestEffort()
                _ui.value = SplashUi(status = "준비 완료", ready = true)
            } catch (t: Throwable) {
                _ui.value = SplashUi(status = "오류", ready = true, error = t.message ?: t.javaClass.simpleName)
            }
        }
    }
}