package au.barney.tripkit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.Item
import au.barney.tripkit.data.model.PayloadLocation
import au.barney.tripkit.ui.viewmodel.ItemViewModel
import au.barney.tripkit.ui.viewmodel.ListViewModel
import au.barney.tripkit.ui.components.WeightInput
import au.barney.tripkit.ui.components.convertToGrams
import au.barney.tripkit.ui.components.formatWeightForInput

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    itemId: Int,
    viewModel: ItemViewModel,
    listViewModel: ListViewModel,
    onBack: () -> Unit
) {
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val item by viewModel.currentItem.collectAsState()
    val payloadLocations by listViewModel.payloadLocations.collectAsState()

    // Reload item every time itemId changes
    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when {
                loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                error != null -> {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                item != null -> {
                    EditItemForm(
                        item = item!!,
                        payloadLocations = payloadLocations,
                        onSave = { name, quantity, notes, isContainer, entryId, imagePath, color, weightGrams, payloadId ->
                            viewModel.updateItem(
                                itemId = itemId,
                                name = name,
                                quantity = quantity,
                                notes = notes,
                                isContainer = isContainer,
                                entryId = entryId,
                                imagePath = imagePath,
                                color = color,
                                weightGrams = weightGrams,
                                payloadLocationId = payloadId
                            )
                            onBack()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditItemForm(
    item: Item,
    payloadLocations: List<PayloadLocation>,
    onSave: (String, Int, String?, Boolean, Int, String?, String, Int, Int?) -> Unit
) {
    var name by remember(item) { mutableStateOf(item.item_name) }
    var quantity by remember(item) { mutableStateOf(item.quantity.toString()) }
    var notes by remember(item) { mutableStateOf(item.notes ?: "") }
    var isContainer by remember(item) { mutableStateOf(item.is_container) }
    var imagePath by remember(item) { mutableStateOf(item.image_path) }
    var colorHex by remember(item) { mutableStateOf(item.color) }
    var payloadId by remember(item) { mutableStateOf(item.payloadLocationId) }

    var weightInput by remember(item) {
        val (input, _) = formatWeightForInput(item.weightGrams)
        mutableStateOf(input)
    }
    var weightUnit by remember(item) {
        val (_, unit) = formatWeightForInput(item.weightGrams)
        mutableStateOf(unit)
    }

    val presetColors = listOf(
        "#800000", "#FF0000", "#FF4500", "#FF8C00", "#FFD700",
        "#008000", "#006400", "#228B22", "#008080", "#000080",
        "#0000FF", "#4B0082", "#800080", "#FF00FF", "#000000"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                label = { Text("Quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            WeightInput(
                weightInput = weightInput,
                onWeightInputChange = { weightInput = it },
                weightUnit = weightUnit,
                onWeightUnitChange = { weightUnit = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            var expanded by remember { mutableStateOf(false) }
            val selectedPayloadName = payloadLocations.find { it.id == payloadId }?.name ?: "Unassigned"

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedPayloadName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payload Location") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Unassigned") },
                        onClick = {
                            payloadId = null
                            expanded = false
                        }
                    )
                    payloadLocations.forEach { location ->
                        DropdownMenuItem(
                            text = { Text(location.name) },
                            onClick = {
                                payloadId = location.id
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isContainer,
                    onCheckedChange = { isContainer = it },
                    enabled = !item.is_container // Cannot change back to single item if already a container
                )
                Text("Is this a sub-container?")
            }
        }

        if (isContainer) {
            item {
                Text("Container Line Color:", style = MaterialTheme.typography.bodyMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    items(presetColors) { hex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (colorHex == hex) 3.dp else 1.dp,
                                    color = if (colorHex == hex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }
        }

        item {
            ImagePicker(
                currentImagePath = imagePath,
                onImageSelected = { imagePath = it }
            )
        }

        item {
            Button(
                onClick = {
                    val qtyVal = quantity.toIntOrNull() ?: 1
                    val weightGrams = convertToGrams(weightInput, weightUnit)

                    onSave(name, qtyVal, notes, isContainer, item.entry_id, imagePath, colorHex, weightGrams, payloadId)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
