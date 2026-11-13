package com.organizador.agenda.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import com.organizador.agenda.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Mantém o widget em dia: redesenha quando tarefas ou compromissos mudam (com o processo vivo)
 * e arma um alarme não-despertador para o próximo início/fim de compromisso ou meia-noite.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val data: WidgetData,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private var started = false

    @OptIn(FlowPreview::class)
    fun start() {
        if (started) return
        started = true
        scope.launch {
            data.states().debounce(300).collect { state ->
                if (hasWidgets()) safely { PulsoWidget().updateAll(context); arm(state.nextRefresh) }
            }
        }
    }

    /** Redesenha agora e rearma (alarme de virada, widget recém-colocado). */
    suspend fun refresh() {
        if (!hasWidgets()) return
        safely {
            PulsoWidget().updateAll(context)
            arm(data.current().nextRefresh)
        }
    }

    private fun hasWidgets(): Boolean =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, PulsoWidgetReceiver::class.java)).isNotEmpty()

    private fun arm(at: Instant) {
        val intent = PendingIntent.getBroadcast(
            context, REQUEST_REFRESH, Intent(context, WidgetRefreshReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // RTC (não acorda o aparelho): o widget só precisa estar certo quando a tela acender.
        context.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC, at.toEpochMilli(), intent)
    }

    private suspend fun safely(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Pulso", "Falha ao atualizar o widget", e)
        }
    }

    private companion object {
        const val REQUEST_REFRESH = 400
    }
}

class PulsoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PulsoWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Widget recém-colocado ou atualização periódica: garante o alarme da próxima virada.
        context.widgetEntryPoint().widgetUpdater().start()
    }
}

/** Alarme da próxima virada (compromisso começa/termina, meia-noite). */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val entry = context.widgetEntryPoint()
        val updater = entry.widgetUpdater()
        entry.scope().launch {
            try {
                withTimeoutOrNull(8_000L) { updater.refresh() }
            } finally {
                pending.finish()
            }
        }
    }
}
