
package com.planeat.planeat.ui.components

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.compose.onSurfaceVariantLight
import com.example.compose.surfaceContainerLowestLight
import com.planeat.planeat.R
import com.planeat.planeat.data.Agenda
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(
     ExperimentalFoundationApi::class,
 )
@Composable
fun RecipeListItem(
    recipe: Recipe,
    onRecipeSelected: (Recipe) -> Unit = {},
    onEditRecipe: (Recipe) -> Unit = {},
    onPlanRecipe: (Recipe) -> Unit = {},
    onRemoveFromAgenda: () -> Unit = {},
    onRecipeDeleted: (Recipe) -> Unit = {},
    onRecipeAdded: (Recipe) -> Unit = {},
    agenda: Agenda? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val existId = remember { mutableLongStateOf(0.toLong()) }
    val icon = remember { mutableStateOf(Icons.Default.Add) }
    val outlinedFav = ImageVector.vectorResource(R.drawable.favorite)

    val showDialog = remember { mutableStateOf(false) }
    var showDeleteDialog = remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .clip(CardDefaults.shape)
            .width((LocalConfiguration.current.screenWidthDp * 0.4f).dp)
            .height((LocalConfiguration.current.screenWidthDp * 0.4f).dp)// Set the width to 40% of the screen
            .combinedClickable(
                onClick = { onRecipeSelected(recipe) },
                onLongClick = { }
            )
            .clip(CardDefaults.shape),
        colors = CardDefaults.cardColors(
            containerColor = surfaceContainerLowestLight,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    try {
                        val rdb = RecipesDb.getDatabase(context)
                        val res = rdb.recipeDao().findByUrl(recipe.url)
                        // rdb.close()
                        if (res != null) {
                            existId.longValue = res.recipeId
                        }
                        icon.value = if (existId.longValue == 0.toLong()) {
                            Icons.Outlined.BookmarkAdd
                        } else {
                            Icons.Default.MoreVert
                        }
                    } catch (error: Exception) {
                        Log.w("PlanEat", "Error: $error")
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f / .5f)
            ) {
                AsyncImage(
                    model = if (recipe.image.startsWith("http")) {
                        recipe.image
                    } else {
                        ImageRequest.Builder(LocalContext.current)
                            .data(recipe.image)
                            .build()
                    },
                    contentDescription = recipe.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                            try {
                                if (recipe.recipeId == 0.toLong()) {
                                    val rdb = RecipesDb.getDatabase(context)
                                    if (existId.longValue == 0.toLong()) {
                                        onRecipeAdded(recipe)
                                        val res = rdb.recipeDao().findByUrl(recipe.url)
                                        existId.longValue = res.recipeId
                                        icon.value = Icons.Filled.Favorite
                                    } else {
                                        Log.w("PlanEat", "Delete recipe: ${recipe.title}")
                                        val r = recipe
                                        r.recipeId = existId.longValue
                                        rdb.recipeDao().delete(r)
                                        existId.longValue = 0
                                        icon.value = outlinedFav
                                    }
                                } else {
                                    showDialog.value = true
                                }
                            } catch (error: Exception) {
                                Log.d("PlanEat", "Error: $error")
                            }
                        } },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(surfaceContainerLowestLight)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = icon.value,
                            contentDescription = stringResource(R.string.favorite),
                        )

                        if (showDialog.value) {
                            DropdownMenu(
                                containerColor = surfaceContainerLowestLight,
                                offset = DpOffset(0.dp, 8.dp),
                                expanded = showDialog.value,
                                onDismissRequest = { showDialog.value = false },
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Filled.Today, contentDescription = null) },
                                    text = {
                                        Text(text = if (agenda != null) "Remove from agenda" else "Add to agenda")
                                    },
                                    onClick = {
                                        showDialog.value = false
                                        if (agenda != null) {
                                            onRemoveFromAgenda()
                                        } else {
                                            if (recipe.recipeId == 0.toLong()) {
                                                val rdb = RecipesDb.getDatabase(context)
                                                if (existId.longValue == 0.toLong()) {
                                                    // If a search result, add it to recipes first
                                                    onRecipeAdded(recipe)
                                                    val res = rdb.recipeDao().findByUrl(recipe.url)
                                                    onPlanRecipe(res)
                                                }
                                            } else {
                                                onPlanRecipe(recipe)
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    text = {
                                        Text(text = "Edit")
                                    },
                                    onClick = {
                                        showDialog.value = false
                                        onEditRecipe(recipe)
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.Red) },
                                    text = {
                                        Text(text = "Delete", color = Color.Red)
                                    },
                                    onClick = {
                                        showDialog.value = false
                                        showDeleteDialog.value = true
                                    }
                                )
                            }
                        }

                        if (showDeleteDialog.value) {
                            AlertDialog(
                                icon = {
                                    Icon(Icons.Filled.Delete, contentDescription = null)
                                },
                                title = {
                                    Text(text = "Confirm deletion")
                                },
                                text = {
                                    Text(text = "This will remove the recipe from your agenda and recipe list.")
                                },
                                onDismissRequest = {
                                    showDeleteDialog.value = false
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showDeleteDialog.value = false
                                            onRecipeDeleted(recipe)
                                        }
                                    ) {
                                        Text("Confirm")
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            showDeleteDialog.value = false
                                        }
                                    ) {
                                        Text("Dismiss")
                                    }
                                }
                            )

                        }
                    }
                }
            }
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp, end = 8.dp, bottom = 0.dp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            Text(
                text = convertDuration(recipe.cookingTime),
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceVariantLight,
                modifier = Modifier.padding(start=8.dp, top = 0.dp, bottom = 0.dp),
            )

            SuggestionChip(
                label = { Text(
                    text = if (recipe.parsed_ingredients.isNotEmpty()) {
                        recipe.parsed_ingredients.size.toString() + " ingrédients"
                    } else {
                        recipe.ingredients.size.toString() + " ingrédients"
                    },
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.height(18.dp),

                ) },
                onClick = {},
                border = null,
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color(0xFFfff4df),
                    labelColor = Color(0xFF664a02)
                )/* TODO SuggestionChipDefaults.suggestionChipColors(
                    containerColor = secondaryContainerLight,
                    labelColor = onSecondaryContainerLight
                )*/,
                modifier = Modifier
                    .padding(8.dp)
                    .height(24.dp),
            )

        }
    }
 }

fun convertDuration(cookingTime: Int): String {
    val hours = cookingTime / 60
    val minutes = cookingTime % 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}min"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}min"
        else -> "0min"
    }
}
