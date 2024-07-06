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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.room.Room
import coil.compose.AsyncImage
import com.planeat.planeat.R
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.ui.components.DockedSearchBar
import com.planeat.planeat.ui.components.RecipeListItem
import com.planeat.planeat.ui.components.calendar.CalendarUiModel
import com.planeat.planeat.ui.theme.backgroundCardRecipe
import com.planeat.planeat.ui.theme.textCardRecipe
import com.planeat.planeat.ui.utils.PlanEatContentType
import com.planeat.planeat.ui.utils.PlanEatNavigationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

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
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun RequestContentPermission(imageUri: Uri?, onUriSelected: (Uri) -> Unit) {
val context = LocalContext.current

val bitmap = Bitmap.createBitmap(
    100,
    100,
    Bitmap.Config.ARGB_8888
)

val imageBitmap =  remember {
    mutableStateOf<ImageBitmap>(bitmap.asImageBitmap())
}
Log.d("PlanEat", "@@@" + imageUri.toString())
if (imageUri != null) {
Log.d("PlanEat", "@@@B" + imageUri.toString())
    val source = ImageDecoder
        .createSource(context.contentResolver, imageUri)
    val drawable = ImageDecoder.decodeDrawable(source)
    if (drawable is BitmapDrawable) {
Log.d("PlanEat", "@@@A" + imageUri.toString())
        val bmp = drawable.bitmap
            if (bmp != null) {
    Log.d("PlanEat", "@@@X" + imageUri.toString())
                imageBitmap.value = bitmap.asImageBitmap()
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(contract =
    ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (imageUri != uri) {
                if (uri != null) {
                    Log.d("PlanEat", "@@@D" + imageUri.toString())
                    onUriSelected(uri)

                    val source = ImageDecoder
                        .createSource(context.contentResolver, uri)
                    val bitmap = ImageDecoder.decodeBitmap(source)
                    imageBitmap.value = bitmap.asImageBitmap()
                }
            }
    }
    Column() {
        Button(onClick = {
            launcher.launch("image/*")
        }) {
            Text(text = "Pick image")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Log.d("PlanEat", imageUri.toString())
        // TODO lagguy!

        imageBitmap.value.let {
            Image(bitmap = it,
                contentDescription = null,
                modifier = Modifier.size(400.dp))
        }
    }
}


@Composable
fun EditRecipeScreen(
    r: Recipe,
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
        val context = LocalContext.current

        var recipe by remember { mutableStateOf(r) }
        var title by remember { mutableStateOf(r.title) }
        var kindOfMeal by remember { mutableStateOf(r.kindOfMeal) }
        var season by remember { mutableStateOf(r.season) }
        var cookingTime by remember { mutableStateOf(r.cookingTime) }
        var tags by remember { mutableStateOf(r.tags.joinToString(", ")) }
        var ingredients by remember { mutableStateOf(r.ingredients.joinToString("\n")) }
        var steps by remember { mutableStateOf(r.steps.joinToString("\n")) }


        var imageUri by remember {
            mutableStateOf<Uri?>(null)
        }
        var imagePath by remember { mutableStateOf(r.image) }
        // Image input
        if (imagePath.startsWith("http")) {
            imagePath = ""
        } else if (imagePath.isNotEmpty()) {
            imageUri = imagePath.toUri()
        }

        // URL input
        var url by remember { mutableStateOf("") }
        if (r.url.startsWith("http")) {
            url = r.url
        }
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
        RequestContentPermission(imageUri, onUriSelected = { uri ->
            val cacheDir = context.cacheDir
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                if (imagePath.isEmpty()) {
                    imagePath = "recipe_${System.currentTimeMillis()}"
                }
                val outputFile = File(cacheDir, imagePath)
                val outputStream = FileOutputStream(outputFile)
                val buffer = ByteArray(4 * 1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()
                imagePath = Uri.fromFile(outputFile).toString()
                imageUri = imagePath.toUri()
                recipe.image = imagePath
            }
        })

        // Title input
        TextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(text = "Title") },
            maxLines = 1,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Kind of Meal input
        TextField(
            value = kindOfMeal,
            onValueChange = { kindOfMeal = it },
            label = { Text(text = "Kind of Meal") },
            maxLines = 1,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Cooking Time input
        TextField(
            value = cookingTime.toString(),
            onValueChange = { cookingTime = it.toIntOrNull() ?: 0 },
            label = { Text(text = "Cooking Time") },
            maxLines = 1,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Season input
        TextField(
            value = season,
            onValueChange = { season = it },
            label = { Text(text = "Season") },
            maxLines = 1,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Tags input
        TextField(
            value = tags,
            onValueChange = { tags = it },
            label = { Text(text = "Tags") },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Ingredients input
        TextField(
            value = ingredients,
            onValueChange = { ingredients = it },
            label = { Text(text = "Ingredients") },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Steps input
        TextField(
            value = steps,
            onValueChange = { steps = it },
            label = { Text(text = "Steps") },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Save button
        Button(
            onClick = {
                Log.d("PlanEat", "Save recipe: $recipe.title")
                recipe.title = title
                recipe.kindOfMeal = kindOfMeal
                recipe.season = season
                recipe.cookingTime = cookingTime
                recipe.tags = tags.split(", ")
                recipe.ingredients = ingredients.split("\n")
                recipe.steps = steps.split("\n")
                recipe.url = if (url.isNotEmpty()) url else "recipe_${System.currentTimeMillis()}"
                onSaved(recipe)
            }
        ) {
            Text(text = "Save")
        }
    }
}