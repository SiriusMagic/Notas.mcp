package com.zirakusa.notas

import android.Manifest
import android.content.Intent
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
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = SqlNoteStore(this)
        val alarms = AlarmReminders(this)
        state = NotesState(store, reminders = alarms)
        val tasks = TasksState(store, reminders = alarms)
        if (savedInstanceState == null) handle(intent)
        setContent {
            NotesTheme {
                BackHandler(state.editing != null || state.selecting) { if (state.editing != null) state.closeEditor() else state.clearSelection() }
                NotesApp(state, tasks, askUnlock = { onOpen -> LockGate.ask(this, onOpen) }, platform = { rememberPlatform(state, it) },
                    allowNotifications = { if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS) })
            }
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handle(intent) }

    /** A home-screen shortcut or a reminder opens its note (a locked one asks for the phone's lock first), or «Tareas». */
    private fun handle(intent: Intent) {
        if (intent.hasExtra(EXTRA_TASKS)) { state.closeEditor(); state.tab = Tab.TASKS; return }
        val id = intent.getLongExtra(EXTRA_NOTE, 0)
        if (id == 0L) return
        state.closeEditor(); state.tab = Tab.NOTES
        if (!state.openById(id) && state.notes.any { it.id == id && it.locked && it.deletedAt == null }) {
            LockGate.ask(this) { state.unlock(); state.openById(id) }
        }
    }

    /** Leaving the app keeps what was written and locks «Bloqueado» again. */
    override fun onStop() { super.onStop(); state.lockAgain() }

    companion object {
        const val EXTRA_NOTE = "note_id"
        const val EXTRA_TASKS = "tasks"
    }
}
