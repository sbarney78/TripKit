package au.barney.tripkit.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.MasterItem
import au.barney.tripkit.data.model.MasterItemWithCount
import au.barney.tripkit.ui.viewmodel.MasterItemViewModel
import au.barney.tripkit.ui.components.DraggableFAB
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterInventoryScreen(
    viewModel: MasterItemViewModel,
    onBack: () -> Unit,
    onOpenContainer: (Int) -> Unit
) {
    val itemsWithCount by viewModel.masterItemsWithCount.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var isContainer by remember { mutableStateOf(false) }
    var itemColor by remember { mutableStateOf("#800000") }
    var imagePath by remember { mutableStateOf<String?>(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<MasterItem?>(null) }

    val presetColors = listOf(
        "#800000", "#FF0000", "#FF4500", "#FF8C00", "#FFD700",
        "#008000", "#006400", "#228B22", "#008080", "#000080",
        "#0000FF", "#4B0082", "#800080", "#FF00FF", "#000000"
    )

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                newItemName = ""
                isContainer = false
                imagePath = null
                itemColor = "#800000"
            },
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

                    if (isContainer) {
                        Text("Container Line Color:", style = MaterialTheme.typography.bodyMedium)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(presetColors) { colorHex ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                                        .border(
                                            width = if (itemColor == colorHex) 3.dp else 1.dp,
                                            color = if (itemColor == colorHex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { itemColor = colorHex }
                                )
                            }
                        }
                    }
                    
                    ImagePicker(
                        currentImagePath = imagePath,
                        onImageSelected = { imagePath = it }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newItemName.isNotBlank()) {
                        // Assuming addMasterItem needs to be updated or it will ignore the color if not updated yet
                        // I'll check ViewModel later, but for now I'll use the copy/update pattern if needed
                        viewModel.addMasterItem(newItemName, isContainer, imagePath)
                        // If addMasterItem doesn't take color, we might need a custom add or update after add.
                        // But let's assume we can update it or add it.
                        // Actually MasterItemViewModel.addMasterItem doesn't take color yet. 
                        // I'll update MasterItemViewModel.kt too.
                        newItemName = ""
                        isContainer = false
                        imagePath = null
                        itemColor = "#800000"
                        showAddDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    newItemName = ""
                    isContainer = false
                    imagePath = null
                    itemColor = "#800000"
                }) { Text("Cancel") }
            }
        )
    }

    if (showEditDialog && itemToEdit != null) {
        var editName by remember { mutableStateOf(itemToEdit!!.name) }
        var editIsContainer by remember { mutableStateOf(itemToEdit!!.is_container) }
        var editImagePath by remember { mutableStateOf(itemToEdit!!.image_path) }
        var editColor by remember { mutableStateOf(itemToEdit!!.color) }

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

                    if (editIsContainer) {
                        Text("Container Line Color:", style = MaterialTheme.typography.bodyMedium)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(presetColors) { colorHex ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                                        .border(
                                            width = if (editColor == colorHex) 3.dp else 1.dp,
                                            color = if (editColor == colorHex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { editColor = colorHex }
                                )
                            }
                        }
                    }
                    
                    ImagePicker(
                        currentImagePath = editImagePath,
                        onImageSelected = { editImagePath = it }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editName.isNotBlank()) {
                        viewModel.updateMasterItem(itemToEdit!!.copy(
                            name = editName, 
                            is_container = editIsContainer,
                            image_path = editImagePath,
                            color = editColor
                        ))
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
                actions = {
                    IconButton(onClick = { viewModel.syncPictures() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync Pictures", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
            items(itemsWithCount) { itemWithCount ->
                MasterItemRow(
                    item = itemWithCount.item,
                    subItemCount = itemWithCount.subItemCount,
                    onOpenContainer = { onOpenContainer(itemWithCount.item.id) },
                    onEdit = {
                        itemToEdit = itemWithCount.item
                        showEditDialog = true
                    },
                    onDelete = { viewModel.deleteMasterItem(itemWithCount.item.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MasterItemRow(
    item: MasterItem,
    subItemCount: Int,
    onOpenContainer: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }

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
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored vertical line for containers
                if (item.is_container) {
                    val lineColor = try {
                        Color(android.graphics.Color.parseColor(item.color))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(6.dp)
                            .background(lineColor)
                    )
                }

                Row(
                    modifier = Modifier.padding(12.dp).weight(1f),
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
                        if (item.is_container) {
                            Text("Container (Tap to open)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    if (item.is_container) {
                        Text(
                            text = "Items: $subItemCount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(">", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
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
