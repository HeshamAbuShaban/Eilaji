package dev.anonymous.eilaji.storage.database.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dev.anonymous.eilaji.storage.database.Repository
import dev.anonymous.eilaji.storage.database.tables.FavoriteMedicine
import dev.anonymous.eilaji.storage.database.tables.Medicine
import dev.anonymous.eilaji.storage.database.tables.Patient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PatientViewModel(private val repository: Repository) : ViewModel() {
    val allPatients = repository.getAllPatients().asLiveData()

    fun insertPatient(patient: Patient) {
        viewModelScope.launch {
            repository.insertPatient(patient)
        }
    }

    fun deletePatient(patientId: Int) {
        viewModelScope.launch {
            repository.deletePatient(patientId)
        }
    }

    fun getFavoriteMedicinesForPatient(patientId: Int): Flow<List<Medicine>> {
        return repository.getFavoriteMedicinesForPatient(patientId)
    }

    fun insertFavoriteMedicine(favoriteMedicine: FavoriteMedicine) {
        viewModelScope.launch {
            repository.insertFavoriteMedicine(favoriteMedicine)
        }
    }

    fun deleteFavoriteMedicine(patientId: Int, medicineId: Int) {
        viewModelScope.launch {
            repository.deleteFavoriteMedicine(patientId, medicineId)
        }
    }
}

