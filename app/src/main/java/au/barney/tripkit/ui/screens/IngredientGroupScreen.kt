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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.IngredientGroup
import au.barney.tripkit.ui.viewmodel.IngredientGroupViewModel
import au.barney.tripkit.ui.viewmodel.IngredientViewModel
import au.barney.tripkit.util.PdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientGroupScreen(
    listId: Int,
    viewModel: IngredientGroupViewModel,
    ingredientViewModel: IngredientViewModel,
    onOpenGroup: (Int) -> Unit,
    onBack: () -> Unit
) {
    val groups by viewModel.groups.collectAsState()
    val allIngredients by ingredientViewModel.allIngredients.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentList by viewModel.currentList.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    var showEditDialog by remember { mutableStateOf(false) }
    var editGroupId by remember { mutableStateOf(0) }
    var editGroupName by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteGroupId by remember { mutableStateOf(0) }

    LaunchedEffect(listId) {
        viewModel.loadGroups(listId)
        ingredientViewModel.loadAllIngredientsForList(listId)
    }

    val pageTitle = if (currentList != null) "Shopping List - ${currentList!!.name}" else "Shopping List"

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Group") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val trimmed = newGroupName.trim()
                    if (trimmed.isNotEmpty()) {
                        viewModel.addGroup(trimmed)
                        newGroupName = ""
                        showAddDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    newGroupName = ""
                    showAddDialog = false
                }) { Text("Cancel") }
            }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Group") },
            text = {
                OutlinedTextField(
                    value = editGroupName,
                    onValueChange = { editGroupName = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val trimmed = editGroupName.trim()
                    if (trimmed.isNotEmpty()) {
                        viewModel.updateGroup(editGroupId, trimmed)
                        showEditDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete this group?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroup(deleteGroupId)
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
                    IconButton(onClick = { 
                        PdfGenerator.generateIngredientsPdf(
                            context = context, 
                            listName = currentList?.name ?: "List_$listId", 
                            groups = groups, 
                            allIngredients = allIngredients.groupBy { it.group_id }
                        )
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                newGroupName = ""
                showAddDialog = true
            }) { Text("+", style = MaterialTheme.typography.headlineSmall) }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                error != null -> Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(groups) { group ->
                        IngredientGroupRow(
                            group = group,
                            onOpen = { onOpenGroup(group.id) },
                            onEdit = {
                                editGroupId = group.id
                                editGroupName = group.group_name
                                showEditDialog = true
                            },
                            onDelete = {
                                deleteGroupId = group.id
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IngredientGroupRow(
    group: IngredientGroup,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { expanded = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.group_name, 
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                
                Text(">", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
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
