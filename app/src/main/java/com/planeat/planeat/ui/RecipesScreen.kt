package com.planeat.planeat.ui

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
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
                }) },
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = innerPadding.calculateTopPadding())
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
                    containerColor = Color(0xFF01AA44),
                    contentColor = Color(0xFFFFFFFF),
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.padding(end = 8.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "New Recipe"
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.End,
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Header element
                    Text(
                        text = stringResource(id = R.string.tab_saved),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(start=16.dp, bottom = 16.dp)
                    )

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
                                query = text,
                                onQueryChange = {
                                    text = it
                                },
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                onSearch = { expanded = false },
                                placeholder = { Text(stringResource(id = R.string.search_placeholder)) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) }
                            )
                        }
                    ) {}

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.width(16.dp))

                        filters.forEach { filter ->

                            Button(
                                onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        onFilterClicked(filter)
                                    }
                                },
                                shape = RoundedCornerShape(100.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 6.dp
                                ),
                                contentPadding = PaddingValues(horizontal=16.dp, vertical=10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = surfaceContainerLowestLight, contentColor = Color.Black),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    toTagIcon(tag = filter)
                                    Text(
                                        text = filter.toString(),
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
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp)
                            ) {
                                Text(
                                    text = "Favorites",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )

                                Spacer(modifier = Modifier.weight(1.0f))

                                if (httpRecipes.size > 5) {
                                    TextButton(onClick = {
                                        filter = "http"
                                    },
                                        modifier = Modifier.align(Alignment.CenterVertically)) {
                                        Text("See more")
                                    }
                                }
                            }

                            Row(
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
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "My recipes",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )

                                Spacer(modifier = Modifier.weight(1.0f))

                                if (nonHttpRecipes.size > 5) {
                                    TextButton(onClick = {
                                        filter = "nonHttp"
                                    },
                                        modifier = Modifier.align(Alignment.CenterVertically)) {
                                        Text("See more")
                                    }
                                }
                            }

                            Row(
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

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.padding(start=16.dp, end=16.dp)
                        ) {
                            if (httpRecipes.size > 0) {
                                item(span = { GridItemSpan(2) }) {
                                    Text(
                                        text = "Saved",
                                        style = MaterialTheme.typography.titleLarge,
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
                            if (nonHttpRecipes.size > 0) {
                                item(span = { GridItemSpan(2) }) {
                                    Text(
                                        text = "My recipes",
                                        style = MaterialTheme.typography.titleLarge,
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
            .padding(8.dp)
            .shadow(8.dp, shape = MaterialTheme.shapes.medium)
    )
}