package com.planeat.planeat.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.planeat.planeat.R

object PlanEatRoute {
    const val AGENDA = "Agenda"
    const val SEARCH = "Recipes"
    const val SAVED = "Pantry"
    const val SHOPPING_LIST = "ShoppingList"
    const val DETAILS = "Details"
    const val EDITION = "Edition"
    const val ACCOUNT = "Account"
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
        route = PlanEatRoute.SEARCH,
        icon = R.drawable.icon_search,
        iconTextId = R.string.tab_search
    ),
    PlanEatTopLevelDestination(
        route = PlanEatRoute.SAVED,
        icon = R.drawable.favorite,
        iconTextId = R.string.tab_saved
    ),
    PlanEatTopLevelDestination(
        route = PlanEatRoute.SHOPPING_LIST,
        icon = R.drawable.icon_shopping_list,
        iconTextId = R.string.tab_shopping_list
    )

)
