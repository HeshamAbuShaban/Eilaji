package com.eilaji.backend.initialization

import com.eilaji.backend.data.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID
import org.mindrot.jbcrypt.BCrypt

object DatabaseSeeder {

    fun seedIfEmpty() {
        transaction {
            val categoriesCount = Categories.selectAll().count()
            if (categoriesCount > 0) {
                println("INFO: Database already has data, skipping seed")
                return@transaction
            }
            println("INFO: Seeding database with test data...")
            seedUsers()
            val categoryIds = seedCategories()
            val subcategoryIds = seedSubcategories(categoryIds)
            seedMedicines(subcategoryIds)
            seedPharmacies()
            println("INFO: Database seeding completed")
        }
    }

    private fun seedUsers() {
        println("Seeding users...")
        val users = listOf(
            Triple(UUID.randomUUID().toString(), "pharmacist1@eilaji.com", "PHARMACIST"),
            Triple(UUID.randomUUID().toString(), "pharmacist2@eilaji.com", "PHARMACIST"),
            Triple(UUID.randomUUID().toString(), "patient@eilaji.com", "PATIENT"),
            Triple(UUID.randomUUID().toString(), "admin@eilaji.com", "ADMIN")
        )
        users.forEach { (id, email, role) ->
            Users.insert {
                it[Users.id] = UUID.fromString(id)
                it[Users.email] = email
                it[Users.passwordHash] = BCrypt.hashpw("password123", BCrypt.gensalt())
                it[Users.fullName] = "Test User $id"
                it[Users.role] = role
                it[Users.isVerified] = true
                it[Users.isActive] = true
                it[Users.createdAt] = Instant.now()
                it[Users.updatedAt] = Instant.now()
            }
        }
        println("Seeded ${users.size} users")
    }

    private fun seedCategories(): List<UUID> {
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
        return categoryIds
    }

    private fun seedSubcategories(categoryIds: List<UUID>): List<UUID> {
        println("Seeding subcategories...")
        val subcategories = mutableListOf<UUID>()
        val defs = listOf(
            Triple(categoryIds[0], "Tablets", "أقراص"),
            Triple(categoryIds[0], "Syrup", "شراب"),
            Triple(categoryIds[1], "Capsules", "كبسولات"),
            Triple(categoryIds[1], "Injections", "حقن"),
            Triple(categoryIds[2], "Tablets", "أقراص"),
            Triple(categoryIds[2], "Gummies", "حلوى مضغ"),
            Triple(categoryIds[3], "Creams", "كريمات"),
            Triple(categoryIds[3], "Lotions", "لوشن"),
            Triple(categoryIds[4], "Tablets", "أقراص"),
            Triple(categoryIds[4], "Syrup", "شراب"),
            Triple(categoryIds[5], "Tablets", "أقراص"),
            Triple(categoryIds[5], "Probiotics", "بروبيوتيك")
        )
        defs.forEachIndexed { idx, (catId, en, ar) ->
            val id = Subcategories.insert {
                it[categoryId] = catId
                it[nameEn] = en
                it[nameAr] = ar
                it[displayOrder] = idx
                it[isActive] = true
            } get Subcategories.id
            subcategories.add(id)
        }
        println("Seeded ${subcategories.size} subcategories")
        return subcategories
    }

