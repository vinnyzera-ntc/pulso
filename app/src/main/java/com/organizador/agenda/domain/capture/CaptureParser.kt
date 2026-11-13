package com.organizador.agenda.domain.capture

import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.model.Recurrence
import com.organizador.agenda.domain.model.RecurrenceUnit
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class CaptureKind { Task, Event }

enum class SpanType { Date, Time, Duration, Tag, Priority, Recurrence }

/** Um trecho do texto que o parser entendeu (para destacar no campo). */
data class CaptureSpan(val range: IntRange, val type: SpanType)

data class ParsedCapture(
    val text: String,
    /** O texto sem os trechos reconhecidos, com a primeira letra maiúscula. */
    val title: String,
    val kind: CaptureKind,
    val date: LocalDate?,
    val time: LocalTime?,
    /** Só quando o texto diz ("por 1h", "das 14h às 15h"). */
    val durationMinutes: Int?,
    val tags: List<String>,
    val priority: Priority,
    /** Repetição entendida ("toda segunda", "todo dia 5", "a cada 2 semanas"), já presa à [date]. */
    val recurrence: Recurrence?,
    /** O trecho do texto que virou a repetição, para mostrar. */
    val recurrenceText: String?,
    val spans: List<CaptureSpan>,
) {
    val isEmpty: Boolean get() = title.isBlank()
}

/**
 * Entende frases curtas em português do Brasil, sem IA nem rede:
 * "ligar pro contador amanhã às 9h #pessoal", "reunião com Ana sexta 14h por 1h".
 *
 * Funciona por camadas de expressões regulares sobre o texto em minúsculas e sem acentos
 * (aceita "amanha", "as 9h", "terca"). Cada camada marca o trecho que consumiu, para a seguinte
 * não o reinterpretar ("por 1h" é duração, não horário).
 */
object CaptureParser {

    private const val MASK = '\u0001'
    private const val B = """(?<![\p{L}\d])""" // início de palavra
    private const val E = """(?![\p{L}\d])""" // fim de palavra

    /** Preposição opcional que entra no trecho: "na sexta", "para amanhã", "até dia 15". */
    private const val PREP = """(?:(?:n[oa]|para|pra|ate|em) (?:[oa] )?)?"""

    private val weekdays = mapOf(
        "segunda" to DayOfWeek.MONDAY, "terca" to DayOfWeek.TUESDAY, "quarta" to DayOfWeek.WEDNESDAY,
        "quinta" to DayOfWeek.THURSDAY, "sexta" to DayOfWeek.FRIDAY, "sabado" to DayOfWeek.SATURDAY,
        "domingo" to DayOfWeek.SUNDAY,
    )
    private val months = listOf(
        "janeiro", "fevereiro", "marco", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    )
    private val WD = weekdays.keys.joinToString("|")
    private val MO = months.joinToString("|")

    private val tagRe = Regex("""(?<!\S)#([\p{L}\d_-]+)""")
    private val priorityRe = Regex("""(?<!\S)!?p([1-4])(?!\S)""")
    // Repetição (na ordem em que são tentadas: as mais específicas primeiro).
    private val workdaysRe = Regex("""$B(?:tod[oa]s? (?:[oa]s )?)?dias? ute(?:is|l)$E""")
    private val monthDayRe = Regex("""$B(?:todo dia (\d{1,2})(?: de (?:cada|todo) mes)?|todo mes(?: no)? dia (\d{1,2}))$E""")
    private val everyNRe = Regex("""${B}a cada (\d{1,2}) (dias?|semanas?|mes(?:es)?|anos?)$E""")
    private val WDP = """(?:$WD)s?(?:-feiras?)?"""
    private val weekdayListRe = Regex("""${B}tod[oa]s? (?:[oa]s )?($WDP(?:(?:, ?| e )(?:[oa]s? )?$WDP)*)$E""")
    private val dailyRe = Regex("""$B(?:todos os dias|todo dia|diariamente)$E""")
    private val weeklyRe = Regex("""$B(?:toda semana|todas as semanas|semanalmente)$E""")
    private val monthlyRe = Regex("""$B(?:todo mes|todos os meses|mensalmente)$E""")
    private val yearlyRe = Regex("""$B(?:todo ano|todos os anos|anualmente)$E""")
    private val weekdayNameRe = Regex("""($WD)""")
    private val rangeRe = Regex("""${B}das? (\d{1,2})(?:[:h](\d{2})|h)? (?:ate )?(?:as|a) (\d{1,2})(?:[:h](\d{2})|h)?$E""")
    private val durationRe = Regex(
        """${B}(?:por|durante) (?:(meia hora)|(\d{1,3}) ?(horas?|h|minutos?|min|m)(?: ?(?:e )?(\d{1,2})(?: ?(?:minutos?|min|m))?)?)$E""",
    )

