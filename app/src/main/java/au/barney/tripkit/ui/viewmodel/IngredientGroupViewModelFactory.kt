package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import au.barney.tripkit.data.repository.TripKitRepository

class IngredientGroupViewModelFactory(
    private val repository: TripKitRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IngredientGroupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IngredientGroupViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
