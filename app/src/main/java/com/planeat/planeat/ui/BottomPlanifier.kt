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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.compose.surfaceContainerLowestLight
import com.planeat.planeat.data.Agenda
import com.planeat.planeat.data.AgendaDb
import com.planeat.planeat.data.Recipe
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
    dataUi: CalendarUiModel,
    toPlanRecipe: Recipe,
    goToAgenda: () -> Unit,
) {
    val context = LocalContext.current

    val selectedSource by remember { mutableStateOf(CalendarDataSource()) }
    var selectedUi by remember { mutableStateOf(selectedSource.getData(lastSelectedDate = selectedSource.today)) }
    val selectedDates = remember { mutableStateListOf<LocalDate>(dataUi.selectedDate.date) }
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
                text = "Choose a date",
                style = MaterialTheme.typography.titleLarge,
            )


            DateHeader(
                dataUi = selectedUi,
                onPrevClickListener = { startDate ->
                    val finalStartDate = startDate.minusDays(1)
                    selectedUi = selectedSource.getData(startDate = finalStartDate, lastSelectedDate = selectedUi.selectedDate.date)
                },
                onNextClickListener = { endDate ->
                    val finalStartDate = endDate.plusDays(2)
                    selectedUi = selectedSource.getData(startDate = finalStartDate, lastSelectedDate = selectedUi.selectedDate.date)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            selectedUi.visibleDates.forEach{ date ->

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = date.date.format(
                            DateTimeFormatter.ofPattern("EEEE d")
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Checkbox(
                        checked = selectedDates.contains(date.date),
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
                        val agendaDb = Room
                            .databaseBuilder(
                                context,
                                AgendaDb::class.java, "AgendaDb"
                            )
                            .build()
                        for (d in selectedDates) {
                            Log.w("PlanEat", "Selected date: ${d}")
                            val dateMidday = d
                                .atTime(12, 0)
                                .toInstant(ZoneOffset.UTC)
                                .toEpochMilli()

                            Log.w("PlanEat", "Recipe: ${toPlanRecipe!!.recipeId}, Date: ${dateMidday}")
                            agendaDb
                                .agendaDao()
                                .insertAll(
                                    Agenda(
                                        date = dateMidday,
                                        recipeId = toPlanRecipe!!.recipeId
                                    )
                                )
                        }
                        agendaDb.close()
                        goToAgenda()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Plan it")
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
                contentDescription = "Back"
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(text = dataUi.startDate.date.format(
                DateTimeFormatter.ofPattern("MMMM yyyy")
            ), modifier = Modifier.align(Alignment.CenterVertically))

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = {
            onNextClickListener(dataUi.endDate.date)
        }) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Next"
            )
        }
    }
}