    private fun seedMedicines(subcategoryIds: List<UUID>) {
        println("Seeding medicines...")
        data class MedSeed(
            val titleEn: String,
            val titleAr: String,
            val descriptionEn: String,
            val manufacturer: String,
            val price: Double,
            val requiresPrescription: Boolean,
            val imageUrl: String,
            val subIdx: Int
        )
        val medicines = listOf(
            MedSeed("Paracetamol 500mg", "باراسيتامول 500مجم", "Pain reliever and fever reducer, effective for headache and mild pain", "Bayer", 5.99, false, "/images/medicines/paracetamol.jpg", 0),
            MedSeed("Ibuprofen 400mg", "إيبوبروفين 400مجم", "Non-steroidal anti-inflammatory drug for pain and inflammation", "Pfizer", 8.50, false, "/images/medicines/ibuprofen.jpg", 0),
            MedSeed("Diclofenac 50mg", "ديكلوفيناك 50مجم", "Potent NSAID for joint pain and arthritis", "Novartis", 12.30, false, "/images/medicines/diclofenac.jpg", 0),
            MedSeed("Aspirin 100mg", "أسبرين 100مجم", "Low-dose aspirin for blood thinning and pain relief", "Bayer", 4.25, false, "/images/medicines/aspirin.jpg", 1),
            MedSeed("Ketoprofen 100mg", "كيتوبروفين 100مجم", "Anti-inflammatory for musculoskeletal pain", "Sanofi", 15.75, true, "/images/medicines/ketoprofen.jpg", 1),
            MedSeed("Tramadol 50mg", "ترامادول 50مجم", "Strong analgesic for moderate to severe pain", "GSK", 22.90, true, "/images/medicines/tramadol.jpg", 0),
            MedSeed("Amoxicillin 500mg", "أموكسيسيلين 500مجم", "Broad-spectrum penicillin antibiotic", "GSK", 9.99, true, "/images/medicines/amoxicillin.jpg", 2),
            MedSeed("Azithromycin 250mg", "أزيثروميسين 250مجم", "Macrolide antibiotic for respiratory infections", "Pfizer", 18.50, true, "/images/medicines/azithromycin.jpg", 2),
            MedSeed("Ciprofloxacin 500mg", "سيبروفلوكساسين 500مجم", "Fluoroquinolone for bacterial infections", "Bayer", 14.20, true, "/images/medicines/ciprofloxacin.jpg", 2),
            MedSeed("Augmentin 625mg", "أوجمنتين 625مجم", "Amoxicillin/clavulanate combination antibiotic", "GSK", 24.99, true, "/images/medicines/augmentin.jpg", 2),
            MedSeed("Doxycycline 100mg", "دوكسيسيكلين 100مجم", "Tetracycline antibiotic for acne and infections", "Pfizer", 11.30, false, "/images/medicines/doxycycline.jpg", 3),
            MedSeed("Metronidazole 400mg", "ميترونيدازول 400مجم", "Antibiotic and antiprotozoal for intestinal infections", "Sanofi", 7.80, false, "/images/medicines/metronidazole.jpg", 3),
            MedSeed("Vitamin C 1000mg", "فيتامين سي 1000مجم", "Immune support and antioxidant supplement", "Hikma Pharmaceuticals", 12.99, false, "/images/medicines/vitamin_c.jpg", 4),
            MedSeed("Vitamin D3 5000IU", "فيتامين د3 5000 وحدة", "Bone health and immune system support", "Pfizer", 16.50, false, "/images/medicines/vitamin_d3.jpg", 4),
            MedSeed("Omega-3 Fish Oil 1000mg", "أوميغا 3 زيت السمك 1000مجم", "Heart and brain health omega-3 supplement", "Novartis", 25.99, false, "/images/medicines/omega3.jpg", 5),
            MedSeed("Centrum Multivitamin", "سنتروم متعدد الفيتامينات", "Complete daily multivitamin for adults", "Pfizer", 28.75, false, "/images/medicines/centrum.jpg", 5),
            MedSeed("Calcium + Magnesium 500mg", "كالسيوم + مغنيسيوم 500مجم", "Bone strength and muscle function support", "Bayer", 18.20, false, "/images/medicines/calcium_magnesium.jpg", 4),
            MedSeed("Zinc 50mg", "زنك 50مجم", "Immune support and wound healing mineral", "Teva", 9.40, false, "/images/medicines/zinc.jpg", 4),
            MedSeed("Hydrocortisone Cream 1%", "كريم هيدروكورتيزون 1%", "Topical corticosteroid for eczema and dermatitis", "Bayer", 13.50, true, "/images/medicines/hydrocortisone.jpg", 6),
            MedSeed("Cetaphil Moisturizer 250ml", "سيتافيل مرطب 250مل", "Gentle daily moisturizer for sensitive skin", "Johnson & Johnson", 19.99, false, "/images/medicines/cetaphil.jpg", 7),
            MedSeed("Eucerin Cream 150ml", "يوسيرين كريم 150مل", "Intensive repair cream for very dry skin", "Novartis", 21.30, false, "/images/medicines/eucerin.jpg", 6),
            MedSeed("Differin Gel 0.1% 30g", "ديفرين جل 0.1% 30غ", "Adapalene gel for acne treatment", "GSK", 32.00, true, "/images/medicines/differin.jpg", 6),
            MedSeed("Nivea Soft Cream 200ml", "نيفيا سوفت كريم 200مل", "Light moisturizing cream for face and body", "Bayer", 7.99, false, "/images/medicines/nivea_soft.jpg", 7),
            MedSeed("Bioderma Sunscreen SPF50", "بيوديرما واقي شمس SPF50", "High protection sunscreen for sensitive skin", "Hikma Pharmaceuticals", 26.50, false, "/images/medicines/bioderma_sunscreen.jpg", 7),
            MedSeed("Panadol Cold & Flu", "بنادول كولد اند فلو", "Relief for cold, flu and fever symptoms", "GSK", 6.99, false, "/images/medicines/panadol_cold_flu.jpg", 8),
            MedSeed("Loratadine 10mg", "لوراتادين 10مجم", "Non-drowsy antihistamine for allergies", "Bayer", 8.99, false, "/images/medicines/loratadine.jpg", 8),
            MedSeed("Cetirizine 10mg", "سيتيريزين 10مجم", "Antihistamine for hay fever and hives", "Pfizer", 7.50, false, "/images/medicines/cetirizine.jpg", 8),
            MedSeed("Oseltamivir 75mg", "أوسيلتاميفير 75مجم", "Antiviral for influenza A and B", "Roche", 45.99, true, "/images/medicines/oseltamivir.jpg", 8),
            MedSeed("Dextromethorphan Syrup 100ml", "ديكستروميثورفان شراب 100مل", "Cough suppressant for dry cough", "Cipla", 6.75, false, "/images/medicines/dextromethorphan.jpg", 9),
            MedSeed("Strepsils Lozenges Honey", "ستربسلز أقراص عسل", "Soothing lozenges for sore throat", "Merck", 3.50, false, "/images/medicines/strepsils.jpg", 9),
            MedSeed("Omeprazole 20mg", "أوميبرازول 20مجم", "Proton pump inhibitor for acid reflux", "AstraZeneca", 10.99, true, "/images/medicines/omeprazole.jpg", 10),
            MedSeed("Nexium 40mg", "نيكسيوم 40مجم", "Esomeprazole for GERD and ulcers", "AstraZeneca", 35.40, true, "/images/medicines/nexium.jpg", 10),
            MedSeed("Gaviscon Syrup 200ml", "جافيسكون شراب 200مل", "Antacid for heartburn and indigestion", "Merck", 11.20, false, "/images/medicines/gaviscon.jpg", 10),
            MedSeed("Buscopan 10mg", "بوسكوبان 10مجم", "Antispasmodic for abdominal cramps", "Sanofi", 9.80, false, "/images/medicines/buscopan.jpg", 10),
            MedSeed("Dulcolax 5mg", "دولكولاكس 5مجم", "Laxative for constipation relief", "Sanofi", 5.20, false, "/images/medicines/dulcolax.jpg", 10),
            MedSeed("Enterogermina Probiotics 10 vials", "إنتروجرمينا بروبيوتيك 10 عبوات", "Probiotic for gut flora balance and diarrhea", "Merck", 18.90, false, "/images/medicines/enterogermina.jpg", 11)
        )
        medicines.forEach { m ->
            Medicines.insert {
                it[titleEn] = m.titleEn
                it[titleAr] = m.titleAr
                it[descriptionEn] = m.descriptionEn
                it[descriptionAr] = m.descriptionEn
                it[manufacturer] = m.manufacturer
                it[requiresPrescription] = m.requiresPrescription
                it[price] = m.price.toBigDecimal()
                it[isActive] = true
                it[subcategoryId] = subcategoryIds[m.subIdx]
                it[imageUrl] = m.imageUrl
            }
        }
        println("Seeded ${medicines.size} medicines")
    }

