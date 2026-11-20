package com.zirakusa.notas.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.notas.data.NoteStore
import com.zirakusa.notas.data.NotesRules
import com.zirakusa.notas.data.Task

/** «Tareas»: short lines to tick off. [editing] is the task in the «Nueva tarea» / edit dialog. */
class TasksState(
    private val store: NoteStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val reminders: ReminderScheduler = object : ReminderScheduler {},
) {
    var tasks by mutableStateOf(store.tasks()); private set
    var editing by mutableStateOf<Task?>(null); private set

    fun visible(query: String) = NotesRules.visibleTasks(tasks, query)

    fun startNew() { editing = Task(text = "") }
    fun edit(task: Task) { editing = task }
    fun cancel() { editing = null }

    /**
     * «Hecho»: keeps the text (one line, at most 500 characters) and the reminder ([remindAt] null = none, a past
     * time is dropped); an empty text deletes the task.
     */
    fun done(text: String, remindAt: Long? = editing?.remindAt) {
        val task = editing ?: return
        editing = null
        val clean = text.replace('\n', ' ').trim().take(500)
        val now = clock()
        val remind = remindAt?.takeIf { it > now }
        if (clean.isEmpty()) {
            if (task.id != 0L) { reminders.cancel(TASK, task.id); store.deleteTask(task.id) }
        } else {
            val saved = task.copy(text = clean, remindAt = remind, createdAt = task.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = if (clean != task.text || task.id == 0L) now else task.updatedAt)
            val id = store.saveTask(saved)
            if (remind != null && !saved.done) reminders.set(TASK, id, remind, clean) else reminders.cancel(TASK, id)
        }
        tasks = store.tasks()
    }

    fun toggle(task: Task) {
        val next = task.copy(done = !task.done, updatedAt = clock())
        store.saveTask(next)
        if (next.done) reminders.cancel(TASK, task.id) else next.remindAt?.takeIf { it > clock() }?.let { reminders.set(TASK, task.id, it, task.text) }
        tasks = store.tasks()
    }

    fun delete(task: Task) { editing = null; reminders.cancel(TASK, task.id); store.deleteTask(task.id); tasks = store.tasks() }

    /** Reminders still to come, to set again after the phone restarts. */
    fun upcomingReminders(): List<Task> = store.tasks().filter { !it.done && (it.remindAt ?: 0) > clock() }

    companion object { const val TASK = "task" }
}
