package com.planeat.planeat.ui.components.calendar

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.compose.surfaceContainerLowestLight
import com.planeat.planeat.R
import com.planeat.planeat.data.Agenda
import com.planeat.planeat.data.AgendaDb
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.ui.components.RecipeListItem
import dashedBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun RecipeCalendar(
    goToDetails: (Recipe) -> Unit,
    goToEdition: (Recipe) -> Unit,
    onRecipeDeleted: (Recipe) -> Unit,
    modifier: Modifier = Modifier,
    dataUi: CalendarUiModel,
    updateDate: (CalendarUiModel, Boolean) -> Unit,
) {
    var refresh by remember { mutableIntStateOf(0) }
    Column(modifier = modifier.fillMaxSize()) {
        dataUi.visibleDates.forEach{ visibleDate ->
            ContentItem(
                goToDetails = goToDetails,
                goToEdition = goToEdition,
                onRecipeDeleted = { r ->
                    onRecipeDeleted(r)
                    refresh += 1
                    updateDate(dataUi, false) // Force refresh
                },
                refresh,
                date = visibleDate,
                dataUi = dataUi,
                updateDate = updateDate
            )
        }
    }
}

data class RecipeAgenda (
    val recipe: Recipe,
    val agenda: Agenda
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun ContentItem(
    goToDetails: (Recipe) -> Unit,
    goToEdition: (Recipe) -> Unit,
    onRecipeDeleted: (Recipe) -> Unit,
    refresh: Int,
    date: CalendarUiModel.Date,
    dataUi: CalendarUiModel,
    updateDate: (CalendarUiModel, Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {

        val context = LocalContext.current
        val recipesPlanned = remember {mutableStateOf(listOf<RecipeAgenda>())}

        LaunchedEffect(date.date, refresh) {
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
                    if (recipe != null && recipe.recipeId != 0.toLong()) {
                        val ra = RecipeAgenda(recipe = recipe, agenda = it)
                        recipesPlanned.value += ra
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
            val scope = rememberCoroutineScope()

            recipesPlanned.value.forEach { recipeAgenda ->
                val recipe = recipeAgenda.recipe
                RecipeListItem(
                    recipe = recipe,
                    onRecipeSelected = { r -> goToDetails(r) },
                    onRecipeDeleted = { r -> onRecipeDeleted(r) },
                    onRecipeAdded = {},
                    onEditRecipe = { r -> goToEdition(r) },
                    onPlanRecipe = {},
                    onRemoveFromAgenda = { scope.launch {
                        // TODO better animation?
                        withContext(Dispatchers.IO) {
                            recipesPlanned.value = emptyList()
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
                                val recipeDb = rdb.recipeDao().findById(it.recipeId)
                                if (recipe != recipeDb) {
                                    val ra = RecipeAgenda(recipe = recipeDb, agenda = it)
                                    recipesPlanned.value += ra
                                } else {
                                    adb.agendaDao().delete(it)
                                }
                            }
                            adb.close()
                            rdb.close()
                        }
                    } },
                    agenda = recipeAgenda.agenda,
                    modifier = Modifier.padding(8.dp).shadow(8.dp, shape = MaterialTheme.shapes.medium)
                )
            }

            AddRecipeCard(
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
fun AddRecipeCard(
    modifier: Modifier = Modifier,
    date: CalendarUiModel.Date,
    dataUi: CalendarUiModel,
    updateDate: (CalendarUiModel, Boolean) -> Unit,
) {
    Card(
        modifier = modifier
            .clip(CardDefaults.shape)
            .width((LocalConfiguration.current.screenWidthDp * 0.4f).dp) // Set the width to 40% of the screen
            .height((LocalConfiguration.current.screenWidthDp * 0.41f).dp)
            .dashedBorder(
                width = 2.dp,
                color = Color(0xFF00AF45),
                shape = MaterialTheme.shapes.medium, on = 8.dp, off = 8.dp
            )
            .combinedClickable(
                onClick = {
                    updateDate(dataUi.copy(
                        selectedDate = date,
                        visibleDates = dataUi.visibleDates.map {
                            it.copy(
                                isSelected = it.date.isEqual(date.date)
                            )
                        }
                    ), true)
                }
            )
            .clip(CardDefaults.shape),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        border = null,
        colors = CardDefaults.cardColors(
            containerColor = surfaceContainerLowestLight,
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
                    tint = Color(0xFF00AF45),
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
