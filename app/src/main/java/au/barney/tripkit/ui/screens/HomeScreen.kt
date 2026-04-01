package au.barney.tripkit.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.FullTripData
import au.barney.tripkit.data.model.ListItem
import au.barney.tripkit.ui.viewmodel.*
import au.barney.tripkit.util.BackupManager
import au.barney.tripkit.util.DataSharingManager
import au.barney.tripkit.util.PdfGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: ListViewModel,
    entryViewModel: EntryViewModel,
    itemViewModel: ItemViewModel,
    menuViewModel: MenuViewModel,
    ingredientGroupViewModel: IngredientGroupViewModel,
    ingredientViewModel: IngredientViewModel,
    itineraryViewModel: ItineraryViewModel,
    onOpenInventory: (Int) -> Unit,
    onOpenMenu: (Int) -> Unit,
    onOpenIngredients: (Int) -> Unit,
    onOpenItinerary: (Int) -> Unit,
    onOpenMasterInventory: () -> Unit
) {
    val lists by viewModel.lists.collectAsState()
    val progress by viewModel.packingProgress.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var addInventory by remember { mutableStateOf(true) }
    var addMenu by remember { mutableStateOf(true) }
    var addIngredients by remember { mutableStateOf(true) }
    var addItinerary by remember { mutableStateOf(true) }

    var showEditDialog by remember { mutableStateOf(false) }
    var editList by remember { mutableStateOf<ListItem?>(null) }
    var editListName by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<ListItem?>(null) }

    var showDuplicateDialog by remember { mutableStateOf(false) }
    var listToDuplicate by remember { mutableStateOf<ListItem?>(null) }
    var duplicateListName by remember { mutableStateOf("") }

    var contextMenuVisible by remember { mutableStateOf<Int?>(null) }

    // Sync / Import State
    var dataToImport by remember { mutableStateOf<FullTripData?>(null) }
    var existingSyncList by remember { mutableStateOf<ListItem?>(null) }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { BackupManager.restoreDatabase(context, it) }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val data = DataSharingManager.readTripFile(context, it)
                    if (data != null) {
                        dataToImport = data
                        existingSyncList = entryViewModel.repository.getListBySyncId(data.list.sync_id)
                    }
                }
            }
        }
    )

    // Handle Import/Sync Dialog
    if (dataToImport != null) {
        if (existingSyncList != null) {
            AlertDialog(
                onDismissRequest = { dataToImport = null },
                title = { Text("Trip Exists") },
                text = { Text("A list named '${existingSyncList!!.name}' already exists. Would you like to sync updates to it or create a brand new copy?") },
                confirmButton = {
                    Button(onClick = {
                        scope.launch { 
                            DataSharingManager.mergeIntoExisting(context, entryViewModel.repository, dataToImport!!)
                            viewModel.loadLists() // Refresh
                        }
                        dataToImport = null
                    }) { Text("Sync Updates") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        scope.launch { 
                            DataSharingManager.importAsNew(context, entryViewModel.repository, dataToImport!!)
                            viewModel.loadLists() // Refresh
                        }
                        dataToImport = null
                    }) { Text("Create New Copy") }
                }
            )
        } else {
            // No conflict, just import immediately
            LaunchedEffect(dataToImport) {
                DataSharingManager.importAsNew(context, entryViewModel.repository, dataToImport!!)
                viewModel.loadLists() // Refresh
                dataToImport = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TripKit Lists", fontWeight = FontWeight.Bold) },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Master Inventory") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = onOpenMasterInventory) {
                            Icon(Icons.Default.Inventory, contentDescription = "Master Inventory", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Import Trip") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                            Icon(Icons.Default.Input, contentDescription = "Import Trip", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Full Backup") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { BackupManager.backupDatabase(context) }) {
                            Icon(Icons.Default.Backup, contentDescription = "Full Backup", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Full Restore") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { restoreLauncher.launch(arrayOf("application/octet-stream", "application/x-sqlite3")) }) {
                            Icon(Icons.Default.SettingsBackupRestore, contentDescription = "Full Restore", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            items(lists) { list ->
                val listProgress = progress[list.id] ?: Pair(0, 0)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { },
                            onLongClick = { contextMenuVisible = list.id }
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // TOP ROW: TITLE AND ACTIONS
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = list.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Row {
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                        tooltip = { PlainTooltip { Text("Export/Share") } },
                                        state = rememberTooltipState()
                                    ) {
                                        IconButton(onClick = {
                                            scope.launch {
                                                DataSharingManager.exportTripList(context, entryViewModel.repository, list.id)
                                            }
                                        }) {
                                            Icon(Icons.Default.Share, contentDescription = "Export/Share", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                        tooltip = { PlainTooltip { Text("Full PDF") } },
                                        state = rememberTooltipState()
                                    ) {
                                        IconButton(onClick = {
                                            scope.launch {
                                                val repository = entryViewModel.repository
                                                val data = FullTripData(
                                                    list = list,
                                                    itinerary = repository.getItinerarySync(list.id),
                                                    entries = repository.getEntriesSync(list.id),
                                                    allItems = repository.getAllItemsForListSync(list.id),
                                                    menu = repository.getMenuSync(list.id),
                                                    ingredientGroups = repository.getIngredientGroupsSync(list.id),
                                                    allIngredients = repository.getAllIngredientsForListSync(list.id)
                                                )
                                                PdfGenerator.generateFullTripPdf(context, data)
                                            }
                                        }) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Full PDF", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                        tooltip = { PlainTooltip { Text("Duplicate") } },
                                        state = rememberTooltipState()
                                    ) {
                                        IconButton(onClick = {
                                            listToDuplicate = list
                                            duplicateListName = "${list.name} (Copy)"
                                            showDuplicateDialog = true
                                        }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }

                            // SECOND ROW: PROGRESS BAR
                            if (list.show_inventory) {
                                if (listProgress.second > 0) {
                                    val percent = listProgress.first.toFloat() / listProgress.second
                                    LinearProgressIndicator(
                                        progress = { percent },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    Text(
                                        text = "Packed: ${listProgress.first}/${listProgress.second}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                } else {
                                    Text("Inventory Empty", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                            if (list.show_inventory || list.show_menu) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (list.show_inventory) {
                                        Button(
                                            onClick = { onOpenInventory(list.id) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 12.dp)
                                        ) {
                                            Text("Inventory")
                                        }
                                    }

                                    if (list.show_menu) {
                                        Button(
                                            onClick = { onOpenMenu(list.id) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 12.dp)
                                        ) {
                                            Text("Menu")
                                        }
                                    }
                                }
                            }

                            if (list.show_ingredients || list.show_itinerary) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (list.show_ingredients) {
                                        Button(
                                            onClick = { onOpenIngredients(list.id) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 12.dp)
                                        ) {
                                            Text("Ingredients")
                                        }
                                    }
                                    
                                    if (list.show_itinerary) {
                                        Button(
                                            onClick = { onOpenItinerary(list.id) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Itinerary")
                                        }
                                    }
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = contextMenuVisible == list.id,
                            onDismissRequest = { contextMenuVisible = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    contextMenuVisible = null
                                    editList = list
                                    editListName = list.name
                                    showEditDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    contextMenuVisible = null
                                    listToDelete = list
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create New List") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text("Trip Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Select lists to include:", fontWeight = FontWeight.SemiBold)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = addInventory, onCheckedChange = { addInventory = it })
                            Text("Inventory")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = addMenu, onCheckedChange = { addMenu = it })
                            Text("Menu")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = addIngredients, onCheckedChange = { addIngredients = it })
                            Text("Ingredients")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = addItinerary, onCheckedChange = { addItinerary = it })
                            Text("Itinerary")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newListName.isNotBlank()) {
                        viewModel.addList(newListName, addInventory, addMenu, addIngredients, addItinerary)
                    }
                    newListName = ""
                    addInventory = true; addMenu = true; addIngredients = true; addItinerary = true
                    showAddDialog = false
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newListName = ""
                    addInventory = true; addMenu = true; addIngredients = true; addItinerary = true
                    showAddDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit List") },
            text = {
                val currentEditList = editList
                if (currentEditList != null) {
                    var editedInventory by remember { mutableStateOf(currentEditList.show_inventory) }
                    var editedMenu by remember { mutableStateOf(currentEditList.show_menu) }
                    var editedIngredients by remember { mutableStateOf(currentEditList.show_ingredients) }
                    var editedItinerary by remember { mutableStateOf(currentEditList.show_itinerary) }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = editListName,
                            onValueChange = { editListName = it },
                            label = { Text("Trip Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Lists to show:", fontWeight = FontWeight.SemiBold)
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = editedInventory, onCheckedChange = { editedInventory = it })
                                Text("Inventory")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = editedMenu, onCheckedChange = { editedMenu = it })
                                Text("Menu")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = editedIngredients, onCheckedChange = { editedIngredients = it })
                                Text("Ingredients")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = editedItinerary, onCheckedChange = { editedItinerary = it })
                                Text("Itinerary")
                            }
                        }
                        
                        // Action buttons for saving the edits
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showEditDialog = false }) {
                                Text("Cancel")
                            }
                            Button(onClick = {
                                if (editListName.isNotBlank()) {
                                    viewModel.updateList(currentEditList.copy(
                                        name = editListName,
                                        show_inventory = editedInventory,
                                        show_menu = editedMenu,
                                        show_ingredients = editedIngredients,
                                        show_itinerary = editedItinerary
                                    ))
                                }
                                showEditDialog = false
                            }) {
                                Text("Save")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = { Text("Duplicate List") },
            text = {
                Column {
                    Text("Create a copy of '${listToDuplicate?.name}'?")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = duplicateListName,
                        onValueChange = { duplicateListName = it },
                        label = { Text("New Trip Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val original = listToDuplicate
                    if (original != null && duplicateListName.isNotBlank()) {
                        viewModel.duplicateList(original.id, duplicateListName)
                    }
                    showDuplicateDialog = false
                }) {
                    Text("Duplicate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete List?") },
            text = { Text("This will permanently remove '${listToDelete?.name}' and all its items. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        listToDelete?.let { viewModel.deleteList(it.id) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
