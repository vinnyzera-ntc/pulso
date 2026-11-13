package com.organizador.agenda.ui.agenda

import com.organizador.agenda.R
import com.organizador.agenda.data.calendar.PendingEventChanges
import com.organizador.agenda.domain.calendar.EventForm
import com.organizador.agenda.domain.model.EventDraft
import com.organizador.agenda.domain.repository.CalendarRepository
import com.organizador.agenda.domain.time.NowProvider
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.common.Messenger
import com.organizador.agenda.ui.common.UiMessage
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** A que a alteração se aplica. [Single] = evento que não se repete. */
enum class EditScope { Single, Occurrence, Series }

/** Uma ocorrência de evento que o usuário quer editar, remarcar ou cancelar. */
data class EventTarget(
    val eventId: Long,
    val instanceId: Long,
    val instanceStart: Instant,
    val instanceEnd: Instant,
    val scope: EditScope,
)

/** Operações de escrita em compromissos, compartilhadas pela Agenda e pela folha de edição. */
@Singleton
class EventEditor @Inject constructor(
    private val calendar: CalendarRepository,
    private val pending: PendingEventChanges,
    private val messenger: Messenger,
    private val now: NowProvider,
) {
    /** Cria (target nulo) ou grava a edição conforme o alcance escolhido. false = falhou (já avisado). */
    suspend fun save(target: EventTarget?, draft: EventDraft): Boolean = messenger.guard(R.string.msg_calendar_write_failed) {
        when (target?.scope) {
            null -> calendar.createEvent(draft)
            EditScope.Single -> calendar.updateEvent(target.eventId, draft)
            EditScope.Occurrence -> calendar.updateOccurrence(target.eventId, target.instanceStart, draft)
            EditScope.Series -> calendar.updateSeries(target.eventId, target.instanceStart, draft)
        }
        true
    } ?: false

    /** Remarca mantendo a duração e todo o resto. Evento avulso ganha "Desfazer". */
    suspend fun move(target: EventTarget, newStart: Instant) {
        // null sem exceção = sem permissão ou o compromisso sumiu: avisa do mesmo jeito.
        val details = messenger.guard(R.string.msg_calendar_write_failed) {
            calendar.getEvent(target.eventId) ?: error("Compromisso indisponível")
        } ?: return
        val original = EventForm.edit(details, target.instanceStart, target.instanceEnd, now.zone).toDraft(now.zone)
        val duration = Duration.between(target.instanceStart, target.instanceEnd)
        val moved = original.copy(start = newStart, end = newStart.plus(duration))
        if (!save(target, moved)) return
        val label = PtBr.time(LocalDateTime.ofInstant(newStart, now.zone).toLocalTime())
        messenger.show(
            if (target.scope == EditScope.Single) {
                UiMessage(R.string.msg_event_moved, R.string.action_undo, args = listOf(label)) {
                    calendar.updateEvent(target.eventId, original)
                }
            } else {
                UiMessage(R.string.msg_event_moved, args = listOf(label))
            },
        )
    }

    /**
     * Cancela: some da tela na hora, mas só é apagado quando o aviso expira. "Desfazer" antes
     * disso apenas volta a mostrar, sem nenhuma escrita na agenda.
     */
    fun cancel(target: EventTarget) {
        val wholeEvent = target.scope != EditScope.Occurrence
        if (wholeEvent) pending.hideEvent(target.eventId) else pending.hideInstance(target.instanceId)
        messenger.show(
            UiMessage(
                R.string.msg_event_cancelled,
                R.string.action_undo,
                onDismissed = {
                    val done = messenger.guard(R.string.msg_calendar_write_failed) {
                        if (wholeEvent) calendar.deleteEvent(target.eventId)
                        else calendar.cancelOccurrence(target.eventId, target.instanceStart)
                    }
                    // Não apagou: o compromisso volta a aparecer.
                    if (done == null) {
                        if (wholeEvent) pending.showEvent(target.eventId) else pending.showInstance(target.instanceId)
                    }
                },
            ) {
                if (wholeEvent) pending.showEvent(target.eventId) else pending.showInstance(target.instanceId)
            },
        )
    }
}
