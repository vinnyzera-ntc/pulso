package com.organizador.agenda.domain.calendar

import com.organizador.agenda.domain.model.DeviceCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarSelectionTest {

    private fun cal(
        id: Long,
        name: String = "Agenda $id",
        google: Boolean = true,
        primary: Boolean = false,
        visible: Boolean = true,
        write: Boolean = true,
    ) = DeviceCalendar(
        id = id, displayName = name, accountName = if (google) "eu@gmail.com" else "Samsung",
        accountType = if (google) DeviceCalendar.GOOGLE_ACCOUNT_TYPE else "com.samsung.android.calendar",
        color = 0, isPrimary = primary, visibleInSystem = visible, canWrite = write,
    )

    @Test
    fun visibleIds_semConfiguracao_segueOSistema() {
        val cals = listOf(cal(1), cal(2, visible = false), cal(3))
        assertEquals(setOf(1L, 3L), CalendarSelection.visibleIds(cals, null))
    }

    @Test
    fun visibleIds_configurado_descartaAgendasQueSumiram() {
        val cals = listOf(cal(1), cal(2))
        assertEquals(setOf(2L), CalendarSelection.visibleIds(cals, setOf(2L, 99L)))
    }

    @Test
    fun visibleIds_configuradoVazio_naoVoltaAoPadrao() {
        assertEquals(emptySet<Long>(), CalendarSelection.visibleIds(listOf(cal(1)), emptySet()))
    }

    @Test
    fun toggle_partindoDoPadrao() {
        val cals = listOf(cal(1), cal(2, visible = false))
        assertEquals(setOf(1L, 2L), CalendarSelection.toggle(cals, null, 2, true))
        assertEquals(emptySet<Long>(), CalendarSelection.toggle(cals, null, 1, false))
    }

    @Test
    fun defaultForCreate_respeitaEscolhaComEscrita() {
        val cals = listOf(cal(1, primary = true), cal(2))
        assertEquals(2L, CalendarSelection.defaultForCreate(cals, 2)?.id)
    }

    @Test
    fun defaultForCreate_escolhaSemEscrita_caiNaPrincipalGoogle() {
        val cals = listOf(cal(1, name = "Feriados", write = false), cal(2, name = "Zeta"), cal(3, name = "Eu", primary = true))
        assertEquals(3L, CalendarSelection.defaultForCreate(cals, 1)?.id)
    }

    @Test
    fun defaultForCreate_preferePrincipalGoogleASamsung() {
        val cals = listOf(cal(1, google = false, primary = true), cal(2, google = true, primary = true))
        assertEquals(2L, CalendarSelection.defaultForCreate(cals, null)?.id)
    }

    @Test
    fun defaultForCreate_semGooglePrincipal_usaGoogleQualquer() {
        val cals = listOf(cal(1, google = false), cal(2, name = "Trabalho"), cal(3, name = "Casa"))
        assertEquals(3L, CalendarSelection.defaultForCreate(cals, null)?.id)
    }

    @Test
    fun defaultForCreate_nenhumaComEscrita() {
        assertNull(CalendarSelection.defaultForCreate(listOf(cal(1, write = false)), null))
    }
}