    private fun seedPharmacies() {
        println("Seeding pharmacies...")
        val pharmacistIds = Users.selectAll().where { Users.role eq "PHARMACIST" }.map { it[Users.id] }
        val primaryOwner = pharmacistIds.firstOrNull() ?: UUID.randomUUID()
        val secondaryOwner = if (pharmacistIds.size > 1) pharmacistIds[1] else primaryOwner
        data class PharmSeed(
            val name: String,
            val address: String,
            val city: String,
            val lat: Double,
            val lng: Double,
            val phone: String,
            val isOpen: Boolean,
            val ratingAvg: Double,
            val totalRatings: Int,
            val license: String,
            val owner: UUID
        )
        val pharmacies = listOf(
            PharmSeed("Al-Shifa Pharmacy", "123 Main St, Damascus", "Damascus", 33.5138, 36.2765, "+963-11-1234567", true, 4.8, 150, "LIC-001", primaryOwner),
            PharmSeed("Al-Hayat Pharmacy", "456 Oak Ave, Aleppo", "Aleppo", 36.2021, 37.1343, "+963-21-7654321", true, 4.5, 120, "LIC-002", primaryOwner),
            PharmSeed("Al-Noor Pharmacy", "Al-Hamra St, Homs", "Homs", 34.7260, 36.7230, "+963-31-2345678", true, 4.2, 85, "LIC-003", secondaryOwner),
            PharmSeed("Al-Amal Pharmacy", "Corniche St, Latakia", "Latakia", 35.5407, 35.7890, "+963-41-3456789", false, 4.6, 95, "LIC-004", secondaryOwner),
            PharmSeed("Al-Salam Pharmacy", "Al-Assi Sq, Hama", "Hama", 35.1318, 36.7578, "+963-33-4567890", true, 3.9, 60, "LIC-005", primaryOwner),
            PharmSeed("CityCare Pharmacy", "Mazza Autostrad, Damascus", "Damascus", 33.5100, 36.2900, "+963-11-9876543", true, 4.4, 110, "LIC-006", secondaryOwner),
            PharmSeed("Aleppo Central Pharmacy", "Old City, Aleppo", "Aleppo", 36.2150, 37.0800, "+963-21-8765432", false, 3.7, 45, "LIC-007", primaryOwner),
            PharmSeed("Homs Modern Pharmacy", "Al-Dablan St, Homs", "Homs", 34.7330, 36.7110, "+963-31-7654321", true, 4.1, 70, "LIC-008", secondaryOwner),
            PharmSeed("Latakia Seaside Pharmacy", "Azhari St, Latakia", "Latakia", 35.5230, 35.7910, "+963-41-6543210", true, 3.5, 30, "LIC-009", primaryOwner),
            PharmSeed("Hama Old Town Pharmacy", "Al-Hader St, Hama", "Hama", 35.1420, 36.7520, "+963-33-5432109", false, 4.0, 55, "LIC-010", secondaryOwner)
        )
        pharmacies.forEach { p ->
            Pharmacies.insert {
                it[ownerUserId] = p.owner
                it[name] = p.name
                it[address] = p.address
                it[city] = p.city
                it[latitude] = p.lat
                it[longitude] = p.lng
                it[phone] = p.phone
                it[isVerified] = true
                it[isOpen] = p.isOpen
                it[openingHours] = "9:00-22:00"
                it[licenseNumber] = p.license
                it[ratingAvg] = p.ratingAvg.toBigDecimal()
                it[totalRatings] = p.totalRatings
            }
        }
        println("Seeded ${pharmacies.size} pharmacies")
    }
}
