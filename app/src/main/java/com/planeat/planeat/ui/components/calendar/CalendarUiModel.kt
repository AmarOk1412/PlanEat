package com.planeat.planeat.ui.components.calendar

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Created by meyta.taliti on 20/05/23.
 */
data class CalendarUiModel(
    val selectedDate: Date,
    val visibleDates: List<Date>,
    val scrollDates: List<Date>
) {

    val startDate: Date = visibleDates.first()
    val endDate: Date = visibleDates.last()

    data class Date(
        val date: LocalDate,
        val isSelected: Boolean,
        val isToday: Boolean
    ) {
        @RequiresApi(Build.VERSION_CODES.O)
        val day: String = date.format(DateTimeFormatter.ofPattern("E")).replace(".", "")
    }
}