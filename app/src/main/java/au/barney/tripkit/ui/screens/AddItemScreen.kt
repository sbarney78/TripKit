package au.barney.tripkit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

    val masterItems by masterViewModel.masterItems.collectAsState()
    var showDropdown by remember { mutableStateOf(false) }
    val filteredMasterItems = remember(name, showDropdown) {
        if (!showDropdown || name.isEmpty()) emptyList()
        else masterItems.filter { !it.is_container && it.name.contains(name, ignoreCase = true) }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

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
                                        showDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantity") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 1
                    val exactMatch = masterItems.any { it.name.equals(name, ignoreCase = true) }
                    
                    if (!exactMatch && name.isNotBlank()) {
                        showMasterDialog = true
                    } else {
                        viewModel.addItem(name, qty, notes)
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
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
                    viewModel.addItem(name, quantity.toIntOrNull() ?: 1, notes)
                    showMasterDialog = false
                    onDone()
                }) { Text("Add & Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.addItem(name, quantity.toIntOrNull() ?: 1, notes)
                    showMasterDialog = false
                    onDone()
                }) { Text("Save Only") }
            }
        )
    }
}
