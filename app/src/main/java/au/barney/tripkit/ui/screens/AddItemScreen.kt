package au.barney.tripkit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.barney.tripkit.ui.viewmodel.ItemViewModel
import au.barney.tripkit.ui.viewmodel.MasterItemViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    entryId: Int,
    viewModel: ItemViewModel,
    masterViewModel: MasterItemViewModel = viewModel(),
    onDone: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    var isContainer by remember { mutableStateOf(false) }
    var imagePath by remember { mutableStateOf<String?>(null) }

    val masterItems by masterViewModel.masterItems.collectAsState()
    var showDropdown by remember { mutableStateOf(false) }
    val filteredMasterItems = remember(name, showDropdown) {
        if (!showDropdown || name.isEmpty()) emptyList()
        else masterItems.filter { it.name.contains(name, ignoreCase = true) }
    }

    var showMasterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(entryId) {
        viewModel.loadItems(entryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Item to Container") }
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
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = filteredMasterItems.isNotEmpty(),
                        onExpandedChange = { }
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { 
                                name = it
                                showDropdown = true
                            },
                            label = { Text("Item Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        if (filteredMasterItems.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = true,
                                onDismissRequest = { showDropdown = false }
                            ) {
                                filteredMasterItems.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item.name) },
                                        onClick = {
                                            name = item.name
                                            isContainer = item.is_container
                                            imagePath = item.image_path
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
                if (!isContainer) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isContainer, onCheckedChange = { isContainer = it })
                    Text("Is this a sub-container?")
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
                        val qty = if (isContainer) 0 else (quantity.toIntOrNull() ?: 1)
                        val exactMatch = masterItems.any { it.name.equals(name, ignoreCase = true) }
                        
                        if (!exactMatch && name.isNotBlank()) {
                            showMasterDialog = true
                        } else {
                            viewModel.addItem(name, qty, notes, isContainer, imagePath, addToMaster = true)
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
                    masterViewModel.addMasterItem(name, isContainer, imagePath)
                    viewModel.addItem(name, if (isContainer) 0 else (quantity.toIntOrNull() ?: 1), notes, isContainer, imagePath, addToMaster = true)
                    showMasterDialog = false
                    onDone()
                }) { Text("Add & Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.addItem(name, if (isContainer) 0 else (quantity.toIntOrNull() ?: 1), notes, isContainer, imagePath, addToMaster = false)
                    showMasterDialog = false
                    onDone()
                }) { Text("Save Only") }
            }
        )
    }
}
