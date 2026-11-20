package com.zirakusa.notas.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.zirakusa.notas.MainActivity
import com.zirakusa.notas.R
import com.zirakusa.notas.data.Note
import com.zirakusa.notas.data.NoteStore
import com.zirakusa.notas.data.NotesRules
import com.zirakusa.notas.data.Section
import com.zirakusa.notas.data.SqlNoteStore
import com.zirakusa.notas.data.TextEdits

/**
 * The home-screen widgets: «Nota» (4×2 and 2×2: the pinned or newest note), «Notas» (4×2: the latest three) and
 * «Tareas» (2×3: open tasks, a ring to tick one, + to add). Locked notes never appear. Redrawn whenever notes change.
 */
object Widgets {
    val kinds = listOf(NoteWidget::class.java, NoteSmallWidget::class.java, NotesListWidget::class.java, TasksWidget::class.java)

    /** Redraws every widget on the home screen (after any change in the app or through MCP). */
    fun refresh(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val store by lazy { SqlNoteStore(context.applicationContext) }
        kinds.forEach { kind ->
            val ids = manager.getAppWidgetIds(ComponentName(context, kind))
            if (ids.isNotEmpty()) ids.forEach { manager.updateAppWidget(it, views(context, kind, store)) }
        }
    }

    /** Puts a widget of [kind] on the home screen (the launcher asks the user). False when the launcher cannot. */
    fun pin(context: Context, kind: Class<*>): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        return manager.isRequestPinAppWidgetSupported && manager.requestPinAppWidget(ComponentName(context, kind), null, null)
    }

    fun views(context: Context, kind: Class<*>, store: NoteStore): RemoteViews = when (kind) {
        NoteWidget::class.java -> note(context, store, R.layout.widget_note)
        NoteSmallWidget::class.java -> note(context, store, R.layout.widget_note_small)
        NotesListWidget::class.java -> list(context, store)
        else -> tasks(context, store)
    }

    private fun visible(store: NoteStore) = NotesRules.visible(store.notes(), Section.All)

    private fun note(context: Context, store: NoteStore, layout: Int) = RemoteViews(context.packageName, layout).apply {
        val n = visible(store).firstOrNull()
        setTextViewText(R.id.title, n?.let { TextEdits.plain(NotesRules.displayTitle(it)) } ?: context.getString(R.string.widget_empty))
        setTextViewText(R.id.preview, n?.let { TextEdits.plain(NotesRules.preview(it)) }.orEmpty())
        setTextViewText(R.id.date, n?.let { NotesRules.shortDate(it.updatedAt, System.currentTimeMillis()) }.orEmpty())
        setOnClickPendingIntent(R.id.root, open(context, n))
    }

    private fun list(context: Context, store: NoteStore) = RemoteViews(context.packageName, R.layout.widget_list).apply {
        val notes = visible(store).take(3)
        removeAllViews(R.id.rows)
        notes.forEach { n ->
            addView(R.id.rows, RemoteViews(context.packageName, R.layout.widget_list_row).apply {
                setTextViewText(R.id.title, TextEdits.plain(NotesRules.displayTitle(n)))
                setTextViewText(R.id.preview, NotesRules.shortDate(n.updatedAt, System.currentTimeMillis()) + "  " +
                    TextEdits.plain(NotesRules.preview(n)).replace('\n', ' '))
                setOnClickPendingIntent(R.id.row, open(context, n))
            })
        }
        setViewVisibility(R.id.empty, if (notes.isEmpty()) View.VISIBLE else View.GONE)
        setOnClickPendingIntent(R.id.header, open(context, null))
    }

    private fun tasks(context: Context, store: NoteStore) = RemoteViews(context.packageName, R.layout.widget_tasks).apply {
        val open = NotesRules.visibleTasks(store.tasks()).filter { !it.done }.take(4)
        removeAllViews(R.id.rows)
        open.forEach { t ->
            addView(R.id.rows, RemoteViews(context.packageName, R.layout.widget_task_row).apply {
                setTextViewText(R.id.text, t.text)
                setOnClickPendingIntent(R.id.tick, PendingIntent.getBroadcast(context, 10_000 + t.id.toInt(),
                    Intent(context, TasksWidget::class.java).setAction(TasksWidget.TICK).putExtra(TasksWidget.ID, t.id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                setOnClickPendingIntent(R.id.text, activity(context, 3, Intent().putExtra(MainActivity.EXTRA_TASKS, 0L)))
            })
        }
        setViewVisibility(R.id.empty, if (open.isEmpty()) View.VISIBLE else View.GONE)
        setOnClickPendingIntent(R.id.header, activity(context, 3, Intent().putExtra(MainActivity.EXTRA_TASKS, 0L)))
        setOnClickPendingIntent(R.id.add, activity(context, 4, Intent().putExtra(MainActivity.EXTRA_TASKS, 0L).putExtra(MainActivity.EXTRA_NEW_TASK, true)))
    }

    private fun open(context: Context, note: Note?) =
        if (note == null) activity(context, 1, Intent()) else activity(context, 100 + note.id.toInt(), Intent().putExtra(MainActivity.EXTRA_NOTE, note.id))

    private fun activity(context: Context, code: Int, extras: Intent): PendingIntent = PendingIntent.getActivity(context, code,
        Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW).putExtras(extras)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

open class NotesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val store = SqlNoteStore(context.applicationContext)
        ids.forEach { manager.updateAppWidget(it, Widgets.views(context, javaClass, store)) }
    }
}

class NoteWidget : NotesWidgetProvider()
class NoteSmallWidget : NotesWidgetProvider()
class NotesListWidget : NotesWidgetProvider()

/** «Tareas»: a tap on a ring marks that task done right from the home screen. */
class TasksWidget : NotesWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TICK) {
            val store = SqlNoteStore(context.applicationContext)
            val id = intent.getLongExtra(ID, 0)
            store.tasks().firstOrNull { it.id == id }?.let { store.saveTask(it.copy(done = true, updatedAt = System.currentTimeMillis())) }
            Widgets.refresh(context)
        } else super.onReceive(context, intent)
    }

    companion object {
        const val TICK = "com.zirakusa.notas.widget.TICK"
        const val ID = "id"
    }
}
