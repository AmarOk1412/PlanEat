
package com.planeat.planeat.ui

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.room.Room
import coil.compose.AsyncImage
import com.planeat.planeat.R
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

@Composable
fun RecipeDetailScreen(
    selectedRecipe: Recipe,
    goBack: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = selectedRecipe.title,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // TODO better icon/redesign
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / .8f)
        ) {
            AsyncImage(
                model = selectedRecipe.image,
                contentDescription = selectedRecipe.title,
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
                            if (selectedRecipe.recipeId != 0L) {
                                rdb.recipeDao().delete(selectedRecipe)
                            } else {
                                rdb.recipeDao().insertAll(selectedRecipe)
                            }
                            rdb.close()
                            goBack()
                        } catch (error: Exception) {
                            Log.d("PlanEat", "Error: $error")
                        }
                    } },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(backgroundCardRecipe)
                        .align(Alignment.TopEnd)
                ) {
                    val img = if (selectedRecipe.recipeId == 0L) ImageVector.vectorResource(R.drawable.favorite) else Icons.Filled.Favorite
                    Icon(
                        imageVector = img,
                        contentDescription = stringResource(R.string.favorite),
                        tint = textCardRecipe,
                    )
                }
            }
        }


        Row(
            modifier = Modifier.padding(top = 12.dp, start = 8.dp).fillMaxWidth(),
        ) {
            for (i in 0 until selectedRecipe.tags.size) {
                val tag = selectedRecipe.tags[i]
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


        Text(
            text = stringResource(id = R.string.ingredients),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        selectedRecipe.ingredients.forEach {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Text(
            text = stringResource(id = R.string.steps),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        selectedRecipe.steps.forEachIndexed { index, step ->
            Text(
                text = "${index + 1}. $step",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}