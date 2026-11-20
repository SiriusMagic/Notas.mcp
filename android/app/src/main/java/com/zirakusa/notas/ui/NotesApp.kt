package com.zirakusa.notas.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.zirakusa.notas.R
import com.zirakusa.notas.data.NotesRules
import com.zirakusa.notas.data.Section
import com.zirakusa.notas.ui.screens.EditorScreen
import com.zirakusa.notas.ui.screens.DrawerLinks
import com.zirakusa.notas.ui.screens.FolderDrawer
import com.zirakusa.notas.ui.screens.McpScreen
import com.zirakusa.notas.ui.screens.SettingsScreen
import com.zirakusa.notas.ui.screens.WidgetKind
import com.zirakusa.notas.ui.screens.WidgetsScreen
import com.zirakusa.notas.ui.screens.InputDialog
import com.zirakusa.notas.ui.screens.NotesScreen
import com.zirakusa.notas.ui.theme.NotesColors
import kotlinx.coroutines.launch

/** What the app asks of the phone outside the editor (widgets, feedback page, clipboard, Gateway Hub). */
class AppActions(
    val pinWidget: (WidgetKind) -> Unit = {}, val feedback: () -> Unit = {}, val copy: (String) -> Unit = {}, val goToHub: () -> Unit = {},
)

/**
 * Drawer + list, or the editor, and the folder dialogs. [askUnlock] runs the phone's lock and calls its
 * argument when it says yes (opening «Bloqueado»).
 */
@Composable fun NotesApp(
    state: NotesState, tasks: TasksState, askUnlock: (() -> Unit) -> Unit = {}, now: () -> Long = System::currentTimeMillis,
    platform: @Composable (Editing) -> EditorPlatform = { remember { object : EditorPlatform {} } },
    allowNotifications: () -> Unit = {},
    actions: AppActions = AppActions(),
) {
    when (state.page) {
        Page.MAIN -> Unit
        Page.SETTINGS -> return SettingsScreen(state.themeMode, state.accent, state::changeTheme, state::changeAccent,
            onMcp = { state.page = Page.MCP }) { state.page = Page.MAIN }
        Page.MCP -> return McpScreen(actions.copy, actions.goToHub) { state.page = Page.SETTINGS }
        Page.WIDGETS -> return WidgetsScreen(NotesRules.visible(state.notes, Section.All), tasks.tasks,
            { NotesRules.shortDate(it.updatedAt, now()) }, actions.pinWidget) { state.page = Page.MAIN }
    }
    val e = state.editing
    if (e != null) {
        EditorScreen(state, e, platform(e), now())
        return
    }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var naming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Long?>(null) }
    val fixed = mapOf(
        Section.All to stringResource(R.string.all_notes), Section.Uncategorized to stringResource(R.string.uncategorized),
        Section.Locked to stringResource(R.string.locked), Section.Trash to stringResource(R.string.trash),
    )
    ModalNavigationDrawer(
        drawerContent = {
            FolderDrawer(state, { s ->
                state.tab = Tab.NOTES
                if (state.pick(s)) scope.launch { drawer.close() } else askUnlock { state.unlock(); scope.launch { drawer.close() } }
            }, { naming = true }, { deleting = it }, DrawerLinks(
                widgets = { state.page = Page.WIDGETS; scope.launch { drawer.close() } },
                feedback = actions.feedback,
                settings = { state.page = Page.SETTINGS; scope.launch { drawer.close() } },
            ))
        },
        drawerState = drawer,
    ) {
        NotesScreen(state, tasks, state.sectionTitle { fixed.getValue(it) }, { NotesRules.shortDate(it.updatedAt, now()) }, allowNotifications) { scope.launch { drawer.open() } }
    }
    if (naming) NameDialog(onDone = { naming = false; if (it != null) { state.addFolder(it); scope.launch { drawer.close() } } })
    deleting?.let { id ->
        val name = state.folders.firstOrNull { it.id == id }?.name.orEmpty()
        AlertDialog({ deleting = null },
            confirmButton = { TextButton({ deleting = null; state.deleteFolder(id) }) { Text(stringResource(R.string.delete), color = NotesColors.Danger) } },
            dismissButton = { TextButton({ deleting = null }) { Text(stringResource(R.string.cancel)) } },
            title = { Text(stringResource(R.string.delete_folder)) }, text = { Text(stringResource(R.string.delete_folder_info, name)) })
    }
}

@Composable private fun NameDialog(onDone: (String?) -> Unit) =
    InputDialog(stringResource(R.string.new_folder), "", 60, { onDone(null) }, { onDone(it) })
