package au.barney.tripkit.data.repository

import au.barney.tripkit.data.local.TripKitDao
import au.barney.tripkit.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
        showItinerary: Boolean,
        templateId: Int? = null
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
            if (templateId != null && templateId > 0) {
                populateFromTemplate(listId.toInt(), templateId)
            } else {
                populateFromMaster(listId.toInt())
            }
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
                    image_path = masterItem.image_path,
                    color = masterItem.color,
                    weightGrams = masterItem.weightGrams
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
                            image_path = masterSub.image_path,
                            color = masterSub.color,
                            weightGrams = masterSub.weightGrams
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
                                    image_path = masterSubSub.image_path,
                                    color = masterSubSub.color,
                                    weightGrams = masterSubSub.weightGrams
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun populateFromTemplate(listId: Int, templateId: Int) {
        val entries = dao.getTemplateEntriesSync(templateId)
        entries.forEach { tEntry ->
            val entryId = dao.insertEntry(
                Entry(
                    entry_name = tEntry.name,
                    entry_type = if (tEntry.is_container) "container" else "single",
                    quantity = tEntry.default_quantity,
                    notes = null,
                    list_id = listId,
                    is_checked = 0,
                    image_path = tEntry.image_path,
                    color = tEntry.color,
                    weightGrams = tEntry.weightGrams
                )
            ).toInt()

            if (tEntry.is_container) {
                val items = dao.getTemplateItemsSync(tEntry.id)
                items.forEach { tItem ->
                    val itemId = dao.insertItem(
                        Item(
                            entry_id = entryId,
                            item_name = tItem.name,
                            quantity = tItem.default_quantity,
                            notes = null,
                            is_checked = 0,
                            is_container = tItem.is_container,
                            image_path = tItem.image_path,
                            color = tItem.color,
                            weightGrams = tItem.weightGrams
                        )
                    ).toInt()

                    if (tItem.is_container) {
                        val subItems = dao.getTemplateSubItemsSync(tItem.id)
                        subItems.forEach { tSub ->
                            dao.insertSubItem(
                                SubItem(
                                    item_id = itemId,
                                    name = tSub.name,
                                    quantity = tSub.default_quantity,
                                    notes = null,
                                    is_checked = 0,
                                    image_path = tSub.image_path,
                                    color = tSub.color,
                                    weightGrams = tSub.weightGrams
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

    suspend fun getEntry(entryId: Int): Entry? = dao.getEntry(entryId)

    suspend fun addEntry(
        name: String,
        type: String,
        quantity: Int,
        notes: String?,
        listId: Int,
        imagePath: String? = null,
        addToMaster: Boolean = false,
        color: String = "#800000",
        weightGrams: Int = 0,
        payloadLocationId: Int? = null
    ) {
        val entryId = dao.insertEntry(
            Entry(
                entry_name = name,
                entry_type = type,
                quantity = quantity,
                notes = notes,
                list_id = listId,
                is_checked = 0,
                image_path = imagePath,
                color = color,
                weightGrams = weightGrams,
                payloadLocationId = payloadLocationId
            )
        ).toInt()

        if (type == "container") {
            val masterItems = dao.getMasterItemsSyncList()
            val masterItem = masterItems.find { it.name.equals(name, ignoreCase = true) && it.is_container }
            if (masterItem != null) {
                val subItems = dao.getMasterSubItemsSync(masterItem.id)
                subItems.forEach { masterSub ->
                    dao.insertItem(
                        Item(
                            entry_id = entryId,
                            item_name = masterSub.name,
                            quantity = masterSub.default_quantity,
                            notes = null,
                            is_checked = 0,
                            is_container = masterSub.is_container,
                            image_path = masterSub.image_path,
                            color = masterSub.color,
                            weightGrams = masterSub.weightGrams
                        )
                    )
                }
            }
        }

        if (addToMaster) {
            dao.insertMasterItem(
                MasterItem(
                    name = name,
                    is_container = type == "container",
                    image_path = imagePath,
                    color = color,
                    weightGrams = weightGrams
                )
            )
        }
    }

    suspend fun updateEntry(entry: Entry) = dao.updateEntry(entry.copy(last_updated = System.currentTimeMillis()))
    suspend fun updateEntryColorByName(name: String, color: String) = dao.updateEntryColorByName(name, color)
    suspend fun updateEntryWeightByName(name: String, weight: Int) = dao.updateEntryWeightByName(name, weight)

    suspend fun deleteEntry(entryId: Int) = dao.deleteEntry(entryId)

    suspend fun toggleEntry(id: Int, checked: Int) {
        dao.toggleEntry(id, checked)
        if (checked == 0) {
            // If entry is unchecked, uncheck all items and sub-items inside it
            val items = dao.getItemsSync(id)
            items.forEach { item ->
                toggleItem(item.item_id, 0)
            }
        }
    }

    // ------------------ ITEMS ------------------

    fun getItems(entryId: Int): Flow<List<Item>> = dao.getItems(entryId)

    fun getItemsWithCount(entryId: Int): Flow<List<ItemWithCount>> = dao.getItemsWithCount(entryId)

    suspend fun getItemsSync(entryId: Int) = dao.getItemsSync(entryId)

    fun getAllItemsForList(listId: Int): Flow<List<Item>> = dao.getAllItemsForList(listId)

    suspend fun getAllItemsForListSync(listId: Int): List<Item> = dao.getAllItemsForListSync(listId)

    suspend fun getItem(itemId: Int): Item? = dao.getItem(itemId)

    suspend fun addItem(
        name: String,
        quantity: Int,
        notes: String?,
        entryId: Int,
        isContainer: Boolean,
        imagePath: String? = null,
        addToMaster: Boolean = false,
        color: String = "#800000",
        weightGrams: Int = 0,
        payloadLocationId: Int? = null
    ) {
        val itemId = dao.insertItem(
            Item(
                entry_id = entryId,
                item_name = name,
                quantity = quantity,
                notes = notes,
                is_checked = 0,
                is_container = isContainer,
                image_path = imagePath,
                color = color,
                weightGrams = weightGrams,
                payloadLocationId = payloadLocationId
            )
        ).toInt()

        if (isContainer) {
            val masterSubs = dao.getAllMasterSubItemsSync()
            val masterSub = masterSubs.find { it.name.equals(name, ignoreCase = true) && it.is_container }
            if (masterSub != null) {
                val masterSubSubs = dao.getMasterSubSubItemsSync(masterSub.id)
                masterSubSubs.forEach { mss ->
                    dao.insertSubItem(
                        SubItem(
                            item_id = itemId,
                            name = mss.name,
                            quantity = mss.default_quantity,
                            notes = null,
                            is_checked = 0,
                            image_path = mss.image_path,
                            color = mss.color,
                            weightGrams = mss.weightGrams
                        )
                    )
                }
            }
        }

        if (addToMaster) {
            // Find parent master item by entry name
            val entry = dao.getEntry(entryId)
            if (entry != null) {
                val masterItems = dao.getMasterItemsSyncList()
                val parentMaster = masterItems.find { it.name.equals(entry.entry_name, ignoreCase = true) }
                if (parentMaster != null) {
                    dao.insertMasterSubItem(
                        MasterSubItem(
                            master_item_id = parentMaster.id,
                            name = name,
                            is_container = isContainer,
                            image_path = imagePath,
                            color = color,
                            weightGrams = weightGrams
                        )
                    )
                }
            }
        }
    }

    suspend fun updateItem(item: Item) = dao.updateItem(item.copy(last_updated = System.currentTimeMillis()))
    suspend fun updateItemColorByName(name: String, color: String) = dao.updateItemColorByName(name, color)
    suspend fun updateItemWeightByName(name: String, weight: Int) = dao.updateItemWeightByName(name, weight)

    suspend fun deleteItem(itemId: Int) = dao.deleteItem(itemId)

    suspend fun toggleItem(id: Int, checked: Int) {
        dao.toggleItem(id, checked)
        if (checked == 1) {
            val item = dao.getItem(id)
            if (item != null) {
                val entry = dao.getEntry(item.entry_id)
                if (entry?.is_checked == 0) {
                    toggleEntry(item.entry_id, 1)
                }
            }
        } else {
            // Uncheck all sub-items
            val subItems = dao.getSubItemsSync(id)
            subItems.forEach { sub ->
                toggleSubItem(sub.id, 0)
            }
        }
    }

    // ------------------ SUB ITEMS ------------------

    fun getSubItems(itemId: Int): Flow<List<SubItem>> = dao.getSubItems(itemId)

    fun getAllSubItemsForList(listId: Int): Flow<List<SubItem>> = dao.getAllSubItemsForList(listId)

    suspend fun getAllSubItemsForListSync(listId: Int): List<SubItem> = dao.getAllSubItemsForListSync(listId)

    suspend fun addSubItem(
        name: String,
        quantity: Int,
        notes: String?,
        itemId: Int,
        imagePath: String? = null,
        addToMaster: Boolean = false,
        color: String = "#800000",
        weightGrams: Int = 0,
        payloadLocationId: Int? = null
    ) {
        dao.insertSubItem(
            SubItem(
                item_id = itemId,
                name = name,
                quantity = quantity,
                notes = notes,
                is_checked = 0,
                image_path = imagePath,
                color = color,
                weightGrams = weightGrams,
                payloadLocationId = payloadLocationId
            )
        )

        if (addToMaster) {
            val item = dao.getItem(itemId)
            if (item != null) {
                val masterSubs = dao.getAllMasterSubItemsSync()
                val parentMasterSub = masterSubs.find { it.name.equals(item.item_name, ignoreCase = true) }
                if (parentMasterSub != null) {
                    dao.insertMasterSubSubItem(
                        MasterSubSubItem(
                            master_sub_item_id = parentMasterSub.id,
                            name = name,
                            image_path = imagePath,
                            color = color,
                            weightGrams = weightGrams
                        )
                    )
                }
            }
        }
    }

    suspend fun updateSubItem(subItem: SubItem) = dao.updateSubItem(subItem.copy(last_updated = System.currentTimeMillis()))
    suspend fun updateSubItemColorByName(name: String, color: String) = dao.updateSubItemColorByName(name, color)
    suspend fun updateSubItemWeightByName(name: String, weight: Int) = dao.updateSubItemWeightByName(name, weight)

    suspend fun deleteSubItem(id: Int) = dao.deleteSubItem(id)

    suspend fun toggleSubItem(id: Int, checked: Int) {
        dao.toggleSubItem(id, checked)
        if (checked == 1) {
            val sub = dao.getSubItem(id)
            if (sub != null) {
                val item = dao.getItem(sub.item_id)
                if (item?.is_checked == 0) {
                    toggleItem(sub.item_id, 1)
                }
            }
        }
    }

    // ------------------ MENU ------------------

    fun getMenu(listId: Int): Flow<List<MenuItem>> = dao.getMenu(listId)

    suspend fun getMenuSync(listId: Int): List<MenuItem> = dao.getMenuSync(listId)

    suspend fun addMenuItem(listId: Int, day: String, mealType: String, description: String) {
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

    suspend fun updateMenuItem(menuItem: MenuItem) {
        dao.updateMenuItem(menuItem.copy(last_updated = System.currentTimeMillis()))
    }

    suspend fun updateMenuItem(menuId: Int, day: String, mealType: String, description: String) {
        val existing = dao.getMenuItemSync(menuId) ?: return
        dao.updateMenuItem(
            existing.copy(
                day = day,
                meal_type = mealType,
                description = description,
                last_updated = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMenuItem(menuId: Int) = dao.deleteMenuItem(menuId)

    // ------------------ INGREDIENTS ------------------

    fun getIngredientGroups(listId: Int): Flow<List<IngredientGroup>> = dao.getIngredientGroups(listId)

    suspend fun getIngredientGroupsSync(listId: Int): List<IngredientGroup> = dao.getIngredientGroupsSync(listId)

    suspend fun getIngredientGroup(groupId: Int): IngredientGroup? = dao.getIngredientGroup(groupId)

    suspend fun addIngredientGroup(listId: Int, name: String) = dao.insertIngredientGroup(
        IngredientGroup(
            list_id = listId,
            group_name = name,
            created_at = getCurrentTimestamp()
        )
    )

    suspend fun updateIngredientGroup(group: IngredientGroup) = dao.updateIngredientGroup(group)

    suspend fun deleteIngredientGroup(groupId: Int) = dao.deleteIngredientGroup(groupId)

    fun getIngredients(groupId: Int): Flow<List<Ingredient>> = dao.getIngredients(groupId)

    fun getAllIngredientsForList(listId: Int): Flow<List<Ingredient>> = dao.getAllIngredientsForList(listId)

    suspend fun getAllIngredientsForListSync(listId: Int): List<Ingredient> = dao.getAllIngredientsForListSync(listId)

    suspend fun addIngredient(groupId: Int, name: String, quantity: String, notes: String?) {
        dao.insertIngredient(
            Ingredient(
                group_id = groupId,
                ingredient_name = name,
                quantity = quantity.toIntOrNull() ?: 1,
                notes = notes,
                is_checked = 0,
                created_at = getCurrentTimestamp()
            )
        )
    }

    suspend fun updateIngredient(ingredient: Ingredient) {
        dao.updateIngredient(ingredient.copy(last_updated = System.currentTimeMillis()))
    }

    suspend fun deleteIngredient(ingredientId: Int) = dao.deleteIngredient(ingredientId)

    suspend fun toggleIngredient(id: Int, checked: Int) = dao.toggleIngredient(id, checked)

    // ------------------ MASTER ------------------

    fun getMasterItems(): Flow<List<MasterItem>> = dao.getMasterItems()

    fun getMasterItemsWithCount(): Flow<List<MasterItemWithCount>> = dao.getMasterItemsWithCount()

    suspend fun getMasterItemsSync() = dao.getMasterItemsSyncList()

    suspend fun addMasterItem(name: String, isContainer: Boolean, weightGrams: Int = 0, imagePath: String? = null, color: String = "#800000") {
        dao.insertMasterItem(MasterItem(name = name, is_container = isContainer, weightGrams = weightGrams, image_path = imagePath, color = color))
    }

    suspend fun updateMasterItem(item: MasterItem) {
        dao.updateMasterItem(item)
        syncAllPicturesFromMaster()
    }

    suspend fun deleteMasterItem(id: Int) = dao.deleteMasterItem(id)

    fun getMasterSubItems(masterItemId: Int): Flow<List<MasterSubItem>> = dao.getMasterSubItems(masterItemId)

    fun getMasterSubItemsWithCount(masterItemId: Int): Flow<List<MasterSubItemWithCount>> = dao.getMasterSubItemsWithCount(masterItemId)

    suspend fun addMasterSubItem(masterItemId: Int, name: String, isContainer: Boolean, weightGrams: Int = 0, imagePath: String? = null, color: String = "#800000") {
        dao.insertMasterSubItem(MasterSubItem(master_item_id = masterItemId, name = name, is_container = isContainer, weightGrams = weightGrams, image_path = imagePath, color = color))
    }

    suspend fun updateMasterSubItem(item: MasterSubItem) {
        dao.updateMasterSubItem(item)
        syncAllPicturesFromMaster()
    }

    suspend fun deleteMasterSubItem(id: Int) = dao.deleteMasterSubItem(id)

    fun getMasterSubSubItems(subItemId: Int): Flow<List<MasterSubSubItem>> = dao.getMasterSubSubItems(subItemId)

    suspend fun addMasterSubSubItem(subItemId: Int, name: String, weightGrams: Int = 0, imagePath: String? = null, color: String = "#800000") {
        dao.insertMasterSubSubItem(MasterSubSubItem(master_sub_item_id = subItemId, name = name, weightGrams = weightGrams, image_path = imagePath, color = color))
    }

    suspend fun updateMasterSubSubItem(item: MasterSubSubItem) {
        dao.updateMasterSubSubItem(item)
        syncAllPicturesFromMaster()
    }

    suspend fun deleteMasterSubSubItem(id: Int) = dao.deleteMasterSubSubItem(id)

    // ------------------ ITINERARY ------------------

    fun getItinerary(listId: Int): Flow<List<ItineraryItem>> = dao.getItinerary(listId)

    suspend fun getItinerarySync(listId: Int): List<ItineraryItem> = dao.getItinerarySync(listId)

    suspend fun addItineraryItem(
        listId: Int,
        day: String,
        time: String,
        activity: String,
        notes: String?,
        location: String?,
        price: Double? = null,
        departureDay: String? = null,
        departureTime: String? = null,
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

    suspend fun deleteItineraryItem(id: Int) = dao.deleteItineraryItem(id)

    // ------------------ TEMPLATES ------------------

    fun getTemplates(): Flow<List<Template>> = dao.getTemplates()
    suspend fun getTemplatesSyncList(): List<Template> = dao.getTemplatesSyncList()
    suspend fun getTemplate(id: Int) = dao.getTemplate(id)
    suspend fun addTemplate(name: String) = dao.insertTemplate(Template(name = name))
    suspend fun updateTemplate(template: Template) = dao.updateTemplate(template)
    suspend fun deleteTemplate(id: Int) = dao.deleteTemplate(id)

    fun getTemplateEntries(templateId: Int) = dao.getTemplateEntries(templateId)
    suspend fun getTemplateEntriesSyncList(templateId: Int): List<TemplateEntry> = dao.getTemplateEntriesSyncList(templateId)
    suspend fun addTemplateEntry(templateId: Int, name: String, isContainer: Boolean, weightGrams: Int = 0, color: String = "#800000") {
        dao.insertTemplateEntry(TemplateEntry(template_id = templateId, name = name, is_container = isContainer, weightGrams = weightGrams, color = color))
    }
    
    suspend fun addTemplateEntry(templateId: Int, masterItemId: Int) {
        val masterItem = dao.getMasterItem(masterItemId) ?: return
        val entryId = dao.insertTemplateEntry(
            TemplateEntry(
                template_id = templateId,
                name = masterItem.name,
                is_container = masterItem.is_container,
                weightGrams = masterItem.weightGrams,
                color = masterItem.color,
                image_path = masterItem.image_path
            )
        ).toInt()

        if (masterItem.is_container) {
            val subItems = dao.getMasterSubItemsSync(masterItemId)
            subItems.forEach { sub ->
                val itemId = dao.insertTemplateItem(
                    TemplateItem(
                        template_entry_id = entryId,
                        name = sub.name,
                        is_container = sub.is_container,
                        weightGrams = sub.weightGrams,
                        color = sub.color,
                        image_path = sub.image_path
                    )
                ).toInt()

                if (sub.is_container) {
                    val subSubs = dao.getMasterSubSubItemsSync(sub.id)
                    subSubs.forEach { ss ->
                        dao.insertTemplateSubItem(
                            TemplateSubItem(
                                template_item_id = itemId,
                                name = ss.name,
                                weightGrams = ss.weightGrams,
                                color = ss.color,
                                image_path = ss.image_path
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun updateTemplateEntry(entry: TemplateEntry) = dao.updateTemplateEntry(entry)
    suspend fun updateTemplateEntryColorByName(name: String, color: String) = dao.updateTemplateEntryColorByName(name, color)
    suspend fun updateTemplateEntryWeightByName(name: String, weight: Int) = dao.updateTemplateEntryWeightByName(name, weight)
    suspend fun deleteTemplateEntry(id: Int) = dao.deleteTemplateEntry(id)
    suspend fun toggleTemplateEntry(id: Int, checked: Int) {
        dao.updateTemplateEntryChecked(id, checked)
        if (checked == 0) {
            dao.updateAllItemsInEntryChecked(id, 0)
            dao.updateAllSubItemsInEntryChecked(id, 0)
        }
    }

    fun getTemplateItems(entryId: Int) = dao.getTemplateItems(entryId)
    suspend fun getTemplateItemsSync(entryId: Int): List<TemplateItem> = dao.getTemplateItemsSync(entryId)
    suspend fun addTemplateItem(entryId: Int, name: String, isContainer: Boolean, weightGrams: Int = 0, color: String = "#800000") {
        dao.insertTemplateItem(TemplateItem(template_entry_id = entryId, name = name, is_container = isContainer, weightGrams = weightGrams, color = color))
    }
    suspend fun updateTemplateItem(item: TemplateItem) = dao.updateTemplateItem(item)
    suspend fun updateTemplateItemColorByName(name: String, color: String) = dao.updateTemplateItemColorByName(name, color)
    suspend fun updateTemplateItemWeightByName(name: String, weight: Int) = dao.updateTemplateItemWeightByName(name, weight)
    suspend fun deleteTemplateItem(id: Int) = dao.deleteTemplateItem(id)
    suspend fun toggleTemplateItem(id: Int, checked: Int) {
        dao.updateTemplateItemChecked(id, checked)
        if (checked == 0) {
            dao.updateAllSubItemsInItemChecked(id, 0)
        } else {
            val item = dao.getTemplateItemSync(id)
            if (item != null) {
                val entry = dao.getTemplateEntrySync(item.template_entry_id)
                if (entry?.is_checked == 0) {
                    dao.updateTemplateEntryChecked(item.template_entry_id, 1)
                }
            }
        }
    }

    fun getTemplateSubItems(itemId: Int) = dao.getTemplateSubItems(itemId)
    suspend fun getTemplateSubItemsSync(itemId: Int): List<TemplateSubItem> = dao.getTemplateSubItemsSync(itemId)
    suspend fun addTemplateSubItem(itemId: Int, name: String, weightGrams: Int = 0, color: String = "#800000") {
        dao.insertTemplateSubItem(TemplateSubItem(template_item_id = itemId, name = name, weightGrams = weightGrams, color = color))
    }
    suspend fun updateTemplateSubItem(subItem: TemplateSubItem) = dao.updateTemplateSubItem(subItem)
    suspend fun updateTemplateSubItemColorByName(name: String, color: String) = dao.updateTemplateSubItemColorByName(name, color)
    suspend fun updateTemplateSubItemWeightByName(name: String, weight: Int) = dao.updateTemplateSubItemWeightByName(name, weight)
    suspend fun deleteTemplateSubItem(id: Int) = dao.deleteTemplateSubItem(id)
    suspend fun toggleTemplateSubItem(id: Int, checked: Int) {
        dao.updateTemplateSubItemChecked(id, checked)
        if (checked == 1) {
            val subItem = dao.getTemplateSubItemSync(id) ?: return
            val item = dao.getTemplateItemSync(subItem.template_item_id)
            if (item?.is_checked == 0) {
                toggleTemplateItem(subItem.template_item_id, 1)
            }
        } else {
            // Only uncheck parent item if NOT all sub-items are checked
            // But we need to be careful not to uncheck it if it's already 0
            val subItem = dao.getTemplateSubItemSync(id) ?: return
            val parentItem = dao.getTemplateItemSync(subItem.template_item_id)
            if (parentItem?.is_checked == 1) {
                toggleTemplateItem(subItem.template_item_id, 0)
            }
        }
    }

    suspend fun createTemplateFromSelection(name: String, masterItemIds: List<Int>) {
        val templateId = dao.insertTemplate(Template(name = name)).toInt()
        masterItemIds.forEach { masterId ->
            val masterItem = dao.getMasterItem(masterId) ?: return@forEach
            val entryId = dao.insertTemplateEntry(
                TemplateEntry(
                    template_id = templateId,
                    name = masterItem.name,
                    is_container = masterItem.is_container,
                    weightGrams = masterItem.weightGrams,
                    color = masterItem.color,
                    image_path = masterItem.image_path
                )
            ).toInt()

            if (masterItem.is_container) {
                val subItems = dao.getMasterSubItemsSync(masterId)
                subItems.forEach { sub ->
                    val itemId = dao.insertTemplateItem(
                        TemplateItem(
                            template_entry_id = entryId,
                            name = sub.name,
                            is_container = sub.is_container,
                            weightGrams = sub.weightGrams,
                            color = sub.color,
                            image_path = sub.image_path
                        )
                    ).toInt()

                    if (sub.is_container) {
                        val subSubs = dao.getMasterSubSubItemsSync(sub.id)
                        subSubs.forEach { ss ->
                            dao.insertTemplateSubItem(
                                TemplateSubItem(
                                    template_item_id = itemId,
                                    name = ss.name,
                                    weightGrams = ss.weightGrams,
                                    color = ss.color,
                                    image_path = ss.image_path
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun importFullTripData(data: FullTripData) {
        val newListId = dao.insertList(
            data.list.copy(
                id = 0,
                sync_id = UUID.randomUUID().toString(),
                created_at = getCurrentTimestamp(),
                last_updated = System.currentTimeMillis()
            )
        ).toInt()

        data.itinerary.forEach { item ->
            dao.insertItineraryItem(item.copy(id = 0, list_id = newListId, sync_id = UUID.randomUUID().toString(), last_updated = System.currentTimeMillis()))
        }

        data.menu.forEach { meal ->
            dao.insertMenuItem(meal.copy(id = 0, list_id = newListId, sync_id = UUID.randomUUID().toString(), last_updated = System.currentTimeMillis()))
        }

        val groupMap = mutableMapOf<Int, Int>() // old group id -> new group id
        data.ingredientGroups.forEach { group ->
            val newGroupId = dao.insertIngredientGroup(
                group.copy(
                    id = 0, 
                    list_id = newListId, 
                    sync_id = UUID.randomUUID().toString()
                )
            ).toInt()
            groupMap[group.id] = newGroupId
        }

        data.allIngredients.forEach { ing ->
            val newGroupId = groupMap[ing.group_id] ?: return@forEach
            dao.insertIngredient(
                ing.copy(
                    id = 0, 
                    group_id = newGroupId,
                    sync_id = UUID.randomUUID().toString(),
                    last_updated = System.currentTimeMillis()
                )
            )
        }

        // Logic for entries, items and sub items would go here too if fully implementing import
    }

    suspend fun mergeTripData(data: FullTripData) {
        // Implement complex merge logic here if needed
    }

    suspend fun syncAllPicturesFromMaster() = coroutineScope {
        val masterEntries = dao.getMasterItemsSyncList()
        val masterItems = dao.getAllMasterSubItemsSync()
        val masterSubItems = dao.getAllMasterSubSubItemsSync()

        // Sync Entries
        dao.getAllEntriesSync().forEach { entry ->
            masterEntries.find { it.name.equals(entry.entry_name, ignoreCase = true) }?.let { master ->
                if (entry.image_path != master.image_path) {
                    launch { dao.updateEntry(entry.copy(image_path = master.image_path)) }
                }
            }
        }

        // Sync Items
        dao.getAllItemsSync().forEach { item ->
            masterItems.find { it.name.equals(item.item_name, ignoreCase = true) }?.let { master ->
                if (item.image_path != master.image_path) {
                    launch { dao.updateItem(item.copy(image_path = master.image_path)) }
                }
            }
        }

        // Sync SubItems
        dao.getAllSubItemsSync().forEach { subItem ->
            masterSubItems.find { it.name.equals(subItem.name, ignoreCase = true) }?.let { master ->
                if (subItem.image_path != master.image_path) {
                    launch { dao.updateSubItem(subItem.copy(image_path = master.image_path)) }
                }
            }
        }

        // Sync Templates
        dao.getTemplatesSyncList().forEach { template ->
            dao.getTemplateEntriesSyncList(template.id).forEach { tEntry ->
                masterEntries.find { it.name.equals(tEntry.name, ignoreCase = true) }?.let { master ->
                    if (tEntry.image_path != master.image_path) {
                        launch { dao.updateTemplateEntry(tEntry.copy(image_path = master.image_path)) }
                    }
                }
                
                dao.getTemplateItemsSync(tEntry.id).forEach { tItem ->
                    masterItems.find { it.name.equals(tItem.name, ignoreCase = true) }?.let { master ->
                        if (tItem.image_path != master.image_path) {
                            launch { dao.updateTemplateItem(tItem.copy(image_path = master.image_path)) }
                        }
                    }
                    
                    dao.getTemplateSubItemsSync(tItem.id).forEach { tSub ->
                        masterSubItems.find { it.name.equals(tSub.name, ignoreCase = true) }?.let { master ->
                            if (tSub.image_path != master.image_path) {
                                launch { dao.updateTemplateSubItem(tSub.copy(image_path = master.image_path)) }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun addMultipleFromMaster(templateId: Int, masterItemIds: List<Int>) {
        masterItemIds.forEach { id ->
            addTemplateEntry(templateId, id)
        }
    }

    // ------------------ WEIGHT CALCULATIONS ------------------

    fun getListWeightDetails(listId: Int): Flow<WeightDetails> {
        return combine(
            dao.getEntries(listId),
            dao.getAllItemsForList(listId),
            dao.getAllSubItemsForList(listId),
            dao.getPayloadLocations(),
            dao.getExtraPayloadProfiles(),
            dao.getListExtraPayloads(listId)
        ) { args ->
            val entries = args[0] as List<Entry>
            val items = args[1] as List<Item>
            val subItems = args[2] as List<SubItem>
            val payloadLocations = args[3] as List<PayloadLocation>
            val extraPayloadProfiles = args[4] as List<ExtraPayloadProfile>
            val selectedPayloadLinks = args[5] as List<ListExtraPayload>

            val subItemsByItemId = subItems.groupBy { it.item_id }
            val itemsByEntryId = items.groupBy { it.entry_id }

            val payloadWeights = mutableMapOf<Int, Int>() // payloadLocationId -> totalWeightGrams

            var totalGearWeight = 0

            entries.forEach { entry ->
                var contentsWeight = 0
                if (entry.entry_type == "container") {
                    val entryItems = itemsByEntryId[entry.entry_id] ?: emptyList()
                    entryItems.forEach { item ->
                        var itemContentWeight = 0
                        if (item.is_container) {
                            val itemSubItems = subItemsByItemId[item.item_id] ?: emptyList()
                            itemSubItems.forEach { sub ->
                                val subWeight = sub.weightGrams * sub.quantity
                                itemContentWeight += subWeight
                                sub.payloadLocationId?.let { pid ->
                                    payloadWeights[pid] = (payloadWeights[pid] ?: 0) + (subWeight * item.quantity * entry.quantity)
                                }
                            }
                        } else {
                            val itemWeight = item.weightGrams * item.quantity
                            itemContentWeight = itemWeight
                            item.payloadLocationId?.let { pid ->
                                payloadWeights[pid] = (payloadWeights[pid] ?: 0) + (itemWeight * entry.quantity)
                            }
                        }
                        contentsWeight += (item.weightGrams + itemContentWeight) * item.quantity
                    }
                } else {
                    val entryWeight = entry.weightGrams * entry.quantity
                    contentsWeight = entryWeight
                    entry.payloadLocationId?.let { pid ->
                        payloadWeights[pid] = (payloadWeights[pid] ?: 0) + entryWeight
                    }
                }
                
                if (entry.entry_type == "container") {
                    totalGearWeight += (entry.weightGrams + contentsWeight) * entry.quantity
                    // Handle container's own weight if assigned to a payload
                    entry.payloadLocationId?.let { pid ->
                        payloadWeights[pid] = (payloadWeights[pid] ?: 0) + (entry.weightGrams * entry.quantity)
                    }
                } else {
                    totalGearWeight += contentsWeight
                }
            }

            val selectedProfileIds = selectedPayloadLinks.map { it.payloadProfileId }.toSet()
            val activeExtraPayloads = extraPayloadProfiles.filter { it.id in selectedProfileIds }
            val extraPayloadWeight = activeExtraPayloads.sumOf { it.weightGrams }
            
            val payloadDetails = payloadLocations.map { loc ->
                val currentWeight = payloadWeights[loc.id] ?: 0
                PayloadAnalysis(
                    location = loc,
                    currentWeightGrams = currentWeight
                )
            }

            WeightDetails(
                totalGearWeightGrams = totalGearWeight,
                extraPayloadWeightGrams = extraPayloadWeight,
                payloadAnalysis = payloadDetails
            )
        }
    }

    // ------------------ EXTRA PAYLOAD LOCATIONS ------------------

    fun getExtraWeightProfiles(): Flow<List<ExtraWeightProfile>> = dao.getExtraWeightProfiles()

    suspend fun addExtraWeightProfile(name: String, weightGrams: Int, category: String) {
        dao.insertExtraWeightProfile(ExtraWeightProfile(name = name, weightGrams = weightGrams, category = category))
    }

    suspend fun updateExtraWeightProfile(profile: ExtraWeightProfile) {
        dao.updateExtraWeightProfile(profile)
    }

    suspend fun deleteExtraWeightProfile(id: Int) {
        dao.deleteExtraWeightProfile(id)
    }

    // ------------------ PAYLOAD LOCATIONS ------------------

    fun getPayloadLocations(): Flow<List<PayloadLocation>> = dao.getPayloadLocations()

    suspend fun addPayloadLocation(name: String, limitGrams: Int?, category: String) {
        dao.insertPayloadLocation(PayloadLocation(name = name, weightLimitGrams = limitGrams, category = category))
    }

    suspend fun updatePayloadLocation(location: PayloadLocation) = dao.updatePayloadLocation(location)

    suspend fun deletePayloadLocation(id: Int) = dao.deletePayloadLocation(id)

    // ------------------ EXTRA PAYLOAD PROFILES ------------------

    fun getExtraPayloadProfiles(): Flow<List<ExtraPayloadProfile>> = dao.getExtraPayloadProfiles()

    suspend fun addExtraPayloadProfile(name: String, weightGrams: Int, category: String) {
        dao.insertExtraPayloadProfile(ExtraPayloadProfile(name = name, weightGrams = weightGrams, category = category))
    }

    suspend fun updateExtraPayloadProfile(profile: ExtraPayloadProfile) {
        dao.updateExtraPayloadProfile(profile)
    }

    suspend fun deleteExtraPayloadProfile(id: Int) {
        dao.deleteExtraPayloadProfile(id)
    }

    // ------------------ LIST EXTRA PAYLOADS ------------------

    fun getListExtraPayloads(listId: Int): Flow<List<ListExtraPayload>> = dao.getListExtraPayloads(listId)

    suspend fun toggleListExtraPayload(listId: Int, profileId: Int, isEnabled: Boolean) {
        if (isEnabled) {
            dao.insertListExtraPayload(ListExtraPayload(listId = listId, payloadProfileId = profileId))
        } else {
            dao.deleteListExtraPayload(listId, profileId)
        }
    }
}
