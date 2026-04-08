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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.Entry
import au.barney.tripkit.data.model.PayloadLocation
import au.barney.tripkit.ui.viewmodel.EntryViewModel
import au.barney.tripkit.ui.viewmodel.ListViewModel
import au.barney.tripkit.ui.components.WeightInput
import au.barney.tripkit.ui.components.convertToGrams
import au.barney.tripkit.ui.components.formatWeightForInput

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryScreen(
    listId: Int,
    entryId: Int,
    viewModel: EntryViewModel,
    listViewModel: ListViewModel,
    onBack: () -> Unit
) {
    val entry by viewModel.currentEntry.collectAsState()
    val payloadLocations by listViewModel.payloadLocations.collectAsState()

    // Reload entry every time entryId changes
    LaunchedEffect(entryId) {
        viewModel.loadEntry(entryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->

        if (entry == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        EditEntryForm(
            entry = entry!!,
            payloadLocations = payloadLocations,
            onSave = { name, qty, notes, type, imagePath, color, weightGrams, payloadId ->
                viewModel.updateEntry(
                    entryId = entryId,
                    name = name,
                    quantity = qty,
                    notes = notes,
                    entryType = type,
                    listId = listId,
                    imagePath = imagePath,
                    color = color,
                    weightGrams = weightGrams,
                    payloadLocationId = payloadId
                )
                onBack()
            },
            onBack = onBack,
            modifier = Modifier.padding(padding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryForm(
    entry: Entry,
    payloadLocations: List<PayloadLocation>,
    onSave: (String, Int, String, String, String?, String, Int, Int?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Reset state whenever a NEW entry is loaded
    var name by remember(entry) { mutableStateOf(entry.entry_name) }
    var qty by remember(entry) { mutableStateOf(entry.quantity.toString()) }
    var notes by remember(entry) { mutableStateOf(entry.notes ?: "") }
    var type by remember(entry) { mutableStateOf(entry.entry_type) }
    var imagePath by remember(entry) { mutableStateOf(entry.image_path) }
    var colorHex by remember(entry) { mutableStateOf(entry.color) }
    var payloadId by remember(entry) { mutableStateOf(entry.payloadLocationId) }

    var weightInput by remember(entry) {
        val (input, _) = formatWeightForInput(entry.weightGrams)
        mutableStateOf(input)
    }
    var weightUnit by remember(entry) {
        val (_, unit) = formatWeightForInput(entry.weightGrams)
        mutableStateOf(unit)
    }

    val isContainer = entry.entry_type == "container"

    val presetColors = listOf(
        "#800000", "#FF0000", "#FF4500", "#FF8C00", "#FFD700",
        "#008000", "#006400", "#228B22", "#008080", "#000080",
        "#0000FF", "#4B0082", "#800080", "#FF00FF", "#000000"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
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
                value = qty,
                onValueChange = { qty = it.filter { c -> c.isDigit() } },
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
            Column {
                Text(
                    "Item Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (isContainer) {
                    Text(
                        "This item is a container and cannot be converted to a single item.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text("Container") },
                        enabled = false
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = type == "container",
                            onCheckedChange = {
                                type = if (it) "container" else "single"
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (type == "container") "Container" else "Single Item")
                    }
                }
            }
        }

        if (type == "container") {
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
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val qtyInt = qty.toIntOrNull() ?: 1
                    val weightGrams = convertToGrams(weightInput, weightUnit)
                    
                    onSave(name, qtyInt, notes, type, imagePath, colorHex, weightGrams, payloadId)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}
