package com.zirakusa.notas

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.zirakusa.notas.data.Attachment
import com.zirakusa.notas.data.NotesRules
import com.zirakusa.notas.data.TextEdits
import com.zirakusa.notas.ui.EditorPlatform
import com.zirakusa.notas.ui.Editing
import com.zirakusa.notas.ui.NotesState
import kotlinx.coroutines.launch
import java.io.File

/** The editor's link to the phone: camera, gallery, ML Kit, dictation, share sheet, home-screen shortcut. */
@Composable fun rememberPlatform(state: NotesState, e: Editing): EditorPlatform {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val dir = state.attachmentDir
    val pending = remember { arrayOfNulls<File>(1) }
    fun toast(res: Int) = Toast.makeText(context, res, Toast.LENGTH_SHORT).show()

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = pending[0] ?: return@rememberLauncherForActivityResult
        scope.launch { if (ok && NoteFiles.fixPhoto(file)) state.attach(Attachment.Kind.PHOTO, file.name) else file.delete() }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch { NoteFiles.import(context, uri, dir)?.let { state.attach(Attachment.Kind.PHOTO, it) } ?: toast(R.string.photo_failed) }
    }
    val ocr = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val text = runCatching { NoteFiles.recognize(context, uri) }.getOrDefault("")
            if (text.isEmpty()) toast(R.string.no_text_found) else e.tool { TextEdits.insert(it, text) }
        }
    }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val said = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (r.resultCode == Activity.RESULT_OK && !said.isNullOrBlank()) e.tool { TextEdits.insert(it, said) }
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val image = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    return remember(e) {
        object : EditorPlatform {
            override fun takePhoto() {
                val file = NoteFiles.newFile(dir, "jpg").also { pending[0] = it }
                try { camera.launch(NoteFiles.uri(context, file)) } catch (_: ActivityNotFoundException) { toast(R.string.no_camera) }
            }
            override fun pickPhoto() = gallery.launch(image)
            override fun recognizeText() = ocr.launch(image)
            override fun dictate() {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                try { speech.launch(intent) } catch (_: ActivityNotFoundException) { toast(R.string.no_dictation) }
            }
            override fun share() = share(context, e)
            override fun pinToHome() {
                state.saveOpen()
                val note = e.note
                if (note.id == 0L) return toast(R.string.write_first)
                val label = NotesRules.displayTitle(note.copy(title = e.draft.title, body = e.draft.body)).ifEmpty { resources.getString(R.string.app_name) }
                val info = ShortcutInfoCompat.Builder(context, "note-${note.id}").setShortLabel(label.take(24)).setLongLabel(label.take(48))
                    .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW).putExtra(MainActivity.EXTRA_NOTE, note.id))
                    .build()
                if (!ShortcutManagerCompat.requestPinShortcut(context, info, null)) toast(R.string.no_shortcuts)
            }
            override fun compressImages() {
                scope.launch {
                    val saved = NoteFiles.compress(e.attachments.filter { it.kind == Attachment.Kind.PHOTO }.map { File(dir, it.file) })
                    Toast.makeText(context, resources.getString(R.string.compressed, saved / 1024), Toast.LENGTH_SHORT).show()
                }
            }
            override fun saveDrawing(bitmap: Bitmap) { scope.launch { state.attach(Attachment.Kind.DRAWING, NoteFiles.saveDrawing(bitmap, dir)) } }
            override fun allowNotifications() {
                if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

/** «Compartir»: the note as text (without the marks) and its photos and drawings. */
private fun share(context: Context, e: Editing) {
    val text = listOf(e.draft.title, TextEdits.plain(e.draft.body)).filter { it.isNotBlank() }.joinToString("\n\n")
    val uris = e.attachments.map { NoteFiles.uri(context, File(File(context.filesDir, "attachments"), it.file)) }
    val intent = if (uris.isEmpty()) Intent(Intent.ACTION_SEND).setType("text/plain")
    else Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/*").putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    intent.putExtra(Intent.EXTRA_TEXT, text).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, null))
}
