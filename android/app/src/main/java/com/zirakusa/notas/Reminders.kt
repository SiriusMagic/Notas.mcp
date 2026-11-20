package com.zirakusa.notas

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zirakusa.notas.data.SqlNoteStore
import com.zirakusa.notas.ui.NotesState
import com.zirakusa.notas.ui.ReminderScheduler
import com.zirakusa.notas.ui.TasksState

/** Reminders of notes and tasks: one alarm each; when it rings, a notification that opens the note (or «Tareas»). */
class AlarmReminders(private val context: Context) : ReminderScheduler {
    override fun set(kind: String, id: Long, at: Long, text: String) {
        context.getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(kind, id, text))
    }

    override fun cancel(kind: String, id: Long) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pending(kind, id, ""))
    }

    private fun pending(kind: String, id: Long, text: String): PendingIntent = PendingIntent.getBroadcast(
        context, requestCode(kind, id),
        Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_KIND, kind).putExtra(EXTRA_ID, id).putExtra(EXTRA_TEXT, text),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_ID = "id"
        const val EXTRA_TEXT = "text"
        const val CHANNEL = "reminders"
        fun requestCode(kind: String, id: Long) = (if (kind == NotesState.NOTE) 0 else 1) + (id * 2).toInt()
    }
}

/** The alarm rang: shows the reminder (the notes app's only notifications). */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(AlarmReminders.EXTRA_KIND) ?: return
        val id = intent.getLongExtra(AlarmReminders.EXTRA_ID, 0)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(NotificationChannel(AlarmReminders.CHANNEL, context.getString(R.string.reminders), NotificationManager.IMPORTANCE_HIGH))
        val open = Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(if (kind == NotesState.NOTE) MainActivity.EXTRA_NOTE else MainActivity.EXTRA_TASKS, id)
        val code = AlarmReminders.requestCode(kind, id)
        val notification = NotificationCompat.Builder(context, AlarmReminders.CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(if (kind == NotesState.NOTE) R.string.reminder_note else R.string.reminder_task))
            .setContentText(intent.getStringExtra(AlarmReminders.EXTRA_TEXT).orEmpty())
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(context, code, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) manager.notify(code, notification)
    }
}

/** Alarms are forgotten when the phone restarts: sets again the reminders still to come. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val store = SqlNoteStore(context)
        val alarms = AlarmReminders(context)
        NotesState(store).upcomingReminders().forEach { alarms.set(NotesState.NOTE, it.id, it.remindAt!!, it.title.ifBlank { it.body.take(60) }) }
        TasksState(store).upcomingReminders().forEach { alarms.set(TasksState.TASK, it.id, it.remindAt!!, it.text) }
    }
}
