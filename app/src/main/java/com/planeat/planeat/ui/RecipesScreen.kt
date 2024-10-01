package com.planeat.planeat.ui

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.compose.onPrimaryLight
import com.example.compose.primaryLight
import com.example.compose.surfaceContainerLowestLight
import com.planeat.planeat.R
import com.planeat.planeat.data.Agenda
import com.planeat.planeat.data.AgendaDb
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.data.Tags
import com.planeat.planeat.data.toTagIcon
import com.planeat.planeat.ui.components.RecipeListItem
import com.planeat.planeat.ui.components.calendar.CalendarUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneOffset

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RecipesScreen(
    model: AppModel,
    modifier: Modifier = Modifier,
    onQueryChanged: (String, Boolean) -> Unit,
    onRecipeDeleted: (Recipe) -> Unit,
    onRecipeAdded: (Recipe) -> Unit,
    dataUi: CalendarUiModel,
    goToAgenda: () -> Unit,
    goToDetails: (Recipe) -> Unit,
    goToEdition: (Recipe) -> Unit,
    onFilterClicked: (Tags) -> Unit,
) {
    var filter by remember { mutableStateOf("") }
    var currentTag by remember { mutableStateOf(model.currentTag.value) }


    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var toPlanRecipe by remember { mutableStateOf<Recipe?>(null) }


    val httpRecipes = model.recipesInDbShown.filter { it.url.startsWith("http") }
    val nonHttpRecipes = model.recipesInDbShown.filter { !it.url.startsWith("http") }

    if (filter.isNotEmpty()) {
        BackHandler {
            filter = ""
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text(text = if (filter == "http") {
                            "Favorites"
                        } else {
                            "My recipes"
                        },
                        style = MaterialTheme.typography.headlineSmall
                        ) },
                    navigationIcon = {
                        IconButton(onClick = { filter = "" }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = innerPadding.calculateTopPadding()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(if (filter == "http") {
                    httpRecipes
                } else {
                    nonHttpRecipes
                }, key = { recipe -> recipe.url }) { recipe ->
                    RecipeItem(
                        recipe,
                        model,
                        goToDetails,
                        goToAgenda,
                        goToEdition,
                        onRecipeDeleted,
                        onRecipeAdded,
                        onPlanRecipe = { r ->
                            toPlanRecipe = r
                            openBottomSheet = true
                        })
                }

                item() {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    } else {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            contentWindowInsets = WindowInsets(0.dp),
            floatingActionButton = {
                FloatingActionButton(onClick = { goToEdition(Recipe()) },
                    containerColor = primaryLight,
                    contentColor = onPrimaryLight,
                    shape = RoundedCornerShape(100.dp),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    modifier = Modifier.padding(vertical = 16.dp)) {
                    Text(
                        text = stringResource(R.string.create_a_recipe),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {

                    var text by rememberSaveable { mutableStateOf("") }
                    var expanded by rememberSaveable { mutableStateOf(false) }
                    val filters = Tags.entries.map { it }

                    LaunchedEffect(Unit) {
                        text = ""
                        expanded = false
                    }

                    LaunchedEffect(text) {
                        delay(300)
                        onQueryChanged.invoke(text, true)
                    }

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
                                    .border(
                                        2.dp,
                                        if (expanded) Color(0xFF00AF45) else Color(0x00000000),
                                        RoundedCornerShape(100.dp)
                                    )
                                    .padding(start = 8.dp),
                                query = text,
                                onQueryChange = {
                                    text = it
                                },
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                onSearch = { expanded = false },
                                placeholder = { Text(stringResource(id = R.string.search_placeholder), style = MaterialTheme.typography.bodyLarge) },
                                trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                            )
                        }
                    ) {}

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 20.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.width(16.dp))

                        filters.forEach { filter ->
                            Button(
                                onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        currentTag = filter
                                        onFilterClicked(filter)
                                    }
                                },
                                shape = RoundedCornerShape(100.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 6.dp
                                ),
                                contentPadding = PaddingValues(horizontal=16.dp, vertical=10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (currentTag == filter) primaryLight else surfaceContainerLowestLight, contentColor = if (currentTag == filter) onPrimaryLight else Color.Black),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    toTagIcon(tag = filter)
                                    Text(
                                        text = filter.getString(LocalContext.current),
                                        fontSize = with(LocalDensity.current) {
                                            14.dp.toSp()
                                        },
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (text.isEmpty()) {
                        if (httpRecipes.isNotEmpty()) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.favorite),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )

                                Spacer(modifier = Modifier.weight(1.0f))

                                if (httpRecipes.size > 2) {
                                    TextButton(onClick = {
                                        filter = "http"
                                    },
                                        modifier = Modifier.align(Alignment.CenterVertically)) {
                                        Text(stringResource(R.string.see_more), style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            ) {
                                httpRecipes.take(5).forEach { recipe ->
                                    RecipeItem(recipe, model, goToDetails, goToAgenda, goToEdition, onRecipeDeleted, onRecipeAdded, onPlanRecipe = { r ->
                                        toPlanRecipe = r
                                        openBottomSheet = true
                                    })
                                }
                            }
                        }

                        if (nonHttpRecipes.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.my_recipes),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )

                                Spacer(modifier = Modifier.weight(1.0f))

                                if (nonHttpRecipes.size > 2) {
                                    TextButton(onClick = {
                                        filter = "nonHttp"
                                    },
                                        modifier = Modifier.align(Alignment.CenterVertically)) {
                                        Text(stringResource(R.string.see_more), style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            ) {
                                nonHttpRecipes.take(5).forEach { recipe ->
                                    RecipeItem(recipe, model, goToDetails, goToAgenda, goToEdition, onRecipeDeleted, onRecipeAdded, onPlanRecipe = { r ->
                                        toPlanRecipe = r
                                        openBottomSheet = true
                                    })
                                }
                            }
                        }

                    } else {

                        LazyColumn(
                            modifier = Modifier.padding(start=16.dp, end=16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (httpRecipes.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.my_favorites),
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                }
                            }
                            items(httpRecipes, key = { recipe -> recipe.url }) { recipe ->
                                RecipeItem(recipe, model, goToDetails, goToAgenda, goToEdition, onRecipeDeleted, onRecipeAdded, onPlanRecipe = { r ->
                                    toPlanRecipe = r
                                    openBottomSheet = true
                                })
                            }

                            // My recipes
                            if (nonHttpRecipes.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.my_recipes),
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                }
                            }
                            items(nonHttpRecipes, key = { recipe -> recipe.url }) { recipe ->
                                RecipeItem(recipe, model, goToDetails, goToAgenda, goToEdition, onRecipeDeleted, onRecipeAdded, onPlanRecipe = { r ->
                                    toPlanRecipe = r
                                    openBottomSheet = true
                                })
                            }
                        }
                    }
                }
            }
        )
    }


    if (openBottomSheet) {
        BottomPlanifier(
            onDismissRequest = { openBottomSheet = false },
            dataUi = dataUi,
            toPlanRecipe = toPlanRecipe!!,
            goToAgenda = {
                openBottomSheet = false
                goToAgenda()
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RecipeItem(recipe: Recipe, model: AppModel, goToDetails: (Recipe) -> Unit,
               goToAgenda: () -> Unit,
               goToEdition: (Recipe) -> Unit,
               onRecipeDeleted: (Recipe) -> Unit,
               onRecipeAdded: (Recipe) -> Unit,
               onPlanRecipe: (Recipe) -> Unit) {
    val context = LocalContext.current
    RecipeListItem(
        recipe = recipe,
        onRecipeSelected = { r ->
            if (model.selectedDate.value == null) {
                goToDetails(r)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    // If id == 0, recipe is not in db yet, add it first
                    var id = r.recipeId
                    val rdb = RecipesDb.getDatabase(context)
                    val newRecipe = r
                    newRecipe.planified += 1
                    if (id == 0.toLong()) {
                        // If a search result, add it to recipes first
                        model.add(recipe)
                        val res = rdb.recipeDao().findByUrl(recipe.url)
                        id = res.recipeId
                        newRecipe.recipeId = res.recipeId
                    }

                    // Increment planified value
                    rdb.recipeDao().update(newRecipe)
                    val res2 = rdb.recipeDao().findByUrl(recipe.url)
                    // Then add it to agenda
                    val adb = AgendaDb.getDatabase(context)
                    Log.w("PlanEat", "Selected date: ${model.selectedDate.value!!}")
                    val dateMidday = model.selectedDate.value!!
                        .atTime(12, 0)
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli()

                    Log.w("PlanEat", "Recipe: ${id}, Date: ${dateMidday}")
                    adb.agendaDao()
                        .insertAll(
                            Agenda(
                                date = dateMidday,
                                recipeId = id
                            )
                        )
                    goToAgenda()
                }
            }
        },
        onEditRecipe = goToEdition,
        onPlanRecipe = onPlanRecipe,
        onRecipeDeleted = onRecipeDeleted,
        onRecipeAdded = onRecipeAdded,
        modifier = Modifier
            .shadow(8.dp, shape = MaterialTheme.shapes.medium)
            .fillMaxWidth()
    )
}