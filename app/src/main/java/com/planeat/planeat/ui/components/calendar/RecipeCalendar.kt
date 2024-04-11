package com.planeat.planeat.ui.components.calendar

import android.os.Build
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.reply.ui.components.RecipeListItem
import com.planeat.planeat.data.AgendaDb
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.ui.theme.calendarEndCardGradient
import com.planeat.planeat.ui.theme.calendarNotSelected
import com.planeat.planeat.ui.theme.calendarStartCardGradient
import com.planeat.planeat.ui.theme.calendarTextNotSelected
import com.planeat.planeat.ui.theme.calendarTextSelected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset
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
    Column(modifier = Modifier.fillMaxSize()) {

        val context = LocalContext.current
        var txt = remember { mutableStateOf("") } // Add this line to store the text value

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val adb = Room.databaseBuilder(
                    context,
                    AgendaDb::class.java, "AgendaDb"
                ).build()
                val rdb = Room.databaseBuilder(
                    context,
                    RecipesDb::class.java, "RecipesDb"
                ).build()
                val recipesPlanned = adb.agendaDao().findByDate(date.date.atTime(12, 0)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli())

                recipesPlanned.forEach {
                    val recipe = rdb.recipeDao().findById(it.recipeId)
                    if (recipe !== null) {
                        val title = recipe.title
                        if (!title.isEmpty()) {
                            txt.value += "$title \n"
                        }
                    } else {
                        adb.agendaDao().delete(it)
                    }
                }
            }
        }

        Text(
            text = date.date.format(
                DateTimeFormatter.ofPattern("EEEE dd MMMM")
            ),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = txt.value,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}