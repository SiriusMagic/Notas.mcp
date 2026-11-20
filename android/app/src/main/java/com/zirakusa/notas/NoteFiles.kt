package com.zirakusa.notas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Photos and drawings of the notes: private files in `files/attachments`, shared only through [uri] when the user shares. */
object NoteFiles {
    private const val MAX_SIDE = 2048
    private const val SMALL_SIDE = 1600

    fun newFile(dir: File, ext: String) = File(dir, "${UUID.randomUUID()}.$ext")

    fun uri(context: Context, file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    /** A camera photo: turned upright and made at most 2048 px on its longest side. False when it cannot be read. */
    suspend fun fixPhoto(file: File): Boolean = withContext(Dispatchers.IO) {
        val bitmap = decode(file.path, MAX_SIDE) ?: return@withContext false
        write(upright(bitmap, ExifInterface(file.path)), file, 90)
        true
    }

    /** Copies a photo chosen in the gallery into [dir]; returns the new file's name, or null. */
    suspend fun import(context: Context, uri: Uri, dir: File): String? = withContext(Dispatchers.IO) {
        val tmp = newFile(dir, "jpg")
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } } ?: return@withContext null
        }.onFailure { tmp.delete(); return@withContext null }
        if (fixPhoto(tmp)) tmp.name else { tmp.delete(); null }
    }

    suspend fun saveDrawing(bitmap: Bitmap, dir: File): String = withContext(Dispatchers.IO) {
        newFile(dir, "png").also { f -> f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }.name
    }

    /** «Comprimir imágenes»: photos to at most 1600 px and quality 80. Returns the bytes saved. */
    suspend fun compress(files: List<File>): Long = withContext(Dispatchers.IO) {
        files.filter { it.extension == "jpg" && it.exists() }.sumOf { f ->
            val before = f.length()
            val bitmap = decode(f.path, SMALL_SIDE) ?: return@sumOf 0L
            val tmp = File(f.parentFile, f.name + ".tmp")
            write(bitmap, tmp, 80)
            if (tmp.length() < before) { tmp.renameTo(f); before - f.length() } else { tmp.delete(); 0L }
        }
    }

    /** «Reconocimiento de texto» with ML Kit inside the phone (nothing is sent anywhere). */
    suspend fun recognize(context: Context, uri: Uri): String {
        val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try { client.process(InputImage.fromFilePath(context, uri)).await().text.trim() } finally { client.close() }
    }

    private fun decode(path: String, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val scale = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
        return if (scale >= 1f) bitmap else bitmap.scale((bitmap.width * scale).toInt(), (bitmap.height * scale).toInt())
    }

    private fun upright(bitmap: Bitmap, exif: ExifInterface): Bitmap {
        val degrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
    }

    private fun write(bitmap: Bitmap, file: File, quality: Int) = file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
}
