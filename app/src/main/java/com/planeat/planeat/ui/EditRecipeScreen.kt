
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.compose.onBackgroundLight
import com.example.compose.onPrimaryContainerLight
import com.example.compose.onSurfaceVariantLight
import com.example.compose.outlineVariantLight
import com.example.compose.primaryContainerLight
import com.example.compose.surfaceContainerLowestLight
import com.example.compose.surfaceLight
import com.example.compose.surfaceVariantLight
import com.planeat.planeat.R
import com.planeat.planeat.data.Ingredient
import com.planeat.planeat.data.IngredientItem
import com.planeat.planeat.data.IngredientsDb
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.Tags
import com.planeat.planeat.ui.utils.Translator
import dashedBorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL


@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun RequestContentPermission(hasImage: Boolean, imageBitmap: MutableState<ImageBitmap>, onUriSelected: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(contract =
    ActivityResultContracts.GetContent()) { uri: Uri? ->
        Log.d("PlanEat", "RequestContentPermission: $uri")
        if (uri != null) {
            Log.d("PlanEat", "RequestContentPermission: $uri")
            onUriSelected(uri)
        }
    }

    if (hasImage) {
        Box(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .wrapContentSize(Alignment.Center),
        ) {
            Image(bitmap = imageBitmap.value,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(CardDefaults.shape)
            )

            IconButton(modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFFFFFFFF)
                ),
                onClick = {
                    launcher.launch("image/*")
                })
            {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit)
                )
            }
        }
    } else {
        PickImageCard(onClick = {
            launcher.launch("image/*")
        })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PickImageCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .clip(CardDefaults.shape)
            .dashedBorder(
                width = 1.dp,
                color = outlineVariantLight,
                shape = MaterialTheme.shapes.small, on = 4.dp, off = 4.dp
            )
            .combinedClickable(onClick = onClick)
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
                .height(200.dp)
                .fillMaxWidth()
                .wrapContentSize(Alignment.Center),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.add_photo_alternate),
                    tint = Color(0xFF000000),
                    contentDescription = stringResource(R.string.add_image)
                )

                Text(
                    text = stringResource(R.string.add_image),
                    style = MaterialTheme.typography.titleSmall.copy(color = onPrimaryContainerLight),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun StepImagePermission(hasImage: Boolean, imageBitmap: MutableState<ImageBitmap>, onUriSelected: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(contract =
    ActivityResultContracts.GetContent()) { uri: Uri? ->
        Log.d("PlanEat", "RequestContentPermission: $uri")
        if (uri != null) {
            Log.d("PlanEat", "RequestContentPermission: $uri")
            onUriSelected(uri)
        }
    }

    if (hasImage) {
        Box(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .wrapContentSize(Alignment.Center),
        ) {
            Image(bitmap = imageBitmap.value,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(CardDefaults.shape)
            )

            IconButton(modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFFFFFFFF)
                ),
                onClick = {
                    launcher.launch("image/*")
                })
            {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit)
                )
            }
        }
    } else {
        PickImageRow(onClick = {
            launcher.launch("image/*")
        })
    }
}


@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PickImageRow(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {

    Row(modifier = Modifier
        .fillMaxSize()
        .combinedClickable(onClick = onClick)
        .padding(start = 12.dp)) {
        IconButton(onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = primaryContainerLight,
                contentColor = onPrimaryContainerLight,
            ),
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.add_a_picture_optional)
            )
        }

        Text(
            text = stringResource(R.string.add_a_picture_optional),
            style = MaterialTheme.typography.titleSmall.copy(color = onPrimaryContainerLight),
            modifier = Modifier
                .padding(start = 8.dp)
                .align(Alignment.CenterVertically)
        )
    }
}


