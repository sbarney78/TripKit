package au.barney.tripkit.data.repository

import au.barney.tripkit.data.local.TripKitDao
import au.barney.tripkit.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.*

class TripKitRepository(private val dao: TripKitDao) {

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    // ------------------ LISTS ------------------

    fun getLists(): Flow<List<ListItem>> = dao.getLists()

    suspend fun getList(listId: Int): ListItem? = dao.getList(listId)

    suspend fun addList(
        name: String,
        showInventory: Boolean,
        showMenu: Boolean,
        showIngredients: Boolean,
        showItinerary: Boolean
    ): Long {
        val listId = dao.insertList(
            ListItem(
                name = name,
                created_at = getCurrentTimestamp(),
                show_inventory = showInventory,
                show_menu = showMenu,
                show_ingredients = showIngredients,
                show_itinerary = showItinerary
            )
        )
        if (showInventory) {
            populateFromMaster(listId.toInt())
        }
        return listId
    }

    private suspend fun populateFromMaster(listId: Int) {
        val masterItems = dao.getMasterItemsSyncList()
        masterItems.forEach { masterItem ->
            val entryId = dao.insertEntry(
                Entry(
                    entry_name = masterItem.name,
                    entry_type = if (masterItem.is_container) "container" else "single",
                    quantity = masterItem.default_quantity,
                    notes = null,
                    list_id = listId,
                    is_checked = 0
                )
            ).toInt()

            if (masterItem.is_container) {
                val subItems = dao.getMasterSubItemsSync(masterItem.id)
                subItems.forEach { sub ->
                    dao.insertItem(
                        Item(
                            entry_id = entryId,
                            item_name = sub.name,
                            quantity = sub.default_quantity,
                            notes = null,
                            is_checked = 0
                        )
                    )
                }
            }
        }
    }

    suspend fun updateList(list: ListItem) {
        dao.updateList(list)
    }

    suspend fun deleteList(listId: Int) = dao.deleteList(listId)

    suspend fun duplicateList(listId: Int, newName: String) {
        val original = dao.getList(listId) ?: return
        val newListId = dao.insertList(
            original.copy(
                id = 0,
                name = newName,
                created_at = getCurrentTimestamp()
            )
        ).toInt()

        val oldEntries = dao.getEntriesSync(listId)
        oldEntries.forEach { entry ->
            val newEntryId = dao.insertEntry(
                entry.copy(entry_id = 0, list_id = newListId, is_checked = 0)
            ).toInt()

            if (entry.entry_type == "container") {
                val oldItems = dao.getItemsSync(entry.entry_id)
                oldItems.forEach { item ->
                    dao.insertItem(item.copy(item_id = 0, entry_id = newEntryId, is_checked = 0))
                }
            }
        }

        val oldMenu = dao.getMenuSync(listId)
        oldMenu.forEach { meal ->
            dao.insertMenuItem(meal.copy(id = 0, list_id = newListId))
        }

        val oldGroups = dao.getIngredientGroupsSync(listId)
        oldGroups.forEach { group ->
            val newGroupId = dao.insertIngredientGroup(
                group.copy(id = 0, list_id = newListId)
            ).toInt()

            val oldIngredients = dao.getIngredientsSync(group.id)
            oldIngredients.forEach { ingredient ->
                dao.insertIngredient(
                    ingredient.copy(id = 0, group_id = newGroupId, is_checked = 0)
                )
            }
        }
    }


    // ------------------ ENTRIES ------------------

    fun getEntries(listId: Int): Flow<List<Entry>> = dao.getEntries(listId)

    fun getPackingProgress(listId: Int): Flow<Pair<Int, Int>> {
        return combine(
            dao.getTotalEntriesCount(listId),
            dao.getCheckedEntriesCount(listId),
            dao.getTotalSubItemsCount(listId),
            dao.getCheckedSubItemsCount(listId)
        ) { totalEntries, checkedEntries, totalSub, checkedSub ->
            Pair(checkedEntries + checkedSub, totalEntries + totalSub)
        }
    }

    suspend fun getEntriesSync(listId: Int) = dao.getEntriesSync(listId)

    suspend fun addEntry(
        name: String,
        type: String,
        quantity: Int,
        notes: String?,
        listId: Int
    ) {
        val entryId = dao.insertEntry(
            Entry(
                entry_name = name,
                entry_type = type,
                quantity = quantity,
                notes = notes,
                list_id = listId,
                is_checked = 0
            )
        ).toInt()

        // If it's a container, check if it exists in Master
        if (type == "container") {
            val masterItems = dao.getMasterItemsSyncList()
            val masterItem = masterItems.find { it.name.equals(name, ignoreCase = true) && it.is_container }
            if (masterItem != null) {
                val subItems = dao.getMasterSubItemsSync(masterItem.id)
                subItems.forEach { sub ->
                    dao.insertItem(Item(entry_id = entryId, item_name = sub.name, quantity = sub.default_quantity, notes = null, is_checked = 0))
                }
            }
        }
        
        // Add to master if not exists
        val masterItems = dao.getMasterItemsSyncList()
        if (masterItems.none { it.name.equals(name, ignoreCase = true) }) {
            dao.insertMasterItem(MasterItem(name = name, is_container = type == "container", default_quantity = quantity))
        }
    }

