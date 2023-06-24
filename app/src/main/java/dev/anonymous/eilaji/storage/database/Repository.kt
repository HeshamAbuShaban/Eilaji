package dev.anonymous.eilaji.storage.database

import dev.anonymous.eilaji.storage.database.daos.CategoryDao
import dev.anonymous.eilaji.storage.database.daos.FavoriteMedicineDao
import dev.anonymous.eilaji.storage.database.daos.MedicineDao
import dev.anonymous.eilaji.storage.database.daos.PatientDao
import dev.anonymous.eilaji.storage.database.tables.Category
import dev.anonymous.eilaji.storage.database.tables.FavoriteMedicine
import dev.anonymous.eilaji.storage.database.tables.Medicine
import dev.anonymous.eilaji.storage.database.tables.Patient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class Repository(
    private val categoryDao: CategoryDao,
    private val medicineDao: MedicineDao,
    private val patientDao: PatientDao,
    private val favoriteMedicineDao: FavoriteMedicineDao
) {
    // Category-related operations
    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }

    fun getCategoryById(categoryId: Int): Flow<Category> {
        return categoryDao.getCategoryById(categoryId)
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(categoryId: Int) {
        categoryDao.deleteCategory(categoryId)
    }

    // Medicine-related operations
    fun getAllMedicines(): Flow<List<Medicine>> {
        return medicineDao.getAllMedicines()
    }

    fun getMedicineById(medicineId: Int): Flow<Medicine> {
        return medicineDao.getMedicineById(medicineId)
    }

    suspend fun insertMedicine(medicine: Medicine) {
        medicineDao.insertMedicine(medicine)
    }

    suspend fun deleteMedicine(medicineId: Int) {
        medicineDao.deleteMedicine(medicineId)
    }

    // Patient-related operations
    fun getAllPatients(): Flow<List<Patient>> {
        return patientDao.getAllPatients()
    }

    fun getPatientById(patientId: Int): Flow<Patient> {
        return patientDao.getPatientById(patientId)
    }

    suspend fun insertPatient(patient: Patient) {
        patientDao.insertPatient(patient)
    }

    suspend fun deletePatient(patientId: Int) {
        patientDao.deletePatient(patientId)
    }

    // FavoriteMedicine-related operations
    fun getFavoriteMedicinesForPatient(patientId: Int): Flow<List<Medicine>> {
        return favoriteMedicineDao.getFavoriteMedicinesForPatient(patientId)
    }

    suspend fun insertFavoriteMedicine(favoriteMedicine: FavoriteMedicine) {
        favoriteMedicineDao.insertFavoriteMedicine(favoriteMedicine)
    }

    suspend fun deleteFavoriteMedicine(patientId: Int, medicineId: Int) {
        favoriteMedicineDao.deleteFavoriteMedicine(patientId, medicineId)
    }
}
