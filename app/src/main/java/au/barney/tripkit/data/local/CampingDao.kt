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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ListItem): Long

    @Update
    suspend fun updateList(list: ListItem)

    @Query("DELETE FROM lists WHERE id = :listId")
    suspend fun deleteList(listId: Int)

    // ------------------ ENTRIES ------------------
    @Query("SELECT * FROM entries WHERE list_id = :listId ORDER BY entry_name ASC")
    fun getEntries(listId: Int): Flow<List<Entry>>

    @Query("""
        SELECT *, (SELECT COUNT(*) FROM items WHERE entry_id = entries.entry_id) as subItemCount 
        FROM entries WHERE list_id = :listId ORDER BY entry_name ASC
    """)
    fun getEntriesWithCount(listId: Int): Flow<List<EntryWithCount>>

    @Query("SELECT * FROM entries WHERE list_id = :listId ORDER BY entry_name ASC")
    suspend fun getEntriesSync(listId: Int): List<Entry>

    @Query("SELECT * FROM entries WHERE entry_id = :entryId")
    suspend fun getEntry(entryId: Int): Entry?

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
    @Query("SELECT * FROM items WHERE entry_id = :entryId ORDER BY item_name ASC")
    fun getItems(entryId: Int): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE entry_id = :entryId ORDER BY item_name ASC")
    suspend fun getItemsSync(entryId: Int): List<Item>

    @Query("SELECT * FROM items WHERE entry_id IN (SELECT entry_id FROM entries WHERE list_id = :listId) ORDER BY item_name ASC")
    fun getAllItemsForList(listId: Int): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE entry_id IN (SELECT entry_id FROM entries WHERE list_id = :listId) ORDER BY item_name ASC")
    suspend fun getAllItemsForListSync(listId: Int): List<Item>

    @Query("SELECT * FROM items WHERE item_id = :itemId")
    suspend fun getItem(itemId: Int): Item?

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(menuItem: MenuItem)

    @Update
    suspend fun updateMenuItem(menuItem: MenuItem)

    @Query("DELETE FROM menu_items WHERE id = :menuId")
    suspend fun deleteMenuItem(menuId: Int)

    // ------------------ INGREDIENT GROUPS ------------------
    @Query("SELECT * FROM ingredient_groups WHERE list_id = :listId ORDER BY group_name ASC")
    fun getIngredientGroups(listId: Int): Flow<List<IngredientGroup>>

    @Query("SELECT * FROM ingredient_groups WHERE list_id = :listId ORDER BY group_name ASC")
    suspend fun getIngredientGroupsSync(listId: Int): List<IngredientGroup>

    @Query("SELECT * FROM ingredient_groups WHERE id = :groupId")
    suspend fun getIngredientGroup(groupId: Int): IngredientGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredientGroup(group: IngredientGroup): Long

    @Update
    suspend fun updateIngredientGroup(group: IngredientGroup)

    @Query("DELETE FROM ingredient_groups WHERE id = :groupId")
    suspend fun deleteIngredientGroup(groupId: Int)

    // ------------------ INGREDIENTS ------------------
    @Query("SELECT * FROM ingredients WHERE group_id = :groupId ORDER BY ingredient_name ASC")
    fun getIngredients(groupId: Int): Flow<List<Ingredient>>

    @Query("SELECT * FROM ingredients WHERE group_id = :groupId ORDER BY ingredient_name ASC")
    suspend fun getIngredientsSync(groupId: Int): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE group_id IN (SELECT id FROM ingredient_groups WHERE list_id = :listId) ORDER BY ingredient_name ASC")
    fun getAllIngredientsForList(listId: Int): Flow<List<Ingredient>>

    @Query("SELECT * FROM ingredients WHERE group_id IN (SELECT id FROM ingredient_groups WHERE list_id = :listId) ORDER BY ingredient_name ASC")
    suspend fun getAllIngredientsForListSync(listId: Int): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE id = :ingredientId")
    suspend fun getIngredient(ingredientId: Int): Ingredient?

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
        FROM master_items ORDER BY name ASC
    """)
    fun getMasterItemsWithCount(): Flow<List<MasterItemWithCount>>

    @Query("""
        SELECT *, (SELECT COUNT(*) FROM master_sub_items WHERE master_item_id = master_items.id) as subItemCount 
        FROM master_items ORDER BY name ASC
    """)
    suspend fun getMasterItemsSyncListWithCount(): List<MasterItemWithCount>

    @Query("SELECT * FROM master_items ORDER BY name ASC")
    fun getMasterItems(): Flow<List<MasterItem>>

    @Query("SELECT * FROM master_items ORDER BY name ASC")
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
    @Query("SELECT * FROM master_sub_items WHERE master_item_id = :masterItemId ORDER BY name ASC")
    fun getMasterSubItems(masterItemId: Int): Flow<List<MasterSubItem>>

    @Query("SELECT * FROM master_sub_items WHERE master_item_id = :masterItemId")
    suspend fun getMasterSubItemsSync(masterItemId: Int): List<MasterSubItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterSubItem(item: MasterSubItem): Long

    @Update
    suspend fun updateMasterSubItem(item: MasterSubItem)

    @Query("DELETE FROM master_sub_items WHERE id = :id")
    suspend fun deleteMasterSubItem(id: Int)

    // ------------------ ITINERARY ------------------
    @Query("SELECT * FROM itinerary_items WHERE list_id = :listId ORDER BY day ASC, time ASC")
    fun getItinerary(listId: Int): Flow<List<ItineraryItem>>

    @Query("SELECT * FROM itinerary_items WHERE list_id = :listId ORDER BY day ASC, time ASC")
    suspend fun getItinerarySync(listId: Int): List<ItineraryItem>

    @Query("SELECT * FROM itinerary_items WHERE id = :id")
    suspend fun getItineraryItem(id: Int): ItineraryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItineraryItem(item: ItineraryItem)

    @Update
    suspend fun updateItineraryItem(item: ItineraryItem)

    @Query("DELETE FROM itinerary_items WHERE id = :itemId")
    suspend fun deleteItineraryItem(itemId: Int)
}
