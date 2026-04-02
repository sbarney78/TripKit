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

    suspend fun getListBySyncId(syncId: String) = dao.getListBySyncId(syncId)

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
                subItems.forEach { masterSub ->
                    val itemId = dao.insertItem(
                        Item(
                            entry_id = entryId,
                            item_name = masterSub.name,
                            quantity = masterSub.default_quantity,
                            notes = null,
                            is_checked = 0,
                            is_container = masterSub.is_container,
                            image_path = masterSub.image_path
                        )
                    ).toInt()
                    
                    if (masterSub.is_container) {
                        val subSubs = dao.getMasterSubSubItemsSync(masterSub.id)
                        subSubs.forEach { masterSubSub ->
                            dao.insertSubItem(
                                SubItem(
                                    item_id = itemId,
                                    name = masterSubSub.name,
                                    quantity = masterSubSub.default_quantity,
                                    notes = null,
                                    is_checked = 0,
                                    image_path = masterSubSub.image_path
                                )
                            )
                        }
                    }
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
                    val newItemId = dao.insertItem(
                        item.copy(
                            item_id = 0, 
                            entry_id = newEntryId, 
                            is_checked = 0, 
                            sync_id = UUID.randomUUID().toString(),
                            last_updated = System.currentTimeMillis()
                        )
                    ).toInt()
                    
                    if (item.is_container) {
                        val oldSubItems = dao.getSubItemsSync(item.item_id)
                        oldSubItems.forEach { subItem ->
                            dao.insertSubItem(
                                subItem.copy(
                                    id = 0,
                                    item_id = newItemId,
                                    is_checked = 0,
                                    sync_id = UUID.randomUUID().toString(),
                                    last_updated = System.currentTimeMillis()
                                )
                            )
                        }
                    }
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
        imagePath: String? = null,
        addToMaster: Boolean = false
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

        if (type == "container") {
            val masterItems = dao.getMasterItemsSyncList()
            val masterItem = masterItems.find { it.name.equals(name, ignoreCase = true) && it.is_container }
            if (masterItem != null) {
                val subItems = dao.getMasterSubItemsSync(masterItem.id)
                subItems.forEach { masterSub ->
                    val itemId = dao.insertItem(Item(
                        entry_id = entryId, 
                        item_name = masterSub.name, 
                        quantity = masterSub.default_quantity, 
                        notes = null, 
                        is_checked = 0, 
                        is_container = masterSub.is_container,
                        image_path = masterSub.image_path
                    )).toInt()

                    if (masterSub.is_container) {
                        val subSubs = dao.getMasterSubSubItemsSync(masterSub.id)
                        subSubs.forEach { masterSubSub ->
                            dao.insertSubItem(SubItem(
                                item_id = itemId,
                                name = masterSubSub.name,
                                quantity = masterSubSub.default_quantity,
                                notes = null,
                                is_checked = 0,
                                image_path = masterSubSub.image_path
                            ))
                        }
                    }
                }
            }
        }
        
        if (addToMaster) {
            val masterItems = dao.getMasterItemsSyncList()
            if (masterItems.none { it.name.equals(name, ignoreCase = true) }) {
                dao.insertMasterItem(MasterItem(name = name, is_container = type == "container", default_quantity = quantity, image_path = imagePath))
            }
        }
    }

    suspend fun toggleEntry(id: Int, checked: Int) {
        dao.toggleEntry(id, checked)
        val entry = dao.getEntry(id)
        if (entry?.entry_type == "container") {
            val items = dao.getItemsSync(id)
            items.forEach { toggleItem(it.item_id, checked) }
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

    fun getItemsWithCount(entryId: Int): Flow<List<ItemWithCount>> = dao.getItemsWithCount(entryId)

    fun getAllItemsForList(listId: Int): Flow<List<Item>> = dao.getAllItemsForList(listId)

    suspend fun getAllItemsForListSync(listId: Int) = dao.getAllItemsForListSync(listId)

    suspend fun getItem(itemId: Int): Item? = dao.getItem(itemId)

    suspend fun addItem(
        entryId: Int,
        name: String,
        quantity: Int,
        notes: String?,
        isContainer: Boolean,
        imagePath: String? = null,
        addToMaster: Boolean = true
    ) {
        val itemId = dao.insertItem(
            Item(
                entry_id = entryId,
                item_name = name,
                quantity = quantity,
                notes = notes,
                is_checked = 0,
                is_container = isContainer,
                image_path = imagePath
            )
        ).toInt()

        if (addToMaster) {
            val entry = dao.getEntry(entryId)
            var addedToMasterSub = false
            if (entry != null) {
                // Try to find parent container in Master Level 1
                val masterItem = dao.getMasterItemsSyncList().find { it.name.equals(entry.entry_name, ignoreCase = true) }
                if (masterItem != null) {
                    val existingSub = dao.getMasterSubItemsSync(masterItem.id).find { it.name.equals(name, ignoreCase = true) }
                    if (existingSub == null) {
                        dao.insertMasterSubItem(MasterSubItem(
                            master_item_id = masterItem.id,
                            name = name,
                            default_quantity = quantity,
                            is_container = isContainer,
                            image_path = imagePath
                        ))
                    }
                    addedToMasterSub = true
                }
            }
            
            if (!addedToMasterSub) {
                // Fallback: only add to top level IF it doesn't exist anywhere in Master level 1
                val masterItems = dao.getMasterItemsSyncList()
                if (masterItems.none { it.name.equals(name, ignoreCase = true) }) {
                    dao.insertMasterItem(MasterItem(name = name, is_container = isContainer, default_quantity = quantity, image_path = imagePath))
                }
            }
        }
    }

    suspend fun toggleItem(id: Int, checked: Int) {
        dao.toggleItem(id, checked)
        val item = dao.getItem(id)
        
        // 1. If it's a container, toggle all its sub-items
        if (item?.is_container == true) {
            val subItems = dao.getSubItemsSync(id)
            subItems.forEach { toggleSubItem(it.id, checked) }
        }
        
        // 2. Update the parent entry's status if necessary
        item?.let {
            val entryId = it.entry_id
            val allItems = dao.getItemsSync(entryId)
            val allChecked = allItems.all { it.is_checked == 1 }
            dao.toggleEntry(entryId, if (allChecked) 1 else 0)
        }
    }

    suspend fun deleteItem(id: Int) = dao.deleteItem(id)

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


    // ------------------ SUB ITEMS ------------------

    fun getSubItems(itemId: Int): Flow<List<SubItem>> = dao.getSubItems(itemId)

    suspend fun getSubItem(subItemId: Int): SubItem? = dao.getSubItem(subItemId)

    suspend fun addSubItem(
        itemId: Int,
        name: String,
        quantity: Int,
        notes: String?,
        imagePath: String? = null,
        addToMaster: Boolean = false
    ) {
        dao.insertSubItem(
            SubItem(
                item_id = itemId,
                name = name,
                quantity = quantity,
                notes = notes,
                is_checked = 0,
                image_path = imagePath
            )
        )

        if (addToMaster) {
            val item = dao.getItem(itemId)
            if (item != null) {
                // Find parent container in Master Sub-Items (Level 2)
                val masterSub = dao.getAllMasterSubItemsSync().find { it.name.equals(item.item_name, ignoreCase = true) }
                if (masterSub != null) {
                    val existingSubSub = dao.getMasterSubSubItemsSync(masterSub.id).find { it.name.equals(name, ignoreCase = true) }
                    if (existingSubSub == null) {
                        dao.insertMasterSubSubItem(MasterSubSubItem(
                            master_sub_item_id = masterSub.id,
                            name = name,
                            default_quantity = quantity,
                            image_path = imagePath
                        ))
                    }
                }
            }
        }
    }

    suspend fun toggleSubItem(id: Int, checked: Int) {
        dao.toggleSubItem(id, checked)
        
        // Update parent item's status if necessary
        val subItem = dao.getSubItem(id)
        subItem?.let {
            val itemId = it.item_id
            val allSubItems = dao.getSubItemsSync(itemId)
            val allChecked = allSubItems.all { it.is_checked == 1 }
            
            // This will recursively trigger the toggleItem logic to check parent entry
            toggleItem(itemId, if (allChecked) 1 else 0)
        }
    }

    suspend fun deleteSubItem(id: Int) = dao.deleteSubItem(id)

    suspend fun updateSubItem(subItem: SubItem) {
        dao.updateSubItem(subItem.copy(last_updated = System.currentTimeMillis()))
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

    suspend fun updateMenuItem(item: MenuItem) {
        dao.updateMenuItem(item.copy(last_updated = System.currentTimeMillis()))
    }

    suspend fun updateMenuItem(id: Int, day: String, mealType: String, description: String) {
        val existing = dao.getMenuItemSync(id)
        if (existing != null) {
            dao.updateMenuItem(existing.copy(day = day, meal_type = mealType, description = description, last_updated = System.currentTimeMillis()))
        }
    }

    suspend fun deleteMenuItem(itemId: Int) = dao.deleteMenuItem(itemId)


    // ------------------ INGREDIENT GROUPS ------------------

    fun getIngredientGroups(listId: Int): Flow<List<IngredientGroup>> = dao.getIngredientGroups(listId)

    suspend fun getIngredientGroupsSync(listId: Int) = dao.getIngredientGroupsSync(listId)

    suspend fun getIngredientGroup(groupId: Int): IngredientGroup? = dao.getIngredientGroup(groupId)

    suspend fun addIngredientGroup(listId: Int, name: String): Long {
        return dao.insertIngredientGroup(IngredientGroup(list_id = listId, group_name = name, created_at = getCurrentTimestamp()))
    }

    suspend fun updateIngredientGroup(groupId: Int, name: String) {
        val existing = dao.getIngredientGroup(groupId)
        if (existing != null) {
            dao.updateIngredientGroup(existing.copy(group_name = name, last_updated = System.currentTimeMillis()))
        }
    }

    suspend fun deleteIngredientGroup(groupId: Int) = dao.deleteIngredientGroup(groupId)


    // ------------------ INGREDIENTS ------------------

    fun getIngredients(groupId: Int): Flow<List<Ingredient>> = dao.getIngredients(groupId)

    fun getAllIngredientsForList(listId: Int): Flow<List<Ingredient>> = dao.getAllIngredientsForList(listId)

    suspend fun getAllIngredientsForListSync(listId: Int) = dao.getAllIngredientsForListSync(listId)

    suspend fun addIngredient(groupId: Int, name: String, quantity: Int = 1, notes: String? = null) {
        dao.insertIngredient(
            Ingredient(
                group_id = groupId,
                ingredient_name = name,
                quantity = quantity,
                notes = notes,
                is_checked = 0,
                created_at = getCurrentTimestamp()
            )
        )
    }

    suspend fun updateIngredient(ingredient: Ingredient) {
        dao.updateIngredient(ingredient.copy(last_updated = System.currentTimeMillis()))
    }

    suspend fun updateIngredient(id: Int, name: String, quantity: Int, notes: String?, isChecked: Int) {
        val existing = dao.getIngredient(id)
        if (existing != null) {
            dao.updateIngredient(existing.copy(
                ingredient_name = name, 
                quantity = quantity, 
                notes = notes, 
                is_checked = isChecked, 
                last_updated = System.currentTimeMillis()
            ))
        }
    }

    suspend fun deleteIngredient(ingredientId: Int) = dao.deleteIngredient(ingredientId)

    suspend fun toggleIngredient(id: Int, checked: Int) = dao.toggleIngredient(id, checked)


    // ------------------ ITINERARY ------------------

    fun getItinerary(listId: Int): Flow<List<ItineraryItem>> = dao.getItinerary(listId)

    suspend fun getItinerarySync(listId: Int) = dao.getItinerarySync(listId)

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


    // ------------------ MASTER ITEMS ------------------

    fun getMasterItems(): Flow<List<MasterItem>> = dao.getMasterItems()

    fun getMasterItemsWithCount(): Flow<List<MasterItemWithCount>> = dao.getMasterItemsWithCount()

    suspend fun addMasterItem(name: String, isContainer: Boolean, imagePath: String? = null) {
        dao.insertMasterItem(MasterItem(name = name, is_container = isContainer, image_path = imagePath))
    }

    suspend fun updateMasterItem(item: MasterItem) {
        dao.updateMasterItem(item.copy(last_updated = System.currentTimeMillis()))
    }

    suspend fun deleteMasterItem(id: Int) = dao.deleteMasterItem(id)

    fun getMasterSubItems(masterItemId: Int): Flow<List<MasterSubItem>> = dao.getMasterSubItems(masterItemId)

    fun getMasterSubItemsWithCount(masterItemId: Int): Flow<List<MasterSubItemWithCount>> = dao.getMasterSubItemsWithCount(masterItemId)

    suspend fun insertMasterSubItem(item: MasterSubItem) {
        dao.insertMasterSubItem(item)
    }

    suspend fun updateMasterSubItem(item: MasterSubItem) {
        dao.updateMasterSubItem(item.copy(last_updated = System.currentTimeMillis()))
    }

    suspend fun deleteMasterSubItem(id: Int) = dao.deleteMasterSubItem(id)

    fun getMasterSubSubItems(subItemId: Int): Flow<List<MasterSubSubItem>> = dao.getMasterSubSubItems(subItemId)

    suspend fun insertMasterSubSubItem(item: MasterSubSubItem) {
        dao.insertMasterSubSubItem(item)
    }

    suspend fun updateMasterSubSubItem(item: MasterSubSubItem) {
        dao.updateMasterSubSubItem(item.copy(last_updated = System.currentTimeMillis()))
    }

    suspend fun deleteMasterSubSubItem(id: Int) = dao.deleteMasterSubSubItem(id)

    suspend fun syncMasterPictures() {
        val allEntries = dao.getAllEntriesSync()
        val allItems = dao.getAllItemsSync()
        val allSubItems = dao.getAllSubItemsSync()
        
        // Sync Master Items -> Entries
        val masterItems = dao.getMasterItemsSyncList()
        masterItems.filter { it.image_path != null }.forEach { master ->
            allEntries.filter { it.entry_name.equals(master.name, ignoreCase = true) && it.image_path == null }.forEach { entry ->
                dao.updateEntry(entry.copy(image_path = master.image_path))
            }
        }
        
        // Sync Master Sub Items -> Items
        val masterSubItems = dao.getAllMasterSubItemsSync()
        masterSubItems.filter { it.image_path != null }.forEach { masterSub ->
            allItems.filter { it.item_name.equals(masterSub.name, ignoreCase = true) && it.image_path == null }.forEach { item ->
                dao.updateItem(item.copy(image_path = masterSub.image_path))
            }
        }

        // Sync Master Sub Sub Items -> Sub Items
        val masterSubSubItems = dao.getAllMasterSubSubItemsSync()
        masterSubSubItems.filter { it.image_path != null }.forEach { masterSubSub ->
            allSubItems.filter { it.name.equals(masterSubSub.name, ignoreCase = true) && it.image_path == null }.forEach { subItem ->
                dao.updateSubItem(subItem.copy(image_path = masterSubSub.image_path))
            }
        }
    }


    // ------------------ SYNC / MERGE ------------------

    suspend fun importFullTripData(data: FullTripData) {
        val newListId = dao.insertList(data.list.copy(id = 0, created_at = getCurrentTimestamp())).toInt()

        data.itinerary.forEach { dao.insertItineraryItem(it.copy(id = 0, list_id = newListId)) }

        data.entries.forEach { entry ->
            val oldEntryId = entry.entry_id
            val newEntryId = dao.insertEntry(entry.copy(entry_id = 0, list_id = newListId)).toInt()
            
            data.allItems.filter { it.entry_id == oldEntryId }.forEach { item ->
                dao.insertItem(item.copy(item_id = 0, entry_id = newEntryId))
            }
        }

        data.menu.forEach { meal ->
            dao.insertMenuItem(meal.copy(id = 0, list_id = newListId))
        }

        data.ingredientGroups.forEach { group ->
            val oldGroupId = group.id
            val newGroupId = dao.insertIngredientGroup(group.copy(id = 0, list_id = newListId)).toInt()

            data.allIngredients.filter { it.group_id == oldGroupId }.forEach { ing ->
                dao.insertIngredient(ing.copy(id = 0, group_id = newGroupId))
            }
        }
    }

    suspend fun mergeTripData(importedData: FullTripData) {
        val existingList = dao.getListBySyncId(importedData.list.sync_id) ?: return
        val listId = existingList.id

        if (importedData.list.last_updated > existingList.last_updated) {
            dao.updateList(importedData.list.copy(id = listId))
        }

        importedData.itinerary.forEach { imp ->
            val local = dao.getItineraryItemBySyncId(imp.sync_id, listId)
            if (local == null) {
                dao.insertItineraryItem(imp.copy(id = 0, list_id = listId))
            } else if (imp.last_updated > local.last_updated) {
                dao.updateItineraryItem(imp.copy(id = local.id, list_id = listId))
            }
        }

        importedData.entries.forEach { impEntry ->
            val localEntry = dao.getEntryBySyncId(impEntry.sync_id, listId)
            val entryIdToUse: Int
            
            if (localEntry == null) {
                entryIdToUse = dao.insertEntry(impEntry.copy(entry_id = 0, list_id = listId)).toInt()
            } else {
                entryIdToUse = localEntry.entry_id
                if (impEntry.last_updated > localEntry.last_updated) {
                    dao.updateEntry(impEntry.copy(entry_id = entryIdToUse, list_id = listId, is_checked = localEntry.is_checked))
                }
            }

            importedData.allItems.filter { it.entry_id == impEntry.entry_id }.forEach { impItem ->
                val localItem = dao.getItemBySyncId(impItem.sync_id, entryIdToUse)
                if (localItem == null) {
                    dao.insertItem(impItem.copy(item_id = 0, entry_id = entryIdToUse))
                } else if (impItem.last_updated > localItem.last_updated) {
                    dao.updateItem(impItem.copy(item_id = localItem.item_id, entry_id = entryIdToUse, is_checked = localItem.is_checked))
                }
            }
        }

        importedData.menu.forEach { impMeal ->
            val localMeal = dao.getMenuItemBySyncId(impMeal.sync_id, listId)
            if (localMeal == null) {
                dao.insertMenuItem(impMeal.copy(id = 0, list_id = listId))
            } else if (impMeal.last_updated > localMeal.last_updated) {
                dao.updateMenuItem(impMeal.copy(id = localMeal.id, list_id = listId))
            }
        }

        importedData.ingredientGroups.forEach { impGroup ->
            val localGroup = dao.getIngredientGroupBySyncId(impGroup.sync_id, listId)
            val groupIdToUse: Int
            
            if (localGroup == null) {
                groupIdToUse = dao.insertIngredientGroup(impGroup.copy(id = 0, list_id = listId)).toInt()
            } else {
                groupIdToUse = localGroup.id
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
