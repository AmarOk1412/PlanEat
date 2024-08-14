
package com.planeat.planeat.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.PrimaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecipeDetailScreen(
    selectedRecipe: Recipe,
    goBack: () -> Unit,
) {
    val context = LocalContext.current
    val scaffoldState = rememberBottomSheetScaffoldState()
    val uriHandler = LocalUriHandler.current

    val logo = when {
        selectedRecipe.url.contains("ricardo") -> R.drawable.ricardo
        selectedRecipe.url.contains("marmiton") -> R.drawable.marmiton
        selectedRecipe.url.contains("cha-cu.it") -> R.drawable.marmiton // TODO cha-cu.it
        else -> null
    }

    var peekHeightPx by remember { mutableStateOf(0) }

    BackHandler {
        goBack()
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.White),
            ) {
                OutlinedButton(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
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
                        }
                    },
                    modifier = Modifier.weight(1f).padding(8.dp)
                ) {
                    Text(text = "Add to recipes")
                }

                Button(
                    onClick = {
                        // TODO
                    },
                    modifier = Modifier.weight(1f).padding(8.dp)
                ) {
                    Text(text = "Plan it")
                }
            }
        }
    ) {

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = ((with(LocalDensity.current) { (LocalContext.current.resources.displayMetrics.heightPixels - peekHeightPx) / density } + 110).dp),
            sheetContent = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    if (logo != null) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id =logo),
                            contentDescription = "Ricardo", // TODO
                            modifier = Modifier.height(50.dp).width(88.dp).padding(start=16.dp, top=8.dp, bottom = 0.dp)
                                .clickable(onClick = {
                                    uriHandler.openUri(selectedRecipe.url)
                                })
                        )
                    }

                    Text(
                        text = selectedRecipe.title,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(start=16.dp, top=8.dp, bottom = 8.dp, end = 16.dp)
                    )

                    Row(
                        modifier = Modifier.padding(start=16.dp, top=8.dp, bottom = 8.dp, end = 16.dp),
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

                    val selectedTabIndex = remember { mutableIntStateOf(0) }

                    TabRow(
                        selectedTabIndex = selectedTabIndex.intValue,
                        modifier = Modifier.fillMaxWidth(),
                        indicator = { tabPositions ->
                            PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex.intValue])
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex.intValue == index,
                                onClick = { selectedTabIndex.intValue = index },
                                text = { Text(text = title) }
                            )
                        }
                    }

                    when (selectedTabIndex.intValue) {
                        0 -> {
                            Column (
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedRecipe.ingredients.forEach {
                                    Card(
                                        modifier = Modifier
                                            .clip(CardDefaults.shape)
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {  },
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
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                        }
                                    }
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


                    Spacer(modifier = Modifier.height(80.dp))

                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.TopStart
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f / .8f)
                ) {
                    var index = 0
                    AsyncImage(
                        model = selectedRecipe.image,
                        contentDescription = selectedRecipe.title,
                        modifier = Modifier.fillMaxSize().onGloballyPositioned {
                            if (index++ == 0) {
                                peekHeightPx = it.size.height.toInt()
                            }
                        },
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
            }
        }
    }

}