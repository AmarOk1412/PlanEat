package com.planeat.planeat.ui.components.calendar

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.compose.onPrimaryContainerLight
import com.example.compose.outlineVariantLight
import com.example.compose.primaryContainerLight
import com.example.compose.surfaceVariantLight
import com.planeat.planeat.R
import com.planeat.planeat.data.Agenda
import com.planeat.planeat.data.AgendaDb
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.ui.AppModel
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
    model: AppModel,
    goToDetails: (Recipe) -> Unit,
    goToEdition: (Recipe) -> Unit,
    onRecipeDeleted: (Recipe) -> Unit,
    modifier: Modifier = Modifier,
    dataUi: CalendarUiModel,
    updateDate: (CalendarUiModel, Boolean) -> Unit,
) {
    var refresh = remember { mutableStateOf(0) }
    model.onAgendaChanged.value = {
        updateDate(dataUi, false) // Force refresh
        refresh.value++
    }
    Column(modifier = modifier.fillMaxSize()) {
        dataUi.visibleDates.forEach{ visibleDate ->
            ContentItem(
                model = model,
                goToDetails = goToDetails,
                goToEdition = goToEdition,
                onRecipeDeleted = { r ->
                    onRecipeDeleted(r)
                    updateDate(dataUi, false) // Force refresh
                },
                date = visibleDate,
                dataUi = dataUi,
                refresh = refresh.value,
                updateDate = updateDate
            )
        }
    }
}

data class RecipeAgenda (
    val recipe: Recipe,
    val agenda: Agenda
)

@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun ContentItem(
    model: AppModel,
    goToDetails: (Recipe) -> Unit,
    goToEdition: (Recipe) -> Unit,
    onRecipeDeleted: (Recipe) -> Unit,
    refresh: Int,
    date: CalendarUiModel.Date,
    dataUi: CalendarUiModel,
    updateDate: (CalendarUiModel, Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(top=32.dp)) {

        val context = LocalContext.current
        val recipesPlanned = remember {mutableStateOf(listOf<RecipeAgenda>())}

        LaunchedEffect(date.date, refresh) {
            recipesPlanned.value = emptyList()
            withContext(Dispatchers.IO) {
                val rdb = RecipesDb.getDatabase(context)
                val adb = AgendaDb.getDatabase(context)
                val d = date.date.atTime(12, 0)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

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
            }
        }

        Text(
            text = date.date.format(
                DateTimeFormatter.ofPattern("EEEE dd MMMM")
            ),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 12.dp, start = 16.dp)
        )

        val scope = rememberCoroutineScope()
        if (recipesPlanned.value.isEmpty()) {
            AddRecipeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(64.dp),
                date = date,
                dataUi = dataUi,
                updateDate = updateDate,
                showLabel = true
            )
        } else {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Spacer(modifier = Modifier.width(8.dp))

                recipesPlanned.value.forEach { recipeAgenda ->
                    val recipe = recipeAgenda.recipe
                    RecipeListItem(
                        model = model,
                        recipe = recipe,
                        onRecipeSelected = { r -> goToDetails(r) },
                        onRecipeDeleted = { r -> onRecipeDeleted(r) },
                        onEditRecipe = { r -> goToEdition(r) },
                        onPlanRecipe = {},
                        onRemoveFromAgenda = { recipeId ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val newRecipesPlanned: MutableList<RecipeAgenda> =
                                        mutableListOf()
                                    val rdb = RecipesDb.getDatabase(context)
                                    val adb = AgendaDb.getDatabase(context)
                                    val recipesPlannedDb = adb.agendaDao().findByDate(
                                        date.date.atTime(12, 0)
                                            .toInstant(ZoneOffset.UTC)
                                            .toEpochMilli()
                                    )

                                    recipesPlannedDb.forEach {
                                        if (recipeId == it.recipeId) {
                                            model.unplanify(it)
                                        } else {
                                            val r = rdb.recipeDao().findById(it.recipeId)
                                            if (r != null)
                                                newRecipesPlanned += RecipeAgenda(
                                                    recipe = r,
                                                    agenda = it
                                                )
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        recipesPlanned.value = newRecipesPlanned
                                    }
                                }
                            }
                        },
                        agenda = recipeAgenda.agenda,
                        modifier = Modifier
                            .width((LocalConfiguration.current.screenWidthDp * 0.70f).dp)
                    )
                }

                AddRecipeCard(
                    modifier = Modifier
                        .width((LocalConfiguration.current.screenWidthDp * 0.25f).dp)
                        .height((LocalConfiguration.current.screenHeightDp * 0.165f).dp)
                        .padding(end = 16.dp),
                    date = date,
                    dataUi = dataUi,
                    updateDate = updateDate
                )
            }
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
    showLabel: Boolean = false,
) {
    Card(
        modifier = modifier
            .clip(CardDefaults.shape)
            .dashedBorder(
                width = 1.dp,
                color = outlineVariantLight,
                shape = MaterialTheme.shapes.small, on = 4.dp, off = 4.dp
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
            defaultElevation = 0.dp
        ),
        border = null,
        colors = CardDefaults.cardColors(
            containerColor = surfaceVariantLight,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .wrapContentSize(Alignment.Center)

        ) {
            if (showLabel) {
                Row(modifier = Modifier.fillMaxSize().padding(start = 12.dp)) {
                    IconButton(onClick = {
                            updateDate(dataUi.copy(
                                selectedDate = date,
                                visibleDates = dataUi.visibleDates.map {
                                    it.copy(
                                        isSelected = it.date.isEqual(date.date)
                                    )
                                }
                            ), true)
                         },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = primaryContainerLight,
                            contentColor = onPrimaryContainerLight,
                        ),
                        modifier = Modifier.size(40.dp).align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add_recipe)
                        )
                    }

                    Text(
                        text = stringResource(R.string.planify_a_recipe),
                        style = MaterialTheme.typography.titleSmall.copy(color = onPrimaryContainerLight),
                        modifier = Modifier.padding(start = 8.dp).align(Alignment.CenterVertically)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = {
                            updateDate(dataUi.copy(
                                selectedDate = date,
                                visibleDates = dataUi.visibleDates.map {
                                    it.copy(
                                        isSelected = it.date.isEqual(date.date)
                                    )
                                }
                            ), true)
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = primaryContainerLight,
                            contentColor = onPrimaryContainerLight,
                        ),
                        modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add_recipe)
                        )
                    }
                }
            }
        }
    }
}
