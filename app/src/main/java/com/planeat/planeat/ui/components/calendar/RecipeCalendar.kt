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
import com.example.reply.ui.components.RecipeListItem
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
fun RecipeCalendar(
    modifier: Modifier = Modifier,
    dataSource: CalendarDataSource,
) {
    var data by remember { mutableStateOf(dataSource.getData(lastSelectedDate = dataSource.today)) }
    Column(modifier = modifier.fillMaxSize()) {
        data.visibleDates.forEach{ visibleDate ->
            ContentItem(
                date = visibleDate,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun ContentItem(
    date: CalendarUiModel.Date,
) {
    Text(
        text = date.date.format(
            DateTimeFormatter.ofPattern("EEEE dd MMMM")
        ),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}