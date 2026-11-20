package com.zirakusa.notas

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import com.zirakusa.notas.ui.theme.NotesColors
import com.zirakusa.notas.ui.theme.Skins
import androidx.core.net.toUri
import com.zirakusa.notas.mcp.McpMode
import com.zirakusa.notas.ui.AppActions
import com.zirakusa.notas.ui.Page
import com.zirakusa.notas.ui.screens.WidgetKind
import com.zirakusa.notas.widget.NoteSmallWidget
import com.zirakusa.notas.widget.NoteWidget
import com.zirakusa.notas.widget.NotesListWidget
import com.zirakusa.notas.widget.TasksWidget
import com.zirakusa.notas.widget.Widgets
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.zirakusa.notas.data.SqlNoteStore
import com.zirakusa.notas.ui.NotesApp
import com.zirakusa.notas.ui.NotesState
import com.zirakusa.notas.ui.Tab
import com.zirakusa.notas.ui.TasksState
import com.zirakusa.notas.ui.theme.NotesTheme

/** A FragmentActivity so the phone's lock prompt (BiometricPrompt) can be shown for «Bloqueado». */
class MainActivity : FragmentActivity() {
    private lateinit var state: NotesState
    private lateinit var tasks: TasksState
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = SqlNoteStore(this)
        val alarms = AlarmReminders(this)
        val widgets = { Widgets.refresh(this) }
        state = NotesState(store, reminders = alarms, onChange = widgets)
        tasks = TasksState(store, reminders = alarms, onChange = widgets)
        McpMode.load(this)
        McpMode.onChanged = { state.refresh(); tasks.refresh(); widgets() }
        if (savedInstanceState == null) handle(intent)
        setContent {
            NotesTheme(state.themeMode, state.accent) {
                // Status and navigation bar icons dark on light screens (the app's theme, or a note's light «Fondo»).
                val lightBars = state.editing?.let { Skins.of(it.note.color).light } ?: NotesColors.isLight
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).run {
                        isAppearanceLightStatusBars = lightBars; isAppearanceLightNavigationBars = lightBars
                    }
                }
                BackHandler(state.editing != null || state.selecting || state.page != Page.MAIN) {
                    when {
                        state.editing != null -> state.closeEditor()
                        state.selecting -> state.clearSelection()
                        state.page == Page.MCP -> state.page = Page.SETTINGS
                        else -> state.page = Page.MAIN
                    }
                }
                NotesApp(state, tasks, askUnlock = { onOpen -> LockGate.ask(this, onOpen) }, platform = { rememberPlatform(state, it) },
                    allowNotifications = { if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    actions = AppActions(pinWidget = ::pinWidget, feedback = ::feedback, copy = ::copy, goToHub = ::goToHub))
            }
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handle(intent) }

    /** A home-screen shortcut or a reminder opens its note (a locked one asks for the phone's lock first), or «Tareas». */
    private fun handle(intent: Intent) {
        if (intent.hasExtra(EXTRA_TASKS)) {
            state.closeEditor(); state.page = Page.MAIN; state.tab = Tab.TASKS
            tasks.refresh()
            if (intent.getBooleanExtra(EXTRA_NEW_TASK, false)) tasks.startNew()
            return
        }
        val id = intent.getLongExtra(EXTRA_NOTE, 0)
        if (id == 0L) return
        state.closeEditor(); state.page = Page.MAIN; state.tab = Tab.NOTES
        if (!state.openById(id) && state.notes.any { it.id == id && it.locked && it.deletedAt == null }) {
            LockGate.ask(this) { state.unlock(); state.openById(id) }
        }
    }

    /** Leaving the app keeps what was written and locks «Bloqueado» again. */
    override fun onStop() { super.onStop(); state.lockAgain() }

    /** Back from the home screen: a task may have been ticked in the widget. */
    override fun onResume() { super.onResume(); tasks.refresh() }

    private fun pinWidget(kind: WidgetKind) {
        val cls = when (kind) {
            WidgetKind.NOTE -> NoteWidget::class.java; WidgetKind.NOTE_SMALL -> NoteSmallWidget::class.java
            WidgetKind.LIST -> NotesListWidget::class.java; WidgetKind.TASKS -> TasksWidget::class.java
        }
        if (!Widgets.pin(this, cls)) Toast.makeText(this, R.string.widget_add_failed, Toast.LENGTH_LONG).show()
    }

    /** «Comentarios»: the project's issues page in the browser (the app itself sends nothing). */
    private fun feedback() {
        try { startActivity(Intent(Intent.ACTION_VIEW, FEEDBACK_URL.toUri())) } catch (_: ActivityNotFoundException) { }
    }

    private fun copy(link: String) {
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Notas MCP", link))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    /** «Ir al Hub»: copies the hub's link (in case it must be pasted) and opens the Gateway Hub app. */
    private fun goToHub() {
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Notas › Gateway Hub", McpMode.hubLink()))
        val open = packageManager.getLaunchIntentForPackage("com.zirakusa.gatewayhub")
        if (open != null) startActivity(open) else Toast.makeText(this, R.string.mcp_paste_in_hub, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val EXTRA_NOTE = "note_id"
        const val EXTRA_TASKS = "tasks"
        const val EXTRA_NEW_TASK = "new_task"
        const val FEEDBACK_URL = "https://github.com/SiriusMagic/Notas.mcp/issues"
    }
}
