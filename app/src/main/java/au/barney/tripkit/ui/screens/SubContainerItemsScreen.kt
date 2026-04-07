package au.barney.tripkit.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.SubItem
import au.barney.tripkit.ui.viewmodel.ItemViewModel
import au.barney.tripkit.ui.components.DraggableFAB
import au.barney.tripkit.util.WeightUtils
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubContainerItemsScreen(
    parentItemId: Int,
    viewModel: ItemViewModel,
    onBack: () -> Unit,
    onEditItem: (Int) -> Unit
) {
    val subItems by viewModel.getSubItems(parentItemId).collectAsState(initial = emptyList())
    val currentItem by viewModel.currentItem.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<SubItem?>(null) }
    
    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("1") }
    var itemNotes by remember { mutableStateOf("") }
    var imagePath by remember { mutableStateOf<String?>(null) }

    var showMasterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(parentItemId) {
        viewModel.loadItem(parentItemId)
    }

    // Add Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Item to ${currentItem?.item_name ?: "Sub-container"}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = itemQty, onValueChange = { itemQty = it }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = itemNotes, onValueChange = { itemNotes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                    ImagePicker(currentImagePath = imagePath, onImageSelected = { imagePath = it })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (itemName.isNotBlank()) {
                        showMasterDialog = true
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showMasterDialog) {
        AlertDialog(
            onDismissRequest = { showMasterDialog = false },
            title = { Text("Add to Master List?") },
            text = { Text("'$itemName' is being added. Would you like to add it to the Master Inventory for future use?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.addSubItem(parentItemId, itemName, itemQty.toIntOrNull() ?: 1, itemNotes, imagePath, addToMaster = true)
                    itemName = ""; itemQty = "1"; itemNotes = ""; imagePath = null
                    showMasterDialog = false
                    showAddDialog = false
                }) { Text("Add & Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.addSubItem(parentItemId, itemName, itemQty.toIntOrNull() ?: 1, itemNotes, imagePath, addToMaster = false)
                    itemName = ""; itemQty = "1"; itemNotes = ""; imagePath = null
                    showMasterDialog = false
                    showAddDialog = false
                }) { Text("Save Only") }
            }
        )
    }

    // Edit Dialog
    if (showEditDialog && editingItem != null) {
        var editName by remember { mutableStateOf(editingItem!!.name) }
        var editQty by remember { mutableStateOf(editingItem!!.quantity.toString()) }
        var editNotes by remember { mutableStateOf(editingItem!!.notes ?: "") }
        var editImagePath by remember { mutableStateOf(editingItem!!.image_path) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editQty, onValueChange = { editQty = it }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editNotes, onValueChange = { editNotes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                    ImagePicker(currentImagePath = editImagePath, onImageSelected = { editImagePath = it })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editName.isNotBlank()) {
                        viewModel.updateSubItem(editingItem!!.copy(
                            name = editName,
                            quantity = editQty.toIntOrNull() ?: 1,
                            notes = editNotes,
                            image_path = editImagePath
                        ))
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
                title = { Text(currentItem?.item_name ?: "Sub-container", fontWeight = FontWeight.Bold) },
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
            DraggableFAB(onClick = { showAddDialog = true }) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = subItems,
                key = { it.id }
            ) { item ->
                SubItemRow(
                    item = item,
                    onToggleChecked = { checked -> viewModel.toggleSubItem(item.id, checked) },
                    onDelete = { viewModel.deleteSubItem(item.id) },
                    onEdit = {
                        editingItem = item
                        showEditDialog = true
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubItemRow(
    item: SubItem,
    onToggleChecked: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }

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
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = item.is_checked == 1, onCheckedChange = onToggleChecked)
                
                if (item.image_path != null) {
                    AsyncImage(
                        model = item.image_path,
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showFullScreen = true },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(WeightUtils.formatWeight(item.weightGrams * item.quantity), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        if (item.quantity > 1) {
                            Spacer(Modifier.width(8.dp))
                            Text("Qty: ${item.quantity}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (!item.notes.isNullOrEmpty()) {
                        Text(item.notes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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

    if (showFullScreen && item.image_path != null) {
        FullScreenImageDialog(item.image_path) { showFullScreen = false }
    }
}
