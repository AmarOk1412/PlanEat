
package com.planeat.planeat.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.planeat.planeat.R
import com.planeat.planeat.data.Recipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL


@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun RequestContentPermission(imageBitmap: MutableState<ImageBitmap>, onUriSelected: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(contract =
    ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onUriSelected(uri)
        }
    }
    Column() {
        Button(onClick = {
            launcher.launch("image/*")
        }) {
            Text(text = stringResource(R.string.pick_image))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Image(bitmap = imageBitmap.value,
            contentDescription = null,
            modifier = Modifier.size(400.dp))
    }
}


@SuppressLint("CoroutineCreationDuringComposition")
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun EditRecipeScreen(
    model: AppModel,
    goBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRecipeUpdated: (Recipe) -> Unit,
    onRecipeDeleted: (Recipe) -> Unit
) {

    BackHandler {
        goBack()
    }

    val r = model.openedRecipe.value!!
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
        var cookingTime by remember { mutableIntStateOf(r.cookingTime) }
        var tags by remember { mutableStateOf(r.tags.joinToString(", ")) }
        var ingredients by remember { mutableStateOf(r.ingredients.joinToString("\n")) }
        var steps by remember { mutableStateOf(r.steps.joinToString("\n@@@\n")) }

        val bitmap = Bitmap.createBitmap(
            100,
            100,
            Bitmap.Config.ARGB_8888
        )
        val imageBitmap =  remember {
            mutableStateOf<ImageBitmap>(bitmap.asImageBitmap())
        }

        // For recipe.image = imagePath
        LaunchedEffect(Unit) {
            // For network operation
            CoroutineScope(Dispatchers.IO).launch {
                var imagePath = r.image
                if (imagePath.startsWith("http")) {
                    val imageUrl = URL(imagePath)
                    val connection = imageUrl.openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val inputStream = connection.inputStream
                    if (inputStream != null) {
                        // TODO if recipe deleted, cache deleted
                        if (imagePath.isEmpty()) {
                            imagePath = "recipe_${System.currentTimeMillis()}"
                        }
                        val outputFile = File(context.cacheDir, imagePath)
                        outputFile.parentFile?.mkdirs() // Create parent directories if they don't exist
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
                        recipe.image = imagePath
                        val source = ImageDecoder
                            .createSource(context.contentResolver, imagePath.toUri())
                        val drawable = ImageDecoder.decodeBitmap(source)
                        imageBitmap.value = drawable.asImageBitmap()
                    }
                } else if (imagePath.isNotEmpty()) {
                    val source = ImageDecoder
                        .createSource(context.contentResolver,  imagePath.toUri())
                    val drawable = ImageDecoder.decodeBitmap(source)
                    imageBitmap.value = drawable.asImageBitmap()
                }
            }
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
            label = { Text(text = stringResource(R.string.url)) },
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
            Text(text = stringResource(R.string.import_from_url))
        }

        // Image input
        RequestContentPermission(imageBitmap, onUriSelected = { uri ->
            val cacheDir = context.cacheDir
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                var imagePath = r.image
                if (imagePath.isEmpty() || imagePath.startsWith("http")) {
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
                recipe.image = imagePath
                val source = ImageDecoder
                    .createSource(context.contentResolver, imagePath.toUri())
                val drawable = ImageDecoder.decodeBitmap(source)
                imageBitmap.value = drawable.asImageBitmap()
            }
        })

        // Title input
        TextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(text = stringResource(R.string.title)) },
            maxLines = 1,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Kind of Meal input
        TextField(
            value = kindOfMeal,
            onValueChange = { kindOfMeal = it },
            label = { Text(text = stringResource(R.string.kind_of_meal)) },
            maxLines = 1,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Cooking Time input
        TextField(
            value = cookingTime.toString(),
            onValueChange = { cookingTime = it.toIntOrNull() ?: 0 },
            label = { Text(text = stringResource(R.string.cooking_time)) },
            maxLines = 1,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Season input
        TextField(
            value = season,
            onValueChange = { season = it },
            label = { Text(text = stringResource(R.string.season)) },
            maxLines = 1,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Tags input
        TextField(
            value = tags,
            onValueChange = { tags = it },
            label = { Text(text = stringResource(R.string.tags)) },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Ingredients input
        TextField(
            value = ingredients,
            onValueChange = { ingredients = it },
            label = { Text(text = stringResource(R.string.ingredients)) },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Steps input
        TextField(
            value = steps,
            onValueChange = { steps = it },
            label = { Text(text = stringResource(R.string.steps)) },
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
                recipe.parsed_ingredients = emptyList()
                recipe.steps = steps.split("\n@@@\n")
                recipe.url = if (url.isNotEmpty()) url else "recipe_${System.currentTimeMillis()}"
                onRecipeUpdated(recipe)
            }
        ) {
            Text(text = stringResource(R.string.save))
        }

        if (recipe.recipeId != 0L) {
            // Delete button
            Button(
                onClick = {
                    onRecipeDeleted(recipe)
                }
            ) {
                Text(text = stringResource(R.string.delete))
            }
        }
    }
}