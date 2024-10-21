
package com.planeat.planeat.ui

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.compose.onPrimaryContainerLight
import com.example.compose.outlineVariantLight
import com.example.compose.primaryContainerLight
import com.example.compose.surfaceContainerLowestLight
import com.planeat.planeat.R
import com.planeat.planeat.data.IngredientsDb
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.data.toIngredientIcon
import com.planeat.planeat.ui.components.calendar.CalendarUiModel
import com.planeat.planeat.ui.components.convertDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONObject

@Serializable
data class Step(val text: String, val image: String) {
}

fun getSteps(steps: String, needsUpdate: ()->Unit): List<Step> {
    val result = emptyList<Step>().toMutableList()
    try {
        val stepsArray = JSONArray(steps)
        for (index in 0 until stepsArray.length()) {
            val step = stepsArray.getJSONObject(index)
            result += Step(step.getString("text"), step.optString("image", ""))
        }
    } catch (e: Exception) {
        steps.split("\n@@@\n").forEach {
            result += Step(it, "")
        }
        needsUpdate()
    }
    return result
}


@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecipeDetailScreen(
    selectedRecipe: Recipe,
    model: AppModel,
    dataUi: CalendarUiModel,
    goToAgenda: () -> Unit,
    goToEdition: (Recipe) -> Unit,
    goBack: () -> Unit,
) {

    // Scaffold and scroll state
    val scrollState = rememberScrollState()
    val isFavorite = remember { mutableStateOf(selectedRecipe.favorite) }

    var showTitleInAppBar by remember { mutableStateOf(false) }
    var titleYPosition by remember { mutableStateOf(0f) } // Track the Y position of the title

    // Monitor scroll changes and determine if title should appear in AppBar
    LaunchedEffect(scrollState.value) {
        showTitleInAppBar = scrollState.value > titleYPosition + 50 // 50 as padding buffer
    }

    val uriHandler = LocalUriHandler.current

    val logo = when {
        selectedRecipe.url.contains("ricardo") -> R.drawable.ricardo
        selectedRecipe.url.contains("marmiton") -> R.drawable.marmiton
        selectedRecipe.url.contains("cha-cu.it") -> R.drawable.chacuit
        selectedRecipe.url.contains("nytimes.com") -> R.drawable.nytimes
        else -> null
    }
    val description = when {
        selectedRecipe.url.contains("ricardo") -> "Ricardo"
        selectedRecipe.url.contains("marmiton") -> "Marmiton"
        selectedRecipe.url.contains("cha-cu.it") -> "ChaCuit"
        selectedRecipe.url.contains("nytimes.com") -> "New-york times"
        else -> null
    }

    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var toPlanRecipe by remember { mutableStateOf<Recipe?>(null) }

    BackHandler {
        goBack()
    }

    var ingredients by remember { mutableStateOf(selectedRecipe.parsed_ingredients) }

    LaunchedEffect(selectedRecipe) {
        if (selectedRecipe.parsed_ingredients.isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                model.classifyRecipeAndIngredients(selectedRecipe)
                ingredients = selectedRecipe.parsed_ingredients
            }
        }
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = surfaceContainerLowestLight,
        bottomBar = {},
        topBar = {
            TopAppBar(title = { if (showTitleInAppBar) Text(selectedRecipe.title, style = MaterialTheme.typography.headlineSmall) else Text("") },
                navigationIcon = {
                    IconButton(onClick = { goBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceContainerLowestLight)
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .background(color = surfaceContainerLowestLight)
                .verticalScroll(scrollState)) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth().height(200.dp)
                    .clip(CardDefaults.shape)
            ) {
                AsyncImage(
                    model = selectedRecipe.image,
                    contentDescription = selectedRecipe.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }


            if (logo != null) {
                Icon(
                    imageVector = ImageVector.vectorResource(id =logo),
                    contentDescription = description!!,
                    modifier = Modifier
                        .width(88.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(start = 16.dp, top = 8.dp, bottom = 0.dp)
                        .clickable(onClick = {
                            uriHandler.openUri(selectedRecipe.url)
                        })
                )
            }

            Text(
                text = selectedRecipe.title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .align(Alignment.CenterHorizontally)
                    .onGloballyPositioned { coordinates ->
                        // Get the Y position of this text element on the screen
                        titleYPosition = coordinates.positionInParent().y
                    }
            )

            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.schedule),
                    contentDescription = "Schedule",
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = convertDuration(selectedRecipe.cookingTime),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            val context = LocalContext.current
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 22.dp, bottom = 16.dp)
            ) {
                Column(Modifier.width(67.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = {
                            toPlanRecipe = selectedRecipe
                            openBottomSheet = true
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = primaryContainerLight,
                            contentColor = onPrimaryContainerLight,
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add_recipe)
                        )
                    }
                    Text(text = stringResource(R.string.plan_it),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(vertical = 8.dp))
                }

                Column(modifier = Modifier.width(67.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedIconButton(onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                selectedRecipe.favorite = !selectedRecipe.favorite
                                if (selectedRecipe.recipeId == 0L) {
                                    model.add(selectedRecipe)
                                    val rdb = RecipesDb.getDatabase(context)
                                    val res = rdb.recipeDao().findByUrl(selectedRecipe.url)
                                    if (res != null) {
                                        selectedRecipe.recipeId = res.recipeId
                                    }
                                } else {
                                    model.update(selectedRecipe)
                                }
                                isFavorite.value = selectedRecipe.favorite
                            } catch (error: Exception) {
                                Log.d("PlanEat", "Error: $error")
                            }
                        }
                    },
                        border = BorderStroke(1.dp, outlineVariantLight),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite.value) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorite)
                        )
                    }

                    Text(text = stringResource(R.string.favorite),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(vertical = 8.dp))
                }

                Column(modifier = Modifier.width(67.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedIconButton(onClick = {
                        if (selectedRecipe.recipeId == 0L) {
                            CoroutineScope(Dispatchers.IO).launch {
                                model.add(selectedRecipe)
                                val rdb = RecipesDb.getDatabase(context)
                                val res = rdb.recipeDao().findByUrl(selectedRecipe.url)
                                if (res != null) {
                                    selectedRecipe.recipeId = res.recipeId
                                    goToEdition(res)
                                }
                            }
                        } else {
                            goToEdition(selectedRecipe)
                        }
                    },
                        border = BorderStroke(1.dp, outlineVariantLight),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.edit)
                        )
                    }

                    Text(text = stringResource(R.string.edit),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            val tabs = listOf(
                stringResource(id = R.string.ingredients),
                stringResource(id = R.string.steps)
            )

            val selectedTabIndex = remember { mutableIntStateOf(0) }

            val density = LocalDensity.current
            val tabWidths = remember {
                val tabWidthStateList = mutableStateListOf<Dp>()
                repeat(tabs.size) {
                    tabWidthStateList.add(0.dp)
                }
                tabWidthStateList
            }

            TabRow(
                selectedTabIndex = selectedTabIndex.intValue,
                modifier = Modifier.fillMaxWidth(),
                containerColor = surfaceContainerLowestLight,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.customTabIndicatorOffset(
                            currentTabPosition = tabPositions[selectedTabIndex.intValue],
                            tabWidth = tabWidths[selectedTabIndex.intValue]
                        )
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex.intValue == index,
                        onClick = { selectedTabIndex.intValue = index },
                        text = {
                            Text(text = title,
                                style = MaterialTheme.typography.labelLarge,
                                onTextLayout = { textLayoutResult ->
                                    tabWidths[index] =
                                        with(density) { textLayoutResult.size.width.toDp() }
                                })
                        }
                    )
                }
            }

            when (selectedTabIndex.intValue) {
                0 -> {
                    Column (
                        modifier = Modifier
                            .background(color = surfaceContainerLowestLight).padding(top = 8.dp),
                    ) {
                        if (ingredients.isNotEmpty()) {
                            ingredients.forEach {
                                val context = LocalContext.current
                                val db = IngredientsDb.getDatabase(context)

                                var name by remember { mutableStateOf(it.name.replaceFirstChar(Char::titlecase)) }
                                var res by remember { mutableStateOf<Int?>(null) }
                                LaunchedEffect(Unit) {
                                    withContext(Dispatchers.IO) {
                                        res = toIngredientIcon(it.name.lowercase(), db, context)
                                        name = it.toLocalName()
                                    }
                                }

                                val quantity = if (it.quantity.toInt().toFloat() != it.quantity) it.quantity.toString() else it.quantity.toInt().toString()

                                Row(modifier = Modifier.padding(horizontal = 16.dp).height(56.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    val painter = rememberAsyncImagePainter(res)
                                    Image(
                                        painter = painter,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp)
                                            .align(Alignment.CenterVertically)
                                    )

                                    Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                                        Text(name.replaceFirstChar(Char::titlecase), style = MaterialTheme.typography.labelLarge)
                                        if (quantity != "1")
                                            Text(quantity + " " + it.unit,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF706C7A)
                                            )
                                    }
                                }
                            }
                        } else {
                            selectedRecipe.ingredients.forEach {
                                ListItem(
                                    headlineContent = { Text(it, style = MaterialTheme.typography.labelLarge) },
                                    colors = ListItemDefaults.colors(containerColor = surfaceContainerLowestLight)
                                )
                            }
                        }
                    }
                }
                1 -> {
                    Column (
                        modifier = Modifier.padding(16.dp)
                    ) {
                        getSteps(selectedRecipe.steps,
                            needsUpdate = {
                                // TODO remove!
                                CoroutineScope(Dispatchers.IO).launch {
                                    val steps = JSONArray()
                                    selectedRecipe.steps.split("\n@@@\n").forEach {
                                        steps.put(JSONObject().put("text", it))
                                    }
                                    selectedRecipe.steps = steps.toString()
                                    model.update(selectedRecipe)
                                }
                            }).forEachIndexed { index, step ->

                                Text(
                                    text = stringResource(R.string.step, index + 1),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                if (step.image.isNotEmpty()) {
                                    AsyncImage(
                                        model = if (step.image.startsWith("http")) {
                                            step.image
                                        } else {
                                            ImageRequest.Builder(LocalContext.current)
                                                .data(step.image)
                                                .build()
                                        },
                                        contentDescription = step.text,
                                        modifier = Modifier.fillMaxWidth().clip(
                                            RoundedCornerShape(8.dp)
                                        ).padding(bottom = 8.dp),
                                        contentScale = ContentScale.Crop,
                                    )
                                }

                                Text(
                                    text = step.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                            }
                    }
                }
            }
        }
    }

    if (openBottomSheet) {
        BottomPlanifier(
            onDismissRequest = { openBottomSheet = false },
            toPlanRecipe = toPlanRecipe!!,
            goToAgenda = {
                openBottomSheet = false
                goToAgenda()
            }
        )
    }
}

fun Modifier.customTabIndicatorOffset(
    currentTabPosition: TabPosition,
    tabWidth: Dp
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "customTabIndicatorOffset"
        value = currentTabPosition
    }
) {
    val currentTabWidth by animateDpAsState(
        targetValue = tabWidth,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
    )
    val indicatorOffset by animateDpAsState(
        targetValue = ((currentTabPosition.left + currentTabPosition.right - tabWidth) / 2),
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
    )
    fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = indicatorOffset)
        .width(currentTabWidth)
}