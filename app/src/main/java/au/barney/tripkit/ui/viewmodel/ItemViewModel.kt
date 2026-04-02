package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.barney.tripkit.data.model.Item
import au.barney.tripkit.data.repository.TripKitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ItemViewModel(
    private val repository: TripKitRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items

    private val _allItems = MutableStateFlow<List<Item>>(emptyList())
    val allItems: StateFlow<List<Item>> = _allItems

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
            repository.getItems(entryId)
                .catch { e ->
                    _error.value = e.message ?: "Unknown error"
                    _loading.value = false
                }
                .collect { itemsList ->
                    _items.value = itemsList
                    _loading.value = false
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
                    itemId = itemId,
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
}
