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
import au.barney.tripkit.ui.viewmodel.ItemViewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    itemId: Int,
    viewModel: ItemViewModel,
    onBack: () -> Unit
) {
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val item by viewModel.currentItem.collectAsState()

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
                    val currentItem = item!!
                    EditItemForm(
                        itemId = itemId,
                        nameInitial = currentItem.item_name,
                        quantityInitial = currentItem.quantity,
                        notesInitial = currentItem.notes ?: "",
                        isContainerInitial = currentItem.is_container,
                        imagePathInitial = currentItem.image_path,
                        colorInitial = currentItem.color,
                        weightGramsInitial = currentItem.weightGrams,
                        onSave = { name, quantity, notes, isContainer, imagePath, color, weightGrams ->
                            viewModel.updateItem(
                                itemId = itemId,
                                name = name,
                                quantity = quantity,
                                notes = notes,
                                isContainer = isContainer,
                                imagePath = imagePath,
                                color = color,
                                weightGrams = weightGrams
                            )
                            onBack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditItemForm(
    itemId: Int,
    nameInitial: String,
    quantityInitial: Int,
    notesInitial: String,
    isContainerInitial: Boolean,
    imagePathInitial: String?,
    colorInitial: String,
    weightGramsInitial: Int,
    onSave: (String, Int, String?, Boolean, String?, String, Int) -> Unit
) {
    var name by remember(itemId) { mutableStateOf(nameInitial) }
    var quantity by remember(itemId) { mutableStateOf(quantityInitial.toString()) }
    var notes by remember(itemId) { mutableStateOf(notesInitial) }
    var isContainer by remember(itemId) { mutableStateOf(isContainerInitial) }
    var imagePath by remember(itemId) { mutableStateOf(imagePathInitial) }
    var colorHex by remember(itemId) { mutableStateOf(colorInitial) }

    var weightInput by remember(itemId) {
        val grams = weightGramsInitial
        mutableStateOf(if (grams >= 1000) (grams / 1000.0).toString() else grams.toString())
    }
    var weightUnit by remember(itemId) { mutableStateOf(if (weightGramsInitial >= 1000) "kg" else "g") }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isContainer,
                    onCheckedChange = { isContainer = it },
                    enabled = !isContainerInitial // Cannot change back to single item if already a container
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
                    val qty = quantity.toIntOrNull() ?: 1
                    val weightGrams = try {
                        val value = weightInput.toDouble()
                        if (weightUnit == "kg") (value * 1000).toInt() else value.toInt()
                    } catch (e: Exception) { weightGramsInitial }

                    onSave(name, qty, notes, isContainer, imagePath, colorHex, weightGrams)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
