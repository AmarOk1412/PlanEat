package com.planeat.planeat.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.compose.onPrimaryContainerLight
import com.example.compose.primaryContainerLight
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.ui.components.calendar.Calendar
import com.planeat.planeat.ui.components.calendar.CalendarDataSource
import com.planeat.planeat.ui.components.calendar.CalendarUiModel
import com.planeat.planeat.ui.components.calendar.RecipeCalendar

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AgendaScreen(
    model: AppModel,
    modifier: Modifier = Modifier,
    onRecipeDeleted: (Recipe) -> Unit,
    dataSource: CalendarDataSource,
    dataUi: CalendarUiModel,
    updateDate: (CalendarUiModel, Boolean) -> Unit,
    goToDetails: (Recipe) -> Unit,
    goToEdition: (Recipe) -> Unit,
    goToAccount: () -> Unit,
) {

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            FloatingActionButton(onClick = { goToAccount() },
                containerColor = primaryContainerLight,
                contentColor = onPrimaryContainerLight,
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.padding(end = 16.dp).size(56.dp)) {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = "Synchronize"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        content = { innerPadding ->
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = innerPadding.calculateTopPadding()),
            ) {
                Calendar(
                    modifier.fillMaxWidth(),
                    dataSource,
                    dataUi,
                    updateDate = updateDate
                )

                RecipeCalendar(
                    model,
                    goToDetails,
                    goToEdition,
                    onRecipeDeleted = onRecipeDeleted,
                    modifier.fillMaxWidth(),
                    dataUi,
                    updateDate = updateDate
                )
            }
        }
    )
}