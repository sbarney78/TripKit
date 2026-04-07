package au.barney.tripkit.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.TemplateEntry
import au.barney.tripkit.data.model.TemplateItem
import au.barney.tripkit.data.model.TemplateSubItem
import au.barney.tripkit.ui.viewmodel.MasterItemViewModel
import au.barney.tripkit.ui.viewmodel.TemplateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTemplateScreen(
    templateId: Int,
    masterViewModel: MasterItemViewModel,
    templateViewModel: TemplateViewModel,
    onBack: () -> Unit
) {
    val templates by templateViewModel.templates.collectAsState()
    val template = templates.find { it.id == templateId }
    val templateEntries by templateViewModel.templateEntries.collectAsState()
    val masterItems by masterViewModel.masterItems.collectAsState()

    var showAddFromMasterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(templateId) {
        templateViewModel.loadTemplateEntries(templateId)
    }

    if (template == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Template: ${template.name}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddFromMasterDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add from Master")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Current Items in Template:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = templateEntries,
                    key = { it.id }
                ) { entry ->
                    TemplateEntryRow(
                        entry = entry,
                        viewModel = templateViewModel
                    )
                }
            }
        }
    }

    if (showAddFromMasterDialog) {
        val existingNames = templateEntries.map { it.name }.toSet()
        val availableItems = masterItems.filter { it.name !in existingNames }

        AlertDialog(
            onDismissRequest = { showAddFromMasterDialog = false },
            title = { Text("Add from Master Inventory") },
            text = {
                if (availableItems.isEmpty()) {
                    Text("No new items available in Master Inventory.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(availableItems) { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = {
                                    templateViewModel.addFromMaster(templateId, item.id)
                                    showAddFromMasterDialog = false
                                },
                                leadingIcon = {
                                    if (item.is_container) {
                                        Box(modifier = Modifier.size(12.dp).background(Color(android.graphics.Color.parseColor(item.color))))
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddFromMasterDialog = false }) { Text("Close") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplateEntryRow(
    entry: TemplateEntry,
    viewModel: TemplateViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var contextMenuVisible by remember { mutableStateOf(false) }
    val items by viewModel.getTemplateItems(entry.id).collectAsState(initial = emptyList())

    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (entry.is_container) expanded = !expanded },
                    onLongClick = { contextMenuVisible = true }
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = entry.is_checked == 1,
                        onCheckedChange = { viewModel.toggleTemplateEntry(entry.id, it) }
                    )
                    
                    if (entry.is_container) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(24.dp)
                                .background(Color(android.graphics.Color.parseColor(entry.color)))
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (entry.is_container) {
                            Text(
                                text = if (expanded) "Hide items" else "Show items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (entry.is_container) {
                        Text(if (expanded) "▼" else ">", style = MaterialTheme.typography.titleMedium)
                    }
                }

                DropdownMenu(
                    expanded = contextMenuVisible,
                    onDismissRequest = { contextMenuVisible = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            viewModel.deleteTemplateEntry(entry.id)
                            contextMenuVisible = false
                        }
                    )
                }
            }
        }

        if (expanded && entry.is_container) {
            Column(modifier = Modifier.padding(start = 32.dp, top = 8.dp)) {
                items.forEach { item ->
                    TemplateItemRow(item = item, viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplateItemRow(
    item: TemplateItem,
    viewModel: TemplateViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var contextMenuVisible by remember { mutableStateOf(false) }
    val subItems by viewModel.getTemplateSubItems(item.id).collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (item.is_container) expanded = !expanded },
                    onLongClick = { contextMenuVisible = true }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.is_checked == 1,
                onCheckedChange = { viewModel.toggleTemplateItem(item.id, it) }
            )
            
            if (item.is_container) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .background(Color(android.graphics.Color.parseColor(item.color)))
                )
                Spacer(Modifier.width(8.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.bodyMedium)
                if (item.is_container) {
                    Text(
                        text = if (expanded) "Hide items" else "Show items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (item.is_container) {
                Text(if (expanded) "▼" else ">", style = MaterialTheme.typography.bodySmall)
            }

            Box {
                DropdownMenu(
                    expanded = contextMenuVisible,
                    onDismissRequest = { contextMenuVisible = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            viewModel.deleteTemplateItem(item.id)
                            contextMenuVisible = false
                        }
                    )
                }
            }
        }

        if (expanded && item.is_container) {
            Column(modifier = Modifier.padding(start = 32.dp, top = 4.dp)) {
                subItems.forEach { subItem ->
                    TemplateSubItemRow(subItem = subItem, viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplateSubItemRow(
    subItem: TemplateSubItem,
    viewModel: TemplateViewModel
) {
    var contextMenuVisible by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = { contextMenuVisible = true }
            )
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = subItem.is_checked == 1,
            onCheckedChange = { viewModel.toggleTemplateSubItem(subItem.id, it) }
        )
        Text(
            text = subItem.name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )

        Box {
            DropdownMenu(
                expanded = contextMenuVisible,
                onDismissRequest = { contextMenuVisible = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        viewModel.deleteTemplateSubItem(subItem.id)
                        contextMenuVisible = false
                    }
                )
            }
        }
    }
}
