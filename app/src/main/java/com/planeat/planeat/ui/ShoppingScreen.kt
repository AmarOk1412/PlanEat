package com.planeat.planeat.ui

import ShoppingIngredient
import ShoppingList
import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.compose.onSurfaceVariantLight
import com.example.compose.outlineLight
import com.example.compose.primaryContainerLight
import com.example.compose.surfaceContainerLowestLight
import com.planeat.planeat.R
import com.planeat.planeat.data.AgendaDb
import com.planeat.planeat.data.Ingredient
import com.planeat.planeat.data.IngredientItem
import com.planeat.planeat.data.IngredientsDb
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.data.toIngredientIcon
import com.planeat.planeat.ui.components.MinimalRecipeItemList
import com.planeat.planeat.ui.utils.IngredientClassifier
import com.planeat.planeat.ui.utils.Translator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ShoppingScreen(
    modifier: Modifier = Modifier,
    onRecipeSelected: (Recipe) -> Unit,
    goToAgenda: () -> Unit
) {
    val context = LocalContext.current
    val ic = IngredientClassifier()

    var shoppingList by remember {
        mutableStateOf<ShoppingList?>(null)
    }
    var sortingMethod by remember {
        mutableStateOf("")
    }

    var searchItem by remember {
        mutableStateOf(false)
    }

    // Fetch data in LaunchedEffect and update state
    LaunchedEffect(Unit) {
        searchItem = false
        withContext(Dispatchers.IO) {
            val adb = AgendaDb.getDatabase(context)
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
            val ingredientsDb = IngredientsDb.getDatabase(context)

            val sh = ShoppingList(planned, rdb, ingredientsDb, ic, context)
            sh.saveLoadFromDisk()
            shoppingList = sh
            sortingMethod = shoppingList!!.sortingMethod
        }
    }

    if (searchItem == false) {
        // UI section that uses the planned recipes and ingredient data
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            contentWindowInsets = WindowInsets(0.dp),
            floatingActionButton = {
                FloatingActionButton(onClick = { searchItem = true },
                    containerColor = Color(0xFF599e39),
                    contentColor = Color(0xFFFFFFFF),
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.padding(end = 16.dp).size(56.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_ingredient)
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

                        // Display number of planned recipes
                        Text(
                            text = LocalContext.current.resources.getQuantityString(
                                R.plurals.recipes,
                                shoppingList!!.plannedRecipesSize(),
                                shoppingList!!.plannedRecipesSize()
                            ),
                            style = MaterialTheme.typography.headlineSmall
                        )

                        // Row for the planned recipes
                        if (shoppingList!!.plannedRecipes.isNotEmpty()) {
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
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            // Display the total number of ingredients
                            Text(
                                text = LocalContext.current.resources.getQuantityString(
                                    R.plurals.items,
                                    shoppingList!!.countUniqueIngredientNames(),
                                    shoppingList!!.countUniqueIngredientNames()
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )

                            Spacer(modifier = Modifier.weight(1.0f))

                            Text(
                                text = stringResource(R.string.sort_by),
                                style = MaterialTheme.typography.bodyMedium,
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
                                    Text(text = if (shoppingList!!.sortingMethod == "Aisle")
                                                    stringResource(R.string.aisle)
                                                else stringResource(R.string.recipe),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.align(Alignment.CenterVertically))

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
                                                Text(text = stringResource(R.string.aisle),
                                                    style = MaterialTheme.typography.bodyMedium)
                                            },
                                            onClick = {
                                                shoppingList!!.changeSortingMethod("Aisle")
                                                sortingMethod = "Aisle"
                                                showDialog.value = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(text = stringResource(R.string.recipe),
                                                    style = MaterialTheme.typography.bodyMedium)
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

                        if (shoppingList!!.shoppingList.isEmpty()) {
                            Spacer(modifier = Modifier.height(112.dp))

                            Image(painter = painterResource(R.drawable.shopping_list_image),
                                contentDescription = null,
                                modifier = Modifier.size(184.dp).align(Alignment.CenterHorizontally))

                            Text(
                                stringResource(R.string.the_list_is_empty),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top=24.dp))

                            Text(
                                stringResource(R.string.shopping_list_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = onSurfaceVariantLight,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                                            .padding(start = 36.dp, end = 36.dp, top = 4.dp))

                            OutlinedButton(
                                onClick = {
                                    goToAgenda()
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp)
                            ) {
                                Text(text = stringResource(R.string.planify_a_recipe),
                                     style = MaterialTheme.typography.bodyMedium,
                                     modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            ShoppingListByCategory(sortingMethod == "Aisle", shoppingList)
                            Spacer(modifier = Modifier.height(64.dp))
                        }
                    }
                }
            }
        )
    } else {
        BackHandler {
            searchItem = false
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text(stringResource(R.string.add_an_item), style = MaterialTheme.typography.headlineSmall) },
                    navigationIcon = {
                        IconButton(onClick = { searchItem = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.go_back)
                            )
                        }
                    }
                )
            }
        ) { Column(modifier= Modifier
            .padding(top = 64.dp)
            .verticalScroll(rememberScrollState())) {
                var ingredients by remember {
                    mutableStateOf<List<Ingredient>>(emptyList())
                }
                var filtered by remember {
                    mutableStateOf<List<Ingredient>>(emptyList())
                }
                val focusRequester = remember { FocusRequester() }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        val ingredientsDb = IngredientsDb.getDatabase(context)
                        ingredients = ingredientsDb.ingredientDao().selectAll()
                        filtered = ingredients
                    }

                    // Switch back to the Main thread for UI updates
                    withContext(Dispatchers.Main) {
                        focusRequester.requestFocus()
                    }
                }
                var text by remember {
                    mutableStateOf("")
                }
                var expanded by rememberSaveable { mutableStateOf(false) }
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = SearchBarDefaults.colors(
                        containerColor = surfaceContainerLowestLight,
                    ),
                    expanded = false,
                    onExpandedChange = { },
                    inputField = {
                        SearchBarDefaults.InputField(
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .testTag("search_input")
                                .border(
                                    2.dp,
                                    if (expanded) Color(0xFF00AF45) else Color(0x00000000),
                                    RoundedCornerShape(100.dp)
                                )
                                .padding(start = 8.dp),
                            query = text,
                            onQueryChange = {
                                text = it
                                filtered = ingredients.filter { ingredient -> ingredient.name.contains(text) }
                            },
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            onSearch = { expanded = false },
                            placeholder = { Text(stringResource(R.string.carrots_eggs_chocolate), style = MaterialTheme.typography.bodyLarge) },
                            trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                        )
                    }
                ) {}

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    ),
                    colors = CardDefaults.cardColors(containerColor = surfaceContainerLowestLight),
                ) {
                    val scope = rememberCoroutineScope()
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                        val matchingIngredient = filtered.find { it.name == text }
                        if (matchingIngredient == null && text.isNotEmpty()) {
                            // Search result
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                                var res by remember { mutableStateOf<Int?>(null) }
                                val db = IngredientsDb.getDatabase(context)
                                LaunchedEffect(text) {
                                    withContext(Dispatchers.IO) {
                                        val ingredientItem = IngredientItem(text)
                                        res = toIngredientIcon(ingredientItem.name.lowercase(), db, context)
                                    }
                                }

                                if (res != null) {
                                    val painter = rememberAsyncImagePainter(res)
                                    Image(painter = painter,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp).align(Alignment.CenterVertically),
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(36.dp))
                                }

                                Text(
                                    text = text.replaceFirstChar(Char::titlecase),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier .align(Alignment.CenterVertically)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(
                                    modifier = Modifier
                                        .testTag("add_ingredient")
                                        .align(Alignment.CenterVertically)
                                        .size(28.dp),
                                    onClick = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                shoppingList!!.addIngredient(IngredientItem(text.lowercase()))
                                                searchItem = false
                                            }
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color(0xFF599e39),// TODO primaryContainerLight,
                                        contentColor = Color(0xFFFFFFFF)
                                    )) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        modifier = Modifier.size(14.dp),
                                        contentDescription = stringResource(R.string.add_ingredient),
                                    )
                                }
                            }
                        }

                        filtered.forEach {
                            IngredientToAdd(ingredient = it, onIngredientAdded = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        shoppingList!!.addIngredient(it)
                                        searchItem = false
                                    }
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IngredientCheckbox(
    item: ShoppingIngredient,
    shoppingList: ShoppingList?,
    onCheckedChange: (Boolean) -> Unit,
    onValidationChange: (Boolean) -> Unit,
    showOnChecked: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    var isChecked by remember { mutableStateOf(item.ingredient.checked) }
    var isVisible by remember { mutableStateOf(!item.validated || showOnChecked) }

    var name by remember { mutableStateOf(item.ingredient.name.replaceFirstChar(Char::titlecase)) }
    var res by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val db = IngredientsDb.getDatabase(context)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            res = toIngredientIcon(item.ingredient.name.lowercase(), db, context)
            name = item.ingredient.toLocalName().replaceFirstChar(Char::titlecase)
        }
    }

    // Logic for checkbox change and validation
    fun handleCheckChange(newCheckedState: Boolean) {
        isChecked = newCheckedState
        shoppingList?.checkIngredient(item.ingredient.name, newCheckedState)
        onCheckedChange(newCheckedState)

        // Update validation state with delay
        coroutineScope.launch {
            delay(1000L)
            shoppingList?.addValidated(item.ingredient)
            onValidationChange(newCheckedState)
            isVisible = !item.validated && !showOnChecked
        }
    }

    // Properly manage visibility and ensure only the correct checkbox responds
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(1000)),
        exit = fadeOut(animationSpec = tween(1000)) + shrinkVertically(animationSpec = tween(1000)),
        modifier = Modifier.clickable { handleCheckChange(!isChecked) }
    ) {
        Row (
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            if (res != null) {
                val painter = rememberAsyncImagePainter(res)
                Image(painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.CenterVertically)
                        .alpha(if (isChecked) 0.5f else 1.0f)
                )
            } else {
                Spacer(modifier = Modifier.width(26.dp))
            }

            Column(modifier = Modifier.padding(start = 16.dp).align(Alignment.CenterVertically)) {
                Text(
                    text = name,
                    style = if (isChecked) MaterialTheme.typography.labelLarge.copy(textDecoration = TextDecoration.LineThrough) else MaterialTheme.typography.labelLarge,
                    modifier = Modifier.alpha(if (isChecked) 0.5f else 1.0f)
                )
                if (item.ingredient.quantity.toInt() != 1) {
                    Text("${item.ingredient.quantity.toInt()} ${item.ingredient.unit}",
                        style=MaterialTheme.typography.bodySmall,
                        modifier = Modifier.alpha(if (isChecked) 0.5f else 1.0f))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Checkbox(
                colors = CheckboxDefaults.colors(uncheckedColor = outlineLight, checkedColor = primaryContainerLight),
                checked = isChecked,
                modifier = Modifier.align(Alignment.CenterVertically),
                onCheckedChange = { handleCheckChange(it) }
            )
        }
    }
}


