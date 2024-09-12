package com.planeat.planeat.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.planeat.planeat.R
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.ui.components.calendar.Calendar
import com.planeat.planeat.ui.components.calendar.CalendarDataSource
import com.planeat.planeat.ui.components.calendar.CalendarUiModel
import com.planeat.planeat.ui.components.calendar.RecipeCalendar

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AgendaScreen(
    modifier: Modifier = Modifier,
    onRecipeDeleted: (Recipe) -> Unit,
    dataSource: CalendarDataSource,
    dataUi: CalendarUiModel,
    updateDate: (CalendarUiModel, Boolean) -> Unit,
    goToDetails: (Recipe) -> Unit,
    goToEdition: (Recipe) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.tab_agenda),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 16.dp, start = 16.dp)
                )

                Calendar(
                    modifier.fillMaxWidth().padding(bottom = 8.dp),
                    dataSource,
                    dataUi,
                    updateDate = updateDate
                )

                RecipeCalendar(
                    goToDetails,
                    goToEdition,
                    onRecipeDeleted = onRecipeDeleted,
                    modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    dataUi,
                    updateDate = updateDate
                )
            }
        }
    }
}