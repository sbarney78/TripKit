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
import au.barney.tripkit.data.model.MasterSubItem
import au.barney.tripkit.ui.viewmodel.MasterItemViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterSubItemsScreen(
    masterItemId: Int,
    viewModel: MasterItemViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.masterSubItems.collectAsState()
    val masterItems by viewModel.masterItems.collectAsState()
    
    // Find the name of the container we are currently in
    val containerName = remember(masterItemId, masterItems) {
        masterItems.find { it.id == masterItemId }?.name ?: "Container Contents"
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("1") }

    var showEditDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<MasterSubItem?>(null) }

    LaunchedEffect(masterItemId) {
        viewModel.loadMasterSubItems(masterItemId)
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Item to $containerName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = itemQty, onValueChange = { itemQty = it }, label = { Text("Default Quantity") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (itemName.isNotBlank()) {
                        viewModel.addMasterSubItem(masterItemId, itemName, itemQty.toIntOrNull() ?: 1)
                        itemName = ""; itemQty = "1"
                        showAddDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditDialog && editItem != null) {
        var editName by remember { mutableStateOf(editItem!!.name) }
        var editQty by remember { mutableStateOf(editItem!!.default_quantity.toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editQty, onValueChange = { editQty = it }, label = { Text("Default Quantity") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editName.isNotBlank()) {
                        viewModel.updateMasterSubItem(editItem!!.copy(name = editName, default_quantity = editQty.toIntOrNull() ?: 1))
                        showEditDialog = false
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
                title = { Text(containerName, fontWeight = FontWeight.Bold) },
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
                MasterSubItemRow(
                    item = item,
                    onEdit = {
                        editItem = item
                        showEditDialog = true
                    },
                    onDelete = { viewModel.deleteMasterSubItem(item.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MasterSubItemRow(
    item: MasterSubItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
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
                    Text("Qty: ${item.default_quantity}", style = MaterialTheme.typography.bodySmall)
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