package com.planeat.planeat.ui.components.calendar

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.planeat.planeat.R
import com.planeat.planeat.data.AgendaDb
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.ui.components.RecipeListItem
import com.planeat.planeat.ui.theme.backgroundCardRecipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun RecipeCalendar(
    modifier: Modifier = Modifier,
    dataSource: CalendarDataSource,
    dataUi: CalendarUiModel,
    updateDate: (CalendarUiModel) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        dataUi.visibleDates.forEach{ visibleDate ->
            ContentItem(
                date = visibleDate,
                dataUi = dataUi,
                updateDate = updateDate
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun ContentItem(
    date: CalendarUiModel.Date,
    dataUi: CalendarUiModel,
    updateDate: (CalendarUiModel) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {

        val context = LocalContext.current
        var recipesPlanned = remember {mutableStateOf(listOf<Recipe>())}

        LaunchedEffect(date.date) {
            recipesPlanned.value = emptyList()
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
                adb.close()
                rdb.close()
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
            modifier = Modifier.horizontalScroll(rememberScrollState()).height(IntrinsicSize.Min)
        ) {
            recipesPlanned.value.forEach { recipe ->
                RecipeListItem(recipe = recipe,
                    onRecipeSelected = {},
                    modifier = Modifier.padding(8.dp).shadow(8.dp, shape = MaterialTheme.shapes.medium))
            }

            addRecipeCard(
                modifier = Modifier.padding(8.dp).shadow(8.dp, shape = MaterialTheme.shapes.medium),
                date = date,
                dataUi = dataUi,
                updateDate = updateDate
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun addRecipeCard(
    modifier: Modifier = Modifier,
    date: CalendarUiModel.Date,
    dataUi: CalendarUiModel,
    updateDate: (CalendarUiModel) -> Unit,
    ) {
    Card(
        modifier = modifier
            .clip(CardDefaults.shape)
            .width((LocalConfiguration.current.screenWidthDp * 0.4f).dp) // Set the width to 40% of the screen
            .height((LocalConfiguration.current.screenWidthDp * 0.55f).dp)
            .combinedClickable(
                onClick = { 
                    updateDate(dataUi.copy(
                        selectedDate = date,
                        visibleDates = dataUi.visibleDates.map {
                            it.copy(
                                isSelected = it.date.isEqual(date.date)
                            )
                        }
                    ))
                }
            )
            .clip(CardDefaults.shape),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundCardRecipe
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color.Green,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .wrapContentSize(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.add),
                    contentDescription = "Add recipe",
                    tint = Color.Green,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "Add recipe",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}
