package au.barney.tripkit.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import au.barney.tripkit.data.local.TripKitDatabase
import au.barney.tripkit.data.repository.TripKitRepository
import au.barney.tripkit.ui.screens.*
import au.barney.tripkit.ui.viewmodel.*

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {

    val navController = rememberNavController()
    val context = LocalContext.current

    val database = TripKitDatabase.getDatabase(context)
    val repository = TripKitRepository(database.tripKitDao())

    val listViewModel: ListViewModel = viewModel(factory = ListViewModelFactory(repository))
    val entryViewModel: EntryViewModel = viewModel(factory = EntryViewModelFactory(repository))
    val itemViewModel: ItemViewModel = viewModel(factory = ItemViewModelFactory(repository))
    val menuViewModel: MenuViewModel = viewModel(factory = MenuViewModelFactory(repository))
    val ingredientGroupViewModel: IngredientGroupViewModel = viewModel(factory = IngredientGroupViewModelFactory(repository))
    val ingredientViewModel: IngredientViewModel = viewModel(factory = IngredientViewModelFactory(repository))
    val masterItemViewModel: MasterItemViewModel = viewModel(factory = MasterItemViewModelFactory(repository))
    val itineraryViewModel: ItineraryViewModel = viewModel(factory = ItineraryViewModelFactory(repository))

    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {
        composable("splash") {
            SplashScreen(onTimeout = {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }

        composable("home") {
            HomeScreen(
                viewModel = listViewModel,
                entryViewModel = entryViewModel,
                itemViewModel = itemViewModel,
                menuViewModel = menuViewModel,
                ingredientGroupViewModel = ingredientGroupViewModel,
                ingredientViewModel = ingredientViewModel,
                itineraryViewModel = itineraryViewModel,
                onOpenInventory = { listId -> navController.navigate("entries/$listId") },
                onOpenMenu = { listId -> navController.navigate("menu/$listId") },
                onOpenIngredients = { listId -> navController.navigate("ingredient_groups/$listId") },
                onOpenItinerary = { listId -> navController.navigate("itinerary/$listId") },
                onOpenMasterInventory = { navController.navigate("master_inventory") }
            )
        }

        composable("master_inventory") {
            MasterInventoryScreen(
                viewModel = masterItemViewModel,
                onBack = { navController.popBackStack() },
                onOpenContainer = { masterItemId -> navController.navigate("master_items/$masterItemId") }
            )
        }

        composable(
            route = "master_items/{masterItemId}",
            arguments = listOf(navArgument("masterItemId") { type = NavType.IntType })
        ) { backStackEntry ->
            val masterItemId = backStackEntry.arguments?.getInt("masterItemId") ?: 0
            MasterSubItemsScreen(
                masterItemId = masterItemId,
                viewModel = masterItemViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "itinerary/{listId}",
            arguments = listOf(navArgument("listId") { type = NavType.IntType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getInt("listId") ?: 0
            ItineraryScreen(
                listId = listId,
                viewModel = itineraryViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "entries/{listId}",
            arguments = listOf(navArgument("listId") { type = NavType.IntType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getInt("listId") ?: 0
            EntryListScreen(
                listId = listId,
                viewModel = entryViewModel,
                itemViewModel = itemViewModel,
                masterItemViewModel = masterItemViewModel,
                onOpenEntryItems = { entryId -> navController.navigate("items/$entryId") },
                onAddEntry = { navController.navigate("add_entry/$listId") },
                onEditEntry = { entryId -> navController.navigate("edit_entry/$listId/$entryId") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "add_entry/{listId}",
            arguments = listOf(navArgument("listId") { type = NavType.IntType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getInt("listId") ?: 0
            AddEntryScreen(
                listId = listId,
                viewModel = entryViewModel,
                masterViewModel = masterItemViewModel,
                onDone = { navController.popBackStack() }
            )
        }

        composable(
            route = "edit_entry/{listId}/{entryId}",
            arguments = listOf(
                navArgument("listId") { type = NavType.IntType },
                navArgument("entryId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getInt("listId") ?: 0
            val entryId = backStackEntry.arguments?.getInt("entryId") ?: 0
            EditEntryScreen(
                listId = listId,
                entryId = entryId,
                viewModel = entryViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "items/{entryId}",
            arguments = listOf(navArgument("entryId") { type = NavType.IntType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getInt("entryId") ?: 0
            ContainerItemsScreen(
                entryId = entryId,
                viewModel = itemViewModel,
                entryViewModel = entryViewModel,
                masterItemViewModel = masterItemViewModel,
                onAddItem = { navController.navigate("add_item/$entryId") },
                onBack = { navController.popBackStack() },
                onEditItem = { itemId -> navController.navigate("edit_item/$itemId") }
            )
        }

        composable(
            route = "add_item/{entryId}",
            arguments = listOf(navArgument("entryId") { type = NavType.IntType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getInt("entryId") ?: 0
            AddItemScreen(
                entryId = entryId,
                viewModel = itemViewModel,
                masterViewModel = masterItemViewModel,
                onDone = { navController.popBackStack() }
            )
        }

        composable(
            route = "edit_item/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.IntType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getInt("itemId") ?: 0
            EditItemScreen(
                itemId = itemId,
                viewModel = itemViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "menu/{listId}",
            arguments = listOf(navArgument("listId") { type = NavType.IntType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getInt("listId") ?: 0
            MenuScreen(
                listId = listId,
                viewModel = menuViewModel,
                onBack = { navController.popBackStack() },
                onAddMeal = { navController.navigate("menu_add/$listId") },
                onEditMeal = { meal -> navController.navigate("menu_edit/${meal.id}") },
                onCreatePdf = { _ -> }
            )
        }

        composable(
            route = "menu_add/{listId}",
            arguments = listOf(navArgument("listId") { type = NavType.IntType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getInt("listId") ?: 0
            AddMealScreen(
                listId = listId,
                viewModel = menuViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "menu_edit/{mealId}",
            arguments = listOf(navArgument("mealId") { type = NavType.IntType })
        ) { backStackEntry ->
            val mealId = backStackEntry.arguments?.getInt("mealId") ?: 0
            val menu by menuViewModel.menu.collectAsState()
            val meal = menu.firstOrNull { it.id == mealId }
            if (meal != null) {
                EditMealScreen(
                    meal = meal,
                    viewModel = menuViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = "ingredient_groups/{listId}",
            arguments = listOf(navArgument("listId") { type = NavType.IntType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getInt("listId") ?: 0
            IngredientGroupScreen(
                listId = listId,
                viewModel = ingredientGroupViewModel,
                ingredientViewModel = ingredientViewModel,
                onOpenGroup = { groupId -> navController.navigate("ingredients/$groupId") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "ingredients/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.IntType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getInt("groupId") ?: 0
            IngredientScreen(
                groupId = groupId,
                viewModel = ingredientViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
