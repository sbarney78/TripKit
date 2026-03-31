package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.barney.tripkit.data.model.ListItem
import au.barney.tripkit.data.model.MenuItem
import au.barney.tripkit.data.repository.TripKitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MenuViewModel(
    private val repository: TripKitRepository
) : ViewModel() {

    private val _menu = MutableStateFlow<List<MenuItem>>(emptyList())
    val menu: StateFlow<List<MenuItem>> = _menu

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _currentList = MutableStateFlow<ListItem?>(null)
    val currentList: StateFlow<ListItem?> = _currentList

    fun loadMenu(listId: Int) {
        viewModelScope.launch {
            _loading.value = true

            try {
                _currentList.value = repository.getList(listId)
            } catch (e: Exception) {
                _error.value = e.message
            }

            repository.getMenu(listId)
                .catch { e ->
                    _error.value = e.message
                    _loading.value = false
                }
                .collect { menuList ->
                    _menu.value = menuList
                    _loading.value = false
                }
        }
    }

    fun addMenuItem(listId: Int, day: String, mealType: String, description: String) {
        viewModelScope.launch {
            try {
                repository.addMenuItem(listId, day, mealType, description)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateMenuItem(menuId: Int, day: String, mealType: String, description: String, listId: Int) {
        viewModelScope.launch {
            try {
                repository.updateMenuItem(menuId, day, mealType, description)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteMenuItem(menuId: Int, listId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteMenuItem(menuId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
