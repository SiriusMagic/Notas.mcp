package com.zirakusa.notas

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.notas.data.Note
import com.zirakusa.notas.data.SqlNoteStore
import com.zirakusa.notas.ui.NotesApp
import com.zirakusa.notas.ui.NotesState
import com.zirakusa.notas.ui.TasksState
import com.zirakusa.notas.data.Task
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.zirakusa.notas.ui.theme.NotesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun gridDrawerAndEditor() {
        val store = SqlNoteStore(ApplicationProvider.getApplicationContext())
        listOf("Hh" to "Cajetines\nSpray negro", "" to "Cierra Taladro Escalera", "Guayaquil" to "Yuanshi", "Fidelio" to "151 metros\n30 metros de blanco")
            .forEachIndexed { i, (t, b) -> store.save(Note(title = t, body = b, createdAt = i + 1L, updatedAt = 1_763_000_000_000 + i)) }
        store.saveTask(Task(text = "Comprar cajetines", createdAt = 2, updatedAt = 2))
        store.saveTask(Task(text = "Llamar al doctor", done = true, createdAt = 1, updatedAt = 3))
        val state = NotesState(store)
        val tasks = TasksState(store)
        compose.setContent { NotesTheme { NotesApp(state, tasks) } }
        compose.onNodeWithText("Guayaquil").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-grid.png")
        compose.onNodeWithText("Guayaquil").performTouchInput { longClick() }
        compose.onNodeWithText("1 elemento seleccionado").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-selection.png")
        compose.onNodeWithContentDescription("Fijar").performClick()
        compose.onNodeWithText("Tareas").performClick()
        compose.onNodeWithText("Comprar cajetines").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-tasks.png")
        compose.onNodeWithContentDescription("Nueva tarea").performClick()
        compose.onNodeWithText("Hecho").assertExists()
        compose.onNode(androidx.compose.ui.test.isDialog()).captureRoboImage("build/screenshots/notas-new-task.png")
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Notas").performClick()
        compose.onNodeWithContentDescription("Carpetas").performClick()
        compose.onNodeWithText("Todas las notas").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-drawer.png")
        compose.onNodeWithText("Todas las notas").performClick()
        compose.onNodeWithText("Fidelio").performClick()
        compose.onNodeWithText("151 metros\n30 metros de blanco").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-editor.png")
    }
}
