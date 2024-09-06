package com.planeat.planeat.data

import kotlinx.serialization.Serializable

@Serializable
data class ParsedIngredient(
    val name: String? = null,
    val qty: String? = null,
    val unit: String? = null
)

@Serializable
data class IngredientItem(
    var quantity: Float = 1.0f,  // Default to 1.0 if qty is absent
    var unit: String = "",       // Default to empty string if unit is absent
    var name: String = "" // This should always be present
)
