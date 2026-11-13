package com.organizador.agenda.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.organizador.agenda.MainActivity
import com.organizador.agenda.R
import com.organizador.agenda.domain.reminder.DueTask
import com.organizador.agenda.focus.FocusController
import com.organizador.agenda.ui.tasks.TaskUiMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Notificação de lembrete: uma por tarefa, com "Concluir" e "Adiar". Tocar abre a tarefa. */
@Singleton
class ReminderNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.reminder_channel_desc)
            },
        )
    }

    fun show(due: DueTask, now: LocalDateTime) {
        if (!FocusController.hasNotificationPermission(context)) return
        val task = due.task
        val id = task.id
        val whenText = task.dueDate?.let { TaskUiMapper.whenLabel(it, task.dueTime, now.toLocalDate()) }.orEmpty()
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_pulso)
            .setColor(ACCENT_VOLT) // lembrete = papel de tempo (volt)
            .setContentTitle(task.title)
            .setContentText(whenText)
            .setContentIntent(openTask(id))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, context.getString(R.string.reminder_action_done), action(ReminderReceiver.ACTION_DONE, id, due.latestReminderId, 0))
            .addAction(0, context.getString(R.string.reminder_action_snooze_short), action(ReminderReceiver.ACTION_SNOOZE, id, due.latestReminderId, SNOOZE_SHORT))
            .addAction(0, context.getString(R.string.reminder_action_snooze_long), action(ReminderReceiver.ACTION_SNOOZE, id, due.latestReminderId, SNOOZE_LONG))
        if (task.notes.isNotBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText("$whenText\n${task.notes.trim()}"))
        }
        manager.notify(TAG, id.toInt(), builder.build())
    }

    fun cancel(taskId: Long) = manager.cancel(TAG, taskId.toInt())

    /** Some com as notificações de tarefas que não estão em [keep] (concluídas ou excluídas). */
    fun cancelAllExcept(keep: Set<Long>) {
        manager.activeNotifications
            .filter { it.tag == TAG && it.id.toLong() !in keep }
            .forEach { manager.cancel(TAG, it.id) }
    }

    private fun openTask(taskId: Long): PendingIntent = PendingIntent.getActivity(
        context, taskId.toInt(),
        Intent(context, MainActivity::class.java)
            .setData(taskUri(taskId))
            .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_TASK)
            .putExtra(MainActivity.EXTRA_TASK_ID, taskId)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun action(action: String, taskId: Long, reminderId: Long, minutes: Int): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        // A URI deixa cada (tarefa, ação, adiamento) com o seu PendingIntent.
        Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .setData(taskUri(taskId).buildUpon().appendPath(minutes.toString()).build())
            .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            .putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            .putExtra(ReminderReceiver.EXTRA_MINUTES, minutes),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun taskUri(taskId: Long): Uri = Uri.parse("pulso://tarefa/$taskId")

    companion object {
        const val CHANNEL = "task_reminders"
        private const val TAG = "tarefa"
        const val SNOOZE_SHORT = 15
        const val SNOOZE_LONG = 60

        /** Volt do tema escuro (#2F95F5): a notificação é do sistema, fora do tema Compose. */
        const val ACCENT_VOLT = 0xFF2F95F5.toInt()
    }
}
