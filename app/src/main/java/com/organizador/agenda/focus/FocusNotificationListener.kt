package com.organizador.agenda.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.edit
import com.organizador.agenda.R
import com.organizador.agenda.alarm.NOTIFICATION_ACCENT_VOLT

/**
 * Durante o Não perturbe do foco, as notificações dos apps liberados ficam seguradas como as
 * outras (a API do Android não aceita lista de apps). Este serviço as vê chegar e as reapresenta
 * num canal do Pulso que fura o Não perturbe, com o mesmo título, texto e toque (contentIntent).
 * Fora do foco, não faz nada. Não guarda nem envia o conteúdo das notificações.
 */
class FocusNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val prefs = FocusDnd.prefs(this)
        if (!FocusDnd.isActive(prefs)) return
        val n = sbn.notification ?: return
        val posted = FocusDndRules.Posted(
            packageName = sbn.packageName,
            ongoing = sbn.isOngoing,
            foregroundService = n.flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
            groupSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            hasContent = !FocusRelay.title(n).isNullOrBlank() || !FocusRelay.text(n).isNullOrBlank(),
            intercepted = intercepted(sbn.key),
        )
        if (FocusDndRules.shouldRelay(posted, dndActive = true, allowed = FocusDnd.allowed(prefs), ownPackage = packageName)) {
            FocusRelay.post(this, sbn)
        }
    }

    /** A original saiu (lida, apagada pelo app): o repasse sai junto. */
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName != packageName) FocusRelay.cancel(this, sbn.key)
    }

    /** false quando o Não perturbe deixou a notificação passar (ela já tocou sozinha). */
    private fun intercepted(key: String): Boolean {
        val ranking = Ranking()
        val found = runCatching { currentRanking?.getRanking(key, ranking) == true }.getOrDefault(false)
        return !found || !ranking.matchesInterruptionFilter()
    }
}

/** As notificações repassadas: canal próprio, ids estáveis por chave e limpeza ao terminar o foco. */
internal object FocusRelay {
    const val PREFS = "focus_relay"
    private const val KEY_IDS = "ids"
    private const val KEY_CHANNEL_VERSION = "channel_version"
    private const val KEY_CHANNEL_WITH_ACCESS = "channel_with_access"
    private const val CHANNEL_PREFIX = "focus_relay_v"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun manager(context: Context) = context.getSystemService(NotificationManager::class.java)

    fun title(n: Notification): CharSequence? = n.extras?.getCharSequence(Notification.EXTRA_TITLE)
    fun text(n: Notification): CharSequence? =
        n.extras?.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: n.extras?.getCharSequence(Notification.EXTRA_TEXT)

    private fun idFor(key: String): Int = REQUEST_BASE + (key.hashCode() and 0x00FF_FFFF)

    /**
     * O canal só fura o Não perturbe se for criado com o acesso ao Não perturbe já concedido
     * (o sistema ignora setBypassDnd sem ele, e não deixa mudar depois). Se o canal existe e foi
     * criado sem acesso, é recriado com um id novo. Um canal criado com acesso fica como o
     * usuário deixar nas configurações.
     */
    fun ensureChannel(context: Context): String {
        val manager = manager(context)
        val prefs = prefs(context)
        val access = manager.isNotificationPolicyAccessGranted
        var version = prefs.getInt(KEY_CHANNEL_VERSION, 1)
        val existing = manager.getNotificationChannel(CHANNEL_PREFIX + version)
        if (existing != null && (existing.canBypassDnd() || prefs.getBoolean(KEY_CHANNEL_WITH_ACCESS, false) || !access)) {
            return existing.id
        }
        if (existing != null) {
            manager.deleteNotificationChannel(existing.id)
            version++
        }
        val channel = NotificationChannel(CHANNEL_PREFIX + version, context.getString(R.string.relay_channel), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.relay_channel_desc)
            setBypassDnd(true)
        }
        manager.createNotificationChannel(channel)
        prefs.edit {
            putInt(KEY_CHANNEL_VERSION, version)
            putBoolean(KEY_CHANNEL_WITH_ACCESS, access)
        }
        return channel.id
    }

    fun post(context: Context, sbn: StatusBarNotification) {
        // Sem permissão de notificações, o repasse não aparece (e o lint exige a checagem).
        if (!FocusController.hasNotificationPermission(context)) return
        val original = sbn.notification
        val pm = context.packageManager
        val appLabel = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)) }
            .getOrElse { if (it is PackageManager.NameNotFoundException) sbn.packageName else throw it }
        val text = text(original)
        val builder = Notification.Builder(context, ensureChannel(context))
            .setSmallIcon(R.drawable.ic_stat_pulso)
            .setColor(NOTIFICATION_ACCENT_VOLT)
            .setContentTitle(title(original) ?: appLabel)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setSubText(appLabel)
            .setContentIntent(original.contentIntent)
            .setAutoCancel(true)
            .setCategory(original.category)
            .setWhen(original.`when`)
            .setShowWhen(true)
            // Atualizações da mesma notificação seguem o original: tocam de novo só se ele tocaria.
            .setOnlyAlertOnce(original.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
        original.getLargeIcon()?.let(builder::setLargeIcon)
        val id = idFor(sbn.key)
        manager(context).notify(id, builder.build())
        prefs(context).edit { putStringSet(KEY_IDS, ids(context) + id.toString()) }
    }

    fun cancel(context: Context, key: String) {
        val id = idFor(key)
        if (id.toString() !in ids(context)) return
        manager(context).cancel(id)
        prefs(context).edit { putStringSet(KEY_IDS, ids(context) - id.toString()) }
    }

    /** Fim do foco: tira todos os repasses (as originais continuam na gaveta). */
    fun cancelAll(context: Context) {
        val manager = manager(context)
        ids(context).forEach { it.toIntOrNull()?.let(manager::cancel) }
        prefs(context).edit { remove(KEY_IDS) }
    }

    private fun ids(context: Context): Set<String> = prefs(context).getStringSet(KEY_IDS, null).orEmpty().toSet()

    /** Faixa de ids própria, longe dos do foco (42xx) e dos lembretes. */
    private const val REQUEST_BASE = 0x5000_0000
}
