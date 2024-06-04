package com.planeat.planeat.ui.components.calendar

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
        Content(dataUi = dataUi,
                onPrevClickListener = { startDate ->
                    val finalStartDate = startDate.minusDays(1)
                    updateDate(dataSource.getData(startDate = finalStartDate, lastSelectedDate = dataUi.selectedDate.date))
                },
                onNextClickListener = { endDate ->
                    val finalStartDate = endDate.plusDays(2)
                    updateDate(dataSource.getData(startDate = finalStartDate, lastSelectedDate = dataUi.selectedDate.date))
                }
        ) { date ->
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
    onPrevClickListener: (LocalDate) -> Unit,
    onNextClickListener: (LocalDate) -> Unit,
    onDateClickListener: (CalendarUiModel.Date) -> Unit,
) {
    val scrollState = rememberScrollState()
    var active by remember { mutableStateOf(false) }

    Row(Modifier.horizontalScroll(scrollState)) {
        Log.d("PlanEat", active.toString())
        if (active) {
            if (scrollState.value > 5*scrollState.maxValue/6) {
                LaunchedEffect(Unit) {
                    onNextClickListener(dataUi.endDate.date)
                    scrollState.scrollTo(scrollState.maxValue/2)
                }
            } else if (scrollState.value < scrollState.maxValue/6) {
                LaunchedEffect(Unit) {
                    onPrevClickListener(dataUi.startDate.date)
                    scrollState.scrollTo(scrollState.maxValue/2)
                }
            }
        } else {
            LaunchedEffect(Unit) {
                scrollState.scrollTo(scrollState.maxValue/2)
                Log.d("PlanEat","GO")
                active = true
            }
        }


        dataUi.scrollDates.forEach{ visibleDate ->
            ContentItem(
                date = visibleDate,
                onDateClickListener
            )
        }
    }
}

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