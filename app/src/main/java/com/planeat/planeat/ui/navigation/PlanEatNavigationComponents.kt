package com.planeat.planeat.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.example.compose.onSurfaceVariantLight
import com.example.compose.primaryLight
import com.example.compose.surfaceContainerLowLight
import com.example.compose.surfaceContainerLowestLight
import com.planeat.planeat.R
import com.planeat.planeat.data.Recipe

@Composable
fun PlanEatNavigationRail(
    selectedDestination: String,
    navigateToTopLevelDestination: (PlanEatTopLevelDestination) -> Unit,
    onDrawerClicked: () -> Unit = {},
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.inverseOnSurface
    ) {
        Column(
            modifier = Modifier.layoutId(LayoutType.HEADER),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NavigationRailItem(
                selected = false,
                onClick = onDrawerClicked,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(id = R.string.navigation_drawer)
                    )
                }
            )
            FloatingActionButton(
                onClick = { /*TODO*/ },
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.edit),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(8.dp)) // NavigationRailHeaderPadding
            Spacer(Modifier.height(4.dp)) // NavigationRailVerticalPadding
        }

        Column(
            modifier = Modifier.layoutId(LayoutType.CONTENT),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TOP_LEVEL_DESTINATIONS.forEach { planEatDestination ->
                NavigationRailItem(
                    selected = selectedDestination == planEatDestination.route,
                    onClick = { navigateToTopLevelDestination(planEatDestination) },
                    icon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(planEatDestination.icon),
                            contentDescription = stringResource(
                                id = planEatDestination.iconTextId
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun PlanEatBottomNavigationBar(
    selectedDestination: String,
    navigateToTopLevelDestination: (PlanEatTopLevelDestination) -> Unit,
    bottomBarState: MutableState<Recipe?>
) {
    AnimatedVisibility(
        visible = bottomBarState.value == null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        content = {
            NavigationBar(
                containerColor = surfaceContainerLowestLight,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp, 32.dp, 0.dp, 0.dp))) {
                TOP_LEVEL_DESTINATIONS.forEach { planEatDestination ->
                    NavigationBarItem(
                        selected = selectedDestination == planEatDestination.route,
                        onClick = { navigateToTopLevelDestination(planEatDestination) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = surfaceContainerLowLight, selectedIconColor = primaryLight, selectedTextColor = primaryLight, unselectedIconColor = onSurfaceVariantLight, unselectedTextColor = onSurfaceVariantLight),
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(planEatDestination.icon),
                                contentDescription = stringResource(id = planEatDestination.iconTextId)
                            )
                        },
                        label = { Text(text = stringResource(id = planEatDestination.iconTextId)) }
                    )
                }
            }
        }
    )
}

enum class LayoutType {
    HEADER, CONTENT
}
