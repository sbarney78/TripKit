package au.barney.tripkit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.barney.tripkit.data.model.ExtraWeightProfile
import au.barney.tripkit.ui.viewmodel.ListViewModel
import au.barney.tripkit.util.WeightUtils.formatWeight
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    listId: Int,
    listName: String,
    viewModel: ListViewModel,
    onBack: () -> Unit
) {
    val weightDetails by viewModel.getListWeightDetails(listId).collectAsState(initial = null)
    val extraProfiles by viewModel.extraWeightProfiles.collectAsState()
    var selectedProfileIds by remember { mutableStateOf(setOf<Int>()) }
    var showAddProfileDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val totalGearWeight = weightDetails?.totalGearWeightGrams ?: 0
    val totalExtrasWeight = extraProfiles.filter { it.id in selectedProfileIds }.sumOf { it.weightGrams }
    val finalTotalWeight = totalGearWeight + totalExtrasWeight

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weight Analysis - $listName", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddProfileDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Extra Weight")
                    }
                }
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
            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        WeightSummaryRow("Gear Weight:", totalGearWeight)
                        WeightSummaryRow("Extras Weight:", totalExtrasWeight)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        WeightSummaryRow("Final Total:", finalTotalWeight, isTotal = true)
                    }
                }
            }

            // Warning indicators
            item {
                WeightWarnings(totalGearWeight, extraProfiles.filter { it.id in selectedProfileIds })
            }

            item {
                Text("Extra Weight Sources:", style = MaterialTheme.typography.titleMedium)
            }

            items(extraProfiles) { profile ->
                val isSelected = profile.id in selectedProfileIds
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        selectedProfileIds = if (isSelected) {
                            selectedProfileIds - profile.id
                        } else {
                            selectedProfileIds + profile.id
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isSelected, onCheckedChange = {
                            selectedProfileIds = if (it) selectedProfileIds + profile.id else selectedProfileIds - profile.id
                        })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.name, fontWeight = FontWeight.SemiBold)
                            Text(profile.category, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(formatWeight(profile.weightGrams), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.deleteExtraWeightProfile(profile.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddProfileDialog) {
        AddExtraWeightDialog(
            onDismiss = { showAddProfileDialog = false },
            onSave = { name, weight, category ->
                viewModel.addExtraWeightProfile(name, weight, category)
                showAddProfileDialog = false
            }
        )
    }
}

@Composable
fun WeightSummaryRow(label: String, grams: Int, isTotal: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = formatWeight(grams),
            style = if (isTotal) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WeightWarnings(gearWeight: Int, selectedExtras: List<ExtraWeightProfile>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        selectedExtras.forEach { profile ->
            when {
                profile.category.equals("Backpack", true) && profile.weightGrams > 15000 -> {
                    WarningCard("Backpack exceeds 15kg!", Color.Red)
                }
                profile.category.equals("Roof Rack", true) && profile.weightGrams > 20000 -> {
                    WarningCard("Roof rack exceeds 20kg!", Color.Red)
                }
                // Add more logic-based warnings here
            }
        }
    }
}

@Composable
fun WarningCard(message: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Text(
            text = "⚠️ $message",
            modifier = Modifier.padding(8.dp),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun AddExtraWeightDialog(onDismiss: () -> Unit, onSave: (String, Int, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }
    var category by remember { mutableStateOf("Vehicle") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Extra Weight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (e.g. Car)") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = weightInput, onValueChange = { weightInput = it }, label = { Text("Weight") }, modifier = Modifier.weight(1f))
                    TextButton(onClick = { unit = if (unit == "kg") "g" else "kg" }) { Text(unit) }
                }
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val grams = try {
                    val v = weightInput.toDouble()
                    if (unit == "kg") (v * 1000).toInt() else v.toInt()
                } catch (e: Exception) { 0 }
                onSave(name, grams, category)
            }) { Text("Save") }
        }
    )
}
