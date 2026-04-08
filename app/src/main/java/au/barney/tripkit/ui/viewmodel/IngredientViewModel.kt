package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.barney.tripkit.data.model.Ingredient
import au.barney.tripkit.data.model.IngredientGroup
import au.barney.tripkit.data.model.ListItem
import au.barney.tripkit.data.repository.TripKitRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class IngredientViewModel(
    private val repository: TripKitRepository
) : ViewModel() {

    private val _ingredients = MutableStateFlow<List<Ingredient>>(emptyList())
    val ingredients: StateFlow<List<Ingredient>> = _ingredients

    private val _allIngredients = MutableStateFlow<List<Ingredient>>(emptyList())
    val allIngredients: StateFlow<List<Ingredient>> = _allIngredients

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _currentGroup = MutableStateFlow<IngredientGroup?>(null)
    val currentGroup: StateFlow<IngredientGroup?> = _currentGroup

    private val _currentList = MutableStateFlow<ListItem?>(null)
    val currentList: StateFlow<ListItem?> = _currentList

    private var currentGroupId: Int = -1
    private var collectionJob: Job? = null

    fun loadIngredients(groupId: Int) {
        if (currentGroupId == groupId) return
        currentGroupId = groupId
        
        // Cancel previous collection to prevent "bleeding" into other groups
        collectionJob?.cancel()

        collectionJob = viewModelScope.launch {
            _loading.value = true
            
            try {
                val group = repository.getIngredientGroup(groupId)
                _currentGroup.value = group
                if (group != null) {
                    _currentList.value = repository.getList(group.list_id)
                }
            } catch (e: Exception) {
                _error.value = e.message
            }

            repository.getIngredients(groupId)
                .catch { e ->
                    _error.value = e.message ?: "Unknown error"
                    _loading.value = false
                }
                .collect { ingredientsList ->
                    _ingredients.value = ingredientsList
                    _loading.value = false
                }
        }
    }

    fun loadAllIngredientsForList(listId: Int) {
        viewModelScope.launch {
            repository.getAllIngredientsForList(listId)
                .catch { e -> _error.value = e.message }
                .collect { all ->
                    _allIngredients.value = all
                }
        }
    }

    fun addIngredient(name: String, quantity: String = "1", notes: String? = null) {
        val groupId = currentGroupId
        if (groupId == -1) return

        viewModelScope.launch {
            try {
                repository.addIngredient(groupId, name, quantity, notes)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun updateIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            try {
                repository.updateIngredient(ingredient)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun deleteIngredient(ingredientId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteIngredient(ingredientId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun toggleIngredient(ingredientId: Int, isChecked: Boolean) {
        val checkedInt = if (isChecked) 1 else 0
        viewModelScope.launch {
            try {
                repository.toggleIngredient(ingredientId, checkedInt)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }
}
