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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.window.layout.DisplayFeature
import com.example.reply.ui.components.RecipeListItem
import com.planeat.planeat.R
import com.planeat.planeat.ui.components.DockedSearchBar
import com.planeat.planeat.ui.utils.ReplyContentType
import com.planeat.planeat.ui.utils.ReplyNavigationType
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    contentType: ReplyContentType,
    navigationType: ReplyNavigationType,
    displayFeatures: List<DisplayFeature>,
    modifier: Modifier = Modifier,
    onQueryChanged: (String) -> Unit,
    recipes: List<Recipe>,
    db: RecipesDb
) {
    /**
     * When moving from LIST_AND_DETAIL page to LIST page clear the selection and user should see LIST screen.
     */
    LaunchedEffect(key1 = contentType) {

    }

    if (contentType == ReplyContentType.DUAL_PANE) {
        // TODO
        TwoPane(
            first = {
                Box(modifier = modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                    DockedSearchBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        onQueryChanged,
                        recipes
                    )

                    Column(
                        modifier = modifier
                            .verticalScroll(rememberScrollState())
                            .padding(top = 80.dp),
                    ) {
                        recipes.forEach { recipe ->
                            RecipeListItem(
                                recipe = recipe,
                                db = db,
                            )
                        }
                    }
                }
            },
            second = {
            },
            strategy = HorizontalTwoPaneStrategy(splitFraction = 0.5f, gapWidth = 16.dp),
            displayFeatures = displayFeatures
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {

            Box(modifier = modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                DockedSearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    onQueryChanged,
                    recipes
                )

                Column(
                    modifier = modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = 80.dp),
                ) {
                    recipes.forEach { recipe ->
                        RecipeListItem(
                            recipe = recipe,
                            db = db,
                        )
                    }
                }
            }
            // When we have bottom navigation we show FAB at the bottom end.
            if (navigationType == ReplyNavigationType.BOTTOM_NAVIGATION) {
                LargeFloatingActionButton(
                    onClick = { /*TODO*/ },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(id = R.string.edit),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}