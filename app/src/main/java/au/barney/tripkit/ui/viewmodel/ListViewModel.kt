package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.barney.tripkit.data.model.ListItem
import au.barney.tripkit.data.repository.TripKitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ListViewModel(
    val repository: TripKitRepository
) : ViewModel() {

    private val _lists = MutableStateFlow(emptyList<ListItem>())
    val lists: StateFlow<List<ListItem>> = _lists

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _packingProgress = MutableStateFlow<Map<Int, Pair<Int, Int>>>(emptyMap())
    val packingProgress: StateFlow<Map<Int, Pair<Int, Int>>> = _packingProgress

    init {
        loadLists()
    }

    fun loadLists() {
        viewModelScope.launch {
            _loading.value = true
            repository.getLists()
                .catch { e ->
                    _error.value = e.message ?: "Unknown error"
                    _loading.value = false
                }
                .collect { listItems ->
                    _lists.value = listItems
                    _loading.value = false
                    // Start observing progress for each list
                    listItems.forEach { list ->
                        observeProgress(list.id)
                    }
                }
        }
    }

    private fun observeProgress(listId: Int) {
        viewModelScope.launch {
            repository.getPackingProgress(listId).collect { progress ->
                val currentMap = _packingProgress.value.toMutableMap()
                currentMap[listId] = progress
                _packingProgress.value = currentMap
            }
        }
    }

    fun addList(
        name: String,
        showInventory: Boolean = true,
        showMenu: Boolean = true,
        showIngredients: Boolean = true,
        showItinerary: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                repository.addList(name, showInventory, showMenu, showIngredients, showItinerary)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun updateList(list: ListItem) {
        viewModelScope.launch {
            try {
                repository.updateList(list)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun deleteList(listId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteList(listId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun duplicateList(listId: Int, newName: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.duplicateList(listId, newName)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
            _loading.value = false
        }
    }
}
