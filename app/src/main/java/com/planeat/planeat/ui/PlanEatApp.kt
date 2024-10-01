package com.planeat.planeat.ui

import android.content.Context
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
import com.planeat.planeat.connectors.ChaCuit
import com.planeat.planeat.connectors.Connector
import com.planeat.planeat.connectors.Marmiton
import com.planeat.planeat.connectors.Nytimes
import com.planeat.planeat.connectors.Ricardo
import com.planeat.planeat.data.IngredientItem
import com.planeat.planeat.data.IngredientsDb
import com.planeat.planeat.data.ParsedIngredient
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.data.Tags
import com.planeat.planeat.data.toIngredientCategory
import com.planeat.planeat.data.toTags
import com.planeat.planeat.ui.components.calendar.CalendarDataSource
import com.planeat.planeat.ui.components.calendar.CalendarUiModel
import com.planeat.planeat.ui.navigation.PlanEatBottomNavigationBar
import com.planeat.planeat.ui.navigation.PlanEatNavigationActions
import com.planeat.planeat.ui.navigation.PlanEatNavigationRail
import com.planeat.planeat.ui.navigation.PlanEatRoute
import com.planeat.planeat.ui.navigation.PlanEatTopLevelDestination
import com.planeat.planeat.ui.utils.IngredientClassifier
import com.planeat.planeat.ui.utils.PlanEatNavigationType
import com.planeat.planeat.ui.utils.TagClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.io.FileWriter
import java.net.SocketTimeoutException
import java.time.LocalDate


class AppModel(private val maxResult: Int, private val db: RecipesDb, private val context: Context) {
    private val connectors: List<Connector>

    var currentTag = mutableStateOf(Tags.All)

    var recipesInDb = mutableListOf<Recipe>()
    var recipesInDbShown = mutableStateListOf<Recipe>()
    val recipesSearched = mutableListOf<Recipe>()
    val recipesSearchedShown = mutableStateListOf<Recipe>()
    val suggestedRecipes = mutableStateListOf<Recipe>()
    val suggestedRecipesShown = mutableStateListOf<Recipe>()

    val openedRecipe = mutableStateOf<Recipe?>(null)
    var selectedDate = mutableStateOf<LocalDate?>(null)

    var searchJobs = mutableListOf<Job>()

    private val hasConnection = mutableStateOf(true)
    private val checkConnectionJob = mutableStateOf<Job?>(null)


    fun writeRecipesToFile(recipes: List<Recipe>, fileName: String) {
        try {
            // Convert the list to JSON string
            val jsonString = Json.encodeToString(ListSerializer(Recipe.serializer()), recipes)

            // Write the JSON string to a file
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace() // Handle any errors
        }
    }

    fun loadRecipesFromFile(fileName: String): List<Recipe>? {
        return try {
            // Read the JSON string from the file
            val jsonString = context.openFileInput(fileName).bufferedReader().use { it.readText() }

            // Convert the JSON string back into a list of Recipe objects
            Json.decodeFromString<List<Recipe>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null // Return null if there's an error
        }
    }

    // Function to get the last modified time of the file
    fun getLastModifiedTime(fileName: String): Long {
        val file = File(context.filesDir, fileName)

        // Check if the file exists
        if (file.exists()) {
            return file.lastModified() // Return the last modified time in milliseconds
        }
        return 0L // Return 0 if the file doesn't exist
    }

    fun isMoreThanOneWeekSinceLastModified(fileName: String): Boolean {
        val lastModifiedTime = getLastModifiedTime(fileName)
        val currentTime = System.currentTimeMillis()

        // A week in milliseconds
        val oneWeekInMillis = 7 * 24 * 60 * 60 * 1000

        // Check if more than one week has passed
        return (currentTime - lastModifiedTime) > oneWeekInMillis
    }


    init {
        val marmiton = Marmiton(maxResult)
        val ricardo = Ricardo(maxResult)
        val chacuit = ChaCuit(maxResult)
        val nytimes = Nytimes(maxResult)
        connectors = listOf(chacuit, ricardo, marmiton, nytimes)
    }

