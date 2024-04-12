/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.planeat.planeat.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.planeat.planeat.R

object ReplyRoute {
    const val AGENDA = "Agenda"
    const val RECIPES = "Recipes"
    const val PANTRY = "Pantry"
    const val SHOPPING_LIST = "ShoppingList"
}

data class ReplyTopLevelDestination(
    val route: String,
    val icon: Int,
    val iconTextId: Int
)

class ReplyNavigationActions(private val navController: NavHostController) {

    fun navigateTo(destination: ReplyTopLevelDestination) {
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
    ReplyTopLevelDestination(
        route = ReplyRoute.AGENDA,
        icon = R.drawable.icon_agenda,
        iconTextId = R.string.tab_agenda
    ),
    ReplyTopLevelDestination(
        route = ReplyRoute.RECIPES,
        icon = R.drawable.icon_recipes,
        iconTextId = R.string.tab_recipes
    ),
    ReplyTopLevelDestination(
        route = ReplyRoute.PANTRY,
        icon = R.drawable.icon_pantry,
        iconTextId = R.string.tab_pantry
    ),
    ReplyTopLevelDestination(
        route = ReplyRoute.SHOPPING_LIST,
        icon = R.drawable.icon_shopping_list,
        iconTextId = R.string.tab_shopping_list
    )

)
