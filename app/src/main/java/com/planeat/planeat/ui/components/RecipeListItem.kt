
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.planeat.planeat.R
import com.planeat.planeat.data.Agenda
import com.planeat.planeat.data.AgendaDb
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.ui.theme.backgroundCardRecipe
import com.planeat.planeat.ui.theme.tagColor0
import com.planeat.planeat.ui.theme.tagColor1
import com.planeat.planeat.ui.theme.tagColor2
import com.planeat.planeat.ui.theme.tagColor3
import com.planeat.planeat.ui.theme.tagColor4
import com.planeat.planeat.ui.theme.tagColor5
import com.planeat.planeat.ui.theme.tagColor6
import com.planeat.planeat.ui.theme.tagColor7
import com.planeat.planeat.ui.theme.tagColor8
import com.planeat.planeat.ui.theme.textCardRecipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(
     ExperimentalFoundationApi::class,
 )
@Composable
fun RecipeListItem(
    recipe: Recipe,
    onRecipeSelected: (Recipe) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .clip(CardDefaults.shape)
            .width((LocalConfiguration.current.screenWidthDp * 0.4f).dp) // Set the width to 40% of the screen
            .combinedClickable(
                onClick = { onRecipeSelected(recipe) },
                onLongClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val agendaDb = Room
                            .databaseBuilder(
                                context,
                                AgendaDb::class.java, "AgendaDb"
                            )
                            .build()
                        val todayMiddayMillis = Instant
                            .now()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .atTime(12, 0)
                            .toInstant(ZoneOffset.UTC)
                            .toEpochMilli()

                        Log.w("PlanEat", "Recipe: ${recipe.recipeId}, Date: ${todayMiddayMillis}")
                        agendaDb
                            .agendaDao()
                            .insertAll(
                                Agenda(
                                    date = todayMiddayMillis,
                                    recipeId = recipe.recipeId
                                )
                            )
                    }
                }
            )
            .clip(CardDefaults.shape),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundCardRecipe
        ),
    ) {
        val exists = remember { mutableStateOf(false) }
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
                    res != null
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f / .8f)
            ) {
                AsyncImage(
                    model = recipe.image,
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
                        onClick = { CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val rdb = Room.databaseBuilder(
                                    context,
                                    RecipesDb::class.java, "RecipesDb"
                                ).build()
                                if (exists.value) {
                                    rdb.recipeDao().delete(recipe)
                                } else {
                                    rdb.recipeDao().insertAll(recipe)
                                }
                                rdb.close()
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
                            imageVector = ImageVector.vectorResource(R.drawable.favorite),
                            contentDescription = stringResource(R.string.favorite),
                            tint = textCardRecipe,
                        )
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

            Row(
                modifier = Modifier.padding(top = 12.dp, start = 8.dp).fillMaxWidth(),
            ) {
                for (i in 0 until minOf(recipe.tags.size, 2)) {
                    val tag = recipe.tags[i]
                    var chipColor = tagColor0
                    if (!tag.isEmpty()) {
                        val colorIndex = tag.first().toInt() % 8
                        chipColor = when (colorIndex) {
                            0 -> tagColor0
                            1 -> tagColor1
                            2 -> tagColor2
                            3 -> tagColor3
                            4 -> tagColor4
                            5 -> tagColor5
                            6 -> tagColor6
                            7 -> tagColor7
                            else -> tagColor8
                        }
                    }

                    SuggestionChip(
                        onClick = { /*TODO*/ },
                        label = { Text(
                            text = tag,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.height(18.dp),

                        ) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = chipColor.copy(alpha = 0.5f),
                            labelColor = chipColor
                        ),
                        modifier = Modifier.padding(end = 8.dp).fillMaxWidth(0.5f).height(24.dp),
                        border = BorderStroke(width = 1.dp, color = chipColor)
                    )

                }
            }


            Row(
                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 12.dp),
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
