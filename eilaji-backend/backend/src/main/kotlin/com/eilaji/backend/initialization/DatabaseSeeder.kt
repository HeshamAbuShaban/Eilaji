package com.eilaji.backend.service

import com.eilaji.backend.data.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

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
            seedCategories()
            seedSubcategories()
            seedMedicines()
            seedPharmacies()
            println("INFO: Database seeding completed")
        }
    }

    private fun seedCategories() {
        val categories = listOf(
            Triple("Pain Relief", "مسكنات الآلام", "ic_pain_relief"),
            Triple("Antibiotics", "المضادات الحيوية", "ic_antibiotics"),
            Triple("Vitamins & Supplements", "الفيتامينات والمكملات", "ic_vitamins"),
            Triple("Chronic Diseases", "الأمراض المزمنة", "ic_chronic"),
            Triple("First Aid", "الإسعافات الأولية", "ic_first_aid"),
            Triple("Skin Care", "العناية بالبشرة", "ic_skin_care"),
            Triple("Cold & Flu", "نزلات البرد والإنفلونزا", "ic_cold_flu")
        )

        categories.forEach { (nameEn, nameAr, icon) ->
            Categories.insert {
                it[id] = UUID.randomUUID()
                it[nameEn] = nameEn
                it[nameAr] = nameAr
                it[iconUrl] = "/icons/$icon.png"
                it[isActive] = true
                it[createdAt] = java.sql.Timestamp.from(Instant.now())
            }
        }
        println("INFO: Seeded ${categories.size} categories")
    }

    private fun seedSubcategories() {
        transaction {
            val painRelief = Categories.select { Categories.nameEn eq "Pain Relief" }.first()
            val antibiotics = Categories.select { Categories.nameEn eq "Antibiotics" }.first()
            val vitamins = Categories.select { Categories.nameEn eq "Vitamins & Supplements" }.first()
            val chronic = Categories.select { Categories.nameEn eq "Chronic Diseases" }.first()
            val coldFlu = Categories.select { Categories.nameEn eq "Cold & Flu" }.first()
            val skinCare = Categories.select { Categories.nameEn eq "Skin Care" }.first()

            val subcategories = listOf(
                // Pain Relief subcategories
                Pair(painRelief[Categories.id], Triple("Headache", "الصداع", "ic_headache")),
                Pair(painRelief[Categories.id], Triple("Muscle Pain", "آلام العضلات", "ic_muscle_pain")),
                Pair(painRelief[Categories.id], Triple("Joint Pain", "آلام المفاصل", "ic_joint_pain")),

                // Antibiotics subcategories
                Pair(antibiotics[Categories.id], Triple("Penicillins", "البنسيلينات", "ic_penicillin")),
                Pair(antibiotics[Categories.id], Triple("Macrolides", "المضادات العريضة", "ic_macrolide")),
                Pair(antibiotics[Categories.id], Triple("Cephalosporins", "السيفالوسبورينات", "ic_cephalosporin")),

                // Vitamins subcategories
                Pair(vitamins[Categories.id], Triple("Multivitamins", "متعددة الفيتامينات", "ic_multivitamin")),
                Pair(vitamins[Categories.id], Triple("Vitamin C", "فيتامين C", "ic_vit_c")),
                Pair(vitamins[Categories.id], Triple("Vitamin D", "فيتامين D", "ic_vit_d")),

                // Chronic Diseases subcategories
                Pair(chronic[Categories.id], Triple("Diabetes", "السكري", "ic_diabetes")),
                Pair(chronic[Categories.id], Triple("Hypertension", "ضغط الدم", "ic_hypertension")),
                Pair(chronic[Categories.id], Triple("Asthma", "الربو", "ic_asthma")),

                // Cold & Flu subcategories
                Pair(coldFlu[Categories.id], Triple("Cough Syrups", "أدوية السعال", "ic_cough")),
                Pair(coldFlu[Categories.id], Triple("Nasal Sprays", "بخاخات الأنف", "ic_nasal")),
                Pair(coldFlu[Categories.id], Triple("Throat Lozenges", "أقراص الحلق", "ic_throat")),

                // Skin Care subcategories
                Pair(skinCare[Categories.id], Triple("Moisturizers", "المرطبات", "ic_moisturizer")),
                Pair(skinCare[Categories.id], Triple("Acne Treatment", "علاج حب الشباب", "ic_acne")),
                Pair(skinCare[Categories.id], Triple("Sun Protection", "الحماية من الشمس", "ic_sunscreen"))
            )

            subcategories.forEach { (categoryId, triple) ->
                Subcategories.insert {
                    it[id] = UUID.randomUUID()
                    it[Subcategories.categoryId] = categoryId
                    it[nameEn] = triple.first
                    it[nameAr] = triple.second
                    it[iconUrl] = "/icons/${triple.third}.png"
                    it[isActive] = true
                    it[createdAt] = java.sql.Timestamp.from(Instant.now())
                }
            }
            println("INFO: Seeded ${subcategories.size} subcategories")
        }
    }

    private fun seedMedicines() {
        transaction {
            val painRelief = Categories.select { Categories.nameEn eq "Pain Relief" }.first()
            val antibiotics = Categories.select { Categories.nameEn eq "Antibiotics" }.first()
            val vitamins = Categories.select { Categories.nameEn eq "Vitamins & Supplements" }.first()

            val medicines = listOf(
                // Pain Relief medicines
                MedicineData("Panadol Extra", "بانادول إكسترا", "Pain relief for headaches and muscle pain", "GSK", painRelief[Categories.id], null, 12.50, false),
                MedicineData("Brufen 400mg", "بروفين 400 مجم", "Anti-inflammatory and pain relief", "Abbott", painRelief[Categories.id], null, 8.75, false),
                MedicineData("Aspirin 100mg", "أسبرين 100 مجم", "Blood thinner and pain relief", "Bayer", painRelief[Categories.id], null, 5.25, false),

                // Antibiotics
                MedicineData("Augmentin 1g", "أجمنتين 1 جرام", "Broad-spectrum antibiotic", "GSK", antibiotics[Categories.id], null, 45.00, true),
                MedicineData("Zithromax 500mg", "زيثروماكس 500 مجم", "Macrolide antibiotic", "Pfizer", antibiotics[Categories.id], null, 38.50, true),
                MedicineData("Cephalexin 500mg", "سيفالكسين 500 مجم", "Cephalosporin antibiotic", "Sandoz", antibiotics[Categories.id], null, 25.00, true),

                // Vitamins
                MedicineData("Centrum", "سنترم", "Multivitamin supplement", "Pfizer", vitamins[Categories.id], null, 32.00, false),
                MedicineData("Vitamin C 1000mg", "فيتامين C 1000 مجم", "Immune system support", "Now Foods", vitamins[Categories.id], null, 15.50, false),
                MedicineData("Vitamin D3 5000 IU", "فيتامين D3 5000 وحدة", "Bone health supplement", "Nature Made", vitamins[Categories.id], null, 18.75, false)
            )

            medicines.forEach { med ->
                Medicines.insert {
                    it[id] = UUID.randomUUID()
                    it[titleEn] = med.titleEn
                    it[titleAr] = med.titleAr
                    it[description] = med.description
                    it[categoryId] = med.categoryId
                    it[subcategoryId] = med.subcategoryId
                    it[manufacturer] = med.manufacturer
                    it[requiresPrescription] = med.requiresPrescription
                    it[price] = med.price?.toBigDecimal()
                    it[isActive] = true
                    it[createdAt] = java.sql.Timestamp.from(Instant.now())
                    it[updatedAt] = java.sql.Timestamp.from(Instant.now())
                }
            }
            println("INFO: Seeded ${medicines.size} medicines")
        }
    }

    private fun seedPharmacies() {
        val pharmacies = listOf(
            PharmacyData("Al Noor Pharmacy", "صيدلية النور", "123 King Fahd Road", "Riyadh", 24.7136, 46.6753, "0112345678", true, true),
            PharmacyData("Al Hawi Pharmacy", "صيدلية الهواي", "456 Prince Sultan St", "Jeddah", 21.5433, 39.1728, "0123456789", true, true),
            PharmacyData("Al Jazeera Pharmacy", "صيدلية الجزيرة", "789 Al Medina Road", "Mecca", 21.3891, 39.8579, "0134567890", false, true),
            PharmacyData("Al Shifa Pharmacy", "صيدلية الشفاء", "321 King Abdulaziz St", "Dammam", 26.3927, 49.9777, "0145678901", true, false),
            PharmacyData("Al Marwa Pharmacy", "صيدلية المروة", "654 Palestine St", "Medina", 24.5247, 39.5692, "0156789012", true, true)
        )

        pharmacies.forEach { ph ->
            Pharmacies.insert {
                it[id] = UUID.randomUUID()
                it[name] = ph.nameEn
                it[description] = ph.nameAr
                it[address] = ph.address
                it[city] = ph.city
                it[latitude] = ph.latitude
                it[longitude] = ph.longitude
                it[phone] = ph.phone
                it[isVerified] = ph.isVerified
                it[isOpen] = ph.isOpen
                it[isActive] = true
                it[createdAt] = java.sql.Timestamp.from(Instant.now())
            }
        }
        println("INFO: Seeded ${pharmacies.size} pharmacies")
    }

    data class MedicineData(
        val titleEn: String,
        val titleAr: String,
        val description: String?,
        val manufacturer: String?,
        val categoryId: UUID,
        val subcategoryId: UUID?,
        val price: Double?,
        val requiresPrescription: Boolean
    )

    data class PharmacyData(
        val nameEn: String,
        val nameAr: String,
        val address: String,
        val city: String,
        val latitude: Double,
        val longitude: Double,
        val phone: String,
        val isVerified: Boolean,
        val isOpen: Boolean
    )
}