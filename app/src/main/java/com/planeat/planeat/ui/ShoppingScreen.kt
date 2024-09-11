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
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.compose.surfaceContainerLowestLight
import com.planeat.planeat.R
import com.planeat.planeat.data.AgendaDb
import com.planeat.planeat.data.IngredientItem
import com.planeat.planeat.data.IngredientsDb
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.data.toIngredientCategory
import com.planeat.planeat.data.toIngredientIcon
import com.planeat.planeat.ui.components.MinimalRecipeItemList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.examples.textclassification.client.IngredientClassifier
import java.time.LocalDate
import java.time.ZoneOffset

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ShoppingScreen(
    modifier: Modifier = Modifier,
    onRecipeSelected: (Recipe) -> Unit,
) {
    val context = LocalContext.current
    var ingredientsPerCategory by remember { mutableStateOf<Map<String, Map<String, IngredientItem>>>(emptyMap()) }
    var planned by remember {
        mutableStateOf<List<Recipe>>(emptyList())
    }

    val ic = IngredientClassifier(context)

    // Fetch data in LaunchedEffect and update state
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val adb = Room.databaseBuilder(
                context,
                AgendaDb::class.java, "AgendaDb"
            ).fallbackToDestructiveMigration().build()
            val rdb = RecipesDb.getDatabase(context)
            val today = LocalDate.now()
            val inTwoWeeks = LocalDate.now().plusWeeks(2)
            val plannedRecipes = adb.agendaDao().findBetweenDates(
                today.atTime(12, 0)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli(),
                inTwoWeeks.atTime(12, 0)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            )
            adb.close()
            val ingredientsDb = IngredientsDb.getDatabase(context)

            plannedRecipes.forEach { agendaItem ->
                val recipe = rdb.recipeDao().findById(agendaItem.recipeId)
                if (recipe != null) {
                    // Update planned recipes and ingredient count in UI state
                    planned = planned + recipe
                    recipe.parsed_ingredients.forEach { ingredient ->
                        val ingredientName = ingredient.name.lowercase()
                        val pluralIngredientName = if (ingredientName.endsWith("s")) ingredientName.dropLast(1) else ingredientName + "s"
                        val category = toIngredientCategory(ingredientName, ic, ingredientsDb)

                        // Get or create a mutable copy of the category's ingredient map
                        val updatedCategoryMap = ingredientsPerCategory.toMutableMap()
                        val innerMap = updatedCategoryMap[category]?.toMutableMap() ?: mutableMapOf()

                        // Check for singular and plural forms and update quantities accordingly
                        val existingIngredientName = when {
                            innerMap.containsKey(ingredientName) -> ingredientName
                            innerMap.containsKey(pluralIngredientName) -> pluralIngredientName
                            else -> null
                        }

                        if (existingIngredientName != null) {
                            // Get the existing ingredient and update its quantity
                            val oldIngredient = innerMap[existingIngredientName]!!
                            oldIngredient.addQuantity(ingredient)
                            innerMap[existingIngredientName] = oldIngredient
                        } else {
                            // If the ingredient doesn't exist, add it to the inner map
                            innerMap[ingredientName] = ingredient
                        }

                        // Update the outer map with the modified inner map
                        updatedCategoryMap[category] = innerMap

                        // Update the state with the modified map
                        ingredientsPerCategory = updatedCategoryMap
                    }
                }
            }
        }
    }

    // UI section that uses the planned recipes and ingredient data
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shopping List Header
            Text(
                text = stringResource(id = R.string.tab_shopping_list),
                style = MaterialTheme.typography.headlineLarge,
            )

            // Display number of planned recipes
            Text(
                text = "${planned.size} recipes",
                style = MaterialTheme.typography.titleSmall
            )

            // Row for the planned recipes
            Row(
                modifier = Modifier
                    .height(100.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                planned.forEach { recipe ->
                    MinimalRecipeItemList(recipe = recipe, onRecipeSelected = onRecipeSelected)
                }
            }

            // Display the total number of ingredients
            Text(
                text = "${ingredientsPerCategory.values.sumOf { it.keys.size }} items",
                style = MaterialTheme.typography.titleSmall
            )

            // Ingredients grouped by category
            ingredientsPerCategory.forEach { (key, ingredients) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceContainerLowestLight),
                ) {
                    Column() {
                        Text(
                            text = key.replaceFirstChar(Char::titlecase),
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                        )

                        ingredients.forEach { (ingredientName, ingredient) ->
                            val quantity = if (ingredient.quantity.toInt()
                                    .toFloat() != ingredient.quantity
                            ) ingredient.quantity.toString() else ingredient.quantity.toInt().toString()

                            ListItem(
                                headlineContent = {
                                    Text(
                                        ingredientName.replaceFirstChar(Char::titlecase)
                                    )
                                },
                                supportingContent = { if (quantity != "1") Text("$quantity ${ingredient.unit}") },
                                leadingContent = {
                                    toIngredientIcon(ingredientName.lowercase())?.let { icon ->
                                        Image(
                                            painter = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                },
                                trailingContent = {
                                    Checkbox(checked = false, onCheckedChange = {})
                                },
                                colors = ListItemDefaults.colors(containerColor = surfaceContainerLowestLight)
                            )
                        }
                    }
                }
            }
        }
    }
}
