package au.barney.tripkit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.barney.tripkit.ui.viewmodel.ListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    viewModel: ListViewModel,
    onOpenList: (Int) -> Unit,
    onOpenMenu: (Int) -> Unit,
    onOpenIngredientGroups: (Int) -> Unit
) {
    val lists by viewModel.lists.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadLists()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TripKit Lists") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.addList("New List")
            }) {
                Text("+")
            }
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
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(lists) { list ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                onClick = { onOpenList(list.id) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        list.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(onClick = { onOpenMenu(list.id) }) {
                                            Text("Menu")
                                        }
                                        Button(onClick = { onOpenIngredientGroups(list.id) }) {
                                            Text("Ingredients")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
