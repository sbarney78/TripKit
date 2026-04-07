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
import au.barney.tripkit.ui.viewmodel.EntryViewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryScreen(
    listId: Int,
    entryId: Int,
    viewModel: EntryViewModel,
    onBack: () -> Unit
) {
    val entry by viewModel.currentEntry.collectAsState()

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
            onSave = { name, qty, notes, type, imagePath, color, weightGrams ->
                viewModel.updateEntry(
                    entryId = entryId,
                    name = name,
                    quantity = qty,
                    notes = notes,
                    entryType = type,
                    listId = listId,
                    imagePath = imagePath,
                    color = color,
                    weightGrams = weightGrams
                )
                onBack()
            },
            onBack = onBack,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun EditEntryForm(
    entry: Entry,
    onSave: (String, Int, String, String, String?, String, Int) -> Unit,
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
    
    var weightInput by remember(entry) {
        val grams = entry.weightGrams
        mutableStateOf(if (grams >= 1000) (grams / 1000.0).toString() else grams.toString())
    }
    var weightUnit by remember(entry) { mutableStateOf(if (entry.weightGrams >= 1000) "kg" else "g") }

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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (Each)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                var unitExpanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { unitExpanded = true }) {
                        Text(weightUnit)
                    }
                    DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        DropdownMenuItem(text = { Text("g") }, onClick = { weightUnit = "g"; unitExpanded = false })
                        DropdownMenuItem(text = { Text("kg") }, onClick = { weightUnit = "kg"; unitExpanded = false })
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
                    val weightGrams = try {
                        val value = weightInput.toDouble()
                        if (weightUnit == "kg") (value * 1000).toInt() else value.toInt()
                    } catch (e: Exception) { entry.weightGrams }
                    
                    onSave(name, qtyInt, notes, type, imagePath, colorHex, weightGrams)
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
