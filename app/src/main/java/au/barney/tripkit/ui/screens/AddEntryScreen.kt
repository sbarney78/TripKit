package au.barney.tripkit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

    val masterItems by masterViewModel.masterItems.collectAsState()
    var showDropdown by remember { mutableStateOf(false) }
    val filteredMasterItems = remember(name, showDropdown) {
        if (!showDropdown || name.isEmpty()) emptyList()
        else masterItems.filter { it.name.contains(name, ignoreCase = true) }
    }

    var showMasterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Entry") }
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

            Text(
                text = "Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

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

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 1
                    val exactMatch = masterItems.any { it.name.equals(name, ignoreCase = true) }

                    if (!exactMatch && name.isNotBlank()) {
                        showMasterDialog = true
                    } else {
                        viewModel.addEntry(listId, name, qty, notes, type)
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
                    masterViewModel.addMasterItem(name, type == "container")
                    viewModel.addEntry(listId, name, quantity.toIntOrNull() ?: 1, notes, type)
                    showMasterDialog = false
                    onDone()
                }) { Text("Add & Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.addEntry(listId, name, quantity.toIntOrNull() ?: 1, notes, type)
                    showMasterDialog = false
                    onDone()
                }) { Text("Save Only") }
            }
        )
    }
}