    private val afterTomorrowRe = Regex("""$B${PREP}depois de amanha$E""")
    private val todayTomorrowRe = Regex("""$B$PREP(hoje|amanha)$E""")
    private val slashDateRe = Regex("""$B$PREP(?:dia )?(\d{1,2})/(\d{1,2})(?:/(\d{4}|\d{2}))?$E""")
    private val dayOfMonthRe = Regex("""$B$PREP(?:dia (\d{1,2})(?: de ($MO))?|(\d{1,2}) de ($MO))$E""")
    private val inDaysRe = Regex("""$B(?:em|daqui a) (\d{1,2}) (dias?|semanas?)$E""")
    private val weekdayRe = Regex("""$B$PREP(proxim[oa] )?($WD)(?:-feira)?( que vem)?$E""")
    private val nextWeekRe = Regex("""$B$PREP(?:semana que vem|proxima semana)$E""")

    private val noonRe = Regex("""$B(?:as |a |ao )?(meio[- ]dia|meia[- ]noite)$E""")
    private val clockRe = Regex("""$B(?:as |a )?(\d{1,2})(?::(\d{2})|h(\d{2})?)(?: ?(?:da|de) (manha|tarde|noite))?$E""")
    private val atHourRe = Regex("""${B}as (\d{1,2})(?: ?(?:da|de) (manha|tarde|noite))?$E""")
    private val hourPeriodRe = Regex("""$B(\d{1,2}) (?:da|de) (manha|tarde|noite)$E""")
    private val periodRe = Regex("""$B(?:de|pela|a|na) (manha|tarde|noite)$E""")

    private val eventWordsRe = Regex(
        """$B(?:reuniao|consulta|encontro|almoco|jantar|call|entrevista|aula|medico|dentista|exame|voo|festa|cinema|show|evento)$E""",
    )

    /** Minúsculas e sem acentos, com o mesmo número de caracteres do original (os índices se mantêm). */
    internal fun normalize(text: String): String = buildString(text.length) {
        for (ch in text) {
            val base = Normalizer.normalize(ch.toString(), Normalizer.Form.NFD)[0]
            append(base.lowercaseChar())
        }
    }

