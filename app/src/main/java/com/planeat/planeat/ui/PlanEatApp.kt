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
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.window.layout.DisplayFeature
import com.planeat.planeat.connectors.ChaCuit
import com.planeat.planeat.connectors.Connector
import com.planeat.planeat.connectors.Marmiton
import com.planeat.planeat.connectors.Ricardo
import com.planeat.planeat.data.IngredientItem
import com.planeat.planeat.data.ParsedIngredient
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.data.Tags
import com.planeat.planeat.data.toTags
import com.planeat.planeat.ui.components.calendar.CalendarDataSource
import com.planeat.planeat.ui.components.calendar.CalendarUiModel
import com.planeat.planeat.ui.navigation.PlanEatBottomNavigationBar
import com.planeat.planeat.ui.navigation.PlanEatNavigationActions
import com.planeat.planeat.ui.navigation.PlanEatNavigationRail
import com.planeat.planeat.ui.navigation.PlanEatRoute
import com.planeat.planeat.ui.navigation.PlanEatTopLevelDestination
import com.planeat.planeat.ui.utils.PlanEatNavigationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.text.Normalizer
import java.time.LocalDate


class AppModel(private val maxResult: Int, private val db: RecipesDb) {
    private val connectors: List<Connector>
    var recipesInDb = mutableListOf<Recipe>()
    val recipesSearched = mutableListOf<Recipe>()
    val recipesShown = mutableStateListOf<Recipe>()
    val ingredients = mutableStateListOf<String>()
    val openedRecipe = mutableStateOf<Recipe?>(null)
    var selectedDate = mutableStateOf<LocalDate?>(null)

    init {
        val marmiton = Marmiton(maxResult)
        val ricardo = Ricardo(maxResult)
        val chacuit = ChaCuit(maxResult)
        connectors = listOf(chacuit, ricardo, marmiton)
    }
    private var listJob: Job? = null
    var currentSearchTerm: String = ""

    fun parseQuantity(qty: String?): Float {
        return qty?.let {
            try {
                // Handle fractions like "3/4"
                if (it.contains("/")) {
                    val parts = it.split("/")
                    parts[0].toFloat() / parts[1].toFloat()
                } else {
                    it.toFloat()
                }
            } catch (e: NumberFormatException) {
                1.0f // Default to 1.0 if parsing fails
            }
        } ?: 1.0f  // Default to 1.0 if qty is absent or null
    }

    fun postIngredients(ingredientsData: String): List<IngredientItem> {
        // Use Jsoup to send a POST request
        val response = Jsoup.connect("https://cha-cu.it/parse")
            .method(Connection.Method.POST)
            .header("Content-Type", "text/plain")
            .requestBody(ingredientsData)
            .ignoreContentType(true) // We want to handle the JSON response
            .execute()

        // Get the response body as a string (JSON)
        val jsonResponse = response.body()

        val json = Json { ignoreUnknownKeys = true }

        // Decode the JSON response to a List of ParsedIngredient
        val parsedIngredients = json.decodeFromString<List<ParsedIngredient>>(jsonResponse)

        // Convert ParsedIngredient to List<IngredientItem> with the necessary transformations
        return parsedIngredients.mapNotNull { parsedIngredient ->
            parsedIngredient.name?.let { // Ignore if name is absent
                IngredientItem(
                    quantity = parseQuantity(parsedIngredient.qty),
                    unit = parsedIngredient.unit ?: "",  // Default to empty string if unit is null
                    name = it
                )
            }
        }
    }

    suspend fun gatherIngredients(recipe: Recipe) {
        if (recipe.parsed_ingredients.isEmpty()) {
            val ingredientsData = recipe.ingredients.joinToString("\n")
            recipe.parsed_ingredients = postIngredients(ingredientsData)
            update(recipe)
            return
        }
        // TODO Replace with IA
        val units = listOf("g", "gram[a-zA-Z]*", "kg", "kilo[a-zA-Z]*", "ml", "cl", "dl", "L", "litres?", "boite", "cups", "c[a-zA-Z]* a cafe", "c[a-zA-Z]* a soupe", "paquets?", "verres?", "brins?")
        val unitsPattern = units.joinToString(separator = "|") { "\\b$it\\b" } // Join the units with the OR operator
        val ingredientPattern = """(\d+(\.\d+)?)?\s*($unitsPattern)?\s*(de|d')?\s*(.+)""".toRegex(RegexOption.IGNORE_CASE)

        recipe.ingredients.forEach { ingredient ->
            var normalizedIngredient = Normalizer.normalize(ingredient, Normalizer.Form.NFD).replace("\\p{M}".toRegex(), "")
            normalizedIngredient = normalizedIngredient.replace("(", "")
            normalizedIngredient = normalizedIngredient.replace(")", "")
            normalizedIngredient = normalizedIngredient.replace("\t", " ")
            val matchResult = ingredientPattern.find(normalizedIngredient)
            if (matchResult != null) {
                var (quantity, _, unit, _, ingredientName) = matchResult.destructured
                quantity = quantity.ifEmpty { "1" }
                ingredients.add("Quantity: $quantity, Unit: $unit, Ingredient: $ingredientName")
            } else {
                ingredients.add("Ingredient: $ingredient")
            }
        }
    }

