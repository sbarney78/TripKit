package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.barney.tripkit.data.model.*
import au.barney.tripkit.data.repository.TripKitRepository
import kotlinx.coroutines.flow.Flow
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

    private val _extraWeightProfiles = MutableStateFlow<List<ExtraWeightProfile>>(emptyList())
    val extraWeightProfiles: StateFlow<List<ExtraWeightProfile>> = _extraWeightProfiles

    private val _payloadLocations = MutableStateFlow<List<PayloadLocation>>(emptyList())
    val payloadLocations: StateFlow<List<PayloadLocation>> = _payloadLocations

    private val _extraPayloadProfiles = MutableStateFlow<List<ExtraPayloadProfile>>(emptyList())
    val extraPayloadProfiles: StateFlow<List<ExtraPayloadProfile>> = _extraPayloadProfiles

    init {
        loadLists()
        loadExtraWeightProfiles()
        loadPayloadLocations()
        loadExtraPayloadProfiles()
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
        showItinerary: Boolean = true,
        templateId: Int? = null
    ) {
        viewModelScope.launch {
            try {
                repository.addList(name, showInventory, showMenu, showIngredients, showItinerary, templateId)
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

    // --- WEIGHTS ---

    fun getListWeightDetails(listId: Int): Flow<WeightDetails> {
        return repository.getListWeightDetails(listId)
    }

    fun loadExtraWeightProfiles() {
        viewModelScope.launch {
            repository.getExtraWeightProfiles().collect { profiles ->
                _extraWeightProfiles.value = profiles
            }
        }
    }

    fun addExtraWeightProfile(name: String, weightGrams: Int, category: String) {
        viewModelScope.launch {
            repository.addExtraWeightProfile(name, weightGrams, category)
        }
    }

    fun deleteExtraWeightProfile(id: Int) {
        viewModelScope.launch {
            repository.deleteExtraWeightProfile(id)
        }
    }

    // --- PAYLOAD LOCATIONS ---

    fun loadPayloadLocations() {
        viewModelScope.launch {
            repository.getPayloadLocations().collect { locations ->
                _payloadLocations.value = locations
            }
        }
    }

    fun addPayloadLocation(name: String, limitGrams: Int?, category: String) {
        viewModelScope.launch {
            repository.addPayloadLocation(name, limitGrams, category)
        }
    }

    fun updatePayloadLocation(location: PayloadLocation) {
        viewModelScope.launch {
            repository.updatePayloadLocation(location)
        }
    }

    fun deletePayloadLocation(id: Int) {
        viewModelScope.launch {
            repository.deletePayloadLocation(id)
        }
    }

    // --- EXTRA PAYLOAD PROFILES ---

    fun loadExtraPayloadProfiles() {
        viewModelScope.launch {
            repository.getExtraPayloadProfiles().collect { profiles ->
                _extraPayloadProfiles.value = profiles
            }
        }
    }

    fun addExtraPayloadProfile(name: String, weightGrams: Int, category: String) {
        viewModelScope.launch {
            repository.addExtraPayloadProfile(name, weightGrams, category)
        }
    }

    fun updateExtraPayloadProfile(profile: ExtraPayloadProfile) {
        viewModelScope.launch {
            repository.updateExtraPayloadProfile(profile)
        }
    }

    fun deleteExtraPayloadProfile(id: Int) {
        viewModelScope.launch {
            repository.deleteExtraPayloadProfile(id)
        }
    }

    // --- LIST EXTRA PAYLOADS ---

    fun getActiveExtraPayloads(listId: Int): Flow<List<ListExtraPayload>> {
        return repository.getListExtraPayloads(listId)
    }

    fun toggleExtraPayloadForList(listId: Int, profileId: Int, active: Boolean) {
        viewModelScope.launch {
            repository.toggleListExtraPayload(listId, profileId, active)
        }
    }
}