    fun parse(text: String, now: LocalDateTime): ParsedCapture {
        val work = StringBuilder(normalize(text))
        val spans = mutableListOf<CaptureSpan>()
        // Caracteres que saem do título (os trechos entendidos).
        val removed = BooleanArray(text.length)
        val today = now.toLocalDate()

        /**
         * Marca o trecho como consumido, para as próximas camadas não o reinterpretarem.
         * Com [type], ele é destacado e sai do título; sem, fica no título (repetição).
         */
        fun take(range: IntRange, type: SpanType?) {
            for (i in range) {
                work[i] = MASK
                if (type != null) removed[i] = true
            }
            if (type != null) spans += CaptureSpan(range, type)
        }
        // Busca numa cópia: o MatchResult lê os grupos da entrada, que não pode mudar depois (take() mascara `work`).
        fun find(re: Regex): MatchResult? = re.find(work.toString())

        // 1. Etiquetas (todas).
        val tags = tagRe.findAll(work.toString()).toList().map { m ->
            take(m.range, SpanType.Tag)
            normalize(text.substring(m.groups[1]!!.range))
        }.distinct()

        // 2. Prioridade.
        val priority = find(priorityRe)?.let { m ->
            take(m.range, SpanType.Priority)
            Priority.of(m.groupValues[1].toInt())
        } ?: Priority.P4

        // 3. Repetição. Antes das datas, para "toda segunda" e "todo dia 5" não virarem uma data só.
        // Semanal sem dia dito ("toda semana") pega o dia da data resolvida mais abaixo.
        var rule: Recurrence? = null
        var weeklyInterval: Int? = null
        var recurrenceText: String? = null
        fun recur(m: MatchResult, r: Recurrence?, weeklyEvery: Int? = null) {
            take(m.range, SpanType.Recurrence)
            recurrenceText = text.substring(m.range)
            rule = r
            weeklyInterval = weeklyEvery
        }
        run {
            find(workdaysRe)?.let { recur(it, Recurrence(RecurrenceUnit.Weekly, weekdays = Recurrence.WORKDAYS)); return@run }
            find(monthDayRe)?.let { m ->
                val day = m.groupValues[1].ifEmpty { m.groupValues[2] }.toInt()
                if (day in 1..31) { recur(m, Recurrence.monthly(day)); return@run }
            }
            find(everyNRe)?.let { m ->
                val n = m.groupValues[1].toInt()
                if (n in 1..Recurrence.MAX_INTERVAL) {
                    val unit = m.groupValues[2]
                    when {
                        unit.startsWith("dia") -> recur(m, Recurrence.daily(n))
                        unit.startsWith("semana") -> recur(m, null, weeklyEvery = n)
                        unit.startsWith("mes") -> recur(m, Recurrence.monthly(null, n))
                        else -> recur(m, Recurrence.yearly(n))
                    }
                    return@run
                }
            }
            find(weekdayListRe)?.let { m ->
                val days = weekdayNameRe.findAll(m.groupValues[1]).map { weekdays.getValue(it.value) }.toSet()
                recur(m, Recurrence(RecurrenceUnit.Weekly, weekdays = days))
                return@run
            }
            find(dailyRe)?.let { recur(it, Recurrence.daily()); return@run }
            find(weeklyRe)?.let { recur(it, null, weeklyEvery = 1); return@run }
            find(monthlyRe)?.let { recur(it, Recurrence.monthly(null)); return@run }
            find(yearlyRe)?.let { recur(it, Recurrence.yearly()) }
        }

        var time: LocalTime? = null
        var duration: Int? = null

        // 4. Intervalo "das 14h às 15h30": hora + duração.
        find(rangeRe)?.let { m ->
            val start = clock(m.groupValues[1], m.groupValues[2], null)
            val end = clock(m.groupValues[3], m.groupValues[4], null)
            if (start != null && end != null && end > start) {
                take(m.range, SpanType.Time)
                time = start
                duration = (end.toSecondOfDay() - start.toSecondOfDay()) / 60
            }
        }

        // 5. Duração "por 1h30", "durante 45 minutos", "por meia hora".
        if (duration == null) {
            find(durationRe)?.let { m ->
                val (half, n, unit, extra) = m.destructured
                val minutes = when {
                    half.isNotEmpty() -> 30
                    unit.startsWith("h") -> n.toInt() * 60 + (extra.toIntOrNull() ?: 0)
                    else -> n.toInt()
                }
                if (minutes in 1..24 * 60) {
                    take(m.range, SpanType.Duration)
                    duration = minutes
                }
            }
        }

        // 6. Data (a primeira forma que casar).
        var date: LocalDate? = null
        var weekdayToday = false
        run {
            find(afterTomorrowRe)?.let { take(it.range, SpanType.Date); date = today.plusDays(2); return@run }
            find(todayTomorrowRe)?.let { m ->
                take(m.range, SpanType.Date)
                date = if (m.groupValues[1] == "hoje") today else today.plusDays(1)
                return@run
            }
            find(slashDateRe)?.let { m ->
                val (d, mo, y) = m.destructured
                val resolved = dateOf(d.toInt(), mo.toInt(), y.toIntOrNull()?.let { if (it < 100) 2000 + it else it }, today)
                if (resolved != null) { take(m.range, SpanType.Date); date = resolved; return@run }
            }
            find(dayOfMonthRe)?.let { m ->
                val day = (m.groupValues[1].ifEmpty { m.groupValues[3] }).toInt()
                val monthName = m.groupValues[2].ifEmpty { m.groupValues[4] }
                val resolved = if (monthName.isEmpty()) dayThisOrNextMonth(day, today)
                else dateOf(day, months.indexOf(monthName) + 1, null, today)
                if (resolved != null) { take(m.range, SpanType.Date); date = resolved; return@run }
            }
            find(inDaysRe)?.let { m ->
                val n = m.groupValues[1].toLong()
                take(m.range, SpanType.Date)
                date = if (m.groupValues[2].startsWith("dia")) today.plusDays(n) else today.plusWeeks(n)
                return@run
            }
            find(weekdayRe)?.let { m ->
                val target = weekdays.getValue(m.groupValues[2])
                val explicitNext = m.groupValues[1].isNotEmpty() || m.groupValues[3].isNotEmpty()
                val ahead = (target.value - today.dayOfWeek.value + 7) % 7
                take(m.range, SpanType.Date)
                // "sexta" dita numa sexta: hoje, a menos que diga "que vem" (ou a hora já tenha passado, visto abaixo).
                weekdayToday = ahead == 0 && !explicitNext
                date = today.plusDays(if (ahead == 0 && explicitNext) 7L else ahead.toLong())
                return@run
            }
            find(nextWeekRe)?.let { take(it.range, SpanType.Date); date = today.plusWeeks(1) }
        }

        // 7. Hora.
        if (time == null) {
            run {
                find(noonRe)?.let { m ->
                    take(m.range, SpanType.Time)
                    time = if (m.groupValues[1].startsWith("meio")) LocalTime.NOON else LocalTime.MIDNIGHT
                    return@run
                }
                find(clockRe)?.let { m ->
                    val minute = m.groupValues[2].ifEmpty { m.groupValues[3] }
                    clock(m.groupValues[1], minute, m.groupValues[4])?.let { take(m.range, SpanType.Time); time = it; return@run }
                }
                find(atHourRe)?.let { m ->
                    clock(m.groupValues[1], "", m.groupValues[2])?.let { take(m.range, SpanType.Time); time = it; return@run }
                }
                find(hourPeriodRe)?.let { m ->
                    clock(m.groupValues[1], "", m.groupValues[2])?.let { take(m.range, SpanType.Time); time = it; return@run }
                }
                find(periodRe)?.let { m ->
                    take(m.range, SpanType.Time)
                    time = when (m.groupValues[1]) {
                        "manha" -> LocalTime.of(9, 0)
                        "tarde" -> LocalTime.of(15, 0)
                        else -> LocalTime.of(20, 0)
                    }
                }
            }
        }

        // Hora sem data: hoje se ainda não passou, senão amanhã. Dia da semana = hoje com hora passada: semana que vem.
        val t = time
        if (t != null) {
            if (date == null) date = if (t > now.toLocalTime()) today else today.plusDays(1)
            else if (weekdayToday && t <= now.toLocalTime()) date = today.plusWeeks(1)
        }

        // Repetição sem data dita: começa na primeira ocorrência a partir de hoje (ou de amanhã,
        // se a hora de hoje já passou). Com data dita, a série começa nela.
        val explicitDate = spans.any { it.type == SpanType.Date }
        weeklyInterval?.let { n ->
            val day = date ?: today
            rule = Recurrence(RecurrenceUnit.Weekly, n, setOf(day.dayOfWeek))
        }
        rule?.let { r ->
            if (!explicitDate) {
                val base = if (t != null && t <= now.toLocalTime()) today.plusDays(1) else today
                date = r.firstOnOrAfter(base)
            }
            rule = r.anchoredOn(date!!)
        }

        val title = buildTitle(text, removed)
        val kind = if (t != null && (duration != null || eventWordsRe.containsMatchIn(normalize(title)))) CaptureKind.Event else CaptureKind.Task
        return ParsedCapture(text, title, kind, date, t, duration, tags, priority, rule, recurrenceText, spans.sortedBy { it.range.first })
    }

