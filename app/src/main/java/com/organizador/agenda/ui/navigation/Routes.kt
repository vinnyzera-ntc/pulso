package com.organizador.agenda.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.organizador.agenda.R
import kotlinx.serialization.Serializable

/* Rotas type-safe do Navigation Compose. */

@Serializable data object TodayRoute
@Serializable data object AgendaRoute
@Serializable data object TasksRoute
@Serializable data object FocusRoute
@Serializable data object SettingsRoute
@Serializable data object CaptureRoute
@Serializable data class TaskDetailRoute(val id: Long)

/** Pedido de abrir uma tarefa vindo de fora (notificação). Classe comum: cada toque é um pedido novo. */
class OpenTaskRequest(val taskId: Long)

/** Destinos do nav pill, na ordem em que aparecem. `key` é usado pelo atalho de debug (-e tela). */
enum class TopLevel(val key: String, val route: Any, @StringRes val label: Int, val icon: ImageVector) {
    Today("hoje", TodayRoute, R.string.nav_today, Icons.Outlined.WbSunny),
    Agenda("agenda", AgendaRoute, R.string.nav_agenda, Icons.Outlined.CalendarMonth),
    Tasks("tarefas", TasksRoute, R.string.nav_tasks, Icons.AutoMirrored.Outlined.List),
    Focus("foco", FocusRoute, R.string.nav_focus, Icons.Outlined.Bolt),
}
