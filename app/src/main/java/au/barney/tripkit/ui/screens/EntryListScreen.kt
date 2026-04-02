package au.barney.tripkit.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.Entry
import au.barney.tripkit.ui.viewmodel.EntryViewModel
import au.barney.tripkit.ui.viewmodel.ItemViewModel
import au.barney.tripkit.ui.viewmodel.MasterItemViewModel
import au.barney.tripkit.util.PdfGenerator
import au.barney.tripkit.ui.components.DraggableFAB

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(
    listId: Int,
    viewModel: EntryViewModel,
    itemViewModel: ItemViewModel,
    masterItemViewModel: MasterItemViewModel,
    onOpenEntryItems: (Int) -> Unit,
    onAddEntry: () -> Unit,
    onEditEntry: (Int) -> Unit,
    onBack: () -> Unit,
    onViewPdf: (String) -> Unit
) {
    val entriesWithCount by viewModel.entriesWithCount.collectAsState()
    val allItems by itemViewModel.allItems.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentList by viewModel.currentList.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(listId) {
        viewModel.loadEntries(listId)
        itemViewModel.loadAllItemsForList(listId)
    }

    val pageTitle = if (currentList != null) "Inventory - ${currentList!!.name}" else "Inventory"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pageTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val file = PdfGenerator.generateInventoryPdf(
                                context = context,
                                listName = currentList?.name ?: "List_$listId",
                                entries = entriesWithCount.map { it.entry },
                                allItems = allItems.groupBy { it.entry_id }
                            )
                            file?.let { onViewPdf(it.absolutePath) }
                        }
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "PDF", tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
            DraggableFAB(onClick = onAddEntry) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
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
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = entriesWithCount,
                            key = { it.entry.entry_id }
                        ) { itemWithCount ->
                            EntryRow(
                                entry = itemWithCount.entry,
                                subItemCount = itemWithCount.subItemCount,
                                onToggle = { checked ->
                                    viewModel.toggleEntry(itemWithCount.entry.entry_id, checked, listId)
                                },
                                onClick = {
                                    if (itemWithCount.entry.entry_type == "container") {
                                        onOpenEntryItems(itemWithCount.entry.entry_id)
                                    }
                                },
                                onEdit = { onEditEntry(itemWithCount.entry.entry_id) },
                                onDelete = {
                                    viewModel.deleteEntry(itemWithCount.entry.entry_id, listId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryRow(
    entry: Entry,
    subItemCount: Int,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isContainer = entry.entry_type == "container"
    var expanded by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isContainer) onClick() },
                onLongClick = { expanded = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    checked = entry.is_checked == 1,
                    onCheckedChange = { onToggle(it) }
                )
                
                if (entry.image_path != null) {
                    AsyncImage(
                        model = entry.image_path,
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showFullScreen = true },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = entry.entry_name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.is_checked == 1) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )

                    if (!isContainer) {
                        Text(
                            text = "Qty: ${entry.quantity}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (!entry.notes.isNullOrEmpty()) {
                        Text(
                            text = entry.notes ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isContainer) {
                    Text(
                        text = "Items: $subItemCount",
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
    
    if (showFullScreen && entry.image_path != null) {
        FullScreenImageDialog(entry.image_path) { showFullScreen = false }
    }
}
