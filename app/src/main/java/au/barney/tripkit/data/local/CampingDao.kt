package au.barney.tripkit.data.local

import androidx.room.*
import au.barney.tripkit.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TripKitDao {

    // ------------------ LISTS ------------------
    @Query("SELECT * FROM lists ORDER BY created_at DESC")
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

    @Query("SELECT * FROM sub_items WHERE id = :id")
    suspend fun getSubItem(id: Int): SubItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubItem(subItem: SubItem): Long

    @Update
    suspend fun updateSubItem(subItem: SubItem)

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
}
