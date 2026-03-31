package au.barney.tripkit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.Entry
import au.barney.tripkit.ui.viewmodel.EntryViewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryScreen(
    listId: Int,          // ⭐ added
    entryId: Int,
    viewModel: EntryViewModel,
    onBack: () -> Unit
) {
    val entry by viewModel.currentEntry.collectAsState()

    // Reload entry every time entryId changes
    LaunchedEffect(entryId) {
        viewModel.loadEntry(entryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->

        if (entry == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        EditEntryForm(
            entry = entry!!,
            onSave = { name, qty, notes, type ->
                viewModel.updateEntry(
                    entryId = entryId,
                    name = name,
                    quantity = qty,
                    notes = notes,
                    entryType = type,
                    listId = listId      // ⭐ now passed
                )
                onBack()
            },
            onBack = onBack,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun EditEntryForm(
    entry: Entry,
    onSave: (String, Int, String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Reset state whenever a NEW entry is loaded
    var name by remember(entry) { mutableStateOf(entry.entry_name) }
    var qty by remember(entry) { mutableStateOf(entry.quantity.toString()) }
    var notes by remember(entry) { mutableStateOf(entry.notes ?: "") }
    var type by remember(entry) { mutableStateOf(entry.entry_type) }

    val isContainer = entry.entry_type == "container"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Item Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = qty,
            onValueChange = { qty = it.filter { c -> c.isDigit() } },
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )

        Column {
            Text(
                "Item Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (isContainer) {
                Text(
                    "This item is a container and cannot be converted to a single item.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("Container") },
                    enabled = false
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = type == "container",
                        onCheckedChange = {
                            type = if (it) "container" else "single"
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (type == "container") "Container" else "Single Item")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val qtyInt = qty.toIntOrNull() ?: 1
                onSave(name, qtyInt, notes, type)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