    suspend fun toggleEntry(id: Int, checked: Int) {
        dao.toggleEntry(id, checked)
        val entry = dao.getEntry(id)
        if (entry?.entry_type == "container") {
            val items = dao.getItemsSync(id)
            items.forEach { dao.toggleItem(it.item_id, checked) }
        }
    }

    suspend fun deleteEntry(id: Int) = dao.deleteEntry(id)

    suspend fun getEntry(entryId: Int): Entry? = dao.getEntry(entryId)

    suspend fun updateEntry(
        entryId: Int,
        name: String,
        quantity: Int,
        notes: String?,
        type: String
    ) {
        val existing = dao.getEntry(entryId)
        if (existing != null) {
            dao.updateEntry(
                existing.copy(
                    entry_name = name,
                    quantity = quantity,
                    notes = notes,
                    entry_type = type
                )
            )
        }
    }


    // ------------------ ITEMS ------------------

    fun getItems(entryId: Int): Flow<List<Item>> = dao.getItems(entryId)

    fun getAllItemsForList(listId: Int): Flow<List<Item>> = dao.getAllItemsForList(listId)

    suspend fun getAllItemsForListSync(listId: Int) = dao.getAllItemsForListSync(listId)

    suspend fun addItem(
        entryId: Int,
        name: String,
        quantity: Int,
        notes: String?
    ) {
        dao.insertItem(
            Item(
                entry_id = entryId,
                item_name = name,
                quantity = quantity,
                notes = notes,
                is_checked = 0
            )
        )
        dao.toggleEntry(entryId, 0)
        
        // Update Master Inventory ONLY as a sub-item of the parent container
        val entry = dao.getEntry(entryId)
        if (entry != null && entry.entry_type == "container") {
            val masterItems = dao.getMasterItemsSyncList()
            val masterItem = masterItems.find { it.name.equals(entry.entry_name, ignoreCase = true) && it.is_container }
            
            if (masterItem != null) {
                val masterSubItems = dao.getMasterSubItemsSync(masterItem.id)
                if (masterSubItems.none { it.name.equals(name, ignoreCase = true) }) {
                    dao.insertMasterSubItem(MasterSubItem(
                        master_item_id = masterItem.id, 
                        name = name, 
                        default_quantity = quantity
                    ))
                }
            }
            // If masterItem is null, we do nothing to Master Inventory.
        }
    }

    suspend fun deleteItem(id: Int) {
        val item = dao.getItem(id)
        dao.deleteItem(id)
        if (item != null) {
            updateContainerStatus(item.entry_id)
        }
    }

    suspend fun toggleItem(itemId: Int, checked: Int) {
        dao.toggleItem(itemId, checked)
        val item = dao.getItem(itemId)
        if (item != null) {
            updateContainerStatus(item.entry_id)
        }
    }

    private suspend fun updateContainerStatus(entryId: Int) {
        val items = dao.getItemsSync(entryId)
        if (items.isEmpty()) return
        
        val allChecked = items.all { it.is_checked == 1 }
        dao.toggleEntry(entryId, if (allChecked) 1 else 0)
    }

    suspend fun getItem(itemId: Int): Item? = dao.getItem(itemId)

    suspend fun updateItem(
        itemId: Int,
        name: String,
        quantity: Int,
        notes: String?
    ) {
        val existing = dao.getItem(itemId)
        if (existing != null) {
            dao.updateItem(
                existing.copy(
                    item_name = name,
                    quantity = quantity,
                    notes = notes
                )
            )
        }
    }


    // ------------------ MENU ------------------

    fun getMenu(listId: Int): Flow<List<MenuItem>> = dao.getMenu(listId)

    suspend fun getMenuSync(listId: Int) = dao.getMenuSync(listId)

    suspend fun addMenuItem(
        listId: Int,
        day: String,
        mealType: String,
        description: String
    ) {
        dao.insertMenuItem(
            MenuItem(
                list_id = listId,
                day = day,
                meal_type = mealType,
                description = description,
                created_at = getCurrentTimestamp()
            )
        )
    }

