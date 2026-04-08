package au.barney.tripkit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.barney.tripkit.data.model.ExtraPayloadProfile
import au.barney.tripkit.data.model.PayloadAnalysis
import au.barney.tripkit.ui.viewmodel.ListViewModel
import au.barney.tripkit.util.WeightUtils.formatWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    listId: Int,
    listName: String,
    viewModel: ListViewModel,
    onManagePayloads: () -> Unit,
    onBack: () -> Unit
) {
    val weightDetails by viewModel.getListWeightDetails(listId).collectAsState(initial = null)
    val extraPayloadProfiles by viewModel.extraPayloadProfiles.collectAsState()
    val activePayloadLinks by viewModel.getActiveExtraPayloads(listId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weight Analysis: $listName", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back", 
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onManagePayloads) {
                        Icon(
                            Icons.Default.Settings, 
                            contentDescription = "Manage Payloads", 
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
            weightDetails?.let { details ->
                item {
                    SummaryCard(
                        totalGear = details.totalGearWeightGrams,
                        extraPayload = details.extraPayloadWeightGrams
                    )
                }

                item {
                    Text(
                        text = "Payload Locations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(details.payloadAnalysis) { analysis ->
                    PayloadLocationCard(analysis)
                }
            }

            item {
                Text(
                    text = "List Extra Payloads",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (extraPayloadProfiles.isEmpty()) {
                item {
                    Text(
                        "No extra payloads defined. Add profiles in 'Manage Payloads' to see them here.", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = Color.Gray
                    )
                }
            } else {
                items(extraPayloadProfiles) { profile ->
                    val isActive = activePayloadLinks.any { it.payloadProfileId == profile.id }
                    ExtraPayloadToggleRow(
                        profile = profile,
                        isActive = isActive,
                        onToggle = { checked ->
                            viewModel.toggleExtraPayloadForList(listId, profile.id, checked)
                        }
                    )
                }
            }
            
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SummaryCard(totalGear: Int, extraPayload: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Total Summary", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Gear Weight:")
                Text(formatWeight(totalGear), fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Extra Payload Weight:")
                Text(formatWeight(extraPayload), fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color.White.copy(alpha = 0.3f)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("GRAND TOTAL:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = formatWeight(totalGear + extraPayload), 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp, 
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun PayloadLocationCard(analysis: PayloadAnalysis) {
    val loc = analysis.location
    val current = analysis.currentWeightGrams
    val limit = loc.weightLimitGrams ?: 0
    val isOver = limit > 0 && current > limit
    val percentage = if (limit > 0) current.toFloat() / limit.toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(loc.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        loc.category,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Current Weight: ${formatWeight(current)}")
                if (limit > 0) {
                    Text("Limit: ${formatWeight(limit)}", color = if (isOver) Color.Red else Color.Unspecified)
                }
            }

            if (limit > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { percentage.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (isOver) Color.Red else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                if (isOver) {
                    Text(
                        "OVER LIMIT BY ${formatWeight(current - limit)}",
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExtraPayloadToggleRow(
    profile: ExtraPayloadProfile,
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isActive,
            onCheckedChange = onToggle
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(profile.name, style = MaterialTheme.typography.bodyLarge)
            Text(profile.category, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        Text(formatWeight(profile.weightGrams), fontWeight = FontWeight.Bold)
    }
}
