package com.organizador.agenda.domain.capture

import com.organizador.agenda.domain.capture.CaptureKind.Event
import com.organizador.agenda.domain.capture.CaptureKind.Task
import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.model.Recurrence
import com.organizador.agenda.domain.model.RecurrenceUnit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Agora" fixo: terça, 10 de novembro de 2026, 10:00. */
class CaptureParserTest {
    private val today = LocalDate.of(2026, 11, 10)
    private val now = LocalDateTime.of(today, LocalTime.of(10, 0))
    private fun d(day: Int, month: Int = 11, year: Int = 2026) = LocalDate.of(year, month, day)
    private fun t(h: Int, m: Int = 0) = LocalTime.of(h, m)
    private fun parse(text: String) = CaptureParser.parse(text, now)

    private data class Case(
        val text: String,
        val title: String,
        val date: LocalDate? = null,
        val time: LocalTime? = null,
        val duration: Int? = null,
        val kind: CaptureKind = Task,
        val tags: List<String> = emptyList(),
        val priority: Priority = Priority.P4,
    )

    private fun check(cases: List<Case>) {
        val failures = cases.mapNotNull { c ->
            val p = parse(c.text)
            val got = Case(c.text, p.title, p.date, p.time, p.durationMinutes, p.kind, p.tags, p.priority)
            if (got == c) null else "\"${c.text}\"\n   esperado: $c\n   obtido:   $got"
        }
        assertTrue("${failures.size} de ${cases.size} casos falharam:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun exemplosDoPedido() = check(
        listOf(
            Case("ligar pro contador amanhã às 9h #pessoal", "Ligar pro contador", d(11), t(9), tags = listOf("pessoal")),
            Case("reunião com Ana sexta 14h por 1h", "Reunião com Ana", d(13), t(14), 60, Event),
        ),
    )

    @Test
    fun datasRelativas() = check(
        listOf(
            Case("comprar pão", "Comprar pão"),
            Case("pagar boleto hoje", "Pagar boleto", today),
            Case("pagar boleto amanha", "Pagar boleto", d(11)),
            Case("depois de amanhã ir ao banco", "Ir ao banco", d(12)),
            Case("voltar em 3 dias", "Voltar", d(13)),
            Case("conferir em 1 dia", "Conferir", d(11)),
            Case("revisar daqui a 2 semanas", "Revisar", d(24)),
            Case("planejar semana que vem", "Planejar", d(17)),
            Case("planejar próxima semana", "Planejar", d(17)),
            Case("deixar pra amanhã", "Deixar", d(11)),
            Case("ligar para a Ana amanhã", "Ligar para a Ana", d(11)),
        ),
    )

    @Test
    fun diasDaSemana() = check(
        listOf(
            Case("academia segunda", "Academia", d(16)),
            Case("academia terça", "Academia", today),
            Case("academia terca 18h", "Academia", today, t(18)),
            // Terça com hora que já passou: a próxima terça.
            Case("academia terça 9h", "Academia", d(17), t(9)),
            Case("entregar até sexta", "Entregar", d(13)),
            Case("mercado no sábado", "Mercado", d(14)),
            Case("call sexta que vem 15h", "Call", d(13), t(15), kind = Event),
            Case("almoço próxima terça meio-dia", "Almoço", d(17), t(12), kind = Event),
            Case("segunda-feira reunião 9h", "Reunião", d(16), t(9), kind = Event),
            Case("almoço domingo", "Almoço", d(15)),
        ),
    )

    @Test
    fun datasDoCalendario() = check(
        listOf(
            Case("dentista dia 15 às 10h30", "Dentista", d(15), t(10, 30), kind = Event),
            // Dia que já passou neste mês: mês que vem.
            Case("viagem dia 5", "Viagem", d(5, 12)),
            Case("pagar no dia 20", "Pagar", d(20)),
            // Novembro não tem dia 31: vai para dezembro.
            Case("pagar aluguel dia 31", "Pagar aluguel", d(31, 12)),
            Case("entregar relatório 15/11", "Entregar relatório", d(15)),
            // Data que já passou neste ano: ano que vem.
            Case("aniversário 03/02", "Aniversário", d(3, 2, 2027)),
            Case("prova 20/12/2026 8h", "Prova", d(20, 12), t(8)),
            Case("prova 15/11/26", "Prova", d(15)),
            Case("consulta 20 de dezembro às 14h", "Consulta", d(20, 12), t(14), kind = Event),
            Case("festa dia 2 de janeiro", "Festa", d(2, 1, 2027)),
        ),
    )

    @Test
    fun horas() = check(
        listOf(
            // Hora sem data: hoje se ainda não passou, senão amanhã.
            Case("ligar 15h", "Ligar", today, t(15)),
            Case("ligar 9h", "Ligar", d(11), t(9)),
            Case("ligar às 9", "Ligar", d(11), t(9)),
            Case("ligar as 14", "Ligar", today, t(14)),
            Case("ligar 9:30", "Ligar", d(11), t(9, 30)),
            Case("ligar 14:00", "Ligar", today, t(14)),
            Case("ligar 9h30", "Ligar", d(11), t(9, 30)),
            Case("jantar às 8 da noite", "Jantar", today, t(20), kind = Event),
            Case("remédio 3 da tarde", "Remédio", today, t(15)),
            Case("exame 7 da manhã amanhã", "Exame", d(11), t(7), kind = Event),
            Case("festa 12 da noite", "Festa", d(11), t(0), kind = Event),
            Case("correr amanhã de manhã", "Correr", d(11), t(9)),
            Case("estudar hoje à tarde", "Estudar", today, t(15)),
            Case("filme hoje à noite", "Filme", today, t(20)),
            Case("meio dia almoço", "Almoço", today, t(12), kind = Event),
            Case("revisar meia-noite", "Revisar", d(11), t(0)),
        ),
    )

    @Test
    fun duracaoEIntervalo() = check(
        listOf(
            Case("reunião das 14h às 15h30", "Reunião", today, t(14), 90, Event),
            Case("reunião das 14 às 15", "Reunião", today, t(14), 60, Event),
            // Duração sem hora continua tarefa.
            Case("estudar por 45 min", "Estudar", duration = 45),
            Case("estudar amanhã 19h por 1h30", "Estudar", d(11), t(19), 90, Event),
            Case("treino hoje 18h por 1 hora e 30 minutos", "Treino", today, t(18), 90, Event),
            Case("foco 16h durante 2 horas", "Foco", today, t(16), 120, Event),
            Case("ler 20h por meia hora", "Ler", today, t(20), 30, Event),
            Case("sexta 14h reunião com Ana por 1h", "Reunião com Ana", d(13), t(14), 60, Event),
        ),
    )

    @Test
    fun etiquetasEPrioridade() = check(
        listOf(
            Case("comprar tinta #casa #obra", "Comprar tinta", tags = listOf("casa", "obra")),
            Case("ligar #Família", "Ligar", tags = listOf("familia")),
            Case("#casa #casa lavar", "Lavar", tags = listOf("casa")),
            Case("enviar proposta p1", "Enviar proposta", priority = Priority.P1),
            Case("!p2 revisar contrato", "Revisar contrato", priority = Priority.P2),
            Case("resolver P3 depois", "Resolver depois", priority = Priority.P3),
            // "p1" dentro de uma palavra não é prioridade.
            Case("ap1 teste", "Ap1 teste"),
            Case("ligar pro contador amanhã 9h #pessoal p1", "Ligar pro contador", d(11), t(9), tags = listOf("pessoal"), priority = Priority.P1),
        ),
    )

    @Test
    fun entradasQueNaoDevemSerInterpretadas() = check(
        listOf(
            Case("", ""),
            Case("#x", "", tags = listOf("x")),
            Case("comprar 2 pães", "Comprar 2 pães"),
            Case("ligar 25h", "Ligar 25h"),
            Case("consulta 31/02", "Consulta 31/02"),
            Case("dar bom dia", "Dar bom dia"),
            Case("ler capítulo 12", "Ler capítulo 12"),
            Case("REUNIÃO AMANHÃ 10H", "REUNIÃO", d(11), t(10), kind = Event),
        ),
    )

    // Hoje nos testes: terça, 10/11/2026, 10:00.
    @Test
    fun repeticao_semanal_comecaNaProximaOcorrencia() {
        val a = parse("academia toda segunda 7h")
        assertEquals(Recurrence.weekly(DayOfWeek.MONDAY), a.recurrence)
        assertEquals("toda segunda", a.recurrenceText)
        assertEquals("Academia", a.title)
        assertEquals(LocalDate.of(2026, 11, 16), a.date)
        assertEquals(t(7), a.time)

        val b = parse("relatório todas as sextas")
        assertEquals(Recurrence.weekly(DayOfWeek.FRIDAY), b.recurrence)
        assertEquals(LocalDate.of(2026, 11, 13), b.date)

        val c = parse("inglês toda segunda e quarta às 19h")
        assertEquals(Recurrence.weekly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), c.recurrence)
        assertEquals(LocalDate.of(2026, 11, 11), c.date)
        assertEquals("Inglês", c.title)
    }

