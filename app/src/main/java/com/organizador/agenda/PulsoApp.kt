package com.organizador.agenda

import android.app.Application
import com.organizador.agenda.reminder.TaskReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PulsoApp : Application() {
    @Inject lateinit var reminders: TaskReminderScheduler

    override fun onCreate() {
        super.onCreate()
        // Qualquer mudança nas tarefas (pelo app, pela notificação, pela Captura) rearma o alarme.
        reminders.start()
    }
}
