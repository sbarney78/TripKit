package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import au.barney.tripkit.data.repository.TripKitRepository

class MenuViewModelFactory(private val repository: TripKitRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MenuViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
