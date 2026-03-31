package au.barney.tripkit.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.MasterItem
import au.barney.tripkit.ui.viewmodel.MasterItemViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterInventoryScreen(
    viewModel: MasterItemViewModel,
    onBack: () -> Unit,
    onOpenContainer: (Int) -> Unit
) {
    val items by viewModel.masterItems.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var isContainer by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<MasterItem?>(null) }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Master Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isContainer, onCheckedChange = { isContainer = it })
                        Text("Is this a container?")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newItemName.isNotBlank()) {
                        viewModel.addMasterItem(newItemName, isContainer)
                        newItemName = ""
                        isContainer = false
                        showAddDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditDialog && itemToEdit != null) {
        var editName by remember { mutableStateOf(itemToEdit!!.name) }
        var editIsContainer by remember { mutableStateOf(itemToEdit!!.is_container) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Master Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = editIsContainer, onCheckedChange = { editIsContainer = it })
                        Text("Is this a container?")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editName.isNotBlank()) {
                        viewModel.updateMasterItem(itemToEdit!!.copy(name = editName, is_container = editIsContainer))
                        showEditDialog = false
                        itemToEdit = null
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Master Inventory", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                MasterItemRow(
                    item = item,
                    onOpenContainer = { onOpenContainer(item.id) },
                    onEdit = {
                        itemToEdit = item
                        showEditDialog = true
                    },
                    onDelete = { viewModel.deleteMasterItem(item.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MasterItemRow(
    item: MasterItem,
    onOpenContainer: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (item.is_container) onOpenContainer() },
                onLongClick = { expanded = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    if (item.is_container) {
                        Text("Container (Tap to open)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                if (item.is_container) {
                    Text(">", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        expanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}