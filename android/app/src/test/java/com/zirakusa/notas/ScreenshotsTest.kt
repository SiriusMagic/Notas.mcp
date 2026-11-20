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
import org.junit.Assert.assertEquals
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

    @Test fun editorPanels() {
        val store = SqlNoteStore(ApplicationProvider.getApplicationContext())
        store.save(Note(title = "Hh", body = "## Materiales\nCajetines\nSpray **negro**\n☐ Cinta aislante\n☑ Cable 12\n• Cable 8\n|  Caja  |  breakers  |",
            createdAt = 1_763_500_000_000, updatedAt = 1_763_508_000_000))
        val state = NotesState(store)
        compose.setContent { NotesTheme { NotesApp(state, TasksState(store)) } }
        compose.onNodeWithText("Hh").performClick()
        compose.onNodeWithContentDescription("Compartir").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-note-reading.png")
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Agregar a pantalla principal").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-note-menu.png")
        compose.onNodeWithText("Fondo").performClick()
        compose.onNodeWithContentDescription("Sin fondo").assertExists()
        state.changeOpen { it.copy(color = 5) }
        compose.waitForIdle()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-note-background.png")
        compose.onNodeWithContentDescription("Cerrar").performClick()
        state.editing!!.writing = true
        compose.onNodeWithContentDescription("Dictar").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-note-writing.png")
        compose.onNodeWithContentDescription("Formato").performClick()
        compose.onNodeWithText("H1").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-note-format.png")
        compose.onNodeWithText("H2").performClick()
        compose.onNodeWithContentDescription("Cerrar").performClick()
        compose.onNodeWithContentDescription("Cámara").performClick()
        compose.onNodeWithText("Reconocimiento de texto").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-note-camera.png")
        compose.onNodeWithContentDescription("Cerrar").performClick()
        compose.onNodeWithContentDescription("Dibujo").performClick()
        compose.onNodeWithContentDescription("Marcador").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-note-drawing.png")
    }

    @Test fun settingsMcpDrawerAndWidgets() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = SqlNoteStore(context)
        store.save(Note(title = "Hh", body = "Cajetines\nSpray negro", createdAt = 1, updatedAt = 1_763_500_000_000))
        store.save(Note(title = "Clave", body = "1234", locked = true, createdAt = 2, updatedAt = 1_763_600_000_000))
        store.saveTask(Task(text = "Comprar cajetines", createdAt = 2, updatedAt = 2))
        val state = NotesState(store)
        compose.setContent { NotesTheme { NotesApp(state, TasksState(store)) } }
        compose.onNodeWithContentDescription("Carpetas").performClick()
        compose.onNodeWithText("Añadir widget").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-drawer-more.png")
        compose.onNodeWithText("Añadir widget").performClick()
        compose.onNodeWithText("2×3").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-widgets.png")
        state.page = com.zirakusa.notas.ui.Page.SETTINGS
        compose.onNodeWithText("Modo MCP").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-settings.png")
        compose.onNodeWithText("Modo MCP").performClick()
        compose.onNodeWithText("Redireccionar a Hub").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/notas-mcp.png")

        // The home-screen widgets draw the real notes (never the locked one) and the open tasks.
        val note = com.zirakusa.notas.widget.Widgets.views(context, com.zirakusa.notas.widget.NoteWidget::class.java, store).apply(context, null)
        assertEquals("Hh", note.findViewById<android.widget.TextView>(R.id.title).text.toString())
        val tasks = com.zirakusa.notas.widget.Widgets.views(context, com.zirakusa.notas.widget.TasksWidget::class.java, store).apply(context, null)
        assertEquals(1, tasks.findViewById<android.widget.LinearLayout>(R.id.rows).childCount)
    }
}