@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@SuppressLint("CoroutineCreationDuringComposition", "MutableCollectionMutableState")
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun EditRecipeScreen(
    model: AppModel,
    goBack: () -> Unit,
    onRecipeUpdated: (Recipe) -> Unit,
) {

    val r = model.openedRecipe.value!!
    var step by remember { mutableIntStateOf(1) }
    var title by remember { mutableStateOf(r.title) }
    val recipe by remember { mutableStateOf(r) }
    var cookingTime by remember { mutableIntStateOf(r.cookingTime) }
    val context = LocalContext.current
    var season by remember { mutableStateOf(r.season) }
    val bitmap = Bitmap.createBitmap(
        100,
        100,
        Bitmap.Config.ARGB_8888
    )
    val imageBitmap = remember {
        mutableStateOf(bitmap.asImageBitmap())
    }
    var hasImage by remember { mutableStateOf(r.image.isNotEmpty()) }
    val currentTags = remember { mutableStateListOf<Tags>() }
    LaunchedEffect(r.tags) {
        if (r.tags.isNotEmpty()) {
            currentTags.clear() // Clear existing tags to avoid duplication.
            try {
                currentTags.addAll(r.tags.map { Tags.valueOf(it) })
            } catch (e: Exception) {
                Log.d("PlanEat", "EditRecipeScreen: ${e.message}")
            }
        }
    }
    var filters = Tags.entries.map { it }
    filters = filters.filter { it != Tags.All && it != Tags.Easy && it != Tags.Medium && it != Tags.Hard }
    val difficultyTags = listOf(Tags.Easy, Tags.Medium, Tags.Hard)

    // Ingredients
    var ingredientList = remember { mutableStateListOf<IngredientItem>() }
    LaunchedEffect(r.parsed_ingredients) {
        if (r.parsed_ingredients.isNotEmpty()) {
            ingredientList.clear() // Clear existing tags to avoid duplication.
            ingredientList.addAll(r.parsed_ingredients)
        } else {
            ingredientList.add(IngredientItem())
        }
    }

    var ingredientsList by remember {
        mutableStateOf<List<Ingredient>>(emptyList())
    }
    var autoCompleteList by remember {
        mutableStateOf<List<Ingredient>>(emptyList())
    }

    // Steps
    var stepList by remember {
        mutableStateOf(
            try {
                Json.decodeFromString<List<Step>>(r.steps).toMutableList()
            } catch (e: Exception) {
                mutableListOf<Step>()
            }
        )
    }
    LaunchedEffect(stepList) {
        if (stepList.isEmpty()) {
            stepList = mutableListOf(Step(text = "", image = ""))
        }
    }

    BackHandler {
        goBack()
    }


    if (step == 1) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = goBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back"
                            )
                        }
                    },
                    title = { },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceContainerLowestLight)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding()
                    )
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.step_d_of_4, step), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantLight)

                    Text(stringResource(R.string.details), style = MaterialTheme.typography.headlineSmall)
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
                                hasImage = true
                            }
                        } else if (imagePath.isNotEmpty()) {
                            val source = ImageDecoder
                                .createSource(context.contentResolver,  imagePath.toUri())
                            val drawable = ImageDecoder.decodeBitmap(source)
                            imageBitmap.value = drawable.asImageBitmap()
                            hasImage = true
                        }
                    }
                }


                // Image input
                RequestContentPermission(hasImage, imageBitmap, onUriSelected = { uri ->
                    val cacheDir = context.cacheDir
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        var imagePath = r.image
                        if (imagePath.isEmpty() || imagePath.startsWith("http") || imagePath.startsWith("file")) {
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
                        hasImage = true
                    }
                })

                // Title input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(text = stringResource(R.string.title)) },
                    maxLines = 1,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = outlineVariantLight,
                        focusedBorderColor = onSurfaceVariantLight,
                        unfocusedPlaceholderColor = outlineVariantLight,
                        focusedPlaceholderColor = onSurfaceVariantLight,
                    ),
                    visualTransformation = if (title.isEmpty())
                            PlaceholderTransformation(" ")
                        else VisualTransformation.None,
                    modifier = Modifier
                        .testTag("title_input")
                        .fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    var baseTime by remember { mutableStateOf((r.cookingTime.toFloat() / 60.0f).toString()) }
                    // Cooking Time input
                    OutlinedTextField(
                        trailingIcon = { Text(text = stringResource(R.string.hours), color = outlineVariantLight) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = true,
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        value = baseTime,
                        onValueChange = {
                            baseTime = it
                            val v = (it.toFloatOrNull() ?: 0.0f)
                            cookingTime = (v * 60).toInt()
                        },
                        label = { Text(text = stringResource(R.string.cooking_time)) },
                        maxLines = 1,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = outlineVariantLight,
                            focusedBorderColor = onSurfaceVariantLight,
                        ),
                        modifier = Modifier
                            .weight(0.5f)
                            .testTag("cooking_time_input")
                    )


                    SeasonalityTextField(
                        recipeSeason = season,
                        onValidate = { season = it },
                        modifier = Modifier
                            .weight(0.5f)
                            .testTag("season_input")
                    )
                }


                Spacer(modifier = Modifier.weight(1.0f))

                // Save button
                Button(
                    enabled = title.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { step += 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryContainerLight, contentColor = onPrimaryContainerLight)
                ) {
                    Text(text = stringResource(R.string.next))
                }
            }
        }
    }
    else if (step == 2) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { step-=1}) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back"
                            )
                        }
                    },
                    title = { },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceContainerLowestLight)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.step_d_of_4, step), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantLight)

                    Text(stringResource(R.string.categories), style = MaterialTheme.typography.headlineSmall)
                }

                Text(stringResource(R.string.choose_a_difficulty), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantLight)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    difficultyTags.forEach { filter ->
                        val active = currentTags.contains(filter)
                        Button(
                            onClick = {
                                Log.d("PlanEat", "@@@ onClick $filter")
                                if (active) {
                                    currentTags.remove(filter)
                                } else {
                                    currentTags.removeAll(difficultyTags)
                                    currentTags.add(filter)
                                }
                            },
                            shape = RoundedCornerShape(100.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) primaryContainerLight else surfaceLight,
                                contentColor = if (active) onPrimaryContainerLight else Color.Black
                            ),
                        ) {
                            Row {
                                Text(
                                    text = filter.getString(LocalContext.current),
                                    fontSize = with(LocalDensity.current) { 14.dp.toSp() },
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                                if (active) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.remove_tag),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Text(stringResource(R.string.choose_1_or_more_categorie_s), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantLight)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        val active = currentTags.contains(filter)
                        Button(
                            onClick = {
                                Log.d("PlanEat", "@@@ onClick $filter")
                                if (active) {
                                    currentTags.remove(filter)
                                } else {
                                    currentTags.add(filter)
                                }
                            },
                            shape = RoundedCornerShape(100.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) primaryContainerLight else surfaceLight,
                                contentColor = if (active) onPrimaryContainerLight else Color.Black
                            ),
                        ) {
                            Row {
                                Text(
                                    text = filter.getString(LocalContext.current),
                                    fontSize = with(LocalDensity.current) { 14.dp.toSp() },
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                                if (active) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.remove_tag),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1.0f))

                // Save button
                Button(
                    enabled = title.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { step += 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryContainerLight, contentColor = onPrimaryContainerLight)
                ) {
                    Text(text = stringResource(R.string.next))
                }
            }
        }
    } else if (step == 3) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { step-=1}) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back"
                            )
                        }
                    },
                    title = { },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceContainerLowestLight)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.step_d_of_4, step), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantLight)

                    Text(stringResource(R.string.ingredients), style = MaterialTheme.typography.headlineSmall)
                }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        val ingredientsDb = IngredientsDb.getDatabase(context)
                        ingredientsList = ingredientsDb.ingredientDao().selectAll()
                        autoCompleteList = ingredientsList
                    }
                }

                Column(modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    ingredientList.forEachIndexed { index, ingredient ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (index > 0) {
                                HorizontalDivider(color = surfaceLight)
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextFieldWithAutoComplete(
                                    value = TextFieldValue(
                                        ingredient.name,
                                        TextRange(ingredient.name.length)
                                    ),
                                    onValueChange = { newValue ->
                                        autoCompleteList = ingredientsList.filter { it.name.contains(newValue.text) }
                                        val ig = ingredient.copy()
                                        ig.name = newValue.text
                                        ingredientList[index] = ig
                                    },
                                    list = autoCompleteList,
                                    modifier = Modifier
                                        .padding(bottom = 16.dp)
                                        .weight(1.0f)
                                        .align(Alignment.CenterVertically)
                                )

                                IconButton(onClick = { ingredientList.removeAt(index) },
                                    modifier = Modifier.align(Alignment.CenterVertically)) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove step"
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.None,
                                        autoCorrect = true,
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    value = ingredient.quantity.toString(),
                                    onValueChange = {
                                        val newQuantity = it.toFloatOrNull() ?: 0.0f
                                        val ig = ingredient.copy()
                                        ig.quantity = newQuantity
                                        ingredientList[index] = ig
                                    },
                                    label = { Text(text = stringResource(R.string.quantity)) },
                                    maxLines = 1,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = outlineVariantLight,
                                        focusedBorderColor = onSurfaceVariantLight,
                                    ),
                                    modifier = Modifier.weight(0.5f)
                                )

                                UnitTextField(
                                    modifier = Modifier.weight(0.5f),
                                    value = TextFieldValue(ingredient.unit, TextRange(ingredient.unit.length)),
                                    onValueChange = {
                                        val ig = ingredient.copy()
                                        ig.unit = it.text
                                        ingredientList[index] = ig
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(onClick = { ingredientList.add(IngredientItem()) })
                    ) {
                        IconButton(
                            onClick = { ingredientList.add(IngredientItem()) }, // Add a new empty step
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = primaryContainerLight,
                                contentColor = onPrimaryContainerLight,
                            ),
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterVertically)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.add_ingredient)
                            )
                        }

                        Text(
                            text = stringResource(R.string.add_ingredient),
                            style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF000000)),
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                    // Adjust Spacer height based on keyboard height
                    Spacer(modifier = Modifier.weight(1.0f))

                    // Save button
                    Button(
                        enabled = title.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { step += 1 },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryContainerLight, contentColor = onPrimaryContainerLight)
                    ) {
                        Text(text = stringResource(R.string.next))
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { step-=1}) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back"
                            )
                        }
                    },
                    title = { },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceContainerLowestLight)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.step_d_of_4, step),
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariantLight
                    )

                    Text(
                        stringResource(R.string.ingredients),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }


                Column(modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Steps
                    stepList.forEachIndexed { index, step ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (index > 0) {
                                HorizontalDivider(color = surfaceLight)
                            }

                            Row {
                                Text(
                                    text = stringResource(R.string.step, index + 1),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = onSurfaceVariantLight,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )

                                Spacer(modifier = Modifier.weight(1.0f))

                                IconButton(onClick = {
                                        stepList = stepList.toMutableList().also {
                                            it.removeAt(index)
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.CenterVertically).size(16.dp)) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove step"
                                    )
                                }
                            }


                            val stepImage = remember {
                                mutableStateOf(bitmap.asImageBitmap())
                            }
                            var hasStepImage by remember { mutableStateOf(step.image.isNotEmpty()) }


                            // For recipe.image = imagePath
                            LaunchedEffect(Unit) {
                                // For network operation
                                CoroutineScope(Dispatchers.IO).launch {
                                    var imagePath = step.image
                                    if (imagePath.startsWith("http")) {
                                        val imageUrl = URL(imagePath)
                                        val connection = imageUrl.openConnection() as HttpURLConnection
                                        connection.doInput = true
                                        connection.connect()
                                        val inputStream = connection.inputStream
                                        if (inputStream != null) {
                                            // TODO if recipe deleted, cache deleted
                                            if (imagePath.isEmpty()) {
                                                imagePath = "recipe_${System.currentTimeMillis()}_step_${index}"
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
                                            val source = ImageDecoder
                                                .createSource(context.contentResolver, imagePath.toUri())
                                            val drawable = ImageDecoder.decodeBitmap(source)
                                            stepImage.value = drawable.asImageBitmap()
                                            hasStepImage = true
                                        }
                                    } else if (imagePath.isNotEmpty()) {
                                        val source = ImageDecoder
                                            .createSource(context.contentResolver,  imagePath.toUri())
                                        val drawable = ImageDecoder.decodeBitmap(source)
                                        stepImage.value = drawable.asImageBitmap()
                                        hasStepImage = true
                                    }
                                }
                            }


                            // Image input
                            StepImagePermission(hasStepImage, stepImage, onUriSelected = { uri ->
                                val cacheDir = context.cacheDir
                                val inputStream = context.contentResolver.openInputStream(uri)
                                if (inputStream != null) {
                                    var imagePath = step.image
                                    if (imagePath.isEmpty() || imagePath.startsWith("http") || imagePath.startsWith("file")) {
                                        imagePath = "recipe_${System.currentTimeMillis()}_step_${index}"
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
                                    val source = ImageDecoder
                                        .createSource(context.contentResolver, imagePath.toUri())
                                    val drawable = ImageDecoder.decodeBitmap(source)
                                    stepImage.value = drawable.asImageBitmap()
                                    hasStepImage = true
                                    stepList = stepList.toMutableList().apply {
                                        this[index] = step.copy(image = imagePath)
                                    }
                                }
                            })

                            OutlinedTextField(
                                value = step.text,
                                onValueChange = { newValue ->
                                    stepList = stepList.toMutableList().apply {
                                        this[index] = step.copy(text = newValue)
                                    }
                                },
                                visualTransformation = if (step.text.isEmpty())
                                    PlaceholderTransformation(" ")
                                else VisualTransformation.None,
                                minLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = outlineVariantLight,
                                    focusedBorderColor = onSurfaceVariantLight,
                                ),
                                shape = RoundedCornerShape(16.dp),
                                label = { Text(text = stringResource(R.string.description)) },
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }

                    // Add a new empty step
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(onClick = {
                                stepList.add(
                                    Step(
                                        text = "",
                                        image = ""
                                    )
                                )
                            })
                    ) {
                        IconButton(
                            onClick = {
                                stepList = stepList.toMutableList().apply {
                                    add(Step(text = "", image = ""))
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = primaryContainerLight,
                                contentColor = onPrimaryContainerLight,
                            ),
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterVertically)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.add_step))
                        }

                        Text(
                            text = stringResource(R.string.add_step),
                            style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF000000)),
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1.0f))

                    // Save button
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            Log.d("PlanEat", "Save recipe: $recipe.title")
                            recipe.title = title
                            recipe.edited = true
                            recipe.season = season
                            recipe.cookingTime = cookingTime
                            recipe.tags = currentTags.map { it.name }
                            recipe.parsed_ingredients = ingredientList
                            recipe.steps = Json.encodeToString(stepList)
                            recipe.url = recipe.url.ifEmpty { "recipe_${System.currentTimeMillis()}" }
                            onRecipeUpdated(recipe)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryContainerLight,
                            contentColor = onPrimaryContainerLight
                        )
                    ) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            }
        }
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitTextField(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    var unitList = listOf("L", "Kg", "g", "cL", "oz")
    var dropDownExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = dropDownExpanded, onExpandedChange = { dropDownExpanded = !dropDownExpanded }, modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                dropDownExpanded = true
            },
            visualTransformation = if (value.text.isEmpty())
                PlaceholderTransformation(" ")
            else VisualTransformation.None,
            shape = RoundedCornerShape(16.dp),
            label = { Text(text = stringResource(R.string.unit)) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = outlineVariantLight,
                focusedBorderColor = onSurfaceVariantLight,
            )
        )
        val filterOpts = unitList.filter { it.contains(value.text, ignoreCase = true) }
        if (filterOpts.isNotEmpty()) {
            ExposedDropdownMenu(expanded = dropDownExpanded, onDismissRequest = { dropDownExpanded = false }, containerColor = surfaceContainerLowestLight) {
                filterOpts.take(5).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option) },
                        onClick = {
                            onValueChange(
                                TextFieldValue(
                                    option,
                                    TextRange(option.length)
                                )
                            )
                            dropDownExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedTextFieldWithAutoComplete(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    list: List<Ingredient>
) {
    var dropDownExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = dropDownExpanded, onExpandedChange = { dropDownExpanded = !dropDownExpanded }, modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                dropDownExpanded = true
            },
            visualTransformation = if (value.text.isEmpty())
                PlaceholderTransformation(" ")
            else VisualTransformation.None,
            shape = RoundedCornerShape(16.dp),
            label = { Text(text = stringResource(R.string.description)) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = outlineVariantLight,
                focusedBorderColor = onSurfaceVariantLight,
            )
        )
        val filterOpts = list.filter { it.name.contains(value.text, ignoreCase = true) }
        if (filterOpts.isNotEmpty()) {
            ExposedDropdownMenu(expanded = dropDownExpanded, onDismissRequest = { dropDownExpanded = false }, containerColor = surfaceContainerLowestLight) {
                filterOpts.take(5).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option.name) },
                        onClick = {
                            onValueChange(
                                TextFieldValue(
                                    option.name,
                                    TextRange(option.name.length)
                                )
                            )
                            dropDownExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SeasonalityTextField(
    recipeSeason: String,
    onValidate: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val source = remember {
        MutableInteractionSource()
    }
    if (source.collectIsPressedAsState().value)
        openBottomSheet = true

    OutlinedTextField(
        value = recipeSeason,
        enabled = false,
        interactionSource = source,
        visualTransformation = if (recipeSeason.isEmpty())
            PlaceholderTransformation(stringResource(R.string.choose))
        else VisualTransformation.None,
        onValueChange = { },
        label = { Text(text = stringResource(R.string.season)) },
        maxLines = 1,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = outlineVariantLight,
            focusedBorderColor = onSurfaceVariantLight,
            disabledBorderColor = outlineVariantLight,
            disabledLabelColor = outlineVariantLight    ,
            disabledTextColor = Color(0xFF000000),
        ),
        modifier = modifier.clickable { openBottomSheet = true }
    )

    if (openBottomSheet) {
        BottomSeasonality(
            recipeSeason,
            onDismissRequest = { openBottomSheet = false },
            onValidate = { text ->
                onValidate(text)
                openBottomSheet = false
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BottomSeasonality(
    recipeSeason: String,
    onDismissRequest: () -> Unit,
    onValidate: (String) -> Unit,
) {
    val seasons = listOf("Winter", "Spring", "Summer", "Autumn")
    val selectedSeasons = remember { mutableStateListOf<String>() }
    for (s in recipeSeason.split(",")) {
        if (s in seasons) {
            selectedSeasons.add(s)
        }
    }

    val skipPartiallyExpanded by rememberSaveable { mutableStateOf(true) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)

    ModalBottomSheet(
        containerColor = surfaceContainerLowestLight,
        onDismissRequest = onDismissRequest,
        sheetState = bottomSheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {

            seasons.forEach { season ->

                var translatedSeason by remember { mutableStateOf(season) }

                LaunchedEffect(Unit) {
                    CoroutineScope(Dispatchers.IO).launch {
                        translatedSeason = Translator().translate(season)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = translatedSeason,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Checkbox(
                        checked = selectedSeasons.contains(season),
                        colors = CheckboxDefaults.colors(
                            uncheckedColor = outlineVariantLight,
                            checkedColor = onBackgroundLight
                        ),
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                selectedSeasons.add(season)
                            } else {
                                selectedSeasons.remove(season)
                            }
                        }
                    )
                }
            }

            Button(
                onClick = {
                    onValidate(selectedSeasons.joinToString(","))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryContainerLight, contentColor = onPrimaryContainerLight)
            ) {
                Text(text = stringResource(R.string.validate),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 10.dp))
            }
        }
    }
}

class PlaceholderTransformation(
    val placeholder: String,
    val placeholderColor: Color = outlineVariantLight
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return PlaceholderFilter(text, placeholder, placeholderColor)
    }
}

fun PlaceholderFilter(
    text: AnnotatedString,
    placeholder: String,
    placeholderColor: Color
): TransformedText {
    val numberOffsetTranslator = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            return 0
        }

        override fun transformedToOriginal(offset: Int): Int {
            return 0
        }
    }

    val placeholderText = AnnotatedString(
        text = placeholder,
        spanStyle = SpanStyle(color = placeholderColor)
    )

    return TransformedText(placeholderText, numberOffsetTranslator)
}