    @Test
    fun repeticao_diaria_horaPassadaComecaAmanha() {
        val a = parse("tomar remédio todo dia 8h")
        assertEquals(Recurrence.daily(), a.recurrence)
        assertEquals("Tomar remédio", a.title)
        assertEquals(d(11), a.date)
        assertEquals(d(10), parse("tomar remédio todo dia 22h").date)
        assertEquals(d(10), parse("beber água diariamente").date)
    }

    @Test
    fun repeticao_mensalAnualEDiasUteis() {
        val a = parse("pagar aluguel todo dia 5")
        assertEquals(Recurrence.monthly(5), a.recurrence)
        assertEquals(LocalDate.of(2026, 12, 5), a.date)
        assertEquals("Pagar aluguel", a.title)

        assertEquals(Recurrence.monthly(15), parse("fatura todo mês dia 15").recurrence)
        // "todo mês" sem dia: o dia da data (hoje, se não disser).
        assertEquals(Recurrence.monthly(10), parse("backup todo mês").recurrence)
        assertEquals(Recurrence.monthly(20), parse("backup todo mês dia 20/11").let { it.recurrence })

        val y = parse("aniversário da Carol 20/11 todo ano")
        assertEquals(Recurrence.yearly(), y.recurrence)
        assertEquals(LocalDate.of(2026, 11, 20), y.date)

        val w = parse("ponto dias úteis 9h")
        assertEquals(Recurrence(RecurrenceUnit.Weekly, weekdays = Recurrence.WORKDAYS), w.recurrence)
        assertEquals(d(11), w.date)
    }

