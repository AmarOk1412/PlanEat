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

package com.planeat.planeat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.planeat.planeat.R
import com.planeat.planeat.data.Recipe
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockedSearchBar(
    modifier: Modifier = Modifier,
    onQueryChanged: (String) -> Unit,
    recipes: List<Recipe>
) {
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        delay(300)
        onQueryChanged.invoke(query)
    }

    DockedSearchBar(
        modifier = modifier,
        query = query,
        onQueryChange = {
            query = it
        },
        onSearch = { active = false },
        active = active,
        onActiveChange = {
            active = it
        },
        placeholder = { Text(text = stringResource(id = R.string.search_placeholder)) },
        leadingIcon = {
            if (active) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.back_button),
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .clickable {
                            active = false
                            query = ""
                        },
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(id = R.string.search),
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        },
    ) {
        if (recipes.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                recipes.forEach { recipe ->
                    Text(
                        text = recipe.title,
                        modifier = Modifier.padding(4.dp).clickable { query = recipe.title },
                    )
                }
            }
        } else {
            Text(
                text = stringResource(id = R.string.no_item_found),
                modifier = Modifier.padding(16.dp)
            )
        }

    }
}
