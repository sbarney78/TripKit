package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.barney.tripkit.data.model.Entry
import au.barney.tripkit.data.model.ListItem
import au.barney.tripkit.data.repository.TripKitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class EntryViewModel(
    val repository: TripKitRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _currentEntry = MutableStateFlow<Entry?>(null)
    val currentEntry: StateFlow<Entry?> = _currentEntry

    private val _currentList = MutableStateFlow<ListItem?>(null)
    val currentList: StateFlow<ListItem?> = _currentList


    // ------------------ LOAD ENTRIES ------------------

    fun loadEntries(listId: Int) {
        viewModelScope.launch {
            _loading.value = true
            
            try {
                _currentList.value = repository.getList(listId)
            } catch (e: Exception) {
                _error.value = e.message
            }

            repository.getEntries(listId)
                .catch { e ->
                    _error.value = e.message ?: "Unknown error"
                    _loading.value = false
                }
                .collect { entriesList ->
                    _entries.value = entriesList
                    _loading.value = false
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

    fun addEntry(listId: Int, name: String, quantity: Int, notes: String?, entryType: String) {
        viewModelScope.launch {
            try {
                repository.addEntry(name, entryType, quantity, notes, listId)
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
        listId: Int
    ) {
        viewModelScope.launch {
            try {
                repository.updateEntry(entryId, name, quantity, notes, entryType)
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
