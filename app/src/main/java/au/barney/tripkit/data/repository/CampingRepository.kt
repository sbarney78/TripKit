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
                    is_checked = 0,
                    image_path = masterItem.image_path
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
                            is_checked = 0,
                            is_container = sub.is_container,
                            image_path = sub.image_path
                        )
                    )
                }
            }
        }
    }

    suspend fun updateList(list: ListItem) {
        dao.updateList(list.copy(last_updated = System.currentTimeMillis()))
    }

    suspend fun deleteList(listId: Int) = dao.deleteList(listId)

    suspend fun duplicateList(listId: Int, newName: String) {
        val original = dao.getList(listId) ?: return
        val newListId = dao.insertList(
            original.copy(
                id = 0,
                name = newName,
                created_at = getCurrentTimestamp(),
                sync_id = UUID.randomUUID().toString(),
                last_updated = System.currentTimeMillis()
            )
        ).toInt()

        val oldEntries = dao.getEntriesSync(listId)
        oldEntries.forEach { entry ->
            val newEntryId = dao.insertEntry(
                entry.copy(
                    entry_id = 0, 
                    list_id = newListId, 
                    is_checked = 0,
                    sync_id = UUID.randomUUID().toString(),
                    last_updated = System.currentTimeMillis()
                )
            ).toInt()

            if (entry.entry_type == "container") {
                val oldItems = dao.getItemsSync(entry.entry_id)
                oldItems.forEach { item ->
                    dao.insertItem(
                        item.copy(
                            item_id = 0, 
                            entry_id = newEntryId, 
                            is_checked = 0,
                            sync_id = UUID.randomUUID().toString(),
                            last_updated = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        val oldMenu = dao.getMenuSync(listId)
        oldMenu.forEach { meal ->
            dao.insertMenuItem(
                meal.copy(
                    id = 0, 
                    list_id = newListId,
                    sync_id = UUID.randomUUID().toString(),
                    last_updated = System.currentTimeMillis()
                )
            )
        }

        val oldGroups = dao.getIngredientGroupsSync(listId)
        oldGroups.forEach { group ->
            val newGroupId = dao.insertIngredientGroup(
                group.copy(
                    id = 0, 
                    list_id = newListId,
                    sync_id = UUID.randomUUID().toString()
                )
            ).toInt()

            val oldIngredients = dao.getIngredientsSync(group.id)
            oldIngredients.forEach { ingredient ->
                dao.insertIngredient(
                    ingredient.copy(
                        id = 0, 
                        group_id = newGroupId, 
                        is_checked = 0,
                        sync_id = UUID.randomUUID().toString(),
                        last_updated = System.currentTimeMillis()
                    )
                )
            }
        }
    }


    // ------------------ ENTRIES ------------------

    fun getEntries(listId: Int): Flow<List<Entry>> = dao.getEntries(listId)

    fun getEntriesWithCount(listId: Int): Flow<List<EntryWithCount>> = dao.getEntriesWithCount(listId)

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
        listId: Int,
        imagePath: String? = null
    ) {
        val entryId = dao.insertEntry(
            Entry(
                entry_name = name,
                entry_type = type,
                quantity = quantity,
                notes = notes,
                list_id = listId,
                is_checked = 0,
                image_path = imagePath
            )
        ).toInt()

        // If it's a container, check if it exists in Master
        if (type == "container") {
            val masterItems = dao.getMasterItemsSyncList()
            val masterItem = masterItems.find { it.name.equals(name, ignoreCase = true) && it.is_container }
            if (masterItem != null) {
                val subItems = dao.getMasterSubItemsSync(masterItem.id)
                subItems.forEach { sub ->
                    dao.insertItem(Item(
                        entry_id = entryId, 
                        item_name = sub.name, 
                        quantity = sub.default_quantity, 
                        notes = null, 
                        is_checked = 0, 
                        is_container = sub.is_container,
                        image_path = sub.image_path
                    ))
                }
            }
        }
        
        // Add to master if not exists
        val masterItems = dao.getMasterItemsSyncList()
        if (masterItems.none { it.name.equals(name, ignoreCase = true) }) {
            dao.insertMasterItem(MasterItem(name = name, is_container = type == "container", default_quantity = quantity, image_path = imagePath))
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
        type: String,
        imagePath: String? = null
    ) {
        val existing = dao.getEntry(entryId)
        if (existing != null) {
            dao.updateEntry(
                existing.copy(
                    entry_name = name,
                    quantity = quantity,
                    notes = notes,
                    entry_type = type,
                    image_path = imagePath,
                    last_updated = System.currentTimeMillis()
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
        notes: String?,
        isContainer: Boolean,
        imagePath: String? = null,
        addToMaster: Boolean = true
    ) {
        dao.insertItem(
            Item(
                entry_id = entryId,
                item_name = name,
                quantity = quantity,
                notes = notes,
                is_checked = 0,
                is_container = isContainer,
                image_path = imagePath
            )
        )
        dao.toggleEntry(entryId, 0)
        
        if (addToMaster) {
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
                            default_quantity = quantity,
                            is_container = isContainer,
                            image_path = imagePath
                        ))
                    }
                }
            }
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
        notes: String?,
        isContainer: Boolean,
        imagePath: String? = null
    ) {
        val existing = dao.getItem(itemId)
        if (existing != null) {
            dao.updateItem(
                existing.copy(
                    item_name = name,
                    quantity = quantity,
                    notes = notes,
                    is_container = isContainer,
                    image_path = imagePath,
                    last_updated = System.currentTimeMillis()
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
                    description = description,
                    last_updated = System.currentTimeMillis()
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
                    is_checked = isChecked,
                    last_updated = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteIngredient(ingredientId: Int) = dao.deleteIngredient(ingredientId)

    suspend fun toggleIngredient(ingredientId: Int, isChecked: Int) = dao.toggleIngredient(ingredientId, isChecked)

    // ------------------ MASTER ITEMS ------------------
    
    fun getMasterItems(): Flow<List<MasterItem>> = dao.getMasterItems()

    fun getMasterItemsWithCount(): Flow<List<MasterItemWithCount>> = dao.getMasterItemsWithCount()
    
    suspend fun getMasterItem(id: Int) = dao.getMasterItem(id)

    suspend fun addMasterItem(name: String, isContainer: Boolean, imagePath: String? = null) {
        dao.insertMasterItem(MasterItem(name = name, is_container = isContainer, image_path = imagePath))
    }

    suspend fun updateMasterItem(item: MasterItem) = dao.updateMasterItem(item)
    
    suspend fun deleteMasterItem(id: Int) = dao.deleteMasterItem(id)

    // --- MASTER SUB ITEMS ---

    fun getMasterSubItems(masterItemId: Int) = dao.getMasterSubItems(masterItemId)

    suspend fun insertMasterSubItem(item: MasterSubItem) = dao.insertMasterSubItem(item)

    suspend fun updateMasterSubItem(item: MasterSubItem) = dao.updateMasterSubItem(item)

    suspend fun deleteMasterSubItem(id: Int) = dao.deleteMasterSubItem(id)

    // --- MASTER SUB SUB ITEMS ---

    fun getMasterSubSubItems(subItemId: Int) = dao.getMasterSubSubItems(subItemId)

    suspend fun insertMasterSubSubItem(item: MasterSubSubItem) = dao.insertMasterSubSubItem(item)

    suspend fun updateMasterSubSubItem(item: MasterSubSubItem) = dao.updateMasterSubSubItem(item)

    suspend fun deleteMasterSubSubItem(id: Int) = dao.deleteMasterSubSubItem(id)

    suspend fun syncMasterPictures() {
        val masterItems = dao.getMasterItemsSyncList()
        val masterSubItems = dao.getAllMasterSubItemsSync()
        
        val allEntries = dao.getAllEntriesSync()
        allEntries.forEach { entry ->
            if (entry.image_path == null) {
                val match = masterItems.find { it.name.equals(entry.entry_name, ignoreCase = true) }
                if (match?.image_path != null) {
                    dao.updateEntry(entry.copy(image_path = match.image_path))
                }
            }
        }
        
        val allItems = dao.getAllItemsSync()
        allItems.forEach { item ->
            if (item.image_path == null) {
                // Check master items (maybe it was a top level item but now is a sub item)
                val match1 = masterItems.find { it.name.equals(item.item_name, ignoreCase = true) }
                if (match1?.image_path != null) {
                    dao.updateItem(item.copy(image_path = match1.image_path))
                    return@forEach
                }
                // Check master sub items
                val match2 = masterSubItems.find { it.name.equals(item.item_name, ignoreCase = true) }
                if (match2?.image_path != null) {
                    dao.updateItem(item.copy(image_path = match2.image_path))
                }
            }
        }
    }

    // ------------------ ITINERARY ------------------

    fun getItinerary(listId: Int): Flow<List<ItineraryItem>> = dao.getItinerary(listId)

    suspend fun getItinerarySync(listId: Int) = dao.getItinerarySync(listId)

    suspend fun getItineraryItem(id: Int) = dao.getItineraryItem(id)

    suspend fun addItineraryItem(
        listId: Int,
        day: String,
        time: String,
        activity: String,
        notes: String?,
        location: String?,
        price: Double?,
        departureDay: String?,
        departureTime: String?,
        category: String? = null,
        bookingRef: String? = null,
        showOnMap: Boolean = true
    ) {
        dao.insertItineraryItem(
            ItineraryItem(
                list_id = listId, 
                day = day, 
                time = time, 
                activity = activity, 
                notes = notes, 
                location = location, 
                price = price, 
                departure_day = departureDay, 
                departure_time = departureTime,
                category = category,
                booking_ref = bookingRef,
                show_on_map = showOnMap
            )
        )
    }

    suspend fun updateItineraryItem(item: ItineraryItem) {
        dao.updateItineraryItem(item.copy(last_updated = System.currentTimeMillis()))
    }

    suspend fun deleteItineraryItem(itemId: Int) = dao.deleteItineraryItem(itemId)

    /**
     * Imports a full trip data structure without affecting Master Inventory.
     */
    suspend fun importFullTripData(data: FullTripData) {
        // 1. Insert List
        val newListId = dao.insertList(data.list.copy(id = 0, created_at = getCurrentTimestamp())).toInt()

        // 2. Insert Itinerary
        data.itinerary.forEach { dao.insertItineraryItem(it.copy(id = 0, list_id = newListId)) }

        // 3. Insert Entries & Items
        data.entries.forEach { entry ->
            val oldEntryId = entry.entry_id
            val newEntryId = dao.insertEntry(entry.copy(entry_id = 0, list_id = newListId)).toInt()
            
            data.allItems.filter { it.entry_id == oldEntryId }.forEach { item ->
                dao.insertItem(item.copy(item_id = 0, entry_id = newEntryId))
            }
        }

        // 4. Insert Menu
        data.menu.forEach { meal ->
            dao.insertMenuItem(meal.copy(id = 0, list_id = newListId))
        }

        // 5. Insert Groups & Ingredients
        data.ingredientGroups.forEach { group ->
            val oldGroupId = group.id
            val newGroupId = dao.insertIngredientGroup(group.copy(id = 0, list_id = newListId)).toInt()

            data.allIngredients.filter { it.group_id == oldGroupId }.forEach { ing ->
                dao.insertIngredient(ing.copy(id = 0, group_id = newGroupId))
            }
        }
    }

    /**
     * Checks if a list already exists by Sync ID.
     */
    suspend fun getListBySyncId(syncId: String) = dao.getListBySyncId(syncId)

    /**
     * Performs a smart merge of trip data. 
     * Keeps local version if it is newer than the imported version.
     */
    suspend fun mergeTripData(importedData: FullTripData) {
        val existingList = dao.getListBySyncId(importedData.list.sync_id) ?: return
        val listId = existingList.id

        // 1. Update List properties if imported is newer
        if (importedData.list.last_updated > existingList.last_updated) {
            dao.updateList(importedData.list.copy(id = listId))
        }

        // 2. Merge Itinerary
        importedData.itinerary.forEach { imp ->
            val local = dao.getItineraryItemBySyncId(imp.sync_id, listId)
            if (local == null) {
                dao.insertItineraryItem(imp.copy(id = 0, list_id = listId))
            } else if (imp.last_updated > local.last_updated) {
                dao.updateItineraryItem(imp.copy(id = local.id, list_id = listId))
            }
        }

        // 3. Merge Entries & Nested Items
        importedData.entries.forEach { impEntry ->
            val localEntry = dao.getEntryBySyncId(impEntry.sync_id, listId)
            val entryIdToUse: Int
            
            if (localEntry == null) {
                entryIdToUse = dao.insertEntry(impEntry.copy(entry_id = 0, list_id = listId)).toInt()
            } else {
                entryIdToUse = localEntry.entry_id
                if (impEntry.last_updated > localEntry.last_updated) {
                    // Important: preserves check status of local user
                    dao.updateEntry(impEntry.copy(entry_id = entryIdToUse, list_id = listId, is_checked = localEntry.is_checked))
                }
            }

            // Merge Items inside this entry
            importedData.allItems.filter { it.entry_id == impEntry.entry_id }.forEach { impItem ->
                val localItem = dao.getItemBySyncId(impItem.sync_id, entryIdToUse)
                if (localItem == null) {
                    dao.insertItem(impItem.copy(item_id = 0, entry_id = entryIdToUse))
                } else if (impItem.last_updated > localItem.last_updated) {
                    dao.updateItem(impItem.copy(item_id = localItem.item_id, entry_id = entryIdToUse, is_checked = localItem.is_checked))
                }
            }
        }

        // 4. Merge Menu
        importedData.menu.forEach { impMeal ->
            val localMeal = dao.getMenuItemBySyncId(impMeal.sync_id, listId)
            if (localMeal == null) {
                dao.insertMenuItem(impMeal.copy(id = 0, list_id = listId))
            } else if (impMeal.last_updated > localMeal.last_updated) {
                dao.updateMenuItem(impMeal.copy(id = localMeal.id, list_id = listId))
            }
        }

        // 5. Merge Ingredients
        importedData.ingredientGroups.forEach { impGroup ->
            val localGroup = dao.getIngredientGroupBySyncId(impGroup.sync_id, listId)
            val groupIdToUse: Int
            
            if (localGroup == null) {
                groupIdToUse = dao.insertIngredientGroup(impGroup.copy(id = 0, list_id = listId)).toInt()
            } else {
                groupIdToUse = localGroup.id
                // Update group name if changed
                if (impGroup.group_name != localGroup.group_name) {
                    dao.updateIngredientGroup(impGroup.copy(id = groupIdToUse, list_id = listId))
                }
            }

            importedData.allIngredients.filter { it.group_id == impGroup.id }.forEach { impIng ->
                val localIng = dao.getIngredientBySyncId(impIng.sync_id, groupIdToUse)
                if (localIng == null) {
                    dao.insertIngredient(impIng.copy(id = 0, group_id = groupIdToUse))
                } else if (impIng.last_updated > localIng.last_updated) {
                    dao.updateIngredient(impIng.copy(id = localIng.id, group_id = groupIdToUse, is_checked = localIng.is_checked))
                }
            }
        }
    }
}
