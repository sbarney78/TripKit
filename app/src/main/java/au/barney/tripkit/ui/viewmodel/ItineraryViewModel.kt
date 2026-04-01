package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.barney.tripkit.data.model.ItineraryItem
import au.barney.tripkit.data.model.ListItem
import au.barney.tripkit.data.repository.TripKitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ItineraryViewModel(
    private val repository: TripKitRepository
) : ViewModel() {

    private val _itinerary = MutableStateFlow<List<ItineraryItem>>(emptyList())
    val itinerary: StateFlow<List<ItineraryItem>> = _itinerary

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _currentList = MutableStateFlow<ListItem?>(null)
    val currentList: StateFlow<ListItem?> = _currentList

    fun loadItinerary(listId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _currentList.value = repository.getList(listId)
            } catch (e: Exception) {
                _error.value = e.message
            }

            repository.getItinerary(listId)
                .catch { e ->
                    _error.value = e.message ?: "Unknown error"
                    _loading.value = false
                }
                .collect { items ->
                    _itinerary.value = items
                    _loading.value = false
                }
        }
    }

    fun addItem(
        listId: Int,
        day: String,
        time: String,
        activity: String,
        notes: String?,
        location: String?,
        price: Double?,
        departureDay: String?,
        departureTime: String?,
        category: String?,
        bookingRef: String?,
        showOnMap: Boolean
    ) {
        viewModelScope.launch {
            try {
                repository.addItineraryItem(listId, day, time, activity, notes, location, price, departureDay, departureTime, category, bookingRef, showOnMap)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun updateItem(item: ItineraryItem) {
        viewModelScope.launch {
            try {
                repository.updateItineraryItem(item)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }

    fun deleteItem(itemId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteItineraryItem(itemId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }
}
