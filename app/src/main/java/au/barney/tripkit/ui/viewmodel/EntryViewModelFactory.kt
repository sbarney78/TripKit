package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import au.barney.tripkit.data.repository.TripKitRepository

class EntryViewModelFactory(private val repository: TripKitRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EntryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EntryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
