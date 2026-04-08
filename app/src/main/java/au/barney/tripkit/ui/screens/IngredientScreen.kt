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
import au.barney.tripkit.data.model.Ingredient
import au.barney.tripkit.ui.viewmodel.IngredientViewModel
import au.barney.tripkit.ui.components.DraggableFAB

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientScreen(
    groupId: Int,
    viewModel: IngredientViewModel,
    onBack: () -> Unit
) {
    val ingredients by viewModel.ingredients.collectAsState()
    val currentGroup by viewModel.currentGroup.collectAsState()
    val currentList by viewModel.currentList.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Add dialog
    var showAddDialog by remember { mutableStateOf(false) }
    var newIngredientName by remember { mutableStateOf("") }

    // Edit dialog
    var showEditDialog by remember { mutableStateOf(false) }
    var editIngredient: Ingredient? by remember { mutableStateOf(null) }
    var editIngredientName by remember { mutableStateOf("") }

    // Delete dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteIngredientId by remember { mutableStateOf(0) }

    LaunchedEffect(groupId) {
        viewModel.loadIngredients(groupId)
    }

    val pageTitle = remember(currentGroup, currentList) {
        val groupName = currentGroup?.group_name ?: "Ingredients"
        val listName = currentList?.name ?: ""
        if (listName.isNotEmpty()) "Ingredients - $groupName - $listName" else "Ingredients - $groupName"
    }

    // ---------------- ADD INGREDIENT DIALOG ----------------
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Ingredient") },
            text = {
                OutlinedTextField(
                    value = newIngredientName,
                    onValueChange = { newIngredientName = it },
                    label = { Text("Ingredient name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val trimmed = newIngredientName.trim()
                    if (trimmed.isNotEmpty()) {
                        viewModel.addIngredient(trimmed)
                        newIngredientName = ""
                        showAddDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    newIngredientName = ""
                    showAddDialog = false
                }) { Text("Cancel") }
            }
        )
    }

    // ---------------- EDIT INGREDIENT DIALOG ----------------
    if (showEditDialog && editIngredient != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Ingredient") },
            text = {
                OutlinedTextField(
                    value = editIngredientName,
                    onValueChange = { editIngredientName = it },
                    label = { Text("Ingredient name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val trimmed = editIngredientName.trim()
                    if (trimmed.isNotEmpty()) {
                        viewModel.updateIngredient(editIngredient!!.copy(ingredient_name = trimmed))
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

    // ---------------- DELETE INGREDIENT DIALOG ----------------
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Ingredient?") },
            text = { Text("Are you sure you want to delete this ingredient?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteIngredient(deleteIngredientId)
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            DraggableFAB(onClick = {
                newIngredientName = ""
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
                    items(
                        items = ingredients,
                        key = { it.id }
                    ) { ingredient ->
                        IngredientItemRow(
                            ingredient = ingredient,
                            onToggle = { checked ->
                                viewModel.toggleIngredient(ingredient.id, checked)
                            },
                            onEdit = {
                                editIngredient = ingredient
                                editIngredientName = ingredient.ingredient_name
                                showEditDialog = true
                            },
                            onDelete = {
                                deleteIngredientId = ingredient.id
                                showAddDialog = false
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
fun IngredientItemRow(
    ingredient: Ingredient,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onToggle(ingredient.is_checked != 1) },
                onLongClick = { expanded = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = ingredient.is_checked == 1,
                    onCheckedChange = { onToggle(it) }
                )
                Text(
                    text = ingredient.ingredient_name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    color = if (ingredient.is_checked == 1) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
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
