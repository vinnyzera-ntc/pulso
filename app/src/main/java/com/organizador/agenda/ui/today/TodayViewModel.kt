package com.organizador.agenda.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.organizador.agenda.domain.time.PtBr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

data class TodayHeader(val eyebrow: String, val greeting: String)

@HiltViewModel
class TodayViewModel @Inject constructor(private val clock: Clock) : ViewModel() {

    val header: StateFlow<TodayHeader> = flow {
        while (true) {
            emit(headerAt(LocalDateTime.now(clock)))
            delay(60_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), headerAt(LocalDateTime.now(clock)))

    private fun headerAt(now: LocalDateTime) = TodayHeader(
        eyebrow = PtBr.eyebrowDate(now.toLocalDate()),
        greeting = PtBr.greeting(now.toLocalTime()),
    )
}
