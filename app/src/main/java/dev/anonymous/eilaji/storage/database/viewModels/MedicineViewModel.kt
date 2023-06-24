package dev.anonymous.eilaji.storage.database.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dev.anonymous.eilaji.storage.database.Repository
import dev.anonymous.eilaji.storage.database.tables.Medicine
import kotlinx.coroutines.launch

class MedicineViewModel(private val repository: Repository) : ViewModel() {
    val allMedicines = repository.getAllMedicines().asLiveData()

    fun insertMedicine(medicine: Medicine) {
        viewModelScope.launch {
            repository.insertMedicine(medicine)
        }
    }

    fun deleteMedicine(medicineId: Int) {
        viewModelScope.launch {
            repository.deleteMedicine(medicineId)
        }
    }
}

