package au.barney.tripkit.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.ExtraPayloadProfile
import au.barney.tripkit.data.model.PayloadLocation
import au.barney.tripkit.ui.viewmodel.ListViewModel
import au.barney.tripkit.util.WeightUtils.formatWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePayloadsScreen(
    viewModel: ListViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.loadPayloadLocations()
        viewModel.loadExtraPayloadProfiles()
    }

    val payloadLocations by viewModel.payloadLocations.collectAsState()
    val extraPayloads by viewModel.extraPayloadProfiles.collectAsState()
    
    var showAddPayloadDialog by remember { mutableStateOf(false) }
    var showAddExtraDialog by remember { mutableStateOf(false) }
    
    var editingPayload by remember { mutableStateOf<PayloadLocation?>(null) }
    var editingExtra by remember { mutableStateOf<ExtraPayloadProfile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Payloads", fontWeight = FontWeight.Bold) },
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Packing Locations", 
                        style = MaterialTheme.typography.titleLarge, 
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = { showAddPayloadDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }

            items(payloadLocations) { location ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(location.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Category: ${location.category}", style = MaterialTheme.typography.bodySmall)
                            location.weightLimitGrams?.let {
                                Text("Limit: ${formatWeight(it)}", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        IconButton(onClick = { editingPayload = location }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.deletePayloadLocation(location.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp), 
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Extra Payloads", 
                        style = MaterialTheme.typography.titleLarge, 
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = { showAddExtraDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
            
            item {
                Text(
                    text = "People, water, fuel, etc. added to total weight.", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(extraPayloads) { extra ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(extra.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Category: ${extra.category}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = formatWeight(extra.weightGrams), 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.secondary
                        )
                        IconButton(onClick = { editingExtra = extra }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.deleteExtraPayloadProfile(extra.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddPayloadDialog || editingPayload != null) {
        PayloadDialog(
            payload = editingPayload,
            onDismiss = { 
                showAddPayloadDialog = false
                editingPayload = null
            },
            onSave = { name, limit, cat ->
                if (editingPayload != null) {
                    viewModel.updatePayloadLocation(editingPayload!!.copy(name = name, weightLimitGrams = limit, category = cat))
                } else {
                    viewModel.addPayloadLocation(name, limit, cat)
                }
                showAddPayloadDialog = false
                editingPayload = null
            }
        )
    }

    if (showAddExtraDialog || editingExtra != null) {
        ExtraPayloadDialog(
            extraProfile = editingExtra,
            onDismiss = { 
                showAddExtraDialog = false
                editingExtra = null
            },
            onSave = { name, weight, cat ->
                if (editingExtra != null) {
                    viewModel.updateExtraPayloadProfile(editingExtra!!.copy(name = name, weightGrams = weight, category = cat))
                } else {
                    viewModel.addExtraPayloadProfile(name, weight, category = cat)
                }
                showAddExtraDialog = false
                editingExtra = null
            }
        )
    }
}

@Composable
fun PayloadDialog(
    payload: PayloadLocation?,
    onDismiss: () -> Unit,
    onSave: (String, Int?, String) -> Unit
) {
    var name by remember { mutableStateOf(payload?.name ?: "") }
    var limitInput by remember { mutableStateOf(payload?.weightLimitGrams?.let { (it / 1000.0).toString() } ?: "") }
    var category by remember { mutableStateOf(payload?.category ?: "Vehicle") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = if (payload == null) "Add Packing Location" else "Edit Packing Location", 
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (e.g. Car Boot)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = limitInput, onValueChange = { limitInput = it }, label = { Text("Weight Limit (kg)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (e.g. vehicle, person)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val limitGrams = limitInput.toDoubleOrNull()?.let { (it * 1000).toInt() }
                onSave(name, limitGrams, category)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ExtraPayloadDialog(
    extraProfile: ExtraPayloadProfile?,
    onDismiss: () -> Unit,
    onSave: (String, Int, String) -> Unit
) {
    var name by remember { mutableStateOf(extraProfile?.name ?: "") }
    var weightInput by remember { mutableStateOf(extraProfile?.weightGrams?.let { (it / 1000.0).toString() } ?: "") }
    var category by remember { mutableStateOf(extraProfile?.category ?: "People") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = if (extraProfile == null) "Add Extra Payload" else "Edit Extra Payload", 
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (e.g. Adult 1)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = weightInput, onValueChange = { weightInput = it }, label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (e.g. people, water)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val grams = (weightInput.toDoubleOrNull() ?: 0.0) * 1000
                onSave(name, grams.toInt(), category)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
