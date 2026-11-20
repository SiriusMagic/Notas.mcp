package com.zirakusa.notas.ui.screens

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.zirakusa.notas.R
import com.zirakusa.notas.ui.theme.NotesColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** «Recordatorio»: a day, then an hour. [onPick] gets the moment, or null for «Quitar» (when [current] exists). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ReminderPicker(current: Long?, onPick: (Long?) -> Unit, onCancel: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val start = current?.let { Instant.ofEpochMilli(it).atZone(zone) } ?: Instant.now().atZone(zone).plusHours(1)
    val date = rememberDatePickerState(start.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    val time = rememberTimePickerState(start.hour, start.minute, is24Hour = false)
    var day by remember { mutableStateOf<LocalDate?>(null) }
    val clear: @Composable () -> Unit = { if (current != null) TextButton({ onPick(null) }) { Text(stringResource(R.string.remove), color = NotesColors.Danger) } else TextButton(onCancel) { Text(stringResource(R.string.cancel)) } }
    if (day == null) {
        DatePickerDialog(onCancel, confirmButton = {
            TextButton({ day = date.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } }) { Text(stringResource(R.string.next)) }
        }, dismissButton = clear) { DatePicker(date) }
    } else {
        AlertDialog(onCancel, confirmButton = {
            TextButton({ onPick(day!!.atTime(LocalTime.of(time.hour, time.minute)).atZone(zone).toInstant().toEpochMilli()) }) { Text(stringResource(R.string.done)) }
        }, dismissButton = clear, text = { TimePicker(time) })
    }
}