    suspend fun gatherIngredients(recipes: List<Recipe>) {
        recipes.forEach { recipe ->
            gatherIngredients(recipe)
        }
    }

    fun filter(tag: Tags) {
        val completeList = recipesInDb + recipesSearched
        val newList = if (tag == Tags.All) {
            completeList
        } else {
            completeList.filter { recipe -> toTags(recipe).contains(tag) }
        }
        recipesShown.clear()
        for (r in newList) {
            recipesShown.add(r)
        }
    }

    suspend fun remove(recipe: Recipe) {
        recipesInDb.remove(recipe)
        recipesShown.remove(recipe)

        coroutineScope {
            async(Dispatchers.IO) {
                try {
                    db.recipeDao().delete(recipe)
                } catch (error: Exception) {
                    Log.d("PlanEat", "Error: $error")
                }
            }
        }
    }

    suspend fun update(recipe: Recipe) {
        coroutineScope {
            async(Dispatchers.IO) {
                try {
                    db.recipeDao().update(recipe)
                } catch (error: Exception) {
                    Log.d("PlanEat", "Error: $error")
                }
            }
        }
    }

    suspend fun add(recipe: Recipe) {
        coroutineScope {
            launch(Dispatchers.IO) {
                Log.w("PlanEat", "Insert new recipe: ${recipe.title}")
                db.recipeDao().insertAll(recipe)
                val res = db.recipeDao().findByUrl(recipe.url)
                recipesInDb.add(res)
            }
        }
    }

    suspend fun getRecipe(url: String, onRecipe: (Recipe) -> Unit) {
        coroutineScope {
            launch(Dispatchers.IO) {
                for (connector in connectors) {
                    if (connector.handleUrl(url)) {
                        val recipe = connector.getRecipe(url)
                        onRecipe(recipe)
                        break
                    }
                }
            }
        }
    }

    suspend fun search(searchTerm: String): Boolean {
        currentSearchTerm = searchTerm
        listJob?.cancel()
        recipesShown.clear()
        ingredients.clear()
        if (searchTerm.isEmpty()) {
            listJob = coroutineScope {
                async(Dispatchers.IO) {
                    if (recipesInDb.isEmpty()) {
                        recipesInDb = db.recipeDao().getAll().toMutableList()
                    }
                    for (recipe in recipesInDb) {
                        recipesShown.add(recipe)
                    }
                    gatherIngredients(recipesShown)
                }
            }
            return true
        }
        listJob = coroutineScope {
            launch {
                withContext(Dispatchers.IO) {
                    for (recipe in recipesInDb) {
                        if (recipe.title.contains(searchTerm, ignoreCase = true)) {
                            recipesShown.add(recipe)
                        }
                    }
                }
                connectors.map { connector ->
                    async(Dispatchers.IO) {
                        // TODO callback return false?
                        connector.search(searchTerm, onRecipe = { recipe ->
                            if (searchTerm == currentSearchTerm) {
                                Log.w("PlanEat", "Adding recipe $recipe")
                                if (!recipesShown.any { it.url == recipe.url }) {
                                    recipesShown.add(recipe)
                                    recipesSearched.add(recipe)
                                    val scope = CoroutineScope(Job() + Dispatchers.Main)
                                    scope.launch { gatherIngredients(recipe) }
                                }
                            }
                        })
                    }
                }//.awaitAll()

                //bertQaHelper.answer("3 verres de genepi bien agrumé au gout", "ingredient?")
                //bertQaHelper.answer("3 verres de genepi bien agrumé au gout", "quantity?")
                //bertQaHelper.answer("sel et poivre", "ingredient?")
                //bertQaHelper.answer("sel et poivre", "quantity?")

                //var text = "sucre | 50 g de sucre en poudre"
                //var results = client!!.classify(text)
                //Log.d("PlanEat", "@@@ ${text} -> ${results}");
            }
        }
        return true
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PlanEatApp(
    windowSize: WindowSizeClass,
    displayFeatures: List<DisplayFeature>,
) {
    /**
     * This will help us select type of navigation and content type depending on window size and
     * fold state of the device.
     */
    val navigationType: PlanEatNavigationType

    val context = LocalContext.current
    val db = RecipesDb.getDatabase(context)
    val model = AppModel(3, db);

    //model.bertQaHelper = BertQaHelper(context = context, answererListener = model)
    val scope = CoroutineScope(Job() + Dispatchers.Main)


    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            navigationType = PlanEatNavigationType.BOTTOM_NAVIGATION
        }
        WindowWidthSizeClass.Medium -> {
            navigationType = PlanEatNavigationType.NAVIGATION_RAIL
        }
        WindowWidthSizeClass.Expanded -> {
            navigationType = PlanEatNavigationType.NAVIGATION_RAIL
        }
        else -> {
            navigationType = PlanEatNavigationType.BOTTOM_NAVIGATION
        }
    }


