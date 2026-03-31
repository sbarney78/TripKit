package au.barney.tripkit.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.Item
import au.barney.tripkit.ui.viewmodel.ItemViewModel
import au.barney.tripkit.ui.viewmodel.MasterItemViewModel
import au.barney.tripkit.ui.viewmodel.EntryViewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerItemsScreen(
    entryId: Int,
    viewModel: ItemViewModel,
    entryViewModel: EntryViewModel, // ADDED
    masterItemViewModel: MasterItemViewModel,
    onAddItem: () -> Unit,
    onBack: () -> Unit,
    onEditItem: (Int) -> Unit,
) {
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val entries by entryViewModel.entries.collectAsState()

    // Find the name of the container
    val containerName = remember(entryId, entries) {
        entries.find { it.entry_id == entryId }?.entry_name ?: "Items"
    }

    LaunchedEffect(entryId) {
        viewModel.loadItems(entryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(containerName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
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
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items) { item ->
                            ItemRow(
                                item = item,
                                onToggleChecked = { checked ->
                                    viewModel.toggleItem(item.item_id, checked)
                                },
                                onEdit = {
                                    onEditItem(item.item_id)
                                },
                                onDelete = {
                                    viewModel.deleteItem(item.item_id)
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
fun ItemRow(
    item: Item,
    onToggleChecked: (Boolean) -> Unit,
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
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    checked = (item.is_checked == 1),
                    onCheckedChange = { checked ->
                        onToggleChecked(checked)
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    Text(
                        text = item.item_name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.is_checked == 1) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Qty: ${item.quantity}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (!item.notes.isNullOrEmpty()) {
                        Text(
                            text = item.notes ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
}
