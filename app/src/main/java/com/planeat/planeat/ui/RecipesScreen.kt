package com.planeat.planeat.ui

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.compose.onPrimaryContainerLight
import com.example.compose.primaryContainerLight
import com.example.compose.primaryLight
import com.example.compose.surfaceContainerLowestLight
import com.example.compose.surfaceLight
import com.planeat.planeat.R
import com.planeat.planeat.data.Agenda
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.data.Tags
import com.planeat.planeat.ui.components.MinimalRecipeItemList
import com.planeat.planeat.ui.components.RecipeListItem
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
    goToAgenda: () -> Unit,
    goToDetails: (Recipe) -> Unit,
    goToEdition: (Recipe) -> Unit,
    onFilterClicked: (Tags) -> Unit,
) {
    var filter by remember { mutableStateOf("") }
    var currentTag by remember { mutableStateOf(model.currentTag.value) }
    var context = LocalContext.current

    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var toPlanRecipe by remember { mutableStateOf<Recipe?>(null) }


    val favoritesRecipes = model.recipesInDbShown.filter { it.favorite }
    val editedRecipes = model.recipesInDbShown.filter { !it.url.startsWith("http") || it.edited }

    if (filter.isNotEmpty()) {
        BackHandler {
            filter = ""
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text(text = if (filter == "favorites") {
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceContainerLowestLight)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = innerPadding.calculateTopPadding()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(if (filter == "favorites") {
                    favoritesRecipes
                } else {
                    editedRecipes
                }, key = { recipe -> recipe.url }) { recipe ->
                    RecipeItem(
                        recipe,
                        model,
                        goToDetails,
                        goToAgenda,
                        goToEdition,
                        onRecipeDeleted,
                        onPlanRecipe = { r ->
                            toPlanRecipe = r
                            openBottomSheet = true
                        },
                        modifier = Modifier.fillMaxWidth())

                    HorizontalDivider(color = surfaceLight)
                }

                item() {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceContainerLowestLight)
        ) {

            var text by rememberSaveable { mutableStateOf("") }
            var expanded by rememberSaveable { mutableStateOf(false) }
            val filters = Tags.entries.map { it }

            LaunchedEffect(text) {
                delay(300)
                onQueryChanged.invoke(text, true)
            }

            val focusRequester = remember { FocusRequester() }

            SearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .padding(horizontal = 16.dp),
                colors = SearchBarDefaults.colors(
                    containerColor = surfaceLight,
                ),
                expanded = false,
                onExpandedChange = { },
                inputField = {
                    SearchBarDefaults.InputField(
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (expanded) primaryLight else Color(0x00000000),
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
                        placeholder = {
                            Text(
                                stringResource(id = R.string.search_placeholder),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                if (expanded) text = "" else focusRequester.requestFocus()
                            }) {
                                Icon(
                                    if (expanded) Icons.Filled.Close else Icons.Default.Search,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            ) {}

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp)
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentTag == filter) primaryContainerLight else surfaceContainerLowestLight,
                            contentColor = if (currentTag == filter) onPrimaryContainerLight else Color.Black
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = filter.getString(LocalContext.current),
                                fontSize = with(LocalDensity.current) {
                                    14.dp.toSp()
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }

            if (text.isEmpty()) {
                if (favoritesRecipes.isNotEmpty()) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.favorites),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Card(
                            modifier = modifier
                                .align(Alignment.CenterVertically)
                                .clip(CardDefaults.shape)
                                .padding(start = 8.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 0.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = surfaceLight,
                            ),
                        ) {
                            Text(
                                text = favoritesRecipes.size.toString(),
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF000000),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1.0f))

                        if (favoritesRecipes.size > 2) {
                            TextButton(
                                onClick = {
                                    filter = "favorites"
                                },
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    stringResource(R.string.see_more),
                                    style = MaterialTheme.typography.labelLarge
                                )
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
                        favoritesRecipes.take(5).forEach { recipe ->
                            MinimalRecipeItemList(
                                recipe,
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
                                            Log.w("PlanEat", "Selected date: ${model.selectedDate.value!!}")
                                            val dateMidday = model.selectedDate.value!!
                                                .atTime(12, 0)
                                                .toInstant(ZoneOffset.UTC)
                                                .toEpochMilli()

                                            Log.w("PlanEat", "Recipe: ${id}, Date: ${dateMidday}")
                                            model.planify(Agenda(
                                                date = dateMidday,
                                                recipeId = id
                                            ))
                                            goToAgenda()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                if (editedRecipes.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.my_creations),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Card(
                            modifier = modifier
                                .align(Alignment.CenterVertically)
                                .clip(CardDefaults.shape)
                                .padding(start = 8.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 0.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = surfaceLight,
                            ),
                        ) {
                            Text(
                                text = editedRecipes.size.toString(),
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF000000),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1.0f))

                        if (editedRecipes.size > 2) {
                            TextButton(
                                onClick = {
                                    filter = "edited"
                                },
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    stringResource(R.string.see_more),
                                    style = MaterialTheme.typography.labelLarge
                                )
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
                        editedRecipes.take(5).forEach { recipe ->
                            MinimalRecipeItemList(
                                recipe,
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
                                            Log.w("PlanEat", "Selected date: ${model.selectedDate.value!!}")
                                            val dateMidday = model.selectedDate.value!!
                                                .atTime(12, 0)
                                                .toInstant(ZoneOffset.UTC)
                                                .toEpochMilli()

                                            Log.w("PlanEat", "Recipe: ${id}, Date: ${dateMidday}")
                                            model.planify(Agenda(
                                                date = dateMidday,
                                                recipeId = id
                                            ))
                                            goToAgenda()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

            } else {

                LazyColumn(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (favoritesRecipes.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.my_favorites),
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                    items(favoritesRecipes, key = { recipe -> recipe.url }) { recipe ->
                        RecipeItem(
                            recipe,
                            model,
                            goToDetails,
                            goToAgenda,
                            goToEdition,
                            onRecipeDeleted,
                            onPlanRecipe = { r ->
                                toPlanRecipe = r
                                openBottomSheet = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(color = surfaceLight)
                    }

                    // My recipes
                    if (editedRecipes.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.my_creations),
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                    items(editedRecipes.filter { !it.favorite }, key = { recipe -> recipe.url }) { recipe ->
                        RecipeItem(
                            recipe,
                            model,
                            goToDetails,
                            goToAgenda,
                            goToEdition,
                            onRecipeDeleted,
                            onPlanRecipe = { r ->
                                toPlanRecipe = r
                                openBottomSheet = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(color = surfaceLight)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1.0f))

            HorizontalDivider(color = surfaceLight)

            Button(onClick = { goToEdition(Recipe()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryContainerLight,
                    contentColor = onPrimaryContainerLight
                ),
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(stringResource(R.string.create_a_recipe))
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RecipeItem(recipe: Recipe, model: AppModel, goToDetails: (Recipe) -> Unit,
               goToAgenda: () -> Unit,
               goToEdition: (Recipe) -> Unit,
               onRecipeDeleted: (Recipe) -> Unit,
               onPlanRecipe: (Recipe) -> Unit,
               modifier: Modifier = Modifier) {
    val context = LocalContext.current
    RecipeListItem(
        recipe = recipe,
        model = model,
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
                    Log.w("PlanEat", "Selected date: ${model.selectedDate.value!!}")
                    val dateMidday = model.selectedDate.value!!
                        .atTime(12, 0)
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli()

                    Log.w("PlanEat", "Recipe: ${id}, Date: ${dateMidday}")
                    model.planify(Agenda(
                        date = dateMidday,
                        recipeId = id
                    ))
                    goToAgenda()
                }
            }
        },
        onEditRecipe = goToEdition,
        onPlanRecipe = onPlanRecipe,
        onRecipeDeleted = onRecipeDeleted,
        modifier = modifier
    )
}