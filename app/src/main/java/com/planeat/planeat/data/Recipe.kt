package com.planeat.planeat.data

data class Recipe(
    val title: String = "",
    val url: String = "",
    val image: String = "",
    val kindOfMeal: String = "",
    val cookingTime: Int = 0,
    val season: String = "",
    val tags: List<String> = emptyList(),
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList()
)
