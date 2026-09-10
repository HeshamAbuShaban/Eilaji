package com.eilaji.backend.initialization

import com.eilaji.backend.data.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID
import org.mindrot.jbcrypt.BCrypt

object DatabaseSeeder {

    fun seedIfEmpty() {
        transaction {
            val categoriesCount = Categories.selectAll().count()
            val pharmaciesCount = try { Pharmacies.selectAll().count() } catch (_: Exception) { 0 }
            if (categoriesCount > 0 && pharmaciesCount >= 30) {
                println("INFO: Database already has data, skipping seed")
                return@transaction
            }
            if (categoriesCount == 0L) {
                println("INFO: Seeding database with test data...")
                seedUsers()
                val categoryIds = seedCategories()
                val subcategoryIds = seedSubcategories(categoryIds)
                seedMedicines(subcategoryIds)
                seedPharmacies()
                seedChats()
                println("INFO: Database seeding completed")
                return@transaction
            }
            if (pharmaciesCount < 30) {
                println("INFO: Seeding additional pharmacies to reach 30 (current $pharmaciesCount)...")
                try { Messages.deleteAll(); Chats.deleteAll(); Pharmacies.deleteAll() } catch (_: Exception) { println("WARN: delete failed, continuing") }
                seedPharmacies()
                seedChats()
                println("INFO: Additional pharmacies seeded")
            }
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
            PharmSeed("Al-Shifa Gaza", "Al-Rimal District, Omar Al-Mukhtar St, Gaza", "Gaza", 31.4495, 34.3925, "+970-8-2841234", true, 4.8, 150, "LIC-001", primaryOwner),
            PharmSeed("Al-Quds Gaza", "Al-Nasr St, Gaza City", "Gaza", 31.4498, 34.3932, "+970-8-2841235", false, 4.5, 120, "LIC-002", secondaryOwner),
            PharmSeed("Al-Aqsa Gaza", "Al-Wehda St, Gaza", "Gaza", 31.4500, 34.3940, "+970-8-2841236", true, 4.2, 85, "LIC-003", primaryOwner),
            PharmSeed("Al-Rimal Gaza", "Al-Rashid St, Gaza Beach Camp", "Gaza", 31.4492, 34.3950, "+970-8-2841237", false, 4.6, 95, "LIC-004", secondaryOwner),
            PharmSeed("Al-Zahra Gaza", "Al-Jalaa St, Gaza", "Gaza", 31.4496, 34.3960, "+970-8-2841238", true, 3.9, 60, "LIC-005", primaryOwner),
            PharmSeed("Al-Salam Gaza", "Salah Al-Din St, Gaza", "Gaza", 31.5000, 34.4700, "+970-8-2841239", false, 4.4, 110, "LIC-006", secondaryOwner),
            PharmSeed("Al-Noor Gaza", "Al-Maghazi Camp St, Gaza", "Gaza", 31.5010, 34.4710, "+970-8-2841240", true, 3.7, 45, "LIC-007", primaryOwner),
            PharmSeed("Al-Amal Gaza", "Khan Younis Border St, Gaza", "Gaza", 31.4980, 34.4690, "+970-8-2841241", false, 4.1, 70, "LIC-008", secondaryOwner),
            PharmSeed("Al-Hayat Gaza", "Beach Road, Gaza Port", "Gaza", 31.4485, 34.3920, "+970-8-2841242", true, 3.5, 30, "LIC-009", primaryOwner),
            PharmSeed("Al-Wafa Gaza", "Al-Shifa Hospital St, Gaza", "Gaza", 31.4510, 34.3970, "+970-8-2841243", false, 4.0, 55, "LIC-010", secondaryOwner),
            PharmSeed("Al-Shifa Damascus", "123 Main St, Damascus", "Damascus", 33.5138, 36.2765, "+963-11-1234567", true, 4.7, 140, "LIC-011", primaryOwner),
            PharmSeed("Al-Hayat Aleppo", "456 Old City, Aleppo", "Aleppo", 36.2021, 37.1343, "+963-21-7654321", false, 4.3, 100, "LIC-012", secondaryOwner),
            PharmSeed("Al-Noor Homs", "Al-Hamra St, Homs", "Homs", 34.7260, 36.7230, "+963-31-2345678", true, 4.0, 80, "LIC-013", primaryOwner),
            PharmSeed("Al-Amal Latakia", "Corniche St, Latakia", "Latakia", 35.5407, 35.7890, "+963-41-3456789", false, 4.5, 90, "LIC-014", secondaryOwner),
            PharmSeed("Al-Salam Hama", "Al-Assi Sq, Hama", "Hama", 35.1318, 36.7578, "+963-33-4567890", true, 3.8, 65, "LIC-015", primaryOwner),
            PharmSeed("Cairo Central Pharmacy", "Tahrir Square, Downtown Cairo", "Cairo", 30.0444, 31.2357, "+20-2-27912345", false, 4.6, 130, "LIC-016", secondaryOwner),
            PharmSeed("Alexandria Care Pharmacy", "Corniche Road, Alexandria", "Alexandria", 31.2001, 29.9187, "+20-3-4845678", true, 4.2, 95, "LIC-017", primaryOwner),
            PharmSeed("Giza Health Pharmacy", "Pyramids Ave, Giza", "Giza", 30.0131, 31.2089, "+20-2-35678901", false, 4.4, 105, "LIC-018", secondaryOwner),
            PharmSeed("Luxor Nile Pharmacy", "Karnak Temple St, Luxor", "Luxor", 25.6872, 32.6396, "+20-95-2376789", true, 3.9, 75, "LIC-019", primaryOwner),
            PharmSeed("Aswan Nubian Pharmacy", "Corniche El Nil St, Aswan", "Aswan", 24.0889, 32.8998, "+20-97-2314567", false, 4.1, 85, "LIC-020", secondaryOwner),
            PharmSeed("Jeddah Seaside Pharmacy", "Corniche Road, Jeddah", "Jeddah", 21.5433, 39.1728, "+966-12-6531234", true, 4.8, 160, "LIC-021", primaryOwner),
            PharmSeed("Riyadh Central Pharmacy", "King Fahd Road, Riyadh", "Riyadh", 24.7136, 46.6753, "+966-11-4612345", false, 4.7, 155, "LIC-022", secondaryOwner),
            PharmSeed("Mecca Holy Pharmacy", "Ajyad St, Mecca", "Mecca", 21.3891, 39.8579, "+966-12-5426789", true, 4.6, 145, "LIC-023", primaryOwner),
            PharmSeed("Medina Noor Pharmacy", "Quba Road, Medina", "Medina", 24.4672, 39.6111, "+966-14-8223456", false, 4.5, 135, "LIC-024", secondaryOwner),
            PharmSeed("Dammam Gulf Pharmacy", "King Saud St, Dammam", "Dammam", 26.4207, 50.0888, "+966-13-8334567", true, 4.3, 115, "LIC-025", primaryOwner),
            PharmSeed("Khobar Care Pharmacy", "Corniche St, Khobar", "Khobar", 26.2833, 50.2100, "+966-13-8645678", false, 4.2, 100, "LIC-026", secondaryOwner),
            PharmSeed("Taif Mountain Pharmacy", "Shubra St, Taif", "Taif", 21.2703, 40.4158, "+966-12-7326789", true, 3.8, 70, "LIC-027", primaryOwner),
            PharmSeed("Tabuk Northern Pharmacy", "Tabuk City Center", "Tabuk", 28.3838, 36.5550, "+966-14-4227890", false, 3.6, 50, "LIC-028", secondaryOwner),
            PharmSeed("Abha Heights Pharmacy", "King Khalid St, Abha", "Abha", 18.2164, 42.5052, "+966-17-2298901", true, 4.0, 80, "LIC-029", primaryOwner),
            PharmSeed("Najran Oasis Pharmacy", "King Abdulaziz St, Najran", "Najran", 17.4917, 44.1320, "+966-17-5229012", false, 3.9, 60, "LIC-030", secondaryOwner)
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

    private fun seedChats() {
        println("Seeding chats...")
        val patientId = Users.selectAll().where { Users.email eq "patient@eilaji.com" }.map { it[Users.id] }.firstOrNull() ?: return
        val gazaPharmacies = Pharmacies.selectAll().where { Pharmacies.city eq "Gaza" }.limit(2).map { it[Pharmacies.ownerUserId] to it[Pharmacies.id] }
        if (gazaPharmacies.size < 2) return
        gazaPharmacies.forEachIndexed { idx, (pharmacyUserId, _) ->
            val existing = Chats.selectAll().where { (Chats.patientUserId eq patientId) and (Chats.pharmacyUserId eq pharmacyUserId) }.count()
            if (existing > 0) return@forEachIndexed
            val chatId = Chats.insert {
                it[Chats.patientUserId] = patientId
                it[Chats.pharmacyUserId] = pharmacyUserId
                it[Chats.prescriptionId] = null
                it[Chats.lastMessageText] = if (idx == 0) "Hello, do you have Paracetamol 500mg available?" else "Is Vitamin C 1000mg in stock?"
                it[Chats.lastMessageSenderId] = patientId
                it[Chats.lastMessageAt] = Instant.now()
                it[Chats.unreadCountPatient] = 0
                it[Chats.unreadCountPharmacy] = 1
                it[Chats.isArchived] = false
                it[Chats.createdAt] = Instant.now()
                it[Chats.updatedAt] = Instant.now()
            } get Chats.id
            Messages.insert {
                it[Messages.chatId] = chatId
                it[Messages.senderUserId] = patientId
                it[Messages.messageText] = if (idx == 0) "Hello, do you have Paracetamol 500mg available?" else "Is Vitamin C 1000mg in stock?"
                it[Messages.messageType] = "TEXT"
                it[Messages.isRead] = false
                it[Messages.createdAt] = Instant.now()
            }
            val reply = if (idx == 0) "Yes, we have it in stock. Price is 5.99." else "Yes, available. Would you like delivery?"
            Messages.insert {
                it[Messages.chatId] = chatId
                it[Messages.senderUserId] = pharmacyUserId
                it[Messages.messageText] = reply
                it[Messages.messageType] = "TEXT"
                it[Messages.isRead] = false
                it[Messages.createdAt] = Instant.now()
            }
            Chats.update({ Chats.id eq chatId }) {
                it[Chats.lastMessageText] = reply
                it[Chats.lastMessageSenderId] = pharmacyUserId
                it[Chats.lastMessageAt] = Instant.now()
                it[Chats.unreadCountPatient] = 1
            }
        }
        println("Seeded ${gazaPharmacies.size} chats")
    }
}
