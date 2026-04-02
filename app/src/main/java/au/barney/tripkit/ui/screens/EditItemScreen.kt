package au.barney.tripkit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.barney.tripkit.ui.viewmodel.ItemViewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    itemId: Int,
    viewModel: ItemViewModel,
    onBack: () -> Unit
) {
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val item by viewModel.currentItem.collectAsState()

    // Reload item every time itemId changes
    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
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
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                item != null -> {
                    val currentItem = item!!
                    EditItemForm(
                        itemId = itemId,
                        nameInitial = currentItem.item_name,
                        quantityInitial = currentItem.quantity,
                        notesInitial = currentItem.notes ?: "",
                        imagePathInitial = currentItem.image_path,
                        onSave = { name, quantity, notes, imagePath ->
                            viewModel.updateItem(
                                itemId = itemId,
                                name = name,
                                quantity = quantity,
                                notes = notes,
                                isContainer = currentItem.is_container,
                                imagePath = imagePath
                            )
                            onBack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditItemForm(
    itemId: Int,
    nameInitial: String,
    quantityInitial: Int,
    notesInitial: String,
    imagePathInitial: String?,
    onSave: (String, Int, String?, String?) -> Unit
) {
    var name by remember(itemId) { mutableStateOf(nameInitial) }
    var quantity by remember(itemId) { mutableStateOf(quantityInitial.toString()) }
    var notes by remember(itemId) { mutableStateOf(notesInitial) }
    var imagePath by remember(itemId) { mutableStateOf(imagePathInitial) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                label = { Text("Quantity") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            ImagePicker(
                currentImagePath = imagePath,
                onImageSelected = { imagePath = it }
            )
        }

        item {
            Button(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 1
                    onSave(name, qty, notes, imagePath)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
