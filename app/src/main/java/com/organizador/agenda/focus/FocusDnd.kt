package com.organizador.agenda.focus

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import android.service.notification.ZenPolicy
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import com.organizador.agenda.MainActivity
import com.organizador.agenda.R
import com.organizador.agenda.di.ApplicationScope
import com.organizador.agenda.domain.focus.FocusState
import com.organizador.agenda.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Não perturbe do modo foco. Liga nas fases de foco e desliga nas pausas e ao encerrar
 * ([FocusDndRules.wanted]).
 *
 * - Android 10+: uma regra automática própria ("Foco do Pulso") com política só para ela:
 *   sem sons (alarmes e mídia continuam, então a música lofi toca), sem aparecer por cima, sem
 *   tela cheia nem luz; as notificações ficam na gaveta. Ligar e desligar não mexe no Não
 *   perturbe que o usuário já tenha configurado.
 * - Android 8/9: o modo prioridade do sistema, só se estava em "tudo"; ao terminar, volta só se
 *   ainda estiver como deixamos.
 *
 * O estado (ativo, id da regra, lista de liberados) fica em SharedPreferences [PREFS]: se o
 * processo morrer no meio, a próxima vez que ele subir (o serviço de notificações sobe sozinho)
 * desliga o que ficou ligado. A API do Android não aceita lista de apps no Não perturbe: os
 * liberados tocam pelo repasse de [FocusNotificationListener].
 */
@Singleton
class FocusDnd @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val controller: FocusController,
    private val settings: SettingsRepository,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val prefs = prefs(context)
    private val mutex = Mutex()
    private val refreshes = MutableStateFlow(0)
    private var started = false

    fun start() {
        if (started) return
        started = true
        // Mudanças de fase: síncrono, antes do aviso de fim de fase (que não pode sair silenciado).
        controller.addStateHook { sync(it) }
        // Mudanças nos Ajustes e voltas ao app (o acesso pode ter sido dado ou tirado).
        val options = settings.settings.map { it.focus.doNotDisturb to it.focus.dndAllowedPackages }.distinctUntilChanged()
        scope.launch {
            combine(options, refreshes) { _, _ -> }.collect { sync(controller.state.value) }
        }
    }

    /** Relê o acesso (chamado ao voltar para o app). */
    fun refresh() = refreshes.update { it + 1 }

    private suspend fun sync(state: FocusState) = mutex.withLock {
        val focus = settings.settings.first().focus
        prefs.edit { putStringSet(KEY_ALLOWED, focus.dndAllowedPackages) }
        val want = FocusDndRules.wanted(state, focus.doNotDisturb, manager.isNotificationPolicyAccessGranted)
        val active = prefs.getBoolean(KEY_ACTIVE, false)
        try {
            if (want && !active) turnOn() else if (!want && active) turnOff()
        } catch (e: SecurityException) {
            // Acesso tirado no meio do foco: o sistema já desfaz a regra; só limpa o estado.
            Log.w(TAG, "Sem acesso ao Não perturbe", e)
            prefs.edit { putBoolean(KEY_ACTIVE, false).putBoolean(KEY_LEGACY_SET, false) }
            FocusRelay.cancelAll(context)
        }
    }

    private fun turnOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            manager.setAutomaticZenRuleState(ensureRule(), condition(Condition.STATE_TRUE))
        } else {
            val setByUs = FocusDndRules.legacyShouldEnable(manager.currentInterruptionFilter)
            if (setByUs) manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            prefs.edit { putBoolean(KEY_LEGACY_SET, setByUs) }
        }
        // O canal do repasse precisa existir (e furar o Não perturbe) antes da primeira notificação.
        FocusRelay.ensureChannel(context)
        prefs.edit { putBoolean(KEY_ACTIVE, true) }
    }

    private fun turnOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            prefs.getString(KEY_RULE_ID, null)?.let { manager.setAutomaticZenRuleState(it, condition(Condition.STATE_FALSE)) }
        } else if (FocusDndRules.legacyShouldRestore(manager.currentInterruptionFilter, prefs.getBoolean(KEY_LEGACY_SET, false))) {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
        prefs.edit { putBoolean(KEY_ACTIVE, false).putBoolean(KEY_LEGACY_SET, false) }
        FocusRelay.cancelAll(context)
    }

    private val conditionId: Uri = Uri.Builder().scheme(Condition.SCHEME).authority(context.packageName).appendPath("foco").build()

    private fun condition(state: Int) = Condition(conditionId, context.getString(R.string.dnd_rule_summary), state)

    /** A regra "Foco do Pulso": criada uma vez e atualizada (se o usuário a apagar, é recriada). */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun ensureRule(): String {
        val policy = ZenPolicy.Builder()
            .disallowAllSounds()
            .allowAlarms(true)
            .allowMedia(true)
            .showPeeking(false)
            .showFullScreenIntent(false)
            .showLights(false)
            .showInNotificationList(true)
            .build()
        val rule = AutomaticZenRule(
            context.getString(R.string.dnd_rule_name),
            null,
            ComponentName(context, MainActivity::class.java),
            conditionId,
            policy,
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            true,
        )
        val id = prefs.getString(KEY_RULE_ID, null)
        val existing = id?.let { manager.getAutomaticZenRule(it) }
        if (id != null && existing != null) {
            // Se o usuário desativou a regra nas configurações, continua desativada.
            rule.isEnabled = existing.isEnabled
            manager.updateAutomaticZenRule(id, rule)
            return id
        }
        return manager.addAutomaticZenRule(rule).also { prefs.edit { putString(KEY_RULE_ID, it) } }
    }

    companion object {
        private const val TAG = "PulsoDnd"
        const val PREFS = "focus_dnd"
        private const val KEY_ACTIVE = "active"
        private const val KEY_RULE_ID = "rule_id"
        private const val KEY_LEGACY_SET = "legacy_set"
        private const val KEY_ALLOWED = "allowed"

        fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        /** O Não perturbe do foco está ligado agora (lido pelo serviço de notificações). */
        fun isActive(prefs: SharedPreferences): Boolean = prefs.getBoolean(KEY_ACTIVE, false)

        /** Espelho dos apps liberados, para o serviço de notificações ler sem esperar o DataStore. */
        fun allowed(prefs: SharedPreferences): Set<String> = prefs.getStringSet(KEY_ALLOWED, null).orEmpty()
    }
}
