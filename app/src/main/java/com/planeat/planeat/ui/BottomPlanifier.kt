package com.planeat.planeat.ui

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.compose.onBackgroundLight
import com.example.compose.onPrimaryContainerLight
import com.example.compose.outlineVariantLight
import com.example.compose.primaryContainerLight
import com.example.compose.surfaceContainerLowestLight
import com.example.compose.surfaceLight
import com.planeat.planeat.R
import com.planeat.planeat.data.Agenda
import com.planeat.planeat.data.AgendaDb
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.ui.components.calendar.CalendarDataSource
import com.planeat.planeat.ui.components.calendar.CalendarUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BottomPlanifier(
    onDismissRequest: () -> Unit,
    toPlanRecipe: Recipe,
    goToAgenda: () -> Unit,
) {
    val context = LocalContext.current

    val selectedSource by remember { mutableStateOf(CalendarDataSource()) }
    var selectedUi by remember { mutableStateOf(selectedSource.getData(lastSelectedDate = selectedSource.today)) }
    val selectedDates = remember { mutableStateListOf<LocalDate>() }
    val skipPartiallyExpanded by rememberSaveable { mutableStateOf(true) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)

    ModalBottomSheet(
        containerColor = surfaceContainerLowestLight,
        onDismissRequest = onDismissRequest,
        sheetState = bottomSheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {

            Text(
                text = stringResource(R.string.choose_a_date),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            DateHeader(
                dataUi = selectedUi,
                onPrevClickListener = { startDate ->
                    val finalStartDate = startDate.minusDays(7)
                    selectedUi = selectedSource.getData(startDate = finalStartDate, lastSelectedDate = selectedUi.selectedDate.date)
                },
                onNextClickListener = { endDate ->
                    val finalStartDate = endDate.plusDays(1)
                    selectedUi = selectedSource.getData(startDate = finalStartDate, lastSelectedDate = selectedUi.selectedDate.date)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = surfaceLight)

            selectedUi.visibleDates.forEach{ date ->

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = date.date.format(
                            DateTimeFormatter.ofPattern("EEEE d")
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                    )
                    Checkbox(
                        checked = selectedDates.contains(date.date),
                        modifier = Modifier.align(Alignment.CenterVertically),
                        colors = CheckboxDefaults.colors(uncheckedColor = outlineVariantLight, checkedColor = onBackgroundLight),
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                selectedDates.add(date.date)
                            } else {
                                selectedDates.remove(date.date)
                            }
                        }
                    )
                }


            }

            Button(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val agendaDb = AgendaDb.getDatabase(context)

                        if (toPlanRecipe.recipeId == 0.toLong()) {
                            val recipesDb = RecipesDb.getDatabase(context)
                            val newRecipe = recipesDb.recipeDao().findByUrl(toPlanRecipe.url)
                            if (newRecipe != null) {
                                toPlanRecipe.recipeId = newRecipe.recipeId
                            } else {
                                recipesDb.recipeDao().insertAll(listOf(toPlanRecipe))
                                val newRecipe = recipesDb.recipeDao().findByUrl(toPlanRecipe.url)
                                if (newRecipe != null) {
                                    toPlanRecipe.recipeId = newRecipe.recipeId
                                } else {
                                    Log.e("PlanEat", "Recipe not found")
                                    goToAgenda()
                                    return@launch
                                }
                            }
                        }
                        for (d in selectedDates) {
                            Log.w("PlanEat", "Selected date: ${d}")
                            val dateMidday = d
                                .atTime(12, 0)
                                .toInstant(ZoneOffset.UTC)
                                .toEpochMilli()

                            Log.w("PlanEat", "Recipe: ${toPlanRecipe.recipeId}, Date: ${dateMidday}")
                            agendaDb
                                .agendaDao()
                                .insertAll(listOf(
                                    Agenda(
                                        date = dateMidday,
                                        recipeId = toPlanRecipe.recipeId
                                    ))
                                )
                        }
                        goToAgenda()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryContainerLight, contentColor = onPrimaryContainerLight)
            ) {
                Text(text = stringResource(R.string.plan_it),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 10.dp))
            }
        }
    }
}

@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun DateHeader(
    dataUi: CalendarUiModel,
    onPrevClickListener: (LocalDate) -> Unit,
    onNextClickListener: (LocalDate) -> Unit,
) {
    Row (modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)) {
        IconButton(onClick = {
            onPrevClickListener(dataUi.startDate.date)
        }) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = stringResource(R.string.back)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(text = dataUi.startDate.date.format(
                DateTimeFormatter.ofPattern("MMMM yyyy")
            ), modifier = Modifier.align(Alignment.CenterVertically),
            style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = {
            onNextClickListener(dataUi.endDate.date)
        }) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription =  stringResource(R.string.next)
            )
        }
    }
}