package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.barney.tripkit.data.model.MasterItem
import au.barney.tripkit.data.model.MasterSubItem
import au.barney.tripkit.data.repository.TripKitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MasterItemViewModel(
    private val repository: TripKitRepository
) : ViewModel() {

    private val _masterItems = MutableStateFlow<List<MasterItem>>(emptyList())
    val masterItems: StateFlow<List<MasterItem>> = _masterItems

    private val _masterSubItems = MutableStateFlow<List<MasterSubItem>>(emptyList())
    val masterSubItems: StateFlow<List<MasterSubItem>> = _masterSubItems

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Keep track of which container we are currently looking at
    private var currentMasterItemId: Int? = null

    init {
        loadMasterItems()
    }

    fun loadMasterItems() {
        viewModelScope.launch {
            _loading.value = true
            repository.getMasterItems()
                .catch { e ->
                    _error.value = e.message ?: "Unknown error"
                    _loading.value = false
                }
                .collect { items ->
                    _masterItems.value = items
                    _loading.value = false
                }
        }
    }

    fun addMasterItem(name: String, isContainer: Boolean) {
        viewModelScope.launch {
            try {
                repository.addMasterItem(name, isContainer)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun updateMasterItem(item: MasterItem) {
        viewModelScope.launch {
            try {
                repository.updateMasterItem(item)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun deleteMasterItem(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteMasterItem(id)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    // --- SUB ITEMS ---

    fun loadMasterSubItems(masterItemId: Int) {
        currentMasterItemId = masterItemId
        viewModelScope.launch {
            repository.getMasterSubItems(masterItemId)
                .catch { e -> _error.value = e.message }
                .collectLatest { list ->
                    // Only update if this is still the container the user is looking at
                    if (currentMasterItemId == masterItemId) {
                        _masterSubItems.value = list
                    }
                }
        }
    }

    fun addMasterSubItem(masterItemId: Int, name: String, qty: Int) {
        viewModelScope.launch {
            try {
                repository.insertMasterSubItem(MasterSubItem(master_item_id = masterItemId, name = name, default_quantity = qty))
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateMasterSubItem(item: MasterSubItem) {
        viewModelScope.launch {
            try {
                repository.updateMasterSubItem(item)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteMasterSubItem(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteMasterSubItem(id)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
