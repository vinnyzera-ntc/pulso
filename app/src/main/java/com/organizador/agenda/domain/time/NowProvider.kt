package com.organizador.agenda.domain.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Fonte do "agora" para as telas. Os testes usam uma implementação com tempo fixo. */
interface NowProvider {
    val zone: ZoneId
    fun now(): LocalDateTime

    /** Emite o momento atual ao assinar e a cada virada de minuto. */
    val minuteTicks: Flow<LocalDateTime>
}

@Singleton
class ClockNowProvider @Inject constructor(private val clock: Clock) : NowProvider {
    override val zone: ZoneId get() = clock.zone
    override fun now(): LocalDateTime = LocalDateTime.now(clock)

    override val minuteTicks: Flow<LocalDateTime> = flow {
        while (true) {
            val now = now()
            emit(now)
            // Dorme até o próximo minuto cheio (+ folga) para os textos "em 25 min" virarem na hora.
            delay((60 - now.second) * 1000L - now.nano / 1_000_000 + 50)
        }
    }
}
