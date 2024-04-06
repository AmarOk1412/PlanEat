package com.planeat.planeat.connectors

import com.planeat.planeat.data.Recipe

abstract class Connector {
    constructor()
    abstract fun handleUrl(url: String): Boolean
    abstract fun search(searchTerm: String): List<Recipe>
    abstract fun getRecipe(url: String): Recipe
}