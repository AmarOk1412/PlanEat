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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.window.layout.DisplayFeature
import coil.compose.AsyncImage
import com.planeat.planeat.R
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.ui.components.DockedSearchBar
import com.planeat.planeat.ui.components.RecipeListItem
import com.planeat.planeat.ui.theme.backgroundCardRecipe
import com.planeat.planeat.ui.theme.textCardRecipe
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
) {
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    var addNewRecipe by remember { mutableStateOf(false) }
    val context = LocalContext.current

    /**
     * When moving from LIST_AND_DETAIL page to LIST page clear the selection and user should see LIST screen.
     */
    LaunchedEffect(key1 = contentType) {

    }
    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = modifier.windowInsetsPadding(WindowInsets.statusBars)) {

            if (selectedRecipe != null) {
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
                    Text(
                        text = selectedRecipe!!.title,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // TODO better icon/redesign
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f / .8f)
                    ) {
                        AsyncImage(
                            model = selectedRecipe!!.image,
                            contentDescription = selectedRecipe!!.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            IconButton(
                                onClick = { CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val rdb = Room.databaseBuilder(
                                            context,
                                            RecipesDb::class.java, "RecipesDb"
                                        ).build()
                                        rdb.recipeDao().delete(selectedRecipe!!)
                                        rdb.close()
                                        selectedRecipe = null
                                    } catch (error: Exception) {
                                        Log.d("PlanEat", "Error: $error")
                                    }
                                } },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(backgroundCardRecipe)
                                    .align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.favorite),
                                    contentDescription = stringResource(R.string.favorite),
                                    tint = textCardRecipe,
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(id = R.string.ingredients),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    selectedRecipe!!.ingredients.forEach {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Text(
                        text = stringResource(id = R.string.steps),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    selectedRecipe!!.steps.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

            } else if (addNewRecipe) {
                // Show the detail screen
                // TODO separate the detail screen into a separate composable
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                ) {
                    BackHandler {
                        addNewRecipe = false
                    }

                    EditRecipeScreen(
                        model = model,
                        modifier = Modifier.fillMaxWidth(),
                        onSaved = { recipe -> CoroutineScope(Dispatchers.IO).launch {
                                val rdb = Room.databaseBuilder(
                                    context,
                                    RecipesDb::class.java, "RecipesDb"
                                ).build()
                                rdb.recipeDao().insertAll(recipe)
                                rdb.close()
                                addNewRecipe = false
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

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        items(count = recipes.size, key = {index -> index}, itemContent = { index ->
                            RecipeListItem(
                                recipe = recipes[index],
                                onRecipeSelected = { r -> selectedRecipe = r },
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
                LargeFloatingActionButton(
                    onClick = { selectedRecipe = null; addNewRecipe = true },
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
@Composable
fun RequestContentPermission(onUriSelected: (Uri?) -> Unit) {
    val context = LocalContext.current
    val bitmap =  remember {
        mutableStateOf<Bitmap?>(null)
    }
    var imageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val launcher = rememberLauncherForActivityResult(contract =
    ActivityResultContracts.GetContent()) { uri: Uri? ->
            onUriSelected(uri)
            imageUri = uri
    }
    Column() {
        Button(onClick = {
            launcher.launch("image/*")
        }) {
            Text(text = "Pick image")
        }

        Spacer(modifier = Modifier.height(12.dp))

        imageUri?.let {
            if (Build.VERSION.SDK_INT < 28) {
                bitmap.value = MediaStore.Images
                    .Media.getBitmap(context.contentResolver,it)

            } else {
                val source = ImageDecoder
                    .createSource(context.contentResolver,it)
                bitmap.value = ImageDecoder.decodeBitmap(source)
            }

            bitmap.value?.let {  btm ->
                Image(bitmap = btm.asImageBitmap(),
                    contentDescription =null,
                    modifier = Modifier.size(400.dp))
            }
        }
    }
}


@Composable
fun EditRecipeScreen(
    model: AppModel,
    modifier: Modifier = Modifier,
    onSaved: (Recipe) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()), // Add vertical scroll modifier
    ) {
        var recipe by remember { mutableStateOf(Recipe()) }

        // URL input
        var url by remember { mutableStateOf("") }
        TextField(
            value = url,
            onValueChange = { url = it },
            maxLines = 1,
            label = { Text(text = "URL") },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Import button
        Button(
            onClick = {
                // Add your import logic here
                CoroutineScope(Dispatchers.IO).launch {
                    model.getRecipe(url) { recipe = it }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Import from URL")
        }

        // Image input
        RequestContentPermission(onUriSelected = { uri -> recipe.image = uri.toString() })

        // Title input
        TextField(
            value = recipe.title,
            onValueChange = { recipe.title = it },
            label = { Text(text = "Title") },
            maxLines = 1,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Kind of Meal input
        TextField(
            value = recipe.kindOfMeal,
            onValueChange = { recipe.kindOfMeal = it },
            label = { Text(text = "Kind of Meal") },
            maxLines = 1,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Cooking Time input
        TextField(
            value = recipe.cookingTime.toString(),
            onValueChange = { recipe.cookingTime = it.toIntOrNull() ?: 0 },
            label = { Text(text = "Cooking Time") },
            maxLines = 1,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Season input
        TextField(
            value = recipe.season,
            onValueChange = { recipe.season = it },
            label = { Text(text = "Season") },
            maxLines = 1,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Tags input
        TextField(
            value = recipe.tags.joinToString("\n"),
            onValueChange = { recipe.tags = it.split("\n") },
            label = { Text(text = "Tags") },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Ingredients input
        TextField(
            value = recipe.ingredients.joinToString("\n"),
            onValueChange = { recipe.ingredients = it.split("\n") },
            label = { Text(text = "Ingredients") },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Steps input
        TextField(
            value = recipe.steps.joinToString("\n"),
            onValueChange = { recipe.steps = it.split("\n") },
            label = { Text(text = "Steps") },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Save button
        Button(
            onClick = {
                Log.d("PlanEat", "Save recipe: $recipe.title")
                recipe.url = if (url.isNotEmpty()) url else "recipe_${System.currentTimeMillis()}"
                onSaved(recipe)
            }
        ) {
            Text(text = "Save")
        }
    }
}