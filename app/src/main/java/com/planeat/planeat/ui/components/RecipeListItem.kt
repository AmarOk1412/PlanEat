
package com.planeat.planeat.ui.components

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.room.Room
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.planeat.planeat.R
import com.planeat.planeat.data.Agenda
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.ui.theme.backgroundCardRecipe
import com.planeat.planeat.ui.theme.tagColor
import com.planeat.planeat.ui.theme.textCardRecipe
import com.planeat.planeat.ui.theme.textColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(
     ExperimentalFoundationApi::class,
 )
@Composable
fun RecipeListItem(
    recipe: Recipe,
    onRecipeSelected: (Recipe) -> Unit,
    onRecipeDeleted: (Recipe) -> Unit,
    onRecipeAdded: (Recipe) -> Unit,
    onAgendaDeleted: (Agenda) -> Unit,
    selectedDate: LocalDate,
    searching: Boolean=false,
    agenda: Agenda?,
    goToAgenda: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val existId = remember { mutableLongStateOf(0.toLong()) }
    val icon = remember { mutableStateOf(Icons.Default.Add) }
    val outlinedFav = ImageVector.vectorResource(R.drawable.favorite)

    val showDialog = remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .clip(CardDefaults.shape)
            .width((LocalConfiguration.current.screenWidthDp * 0.4f).dp) // Set the width to 40% of the screen
            .combinedClickable(
                onClick = { onRecipeSelected(recipe) },
                onLongClick = { }
            )
            .clip(CardDefaults.shape),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundCardRecipe
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    val rdb = Room.databaseBuilder(
                        context,
                        RecipesDb::class.java, "RecipesDb"
                    ).build()
                    val res = rdb.recipeDao().findByUrl(recipe.url)
                    rdb.close()
                    if (res != null) {
                        existId.value = res.recipeId
                    }
                    icon.value = if (agenda != null) {
                        Icons.Default.MoreVert
                    } else {
                        if (searching) {
                            if (existId.value != 0.toLong()) {
                                Icons.Filled.Favorite
                            } else {
                                outlinedFav
                            }
                        } else {
                            Icons.Default.MoreVert
                        }
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
                                    val rdb = Room.databaseBuilder(
                                        context,
                                        RecipesDb::class.java, "RecipesDb"
                                    ).build()
                                    if (existId.value == 0.toLong()) {
                                        onRecipeAdded(recipe)
                                        val res = rdb.recipeDao().findByUrl(recipe.url)
                                        rdb.close()
                                        existId.value = res.recipeId
                                        icon.value = Icons.Filled.Favorite
                                    } else {
                                        Log.w("PlanEat", "Delete recipe: ${recipe.title}")
                                        var r = recipe
                                        r.recipeId = existId.value
                                        rdb.recipeDao().delete(r)
                                        rdb.close()
                                        existId.value = 0
                                        icon.value = outlinedFav
                                    }
                                } else {
                                    showDialog.value = true
                                    /*val agendaDb = Room
                                        .databaseBuilder(
                                            context,
                                            AgendaDb::class.java, "AgendaDb"
                                        )
                                        .build()
                                    Log.w("PlanEat", "Selected date: ${selectedDate}")
                                    val todayMiddayMillis = selectedDate
                                        .atTime(12, 0)
                                        .toInstant(ZoneOffset.UTC)
                                        .toEpochMilli()

                                    Log.w("PlanEat", "Recipe: ${recipe.recipeId}, Date: ${todayMiddayMillis}")
                                    if (agenda != null) {
                                        agendaDb.agendaDao().delete(agenda)
                                        onAgendaDeleted(agenda)
                                        agendaDb.close()
                                    } else {
                                        agendaDb
                                            .agendaDao()
                                            .insertAll(
                                                Agenda(
                                                    date = todayMiddayMillis,
                                                    recipeId = recipe.recipeId
                                                )
                                            )
                                        agendaDb.close()
                                        goToAgenda()
                                    }*/
                                }
                            } catch (error: Exception) {
                                Log.d("PlanEat", "Error: $error")
                            }
                        } },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(backgroundCardRecipe)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = icon.value,
                            contentDescription = stringResource(R.string.favorite),
                            tint = textCardRecipe,
                        )



                        if (showDialog.value) {
                            DropdownMenu(
                                expanded = showDialog.value,
                                onDismissRequest = { showDialog.value = false },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(text = "Plan it")
                                    },
                                    onClick = {
                                        showDialog.value = false
                                        // Handle "Plan it" button click
                                        // Call the appropriate function or navigate to the desired screen
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(text = "Edit recipe")
                                    },
                                    onClick = {
                                        showDialog.value = false
                                        // Handle "Edit recipe" button click
                                        // Call the appropriate function or navigate to the desired screen
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(text = "Remove recipe")
                                    },
                                    onClick = {
                                        showDialog.value = false
                                        // Handle "Edit recipe" button click
                                        // Call the appropriate function or navigate to the desired screen
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp, end = 8.dp, bottom = 0.dp),
                color = textCardRecipe,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            SuggestionChip(
                onClick = { /*TODO*/ },
                label = { Text(
                    text = recipe.ingredients.size.toString() + " ingrédients",
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.height(18.dp),

                ) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = tagColor,
                    labelColor = textColor
                ),
                modifier = Modifier
                    .padding(8.dp)
                    .height(24.dp),
                border = BorderStroke(width = 1.dp, color = tagColor)
            )

            Row(
                modifier = Modifier.padding(start = 8.dp, top = 0.dp, bottom = 12.dp),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.schedule),
                    contentDescription = "Schedule",
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = convertDuration(recipe.cookingTime),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                    color = textCardRecipe
                )
            }

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
