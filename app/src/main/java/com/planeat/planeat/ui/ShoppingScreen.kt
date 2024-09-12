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

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.compose.surfaceContainerLowestLight
import com.planeat.planeat.R
import com.planeat.planeat.data.AgendaDb
import com.planeat.planeat.data.IngredientItem
import com.planeat.planeat.data.IngredientsDb
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.data.ShoppingList
import com.planeat.planeat.data.toIngredientIcon
import com.planeat.planeat.ui.components.MinimalRecipeItemList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.examples.textclassification.client.IngredientClassifier
import java.time.LocalDate
import java.time.ZoneOffset

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ShoppingScreen(
    modifier: Modifier = Modifier,
    onRecipeSelected: (Recipe) -> Unit,
) {
    val context = LocalContext.current
    val ic = IngredientClassifier(context)

    var shoppingList by remember {
        mutableStateOf<ShoppingList?>(null)
    }
    var sortingMethod by remember {
        mutableStateOf("")
    }


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
            val planned = adb.agendaDao().findBetweenDates(
                today.atTime(12, 0)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli(),
                inTwoWeeks.atTime(12, 0)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            )
            adb.close()
            val ingredientsDb = IngredientsDb.getDatabase(context)

            val sh = ShoppingList(planned, rdb, ingredientsDb, ic, context)
            sh.saveLoadFromDisk()
            shoppingList = sh
            sortingMethod = shoppingList!!.sortingMethod
        }
    }

    // UI section that uses the planned recipes and ingredient data
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            FloatingActionButton(onClick = { /*TODO*/ },
                containerColor = Color(0xFF01AA44),
                contentColor = Color(0xFFFFFFFF),
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.padding(end = 8.dp)) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add ingredient"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        content = {
            if (shoppingList != null) {
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
                        text = "${shoppingList!!.plannedRecipes.size} recipes",
                        style = MaterialTheme.typography.titleSmall
                    )

                    // Row for the planned recipes
                    Row(
                        modifier = Modifier
                            .height(100.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        shoppingList!!.plannedRecipes.forEach { recipe ->
                            MinimalRecipeItemList(recipe = recipe, onRecipeSelected = onRecipeSelected)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        // Display the total number of ingredients
                        Text(
                            text = "${shoppingList!!.ingredientsPerCategory.values.sumOf { it.keys.size } + shoppingList!!.customIngredients.size} items",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Spacer(modifier = Modifier.weight(1.0f))

                        Text(
                            text = "Sort by:",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        var showDialog = remember { mutableStateOf(false) }

                        ElevatedButton(onClick = { showDialog.value = true }, colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFFFFFFFF),
                            contentColor = Color(0xFF000000)
                        ), modifier = Modifier.padding(start = 8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Row {
                                Text(text = shoppingList!!.sortingMethod, modifier = Modifier.align(Alignment.CenterVertically))

                                Icon(Icons.Filled.KeyboardArrowDown, tint = Color(0xFF949494), contentDescription = null)
                            }

                            if (showDialog.value) {
                                DropdownMenu(
                                    containerColor = surfaceContainerLowestLight,
                                    offset = DpOffset(0.dp, 8.dp),
                                    expanded = showDialog.value,
                                    onDismissRequest = { showDialog.value = false },
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = "Aisle")
                                        },
                                        onClick = {
                                            shoppingList!!.changeSortingMethod("Aisle")
                                            sortingMethod = "Aisle"
                                            showDialog.value = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = "Recipe")
                                        },
                                        onClick = {
                                            shoppingList!!.changeSortingMethod("Recipe")
                                            sortingMethod = "Recipe"
                                            showDialog.value = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (sortingMethod == "Aisle") {
                        // Ingredients grouped by category
                        shoppingList!!.ingredientsPerCategory.forEach { (key, ingredients) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 6.dp
                                ),
                                colors = CardDefaults.cardColors(containerColor = surfaceContainerLowestLight),
                            ) {
                                Column() {
                                    Text(
                                        text = key.replaceFirstChar(Char::titlecase),
                                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                                    )

                                    ingredients.forEach { (ingredientName, ingredient) ->
                                        IngredientCheckbox(ingredient, ingredientName, onCheckedChange = { checked ->
                                            // Force refresh by creating a new copy
                                            shoppingList = shoppingList?.let {
                                                it.checkIngredient(ingredientName.lowercase(), checked)
                                                it.copy() // Update shoppingList to a new copy
                                            }
                                        })
                                    }
                                }
                            }
                        }
                    } else {
                        // Ingredients grouped by category
                        shoppingList!!.ingredientsPerRecipes.forEach { (key, ingredients) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 6.dp
                                ),
                                colors = CardDefaults.cardColors(containerColor = surfaceContainerLowestLight),
                            ) {
                                Column() {
                                    shoppingList!!.plannedRecipes.find { it.recipeId == key }?.title?.let { it1 ->
                                        Text(
                                            text = it1,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                                        )
                                    }

                                    ingredients.forEach { (ingredientName, ingredient) ->
                                        IngredientCheckbox(ingredient, ingredientName, onCheckedChange = { checked ->
                                            shoppingList = shoppingList?.let {
                                                it.checkIngredient(ingredientName.lowercase(), checked)
                                                it.copy() // Update shoppingList to a new copy
                                            }
                                        })
                                    }
                                }
                            }
                        }
                    }

                    if (shoppingList!!.customIngredients.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 6.dp
                            ),
                            colors = CardDefaults.cardColors(containerColor = surfaceContainerLowestLight),
                        ) {
                            Column() {
                                Text(
                                    text = "Custom",
                                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                                )

                                shoppingList!!.customIngredients.forEach { (ingredientName, ingredient) ->
                                    IngredientCheckbox(ingredient, ingredientName, onCheckedChange = { checked ->
                                        shoppingList = shoppingList?.let {
                                            it.checkIngredient(ingredientName.lowercase(), checked)
                                            it.copy() // Update shoppingList to a new copy
                                        }
                                    })
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(42.dp))
                }
            }
        }
    )
}

@Composable
fun IngredientCheckbox(ingredient: IngredientItem, ingredientName: String, onCheckedChange: (Boolean) -> Unit) {
    val quantity = if (ingredient.quantity.toInt().toFloat() != ingredient.quantity) ingredient.quantity.toString() else ingredient.quantity.toInt().toString()

    ListItem(
        modifier = Modifier.clickable {
            onCheckedChange(!ingredient.checked)
        },
        headlineContent = {
            Text(
                text = ingredientName.replaceFirstChar(Char::titlecase),
                style = if (ingredient.checked) {
                    TextStyle(textDecoration = TextDecoration.LineThrough)
                } else {
                    TextStyle()
                }
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
            Checkbox(
                checked = ingredient.checked,
                onCheckedChange = {
                    onCheckedChange(it)
                }
            )
        },
        colors = ListItemDefaults.colors(containerColor = surfaceContainerLowestLight)
    )
}