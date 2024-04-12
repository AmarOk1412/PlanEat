package com.planeat.planeat.ui.components.calendar

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.planeat.planeat.ui.theme.calendarEndCardGradient
import com.planeat.planeat.ui.theme.calendarNotSelected
import com.planeat.planeat.ui.theme.calendarStartCardGradient
import com.planeat.planeat.ui.theme.calendarTextNotSelected
import com.planeat.planeat.ui.theme.calendarTextSelected
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle



@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun Calendar(
    modifier: Modifier = Modifier,
    dataSource: CalendarDataSource,
    dataUi: CalendarUiModel,
    updateDate: (CalendarUiModel) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Header(
            dataUi = dataUi,
            onPrevClickListener = { startDate ->
                val finalStartDate = startDate.minusDays(1)
                updateDate(dataSource.getData(startDate = finalStartDate, lastSelectedDate = dataUi.selectedDate.date))
            },
            onNextClickListener = { endDate ->
                val finalStartDate = endDate.plusDays(2)
                updateDate(dataSource.getData(startDate = finalStartDate, lastSelectedDate = dataUi.selectedDate.date))
            }
        )
        Content(dataUi = dataUi) { date ->
            updateDate(dataUi.copy(
                selectedDate = date,
                visibleDates = dataUi.visibleDates.map {
                    it.copy(
                        isSelected = it.date.isEqual(date.date)
                    )
                }
            ))
        }
    }
}

@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun Header(
    dataUi: CalendarUiModel,
    onPrevClickListener: (LocalDate) -> Unit,
    onNextClickListener: (LocalDate) -> Unit,
) {
    Row {
        Text(
            text = if (dataUi.selectedDate.isToday) {
                "Today"
            } else {
                dataUi.selectedDate.date.format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                )
            },
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        )
        IconButton(onClick = {
            onPrevClickListener(dataUi.startDate.date)
        }) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "Back"
            )
        }
        IconButton(onClick = {
            onNextClickListener(dataUi.endDate.date)
        }) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Next"
            )
        }
    }
}

@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun Content(
    dataUi: CalendarUiModel,
    onDateClickListener: (CalendarUiModel.Date) -> Unit,
) {
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        dataUi.visibleDates.forEach{ visibleDate ->
            ContentItem(
                date = visibleDate,
                onDateClickListener
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun ContentItem(
    date: CalendarUiModel.Date,
    onClickListener: (CalendarUiModel.Date) -> Unit,
) {

    Card(
        modifier = Modifier
            .width(70.dp)
            .height(70.dp)
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .clickable {
                onClickListener(date)
            }
        ,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (date.isSelected) {
                            listOf(calendarStartCardGradient, calendarEndCardGradient)
                        } else {
                            listOf(calendarNotSelected, calendarNotSelected)
                        }
                    )
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = String.format("%02d", date.date.dayOfMonth),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.titleLarge,
                color = if (date.isSelected) {
                    calendarTextSelected
                } else {
                    calendarTextNotSelected
                }
            )
            Text(
                text = date.day,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.titleMedium,
                color = if (date.isSelected) {
                    calendarTextSelected
                } else {
                    calendarTextNotSelected
                }
            )
        }
    }
}