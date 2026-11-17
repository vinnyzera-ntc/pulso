package com.organizador.agenda

import android.app.Application
import com.organizador.agenda.focus.FocusDnd
import com.organizador.agenda.reminder.TaskReminderScheduler
import com.organizador.agenda.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PulsoApp : Application() {
    @Inject lateinit var reminders: TaskReminderScheduler
    @Inject lateinit var widget: WidgetUpdater
    @Inject lateinit var focusDnd: FocusDnd

    override fun onCreate() {
        super.onCreate()
        // Qualquer mudança nas tarefas (pelo app, pela notificação, pela Captura) rearma o alarme.
        reminders.start()
        widget.start()
        // Liga/desliga o Não perturbe com as fases do foco (e desfaz o que ficou se o processo morreu).
        focusDnd.start()
    }
}
