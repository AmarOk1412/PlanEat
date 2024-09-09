
package com.planeat.planeat.ui.components

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.planeat.planeat.data.Recipe

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(
     ExperimentalFoundationApi::class,
 )
@Composable
fun MinimalRecipeItemList(
    recipe: Recipe,
    onRecipeSelected: (Recipe) -> Unit = {},
) {
    Box(
        modifier = Modifier.height(100.dp).width(100.dp)
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
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = { onRecipeSelected(recipe) },
                    onLongClick = { }
                ),
            contentScale = ContentScale.Crop,
        )
    }

 }