    /** "9", "30", "tarde" -> 21:30. Nulo se a hora não existe. */
    private fun clock(hour: String, minute: String, period: String?): LocalTime? {
        var h = hour.toIntOrNull() ?: return null
        val m = minute.toIntOrNull() ?: 0
        if (h > 23 || m > 59) return null
        when (period) {
            "tarde" -> if (h < 12) h += 12
            "noite" -> if (h == 12) h = 0 else if (h < 12) h += 12
            "manha" -> if (h == 12) h = 0
        }
        return if (h in 0..23) LocalTime.of(h, m) else null
    }

    /** Dia/mês(/ano). Sem ano: este ano, ou o próximo se a data já passou. Nulo se a data não existe. */
    private fun dateOf(day: Int, month: Int, year: Int?, today: LocalDate): LocalDate? {
        fun at(y: Int) = runCatching { LocalDate.of(y, month, day) }.getOrNull()
        if (year != null) return at(year)
        val thisYear = at(today.year) ?: return at(today.year + 1)
        return if (thisYear < today) at(today.year + 1) else thisYear
    }

    /** "dia 15": neste mês se ainda não passou; senão no próximo. */
    private fun dayThisOrNextMonth(day: Int, today: LocalDate): LocalDate? {
        if (day !in 1..31) return null
        var month = java.time.YearMonth.from(today)
        if (day < today.dayOfMonth) month = month.plusMonths(1)
        // Pula meses sem esse dia (dia 31 em novembro vai para dezembro).
        repeat(3) {
            if (month.isValidDay(day)) return month.atDay(day)
            month = month.plusMonths(1)
        }
        return null
    }

    private val danglingRe = Regex("""(?:^|\s)(?:as|a|em|no|na|para|pra|de|e|ate)$""")

    /** O texto original menos os trechos destacados, limpo de sobras. */
    private fun buildTitle(text: String, removed: BooleanArray): String {
        val sb = StringBuilder(text.length)
        for (i in text.indices) sb.append(if (removed[i]) ' ' else text[i])
        var title = sb.toString().replace(Regex("""\s+"""), " ").trim().trim(',', ';', '-', '.', ' ')
        // Conectivos que sobraram no fim: "ligar pro contador às" -> "ligar pro contador".
        while (true) {
            val m = danglingRe.find(normalize(title)) ?: break
            title = title.substring(0, m.range.first).trim().trim(',', ';', '-', ' ')
        }
        return title.replaceFirstChar { it.titlecase() }
    }
}
