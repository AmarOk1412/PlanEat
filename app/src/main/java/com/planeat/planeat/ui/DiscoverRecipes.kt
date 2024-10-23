package com.planeat.planeat.ui

import android.annotation.SuppressLint
import android.os.Build
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.compose.onPrimaryContainerLight
import com.example.compose.primaryContainerLight
import com.example.compose.primaryLight
import com.example.compose.surfaceContainerLowestLight
import com.example.compose.surfaceLight
import com.planeat.planeat.R
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.Tags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DiscoverScreen(
    model: AppModel,
    modifier: Modifier = Modifier,
    onQueryChanged: (String, Boolean) -> Unit,
    onRecipeDeleted: (Recipe) -> Unit,
    goToAgenda: () -> Unit,
    goToDetails: (Recipe) -> Unit,
    goToEdition: (Recipe) -> Unit,
    onFilterClicked: (Tags) -> Unit,
) {

    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var toPlanRecipe by remember { mutableStateOf<Recipe?>(null) }
    var currentTag by remember { mutableStateOf(model.currentTag.value) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentWindowInsets = WindowInsets(0.dp),
        content = {
            // Show the list screen
            // TODO separate the list screen into a separate composable
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                var text by rememberSaveable { mutableStateOf("") }
                var expanded by rememberSaveable { mutableStateOf(false) }
                val filters = Tags.entries.map { it }

                BackHandler {
                    text = ""
                    expanded = false
                }

                LaunchedEffect(Unit) {
                    text = ""
                    expanded = false
                }

                LaunchedEffect(text) {
                    delay(300)
                    onQueryChanged.invoke(text, false)
                }

                val focusRequester = remember { FocusRequester() }

                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = SearchBarDefaults.colors(
                        containerColor = surfaceLight,
                    ),
                    expanded = false,
                    onExpandedChange = { },
                    inputField = {
                        SearchBarDefaults.InputField(
                            modifier = Modifier
                                .border(1.dp,
                                    if (expanded) primaryLight else Color(0x00000000),
                                    RoundedCornerShape(100.dp)
                                )
                                .testTag("search_input")
                                .focusRequester(focusRequester)
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
                        .padding(top = 8.dp)
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
                            modifier = Modifier.padding(end = 8.dp, bottom = 12.dp)
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
                    LazyColumn(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (model.suggestedRecipesShown.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.you_may_like),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        }

                        items(
                            model.suggestedRecipesShown,
                            key = { recipe -> recipe.url }) { recipe ->
                            RecipeItem(
                                recipe,
                                model,
                                goToDetails = { goToDetails(it) },
                                goToAgenda = { goToAgenda() },
                                goToEdition = { goToEdition(it) },
                                onRecipeDeleted = { onRecipeDeleted(it) },
                                onPlanRecipe = { r ->
                                    toPlanRecipe = r
                                    openBottomSheet = true
                                })

                            HorizontalDivider(color = surfaceLight)
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (model.recipesSearchedShown.size > 0 && model.recipesInDbShown.size > 0) {
                            item {
                                Text(
                                    text = stringResource(R.string.my_recipes),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(top = 20.dp)
                                )
                            }
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    model.recipesInDbShown,
                                    key = { recipe -> recipe.url }) { recipe ->
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
                                        modifier = Modifier.width((LocalConfiguration.current.screenWidthDp * 0.75f).dp))
                                }
                            }
                        }

                        if (model.recipesSearchedShown.size > 0) {
                            item {
                                Text(
                                    text = stringResource(R.string.new_recipes),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(top = 20.dp)
                                )
                            }
                            items(
                                model.recipesSearchedShown,
                                key = { recipe -> recipe.url }) { recipe ->
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
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                if (openBottomSheet) {
                    BottomPlanifier(
                        onDismissRequest = { openBottomSheet = false },
                        toPlanRecipe = toPlanRecipe!!,
                        goToAgenda = {
                            openBottomSheet = false
                            toPlanRecipe = null
                            goToAgenda()
                        }
                    )
                }
            }
        }
    )
}