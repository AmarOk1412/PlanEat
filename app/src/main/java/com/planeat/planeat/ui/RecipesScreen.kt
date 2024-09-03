/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.planeat.planeat.ui

import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.room.Room
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.compose.surfaceContainerLowestLight
import com.planeat.planeat.R
import com.planeat.planeat.data.Agenda
import com.planeat.planeat.data.AgendaDb
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.data.Tags
import com.planeat.planeat.data.toTagIcon
import com.planeat.planeat.ui.components.RecipeListItem
import com.planeat.planeat.ui.components.calendar.CalendarDataSource
import com.planeat.planeat.ui.components.calendar.CalendarUiModel
import com.planeat.planeat.ui.utils.PlanEatNavigationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RecipesScreen(
    model: AppModel,
    modifier: Modifier = Modifier,
    onQueryChanged: (String) -> Unit,
    onRecipeDeleted: (Recipe) -> Unit,
    onRecipeAdded: (Recipe) -> Unit,
    dataUi: CalendarUiModel,
    goToAgenda: () -> Unit,
    goToDetails: (Recipe) -> Unit,
    onFilterClicked: (Tags) -> Unit,
) {
    var editRecipe by remember { mutableStateOf<Recipe?>(null) }
    val context = LocalContext.current

    Box(modifier = modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)) {

        if (editRecipe != null) {
            // Show the detail screen
            // TODO separate the detail screen into a separate composable
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                BackHandler {
                    editRecipe = null
                }

                EditRecipeScreen(
                    r = editRecipe ?: Recipe(),
                    model = model,
                    modifier = Modifier.fillMaxWidth(),
                    onRecipeDeleted = {r ->
                        editRecipe = null
                        onRecipeDeleted(r)
                    },
                    onSaved = { recipe -> CoroutineScope(Dispatchers.IO).launch {
                            val rdb = Room.databaseBuilder(
                                context,
                                RecipesDb::class.java, "RecipesDb"
                            ).build()

                            val r = rdb.recipeDao().findById(recipe.recipeId)
                            if (r.recipeId == 0.toLong()) {
                                rdb.recipeDao().insertAll(recipe)
                            } else {
                                rdb.recipeDao().update(recipe)
                            }

                            rdb.close()
                            editRecipe= null
                        }
                    }
                )
            }
        } else {
            // Show the list screen
            // TODO separate the list screen into a separate composable
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Header element
                Text(
                    text = stringResource(id = R.string.tab_recipes),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(start=16.dp, bottom = 16.dp)
                )

                var text by rememberSaveable { mutableStateOf("") }
                var expanded by rememberSaveable { mutableStateOf(false) }
                val filters = Tags.entries.map { it }

                LaunchedEffect(text) {
                    delay(300)
                    onQueryChanged.invoke(text)
                }

                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = SearchBarDefaults.colors(
                        containerColor = surfaceContainerLowestLight,
                    ),
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = text,
                            onQueryChange = {
                                text = it
                            },
                            onSearch = { expanded = false },
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            placeholder = { Text(stringResource(id = R.string.search_placeholder)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 0.dp, top = 16.dp, end = 0.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .semantics { traversalIndex = 1f },
                    ) {

                        items(model.recipesShown) { recipe ->
                            ListItem(
                                headlineContent = { Text(recipe.title) },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        AsyncImage(
                                            model = if (recipe.image.startsWith("http")) {
                                                recipe.image
                                            } else {
                                                ImageRequest.Builder(LocalContext.current)
                                                    .data(recipe.image)
                                                    .build()
                                            },
                                            contentDescription = recipe.title,
                                            contentScale = ContentScale.Crop,
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier =
                                Modifier
                                    .clickable {
                                        goToDetails(recipe)
                                    }
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            )

                        }

                        item {

                            ListItem(
                                headlineContent = { Text("Categories") },
                                leadingContent = { Icon(Icons.Filled.Star, contentDescription = null) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            )

                            filters.forEach { filter ->
                                ListItem(
                                    headlineContent = { Text(filter.toString()) },
                                    leadingContent = { toTagIcon(tag = filter) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier =
                                    Modifier
                                        .clickable {
                                            onFilterClicked(filter)
                                            expanded = false
                                        }
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.width(16.dp))

                    filters.forEach { filter ->

                        Button(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    onFilterClicked(filter)
                                }
                            },
                            shape = RoundedCornerShape(100.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 6.dp
                            ),
                            contentPadding = PaddingValues(horizontal=16.dp, vertical=10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = surfaceContainerLowestLight, contentColor = Color.Black),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    toTagIcon(tag = filter)
                                    Text(
                                        text = filter.toString(),
                                        fontSize = with(LocalDensity.current) {
                                            14.dp.toSp()
                                        },
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .padding(start = 8.dp)
                                    )
                                }
                        }
                    }
                }

                var openBottomSheet by rememberSaveable { mutableStateOf(false) }
                var skipPartiallyExpanded by rememberSaveable { mutableStateOf(false) }
                val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
                var toPlanRecipe by remember { mutableStateOf<Recipe?>(null) }


                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.padding(start=16.dp, end=16.dp, top = 8.dp)
                ) {
                    items(model.recipesShown) { recipe ->
                        RecipeListItem(
                            recipe = recipe,
                            onRecipeSelected = { r ->
                                goToDetails(r)
                            },
                            onEditRecipe = { r ->
                                editRecipe = r
                            },
                            onPlanRecipe = { r ->
                                toPlanRecipe = r
                                openBottomSheet = true
                            },
                            onRecipeDeleted = onRecipeDeleted,
                            onRecipeAdded = onRecipeAdded,
                            searching = model.currentSearchTerm.isNotEmpty(),
                            onAgendaDeleted = {},
                            agenda = null,
                            selectedDate = dataUi.selectedDate.date,
                            goToAgenda = goToAgenda,
                            modifier = Modifier
                                .padding(8.dp)
                                .shadow(8.dp, shape = MaterialTheme.shapes.medium)
                        )
                    }
                }

                if (openBottomSheet) {


                    val selectedSource by remember { mutableStateOf(CalendarDataSource()) }
                    var selectedUi by remember { mutableStateOf(selectedSource.getData(lastSelectedDate = selectedSource.today)) }
                    val selectedDates = remember { mutableStateListOf<LocalDate>(dataUi.selectedDate.date) }

                    ModalBottomSheet(
                        onDismissRequest = { openBottomSheet = false },
                        sheetState = bottomSheetState,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {

                            Text(
                                text = "Choose a date",
                                style = MaterialTheme.typography.titleLarge,
                            )


                            DateHeader(
                                dataUi = selectedUi,
                                onPrevClickListener = { startDate ->
                                    val finalStartDate = startDate.minusDays(1)
                                    selectedUi = selectedSource.getData(startDate = finalStartDate, lastSelectedDate = selectedUi.selectedDate.date)
                                },
                                onNextClickListener = { endDate ->
                                    val finalStartDate = endDate.plusDays(2)
                                    selectedUi = selectedSource.getData(startDate = finalStartDate, lastSelectedDate = selectedUi.selectedDate.date)
                                }
                            )

                            selectedUi.visibleDates.forEach{ date ->

                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = date.date.format(
                                            DateTimeFormatter.ofPattern("EEEE d")
                                        ),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Checkbox(
                                        checked = selectedDates.contains(date.date),
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) {
                                                selectedDates.add(date.date)
                                            } else {
                                                selectedDates.remove(date.date)
                                            }
                                        }
                                    )
                                }


                            }

                            Button(
                                onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val agendaDb = Room
                                            .databaseBuilder(
                                                context,
                                                AgendaDb::class.java, "AgendaDb"
                                            )
                                            .build()
                                        for (d in selectedDates) {
                                            Log.w("PlanEat", "Selected date: ${d}")
                                            val dateMidday = d
                                                .atTime(12, 0)
                                                .toInstant(ZoneOffset.UTC)
                                                .toEpochMilli()

                                            Log.w("PlanEat", "Recipe: ${toPlanRecipe!!.recipeId}, Date: ${dateMidday}")
                                            agendaDb
                                                .agendaDao()
                                                .insertAll(
                                                    Agenda(
                                                        date = dateMidday,
                                                        recipeId = toPlanRecipe!!.recipeId
                                                    )
                                                )
                                        }
                                        agendaDb.close()
                                        openBottomSheet = false
                                        goToAgenda()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Plan it")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun DateHeader(
    dataUi: CalendarUiModel,
    onPrevClickListener: (LocalDate) -> Unit,
    onNextClickListener: (LocalDate) -> Unit,
) {
    Row (modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)) {
        IconButton(onClick = {
            onPrevClickListener(dataUi.startDate.date)
        }) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "Back"
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(text = dataUi.startDate.date.format(
                DateTimeFormatter.ofPattern("MMMM yyyy")
            ), modifier = Modifier.align(Alignment.CenterVertically))

        Spacer(modifier = Modifier.weight(1f))

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