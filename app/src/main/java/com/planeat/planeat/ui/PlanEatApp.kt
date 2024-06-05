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

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import com.planeat.planeat.ai.client.BertQaHelper
import com.planeat.planeat.connectors.ChaCuit
import com.planeat.planeat.connectors.Connector
import com.planeat.planeat.connectors.Marmiton
import com.planeat.planeat.connectors.Ricardo
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.ui.navigation.ModalNavigationDrawerContent
import com.planeat.planeat.ui.navigation.PermanentNavigationDrawerContent
import com.planeat.planeat.ui.navigation.PlanEatBottomNavigationBar
import com.planeat.planeat.ui.navigation.PlanEatNavigationActions
import com.planeat.planeat.ui.navigation.PlanEatNavigationRail
import com.planeat.planeat.ui.navigation.PlanEatRoute
import com.planeat.planeat.ui.navigation.PlanEatTopLevelDestination
import com.planeat.planeat.ui.utils.DevicePosture
import com.planeat.planeat.ui.utils.PlanEatContentType
import com.planeat.planeat.ui.utils.PlanEatNavigationContentPosition
import com.planeat.planeat.ui.utils.PlanEatNavigationType
import com.planeat.planeat.ui.utils.isBookPosture
import com.planeat.planeat.ui.utils.isSeparating
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.text.qa.QaAnswer
import java.text.Normalizer


class AppModel(private val maxResult: Int, private val db: RecipesDb) : BertQaHelper.AnswererListener {
    private val connectors: List<Connector>
    val recipes = mutableStateListOf<Recipe>()
    val ingredients = mutableStateListOf<String>()
    lateinit var bertQaHelper: BertQaHelper

    init {
        val marmiton = Marmiton(maxResult)
        val ricardo = Ricardo(maxResult)
        val chacuit = ChaCuit(maxResult)
        connectors = listOf(/*chacuit,*/ ricardo, marmiton)
    }
    private var listJob: Job? = null
    private var currentSearchTerm: String = ""

