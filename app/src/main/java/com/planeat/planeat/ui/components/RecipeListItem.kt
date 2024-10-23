
package com.planeat.planeat.ui.components

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.compose.onBackgroundLight
import com.example.compose.onSecondaryContainerLight
import com.example.compose.onSurfaceVariantLight
import com.example.compose.secondaryContainerLight
import com.example.compose.surfaceContainerLowestLight
import com.planeat.planeat.R
import com.planeat.planeat.data.Agenda
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.data.Tags
import com.planeat.planeat.ui.AppModel
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
    onRemoveFromAgenda: (Long) -> Unit = {},
    onRecipeDeleted: (Recipe) -> Unit = {},
    model: AppModel,
    agenda: Agenda? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val existId = remember { mutableLongStateOf(0.toLong()) }

    val showDialog = remember { mutableStateOf(false) }
    var showDeleteDialog = remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .combinedClickable(
                onClick = { onRecipeSelected(recipe) },
                onLongClick = { }
            )
            .clip(CardDefaults.shape),
    ) {
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                try {
                    val rdb = RecipesDb.getDatabase(context)
                    val res = rdb.recipeDao().findByUrl(recipe.url)
                    if (res != null) {
                        existId.longValue = res.recipeId
                    }
                } catch (error: Exception) {
                    Log.w("PlanEat", "Error: $error")
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardDefaults.shape)
                .height((LocalConfiguration.current.screenHeightDp * 0.165f).dp)
        ) {
            var showDefault by remember { mutableStateOf(false) }
            if (!showDefault) {
                AsyncImage(
                    model = if (recipe.image.startsWith("http")) {
                        recipe.image
                    } else {
                        ImageRequest.Builder(LocalContext.current)
                            .data(recipe.image)
                            .build()
                    },
                    onError = {
                        showDefault = true
                    },
                    contentDescription = recipe.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.empty_image_recipe),
                    contentDescription = recipe.title,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = { onRecipeSelected(recipe) },
                            onLongClick = { }
                        ),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {

                if (recipe.edited) {
                    SuggestionChip(
                        label = { Text(
                            text = if (recipe.url.startsWith("http")) stringResource(R.string.edited) else stringResource(R.string.my_recipe),
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall,
                        ) },
                        onClick = { onRecipeSelected(recipe) },
                        border = null,
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xCCFFFFFF),
                            labelColor = onSurfaceVariantLight
                        ),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .height(24.dp),
                    )
                }
            }
        }

        Row {
            Column() {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 8.dp, top = 12.dp, end = 8.dp, bottom = 0.dp)
                        .testTag("recipe_title"),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )

                val tagStr = if (recipe.tags.isNotEmpty()) {
                    val t = Tags.fromString(recipe.tags.first())
                    if (t != null) {
                        " · " + t.getString(context)
                    } else {
                        ""
                    }
                } else {
                    ""
                }
                val middleLabel = convertDuration(recipe.cookingTime) + tagStr

                Text(
                    text = middleLabel,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start=8.dp, top = 4.dp, bottom = 0.dp).testTag("recipe_label"),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { showDialog.value = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .align(Alignment.CenterVertically)
                    .background(surfaceContainerLowestLight)
                    .size(40.dp)
                    .testTag("favorite_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
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
                            leadingIcon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.today), contentDescription = null, tint = onBackgroundLight) },
                            text = {
                                Text(text = if (agenda != null) stringResource(R.string.remove_from_agenda) else stringResource(
                                    R.string.add_to_agenda
                                ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    showDialog.value = false
                                    if (agenda != null) {
                                        onRemoveFromAgenda(recipe.recipeId)
                                    } else {
                                        if (recipe.recipeId == 0.toLong()) {
                                            val rdb = RecipesDb.getDatabase(context)
                                            if (existId.longValue == 0.toLong()) {
                                                // If a search result, add it to recipes first
                                                Log.d("PlanEat", "Add recipe: ${recipe.title}")
                                                model.add(recipe)
                                                val res = rdb.recipeDao().findByUrl(recipe.url)
                                                onPlanRecipe(res)
                                            }
                                        } else {
                                            onPlanRecipe(recipe)
                                        }
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.edit), contentDescription = null, tint = onBackgroundLight) },
                            text = {
                                Text(text = stringResource(R.string.edit), style = MaterialTheme.typography.bodyMedium)
                            },
                            onClick = {
                                showDialog.value = false
                                onEditRecipe(recipe)
                            }
                        )
                        if (agenda == null && recipe.recipeId != 0.toLong()) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                leadingIcon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.delete), contentDescription = null, tint = Color.Red) },
                                text = {
                                    Text(text = stringResource(R.string.delete), color = Color.Red, style = MaterialTheme.typography.bodyMedium)
                                },
                                onClick = {
                                    showDialog.value = false
                                    showDeleteDialog.value = true
                                }
                            )
                        }
                    }
                }

                if (showDeleteDialog.value) {
                    AlertDialog(
                        icon = {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                        },
                        title = {
                            Text(text = stringResource(R.string.confirm_deletion))
                        },
                        text = {
                            Text(text = stringResource(R.string.remove_from_agenda_confirmation))
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
                                Text(stringResource(R.string.confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showDeleteDialog.value = false
                                }
                            ) {
                                Text(stringResource(R.string.dismiss))
                            }
                        }
                    )

                }
            }
        }

        SuggestionChip(
            label = {
                val ingredients = recipe.parsed_ingredients.ifEmpty { recipe.ingredients }
                Text(
                    text = LocalContext.current.resources.getQuantityString(
                        R.plurals._1_ingredients,
                        ingredients.size,
                        ingredients.size
                    ),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall.copy(color = onSecondaryContainerLight),
                )
            },
            shape = RoundedCornerShape(100.dp),
            onClick = {},
            border = null,
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = secondaryContainerLight,
                labelColor = onSecondaryContainerLight
            ),
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp).height(22.dp)
        )

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
