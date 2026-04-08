package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.barney.tripkit.data.model.IngredientGroup
import au.barney.tripkit.data.model.ListItem
import au.barney.tripkit.data.repository.TripKitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class IngredientGroupViewModel(
    private val repository: TripKitRepository
) : ViewModel() {

    private val _groups = MutableStateFlow<List<IngredientGroup>>(emptyList())
    val groups: StateFlow<List<IngredientGroup>> = _groups

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _currentList = MutableStateFlow<ListItem?>(null)
    val currentList: StateFlow<ListItem?> = _currentList

    private var currentListId: Int = -1

    fun loadGroups(listId: Int) {
        currentListId = listId

        viewModelScope.launch {
            _loading.value = true

            try {
                _currentList.value = repository.getList(listId)
            } catch (e: Exception) {
                _error.value = e.message
            }

            repository.getIngredientGroups(listId)
                .catch { e ->
                    _error.value = e.message ?: "Unknown error"
                    _loading.value = false
                }
                .collect { groupsList ->
                    _groups.value = groupsList
                    _loading.value = false
                }
        }
    }

    fun addGroup(name: String) {
        if (currentListId == -1) return

        viewModelScope.launch {
            try {
                repository.addIngredientGroup(currentListId, name)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun updateGroup(group: IngredientGroup) {
        viewModelScope.launch {
            try {
                repository.updateIngredientGroup(group)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun deleteGroup(groupId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteIngredientGroup(groupId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }
}