    fun gatherIngredients(recipe: Recipe) {
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
               // val words = normalizedIngredient.split(" ")
               // for (word in words) {
               //     Log.d("PlanEat", "@@@ $word | $normalizedIngredient | $quantity | $unit | $ingredientName")
               // }

                quantity = quantity.ifEmpty { "1" }
                ingredients.add("Quantity: $quantity, Unit: $unit, Ingredient: $ingredientName")
            } else {
                ingredients.add("Ingredient: $ingredient")
            }
        }
    }

    fun gatherIngredients(recipes: List<Recipe>) {
        recipes.forEach { recipe ->
            gatherIngredients(recipe)
        }
    }

    suspend fun search(searchTerm: String): Boolean {
        currentSearchTerm = searchTerm
        listJob?.cancel()
        recipes.clear()
        ingredients.clear()
        if (searchTerm.isEmpty()) {
            listJob = coroutineScope {
                async(Dispatchers.IO) {
                    for (recipe in db.recipeDao().getAll()) {
                        recipes.add(recipe)
                    }
                    gatherIngredients(recipes)
                }
            }
            return true
        }
        listJob = coroutineScope {
            launch {
                connectors.map { connector ->
                    async(Dispatchers.IO) {
                        // TODO callback return false?
                        connector.search(searchTerm, onRecipe = { recipe ->
                            if (searchTerm == currentSearchTerm) {
                                Log.w("PlanEat", "Adding recipe $recipe")
                                recipes.add(recipe)
                                gatherIngredients(recipe)
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

    override fun onError(error: String) {
        Log.e("PlanEat", error)
    }

    override fun onResults(contextOfQuestion: String, question: String, results: List<QaAnswer>?, inferenceTime: Long) {
        results?.first()?.let {
            Log.d("PlanEat", it.text)
        }
        Log.d("PlanEat", inferenceTime.toString())
    }
}


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
    val contentType: PlanEatContentType

    val context = LocalContext.current
    val db = Room.databaseBuilder(
        context,
        RecipesDb::class.java, "RecipesDb"
    ).build()
    val model = AppModel(3, db);

    //model.bertQaHelper = BertQaHelper(context = context, answererListener = model)
    val scope = CoroutineScope(Job() + Dispatchers.Main)

    /**
     * We are using display's folding features to map the device postures a fold is in.
     * In the state of folding device If it's half fold in BookPosture we want to avoid content
     * at the crease/hinge
     */
    val foldingFeature = displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()

    val foldingDevicePosture = when {
        isBookPosture(foldingFeature) ->
            DevicePosture.BookPosture(foldingFeature.bounds)

        isSeparating(foldingFeature) ->
            DevicePosture.Separating(foldingFeature.bounds, foldingFeature.orientation)

        else -> DevicePosture.NormalPosture
    }

    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            navigationType = PlanEatNavigationType.BOTTOM_NAVIGATION
            contentType = PlanEatContentType.SINGLE_PANE
        }
        WindowWidthSizeClass.Medium -> {
            navigationType = PlanEatNavigationType.NAVIGATION_RAIL
            contentType = if (foldingDevicePosture != DevicePosture.NormalPosture) {
                PlanEatContentType.DUAL_PANE
            } else {
                PlanEatContentType.SINGLE_PANE
            }
        }
        WindowWidthSizeClass.Expanded -> {
            navigationType = if (foldingDevicePosture is DevicePosture.BookPosture) {
                PlanEatNavigationType.NAVIGATION_RAIL
            } else {
                PlanEatNavigationType.PERMANENT_NAVIGATION_DRAWER
            }
            contentType = PlanEatContentType.DUAL_PANE
        }
        else -> {
            navigationType = PlanEatNavigationType.BOTTOM_NAVIGATION
            contentType = PlanEatContentType.SINGLE_PANE
        }
    }

    /**
     * Content inside Navigation Rail/Drawer can also be positioned at top, bottom or center for
     * ergonomics and reachability depending upon the height of the device.
     */
    val navigationContentPosition = when (windowSize.heightSizeClass) {
        WindowHeightSizeClass.Compact -> {
            PlanEatNavigationContentPosition.TOP
        }
        WindowHeightSizeClass.Medium,
        WindowHeightSizeClass.Expanded -> {
            PlanEatNavigationContentPosition.CENTER
        }
        else -> {
            PlanEatNavigationContentPosition.TOP
        }
    }

    NavigationWrapper(
        onQueryChanged = { value -> scope.launch {
            model.search(value)
        } },
        recipes = model.recipes,
        ingredients = model.ingredients,
        navigationType = navigationType,
        contentType = contentType,
        navigationContentPosition = navigationContentPosition,
    )
}

@Composable
private fun NavigationWrapper(
    onQueryChanged: (String) -> Unit,
    recipes: List<Recipe>,
    ingredients: List<String>,
    navigationType: PlanEatNavigationType,
    contentType: PlanEatContentType,
    navigationContentPosition: PlanEatNavigationContentPosition,
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

    if (navigationType == PlanEatNavigationType.PERMANENT_NAVIGATION_DRAWER) {
        // TODO check on custom width of PermanentNavigationDrawer: b/232495216
        PermanentNavigationDrawer(drawerContent = {
            PermanentNavigationDrawerContent(
                selectedDestination = selectedDestination,
                navigationContentPosition = navigationContentPosition,
                navigateToTopLevelDestination = navigationActions::navigateTo,
            )
        }) {
            AppContent(
                onQueryChanged = onQueryChanged,
                recipes = recipes,
                ingredients = ingredients,
                navigationType = navigationType,
                contentType = contentType,
                navigationContentPosition = navigationContentPosition,
                navController = navController,
                selectedDestination = selectedDestination,
                navigateToTopLevelDestination = navigationActions::navigateTo,
            )
        }
    } else {
        ModalNavigationDrawer(
            drawerContent = {
                ModalNavigationDrawerContent(
                    selectedDestination = selectedDestination,
                    navigationContentPosition = navigationContentPosition,
                    navigateToTopLevelDestination = navigationActions::navigateTo,
                    onDrawerClicked = {
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )
            },
            drawerState = drawerState
        ) {
            AppContent(
                onQueryChanged = onQueryChanged,
                recipes = recipes,
                ingredients = ingredients,
                navigationType = navigationType,
                contentType = contentType,
                navigationContentPosition = navigationContentPosition,
                navController = navController,
                selectedDestination = selectedDestination,
                navigateToTopLevelDestination = navigationActions::navigateTo,
            ) {
                scope.launch {
                    drawerState.open()
                }
            }
        }
    }
}

@Composable
fun AppContent(
    modifier: Modifier = Modifier,
    onQueryChanged: (String) -> Unit,
    recipes: List<Recipe>,
    ingredients: List<String>,
    navigationType: PlanEatNavigationType,
    contentType: PlanEatContentType,
    navigationContentPosition: PlanEatNavigationContentPosition,
    navController: NavHostController,
    selectedDestination: String,
    navigateToTopLevelDestination: (PlanEatTopLevelDestination) -> Unit,
    onDrawerClicked: () -> Unit = {}
) {
    Row(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = navigationType == PlanEatNavigationType.NAVIGATION_RAIL) {
            PlanEatNavigationRail(
                selectedDestination = selectedDestination,
                navigationContentPosition = navigationContentPosition,
                navigateToTopLevelDestination = navigateToTopLevelDestination,
                onDrawerClicked = onDrawerClicked,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            NavHost(
                navController = navController,
                contentType = contentType,
                navigationType = navigationType,
                modifier = Modifier.weight(1f),
                onQueryChanged = onQueryChanged,
                recipes = recipes,
                ingredients = ingredients,
            )
            AnimatedVisibility(visible = navigationType == PlanEatNavigationType.BOTTOM_NAVIGATION) {
                PlanEatBottomNavigationBar(
                    selectedDestination = selectedDestination,
                    navigateToTopLevelDestination = navigateToTopLevelDestination
                )
            }
        }
    }
}

@Composable
private fun NavHost(
    navController: NavHostController,
    contentType: PlanEatContentType,
    navigationType: PlanEatNavigationType,
    modifier: Modifier = Modifier,
    onQueryChanged: (String) -> Unit,
    recipes: List<Recipe>,
    ingredients: List<String>,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = PlanEatRoute.AGENDA,
    ) {
        composable(PlanEatRoute.AGENDA) {
            AgendaScreen()
        }
        composable(PlanEatRoute.RECIPES) {
            RecipesScreen(
                contentType = contentType,
                navigationType = navigationType,
                onQueryChanged = onQueryChanged,
                recipes = recipes,
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
    }
}
