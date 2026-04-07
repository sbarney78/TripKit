package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.barney.tripkit.data.model.Entry
import au.barney.tripkit.data.model.EntryWithCount
import au.barney.tripkit.data.model.ListItem
import au.barney.tripkit.data.repository.TripKitRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class EntryViewModel(
    val repository: TripKitRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries

    private val _entriesWithCount = MutableStateFlow<List<EntryWithCount>>(emptyList())
    val entriesWithCount: StateFlow<List<EntryWithCount>> = _entriesWithCount

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _currentEntry = MutableStateFlow<Entry?>(null)
    val currentEntry: StateFlow<Entry?> = _currentEntry

    private val _currentList = MutableStateFlow<ListItem?>(null)
    val currentList: StateFlow<ListItem?> = _currentList

    private var loadJob: Job? = null

    // ------------------ LOAD ENTRIES ------------------

    fun loadEntries(listId: Int) {
        // Cancel the previous collection job to prevent flickering and bleeding
        loadJob?.cancel()
        
        loadJob = viewModelScope.launch {
            _loading.value = true
            _entries.value = emptyList()
            _entriesWithCount.value = emptyList()
            
            try {
                _currentList.value = repository.getList(listId)
            } catch (e: Exception) {
                _error.value = e.message
            }

            // Load regular entries
            launch {
                repository.getEntries(listId)
                    .catch { e -> _error.value = e.message ?: "Unknown error" }
                    .collect { entriesList ->
                        _entries.value = entriesList
                    }
            }

            // Load entries with count
            launch {
                repository.getEntriesWithCount(listId)
                    .catch { e ->
                        _error.value = e.message ?: "Unknown error"
                        _loading.value = false
                    }
                    .collect { list ->
                        _entriesWithCount.value = list
                        _loading.value = false
                    }
            }
        }
    }


    // ------------------ LOAD SINGLE ENTRY ------------------

    fun loadEntry(entryId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _currentEntry.value = repository.getEntry(entryId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
            _loading.value = false
        }
    }


    // ------------------ ADD ENTRY ------------------

    fun addEntry(listId: Int, name: String, quantity: Int, notes: String?, entryType: String, imagePath: String? = null, addToMaster: Boolean = false, color: String = "#800000") {
        viewModelScope.launch {
            try {
                repository.addEntry(name, entryType, quantity, notes, listId, imagePath, addToMaster, color)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }


    // ------------------ UPDATE ENTRY ------------------

    fun updateEntry(
        entryId: Int,
        name: String,
        quantity: Int,
        notes: String?,
        entryType: String,
        listId: Int,
        imagePath: String? = null,
        color: String = "#800000"
    ) {
        viewModelScope.launch {
            try {
                repository.updateEntry(entryId, name, quantity, notes, entryType, imagePath, color)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }


    // ------------------ DELETE ENTRY ------------------

    fun deleteEntry(entryId: Int, listId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteEntry(entryId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }


    // ------------------ TOGGLE ENTRY CHECKBOX ------------------

    fun toggleEntry(entryId: Int, checked: Boolean, listId: Int) {
        val newCheckedValue = if (checked) 1 else 0
        viewModelScope.launch {
            try {
                repository.toggleEntry(entryId, newCheckedValue)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }
}