    @Test
    fun repeticao_aCadaN() {
        assertEquals(Recurrence.daily(15), parse("trocar filtro a cada 15 dias").recurrence)
        assertEquals(Recurrence.weekly(DayOfWeek.TUESDAY, interval = 2), parse("faxina a cada 2 semanas").recurrence)
        assertEquals(Recurrence.weekly(DayOfWeek.FRIDAY, interval = 2), parse("faxina sexta a cada 2 semanas").recurrence)
        assertEquals(Recurrence.monthly(10, 3), parse("revisão a cada 3 meses").recurrence)
        assertNull(parse("ligar amanhã").recurrence)
    }

    @Test
    fun trechosDestacados_apontamParaOTextoOriginal() {
        val text = "ligar pro contador amanhã às 9h #pessoal"
        val p = parse(text)
        assertEquals(
            listOf(SpanType.Date to "amanhã", SpanType.Time to "às 9h", SpanType.Tag to "#pessoal"),
            p.spans.map { it.type to text.substring(it.range) },
        )
        val q = parse("reunião sexta 14h por 1h p2")
        assertEquals(listOf(SpanType.Date, SpanType.Time, SpanType.Duration, SpanType.Priority), q.spans.map { it.type })
        assertEquals(listOf(SpanType.Recurrence), parse("academia toda segunda").spans.map { it.type })
    }

    @Test
    fun normalize_mantemOTamanho() {
        val text = "Amanhã às 9h — reunião, AÇÃO!"
        assertEquals(text.length, CaptureParser.normalize(text).length)
        assertEquals("amanha as 9h", CaptureParser.normalize("Amanhã às 9h"))
    }
}
