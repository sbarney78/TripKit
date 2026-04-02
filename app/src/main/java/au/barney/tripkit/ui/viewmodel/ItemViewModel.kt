package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.barney.tripkit.data.model.Item
import au.barney.tripkit.data.model.ItemWithCount
import au.barney.tripkit.data.model.SubItem
import au.barney.tripkit.data.repository.TripKitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ItemViewModel(
    private val repository: TripKitRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items

    private val _itemsWithCount = MutableStateFlow<List<ItemWithCount>>(emptyList())
    val itemsWithCount: StateFlow<List<ItemWithCount>> = _itemsWithCount

    private val _allItems = MutableStateFlow<List<Item>>(emptyList())
    val allItems: StateFlow<List<Item>> = _allItems

    private val _allSubItems = MutableStateFlow<List<SubItem>>(emptyList())
    val allSubItems: StateFlow<List<SubItem>> = _allSubItems

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var currentEntryId: Int = -1

    private val _currentItem = MutableStateFlow<Item?>(null)
    val currentItem: StateFlow<Item?> = _currentItem


    // ------------------ LOAD ALL ITEMS FOR ENTRY ------------------

    fun loadItems(entryId: Int) {
        currentEntryId = entryId

        viewModelScope.launch {
            _loading.value = true
            
            // Get simple items
            launch {
                repository.getItems(entryId)
                    .catch { e -> _error.value = e.message }
                    .collect { _items.value = it }
            }

            // Get items with count
            launch {
                repository.getItemsWithCount(entryId)
                    .catch { e ->
                        _error.value = e.message ?: "Unknown error"
                        _loading.value = false
                    }
                    .collect { itemsList ->
                        _itemsWithCount.value = itemsList
                        _loading.value = false
                    }
            }
        }
    }

    // ------------------ LOAD ALL ITEMS FOR PDF ------------------

    fun loadAllItemsForList(listId: Int) {
        viewModelScope.launch {
            repository.getAllItemsForList(listId)
                .catch { e -> _error.value = e.message }
                .collect { all ->
                    _allItems.value = all
                }
        }
        
        viewModelScope.launch {
            repository.getAllSubItemsForList(listId)
                .catch { e -> _error.value = e.message }
                .collect { all ->
                    _allSubItems.value = all
                }
        }
    }


    // ------------------ ADD ITEM ------------------

    fun addItem(name: String, quantity: Int, notes: String?, isContainer: Boolean, imagePath: String? = null, addToMaster: Boolean = true) {
        if (currentEntryId == -1) return

        viewModelScope.launch {
            try {
                repository.addItem(currentEntryId, name, quantity, notes, isContainer, imagePath, addToMaster)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }


    // ------------------ DELETE ITEM ------------------

    fun deleteItem(itemId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteItem(itemId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }


    // ------------------ TOGGLE ITEM CHECKBOX ------------------

    fun toggleItem(itemId: Int, checked: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleItem(
                    id = itemId,
                    checked = if (checked) 1 else 0
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }


    // ------------------ LOAD SINGLE ITEM FOR EDITING ------------------

    fun loadItem(itemId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _currentItem.value = repository.getItem(itemId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
            _loading.value = false
        }
    }


    // ------------------ UPDATE ITEM ------------------

    fun updateItem(itemId: Int, name: String, quantity: Int, notes: String?, isContainer: Boolean, imagePath: String? = null) {
        viewModelScope.launch {
            try {
                repository.updateItem(itemId, name, quantity, notes, isContainer, imagePath)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    // ------------------ SUB ITEMS ------------------

    fun getSubItems(itemId: Int): Flow<List<SubItem>> = repository.getSubItems(itemId)

    fun addSubItem(itemId: Int, name: String, quantity: Int, notes: String?, imagePath: String? = null, addToMaster: Boolean = true) {
        viewModelScope.launch {
            try {
                repository.addSubItem(itemId, name, quantity, notes, imagePath, addToMaster)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun toggleSubItem(id: Int, checked: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleSubItem(id, if (checked) 1 else 0)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteSubItem(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteSubItem(id)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateSubItem(subItem: SubItem) {
        viewModelScope.launch {
            try {
                repository.updateSubItem(subItem)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
