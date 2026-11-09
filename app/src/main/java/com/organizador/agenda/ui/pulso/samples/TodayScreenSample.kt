package com.organizador.agenda.ui.pulso.samples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoSpace
import com.organizador.agenda.ui.pulso.PulsoTheme
import com.organizador.agenda.ui.pulso.components.BottomNavPill
import com.organizador.agenda.ui.pulso.components.ChipTone
import com.organizador.agenda.ui.pulso.components.LabelIcon
import com.organizador.agenda.ui.pulso.components.LiveDot
import com.organizador.agenda.ui.pulso.components.NavItem
import com.organizador.agenda.ui.pulso.components.NowEventCard
import com.organizador.agenda.ui.pulso.components.Person
import com.organizador.agenda.ui.pulso.components.Priority
import com.organizador.agenda.ui.pulso.components.ProgressSegments
import com.organizador.agenda.ui.pulso.components.PulsoChip
import com.organizador.agenda.ui.pulso.components.SectionLabel
import com.organizador.agenda.ui.pulso.components.TaskRow
import com.organizador.agenda.ui.pulso.components.aurora

/** Reference composition of the "Hoje" screen. Requires material-icons-extended for the icons. */
private data class DemoTask(val title: String, val priority: Priority, val time: String?, val tag: String?, val overdue: Boolean = false, val done: Boolean = false)

@Composable
fun TodayScreenSample() {
    val c = Pulso.colors
    var tab by remember { mutableIntStateOf(0) }
    val tasks = remember {
        mutableStateListOf(
            DemoTask("Revisar regras do firewall", Priority.None, null, null, done = true),
            DemoTask("Enviar relatório de vulnerabilidades", Priority.P1, null, null, overdue = true),
            DemoTask("Atualizar diagrama de rede", Priority.P2, "14:00", "#arquitetura"),
            DemoTask("Ligar para o contador", Priority.None, "16:30", "#pessoal"),
        )
    }
    Scaffold(
        containerColor = c.bg,
        bottomBar = {
            BottomNavPill(
                items = listOf(
                    NavItem("Hoje", Icons.Outlined.WbSunny), NavItem("Agenda", Icons.Outlined.CalendarMonth),
                    NavItem("Tarefas", Icons.AutoMirrored.Outlined.List), NavItem("Foco", Icons.Outlined.Bolt),
                ),
                selectedIndex = tab, onSelect = { tab = it }, onCreate = { /* open New sheet; long-press → Quick Capture */ },
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().aurora(c)) {
            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(start = PulsoSpace.gutter, end = PulsoSpace.gutter, top = 14.dp, bottom = inner.calculateBottomPadding() + 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item {
                    Column(Modifier.padding(bottom = 20.dp)) {
                        Text("SEXTA · 6 NOV", style = Pulso.type.eyebrow, color = c.text2)
                        Text("Bom dia, Ana", style = Pulso.type.headline, color = c.text, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                        val done = tasks.count { it.done }
                        ProgressSegments(total = 8, done = done + 4)
                        Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${done + 4} de 8 tarefas", style = Pulso.type.meta, color = c.text2)
                            Text("${(done + 4) * 100 / 8}%", style = Pulso.type.numeric, color = c.text2)
                        }
                    }
                }
                item {
                    SectionLabel("Agora", leading = { LiveDot() }, actionLabel = "Agenda", onAction = {})
                    Spacer(Modifier.height(8.dp))
                    NowEventCard(
                        timeRange = "10:00 – 11:30", remaining = "termina em 43 min", title = "Workshop Zero Trust",
                        progress = 0.52f, location = "Sala 3 · Prédio B",
                        attendees = listOf(Person("1", "Thomas H"), Person("2", "Valéria R"), Person("3", "James S"), Person("4", "João N")),
                    )
                    Spacer(Modifier.height(22.dp))
                    SectionLabel("Tarefas de hoje", leading = { LabelIcon(Icons.AutoMirrored.Outlined.List) }, actionLabel = "Ver todas", onAction = {})
                }
                itemsIndexed(tasks) { i, t ->
                    TaskRow(
                        title = t.title, checked = t.done, priority = t.priority,
                        onCheckedChange = { tasks[i] = t.copy(done = it) },
                        meta = {
                            if (t.overdue) PulsoChip("Atrasada · ontem", tone = ChipTone.Flare, icon = Icons.Outlined.Flag)
                            if (t.time != null) PulsoChip(t.time, tone = if (t.priority == Priority.P2) ChipTone.Volt else ChipTone.Neutral, icon = Icons.Outlined.Schedule)
                            if (t.tag != null) PulsoChip(t.tag)
                        },
                    )
                    if (i < tasks.lastIndex) HorizontalDivider(color = c.line, modifier = Modifier.padding(start = 35.dp))
                }
            }
        }
    }
}

@Preview(name = "Hoje · escuro", showBackground = true, backgroundColor = 0xFF0E0E0F, widthDp = 360, heightDp = 780)
@Composable
private fun TodayDarkPreview() = PulsoTheme(darkTheme = true) { TodayScreenSample() }

@Preview(name = "Hoje · claro", showBackground = true, backgroundColor = 0xFFF2F4F3, widthDp = 360, heightDp = 780)
@Composable
private fun TodayLightPreview() = PulsoTheme(darkTheme = false) { TodayScreenSample() }

