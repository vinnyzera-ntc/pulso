package com.organizador.agenda.focus

import android.app.NotificationManager
import com.organizador.agenda.domain.focus.FocusState
import com.organizador.agenda.domain.model.FocusKind

/** Regras do Não perturbe do foco e do repasse dos apps liberados. Puras (testadas em JVM). */
object FocusDndRules {

    /**
     * O Não perturbe fica ligado só nas fases de foco (também pausadas), com a opção ligada e o
     * acesso concedido. Nas pausas e com a fase concluída (o aviso de fim precisa tocar), desliga.
     */
    fun wanted(state: FocusState, enabled: Boolean, hasAccess: Boolean): Boolean =
        enabled && hasAccess && state.active && state.phase == FocusKind.Focus && !state.finished

    /** Android 8/9: só liga se o modo atual for "tudo" (não atropela um Não perturbe do usuário). */
    fun legacyShouldEnable(currentFilter: Int): Boolean = currentFilter == NotificationManager.INTERRUPTION_FILTER_ALL

    /**
     * Android 8/9, ao terminar: volta para "tudo" só se fomos nós que ligamos e o modo ainda é o
     * nosso (prioridade). Se o usuário mudou no meio, fica como ele deixou.
     */
    fun legacyShouldRestore(currentFilter: Int, setByUs: Boolean): Boolean =
        setByUs && currentFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY

    /** O que importa de uma notificação postada para decidir o repasse. */
    data class Posted(
        val packageName: String,
        val ongoing: Boolean = false,
        val foregroundService: Boolean = false,
        val groupSummary: Boolean = false,
        /** Tem título ou texto (uma notificação vazia não vale um som). */
        val hasContent: Boolean = true,
        /**
         * O Não perturbe segurou esta notificação (Ranking.matchesInterruptionFilter = false).
         * Se ela já tocou sozinha (ex.: regra do usuário que deixa passar), repassar tocaria duas vezes.
         */
        val intercepted: Boolean = true,
    )

    /**
     * Repassa (num canal que fura o Não perturbe) a notificação de um app liberado enquanto o
     * Não perturbe do foco está ativo e segurou a notificação. Ficam de fora as contínuas, as de
     * serviço em primeiro plano, os resumos de grupo, as vazias e as do próprio Pulso.
     */
    fun shouldRelay(n: Posted, dndActive: Boolean, allowed: Set<String>, ownPackage: String): Boolean =
        dndActive && n.intercepted && n.packageName != ownPackage && n.packageName in allowed &&
            !n.ongoing && !n.foregroundService && !n.groupSummary && n.hasContent
}
