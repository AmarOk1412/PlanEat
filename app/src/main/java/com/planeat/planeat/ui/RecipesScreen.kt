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
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.planeat.planeat.R
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.data.Tags
import com.planeat.planeat.ui.components.DockedSearchBar
import com.planeat.planeat.ui.components.RecipeListItem
import com.planeat.planeat.ui.components.calendar.CalendarUiModel
import com.planeat.planeat.ui.utils.PlanEatContentType
import com.planeat.planeat.ui.utils.PlanEatNavigationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RecipesScreen(
    model: AppModel,
    contentType: PlanEatContentType,
    navigationType: PlanEatNavigationType,
    modifier: Modifier = Modifier,
    onQueryChanged: (String) -> Unit,
    recipes: List<Recipe>,
    onRecipeDeleted: (Recipe) -> Unit,
    dataUi: CalendarUiModel,
    goToAgenda: () -> Unit,
    onFilterClicked: (Tags) -> Unit,
) {
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    var editRecipe by remember { mutableStateOf(false) }
    val context = LocalContext.current

    /**
     * When moving from LIST_AND_DETAIL page to LIST page clear the selection and user should see LIST screen.
     */
    LaunchedEffect(key1 = contentType) {

    }
    Box(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {

        if (editRecipe) {
            // Show the detail screen
            // TODO separate the detail screen into a separate composable
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                BackHandler {
                    editRecipe = false
                }

                EditRecipeScreen(
                    r = selectedRecipe ?: Recipe(),
                    model = model,
                    modifier = Modifier.fillMaxWidth(),
                    onRecipeDeleted = {r ->
                        editRecipe = false
                        selectedRecipe = null
                        onRecipeDeleted(r)
                    },
                    onSaved = { recipe -> CoroutineScope(Dispatchers.IO).launch {
                            val rdb = Room.databaseBuilder(
                                context,
                                RecipesDb::class.java, "RecipesDb"
                            ).build()

                            val r = rdb.recipeDao().findById(recipe.recipeId)
                            if (r == null) {
                                rdb.recipeDao().insertAll(recipe)
                            } else {
                                rdb.recipeDao().update(recipe)
                            }

                            rdb.close()
                            editRecipe = false
                        }
                    }
                )
            }
        } else if (selectedRecipe != null) {
            // Show the detail screen
            // TODO separate the detail screen into a separate composable
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                BackHandler {
                    selectedRecipe = null
                }

                RecipeDetailScreen(
                    selectedRecipe = selectedRecipe!!,
                    goBack = {
                        selectedRecipe = null
                    }
                )
            }

        } else {
            // Show the list screen
            // TODO separate the list screen into a separate composable
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                // Header element
                Text(
                    text = stringResource(id = R.string.tab_recipes),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DockedSearchBar(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onQueryChanged,
                    recipes
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val filters = Tags.entries.map { it }
                    filters.forEach { filter ->
                        Button(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    onFilterClicked(filter)
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(text = filter.toString())
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    items(count = recipes.size, key = {index -> index}, itemContent = { index ->
                        RecipeListItem(
                            recipe = recipes[index],
                            onRecipeSelected = { r -> selectedRecipe = r },
                            onRecipeDeleted = { r -> onRecipeDeleted(r) },
                            onAgendaDeleted = {},
                            agenda = null,
                            selectedDate = dataUi.selectedDate.date,
                            goToAgenda = goToAgenda,
                            modifier = Modifier
                                .padding(8.dp)
                                .shadow(8.dp, shape = MaterialTheme.shapes.medium)
                        )
                    })
                }
            }
        }

        // When we have bottom navigation we show FAB at the bottom end.
        if (navigationType == PlanEatNavigationType.BOTTOM_NAVIGATION) {
            if (!editRecipe) {
                LargeFloatingActionButton(
                    onClick = { editRecipe = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(id = R.string.edit),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}