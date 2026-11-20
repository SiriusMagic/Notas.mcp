package com.zirakusa.notas.ui

import android.graphics.Bitmap

/** What the editor asks of the phone (camera, gallery, dictation, sharing…). Tests use the empty default. */
interface EditorPlatform {
    fun takePhoto() = Unit
    fun pickPhoto() = Unit
    fun recognizeText() = Unit
    fun dictate() = Unit
    fun share() = Unit
    fun pinToHome() = Unit
    fun compressImages() = Unit
    fun saveDrawing(bitmap: Bitmap) = Unit
    /** Before the first reminder: asks to show notifications (Android 13+). */
    fun allowNotifications() = Unit
}
