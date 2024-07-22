
package com.planeat.planeat.ui

import android.util.Log
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.room.Room
import coil.compose.AsyncImage
import com.planeat.planeat.R
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.ui.components.convertDuration
import com.planeat.planeat.ui.theme.backgroundCardRecipe
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
    Box(modifier = Modifier
        .fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {

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

            Text(
                text = selectedRecipe.url,
                style = MaterialTheme.typography.bodySmall,
            )


            Text(
                text = selectedRecipe.title,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(16.dp)
            )


            Row(
                modifier = Modifier.padding(16.dp, top = 0.dp),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.schedule),
                    contentDescription = "Schedule",
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = convertDuration(selectedRecipe.cookingTime),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                    color = textCardRecipe
                )
            }

            val tabs = listOf(
                stringResource(id = R.string.ingredients),
                stringResource(id = R.string.steps)
            )

            val selectedTabIndex = remember { mutableStateOf(0) }

            TabRow(
                selectedTabIndex = selectedTabIndex.value,
                modifier = Modifier.fillMaxWidth(),
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex.value])
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex.value == index,
                        onClick = { selectedTabIndex.value = index },
                        text = { Text(text = title) }
                    )
                }
            }

            when (selectedTabIndex.value) {
                0 -> {
                    Column (
                        modifier = Modifier.padding(16.dp)
                    ) {
                        selectedRecipe.ingredients.forEach {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }
                1 -> {
                    Column (
                        modifier = Modifier.padding(16.dp)
                    ) {
                        selectedRecipe.steps.forEachIndexed { index, step ->
                            Text(
                                text = "${index + 1}. $step",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}