package org.bookie.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LibraryNumberPickerDialog(
    title: String,
    initialValue: Int?,
    minValue: Int,
    maxValue: Int,
    allowEmpty: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit,
) {
    var digits by remember(title, initialValue) {
        mutableStateOf(initialValue?.toString().orEmpty())
    }
    val selected = digits.toIntOrNull()
    val valid = if (digits.isBlank()) allowEmpty else selected != null && selected in minValue..maxValue
    val maxDigits = maxValue.toString().length

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    selected?.toString() ?: if (digits.isEmpty()) "Sin seleccionar" else digits,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Elegí un valor entre $minValue y $maxValue.")
                (1..9).toList().chunked(3).forEach { rowValues ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowValues.forEach { digit ->
                            OutlinedButton(
                                onClick = { if (digits.length < maxDigits) digits += digit },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) { Text(digit.toString()) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { digits = digits.dropLast(1) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = digits.isNotEmpty(),
                    ) { Text("Borrar") }
                    OutlinedButton(
                        onClick = { if (digits.length < maxDigits) digits += "0" },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text("0") }
                    OutlinedButton(
                        onClick = { digits = "" },
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = allowEmpty && digits.isNotEmpty(),
                    ) { Text("Limpiar") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }, enabled = valid) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryDatePickerDialog(
    title: String,
    initialValue: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialValue.toUtcDateMillis())

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(state.selectedDateMillis?.toIsoDateTime()) },
                enabled = state.selectedDateMillis != null,
            ) { Text("Aceptar") }
        },
        dismissButton = {
            Row {
                if (initialValue != null) {
                    TextButton(onClick = { onConfirm(null) }) { Text("Quitar fecha") }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    ) {
        Column {
            Text(title, modifier = Modifier.padding(start = 24.dp, top = 12.dp))
            DatePicker(state = state)
        }
    }
}

internal fun String?.toUtcDateMillis(): Long? {
    val date = this?.take(10) ?: return null
    if (date.length != 10 || date[4] != '-' || date[7] != '-') return null
    val year = date.substring(0, 4).toIntOrNull() ?: return null
    val month = date.substring(5, 7).toIntOrNull() ?: return null
    val day = date.substring(8, 10).toIntOrNull() ?: return null
    if (year < 1 || month !in 1..12 || day !in 1..daysInMonth(year, month)) return null

    val adjustedYear = year - if (month <= 2) 1 else 0
    val era = adjustedYear / 400
    val yearOfEra = adjustedYear - era * 400
    val adjustedMonth = month + if (month > 2) -3 else 9
    val dayOfYear = (153 * adjustedMonth + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    val epochDays = era.toLong() * 146097 + dayOfEra - 719468
    return epochDays * MILLIS_PER_DAY
}

internal fun Long.toIsoDateTime(): String {
    val epochDays = floorDiv(this, MILLIS_PER_DAY)
    val z = epochDays + 719468
    val era = floorDiv(z, 146097)
    val dayOfEra = (z - era * 146097).toInt()
    val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
    var year = yearOfEra + era.toInt() * 400
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val monthPrime = (5 * dayOfYear + 2) / 153
    val day = dayOfYear - (153 * monthPrime + 2) / 5 + 1
    val month = monthPrime + if (monthPrime < 10) 3 else -9
    if (month <= 2) year++
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-" +
        "${day.toString().padStart(2, '0')}T00:00:00.000Z"
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    2 -> if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

private fun floorDiv(value: Long, divisor: Long): Long {
    val quotient = value / divisor
    return if (value % divisor < 0) quotient - 1 else quotient
}

private const val MILLIS_PER_DAY = 86_400_000L
