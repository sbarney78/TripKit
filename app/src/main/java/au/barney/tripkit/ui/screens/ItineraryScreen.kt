package au.barney.tripkit.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.ItineraryItem
import au.barney.tripkit.ui.viewmodel.ItineraryViewModel
import au.barney.tripkit.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(
    listId: Int,
    viewModel: ItineraryViewModel,
    onBack: () -> Unit
) {
    val itinerary by viewModel.itinerary.collectAsState()
    val currentList by viewModel.currentList.collectAsState()
    val context = LocalContext.current
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ItineraryItem?>(null) }

    LaunchedEffect(listId) {
        viewModel.loadItinerary(listId)
    }

    if (showAddDialog) {
        ItineraryDialog(
            title = "Add Activity",
            onDismiss = { showAddDialog = false },
            onConfirm = { d, t, a, n, loc, p, dd, dt ->
                viewModel.addItem(listId, d, t, a, n, loc, p, dd, dt)
                showAddDialog = false
            }
        )
    }

    if (showEditDialog && itemToEdit != null) {
        ItineraryDialog(
            title = "Edit Activity",
            initialItem = itemToEdit,
            onDismiss = { 
                showEditDialog = false
                itemToEdit = null
            },
            onConfirm = { d, t, a, n, loc, p, dd, dt ->
                viewModel.updateItem(itemToEdit!!.copy(day = d, time = t, activity = a, notes = n, location = loc, price = p, departure_day = dd, departure_time = dt))
                showEditDialog = false
                itemToEdit = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Itinerary - ${currentList?.name ?: ""}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        PdfGenerator.generateItineraryPdf(context, currentList?.name ?: "List_$listId", itinerary)
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { padding ->
        val sdfDate = SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val items = itinerary.flatMap { item ->
            val list = mutableListOf<DisplayItineraryItem>()
            list.add(DisplayItineraryItem(item.day, item.time, item.activity, item.location, item.price, item.notes, item, false))
            if (!item.departure_day.isNullOrBlank()) {
                list.add(DisplayItineraryItem(item.departure_day!!, item.departure_time ?: "", "Departure: ${item.activity}", item.location, null, null, item, true))
            }
            list
        }.sortedWith { a, b ->
            val dateA = try { sdfDate.parse(a.day) } catch (e: Exception) { null }
            val dateB = try { sdfDate.parse(b.day) } catch (e: Exception) { null }
            
            if (dateA != null && dateB != null) {
                val dateComparison = dateA.compareTo(dateB)
                if (dateComparison != 0) return@sortedWith dateComparison
            }
            
            val timeA = try { sdfTime.parse(a.time) } catch (e: Exception) { null }
            val timeB = try { sdfTime.parse(b.time) } catch (e: Exception) { null }
            
            if (timeA != null && timeB != null) {
                timeA.compareTo(timeB)
            } else {
                a.time.compareTo(b.time)
            }
        }

        val grouped = items.groupBy { it.day }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            grouped.forEach { (dayHeader, activities) ->
                item {
                    Text(dayHeader, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                items(activities) { item ->
                    ItineraryCard(
                        item = item,
                        onEdit = { 
                            itemToEdit = item.original
                            showEditDialog = true
                        },
                        onDelete = { viewModel.deleteItem(item.original.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItineraryCard(
    item: DisplayItineraryItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = { expanded = true }
            ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.time, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Text(item.activity, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (!item.location.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = item.location!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                    if (item.price != null) {
                        Text("Cost: AU$${String.format("%.2f", item.price)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    if (!item.notes.isNullOrBlank()) {
                        Text(item.notes!!, style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                if (!item.location.isNullOrBlank()) {
                    IconButton(onClick = {
                        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(item.location)}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        context.startActivity(mapIntent)
                    }) {
                        Icon(Icons.Default.Map, contentDescription = "View on Map", tint = MaterialTheme.colorScheme.primary)
                    }
                }
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

data class DisplayItineraryItem(
    val day: String,
    val time: String,
    val activity: String,
    val location: String?,
    val price: Double?,
    val notes: String?,
    val original: ItineraryItem,
    val isDeparture: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryDialog(
    title: String,
    initialItem: ItineraryItem? = null,
    onDismiss: () -> Unit,
    onConfirm: (day: String, time: String, activity: String, notes: String?, location: String?, price: Double?, depDay: String?, depTime: String?) -> Unit
) {
    var day by remember { mutableStateOf(initialItem?.day ?: "") }
    var time by remember { mutableStateOf(initialItem?.time ?: "") }
    var activity by remember { mutableStateOf(initialItem?.activity ?: "") }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }
    var location by remember { mutableStateOf(initialItem?.location ?: "") }
    var price by remember { mutableStateOf(initialItem?.price?.toString() ?: "") }
    var depDay by remember { mutableStateOf(initialItem?.departure_day ?: "") }
    var depTime by remember { mutableStateOf(initialItem?.departure_time ?: "") }

    var activePicker by remember { mutableStateOf<String?>(null) } // "day", "time", "depDay", "depTime"
    val datePickerState = rememberDatePickerState()
    val calendar = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = false
    )

    if (activePicker == "day" || activePicker == "depDay") {
        DatePickerDialog(
            onDismissRequest = { activePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault())
                        if (activePicker == "day") day = sdf.format(Date(it))
                        else depDay = sdf.format(Date(it))
                    }
                    activePicker = null
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (activePicker == "time" || activePicker == "depTime") {
        AlertDialog(
            onDismissRequest = { activePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    val hour = timePickerState.hour
                    val minute = timePickerState.minute
                    val amPm = if (hour < 12) "AM" else "PM"
                    val displayHour = if (hour % 12 == 0) 12 else hour % 12
                    val t = String.format("%02d:%02d %s", displayHour, minute, amPm)
                    if (activePicker == "time") time = t else depTime = t
                    activePicker = null
                }) { Text("OK") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Activity Start", fontWeight = FontWeight.Bold) }
                item { OutlinedButton(onClick = { activePicker = "day" }, modifier = Modifier.fillMaxWidth()) { Text(if (day.isEmpty()) "Select Date" else day) } }
                item { OutlinedButton(onClick = { activePicker = "time" }, modifier = Modifier.fillMaxWidth()) { Text(if (time.isEmpty()) "Select Time" else time) } }
                item { OutlinedTextField(value = activity, onValueChange = { activity = it }, label = { Text("Activity") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Address / Location") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (AU$)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
                
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                item { Text("Departure (Optional)", fontWeight = FontWeight.Bold) }
                item { OutlinedButton(onClick = { activePicker = "depDay" }, modifier = Modifier.fillMaxWidth()) { Text(if (depDay.isEmpty()) "Departure Date" else depDay) } }
                item { OutlinedButton(onClick = { activePicker = "depTime" }, modifier = Modifier.fillMaxWidth()) { Text(if (depTime.isEmpty()) "Departure Time" else depTime) } }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (activity.isNotBlank() && day.isNotBlank()) {
                    onConfirm(day, time, activity, notes, location, price.toDoubleOrNull(), depDay.ifBlank { null }, depTime.ifBlank { null })
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
