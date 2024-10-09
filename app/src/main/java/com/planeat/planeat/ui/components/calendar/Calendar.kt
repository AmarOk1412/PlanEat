package com.planeat.planeat.ui.components.calendar

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.compose.onPrimaryContainerLight
import com.example.compose.onSurfaceLight
import com.example.compose.primaryContainerLight
import com.example.compose.surfaceContainerLowestLight
import com.planeat.planeat.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun Calendar(
    modifier: Modifier = Modifier,
    dataSource: CalendarDataSource,
    dataUi: CalendarUiModel,
    updateDate: (CalendarUiModel, Boolean) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Header(
            dataUi = dataUi,
            onPrevClickListener = { startDate ->
                val finalStartDate = startDate.minusDays(7)
                updateDate(dataSource.getData(startDate = finalStartDate, lastSelectedDate = dataUi.selectedDate.date), false)
            },
            onNextClickListener = { endDate ->
                val finalStartDate = endDate.plusDays(1)
                updateDate(dataSource.getData(startDate = finalStartDate, lastSelectedDate = dataUi.selectedDate.date), false)
            }
        )
        Content(dataUi = dataUi,
                onPrevClickListener = { startDate ->
                    val finalStartDate = startDate.minusDays(7)
                    updateDate(dataSource.getData(startDate = finalStartDate, lastSelectedDate = dataUi.selectedDate.date), false)
                },
                onNextClickListener = { endDate ->
                    val finalStartDate = endDate.plusDays(1)
                    updateDate(dataSource.getData(startDate = finalStartDate, lastSelectedDate = dataUi.selectedDate.date), false)
                }
        ) { date ->
            updateDate(dataUi.copy(
                selectedDate = date,
                visibleDates = dataUi.visibleDates.map {
                    it.copy(
                        isSelected = it.date.isEqual(date.date)
                    )
                }
            ), false)
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
    Row (modifier = Modifier.padding(start=16.dp)) {
        Text(
            text = if (dataUi.selectedDate.isToday) {
                stringResource(R.string.today)
            } else {
                dataUi.selectedDate.date.format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                )
            },
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        )
        IconButton(onClick = {
            onPrevClickListener(dataUi.startDate.date)
        }) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = stringResource(R.string.back)
            )
        }
        IconButton(onClick = {
            onNextClickListener(dataUi.endDate.date)
        }) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.next)
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

    Row(Modifier.horizontalScroll(scrollState).padding(top = 12.dp)) {
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
                active = true
            }
        }


        dataUi.scrollDates.forEach{ visibleDate ->
            ContentItem(
                dataUi = dataUi,
                date = visibleDate,
                onDateClickListener
            )
        }
    }
}

@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun ContentItem(
    dataUi: CalendarUiModel,
    date: CalendarUiModel.Date,
    onClickListener: (CalendarUiModel.Date) -> Unit,
) {
    var contentColor by remember { mutableStateOf(Color.White) }
    var containerColor by remember { mutableStateOf(Color.White) }

    LaunchedEffect(dataUi) {
        if (date == dataUi.selectedDate) {
            contentColor = onPrimaryContainerLight
            containerColor = primaryContainerLight
        } else {
            contentColor = onSurfaceLight
            containerColor = surfaceContainerLowestLight
        }
    }

    Button(
        modifier = Modifier
            .width(70.dp)
            .height(70.dp)
            .padding(horizontal = 4.dp),
        onClick = {
            onClickListener(date)
        },
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp
        ),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = String.format("%02d", date.date.dayOfMonth),
                fontSize = with(LocalDensity.current) {
                    22.dp.toSp()
                }
            )
            Text(
                text = date.day,
                fontSize = with(LocalDensity.current) {
                    14.dp.toSp()
                }
            )
        }
    }
}