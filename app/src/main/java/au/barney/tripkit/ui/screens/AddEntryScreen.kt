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
import au.barney.tripkit.ui.viewmodel.EntryViewModel
import au.barney.tripkit.ui.viewmodel.MasterItemViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    listId: Int,
    viewModel: EntryViewModel,
    masterViewModel: MasterItemViewModel,
    onDone: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("single") }
    var colorHex by remember { mutableStateOf("#800000") }
    var imagePath by remember { mutableStateOf<String?>(null) }
    
    var weightInput by remember { mutableStateOf("") }
    var weightUnit by remember { mutableStateOf("g") }

    val masterItems by masterViewModel.masterItems.collectAsState()
    var showDropdown by remember { mutableStateOf(false) }
    val filteredMasterItems = remember(name, showDropdown) {
        if (!showDropdown || name.isEmpty()) emptyList()
        else masterItems.filter { it.name.contains(name, ignoreCase = true) }
    }

    var showMasterDialog by remember { mutableStateOf(false) }

    val presetColors = listOf(
        "#800000", "#FF0000", "#FF4500", "#FF8C00", "#FFD700",
        "#008000", "#006400", "#228B22", "#008080", "#000080",
        "#0000FF", "#4B0082", "#800080", "#FF00FF", "#000000"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Entry") }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            showDropdown = true
                        },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (filteredMasterItems.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            LazyColumn {
                                items(filteredMasterItems) { item ->
                                    DropdownMenuItem(
                                        text = { Text(item.name) },
                                        onClick = {
                                            name = item.name
                                            type = if (item.is_container) "container" else "single"
                                            imagePath = item.image_path
                                            colorHex = item.color
                                            weightInput = if (item.weightGrams >= 1000) (item.weightGrams / 1000.0).toString() else item.weightGrams.toString()
                                            weightUnit = if (item.weightGrams >= 1000) "kg" else "g"
                                            showDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
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
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = "Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = type == "single",
                        onClick = { type = "single" }
                    )
                    Text("Single Item")

                    Spacer(modifier = Modifier.width(20.dp))

                    RadioButton(
                        selected = type == "container",
                        onClick = { type = "container" }
                    )
                    Text("Container")
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
                        val qty = quantity.toIntOrNull() ?: 1
                        val weightGrams = try {
                            val value = weightInput.toDouble()
                            if (weightUnit == "kg") (value * 1000).toInt() else value.toInt()
                        } catch (e: Exception) { 0 }

                        val exactMatch = masterItems.any { it.name.equals(name, ignoreCase = true) }

                        if (!exactMatch && name.isNotBlank()) {
                            showMasterDialog = true
                        } else {
                            viewModel.addEntry(listId, name, qty, notes, type, imagePath, addToMaster = false, color = colorHex, weightGrams = weightGrams)
                            onDone()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
            }
        }
    }

    if (showMasterDialog) {
        AlertDialog(
            onDismissRequest = { showMasterDialog = false },
            title = { Text("Add to Master List?") },
            text = { Text("'$name' is not in your Master Inventory. Would you like to add it for future use?") },
            confirmButton = {
                Button(onClick = {
                    val qty = quantity.toIntOrNull() ?: 1
                    val weightGrams = try {
                        val value = weightInput.toDouble()
                        if (weightUnit == "kg") (value * 1000).toInt() else value.toInt()
                    } catch (e: Exception) { 0 }

                    viewModel.addEntry(listId, name, qty, notes, type, imagePath, addToMaster = true, color = colorHex, weightGrams = weightGrams)
                    showMasterDialog = false
                    onDone()
                }) { Text("Add & Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val qty = quantity.toIntOrNull() ?: 1
                    val weightGrams = try {
                        val value = weightInput.toDouble()
                        if (weightUnit == "kg") (value * 1000).toInt() else value.toInt()
                    } catch (e: Exception) { 0 }

                    viewModel.addEntry(listId, name, qty, notes, type, imagePath, addToMaster = false, color = colorHex, weightGrams = weightGrams)
                    showMasterDialog = false
                    onDone()
                }) { Text("Save Only") }
            }
        )
    }
}
