package au.barney.tripkit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.MenuItem
import au.barney.tripkit.ui.viewmodel.MenuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMealScreen(
    meal: MenuItem,
    viewModel: MenuViewModel,
    onBack: () -> Unit
) {
    val days = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday",
        "Friday", "Saturday", "Sunday"
    )

    val mealTypes = listOf(
        "Breakfast", "Morning Tea", "Lunch", "Afternoon Tea",
        "Dinner", "Dessert", "Supper"
    )

    var day by remember { mutableStateOf(meal.day) }
    var dayExpanded by remember { mutableStateOf(false) }

    var mealType by remember { mutableStateOf(meal.meal_type) }
    var mealTypeExpanded by remember { mutableStateOf(false) }

    var description by remember { mutableStateOf(meal.description) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Meal") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("<") }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // DAY DROPDOWN
            ExposedDropdownMenuBox(
                expanded = dayExpanded,
                onExpandedChange = { dayExpanded = !dayExpanded }
            ) {
                TextField(
                    value = day,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Day") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) }
                )

                ExposedDropdownMenu(
                    expanded = dayExpanded,
                    onDismissRequest = { dayExpanded = false }
                ) {
                    days.forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                day = it
                                dayExpanded = false
                            }
                        )
                    }
                }
            }

            // MEAL TYPE DROPDOWN
            ExposedDropdownMenuBox(
                expanded = mealTypeExpanded,
                onExpandedChange = { mealTypeExpanded = !mealTypeExpanded }
            ) {
                TextField(
                    value = mealType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Meal Type") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mealTypeExpanded) }
                )

                ExposedDropdownMenu(
                    expanded = mealTypeExpanded,
                    onDismissRequest = { mealTypeExpanded = false }
                ) {
                    mealTypes.forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                mealType = it
                                mealTypeExpanded = false
                            }
                        )
                    }
                }
            }

            // DESCRIPTION
            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.updateMenuItem(
                        meal.id,
                        day,
                        mealType,
                        description,
                        meal.list_id   // FIXED
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    }
}
