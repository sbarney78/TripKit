package au.barney.tripkit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.barney.tripkit.data.model.Template
import au.barney.tripkit.data.model.TemplateEntry
import au.barney.tripkit.data.model.TemplateItem
import au.barney.tripkit.data.model.TemplateSubItem
import au.barney.tripkit.data.repository.TripKitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TemplateViewModel(private val repository: TripKitRepository) : ViewModel() {

    private val _templates = MutableStateFlow<List<Template>>(emptyList())
    val templates: StateFlow<List<Template>> = _templates

    private val _templateEntries = MutableStateFlow<List<TemplateEntry>>(emptyList())
    val templateEntries: StateFlow<List<TemplateEntry>> = _templateEntries

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadTemplates()
    }

    fun loadTemplates() {
        viewModelScope.launch {
            _loading.value = true
            repository.getTemplates()
                .catch { e -> _error.value = e.message }
                .collect { 
                    _templates.value = it
                    _loading.value = false
                }
        }
    }

    fun createTemplate(name: String, selectedMasterItemIds: List<Int>) {
        viewModelScope.launch {
            try {
                repository.createTemplateFromSelection(name, selectedMasterItemIds)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteTemplate(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteTemplate(id)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadTemplateEntries(templateId: Int) {
        viewModelScope.launch {
            repository.getTemplateEntries(templateId)
                .catch { e -> _error.value = e.message }
                .collect { _templateEntries.value = it }
        }
    }

    fun addFromMaster(templateId: Int, masterItemId: Int) {
        viewModelScope.launch {
            try {
                repository.addTemplateEntry(templateId, masterItemId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun getTemplateItems(entryId: Int) = repository.getTemplateItems(entryId)
    fun getTemplateSubItems(itemId: Int) = repository.getTemplateSubItems(itemId)

    fun toggleTemplateEntry(id: Int, checked: Boolean) {
        viewModelScope.launch {
            repository.toggleTemplateEntry(id, if (checked) 1 else 0)
        }
    }

    fun toggleTemplateItem(id: Int, checked: Boolean) {
        viewModelScope.launch {
            repository.toggleTemplateItem(id, if (checked) 1 else 0)
        }
    }

    fun toggleTemplateSubItem(id: Int, checked: Boolean) {
        viewModelScope.launch {
            repository.toggleTemplateSubItem(id, if (checked) 1 else 0)
        }
    }

    fun deleteTemplateEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteTemplateEntry(id)
        }
    }

    fun deleteTemplateItem(id: Int) {
        viewModelScope.launch {
            repository.deleteTemplateItem(id)
        }
    }

    fun deleteTemplateSubItem(id: Int) {
        viewModelScope.launch {
            repository.deleteTemplateSubItem(id)
        }
    }
}
