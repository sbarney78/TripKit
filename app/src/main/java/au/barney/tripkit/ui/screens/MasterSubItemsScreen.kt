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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.MasterSubItem
import au.barney.tripkit.ui.viewmodel.MasterItemViewModel
import au.barney.tripkit.ui.components.DraggableFAB
import au.barney.tripkit.ui.components.WeightInput
import au.barney.tripkit.ui.components.convertToGrams
import au.barney.tripkit.ui.components.formatWeightForInput
import au.barney.tripkit.util.WeightUtils
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterSubItemsScreen(
    masterItemId: Int,
    viewModel: MasterItemViewModel,
    onBack: () -> Unit,
    onOpenSubContainer: (Int) -> Unit = {}
) {
    val itemsWithCount by viewModel.masterSubItemsWithCount.collectAsState()
    val masterItems by viewModel.masterItems.collectAsState()
    
    // Find the name of the container we are currently in
    val containerName = remember(masterItemId, masterItems) {
        masterItems.find { it.id == masterItemId }?.name ?: "Container Contents"
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("1") }
    var isContainer by remember { mutableStateOf(false) }
    var itemColor by remember { mutableStateOf("#800000") }
    var imagePath by remember { mutableStateOf<String?>(null) }
    var weightInput by remember { mutableStateOf("") }
    var weightUnit by remember { mutableStateOf("g") }

    var showEditDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<MasterSubItem?>(null) }

    val presetColors = listOf(
        "#800000", "#FF0000", "#FF4500", "#FF8C00", "#FFD700",
        "#008000", "#006400", "#228B22", "#008080", "#000080",
        "#0000FF", "#4B0082", "#800080", "#FF00FF", "#000000"
    )

    LaunchedEffect(masterItemId) {
        viewModel.loadMasterSubItems(masterItemId)
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                itemName = ""; itemQty = "1"; isContainer = false; imagePath = null; itemColor = "#800000"; weightInput = ""; weightUnit = "g"
            },
            title = { Text("Add Item to $containerName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
                    
                    WeightInput(
                        weightInput = weightInput,
                        onWeightInputChange = { weightInput = it },
                        weightUnit = weightUnit,
                        onWeightUnitChange = { weightUnit = it },
                        label = "Weight",
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!isContainer) {
                        OutlinedTextField(value = itemQty, onValueChange = { itemQty = it }, label = { Text("Default Quantity") }, modifier = Modifier.fillMaxWidth())
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isContainer, onCheckedChange = { isContainer = it })
                        Text("Is this a sub-category?")
                    }

                    if (isContainer) {
                        Text("Sub-Category Line Color:", style = MaterialTheme.typography.bodyMedium)
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

                    ImagePicker(currentImagePath = imagePath, onImageSelected = { imagePath = it })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (itemName.isNotBlank()) {
                        val weightGrams = convertToGrams(weightInput, weightUnit)

                        viewModel.addMasterSubItem(masterItemId, itemName, if (isContainer) 0 else (itemQty.toIntOrNull() ?: 1), isContainer, imagePath, itemColor, weightGrams)
                        itemName = ""; itemQty = "1"; isContainer = false; imagePath = null; itemColor = "#800000"; weightInput = ""; weightUnit = "g"
                        showAddDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    itemName = ""; itemQty = "1"; isContainer = false; imagePath = null; itemColor = "#800000"; weightInput = ""
                }) { Text("Cancel") }
            }
        )
    }

    if (showEditDialog && editItem != null) {
        var editName by remember { mutableStateOf(editItem!!.name) }
        var editQty by remember { mutableStateOf(editItem!!.default_quantity.toString()) }
        var editIsContainer by remember { mutableStateOf(editItem!!.is_container) }
        var editImagePath by remember { mutableStateOf(editItem!!.image_path) }
        var editColor by remember { mutableStateOf(editItem!!.color) }
        var editWeightInput by remember { 
            val (input, _) = formatWeightForInput(editItem!!.weightGrams)
            mutableStateOf(input)
        }
        var editWeightUnit by remember {
            val (_, unit) = formatWeightForInput(editItem!!.weightGrams)
            mutableStateOf(unit)
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
                    
                    WeightInput(
                        weightInput = editWeightInput,
                        onWeightInputChange = { editWeightInput = it },
                        weightUnit = editWeightUnit,
                        onWeightUnitChange = { editWeightUnit = it },
                        label = "Weight",
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!editIsContainer) {
                        OutlinedTextField(value = editQty, onValueChange = { editQty = it }, label = { Text("Default Quantity") }, modifier = Modifier.fillMaxWidth())
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = editIsContainer,
                            onCheckedChange = { editIsContainer = it },
                            enabled = !editItem!!.is_container // Cannot change back if already a container
                        )
                        Text("Is this a sub-category?")
                    }

                    if (editIsContainer) {
                        Text("Sub-Category Line Color:", style = MaterialTheme.typography.bodyMedium)
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

                    ImagePicker(currentImagePath = editImagePath, onImageSelected = { editImagePath = it })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editName.isNotBlank()) {
                        val weightGrams = convertToGrams(editWeightInput, editWeightUnit)

                        viewModel.updateMasterSubItem(editItem!!.copy(
                            name = editName, 
                            default_quantity = if (editIsContainer) 0 else (editQty.toIntOrNull() ?: 1),
                            is_container = editIsContainer,
                            image_path = editImagePath,
                            color = editColor,
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
            items(itemsWithCount) { itemWithCount ->
                MasterSubItemRow(
                    item = itemWithCount.subItem,
                    subSubItemCount = itemWithCount.subSubItemCount,
                    onOpenSubContainer = { onOpenSubContainer(itemWithCount.subItem.id) },
                    onEdit = {
                        editItem = itemWithCount.subItem
                        showEditDialog = true
                    },
                    onDelete = { viewModel.deleteMasterSubItem(itemWithCount.subItem.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MasterSubItemRow(
    item: MasterSubItem,
    subSubItemCount: Int,
    onOpenSubContainer: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (item.is_container) onOpenSubContainer() },
                onLongClick = { expanded = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored vertical line for sub-containers
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(WeightUtils.formatWeight(item.weightGrams), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            if (item.is_container) {
                                Spacer(Modifier.width(8.dp))
                                Text("Sub-Category (Tap to open)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Spacer(Modifier.width(8.dp))
                                Text("Qty: ${item.default_quantity}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (item.is_container) {
                        Text(
                            text = "Items: $subSubItemCount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = ">",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
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
