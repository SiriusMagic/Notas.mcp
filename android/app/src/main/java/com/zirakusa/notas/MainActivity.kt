package com.zirakusa.notas

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.zirakusa.notas.data.SqlNoteStore
import com.zirakusa.notas.ui.NotesApp
import com.zirakusa.notas.ui.NotesState
import com.zirakusa.notas.ui.TasksState
import com.zirakusa.notas.ui.theme.NotesTheme

/** A FragmentActivity so the phone's lock prompt (BiometricPrompt) can be shown for «Bloqueado». */
class MainActivity : FragmentActivity() {
    private lateinit var state: NotesState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = SqlNoteStore(this)
        state = NotesState(store)
        val tasks = TasksState(store)
        setContent {
            NotesTheme {
                BackHandler(state.editing != null || state.selecting) { if (state.editing != null) state.closeEditor() else state.clearSelection() }
                NotesApp(state, tasks, askUnlock = { onOpen -> LockGate.ask(this, onOpen) })
            }
        }
    }

    /** Leaving the app keeps what was written and locks «Bloqueado» again. */
    override fun onStop() { super.onStop(); state.lockAgain() }
}
