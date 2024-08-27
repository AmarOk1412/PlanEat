package com.planeat.planeat.data

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.planeat.planeat.R

enum class Tags {
    All,
    Easy,
    Medium,
    Hard,
    Appetizer,
    Healthy,
    MiddleEastern,
    Asian,
    Vegetarian,
    Desserts,
    ComfortFood,
    European,
    Bakery,
    Seafood,
    American,
    Drinks;

    override fun toString(): String {
        return name.replace(Regex("([A-Z])"), " $1").trim()
    }
}

@Composable
fun toTagIcon(tag: Tags) {
    if (tag == Tags.Easy) {
        return Image(painter = painterResource(R.drawable.cooking_3d),
                     contentDescription = null,
                     modifier = Modifier.size(24.dp))
    } else if (tag == Tags.Medium) {
        return Image(painter = painterResource(R.drawable.curry_rice_3d),
                     contentDescription = null,
                     modifier = Modifier.size(24.dp))
    } else if (tag == Tags.Hard) {
        return Image(painter = painterResource(R.drawable.birthday_cake_3d),
                     contentDescription = null,
                     modifier = Modifier.size(24.dp))
    } else if (tag == Tags.Appetizer) {
        return Image(painter = painterResource(R.drawable.clinking_beer_mugs_3d),
                     contentDescription = null,
                     modifier = Modifier.size(24.dp))
    } else if (tag == Tags.Healthy) {
        return Image(painter = painterResource(R.drawable.green_salad_3d),
                     contentDescription = null,
                     modifier = Modifier.size(24.dp))
    } else if (tag == Tags.MiddleEastern) {
        return Image(painter = painterResource(R.drawable.falafel_3d),
                     contentDescription = null,
                     modifier = Modifier.size(24.dp))
    } else if (tag == Tags.Asian) {
        return Image(painter = painterResource(R.drawable.dumpling_3d),
                     contentDescription = null,
                     modifier = Modifier.size(24.dp))
    } else if (tag == Tags.Vegetarian) {
        return Image(painter = painterResource(R.drawable.broccoli_3d),
                     contentDescription = null,
                     modifier = Modifier.size(24.dp))
    } else if (tag == Tags.Desserts) {
        return Image(painter = painterResource(R.drawable.pie_3d),
                     contentDescription = null,
                     modifier = Modifier.size(24.dp))
    } else if (tag == Tags.ComfortFood) {
        return Image(painter = painterResource(R.drawable.pizza_3d),
                     contentDescription = null,
                     modifier = Modifier.size(24.dp))
    } else if (tag == Tags.European) {
        return Image(painter = painterResource(R.drawable.fondue_3d),
                     contentDescription = null,
                     modifier = Modifier.size(24.dp))
    } else if (tag == Tags.Bakery) {
        return Image(painter = painterResource(R.drawable.croissant_3d),
                     contentDescription = null,
                     modifier = Modifier.size(24.dp))
    } else if (tag == Tags.Seafood) {
        return Image(painter = painterResource(R.drawable.shrimp_3d),
                     contentDescription = null,
                     modifier = Modifier.size(24.dp))
    } else if (tag == Tags.American) {
        return Image(painter = painterResource(R.drawable.hamburger_3d),
                     contentDescription = null,
                     modifier = Modifier.size(24.dp))
    } else if (tag == Tags.Drinks) {
        return Image(painter = painterResource(R.drawable.cocktail_glass_3d),
                     contentDescription = null,
                     modifier = Modifier.size(24.dp))
    }
    return  Icon(Icons.Filled.Star, contentDescription = null)
}

fun toTags(recipe: Recipe): List<Tags> {
    val tags = mutableListOf<Tags>()
    // Add logic to determine the tags based on the recipe
    if (recipe.cookingTime < 30) {
        tags.add(Tags.Easy)
    } else if (recipe.cookingTime < 60) {
        tags.add(Tags.Medium)
    } else {
        tags.add(Tags.Hard)
    }

    if (recipe.tags.contains("poisson")) {
        tags.add(Tags.Seafood)
    } else if (recipe.tags.contains("boisson") || recipe.tags.contains("concktail")) {
        tags.add(Tags.Drinks)
    } else if (recipe.tags.contains("petit-déjeuner")) {
        tags.add(Tags.Bakery)
    } else if (recipe.tags.contains("pâtisserie")) {
        tags.add(Tags.Desserts)
    }

    // TODO Create classification model

    return tags
}