    suspend fun gigaDataset() {
        val categoriesUrl = mutableMapOf<Tags, List<String>>()
        categoriesUrl[Tags.Appetizer] = listOf("https://www.marmiton.org/recettes/index/categorie/aperitif-ou-buffet/PAGE")
        categoriesUrl[Tags.Healthy] = listOf("https://www.ricardocuisine.com/themes/sante?sort=recent&content-type=theme&grouping-type=4&grouping-id=60&currentPage=PAGE")
        categoriesUrl[Tags.MiddleEastern] = listOf("https://www.marmiton.org/recettes/selection_merveilles-du-maghreb-et-de-l-orient.aspx?p=PAGE", "https://www.ricardocuisine.com/themes/mediterraneen?searchValue=&sort=recent&content-type=theme&grouping-type=4&grouping-id=2447&currentPage=PAGE")
        categoriesUrl[Tags.Drinks] = listOf("https://www.marmiton.org/recettes?type=boisson&page=PAGE")
        categoriesUrl[Tags.American] = listOf("https://www.ricardocuisine.com/themes/cuisine-americaine?searchValue=&sort=recent&content-type=theme&grouping-type=4&grouping-id=2545&currentPage=PAGE")
        categoriesUrl[Tags.Seafood] = listOf("https://www.ricardocuisine.com/themes/bord-de-mer?searchValue=&sort=recent&content-type=theme&grouping-type=4&grouping-id=2540&currentPage=PAGE")
        categoriesUrl[Tags.Bakery] = listOf("https://www.ricardocuisine.com/themes/meilleurs-gateaux?searchValue=&sort=recent&content-type=theme&grouping-type=4&grouping-id=2286&currentPage=PAGE")
        categoriesUrl[Tags.European] = listOf("https://www.marmiton.org/recettes/selection_recettes_portugaises.aspx?p=PAGE", "https://www.marmiton.org/recettes/selection_belgique.aspx?p=PAGE", "https://www.marmiton.org/recettes/selection_recettes_grecques.aspx?p=PAGE", "https://www.marmiton.org/recettes/selection_recettes_francaises.aspx?p=PAGE", "https://www.ricardocuisine.com/themes/cuisine-espagnole?searchValue=&sort=recent&content-type=theme&grouping-type=4&grouping-id=2552&currentPage=PAGE", "https://www.ricardocuisine.com/themes/cuisine-italienne?searchValue=&sort=recent&content-type=theme&grouping-type=4&grouping-id=2429&currentPage=PAGE")
        categoriesUrl[Tags.Asian] = listOf(" https://www.ricardocuisine.com/themes/cuisine-chinoise?searchValue=&sort=recent&content-type=theme&grouping-type=4&grouping-id=2663&currentPage=PAGE", "https://www.ricardocuisine.com/themes/cuisine-coreenne?searchValue=&sort=recent&content-type=theme&grouping-type=4&grouping-id=2661&currentPage=PAGE", "https://www.ricardocuisine.com/themes/cuisine-japonaise?searchValue=&sort=recent&content-type=theme&grouping-type=4&grouping-id=2660&currentPage=PAGE")
        categoriesUrl[Tags.Vegetarian] = listOf("https://www.ricardocuisine.com/themes/vegetarien?searchValue=&sort=recent&content-type=theme&grouping-type=4&grouping-id=2547&currentPage=PAGE")
        categoriesUrl[Tags.Desserts] = listOf("https://www.marmiton.org/recettes/index/categorie/dessert/PAGE")
        categoriesUrl[Tags.ComfortFood] = listOf("https://www.ricardocuisine.com/themes/comfort-food?searchValue=&sort=recent&content-type=theme&grouping-type=4&grouping-id=2266&currentPage=PAGE")

        var dataset = mutableMapOf<Recipe, List<Tags>>()
        coroutineScope {

            val marmiton = Marmiton(3)
            val ricardo = Ricardo(3)
            var i = 0
            launch(Dispatchers.IO) {
                categoriesUrl.forEach {tag, listUrls ->

                    val onRecipe: (Recipe) -> Unit = { r->
                        val result = emptyList<Tags>().toMutableList()
                        result += tag
                        if (r.tags.any { it.contains("boisson") })
                            result += Tags.Drinks
                        if (r.tags.any { it.contains("maroc") })
                            result += Tags.MiddleEastern
                        if (r.tags.any { it.contains("oriental") })
                            result += Tags.MiddleEastern
                        if (r.tags.any { it.contains("amuse-gueule") })
                            result += Tags.Appetizer
                        if (r.tags.any { it.contains("comfort") })
                            result += Tags.ComfortFood
                        if (r.tags.any { it.contains("sant") })
                            result += Tags.Healthy
                        if (r.tags.any { it.contains("fruits de mer") } || r.tags.any { it.contains("crustacés") })
                            result += Tags.Seafood
                        if (r.tags.any { it.contains("méditerranée") })
                            result += Tags.MiddleEastern
                        if (r.tags.any { it.contains("asia") } || r.tags.any { it.contains("japonais") })
                            result += Tags.Asian
                        if (r.tags.any { it.contains("été") })
                            result += Tags.ComfortFood
                        if (r.tags.any { it.contains("américain") })
                            result += Tags.American
                        if (r.tags.any { it.contains("grec") } || r.tags.any { it.contains("italie") } || r.tags.any { it.contains("portugua") } || r.tags.any { it.contains("fran") })
                            result += Tags.European
                        if (r.tags.any { it.contains("végétarien") })
                            result += Tags.Vegetarian
                        if (r.tags.any { it.contains("dessert") } || r.tags.any { it.contains("gâteau") })
                            result += Tags.Desserts
                        synchronized(dataset) {
                            dataset[r] = result
                            i += 1
                            Log.d("PlanEat", "Dataset size: ${i}")
                            if (dataset.size >= 100) {
                                dataset.forEach { recipe, tags ->
                                    val file = File(context.filesDir, "input.csv")
                                    FileWriter(file, true).use { writer ->
                                        dataset.forEach { recipe, tags ->
                                            // Convert recipe to JSON string and tags to a string separated by commas
                                            val recipeJson = recipe.toSmallJson() // Assuming this is your JSON conversion method
                                            val tagsString = tags.distinct().joinToString(",") // Convert list of tags to comma-separated string

                                            // Append to the CSV file, separating the columns with '@'
                                            writer.appendLine("$recipeJson@$tagsString")
                                        }
                                    }
                                }
                                dataset.clear()
                            }
                        }
                    }

                    listUrls.forEach { url ->
                        if (url.contains("marmiton")) {
                            marmiton.parsePages(url, onRecipe)
                        } else if (url.contains("ricardo")) {
                            ricardo.parsePages(url, onRecipe)
                        }
                    }
                }
            }
        }
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
        if (!hasConnection.value)
            return emptyList()

        val ic = IngredientClassifier()
        val db = IngredientsDb.getDatabase(context)
        try {
            // Use Jsoup to send a POST request
            val response = Jsoup.connect("https://cha-cu.it/parse")
                .method(Connection.Method.POST)
                .header("Content-Type", "text/plain")
                .requestBody(ingredientsData)
                .timeout(2000)
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
                        name = it,
                        category = toIngredientCategory(it, ic, db)
                    )
                }
            }
        } catch (error: SocketTimeoutException) {
            hasConnection.value = false
            startCheckConnection()
        } catch(error: Exception) {
            Log.d("PlanEat", "Error: $error")
        }
        return emptyList()
    }

    private fun startCheckConnection() {
        if (checkConnectionJob.value == null) { // Only start if no job is running
            val scope = CoroutineScope(Dispatchers.Default)

            // Launch a job that checks the connection every minute
            checkConnectionJob.value = scope.launch {
                while (!hasConnection.value) {
                    // Perform your connection check here
                    Log.e("PlanEat", "Checking connection to parse ingredients.")

                    // Wait for 1 minute
                    delay(60 * 1000L)

                    try {
                        // Use Jsoup to send a POST request
                        val response = Jsoup.connect("https://cha-cu.it/parse")
                            .method(Connection.Method.POST)
                            .header("Content-Type", "text/plain")
                            .requestBody("eggs")
                            .timeout(2000)
                            .ignoreContentType(true) // We want to handle the JSON response
                            .execute()

                        // Get the response body as a string (JSON)
                        response.body()
                        hasConnection.value = true
                        Log.d("PlanEat", "Connection to parse ingredients is back!")
                    } catch (error: Exception) {
                        Log.d("PlanEat", "Error: $error")
                        continue
                    }
                }

                // When hasConnection becomes false, stop the job and set it to null
                println("No connection, stopping job.")
                checkConnectionJob.value = null
            }
        }
    }

    suspend fun classifyRecipeAndIngredients(recipe: Recipe) {
        if (recipe.parsed_ingredients.isEmpty()) {
            val ingredientsData = recipe.ingredients.joinToString("\n")
            recipe.parsed_ingredients = postIngredients(ingredientsData)
            recipe.tags = TagClassifier(context).classify(recipe.toSmallJson().toString())
            update(recipe)
            return
        }
    }

    suspend fun classifyRecipeAndIngredients(recipes: List<Recipe>) {
        recipes.forEach { recipe ->
            classifyRecipeAndIngredients(recipe)
        }
    }

    fun filter(tag: Tags) {
        currentTag.value = tag

        // Update my recipes
        var newList = if (tag == Tags.All) {
            recipesInDb
        } else {
            recipesInDb.filter { recipe -> toTags(recipe).contains(tag) }
        }
        recipesInDbShown.clear()
        for (r in newList) {
            recipesInDbShown.add(r)
        }
        // Update results
        newList = if (tag == Tags.All) {
            recipesSearched
        } else {
            recipesSearched.filter { recipe -> toTags(recipe).contains(tag) }
        }
        recipesSearchedShown.clear()
        for (r in newList) {
            recipesSearchedShown.add(r)
        }
        // Update suggested
        newList = if (tag == Tags.All) {
            suggestedRecipes
        } else {
            suggestedRecipes.filter { recipe -> toTags(recipe).contains(tag) }
        }
        suggestedRecipesShown.clear()
        for (r in newList) {
            suggestedRecipesShown.add(r)
        }
    }

    suspend fun remove(recipe: Recipe) {
        recipesInDb.remove(recipe)
        recipesInDbShown.remove(recipe)

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

    suspend fun update(recipe: Recipe, addRecipe: Boolean = false) {
        if (recipe.recipeId != 0.toLong()) {
            coroutineScope {
                async(Dispatchers.IO) {
                    try {
                        db.recipeDao().update(recipe)
                        classifyRecipeAndIngredients(recipe)
                    } catch (error: Exception) {
                        Log.d("PlanEat", "Error: $error")
                    }
                }
            }
        } else if (addRecipe) {
            coroutineScope {
                async(Dispatchers.IO) {
                    try {
                        db.recipeDao().insertAll(recipe)
                        val res = db.recipeDao().findByUrl(recipe.url)
                        if (res != null) {
                            recipesInDb.add(res)
                            recipesInDb.sortWith(compareBy({ -it.planified }, { it.title }))
                            recipesInDbShown.add(recipe)
                            recipesInDbShown.sortWith(compareBy({ -it.planified }, { it.title }))
                            recipe.recipeId = res.recipeId
                            classifyRecipeAndIngredients(recipe)
                        }
                    } catch (error: Exception) {
                        Log.d("PlanEat", "Error: $error")
                    }
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
                recipesInDb.sortWith(compareBy({ -it.planified }, { it.title }))
                // Update visibiliy
                if (recipesSearchedShown.any { it.url == recipe.url }) {
                    recipesSearchedShown.remove(recipe)
                }
                if (suggestedRecipes.any { it.url == recipe.url }) {
                    suggestedRecipes.remove(recipe)
                    suggestedRecipesShown.remove(recipe)
                }
                recipesInDbShown.add(res)
                classifyRecipeAndIngredients(res)
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

    suspend fun initSuggestions() {
        coroutineScope {
            launch {
                withContext(Dispatchers.IO) {
                    if (recipesInDb.isEmpty()) {

                        recipesInDb = db.recipeDao().getAll().toMutableList()
                        recipesInDb.sortWith(compareBy({ -it.planified }, { it.title }))
                    }

                    if (suggestedRecipes.isEmpty()) {
                        if (!isMoreThanOneWeekSinceLastModified(
                                "suggestions.json"
                            )
                        ) {
                            val suggestions =
                                loadRecipesFromFile("suggestions.json")
                            suggestions?.forEach { recipe ->
                                if (!recipesInDb.any { it.url == recipe.url }) {
                                    suggestedRecipes.add(recipe)
                                    suggestedRecipesShown.add(recipe)
                                    if (recipe.parsed_ingredients.isEmpty()) {
                                        classifyRecipeAndIngredients(recipe)
                                        writeRecipesToFile(
                                            suggestedRecipes,
                                            "suggestions.json"
                                        )
                                    }
                                }
                            }
                        }
                        // If nothing cached
                        if (suggestedRecipes.isEmpty()) {
                            connectors.map { connector ->
                                searchJobs += launch(Dispatchers.IO) {
                                    // If no suggestions, find new one
                                    connector.suggest(onRecipe = { recipe ->
                                        // Add new recipes to results
                                        if (!recipesInDb.any { it.url == recipe.url }) {
                                            suggestedRecipes.add(recipe)
                                            suggestedRecipesShown.add(recipe)
                                            launch(Dispatchers.IO) {
                                                classifyRecipeAndIngredients(recipe)
                                                writeRecipesToFile(
                                                    suggestedRecipes,
                                                    "suggestions.json"
                                                )
                                            }
                                        }
                                    })
                                    // Cache it
                                    writeRecipesToFile(
                                        suggestedRecipes,
                                        "suggestions.json"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun search(searchTerm: String, context: Context, noExternal: Boolean): Boolean {
        // Cancel current search and refresh recipesShown()
        currentSearchTerm = searchTerm
        listJob?.cancel()
        searchJobs.forEach { it.cancel() }
        searchJobs.clear()
        recipesInDbShown.clear()
        recipesSearchedShown.clear()
        // If empty show recipes in database
        if (searchTerm.isEmpty()) {
            listJob = coroutineScope {
                async(Dispatchers.IO) {
                    for (recipe in recipesInDb) {
                        recipesInDbShown.add((recipe))
                    }
                }
            }
            return true
        }
        // Else perform a search
        listJob = coroutineScope {
            launch {
                withContext(Dispatchers.IO) {
                    for (recipe in recipesInDb) {
                        if (recipe.title.contains(searchTerm, ignoreCase = true)) {
                            recipesInDbShown.add(recipe)
                        }
                    }
                }
                if (!noExternal) {
                    connectors.map { connector ->
                        searchJobs += launch(Dispatchers.IO) {
                            connector.search(searchTerm, onRecipe = { recipe ->
                                if (searchTerm == currentSearchTerm) {
                                    Log.w("PlanEat", "Adding recipe $recipe")
                                    // Add new recipes to results
                                    if (!recipesInDbShown.any { it.url == recipe.url }) {
                                        recipesSearchedShown.add(recipe)
                                        recipesSearched.add(recipe)
                                    }
                                }
                            })
                        }
                    }
                }
            }
        }
        return true
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PlanEatApp(
    windowSize: WindowSizeClass,
) {
    /**
     * This will help us select type of navigation and content type depending on window size and
     * fold state of the device.
     */
    val navigationType: PlanEatNavigationType

    val context = LocalContext.current
    val db = RecipesDb.getDatabase(context)
    val model = AppModel(3, db, context);

    //model.bertQaHelper = BertQaHelper(context = context, answererListener = model)
    val scope = CoroutineScope(Job() + Dispatchers.Main)

    scope.launch {

       // model.gigaDataset()
        model.initSuggestions()
    }


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
        onQueryChanged = { value, noExternal -> scope.launch {
            model.search(value, context, noExternal)
        } },
        onRecipeDeleted = {recipe -> scope.launch {
            model.remove(recipe)
        } },
        onRecipeUpdated = {recipe -> scope.launch {
            model.update(recipe, true)
        } },
        onRecipeAdded = {recipe -> scope.launch { model.add(recipe) } },
        navigationType = navigationType,
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun NavigationWrapper(
    model: AppModel,
    onQueryChanged: (String, Boolean) -> Unit,
    onRecipeDeleted: (Recipe) -> Unit,
    onRecipeUpdated: (Recipe) -> Unit,
    onRecipeAdded: (Recipe) -> Unit,
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
    onQueryChanged: (String, Boolean) -> Unit,
    onRecipeDeleted: (Recipe) -> Unit,
    onRecipeUpdated: (Recipe) -> Unit,
    onRecipeAdded: (Recipe) -> Unit,
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
    onQueryChanged: (String, Boolean) -> Unit,
    onRecipeDeleted: (Recipe) -> Unit,
    onRecipeAdded: (Recipe) -> Unit,
    onRecipeUpdated: (Recipe) -> Unit,
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
                        navigateToTopLevelDestination(
                            PlanEatTopLevelDestination(
                                PlanEatRoute.SAVED,
                                0,
                                0
                            )
                        )
                    }
                    model.selectedDate.value = newUi.selectedDate.date
                },
                goToDetails = { r ->
                    model.openedRecipe.value = r
                    Handler(Looper.getMainLooper()).post {
                        navController.navigate(PlanEatRoute.DETAILS)
                    }
                },
                goToEdition = { r ->
                    model.openedRecipe.value = r
                    Handler(Looper.getMainLooper()).post {
                        navController.navigate(PlanEatRoute.EDITION)
                    }
                },
                goToAccount = {
                    Handler(Looper.getMainLooper()).post {
                        navController.navigate(PlanEatRoute.ACCOUNT)
                    }
                },
                onRecipeDeleted = onRecipeDeleted)
        }
        composable(PlanEatRoute.ACCOUNT) {
            AccountScreen()
        }
        composable(PlanEatRoute.SEARCH) {
            DiscoverScreen(
                model = model,
                onQueryChanged = onQueryChanged,
                onRecipeDeleted = onRecipeDeleted,
                onRecipeAdded = onRecipeAdded,
                onFilterClicked = { filter ->
                    model.filter(filter)
                },
                dataUi = dataUi,
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
                goToDetails = { r ->
                    model.openedRecipe.value = r
                    Handler(Looper.getMainLooper()).post {
                        navController.navigate(PlanEatRoute.DETAILS)
                    }
                },
                goToEdition = { r ->
                    model.openedRecipe.value = r
                    Handler(Looper.getMainLooper()).post {
                        navController.navigate(PlanEatRoute.EDITION)
                    }
                }
            )
        }
        composable(PlanEatRoute.SAVED) {
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
                goToDetails = { r ->
                    model.openedRecipe.value = r
                    Handler(Looper.getMainLooper()).post {
                        navController.navigate(PlanEatRoute.DETAILS)
                    }
                },
                goToEdition = { r ->
                    model.openedRecipe.value = r
                    Handler(Looper.getMainLooper()).post {
                        navController.navigate(PlanEatRoute.EDITION)
                    }
                }
            )
        }
        composable(PlanEatRoute.SHOPPING_LIST) {
            ShoppingScreen(
                onRecipeSelected = { r ->
                    model.openedRecipe.value = r
                    Handler(Looper.getMainLooper()).post {
                        navController.navigate(PlanEatRoute.DETAILS)
                    }
                },
                goToAgenda = {
                    model.openedRecipe.value = null
                    Handler(Looper.getMainLooper()).post {
                        navController.navigate(PlanEatRoute.AGENDA)
                    }
                }
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
                            navController.popBackStack()
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
                            navController.navigate(PlanEatRoute.EDITION)
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
                        val isDetailsPage = navController.currentBackStackEntry?.destination?.route == PlanEatRoute.DETAILS
                        if (isDetailsPage) {
                            navController.popBackStack()
                        }
                    },
                    onRecipeDeleted = { r ->
                        onRecipeDeleted(r)
                        model.openedRecipe.value = null
                        navController.popBackStack()
                        val isDetailsPage = navController.currentBackStackEntry?.destination?.route == PlanEatRoute.DETAILS
                        if (isDetailsPage) {
                            navController.popBackStack()
                        }
                    },
                    goBack = {
                        model.openedRecipe.value = null
                        navController.popBackStack()
                        val isDetailsPage = navController.currentBackStackEntry?.destination?.route == PlanEatRoute.DETAILS
                        if (isDetailsPage) {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}
