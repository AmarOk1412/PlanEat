package com.planeat.planeat.data

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
    }

    // TODO Create classification model

    return tags
}