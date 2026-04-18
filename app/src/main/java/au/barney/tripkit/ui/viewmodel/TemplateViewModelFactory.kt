package au.barney.tripkit.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import au.barney.tripkit.data.repository.TripKitRepository
import au.barney.tripkit.util.PremiumManager

class TemplateViewModelFactory(
    private val repository: TripKitRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TemplateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val isPremium = PremiumManager.isPremium(context)
            return TemplateViewModel(repository, isPremium) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}