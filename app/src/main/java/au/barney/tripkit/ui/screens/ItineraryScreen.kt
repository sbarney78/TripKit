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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.barney.tripkit.data.model.ItineraryItem
import au.barney.tripkit.ui.viewmodel.ItineraryViewModel
import au.barney.tripkit.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(
    listId: Int,
    viewModel: ItineraryViewModel,
    onBack: () -> Unit,
    onViewPdf: (String) -> Unit
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
            onConfirm = { d, t, a, n, loc, p, dd, dt, cat, ref, som ->
                viewModel.addItem(listId, d, t, a, n, loc, p, dd, dt, cat, ref, som)
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
            onConfirm = { d, t, a, n, loc, p, dd, dt, cat, ref, som ->
                viewModel.updateItem(itemToEdit!!.copy(
                    day = d, 
                    time = t, 
                    activity = a, 
                    notes = n, 
                    location = loc, 
                    price = p, 
                    departure_day = dd, 
                    departure_time = dt,
                    category = cat,
                    booking_ref = ref,
                    show_on_map = som
                ))
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
                        val file = PdfGenerator.generateItineraryPdf(context, currentList?.name ?: "List_$listId", itinerary)
                        file?.let { onViewPdf(it.absolutePath) }
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

        val sortedItinerary = itinerary.sortedWith { a, b ->
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

        val grouped = sortedItinerary.groupBy { it.day }
        
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
                            itemToEdit = item
                            showEditDialog = true
                        },
                        onDelete = { viewModel.deleteItem(item.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItineraryCard(
    item: ItineraryItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val sdfFullDate = SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault())
    val sdfShortDate = SimpleDateFormat("EEE dd MMM", Locale.getDefault())
    val sdfFullTime = SimpleDateFormat("hh:mm a", Locale.getDefault())

    // Helper to format time as 7 PM instead of 07:00 PM
    fun formatShortTime(timeStr: String?): String {
        if (timeStr.isNullOrBlank()) return ""
        return try {
            val date = sdfFullTime.parse(timeStr)
            if (date != null) {
                val cal = Calendar.getInstance().apply { time = date }
                val hour = if (cal.get(Calendar.HOUR) == 0) 12 else cal.get(Calendar.HOUR)
                val minute = cal.get(Calendar.MINUTE)
                val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
                if (minute == 0) "$hour $amPm" else "$hour:${String.format("%02d", minute)} $amPm"
            } else timeStr
        } catch (e: Exception) { timeStr }
    }

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
                    // Activity Name with Icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val categoryIcon = when (item.category) {
                            "Travel" -> Icons.Default.DirectionsCar
                            "Meal" -> Icons.Default.Restaurant
                            "Accommodation" -> Icons.Default.Hotel
                            "Activity" -> Icons.Default.Sailing
                            "Holiday" -> Icons.Default.BeachAccess
                            "Coffee" -> Icons.Default.LocalCafe
                            "IceCream" -> Icons.Default.Icecream
                            "Easter Bunny" -> Icons.Default.Egg
                            "Santa" -> Icons.Default.Toys
                            "Shopping" -> Icons.Default.ShoppingBag
                            "Sightseeing" -> Icons.Default.CameraAlt
                            else -> Icons.Default.Event
                        }
                        Icon(categoryIcon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(item.activity, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(4.dp))

                    // Arrival / Departure Row
                    val startTime = formatShortTime(item.time)
                    val endTime = formatShortTime(item.departure_time)
                    val hasDepDay = !item.departure_day.isNullOrBlank()
                    
                    val timeString = buildString {
                        append(startTime)
                        if (hasDepDay || !endTime.isEmpty()) {
                            append(" → ")
                            if (hasDepDay) {
                                try {
                                    val d = sdfFullDate.parse(item.departure_day!!)
                                    if (d != null) append(sdfShortDate.format(d)).append(", ")
                                } catch (e: Exception) { append(item.departure_day).append(", ") }
                            }
                            append(endTime)
                        }
                    }
                    Text(timeString, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)

                    // Calculate and Show Duration
                    val durationText = try {
                        val startD = sdfFullDate.parse(item.day)
                        val endDayStr = if (!item.departure_day.isNullOrBlank()) item.departure_day else item.day
                        val endD = sdfFullDate.parse(endDayStr!!)
                        
                        if (startD != null && endD != null) {
                            if (startD.before(endD)) {
                                // Multi-day: Show nights
                                val diffInMillis = endD.time - startD.time
                                val nights = TimeUnit.MILLISECONDS.toDays(diffInMillis)
                                if (nights > 0) "$nights night${if (nights > 1) "s" else ""}" else null
                            } else {
                                // Same day: Show hours and minutes
                                val startT = if (item.time.isNotBlank()) sdfFullTime.parse(item.time) else null
                                val endT = if (!item.departure_time.isNullOrBlank()) sdfFullTime.parse(item.departure_time!!) else null
                                
                                if (startT != null && endT != null) {
                                    val diffInMillis = endT.time - startT.time
                                    if (diffInMillis > 0) {
                                        val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
                                        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60
                                        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                                    } else null
                                } else null
                            }
                        } else null
                    } catch (e: Exception) { null }

                    if (durationText != null) {
                        Text("Duration: $durationText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                    }

                    if (!item.location.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(4.dp))
                            Text(item.location!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    if (item.price != null && item.price > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("Price: $${String.format("%.2f", item.price)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (!item.booking_ref.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(4.dp))
                            Text("Ref: ${item.booking_ref}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    if (item.show_on_map && !item.location.isNullOrBlank()) {
                        IconButton(
                            onClick = {
                                val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(item.location)}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                context.startActivity(mapIntent)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = "View on Map", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        expanded = false
                        onEdit()
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        expanded = false
                        onDelete()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                )
                if (!item.location.isNullOrBlank() && item.show_on_map) {
                    DropdownMenuItem(
                        text = { Text("Navigate") },
                        onClick = {
                            expanded = false
                            val uri = Uri.parse("geo:0,0?q=${Uri.encode(item.location)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            val chooser = Intent.createChooser(mapIntent, "Navigate with")
                            context.startActivity(chooser)
                        },
                        leadingIcon = { Icon(Icons.Default.Navigation, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryDialog(
    title: String,
    initialItem: ItineraryItem? = null,
    onDismiss: () -> Unit,
    onConfirm: (day: String, time: String, activity: String, notes: String?, location: String?, price: Double?, depDay: String?, depTime: String?, category: String?, bookingRef: String?, showOnMap: Boolean) -> Unit
) {
    var day by remember { mutableStateOf(initialItem?.day ?: "") }
    var time by remember { mutableStateOf(initialItem?.time ?: "") }
    var activity by remember { mutableStateOf(initialItem?.activity ?: "") }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }
    var location by remember { mutableStateOf(initialItem?.location ?: "") }
    var price by remember { mutableStateOf(initialItem?.price?.toString() ?: "") }
    var depDay by remember { mutableStateOf(initialItem?.departure_day ?: "") }
    var depTime by remember { mutableStateOf(initialItem?.departure_time ?: "") }
    var category by remember { mutableStateOf(initialItem?.category ?: "Activity") }
    var bookingRef by remember { mutableStateOf(initialItem?.booking_ref ?: "") }
    var showOnMap by remember { mutableStateOf(initialItem?.show_on_map ?: true) }

    var activePicker by remember { mutableStateOf<String?>(null) } // "day", "time", "depDay", "depTime"
    val datePickerState = rememberDatePickerState()
    val calendar = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = false
    )

    val categories = listOf("Activity", "Holiday", "Travel", "Meal", "Accommodation", "Coffee", "IceCream", "Easter Bunny", "Santa", "Shopping", "Sightseeing", "Other")
    var categoryExpanded by remember { mutableStateOf(false) }

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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { 
                    Column {
                        Text("Arrival / Start", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { activePicker = "day" }, modifier = Modifier.weight(1f)) { 
                                Text(if (day.isEmpty()) "Select Date" else day) 
                            }
                            OutlinedButton(onClick = { activePicker = "time" }, modifier = Modifier.weight(1f)) { 
                                Text(if (time.isEmpty()) "Select Time" else time) 
                            }
                        }
                    }
                }
                
                item { 
                    Column {
                        Text("Departure / Finish", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { activePicker = "depDay" }, modifier = Modifier.weight(1f)) { 
                                Text(if (depDay.isEmpty()) "Select Date" else depDay) 
                            }
                            OutlinedButton(onClick = { activePicker = "depTime" }, modifier = Modifier.weight(1f)) { 
                                Text(if (depTime.isEmpty()) "Select Time" else depTime) 
                            }
                        }
                    }
                }

                item { OutlinedTextField(value = activity, onValueChange = { activity = it }, label = { Text("Activity") }, modifier = Modifier.fillMaxWidth()) }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Address / Location") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Show on Map", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = showOnMap,
                                onCheckedChange = { showOnMap = it },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }
                }
                
                item { OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (AU$)") }, modifier = Modifier.fillMaxWidth()) }
                
                item {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item { OutlinedTextField(value = bookingRef, onValueChange = { bookingRef = it }, label = { Text("Booking Ref") }, modifier = Modifier.fillMaxWidth()) }
                
                item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (activity.isNotBlank() && day.isNotBlank()) {
                    onConfirm(day, time, activity, notes.ifBlank { null }, location.ifBlank { null }, price.toDoubleOrNull(), depDay.ifBlank { null }, depTime.ifBlank { null }, category, bookingRef.ifBlank { null }, showOnMap)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
