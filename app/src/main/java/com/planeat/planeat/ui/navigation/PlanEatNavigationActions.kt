package com.planeat.planeat.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.planeat.planeat.R

object PlanEatRoute {
    const val AGENDA = "Agenda"
    const val RECIPES = "Recipes"
    const val PANTRY = "Pantry"
    const val SHOPPING_LIST = "ShoppingList"
    const val DETAILS = "Details"
    const val EDITION = "Edition"
}

data class PlanEatTopLevelDestination(
    val route: String,
    val icon: Int,
    val iconTextId: Int
)

class PlanEatNavigationActions(private val navController: NavHostController) {

    fun navigateTo(destination: PlanEatTopLevelDestination) {
        navController.navigate(destination.route) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
    }
}

val TOP_LEVEL_DESTINATIONS = listOf(
    PlanEatTopLevelDestination(
        route = PlanEatRoute.AGENDA,
        icon = R.drawable.icon_agenda,
        iconTextId = R.string.tab_agenda
    ),
    PlanEatTopLevelDestination(
        route = PlanEatRoute.RECIPES,
        icon = R.drawable.icon_recipes,
        iconTextId = R.string.tab_recipes
    ),
    PlanEatTopLevelDestination(
        route = PlanEatRoute.PANTRY,
        icon = R.drawable.icon_pantry,
        iconTextId = R.string.tab_pantry
    ),
    PlanEatTopLevelDestination(
        route = PlanEatRoute.SHOPPING_LIST,
        icon = R.drawable.icon_shopping_list,
        iconTextId = R.string.tab_shopping_list
    )

)
