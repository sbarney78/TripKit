package au.barney.tripkit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About TripKit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "TripKit is your ultimate companion for planning and organizing camping trips and outdoor adventures. Designed by campers for campers, TripKit helps you manage everything from gear lists and meal plans to ingredients and detailed itineraries.",
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )

            Text(
                text = "Key Features:",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            FeatureItem("Hierarchical Inventory", "Organize your gear into containers and items, with weight tracking at every level.")
            FeatureItem("Meal Planning", "Plan your breakfast, lunch, dinner, and snacks for every day of your trip.")
            FeatureItem("Shopping Lists", "Automatically group ingredients for your planned meals.")
            FeatureItem("Itineraries", "Keep track of your activities, bookings, and locations.")
            FeatureItem("Templates", "Save your favorite gear setups as templates to quickly start new trip lists.")
            FeatureItem("Weight Analysis", "Monitor your total gear weight and see how it's distributed across different payload locations.")
            FeatureItem("Data Sync & Sharing", "Export and import trips to share with friends or sync between devices.")

            Spacer(Modifier.height(16.dp))

            Text(
                text = "TripKit is built with Jetpack Compose and Room for a modern, fast, and reliable experience.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun FeatureItem(title: String, description: String) {
    Column(modifier = Modifier.padding(start = 8.dp)) {
        Text(
            text = "• $title",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
