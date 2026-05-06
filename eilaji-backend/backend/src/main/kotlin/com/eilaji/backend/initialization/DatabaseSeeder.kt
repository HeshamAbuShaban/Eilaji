package com.eilaji.backend.initialization

import com.eilaji.backend.data.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID
import org.mindrot.jbcrypt.BCrypt

object DatabaseSeeder {

    fun seedIfEmpty() {
        transaction {
            // Check if data already exists
            val categoriesCount = Categories.selectAll().count()
            if (categoriesCount > 0) {
                println("INFO: Database already has data, skipping seed")
                return@transaction
            }

            println("INFO: Seeding database with test data...")
            seedUsers()
            seedCategories()
            seedMedicines()
            seedPharmacies()
            println("INFO: Database seeding completed")
        }
    }

    private fun seedUsers() {
        println("Seeding users...")
        val users = listOf(
            Triple("test-owner-1", "pharmacist1@eilaji.com", "PHARMACIST"),
            Triple("test-owner-2", "pharmacist2@eilaji.com", "PHARMACIST"),
            Triple("test-patient-1", "patient@eilaji.com", "PATIENT"),
            Triple("test-admin", "admin@eilaji.com", "ADMIN")
        )
        users.forEach { (id, email, role) ->
            Users.insert {
                it[Users.id] = id
                it[Users.email] = email
                it[Users.passwordHash] = BCrypt.hashpw("password123", BCrypt.gensalt())
                it[Users.fullName] = "Test User ${id}"
                it[Users.role] = role
                it[Users.isVerified] = true
                it[Users.isActive] = true
                it[Users.createdAt] = Instant.now()
                it[Users.updatedAt] = Instant.now()
            }
        }
        println("Seeded ${users.size} users")
    }

    private fun seedCategories() {
        println("Seeding categories...")
        val categoryIds = listOf(
            Categories.insert {
                it[nameEn] = "Pain Relievers"
                it[nameAr] = "مسكنات الألم"
            } get Categories.id,

            Categories.insert {
                it[nameEn] = "Antibiotics"
                it[nameAr] = "المضادات الحيوية"
            } get Categories.id,

            Categories.insert {
                it[nameEn] = "Vitamins & Supplements"
                it[nameAr] = "الفيتامينات والمكملات"
            } get Categories.id,

            Categories.insert {
                it[nameEn] = "Skin Care"
                it[nameAr] = "العناية بالبشرة"
            } get Categories.id,

            Categories.insert {
                it[nameEn] = "Cold & Flu"
                it[nameAr] = "البرد والإنفلونزا"
            } get Categories.id,

            Categories.insert {
                it[nameEn] = "Digestive Health"
                it[nameAr] = "صحة الجهاز الهضمي"
            } get Categories.id
        )
        println("Seeded ${categoryIds.size} categories")
    }

    private fun seedMedicines() {
        println("Seeding medicines...")
        // Get first category for examples
        val firstCategoryId = Categories.selectAll().first()[Categories.id]

        val medicineIds = listOf(
            Medicines.insert {
                it[titleEn] = "Paracetamol 500mg"
                it[titleAr] = "باراسيتامول 500مجم"
                it[description] = "Pain reliever and fever reducer"
                it[categoryId] = firstCategoryId
                it[manufacturer] = "PharmaCorp"
                it[requiresPrescription] = false
                it[price] = 5.99.toBigDecimal()
                it[isActive] = true
            } get Medicines.id,

            Medicines.insert {
                it[titleEn] = "Ibuprofen 400mg"
                it[titleAr] = "إيبوبروفين 400مجم"
                it[description] = "Anti-inflammatory pain reliever"
                it[categoryId] = firstCategoryId
                it[manufacturer] = "MedLife"
                it[requiresPrescription] = false
                it[price] = 8.50.toBigDecimal()
                it[isActive] = true
            } get Medicines.id
        )
        println("Seeded ${medicineIds.size} medicines")
    }

    private fun seedPharmacies() {
        println("Seeding pharmacies...")
        val pharmacyIds = listOf(
            Pharmacies.insert {
                it[ownerId] = "test-owner-1"
                it[name] = "Al-Shifa Pharmacy"
                it[address] = "123 Main St, Damascus"
                it[city] = "Damascus"
                it[latitude] = 33.5138
                it[longitude] = 36.2765
                it[phone] = "+963-11-1234567"
                it[isVerified] = true
                it[isOpen] = true
                it[openingHours] = "9:00-22:00"
                it[licenseNumber] = "LIC-001"
            } get Pharmacies.id,

            Pharmacies.insert {
                it[ownerId] = "test-owner-2"
                it[name] = "Al-Hayat Pharmacy"
                it[address] = "456 Oak Ave, Aleppo"
                it[city] = "Aleppo"
                it[latitude] = 36.2021
                it[longitude] = 37.1343
                it[phone] = "+963-21-7654321"
                it[isVerified] = true
                it[isOpen] = true
                it[openingHours] = "8:00-21:00"
                it[licenseNumber] = "LIC-002"
            } get Pharmacies.id
        )
        println("Seeded ${pharmacyIds.size} pharmacies")
    }
}
