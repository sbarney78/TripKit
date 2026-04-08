package au.barney.tripkit.data.local

import androidx.room.*
import au.barney.tripkit.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TripKitDao {

    // ------------------ LISTS ------------------
    @Query("SELECT * FROM lists ORDER BY name COLLATE NOCASE ASC")
    fun getLists(): Flow<List<ListItem>>

    @Query("SELECT * FROM lists WHERE id = :listId")
    suspend fun getList(listId: Int): ListItem?

    @Query("SELECT * FROM lists WHERE sync_id = :syncId")
    suspend fun getListBySyncId(syncId: String): ListItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ListItem): Long

    @Update
    suspend fun updateList(list: ListItem)

    @Query("DELETE FROM lists WHERE id = :listId")
    suspend fun deleteList(listId: Int)

    // ------------------ ENTRIES ------------------
    @Query("SELECT * FROM entries WHERE list_id = :listId ORDER BY entry_name COLLATE NOCASE ASC")
    fun getEntries(listId: Int): Flow<List<Entry>>

    @Query("""
        SELECT *, (SELECT COUNT(*) FROM items WHERE entry_id = entries.entry_id) as subItemCount 
        FROM entries WHERE list_id = :listId ORDER BY entry_name COLLATE NOCASE ASC
    """)
    fun getEntriesWithCount(listId: Int): Flow<List<EntryWithCount>>

    @Query("SELECT * FROM entries WHERE list_id = :listId ORDER BY entry_name COLLATE NOCASE ASC")
    suspend fun getEntriesSync(listId: Int): List<Entry>
    
    @Query("SELECT * FROM entries")
    suspend fun getAllEntriesSync(): List<Entry>

    @Query("SELECT * FROM entries WHERE entry_id = :entryId")
    suspend fun getEntry(entryId: Int): Entry?

    @Query("SELECT * FROM entries WHERE sync_id = :syncId AND list_id = :listId")
    suspend fun getEntryBySyncId(syncId: String, listId: Int): Entry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: Entry): Long

    @Update
    suspend fun updateEntry(entry: Entry)

    @Query("UPDATE entries SET color = :color WHERE entry_name = :name COLLATE NOCASE")
    suspend fun updateEntryColorByName(name: String, color: String)

    @Query("UPDATE entries SET weightGrams = :weight WHERE entry_name = :name COLLATE NOCASE")
    suspend fun updateEntryWeightByName(name: String, weight: Int)

    @Query("DELETE FROM entries WHERE entry_id = :entryId")
    suspend fun deleteEntry(entryId: Int)

    @Query("UPDATE entries SET is_checked = :checked WHERE entry_id = :id")
    suspend fun toggleEntry(id: Int, checked: Int)

    @Query("SELECT COUNT(*) FROM entries WHERE list_id = :listId")
    fun getTotalEntriesCount(listId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM entries WHERE list_id = :listId AND is_checked = 1")
    fun getCheckedEntriesCount(listId: Int): Flow<Int>

    // ------------------ ITEMS ------------------
    @Query("SELECT * FROM items WHERE entry_id = :entryId ORDER BY item_name COLLATE NOCASE ASC")
    fun getItems(entryId: Int): Flow<List<Item>>

    @Query("""
        SELECT *, (SELECT COUNT(*) FROM sub_items WHERE item_id = items.item_id) as subSubItemCount 
        FROM items WHERE entry_id = :entryId ORDER BY item_name COLLATE NOCASE ASC
    """)
    fun getItemsWithCount(entryId: Int): Flow<List<ItemWithCount>>

    @Query("SELECT * FROM items WHERE entry_id = :entryId ORDER BY item_name COLLATE NOCASE ASC")
    suspend fun getItemsSync(entryId: Int): List<Item>
    
    @Query("SELECT * FROM items")
    suspend fun getAllItemsSync(): List<Item>

    @Query("SELECT * FROM items WHERE entry_id IN (SELECT entry_id FROM entries WHERE list_id = :listId) ORDER BY item_name COLLATE NOCASE ASC")
    fun getAllItemsForList(listId: Int): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE entry_id IN (SELECT entry_id FROM entries WHERE list_id = :listId) ORDER BY item_name COLLATE NOCASE ASC")
    suspend fun getAllItemsForListSync(listId: Int): List<Item>

    @Query("SELECT * FROM items WHERE item_id = :itemId")
    suspend fun getItem(itemId: Int): Item?

    @Query("SELECT * FROM items WHERE sync_id = :syncId AND entry_id = :entryId")
    suspend fun getItemBySyncId(syncId: String, entryId: Int): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    @Update
    suspend fun updateItem(item: Item)

    @Query("UPDATE items SET color = :color WHERE item_name = :name COLLATE NOCASE")
    suspend fun updateItemColorByName(name: String, color: String)

    @Query("UPDATE items SET weightGrams = :weight WHERE item_name = :name COLLATE NOCASE")
    suspend fun updateItemWeightByName(name: String, weight: Int)

    @Query("DELETE FROM items WHERE item_id = :itemId")
    suspend fun deleteItem(itemId: Int)

    @Query("UPDATE items SET is_checked = :checked WHERE item_id = :itemId")
    suspend fun toggleItem(itemId: Int, checked: Int)

    @Query("SELECT COUNT(*) FROM items WHERE entry_id IN (SELECT entry_id FROM entries WHERE list_id = :listId)")
    fun getTotalSubItemsCount(listId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE entry_id IN (SELECT entry_id FROM entries WHERE list_id = :listId) AND is_checked = 1")
    fun getCheckedSubItemsCount(listId: Int): Flow<Int>

    // ------------------ SUB ITEMS (3rd LEVEL) ------------------
    @Query("SELECT * FROM sub_items WHERE item_id = :itemId ORDER BY name COLLATE NOCASE ASC")
    fun getSubItems(itemId: Int): Flow<List<SubItem>>

    @Query("SELECT * FROM sub_items WHERE item_id = :itemId")
    suspend fun getSubItemsSync(itemId: Int): List<SubItem>

    @Query("SELECT * FROM sub_items")
    suspend fun getAllSubItemsSync(): List<SubItem>

    @Query("SELECT * FROM sub_items WHERE item_id IN (SELECT item_id FROM items WHERE entry_id IN (SELECT entry_id FROM entries WHERE list_id = :listId))")
    fun getAllSubItemsForList(listId: Int): Flow<List<SubItem>>

    @Query("SELECT * FROM sub_items WHERE item_id IN (SELECT item_id FROM items WHERE entry_id IN (SELECT entry_id FROM entries WHERE list_id = :listId))")
    suspend fun getAllSubItemsForListSync(listId: Int): List<SubItem>

    @Query("SELECT * FROM sub_items WHERE id = :id")
    suspend fun getSubItem(id: Int): SubItem?

    @Query("SELECT * FROM sub_items WHERE sync_id = :syncId AND item_id = :itemId")
    suspend fun getSubItemBySyncId(syncId: String, itemId: Int): SubItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubItem(subItem: SubItem): Long

    @Update
    suspend fun updateSubItem(subItem: SubItem)

    @Query("UPDATE sub_items SET color = :color WHERE name = :name COLLATE NOCASE")
    suspend fun updateSubItemColorByName(name: String, color: String)

    @Query("UPDATE sub_items SET weightGrams = :weight WHERE name = :name COLLATE NOCASE")
    suspend fun updateSubItemWeightByName(name: String, weight: Int)

    @Query("DELETE FROM sub_items WHERE id = :id")
    suspend fun deleteSubItem(id: Int)

    @Query("UPDATE sub_items SET is_checked = :checked WHERE id = :id")
    suspend fun toggleSubItem(id: Int, checked: Int)

    // ------------------ MENU ------------------
    @Query("""
        SELECT * FROM menu_items 
        WHERE list_id = :listId 
        ORDER BY 
            (SELECT MIN(id) FROM menu_items AS mi2 WHERE mi2.day = menu_items.day AND mi2.list_id = menu_items.list_id),
            CASE meal_type 
                WHEN 'Breakfast' THEN 1 
                WHEN 'Morning Tea' THEN 2 
                WHEN 'Lunch' THEN 3 
                WHEN 'Afternoon Tea' THEN 4 
                WHEN 'Dinner' THEN 5 
                WHEN 'Dessert' THEN 6 
                WHEN 'Supper' THEN 7 
                ELSE 8 
            END
    """)
    fun getMenu(listId: Int): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items WHERE list_id = :listId")
    suspend fun getMenuSync(listId: Int): List<MenuItem>

    @Query("SELECT * FROM menu_items WHERE id = :menuId")
    suspend fun getMenuItemSync(menuId: Int): MenuItem?

    @Query("SELECT * FROM menu_items WHERE sync_id = :syncId AND list_id = :listId")
    suspend fun getMenuItemBySyncId(syncId: String, listId: Int): MenuItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(menuItem: MenuItem)

    @Update
    suspend fun updateMenuItem(menuItem: MenuItem)

    @Query("DELETE FROM menu_items WHERE id = :menuId")
    suspend fun deleteMenuItem(menuId: Int)

    // ------------------ INGREDIENT GROUPS ------------------
    @Query("SELECT * FROM ingredient_groups WHERE list_id = :listId ORDER BY group_name COLLATE NOCASE ASC")
    fun getIngredientGroups(listId: Int): Flow<List<IngredientGroup>>

    @Query("SELECT * FROM ingredient_groups WHERE list_id = :listId ORDER BY group_name COLLATE NOCASE ASC")
    suspend fun getIngredientGroupsSync(listId: Int): List<IngredientGroup>

    @Query("SELECT * FROM ingredient_groups WHERE id = :groupId")
    suspend fun getIngredientGroup(groupId: Int): IngredientGroup?

    @Query("SELECT * FROM ingredient_groups WHERE sync_id = :syncId AND list_id = :listId")
    suspend fun getIngredientGroupBySyncId(syncId: String, listId: Int): IngredientGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredientGroup(group: IngredientGroup): Long

    @Update
    suspend fun updateIngredientGroup(group: IngredientGroup)

    @Query("DELETE FROM ingredient_groups WHERE id = :groupId")
    suspend fun deleteIngredientGroup(groupId: Int)

    // ------------------ INGREDIENTS ------------------
    @Query("SELECT * FROM ingredients WHERE group_id = :groupId ORDER BY ingredient_name COLLATE NOCASE ASC")
    fun getIngredients(groupId: Int): Flow<List<Ingredient>>

    @Query("SELECT * FROM ingredients WHERE group_id = :groupId ORDER BY ingredient_name COLLATE NOCASE ASC")
    suspend fun getIngredientsSync(groupId: Int): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE group_id IN (SELECT id FROM ingredient_groups WHERE list_id = :listId) ORDER BY ingredient_name COLLATE NOCASE ASC")
    fun getAllIngredientsForList(listId: Int): Flow<List<Ingredient>>

    @Query("SELECT * FROM ingredients WHERE group_id IN (SELECT id FROM ingredient_groups WHERE list_id = :listId) ORDER BY ingredient_name COLLATE NOCASE ASC")
    suspend fun getAllIngredientsForListSync(listId: Int): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE id = :ingredientId")
    suspend fun getIngredient(ingredientId: Int): Ingredient?

    @Query("SELECT * FROM ingredients WHERE sync_id = :syncId AND group_id = :groupId")
    suspend fun getIngredientBySyncId(syncId: String, groupId: Int): Ingredient?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: Ingredient)

    @Update
    suspend fun updateIngredient(ingredient: Ingredient)

    @Query("DELETE FROM ingredients WHERE id = :ingredientId")
    suspend fun deleteIngredient(ingredientId: Int)

    @Query("UPDATE ingredients SET is_checked = :isChecked WHERE id = :ingredientId")
    suspend fun toggleIngredient(ingredientId: Int, isChecked: Int)

    // ------------------ MASTER ITEMS ------------------
    @Query("""
        SELECT *, (SELECT COUNT(*) FROM master_sub_items WHERE master_item_id = master_items.id) as subItemCount 
        FROM master_items ORDER BY name COLLATE NOCASE ASC
    """)
    fun getMasterItemsWithCount(): Flow<List<MasterItemWithCount>>

    @Query("""
        SELECT *, (SELECT COUNT(*) FROM master_sub_items WHERE master_item_id = master_items.id) as subItemCount 
        FROM master_items ORDER BY name COLLATE NOCASE ASC
    """)
    suspend fun getMasterItemsSyncListWithCount(): List<MasterItemWithCount>

    @Query("SELECT * FROM master_items ORDER BY name COLLATE NOCASE ASC")
    fun getMasterItems(): Flow<List<MasterItem>>

    @Query("SELECT * FROM master_items ORDER BY name COLLATE NOCASE ASC")
    suspend fun getMasterItemsSyncList(): List<MasterItem>

    @Query("SELECT * FROM master_items WHERE id = :id")
    suspend fun getMasterItem(id: Int): MasterItem?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMasterItem(item: MasterItem): Long

    @Update
    suspend fun updateMasterItem(item: MasterItem)

    @Query("DELETE FROM master_items WHERE id = :id")
    suspend fun deleteMasterItem(id: Int)

    // ------------------ MASTER SUB ITEMS ------------------
    @Query("SELECT * FROM master_sub_items WHERE master_item_id = :masterItemId ORDER BY name COLLATE NOCASE ASC")
    fun getMasterSubItems(masterItemId: Int): Flow<List<MasterSubItem>>

    @Query("""
        SELECT *, (SELECT COUNT(*) FROM master_sub_sub_items WHERE master_sub_item_id = master_sub_items.id) as subSubItemCount 
        FROM master_sub_items WHERE master_item_id = :masterItemId ORDER BY name COLLATE NOCASE ASC
    """)
    fun getMasterSubItemsWithCount(masterItemId: Int): Flow<List<MasterSubItemWithCount>>

    @Query("SELECT * FROM master_sub_items WHERE master_item_id = :masterItemId")
    suspend fun getMasterSubItemsSync(masterItemId: Int): List<MasterSubItem>
    
    @Query("SELECT * FROM master_sub_items")
    suspend fun getAllMasterSubItemsSync(): List<MasterSubItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterSubItem(item: MasterSubItem): Long

    @Update
    suspend fun updateMasterSubItem(item: MasterSubItem)

    @Query("DELETE FROM master_sub_items WHERE id = :id")
    suspend fun deleteMasterSubItem(id: Int)

    // ------------------ MASTER SUB SUB ITEMS ------------------
    @Query("SELECT * FROM master_sub_sub_items WHERE master_sub_item_id = :subItemId ORDER BY name COLLATE NOCASE ASC")
    fun getMasterSubSubItems(subItemId: Int): Flow<List<MasterSubSubItem>>

    @Query("SELECT * FROM master_sub_sub_items WHERE master_sub_item_id = :subItemId ORDER BY name COLLATE NOCASE ASC")
    suspend fun getMasterSubSubItemsSync(subItemId: Int): List<MasterSubSubItem>

    @Query("SELECT * FROM master_sub_sub_items")
    suspend fun getAllMasterSubSubItemsSync(): List<MasterSubSubItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterSubSubItem(item: MasterSubSubItem): Long

    @Update
    suspend fun updateMasterSubSubItem(item: MasterSubSubItem)

    @Query("DELETE FROM master_sub_sub_items WHERE id = :id")
    suspend fun deleteMasterSubSubItem(id: Int)

    // ------------------ ITINERARY ------------------
    @Query("SELECT * FROM itinerary_items WHERE list_id = :listId ORDER BY day ASC, time ASC")
    fun getItinerary(listId: Int): Flow<List<ItineraryItem>>

    @Query("SELECT * FROM itinerary_items WHERE list_id = :listId ORDER BY day ASC, time ASC")
    suspend fun getItinerarySync(listId: Int): List<ItineraryItem>

    @Query("SELECT * FROM itinerary_items WHERE id = :id")
    suspend fun getItineraryItem(id: Int): ItineraryItem?

    @Query("SELECT * FROM itinerary_items WHERE sync_id = :sync_id AND list_id = :listId")
    suspend fun getItineraryItemBySyncId(sync_id: String, listId: Int): ItineraryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItineraryItem(item: ItineraryItem)

    @Update
    suspend fun updateItineraryItem(item: ItineraryItem)

    @Query("DELETE FROM itinerary_items WHERE id = :itemId")
    suspend fun deleteItineraryItem(itemId: Int)

    // ------------------ TEMPLATES ------------------
    @Query("SELECT * FROM templates ORDER BY name COLLATE NOCASE ASC")
    fun getTemplates(): Flow<List<Template>>

    @Query("SELECT * FROM templates WHERE id = :templateId")
    suspend fun getTemplate(templateId: Int): Template?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: Template): Long

    @Update
    suspend fun updateTemplate(template: Template)

    @Query("DELETE FROM templates WHERE id = :templateId")
    suspend fun deleteTemplate(templateId: Int)

    @Query("SELECT * FROM template_entries WHERE template_id = :templateId ORDER BY name COLLATE NOCASE ASC")
    fun getTemplateEntries(templateId: Int): Flow<List<TemplateEntry>>

    @Query("SELECT * FROM template_entries WHERE template_id = :templateId ORDER BY name COLLATE NOCASE ASC")
    suspend fun getTemplateEntriesSync(templateId: Int): List<TemplateEntry>

    @Query("SELECT * FROM template_entries WHERE id = :id")
    suspend fun getTemplateEntrySync(id: Int): TemplateEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateEntry(entry: TemplateEntry): Long

    @Update
    suspend fun updateTemplateEntry(entry: TemplateEntry)

    @Query("UPDATE template_entries SET color = :color WHERE name = :name COLLATE NOCASE")
    suspend fun updateTemplateEntryColorByName(name: String, color: String)

    @Query("UPDATE template_entries SET weightGrams = :weight WHERE name = :name COLLATE NOCASE")
    suspend fun updateTemplateEntryWeightByName(name: String, weight: Int)

    @Query("UPDATE template_entries SET is_checked = :checked WHERE id = :id")
    suspend fun updateTemplateEntryChecked(id: Int, checked: Int)

    @Query("DELETE FROM template_entries WHERE id = :id")
    suspend fun deleteTemplateEntry(id: Int)

    @Query("DELETE FROM template_entries WHERE template_id = :templateId")
    suspend fun deleteTemplateEntries(templateId: Int)

    @Query("SELECT * FROM template_items WHERE template_entry_id = :entryId ORDER BY name COLLATE NOCASE ASC")
    fun getTemplateItems(entryId: Int): Flow<List<TemplateItem>>

    @Query("SELECT * FROM template_items WHERE template_entry_id = :entryId ORDER BY name COLLATE NOCASE ASC")
    suspend fun getTemplateItemsSync(entryId: Int): List<TemplateItem>

    @Query("SELECT * FROM template_items WHERE id = :id")
    suspend fun getTemplateItemSync(id: Int): TemplateItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateItem(item: TemplateItem): Long

    @Update
    suspend fun updateTemplateItem(item: TemplateItem)

    @Query("UPDATE template_items SET color = :color WHERE name = :name COLLATE NOCASE")
    suspend fun updateTemplateItemColorByName(name: String, color: String)

    @Query("UPDATE template_items SET weightGrams = :weight WHERE name = :name COLLATE NOCASE")
    suspend fun updateTemplateItemWeightByName(name: String, weight: Int)

    @Query("UPDATE template_items SET is_checked = :checked WHERE id = :id")
    suspend fun updateTemplateItemChecked(id: Int, checked: Int)

    @Query("UPDATE template_items SET is_checked = :checked WHERE template_entry_id = :entryId")
    suspend fun updateAllItemsInEntryChecked(entryId: Int, checked: Int)

    @Query("DELETE FROM template_items WHERE id = :id")
    suspend fun deleteTemplateItem(id: Int)

    @Query("SELECT * FROM template_sub_items WHERE template_item_id = :itemId ORDER BY name COLLATE NOCASE ASC")
    fun getTemplateSubItems(itemId: Int): Flow<List<TemplateSubItem>>

    @Query("SELECT * FROM template_sub_items WHERE template_item_id = :itemId ORDER BY name COLLATE NOCASE ASC")
    suspend fun getTemplateSubItemsSync(itemId: Int): List<TemplateSubItem>

    @Query("SELECT * FROM template_sub_items WHERE id = :id")
    suspend fun getTemplateSubItemSync(id: Int): TemplateSubItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateSubItem(subItem: TemplateSubItem): Long

    @Update
    suspend fun updateTemplateSubItem(subItem: TemplateSubItem)

    @Query("UPDATE template_sub_items SET color = :color WHERE name = :name COLLATE NOCASE")
    suspend fun updateTemplateSubItemColorByName(name: String, color: String)

    @Query("UPDATE template_sub_items SET weightGrams = :weight WHERE name = :name COLLATE NOCASE")
    suspend fun updateTemplateSubItemWeightByName(name: String, weight: Int)

    @Query("UPDATE template_sub_items SET is_checked = :checked WHERE id = :id")
    suspend fun updateTemplateSubItemChecked(id: Int, checked: Int)

    @Query("UPDATE template_sub_items SET is_checked = :checked WHERE template_item_id = :itemId")
    suspend fun updateAllSubItemsInItemChecked(itemId: Int, checked: Int)

    @Query("UPDATE template_sub_items SET is_checked = :checked WHERE template_item_id IN (SELECT id FROM template_items WHERE template_entry_id = :entryId)")
    suspend fun updateAllSubItemsInEntryChecked(entryId: Int, checked: Int)

    @Query("DELETE FROM template_sub_items WHERE id = :id")
    suspend fun deleteTemplateSubItem(id: Int)

    // ------------------ EXTRA WEIGHT PROFILES ------------------
    @Query("SELECT * FROM extra_weight_profiles ORDER BY category ASC, name ASC")
    fun getExtraWeightProfiles(): Flow<List<ExtraWeightProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtraWeightProfile(profile: ExtraWeightProfile)

    @Update
    suspend fun updateExtraWeightProfile(profile: ExtraWeightProfile)

    @Query("DELETE FROM extra_weight_profiles WHERE id = :id")
    suspend fun deleteExtraWeightProfile(id: Int)

    // ------------------ PAYLOAD LOCATIONS ------------------
    @Query("SELECT * FROM payload_locations ORDER BY category ASC, name ASC")
    fun getPayloadLocations(): Flow<List<PayloadLocation>>

    @Query("SELECT * FROM payload_locations WHERE id = :id")
    suspend fun getPayloadLocation(id: Int): PayloadLocation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayloadLocation(location: PayloadLocation): Long

    @Update
    suspend fun updatePayloadLocation(location: PayloadLocation)

    @Query("DELETE FROM payload_locations WHERE id = :id")
    suspend fun deletePayloadLocation(id: Int)

    // ------------------ EXTRA PAYLOAD PROFILES ------------------
    @Query("SELECT * FROM extra_payload_profiles ORDER BY category ASC, name ASC")
    fun getExtraPayloadProfiles(): Flow<List<ExtraPayloadProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtraPayloadProfile(profile: ExtraPayloadProfile)

    @Update
    suspend fun updateExtraPayloadProfile(profile: ExtraPayloadProfile)

    @Query("DELETE FROM extra_payload_profiles WHERE id = :id")
    suspend fun deleteExtraPayloadProfile(id: Int)

    // ------------------ LIST EXTRA PAYLOADS ------------------
    @Query("SELECT * FROM list_extra_payloads WHERE listId = :listId")
    fun getListExtraPayloads(listId: Int): Flow<List<ListExtraPayload>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListExtraPayload(link: ListExtraPayload)

    @Query("DELETE FROM list_extra_payloads WHERE listId = :listId AND payloadProfileId = :profileId")
    suspend fun deleteListExtraPayload(listId: Int, profileId: Int)
}