fun mergeDuplicateIngredients(ingredients: List<ShoppingIngredient>): List<ShoppingIngredient> {
    val mergedIngredientsMap = mutableMapOf<String, ShoppingIngredient>()

    ingredients.forEach { item ->
        val ingredientName = item.ingredient.name.lowercase() // Use lowercase to avoid case-sensitive duplicates
        if (mergedIngredientsMap.containsKey(ingredientName)) {
            // If ingredient is already in the map, add the quantities
            mergedIngredientsMap[ingredientName]?.ingredient?.addQuantity(item.ingredient)
        } else {
            // If not, add the ingredient to the map
            mergedIngredientsMap[ingredientName] = item.copy()
        }
    }

    // Return the list of merged ingredients
    return mergedIngredientsMap.values.toList()
}

@Composable
fun CategoryCard(
    category: String,
    ingredients: List<ShoppingIngredient>,
    shoppingList: ShoppingList?,
    onCategoryEmpty: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onValidationChange: (Boolean) -> Unit
) {
    val nonValidatedItems = ingredients.filter { !it.validated }

    val list = mergeDuplicateIngredients(nonValidatedItems)

    if (list.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceContainerLowestLight),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = category.replaceFirstChar(Char::titlecase),
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariantLight,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )

                // List of unvalidated ingredients with unique keys
                list.forEach { shoppingIngredient ->
                    key(shoppingIngredient.ingredient.name, shoppingIngredient.ingredient.checked) {
                        IngredientCheckbox(
                            item = shoppingIngredient,
                            shoppingList = shoppingList,
                            onCheckedChange,
                            onValidationChange = { validated ->
                                if (validated && list.all { it.validated }) {
                                    onCategoryEmpty() // Notify parent when category is empty
                                }
                                onValidationChange(validated)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ValidatedCategoryCard(
    category: String,
    validatedIngredients: List<ShoppingIngredient>,
    shoppingList: ShoppingList?,
    onUndoValidation: (ShoppingIngredient) -> Unit, // Callback to move items back to original category
    onValidationChange: (Boolean) -> Unit
) {
    val list = mergeDuplicateIngredients(validatedIngredients)
    if (list.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceContainerLowestLight),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariantLight,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )

                // List of validated ingredients
                list.forEach { shoppingIngredient ->
                    key(shoppingIngredient.ingredient.name, shoppingIngredient.ingredient.checked) {
                        IngredientCheckbox(
                            item = shoppingIngredient,
                            shoppingList = shoppingList,
                            onValidationChange = { validated ->
                                if (!validated) {
                                    onUndoValidation(shoppingIngredient) // Move back to original category
                                }
                                onValidationChange(validated)
                            },
                            onCheckedChange = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShoppingListByCategory(
    sortedByCategory: Boolean,
    shoppingList: ShoppingList?
) {
    val visibleCategories = remember { mutableStateListOf<String>() }
    var validatedItems by remember { mutableStateOf(shoppingList?.shoppingList?.filter { it.validated }) }
    val scope = rememberCoroutineScope()

    // Group ingredients by category or recipe
    val groupedList = if (sortedByCategory) {
        shoppingList?.shoppingList?.groupBy { it.ingredient.category }
    } else {
        shoppingList?.shoppingList?.groupBy { it.recipeId.toString() }
    }

    // Render each category and its ingredients
    groupedList?.forEach { (groupKey, categoryIngredients) ->
        val k = groupKey.takeIf { sortedByCategory } ?: shoppingList?.plannedRecipes?.find { it.recipeId.toString() == groupKey }?.title.orEmpty()
        var key by remember { mutableStateOf(k) }
        if (!sortedByCategory && groupKey == "0") {
            key = stringResource(R.string.custom)
        }
        if (sortedByCategory) {
            scope.launch {
                key = Translator().translate(key)
            }
        }

        // Show category card with non-validated items
        CategoryCard(
            category = key,
            ingredients = categoryIngredients,
            shoppingList = shoppingList,
            onCategoryEmpty = {
                visibleCategories.remove(key) // Remove category if empty
            },
            onCheckedChange = {
                validatedItems = shoppingList?.shoppingList?.filter { it.validated }
            },
            onValidationChange = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        delay(1000) // For animation to execute
                        validatedItems = shoppingList?.shoppingList?.filter { it.validated }
                    }
                }
            }
        )
    }

    // Show validated ingredients
    if (validatedItems?.isNotEmpty() == true) {
        ValidatedCategoryCard(
            category = stringResource(R.string.validated_ingredients),
            validatedIngredients = validatedItems!!,
            shoppingList = shoppingList,
            onUndoValidation = { unvalidatedIngredient ->
                visibleCategories.add(unvalidatedIngredient.ingredient.category) // Re-add original category when unvalidated
            },
            onValidationChange = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        delay(1000) // For animation to execute
                        validatedItems = shoppingList?.shoppingList?.filter { it.validated }
                    }
                }
            }
        )
    }
}

@Composable
fun IngredientToAdd(ingredient: Ingredient, onIngredientAdded: (IngredientItem) -> Unit) {
    var name by remember { mutableStateOf(ingredient.name.replaceFirstChar(Char::titlecase)) }
    var res by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val db = IngredientsDb.getDatabase(context)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val ingredientItem = IngredientItem(ingredient.name)
            res = toIngredientIcon(ingredientItem.name.lowercase(), db, context)
            name = ingredientItem.toLocalName()
        }
    }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {

        if (res != null) {
            val painter = rememberAsyncImagePainter(res)
            Image(painter = painter,
                contentDescription = null,
                modifier = Modifier.size(36.dp).align(Alignment.CenterVertically),
            )
        } else {
            Spacer(modifier = Modifier.width(36.dp))
        }

        Text(
            text = name.replaceFirstChar(Char::titlecase),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier .align(Alignment.CenterVertically)
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            modifier = Modifier
                .testTag("add_ingredient")
                .align(Alignment.CenterVertically)
                .size(28.dp),
            onClick = { onIngredientAdded(IngredientItem(ingredient.name.replaceFirstChar(Char::titlecase)))},
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color(0xFF599e39),// TODO primaryContainerLight,
                contentColor = Color(0xFFFFFFFF)
            )) {
            Icon(
                imageVector = Icons.Filled.Add,
                modifier = Modifier.size(14.dp),
                contentDescription = stringResource(R.string.add_ingredient),
            )
        }
    }
}