package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import au.barney.tripkit.data.repository.TripKitRepository

class MasterItemViewModelFactory(
    private val repository: TripKitRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MasterItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MasterItemViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}