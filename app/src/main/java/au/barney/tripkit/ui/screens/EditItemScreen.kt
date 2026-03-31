package au.barney.tripkit.ui.screens

import androidx.compose.foundation.layout.*
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

    // ⭐ FIX: Reload item every time itemId changes
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
                .padding(16.dp)
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
                    EditItemForm(
                        itemId = itemId,
                        nameInitial = item!!.item_name,
                        quantityInitial = item!!.quantity,
                        notesInitial = item!!.notes ?: "",
                        onSave = { name, quantity, notes ->
                            viewModel.updateItem(itemId, name, quantity, notes)
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
    onSave: (String, Int, String?) -> Unit
) {
    // ⭐ FIX: Reset state whenever a NEW item is loaded
    var name by remember(itemId) { mutableStateOf(nameInitial) }
    var quantity by remember(itemId) { mutableStateOf(quantityInitial.toString()) }
    var notes by remember(itemId) { mutableStateOf(notesInitial) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Item Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it.filter { c -> c.isDigit() } },
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val qty = quantity.toIntOrNull() ?: 1
                onSave(name, qty, notes)
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save")
        }
    }
}