package com.planeat.planeat.ui.components.calendar

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.planeat.planeat.data.AgendaDb
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.ui.components.RecipeListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


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
        var recipesPlanned = remember {mutableStateOf(listOf<Recipe>())}

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
                val recipesPlannedDb = adb.agendaDao().findByDate(date.date.atTime(12, 0)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli())

                recipesPlannedDb.forEach {
                    val recipe = rdb.recipeDao().findById(it.recipeId)
                    if (recipe !== null) {
                        recipesPlanned.value += recipe
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

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            recipesPlanned.value.forEach { recipe ->
                RecipeListItem(recipe = recipe, modifier = Modifier.padding(8.dp).shadow(8.dp, shape = MaterialTheme.shapes.medium))
            }
        }
    }
}