    suspend fun updateMenuItem(
        menuId: Int,
        day: String,
        mealType: String,
        description: String
    ) {
        val existing = dao.getMenuItemSync(menuId)
        if (existing != null) {
            dao.updateMenuItem(
                existing.copy(
                    day = day,
                    meal_type = mealType,
                    description = description
                )
            )
        }
    }

    suspend fun deleteMenuItem(menuId: Int) = dao.deleteMenuItem(menuId)


    // ------------------ INGREDIENT GROUPS ------------------

    fun getIngredientGroups(listId: Int): Flow<List<IngredientGroup>> = dao.getIngredientGroups(listId)

    suspend fun getIngredientGroupsSync(listId: Int) = dao.getIngredientGroupsSync(listId)

    suspend fun addIngredientGroup(listId: Int, groupName: String) {
        dao.insertIngredientGroup(
            IngredientGroup(
                list_id = listId,
                group_name = groupName,
                created_at = getCurrentTimestamp()
            )
        )
    }

    suspend fun getIngredientGroup(groupId: Int): IngredientGroup? = dao.getIngredientGroup(groupId)

    suspend fun updateIngredientGroup(groupId: Int, groupName: String) {
        val existing = dao.getIngredientGroup(groupId)
        if (existing != null) {
            dao.updateIngredientGroup(existing.copy(group_name = groupName))
        }
    }

    suspend fun deleteIngredientGroup(groupId: Int) = dao.deleteIngredientGroup(groupId)


    // ------------------ INGREDIENTS ------------------

    fun getIngredients(groupId: Int): Flow<List<Ingredient>> = dao.getIngredients(groupId)

    fun getAllIngredientsForList(listId: Int): Flow<List<Ingredient>> = dao.getAllIngredientsForList(listId)

    suspend fun getAllIngredientsForListSync(listId: Int) = dao.getAllIngredientsForListSync(listId)

    suspend fun addIngredient(groupId: Int, ingredientName: String) {
        dao.insertIngredient(
            Ingredient(
                group_id = groupId,
                ingredient_name = ingredientName,
                created_at = getCurrentTimestamp()
            )
        )
    }

    suspend fun getIngredient(ingredientId: Int): Ingredient? = dao.getIngredient(ingredientId)

    suspend fun updateIngredient(
        ingredientId: Int,
        ingredientName: String,
        quantity: Int,
        notes: String?,
        isChecked: Int
    ) {
        val existing = dao.getIngredient(ingredientId)
        if (existing != null) {
            dao.updateIngredient(
                existing.copy(
                    ingredient_name = ingredientName,
                    quantity = quantity,
                    notes = notes,
                    is_checked = isChecked
                )
            )
        }
    }

    suspend fun deleteIngredient(ingredientId: Int) = dao.deleteIngredient(ingredientId)

    suspend fun toggleIngredient(ingredientId: Int, isChecked: Int) = dao.toggleIngredient(ingredientId, isChecked)

    // ------------------ MASTER ITEMS ------------------
    
    fun getMasterItems(): Flow<List<MasterItem>> = dao.getMasterItems()
    
    suspend fun getMasterItem(id: Int) = dao.getMasterItem(id)

    suspend fun addMasterItem(name: String, isContainer: Boolean) {
        dao.insertMasterItem(MasterItem(name = name, is_container = isContainer))
    }

    suspend fun updateMasterItem(item: MasterItem) = dao.updateMasterItem(item)
    
    suspend fun deleteMasterItem(id: Int) = dao.deleteMasterItem(id)

    // --- MASTER SUB ITEMS ---

    fun getMasterSubItems(masterItemId: Int) = dao.getMasterSubItems(masterItemId)

    suspend fun insertMasterSubItem(item: MasterSubItem) = dao.insertMasterSubItem(item)

    suspend fun updateMasterSubItem(item: MasterSubItem) = dao.updateMasterSubItem(item)

    suspend fun deleteMasterSubItem(id: Int) = dao.deleteMasterSubItem(id)

    // ------------------ ITINERARY ------------------

    fun getItinerary(listId: Int): Flow<List<ItineraryItem>> = dao.getItinerary(listId)

    suspend fun getItinerarySync(listId: Int) = dao.getItinerarySync(listId)

    suspend fun getItineraryItem(id: Int) = dao.getItineraryItem(id)

    suspend fun addItineraryItem(listId: Int, day: String, time: String, activity: String, notes: String?, location: String?, price: Double?, departureDay: String?, departureTime: String?) {
        dao.insertItineraryItem(ItineraryItem(list_id = listId, day = day, time = time, activity = activity, notes = notes, location = location, price = price, departure_day = departureDay, departure_time = departureTime))
    }

    suspend fun updateItineraryItem(item: ItineraryItem) {
        dao.updateItineraryItem(item)
    }

    suspend fun deleteItineraryItem(itemId: Int) = dao.deleteItineraryItem(itemId)
}
