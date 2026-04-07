package au.barney.tripkit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.barney.tripkit.ui.viewmodel.MasterItemViewModel
import au.barney.tripkit.ui.viewmodel.TemplateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTemplateScreen(
    masterViewModel: MasterItemViewModel,
    templateViewModel: TemplateViewModel,
    onBack: () -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    val masterItems by masterViewModel.masterItems.collectAsState()
    val selectedIds = remember { mutableStateListOf<Int>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Template", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = templateName,
                onValueChange = { templateName = it },
                label = { Text("Template Name (e.g. Camping)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Select items from Master Inventory:", fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(masterItems) { item ->
                    val isSelected = selectedIds.contains(item.id)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (isSelected) selectedIds.remove(item.id)
                            else selectedIds.add(item.id)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    if (it) selectedIds.add(item.id)
                                    else selectedIds.remove(item.id)
                                }
                            )
                            
                            if (item.is_container) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(24.dp)
                                        .background(Color(android.graphics.Color.parseColor(item.color)))
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            
                            Text(item.name, modifier = Modifier.weight(1f))
                            
                            if (item.is_container) {
                                Badge { Text("Container") }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (templateName.isNotBlank() && selectedIds.isNotEmpty()) {
                        templateViewModel.createTemplate(templateName, selectedIds.toList())
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = templateName.isNotBlank() && selectedIds.isNotEmpty()
            ) {
                Text("Create Template")
            }
        }
    }
}
