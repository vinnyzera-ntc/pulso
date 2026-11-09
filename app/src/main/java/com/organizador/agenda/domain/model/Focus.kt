package com.organizador.agenda.domain.model

import java.time.Instant

enum class FocusKind { Focus, ShortBreak, LongBreak }

data class FocusSession(
    val id: Long = 0,
    val taskId: Long?,
    val kind: FocusKind,
    val plannedMinutes: Int,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    /** true quando o timer chegou ao fim; false se a sessão foi interrompida. */
    val completed: Boolean = false,
)
