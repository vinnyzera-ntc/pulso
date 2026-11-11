package com.organizador.agenda.data.calendar

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import android.provider.CalendarContract.Reminders
import androidx.core.content.ContextCompat
import com.organizador.agenda.domain.model.Attendee
import com.organizador.agenda.domain.model.CalendarEvent
import com.organizador.agenda.domain.model.DeviceCalendar
import com.organizador.agenda.domain.model.EventDetails
import com.organizador.agenda.domain.model.EventDraft
import com.organizador.agenda.domain.repository.CalendarRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agendas e compromissos via Android Calendar Provider (CalendarContract), sem OAuth:
 * lê e escreve nas agendas das contas já configuradas no aparelho; a sincronização
 * com o Google é feita pelo próprio sistema.
 */
@Singleton
class CalendarProviderRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val pending: PendingEventChanges,
) : CalendarRepository {

    private val resolver: ContentResolver get() = context.contentResolver
    private val refreshTick = MutableStateFlow(0)

    override fun hasPermission(): Boolean =
        listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR).all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun refresh() {
        refreshTick.value++
    }

    /**
     * Emite ao assinar, a cada mudança no provider e a cada [refresh]. O refresh recria o
     * observador, então uma permissão concedida depois da assinatura passa a ser vista.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun changes(): Flow<Unit> = refreshTick.flatMapLatest {
        callbackFlow {
            trySend(Unit)
            if (!hasPermission()) {
                awaitClose { }
                return@callbackFlow
            }
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
            resolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
            awaitClose { resolver.unregisterContentObserver(observer) }
        }.conflate()
    }

    override fun observeCalendars(): Flow<List<DeviceCalendar>> =
        changes().map { queryCalendars() }.flowOn(Dispatchers.IO)

    override fun observeEvents(from: Instant, to: Instant, calendarIds: Set<Long>): Flow<List<CalendarEvent>> =
        changes().map { queryInstances(from, to, calendarIds) }.flowOn(Dispatchers.IO)
            // Compromissos cancelados cujo "Desfazer" ainda não expirou não aparecem.
            .combine(pending.hidden) { list, hidden -> list.filterNot { hidden.hides(it.eventId, it.instanceId) } }

    private fun queryCalendars(): List<DeviceCalendar> {
        if (!hasPermission()) return emptyList()
        val projection = arrayOf(
            Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE,
            Calendars.CALENDAR_COLOR, Calendars.IS_PRIMARY, Calendars.VISIBLE, Calendars.CALENDAR_ACCESS_LEVEL,
            Calendars.OWNER_ACCOUNT,
        )
        return resolver.query(Calendars.CONTENT_URI, projection, "${Calendars.SYNC_EVENTS} = 1", null, null)
            ?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val access = c.getInt(7)
                        val account = c.getString(2).orEmpty()
                        add(
                            DeviceCalendar(
                                id = c.getLong(0),
                                displayName = c.getString(1).orEmpty(),
                                accountName = account,
                                accountType = c.getString(3).orEmpty(),
                                color = c.getInt(4),
                                // IS_PRIMARY pode vir nulo em provedores antigos: a agenda cujo dono é a conta é a principal.
                                isPrimary = c.getIntOrNull(5)?.let { it == 1 } ?: (c.getString(8) == account),
                                visibleInSystem = c.getInt(6) == 1,
                                canWrite = access >= Calendars.CAL_ACCESS_CONTRIBUTOR,
                            ),
                        )
                    }
                }
            }.orEmpty()
    }

    private fun queryInstances(from: Instant, to: Instant, calendarIds: Set<Long>): List<CalendarEvent> {
        if (!hasPermission() || calendarIds.isEmpty()) return emptyList()
        val uri = Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, from.toEpochMilli())
            ContentUris.appendId(it, to.toEpochMilli())
        }.build()
        val projection = arrayOf(
            Instances.EVENT_ID, Instances._ID, Instances.CALENDAR_ID, Instances.TITLE, Instances.BEGIN, Instances.END,
            Instances.ALL_DAY, Instances.EVENT_LOCATION, Instances.DISPLAY_COLOR, Instances.RRULE, Instances.RDATE,
        )
        val ids = calendarIds.joinToString(",")
        val selection = "${Instances.CALENDAR_ID} IN ($ids) AND " +
            "(${Instances.STATUS} IS NULL OR ${Instances.STATUS} != ${Events.STATUS_CANCELED}) AND " +
            "(${Instances.SELF_ATTENDEE_STATUS} IS NULL OR ${Instances.SELF_ATTENDEE_STATUS} != ${Attendees.ATTENDEE_STATUS_DECLINED})"
        return resolver.query(uri, projection, selection, null, "${Instances.BEGIN} ASC")?.use { c ->
            buildList {
                while (c.moveToNext()) add(c.toEvent())
            }
        }.orEmpty()
    }

    private fun Cursor.toEvent() = CalendarEvent(
        eventId = getLong(0),
        instanceId = getLong(1),
        calendarId = getLong(2),
        title = getString(3).orEmpty(),
        start = Instant.ofEpochMilli(getLong(4)),
        end = Instant.ofEpochMilli(getLong(5)),
        allDay = getInt(6) == 1,
        location = getString(7)?.takeIf { it.isNotBlank() },
        color = getInt(8),
        isRecurring = !getString(9).isNullOrBlank() || !getString(10).isNullOrBlank(),
    )

    override suspend fun getEvent(eventId: Long): EventDetails? = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext null
        val projection = arrayOf(
            Events._ID, Events.CALENDAR_ID, Events.TITLE, Events.DTSTART, Events.DTEND, Events.ALL_DAY,
            Events.EVENT_LOCATION, Events.DISPLAY_COLOR, Events.RRULE, Events.DESCRIPTION, Events.EVENT_TIMEZONE,
            Events.DURATION,
        )
        val uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId)
        resolver.query(uri, projection, null, null, null)?.use { c ->
            if (!c.moveToFirst()) return@use null
            val start = c.getLong(3)
            // Eventos recorrentes têm DURATION ("P3600S") em vez de DTEND.
            val end = c.getLongOrNull(4) ?: (start + parseDurationMillis(c.getString(11)))
            val rrule = c.getString(8)?.takeIf { it.isNotBlank() }
            EventDetails(
                event = CalendarEvent(
                    eventId = c.getLong(0), instanceId = 0, calendarId = c.getLong(1), title = c.getString(2).orEmpty(),
                    start = Instant.ofEpochMilli(start), end = Instant.ofEpochMilli(end), allDay = c.getInt(5) == 1,
                    location = c.getString(6)?.takeIf { it.isNotBlank() }, color = c.getInt(7), isRecurring = rrule != null,
                ),
                description = c.getString(9).orEmpty(),
                timeZone = c.getString(10) ?: "UTC",
                attendees = queryAttendees(eventId),
                reminderMinutes = queryReminders(eventId),
                rrule = rrule,
            )
        }
    }

    private fun queryAttendees(eventId: Long): List<Attendee> =
        resolver.query(
            Attendees.CONTENT_URI,
            arrayOf(Attendees.ATTENDEE_NAME, Attendees.ATTENDEE_EMAIL, Attendees.ATTENDEE_RELATIONSHIP),
            "${Attendees.EVENT_ID} = ?",
            arrayOf(eventId.toString()),
            null,
        )?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    // O organizador (o próprio usuário) também aparece como "attendee"; não é participante.
                    if (c.getInt(2) == Attendees.RELATIONSHIP_ORGANIZER) continue
                    val email = c.getString(1).orEmpty()
                    add(Attendee(name = c.getString(0)?.takeIf { it.isNotBlank() } ?: email, email = email))
                }
            }
        }.orEmpty()

    private fun queryReminders(eventId: Long): List<Int> =
        resolver.query(
            Reminders.CONTENT_URI,
            arrayOf(Reminders.MINUTES),
            "${Reminders.EVENT_ID} = ? AND ${Reminders.METHOD} IN (${Reminders.METHOD_ALERT}, ${Reminders.METHOD_DEFAULT})",
            arrayOf(eventId.toString()),
            null,
        )?.use { c -> buildList { while (c.moveToNext()) add(c.getInt(0)) } }.orEmpty().sorted()

    override suspend fun createEvent(draft: EventDraft): Long = withContext(Dispatchers.IO) {
        val uri = resolver.insert(Events.CONTENT_URI, draft.toValues())
            ?: error("O Calendar Provider recusou o novo evento")
        val eventId = ContentUris.parseId(uri)
        writeReminders(eventId, draft.reminderMinutes)
        draft.attendees.forEach { insertAttendee(eventId, it) }
        eventId
    }

    override suspend fun updateEvent(eventId: Long, draft: EventDraft): Unit = withContext(Dispatchers.IO) {
        resolver.update(ContentUris.withAppendedId(Events.CONTENT_URI, eventId), draft.toValues(), null, null)
        syncChildren(eventId, draft)
    }

    /** Troca os lembretes e ajusta os participantes do evento para os do rascunho. */
    private fun syncChildren(eventId: Long, draft: EventDraft) {
        resolver.delete(Reminders.CONTENT_URI, "${Reminders.EVENT_ID} = ?", arrayOf(eventId.toString()))
        writeReminders(eventId, draft.reminderMinutes)
        // Participantes: só mexe na diferença, para não reenviar convite a quem já estava.
        val current = queryAttendees(eventId).map { it.email.lowercase() }.toSet()
        val wanted = draft.attendees.associateBy { it.email.lowercase() }
        (current - wanted.keys).forEach { email ->
            resolver.delete(
                Attendees.CONTENT_URI,
                "${Attendees.EVENT_ID} = ? AND LOWER(${Attendees.ATTENDEE_EMAIL}) = ?",
                arrayOf(eventId.toString(), email),
            )
        }
        (wanted.keys - current).forEach { insertAttendee(eventId, wanted.getValue(it)) }
    }

    override suspend fun deleteEvent(eventId: Long): Unit = withContext(Dispatchers.IO) {
        resolver.delete(ContentUris.withAppendedId(Events.CONTENT_URI, eventId), null, null)
    }

    /*
     * Eventos recorrentes. "Só este" cria uma exceção (CONTENT_EXCEPTION_URI) para a ocorrência
     * que começa em instanceStart; "toda a série" altera a linha mestre, que guarda DTSTART da
     * primeira ocorrência + DURATION (nunca DTEND).
     */

    override suspend fun cancelOccurrence(eventId: Long, instanceStart: Instant): Unit = withContext(Dispatchers.IO) {
        resolver.insert(
            ContentUris.withAppendedId(Events.CONTENT_EXCEPTION_URI, eventId),
            ContentValues().apply {
                put(Events.ORIGINAL_INSTANCE_TIME, instanceStart.toEpochMilli())
                put(Events.STATUS, Events.STATUS_CANCELED)
            },
        )
    }

    override suspend fun updateOccurrence(eventId: Long, instanceStart: Instant, draft: EventDraft): Long =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(Events.ORIGINAL_INSTANCE_TIME, instanceStart.toEpochMilli())
                // Sem isto o provider marca a exceção como "provisória".
                put(Events.STATUS, Events.STATUS_CONFIRMED)
                put(Events.TITLE, draft.title.trim())
                put(Events.DESCRIPTION, draft.description)
                put(Events.EVENT_LOCATION, draft.location.trim())
                put(Events.EVENT_TIMEZONE, draft.timeZone.id)
                put(Events.DTSTART, draft.start.toEpochMilli())
                // O provider não aceita DTEND numa exceção: ele calcula o fim por DTSTART + DURATION.
                put(Events.DURATION, "P${(draft.end.epochSecond - draft.start.epochSecond).coerceAtLeast(0)}S")
            }
            val uri = resolver.insert(ContentUris.withAppendedId(Events.CONTENT_EXCEPTION_URI, eventId), values)
                ?: error("O Calendar Provider recusou a alteração da ocorrência")
            // O provider copia lembretes e participantes da série para a exceção; ajusta para o rascunho.
            ContentUris.parseId(uri).also { syncChildren(it, draft) }
        }

    override suspend fun updateSeries(eventId: Long, instanceStart: Instant, draft: EventDraft): Unit =
        withContext(Dispatchers.IO) {
            val uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId)
            val master = resolver.query(uri, arrayOf(Events.DTSTART, Events.RRULE, Events.RDATE), null, null, null)?.use { c ->
                if (c.moveToFirst()) Triple(c.getLong(0), c.getString(1), c.getString(2)) else null
            } ?: return@withContext
            // A série inteira anda o mesmo tanto que a ocorrência editada andou.
            val shift = draft.start.toEpochMilli() - instanceStart.toEpochMilli()
            val seconds = (draft.end.epochSecond - draft.start.epochSecond).coerceAtLeast(0)
            val values = ContentValues().apply {
                put(Events.TITLE, draft.title.trim())
                put(Events.DESCRIPTION, draft.description)
                put(Events.EVENT_LOCATION, draft.location.trim())
                put(Events.HAS_ALARM, if (draft.reminderMinutes.isEmpty()) 0 else 1)
                put(Events.DTSTART, master.first + shift)
                put(Events.DURATION, "P${seconds}S")
                putNull(Events.DTEND)
                // Reenvia a regra junto: o provider só recalcula as ocorrências (e a última data)
                // quando a atualização traz a recorrência completa.
                master.second?.let { put(Events.RRULE, it) }
                master.third?.let { put(Events.RDATE, it) }
            }
            resolver.update(uri, values, null, null)
            syncChildren(eventId, draft)
        }

    private fun writeReminders(eventId: Long, minutes: List<Int>) {
        minutes.distinct().forEach { m ->
            resolver.insert(
                Reminders.CONTENT_URI,
                ContentValues().apply {
                    put(Reminders.EVENT_ID, eventId)
                    put(Reminders.MINUTES, m)
                    put(Reminders.METHOD, Reminders.METHOD_ALERT)
                },
            )
        }
    }

    private fun insertAttendee(eventId: Long, a: Attendee) {
        resolver.insert(
            Attendees.CONTENT_URI,
            ContentValues().apply {
                put(Attendees.EVENT_ID, eventId)
                put(Attendees.ATTENDEE_NAME, a.name)
                put(Attendees.ATTENDEE_EMAIL, a.email)
                put(Attendees.ATTENDEE_RELATIONSHIP, Attendees.RELATIONSHIP_ATTENDEE)
                put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_REQUIRED)
                put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_INVITED)
            },
        )
    }

    private fun EventDraft.toValues() = ContentValues().apply {
        put(Events.CALENDAR_ID, calendarId)
        put(Events.TITLE, title.trim())
        put(Events.DESCRIPTION, description)
        put(Events.EVENT_LOCATION, location.trim())
        put(Events.HAS_ALARM, if (reminderMinutes.isEmpty()) 0 else 1)
        if (allDay) {
            // Dia inteiro: o provider exige meia-noite UTC e fuso UTC.
            val startDay = start.atZone(timeZone).toLocalDate()
            val endDay = end.atZone(timeZone).toLocalDate().let { if (it <= startDay) startDay.plusDays(1) else it }
            put(Events.ALL_DAY, 1)
            put(Events.EVENT_TIMEZONE, "UTC")
            put(Events.DTSTART, startDay.utcMillis())
            put(Events.DTEND, endDay.utcMillis())
        } else {
            put(Events.ALL_DAY, 0)
            put(Events.EVENT_TIMEZONE, timeZone.id)
            put(Events.DTSTART, start.toEpochMilli())
            put(Events.DTEND, end.toEpochMilli())
        }
    }

    private fun LocalDate.utcMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun Cursor.getIntOrNull(i: Int): Int? = if (isNull(i)) null else getInt(i)
    private fun Cursor.getLongOrNull(i: Int): Long? = if (isNull(i)) null else getLong(i)

    companion object {
        /** RFC 2445: "P3600S", "PT1H", "P1D", "P1W". */
        internal fun parseDurationMillis(raw: String?): Long {
            if (raw.isNullOrBlank()) return 0
            val m = Regex("""^[+-]?P(?:(\d+)W)?(?:(\d+)D)?(?:T?(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?$""").find(raw.trim())
                ?: return 0
            val (w, d, h, min, s) = m.destructured
            fun n(x: String) = x.toLongOrNull() ?: 0
            return ((((n(w) * 7 + n(d)) * 24 + n(h)) * 60 + n(min)) * 60 + n(s)) * 1000
        }
    }
}
