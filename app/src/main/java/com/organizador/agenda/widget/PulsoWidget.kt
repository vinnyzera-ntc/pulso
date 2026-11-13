package com.organizador.agenda.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.organizador.agenda.MainActivity
import com.organizador.agenda.R
import com.organizador.agenda.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetData(): WidgetData
    fun widgetUpdater(): WidgetUpdater
    @ApplicationScope fun scope(): CoroutineScope
}

fun Context.widgetEntryPoint(): WidgetEntryPoint = EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)

/**
 * Widget "Hoje": data e progresso, o compromisso de agora (ou o próximo) e as tarefas abertas
 * de hoje, com o círculo de concluir. Cores do Pulso em colors.xml (values e values-night).
 */
class PulsoWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = context.widgetEntryPoint().widgetData()
        val initial = data.current()
        provideContent {
            val state by data.states().collectAsState(initial)
            WidgetContent(state)
        }
    }
}

private object WC {
    val bg = ColorProvider(R.color.widget_bg)
    val surface = ColorProvider(R.color.widget_surface)
    val text = ColorProvider(R.color.widget_text)
    val text2 = ColorProvider(R.color.widget_text2)
    val volt = ColorProvider(R.color.widget_volt_text)
    val flare = ColorProvider(R.color.widget_flare_text)
    val onEmber = ColorProvider(R.color.widget_on_ember)
}

private const val HEADER_DP = 52
private const val EVENT_DP = 58
private const val ROW_DP = 44
private const val PADDING_DP = 14

@Composable
private fun WidgetContent(s: WidgetState) {
    val context = LocalContext.current
    val height = LocalSize.current.height.value.toInt()
    Column(
        GlanceModifier.fillMaxSize().background(WC.bg).cornerRadius(24.dp).padding(PADDING_DP.dp),
    ) {
        // Cabeçalho: data + progresso (abre o Hoje) e o "+" da Captura rápida.
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight().clickable(actionStartActivity(context.open(null)))) {
                Text(s.eyebrow.uppercase(), style = TextStyle(color = WC.text2, fontSize = 11.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                Text(
                    if (s.total > 0) context.resources.getQuantityString(R.plurals.today_progress, s.total, s.done, s.total)
                    else context.getString(R.string.widget_title),
                    style = TextStyle(color = WC.text, fontSize = 17.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
            Box(
                GlanceModifier.size(40.dp).background(ImageProvider(R.drawable.widget_fab))
                    .clickable(actionStartActivity(context.open(MainActivity.OPEN_CAPTURE)))
                    .semantics { contentDescription = context.getString(R.string.action_quick_capture) },
                contentAlignment = Alignment.Center,
            ) {
                Image(ImageProvider(R.drawable.ic_widget_add), null, GlanceModifier.size(20.dp), colorFilter = ColorFilter.tint(WC.onEmber))
            }
        }

        s.event?.let { e ->
            Spacer(GlanceModifier.height(8.dp))
            Column(
                GlanceModifier.fillMaxWidth().background(WC.surface).cornerRadius(14.dp).padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable(actionStartActivity(context.open(MainActivity.OPEN_AGENDA))),
            ) {
                Text(e.label, style = TextStyle(color = WC.volt, fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                Text(e.title, style = TextStyle(color = WC.text, fontSize = 14.sp), maxLines = 1)
            }
        }

        Spacer(GlanceModifier.height(4.dp))
        val available = height - PADDING_DP * 2 - HEADER_DP - (if (s.event != null) EVENT_DP else 0) - 4
        var rows = (available / ROW_DP).coerceAtLeast(0)
        // Se nem todas cabem, uma linha vira o "+N mais".
        if (s.tasks.size > rows && rows > 0) rows -= 1
        if (s.tasks.isEmpty()) {
            Text(
                context.getString(R.string.widget_empty),
                style = TextStyle(color = WC.text2, fontSize = 13.sp),
                modifier = GlanceModifier.padding(top = 8.dp),
            )
        } else {
            s.tasks.take(rows).forEach { TaskRow(it) }
            val more = s.tasks.size - rows
            if (more > 0) {
                Text(
                    context.getString(R.string.widget_more, more),
                    style = TextStyle(color = WC.text2, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    modifier = GlanceModifier.padding(start = 38.dp, top = 6.dp).clickable(actionStartActivity(context.open(MainActivity.OPEN_TASKS))),
                )
            }
        }
    }
}

@Composable
private fun TaskRow(t: WidgetTask) {
    val context = LocalContext.current
    Row(GlanceModifier.fillMaxWidth().height(ROW_DP.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            GlanceModifier.size(32.dp)
                .clickable(actionRunCallback<CompleteTaskAction>(actionParametersOf(CompleteTaskAction.TASK_ID to t.id)))
                .semantics { contentDescription = context.getString(R.string.task_checkbox_label, t.title) },
            contentAlignment = Alignment.Center,
        ) {
            Image(ImageProvider(R.drawable.widget_ring), null, GlanceModifier.size(20.dp))
        }
        Spacer(GlanceModifier.width(6.dp))
        Column(GlanceModifier.defaultWeight().clickable(actionStartActivity(context.openTask(t.id)))) {
            Text(t.title, style = TextStyle(color = WC.text, fontSize = 14.sp), maxLines = 1)
            t.meta?.let {
                Text(it, style = TextStyle(color = if (t.overdue) WC.flare else WC.volt, fontSize = 11.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            }
        }
    }
}

private fun Context.open(target: String?): Intent =
    Intent(this, MainActivity::class.java)
        .setData(Uri.parse("pulso://widget/${target ?: "hoje"}"))
        .apply { if (target != null) putExtra(MainActivity.EXTRA_OPEN, target) }
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

private fun Context.openTask(id: Long): Intent =
    open(MainActivity.OPEN_TASK).setData(Uri.parse("pulso://widget/tarefa/$id")).putExtra(MainActivity.EXTRA_TASK_ID, id)

/** Círculo de concluir: conclui (ou, se a tarefa se repete, leva à próxima data) e redesenha. */
class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TASK_ID] ?: return
        context.widgetEntryPoint().widgetData().complete(id)
        PulsoWidget().updateAll(context)
    }

    companion object {
        val TASK_ID = ActionParameters.Key<Long>("task_id")
    }
}
