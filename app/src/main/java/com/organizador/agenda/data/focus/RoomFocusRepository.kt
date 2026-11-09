package com.organizador.agenda.data.focus

import com.organizador.agenda.data.local.FocusDao
import com.organizador.agenda.data.local.FocusSessionEntity
import com.organizador.agenda.data.local.toDomain
import com.organizador.agenda.domain.model.FocusKind
import com.organizador.agenda.domain.model.FocusSession
import com.organizador.agenda.domain.repository.FocusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomFocusRepository @Inject constructor(
    private val dao: FocusDao,
    private val clock: Clock,
) : FocusRepository {

    override suspend fun start(taskId: Long?, kind: FocusKind, plannedMinutes: Int): Long =
        dao.insert(
            FocusSessionEntity(taskId = taskId, kind = kind.name, plannedMinutes = plannedMinutes, startedAt = Instant.now(clock)),
        )

    override suspend fun finish(sessionId: Long, completed: Boolean) =
        dao.finish(sessionId, Instant.now(clock), completed)

    override fun observeForTask(taskId: Long): Flow<List<FocusSession>> =
        dao.observeForTask(taskId).map { list -> list.map { it.toDomain() } }

    override fun observeBetween(from: Instant, to: Instant): Flow<List<FocusSession>> =
        dao.observeBetween(from, to).map { list -> list.map { it.toDomain() } }
}
