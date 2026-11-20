package com.zirakusa.notas.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.notas.data.NoteStore
import com.zirakusa.notas.data.NotesRules
import com.zirakusa.notas.data.Task

/** «Tareas»: short lines to tick off. [editing] is the task in the «Nueva tarea» / edit dialog. */
class TasksState(private val store: NoteStore, private val clock: () -> Long = System::currentTimeMillis) {
    var tasks by mutableStateOf(store.tasks()); private set
    var editing by mutableStateOf<Task?>(null); private set

    fun visible(query: String) = NotesRules.visibleTasks(tasks, query)

    fun startNew() { editing = Task(text = "") }
    fun edit(task: Task) { editing = task }
    fun cancel() { editing = null }

    /** «Hecho»: keeps the text (one line, at most 500 characters); an empty text deletes the task. */
    fun done(text: String) {
        val task = editing ?: return
        editing = null
        val clean = text.replace('\n', ' ').trim().take(500)
        val now = clock()
        when {
            clean.isEmpty() -> if (task.id != 0L) store.deleteTask(task.id)
            task.id == 0L -> store.saveTask(task.copy(text = clean, createdAt = now, updatedAt = now))
            clean != task.text -> store.saveTask(task.copy(text = clean, updatedAt = now))
        }
        tasks = store.tasks()
    }

    fun toggle(task: Task) { store.saveTask(task.copy(done = !task.done, updatedAt = clock())); tasks = store.tasks() }

    fun delete(task: Task) { editing = null; store.deleteTask(task.id); tasks = store.tasks() }
}
