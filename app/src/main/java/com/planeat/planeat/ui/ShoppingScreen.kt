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
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.data.toIngredientIcon
import com.planeat.planeat.ui.components.MinimalRecipeItemList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ShoppingScreen(
    modifier: Modifier = Modifier,
    onRecipeSelected: (Recipe) -> Unit,
) {
    val context = LocalContext.current
    var ingredients by remember { mutableStateOf<List<IngredientItem>>(emptyList()) }
    var planned by remember {
        mutableStateOf<List<Recipe>>(emptyList())
    }
    LaunchedEffect(Unit) {

        withContext(Dispatchers.IO) {
            val adb = Room.databaseBuilder(
                context,
                AgendaDb::class.java, "AgendaDb"
            ).fallbackToDestructiveMigration().build()
            val rdb = RecipesDb.getDatabase(context)
            val today = LocalDate.now()
            val inTwoWeeks = LocalDate.now().plusWeeks(2)
            val plannedRecipes = adb.agendaDao().findBetweenDates(today.atTime(12, 0)
                                                                        .toInstant(ZoneOffset.UTC)
                                                                        .toEpochMilli(),
                                                                    inTwoWeeks.atTime(12, 0)
                                                                    .toInstant(ZoneOffset.UTC)
                                                                    .toEpochMilli())
            adb.close()

            plannedRecipes.forEach {
                val recipe = rdb.recipeDao().findById(it.recipeId)
                if (recipe != null) {
                    planned += recipe
                    ingredients += recipe.parsed_ingredients
                }
            }
        }
    }


    Box(modifier = modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.tab_shopping_list),
                style = MaterialTheme.typography.headlineLarge,
            )

            Text(text = planned.size.toString() + " recipes", style = MaterialTheme.typography.titleSmall)

            Row(modifier = Modifier
                .height(100.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                planned.forEach { recipe ->
                    MinimalRecipeItemList(recipe = recipe, onRecipeSelected = onRecipeSelected)
                }
            }

            Text(text = ingredients.size.toString() + " items", style = MaterialTheme.typography.titleSmall)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerLowestLight),
            ) {
                Column {
                    ingredients.forEach {
                        val quantity = if (it.quantity.toInt().toFloat() != it.quantity) it.quantity.toString() else it.quantity.toInt().toString()
                        ListItem(
                            headlineContent = { Text(it.name.replaceFirstChar(Char::titlecase)) },
                            supportingContent = { if (quantity != "1") Text(quantity + " " + it.unit) },
                            leadingContent = {
                                toIngredientIcon(it.name.lowercase())?.let { it1 ->
                                    Image(painter = it1,
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp))
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