    NavigationWrapper(
        model = model,
        onQueryChanged = { value -> scope.launch {
            model.search(value)
        } },
        onRecipeDeleted = {recipe -> scope.launch {
            model.remove(recipe)
        } },
        onRecipeUpdated = {recipe -> scope.launch {
            model.update(recipe)
        } },
        onRecipeAdded = {recipe -> scope.launch { model.add(recipe) } },
        ingredients = model.ingredients,
        navigationType = navigationType,
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun NavigationWrapper(
    model: AppModel,
    onQueryChanged: (String) -> Unit,
    onRecipeDeleted: (Recipe) -> Unit,
    onRecipeUpdated: (Recipe) -> Unit,
    onRecipeAdded: (Recipe) -> Unit,
    ingredients: List<String>,
    navigationType: PlanEatNavigationType,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navController = rememberNavController()
    val navigationActions = remember(navController) {
        PlanEatNavigationActions(navController)
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedDestination =
        navBackStackEntry?.destination?.route ?: PlanEatRoute.AGENDA

    AppContent(
        model = model,
        onQueryChanged = onQueryChanged,
        onRecipeDeleted = onRecipeDeleted,
        onRecipeUpdated = onRecipeUpdated,
        onRecipeAdded = onRecipeAdded,
        ingredients = ingredients,
        navigationType = navigationType,
        navController = navController,
        selectedDestination = selectedDestination,
        navigateToTopLevelDestination = navigationActions::navigateTo,
    ) {
        scope.launch {
            drawerState.open()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppContent(
    modifier: Modifier = Modifier,
    model: AppModel,
    onQueryChanged: (String) -> Unit,
    onRecipeDeleted: (Recipe) -> Unit,
    onRecipeUpdated: (Recipe) -> Unit,
    onRecipeAdded: (Recipe) -> Unit,
    ingredients: List<String>,
    navigationType: PlanEatNavigationType,
    navController: NavHostController,
    selectedDestination: String,
    navigateToTopLevelDestination: (PlanEatTopLevelDestination) -> Unit,
    onDrawerClicked: () -> Unit = {}
) {
    val navTo : (PlanEatTopLevelDestination) -> Unit = { it ->
        model.selectedDate.value = null
        navigateToTopLevelDestination(it)
    }
    Row(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = navigationType == PlanEatNavigationType.NAVIGATION_RAIL) {
            PlanEatNavigationRail(
                selectedDestination = selectedDestination,
                navigateToTopLevelDestination = navTo,
                onDrawerClicked = onDrawerClicked,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            NavHost(
                model = model,
                navController = navController,
                modifier = Modifier.weight(1f),
                onQueryChanged = onQueryChanged,
                onRecipeDeleted = onRecipeDeleted,
                onRecipeUpdated = onRecipeUpdated,
                onRecipeAdded = onRecipeAdded,
                ingredients = ingredients,
                navigateToTopLevelDestination = navTo
            )
            AnimatedVisibility(visible = navigationType == PlanEatNavigationType.BOTTOM_NAVIGATION) {
                PlanEatBottomNavigationBar(
                    selectedDestination = selectedDestination,
                    navigateToTopLevelDestination = navTo,
                    bottomBarState = model.openedRecipe
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun NavHost(
    model: AppModel,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onQueryChanged: (String) -> Unit,
    onRecipeDeleted: (Recipe) -> Unit,
    onRecipeAdded: (Recipe) -> Unit,
    onRecipeUpdated: (Recipe) -> Unit,
    ingredients: List<String>,
    navigateToTopLevelDestination: (PlanEatTopLevelDestination) -> Unit
) {
    val dataSource by remember { mutableStateOf(CalendarDataSource()) }
    var dataUi by remember { mutableStateOf(dataSource.getData(lastSelectedDate = dataSource.today)) }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = PlanEatRoute.AGENDA,
    ) {
        composable(PlanEatRoute.AGENDA) {
            AgendaScreen(dataSource = dataSource, dataUi = dataUi,
                updateDate = { newUi: CalendarUiModel, changePage: Boolean ->
                    dataUi = newUi
                    if (changePage) {
                        val destination = PlanEatTopLevelDestination(
                            route = PlanEatRoute.RECIPES,
                            icon = 0,
                            iconTextId = 0
                        )
                        navigateToTopLevelDestination(destination)
                    }
                    model.selectedDate.value = newUi.selectedDate.date
                },
                goToDetails = { r ->
                    model.openedRecipe.value = r
                    Handler(Looper.getMainLooper()).post {
                        navigateToTopLevelDestination(
                            PlanEatTopLevelDestination(
                                PlanEatRoute.DETAILS,
                                0,
                                0
                            )
                        )
                    }
                },
                goToEdition = { r ->
                    model.openedRecipe.value = r
                    Handler(Looper.getMainLooper()).post {
                        navigateToTopLevelDestination(
                            PlanEatTopLevelDestination(
                                PlanEatRoute.EDITION,
                                0,
                                0
                            )
                        )
                    }
                },
                onRecipeDeleted = onRecipeDeleted)
        }
        composable(PlanEatRoute.RECIPES) {
            RecipesScreen(
                model = model,
                onQueryChanged = onQueryChanged,
                onRecipeDeleted = onRecipeDeleted,
                onRecipeAdded = onRecipeAdded,
                onFilterClicked = { filter ->
                    model.filter(filter)
                },
                dataUi = dataUi,
                goToAgenda = {
                    Handler(Looper.getMainLooper()).post {
                        navigateToTopLevelDestination(
                            PlanEatTopLevelDestination(
                                PlanEatRoute.AGENDA,
                                0,
                                0
                            )
                        )
                    }
                },
                goToDetails = { r ->
                    model.openedRecipe.value = r
                    Handler(Looper.getMainLooper()).post {
                        navigateToTopLevelDestination(
                            PlanEatTopLevelDestination(
                                PlanEatRoute.DETAILS,
                                0,
                                0
                            )
                        )
                    }
                },
                goToEdition = { r ->
                    model.openedRecipe.value = r
                    Handler(Looper.getMainLooper()).post {
                        navigateToTopLevelDestination(
                            PlanEatTopLevelDestination(
                                PlanEatRoute.EDITION,
                                0,
                                0
                            )
                        )
                    }
                }
            )
        }
        composable(PlanEatRoute.PANTRY) {
            EmptyComingSoon()
        }
        composable(PlanEatRoute.SHOPPING_LIST) {
            ShoppingScreen(
                ingredients = ingredients
            )
        }
        composable(PlanEatRoute.DETAILS) {
            if (model.openedRecipe.value != null) {
                RecipeDetailScreen(
                    selectedRecipe = model.openedRecipe.value!!,
                    dataUi = dataUi,
                    model = model,
                    goToAgenda = {
                        model.openedRecipe.value = null
                        Handler(Looper.getMainLooper()).post {
                            navigateToTopLevelDestination(
                                PlanEatTopLevelDestination(
                                    PlanEatRoute.AGENDA,
                                    0,
                                    0
                                )
                            )
                        }
                    },
                    goToEdition = { r ->
                        model.openedRecipe.value = r
                        Handler(Looper.getMainLooper()).post {
                            navigateToTopLevelDestination(
                                PlanEatTopLevelDestination(
                                    PlanEatRoute.EDITION,
                                    0,
                                    0
                                )
                            )
                        }
                    },
                    goBack = {
                        model.openedRecipe.value = null
                        navController.popBackStack()
                    }
                )
            }
        }
        composable(PlanEatRoute.EDITION) {
            if (model.openedRecipe.value != null) {
                EditRecipeScreen(
                    model = model,
                    onRecipeUpdated = { r ->
                        onRecipeUpdated(r)
                        model.openedRecipe.value = null
                        navController.popBackStack()
                    },
                    onRecipeDeleted = { r ->
                        onRecipeDeleted(r)
                        model.openedRecipe.value = null
                        navController.popBackStack()
                    },
                    goBack = {
                        model.openedRecipe.value = null
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
