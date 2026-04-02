package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.barney.tripkit.data.model.MasterItem
import au.barney.tripkit.data.model.MasterItemWithCount
import au.barney.tripkit.data.model.MasterSubItem
import au.barney.tripkit.data.model.MasterSubSubItem
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

    private val _masterItemsWithCount = MutableStateFlow<List<MasterItemWithCount>>(emptyList())
    val masterItemsWithCount: StateFlow<List<MasterItemWithCount>> = _masterItemsWithCount

    private val _masterSubItems = MutableStateFlow<List<MasterSubItem>>(emptyList())
    val masterSubItems: StateFlow<List<MasterSubItem>> = _masterSubItems

    private val _masterSubSubItems = MutableStateFlow<List<MasterSubSubItem>>(emptyList())
    val masterSubSubItems: StateFlow<List<MasterSubSubItem>> = _masterSubSubItems

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Keep track of which container we are currently looking at
    private var currentMasterItemId: Int? = null
    private var currentMasterSubItemId: Int? = null

    init {
        loadMasterItems()
    }

    fun loadMasterItems() {
        viewModelScope.launch {
            _loading.value = true
            
            // Collect master items
            launch {
                repository.getMasterItems()
                    .catch { e -> _error.value = e.message }
                    .collect { items ->
                        _masterItems.value = items
                    }
            }

            // Collect master items with count
            launch {
                repository.getMasterItemsWithCount()
                    .catch { e ->
                        _error.value = e.message ?: "Unknown error"
                        _loading.value = false
                    }
                    .collect { items ->
                        _masterItemsWithCount.value = items
                        _loading.value = false
                    }
            }
        }
    }

    fun addMasterItem(name: String, isContainer: Boolean, imagePath: String? = null) {
        viewModelScope.launch {
            try {
                repository.addMasterItem(name, isContainer, imagePath)
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

    fun addMasterSubItem(masterItemId: Int, name: String, qty: Int, isContainer: Boolean, imagePath: String? = null) {
        viewModelScope.launch {
            try {
                repository.insertMasterSubItem(MasterSubItem(
                    master_item_id = masterItemId, 
                    name = name, 
                    default_quantity = qty, 
                    is_container = isContainer,
                    image_path = imagePath
                ))
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

    // --- SUB SUB ITEMS ---

    fun loadMasterSubSubItems(subItemId: Int) {
        currentMasterSubItemId = subItemId
        viewModelScope.launch {
            repository.getMasterSubSubItems(subItemId)
                .catch { e -> _error.value = e.message }
                .collectLatest { list ->
                    if (currentMasterSubItemId == subItemId) {
                        _masterSubSubItems.value = list
                    }
                }
        }
    }

    fun addMasterSubSubItem(subItemId: Int, name: String, qty: Int, isContainer: Boolean, imagePath: String? = null) {
        viewModelScope.launch {
            try {
                repository.insertMasterSubSubItem(MasterSubSubItem(
                    master_sub_item_id = subItemId,
                    name = name,
                    default_quantity = qty,
                    is_container = isContainer,
                    image_path = imagePath
                ))
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateMasterSubSubItem(item: MasterSubSubItem) {
        viewModelScope.launch {
            try {
                repository.updateMasterSubSubItem(item)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteMasterSubSubItem(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteMasterSubSubItem(id)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun syncPictures() {
        viewModelScope.launch {
            try {
                repository.syncMasterPictures()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
