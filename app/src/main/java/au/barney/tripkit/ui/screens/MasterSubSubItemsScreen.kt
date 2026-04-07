package au.barney.tripkit.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.MasterSubSubItem
import au.barney.tripkit.ui.viewmodel.MasterItemViewModel
import au.barney.tripkit.ui.components.DraggableFAB
import au.barney.tripkit.util.WeightUtils
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterSubSubItemsScreen(
    masterSubItemId: Int,
    viewModel: MasterItemViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.masterSubSubItems.collectAsState()
    val subItems by viewModel.masterSubItems.collectAsState()
    
    // Find the name of the container we are currently in
    val containerName = remember(masterSubItemId, subItems) {
        subItems.find { it.id == masterSubItemId }?.name ?: "Sub-category Contents"
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("1") }
    val isContainer = false // Nested containers not allowed in Master Sub-Sub Items
    var imagePath by remember { mutableStateOf<String?>(null) }
    var weightInput by remember { mutableStateOf("") }
    var weightUnit by remember { mutableStateOf("g") }

    var showEditDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<MasterSubSubItem?>(null) }

    LaunchedEffect(masterSubItemId) {
        viewModel.loadMasterSubSubItems(masterSubItemId)
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                itemName = ""; itemQty = "1"; imagePath = null; weightInput = ""; weightUnit = "g"
            },
            title = { Text("Add Item to $containerName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Weight") },
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

                    OutlinedTextField(value = itemQty, onValueChange = { itemQty = it }, label = { Text("Default Quantity") }, modifier = Modifier.fillMaxWidth())
                    ImagePicker(currentImagePath = imagePath, onImageSelected = { imagePath = it })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (itemName.isNotBlank()) {
                        val weightGrams = try {
                            val value = weightInput.toDouble()
                            if (weightUnit == "kg") (value * 1000).toInt() else value.toInt()
                        } catch (e: Exception) { 0 }

                        viewModel.addMasterSubSubItem(masterSubItemId, itemName, itemQty.toIntOrNull() ?: 1, isContainer, imagePath, weightGrams)
                        itemName = ""; itemQty = "1"; imagePath = null; weightInput = ""; weightUnit = "g"
                        showAddDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    itemName = ""; itemQty = "1"; imagePath = null; weightInput = ""
                }) { Text("Cancel") }
            }
        )
    }

    if (showEditDialog && editItem != null) {
        var editName by remember { mutableStateOf(editItem!!.name) }
        var editQty by remember { mutableStateOf(editItem!!.default_quantity.toString()) }
        val editIsContainer = editItem!!.is_container
        var editImagePath by remember { mutableStateOf(editItem!!.image_path) }
        var editWeightInput by remember { 
            val grams = editItem!!.weightGrams
            mutableStateOf(if (grams >= 1000) (grams / 1000.0).toString() else grams.toString())
        }
        var editWeightUnit by remember { mutableStateOf(if (editItem!!.weightGrams >= 1000) "kg" else "g") }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editWeightInput,
                            onValueChange = { editWeightInput = it },
                            label = { Text("Weight") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        var unitExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { unitExpanded = true }) {
                                Text(editWeightUnit)
                            }
                            DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                DropdownMenuItem(text = { Text("g") }, onClick = { editWeightUnit = "g"; unitExpanded = false })
                                DropdownMenuItem(text = { Text("kg") }, onClick = { editWeightUnit = "kg"; unitExpanded = false })
                            }
                        }
                    }

                    if (!editIsContainer) {
                        OutlinedTextField(value = editQty, onValueChange = { editQty = it }, label = { Text("Default Quantity") }, modifier = Modifier.fillMaxWidth())
                    }
                    ImagePicker(currentImagePath = editImagePath, onImageSelected = { editImagePath = it })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editName.isNotBlank()) {
                        val weightGrams = try {
                            val value = editWeightInput.toDouble()
                            if (editWeightUnit == "kg") (value * 1000).toInt() else value.toInt()
                        } catch (e: Exception) { editItem!!.weightGrams }

                        viewModel.updateMasterSubSubItem(editItem!!.copy(
                            name = editName, 
                            default_quantity = if (editIsContainer) 0 else (editQty.toIntOrNull() ?: 1),
                            is_container = editIsContainer,
                            image_path = editImagePath,
                            weightGrams = weightGrams
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
                items = items,
                key = { it.id }
            ) { item ->
                MasterSubSubItemRow(
                    item = item,
                    onEdit = {
                        editItem = item
                        showEditDialog = true
                    },
                    onDelete = { viewModel.deleteMasterSubSubItem(item.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MasterSubSubItemRow(
    item: MasterSubSubItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(WeightUtils.formatWeight(item.weightGrams), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        if (!item.is_container) {
                            Spacer(Modifier.width(8.dp))
                            Text("Qty: ${item.default_quantity}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
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
    
    if (showFullScreen && item.image_path != null) {
        FullScreenImageDialog(item.image_path) { showFullScreen = false }
    }
}
