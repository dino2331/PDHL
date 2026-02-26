package com.pdrehab.handwritinglab.ui.screens.participant

import androidx.lifecycle.ViewModel
import com.pdrehab.handwritinglab.domain.usecase.CurrentSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ParticipantHomeViewModel @Inject constructor(
    val store: CurrentSessionStore
) : ViewModel() {
    val participant = store.participant
}