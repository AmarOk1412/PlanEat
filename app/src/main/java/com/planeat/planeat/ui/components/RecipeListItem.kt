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

 package com.example.reply.ui.components

 import android.annotation.SuppressLint
 import android.os.Build
 import android.util.Log
 import androidx.annotation.RequiresApi
 import androidx.compose.animation.ExperimentalAnimationApi
 import androidx.compose.foundation.ExperimentalFoundationApi
 import androidx.compose.foundation.background
 import androidx.compose.foundation.clickable
 import androidx.compose.foundation.combinedClickable
 import androidx.compose.foundation.interaction.MutableInteractionSource
 import androidx.compose.foundation.layout.Arrangement
 import androidx.compose.foundation.layout.Box
 import androidx.compose.foundation.layout.Column
 import androidx.compose.foundation.layout.Row
 import androidx.compose.foundation.layout.fillMaxWidth
 import androidx.compose.foundation.layout.padding
 import androidx.compose.foundation.layout.size
 import androidx.compose.foundation.shape.CircleShape
 import androidx.compose.material.icons.Icons
 import androidx.compose.material.icons.filled.Check
 import androidx.compose.material.icons.filled.Star
 import androidx.compose.material.icons.filled.StarBorder
 import androidx.compose.material3.Card
 import androidx.compose.material3.CardDefaults
 import androidx.compose.material3.Icon
 import androidx.compose.material3.IconButton
 import androidx.compose.material3.MaterialTheme
 import androidx.compose.material3.Text
 import androidx.compose.runtime.Composable
 import androidx.compose.runtime.LaunchedEffect
 import androidx.compose.runtime.mutableStateOf
 import androidx.compose.runtime.remember
 import androidx.compose.ui.Alignment
 import androidx.compose.ui.Modifier
 import androidx.compose.ui.draw.clip
 import androidx.compose.ui.platform.LocalContext
 import androidx.compose.ui.semantics.selected
 import androidx.compose.ui.semantics.semantics
 import androidx.compose.ui.unit.dp
 import androidx.room.Room
 import coil.compose.AsyncImage
 import com.planeat.planeat.data.Agenda
 import com.planeat.planeat.data.AgendaDb
 import com.planeat.planeat.data.Recipe
 import com.planeat.planeat.data.RecipesDb
 import kotlinx.coroutines.CoroutineScope
 import kotlinx.coroutines.Dispatchers
 import kotlinx.coroutines.launch
 import kotlinx.coroutines.withContext
 import java.time.Instant
 import java.time.ZoneId
 import java.time.ZoneOffset
 import java.util.Date

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(
     ExperimentalFoundationApi::class,
 )
 @Composable
 fun RecipeListItem(
    recipe: Recipe,
    db: RecipesDb,
    modifier: Modifier = Modifier,
 ) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(CardDefaults.shape)
            .combinedClickable(
                onClick = { Log.d("PlanEat", "TODO") },
                onLongClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                            val agendaDb = Room.databaseBuilder(
                                context,
                                AgendaDb::class.java, "AgendaDb"
                            ).build()
                            val todayMiddayMillis = Instant.now()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .atTime(12, 0)
                                .toInstant(ZoneOffset.UTC)
                                .toEpochMilli()

                            Log.w("PlanEat", "Recipe: ${recipe.recipeId}, Date: ${todayMiddayMillis}")
                            agendaDb.agendaDao().insertAll(Agenda(
                                date = todayMiddayMillis,
                                recipeId = recipe.recipeId
                            ))
                    }
                }
            )
            .clip(CardDefaults.shape),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
            val clickModifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { db.recipeDao().insertAll(recipe) }
            AsyncImage(
                model = recipe.image,
                contentDescription = recipe.title,
                modifier = Modifier.size(width = 160.dp, height = 100.dp)
            )

            val exists = remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                exists.value = withContext(Dispatchers.IO) {
                    db.recipeDao().findByUrl(recipe.url) != null
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            IconButton(
                onClick = { CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (exists.value) {
                            db.recipeDao().delete(recipe)
                        } else {
                            db.recipeDao().insertAll(recipe)
                        }
                    } catch (error: Exception) {
                        Log.d("PlanEat", "Error: $error")
                    }
                } },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(
                    imageVector = if (exists.value) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (exists.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            }

            Text(
                text = recipe.url,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            )
        }
    }
 }