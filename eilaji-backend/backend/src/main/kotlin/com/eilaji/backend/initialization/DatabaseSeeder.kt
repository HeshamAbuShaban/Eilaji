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

    const val SEED_VERSION = 2

    fun seedIfEmpty() {
        transaction {
            val categoriesCount = Categories.selectAll().count()
            if (categoriesCount == 0L) {
                println("INFO: Seeding database with test data (v$SEED_VERSION)...")
                seedUsers()
                val categoryIds = seedCategories()
                val subcategoryIds = seedSubcategories(categoryIds)
                seedMedicines(subcategoryIds)
                seedPharmacies()
                seedChats()
                println("INFO: Database seeding completed")
                return@transaction
            }
            if (needsV2Reseed()) {
                println("INFO: Migrating seed data to v$SEED_VERSION (therapeutic taxonomy + details)...")
                wipeCatalog()
                val categoryIds = seedCategories()
                val subcategoryIds = seedSubcategories(categoryIds)
                seedMedicines(subcategoryIds)
                seedPharmacies()
                seedChats()
                println("INFO: Seed v$SEED_VERSION completed")
                return@transaction
            }
            val pharmaciesCount = try { Pharmacies.selectAll().count() } catch (_: Exception) { 0 }
            if (pharmaciesCount < 30) {
                println("INFO: Seeding additional pharmacies to reach 30 (current $pharmaciesCount)...")
                try { Messages.deleteAll(); Chats.deleteAll(); Pharmacies.deleteAll() } catch (_: Exception) { println("WARN: delete failed, continuing") }
                seedPharmacies()
                seedChats()
                println("INFO: Additional pharmacies seeded")
            } else {
                println("INFO: Database already has data, skipping seed")
            }
        }
    }

    private fun needsV2Reseed(): Boolean {
        return try {
            val medCount = Medicines.selectAll().count()
            if (medCount == 0L) return false // handled by fresh-seed path
            val sample = Medicines.selectAll().limit(1).singleOrNull() ?: return false
            val dosage = try { sample.getOrNull(Medicines.dosage) } catch (_: Exception) { null }
            if (dosage == null) return true
            val subs = Subcategories.selectAll().limit(20).map { it[Subcategories.nameEn] }
            // v1 marker: generic "Tablets" duplicated across categories
            if (subs.count { it == "Tablets" } > 1) return true
            // v2 marker: shared owners instead of per-pharmacy logins
            val legacyOwned = (Pharmacies innerJoin Users).selectAll()
                .where { Users.email inList listOf("pharmacist1@eilaji.com", "pharmacist2@eilaji.com") }
                .count()
            legacyOwned > 0
        } catch (_: Exception) { false }
    }

    private fun wipeCatalog() {
        // Children first to satisfy FKs; users are kept so logins survive.
        try { Messages.deleteAll() } catch (_: Exception) {}
        try { Chats.deleteAll() } catch (_: Exception) {}
        try { Favorites.deleteAll() } catch (_: Exception) {}
        try { Ratings.deleteAll() } catch (_: Exception) {}
        try { Orders.deleteAll() } catch (_: Exception) {}
        try { Prescriptions.deleteAll() } catch (_: Exception) {}
        try { EilajiPlusSync.deleteAll() } catch (_: Exception) {}
        try { PharmacyMedicines.deleteAll() } catch (_: Exception) {}
        try { Medicines.deleteAll() } catch (_: Exception) {}
        try { Subcategories.deleteAll() } catch (_: Exception) {}
        try { Categories.deleteAll() } catch (_: Exception) {}
        try { Pharmacies.deleteAll() } catch (_: Exception) {}
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
        val defs = listOf(
            Triple("Pain Relievers", "مسكنات الألم", "/images/categories/pain.png"),
            Triple("Antibiotics", "المضادات الحيوية", "/images/categories/antibiotics.png"),
            Triple("Vitamins & Supplements", "الفيتامينات والمكملات", "/images/categories/vitamins.png"),
            Triple("Skin Care", "العناية بالبشرة", "/images/categories/skin.png"),
            Triple("Cold & Flu", "البرد والإنفلونزا", "/images/categories/cold.png"),
            Triple("Digestive Health", "صحة الجهاز الهضمي", "/images/categories/digestive.png")
        )
        val categoryIds = defs.mapIndexed { idx, (en, ar, icon) ->
            Categories.insert {
                it[nameEn] = en
                it[nameAr] = ar
                it[iconUrl] = icon
                it[displayOrder] = idx
                it[isActive] = true
            } get Categories.id
        }
        println("Seeded ${categoryIds.size} categories")
        return categoryIds
    }

    private fun seedSubcategories(categoryIds: List<UUID>): List<UUID> {
        println("Seeding subcategories...")
        val subcategories = mutableListOf<UUID>()
        // Therapeutic taxonomy: 2 per category, per-category displayOrder 0..1
        val defs = listOf(
            Triple(categoryIds[0], "NSAIDs", "مضادات الالتهاب"),
            Triple(categoryIds[0], "Paracetamol & Analgesics", "باراسيتامول ومسكنات"),
            Triple(categoryIds[1], "Penicillins", "البنسلينات"),
            Triple(categoryIds[1], "Broad-Spectrum Antibiotics", "مضادات واسعة الطيف"),
            Triple(categoryIds[2], "Vitamins & Minerals", "فيتامينات ومعادن"),
            Triple(categoryIds[2], "Specialty Supplements", "مكملات متخصصة"),
            Triple(categoryIds[3], "Topical Treatments", "علاجات موضعية"),
            Triple(categoryIds[3], "Moisturizers & Sun Care", "ترطيب وحماية من الشمس"),
            Triple(categoryIds[4], "Cold, Flu & Allergy", "برد وإنفلونزا وحساسية"),
            Triple(categoryIds[4], "Cough & Throat", "سعال وحلق"),
            Triple(categoryIds[5], "Acid & Reflux", "حموضة وارتجاع"),
            Triple(categoryIds[5], "Gut & Motility", "هضم وحركة الأمعاء")
        )
        val icons = listOf(
            "/images/subcategories/nsaids.png", "/images/subcategories/analgesics.png",
            "/images/subcategories/penicillins.png", "/images/subcategories/broad_spectrum.png",
            "/images/subcategories/vitamins.png", "/images/subcategories/supplements.png",
            "/images/subcategories/topical.png", "/images/subcategories/moisturizers.png",
            "/images/subcategories/cold_flu.png", "/images/subcategories/cough.png",
            "/images/subcategories/acid.png", "/images/subcategories/gut.png"
        )
        defs.forEachIndexed { idx, (catId, en, ar) ->
            val id = Subcategories.insert {
                it[categoryId] = catId
                it[nameEn] = en
                it[nameAr] = ar
                it[iconUrl] = icons[idx]
                it[displayOrder] = idx % 2
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
            val dosage: String,
            val warnings: String,
            val sideEffects: String,
            val storage: String,
            val manufacturer: String,
            val price: Double,
            val requiresPrescription: Boolean,
            val imageUrl: String,
            val subIdx: Int
        )
        val medicines = listOf(
            MedSeed("Paracetamol 500mg", "باراسيتامول 500مجم", "Pain reliever and fever reducer, effective for headache and mild pain", "500mg every 6 hours as needed, max 4g per day", "Do not exceed the stated dose. Avoid with other paracetamol products or with severe liver disease.", "Rare at recommended doses; nausea or rash in sensitive individuals.", "Store below 30C, keep dry and away from children.", "Bayer", 5.99, false, "/images/medicines/paracetamol.jpg", 1),
            MedSeed("Ibuprofen 400mg", "إيبوبروفين 400مجم", "Non-steroidal anti-inflammatory drug for pain and inflammation", "400mg up to 3 times daily with food", "Avoid with stomach ulcers, severe kidney disease, or late pregnancy. Ask a pharmacist if on blood thinners.", "Stomach upset, heartburn; stop use if black stools or vomiting blood.", "Store at room temperature away from moisture.", "Pfizer", 8.50, false, "/images/medicines/ibuprofen.jpg", 0),
            MedSeed("Diclofenac 50mg", "ديكلوفيناك 50مجم", "Potent NSAID for joint pain and arthritis", "50mg 2-3 times daily with food as directed", "Avoid with heart failure, ulcers, or NSAID allergy. Use lowest effective dose.", "Stomach pain, dizziness; seek care for chest pain or swelling.", "Store below 30C in original pack.", "Novartis", 12.30, false, "/images/medicines/diclofenac.jpg", 0),
            MedSeed("Aspirin 100mg", "أسبرين 100مجم", "Low-dose aspirin for blood thinning and pain relief", "100mg once daily or as directed by a doctor", "Not for children with viral illness. Avoid with bleeding disorders or aspirin allergy.", "Easy bruising, stomach irritation; seek care for unusual bleeding.", "Store in a cool dry place.", "Bayer", 4.25, false, "/images/medicines/aspirin.jpg", 0),
            MedSeed("Ketoprofen 100mg", "كيتوبروفين 100مجم", "Anti-inflammatory for musculoskeletal pain", "100mg twice daily with food, prescription only", "Prescription only. Avoid with ulcers, kidney impairment, or NSAID allergy.", "Stomach upset, headache; stop use if rash or swelling appears.", "Store below 25C away from light.", "Sanofi", 15.75, true, "/images/medicines/ketoprofen.jpg", 0),
            MedSeed("Tramadol 50mg", "ترامادول 50مجم", "Strong analgesic for moderate to severe pain", "50mg as directed, prescription only; do not drive", "Prescription only. Risk of dependence; avoid alcohol and sedatives.", "Drowsiness, dizziness, nausea, constipation.", "Store locked away from children.", "GSK", 22.90, true, "/images/medicines/tramadol.jpg", 1),
            MedSeed("Amoxicillin 500mg", "أموكسيسيلين 500مجم", "Broad-spectrum penicillin antibiotic", "500mg every 8 hours for the full prescribed course", "Prescription only. Complete the full course. Not for viral infections.", "Diarrhea, nausea, rash; seek care for breathing difficulty.", "Store below 25C; suspensions refrigerated per label.", "GSK", 9.99, true, "/images/medicines/amoxicillin.jpg", 2),
            MedSeed("Azithromycin 250mg", "أزيثروميسين 250مجم", "Macrolide antibiotic for respiratory infections", "As prescribed, usually once daily for 3-5 days", "Prescription only. Tell your doctor about heart rhythm disorders.", "Nausea, abdominal pain; seek care for irregular heartbeat.", "Store at room temperature.", "Pfizer", 18.50, true, "/images/medicines/azithromycin.jpg", 3),
            MedSeed("Ciprofloxacin 500mg", "سيبروفلوكساسين 500مجم", "Fluoroquinolone for bacterial infections", "500mg twice daily as prescribed; avoid dairy at dose time", "Prescription only. Avoid with tendon disorders; limit sun exposure.", "Nausea, dizziness; stop use for tendon pain and seek care.", "Store below 30C away from light.", "Bayer", 14.20, true, "/images/medicines/ciprofloxacin.jpg", 3),
            MedSeed("Augmentin 625mg", "أوجمنتين 625مجم", "Amoxicillin/clavulanate combination antibiotic", "625mg every 8-12 hours with food for the full course", "Prescription only. Complete the full course even if feeling better.", "Diarrhea, nausea; seek care for severe rash or jaundice.", "Store below 25C; keep dry.", "GSK", 24.99, true, "/images/medicines/augmentin.jpg", 2),
            MedSeed("Doxycycline 100mg", "دوكسيسيكلين 100مجم", "Tetracycline antibiotic for acne and infections", "100mg once or twice daily with a full glass of water", "Avoid in pregnancy and children under 8. Avoid sun exposure.", "Sunburn risk, nausea; take upright to avoid throat irritation.", "Store at room temperature away from light.", "Pfizer", 11.30, false, "/images/medicines/doxycycline.jpg", 3),
            MedSeed("Metronidazole 400mg", "ميترونيدازول 400مجم", "Antibiotic and antiprotozoal for intestinal infections", "400mg 2-3 times daily as directed", "Avoid alcohol during treatment and for 48 hours after.", "Metallic taste, nausea; seek care for numbness or seizures.", "Store below 30C.", "Sanofi", 7.80, false, "/images/medicines/metronidazole.jpg", 3),
            MedSeed("Vitamin C 1000mg", "فيتامين سي 1000مجم", "Immune support and antioxidant supplement", "1000mg once daily dissolved in water", "Do not exceed the daily dose. High doses may cause stomach upset.", "Diarrhea or cramps at high doses.", "Store in a cool dry place.", "Hikma Pharmaceuticals", 12.99, false, "/images/medicines/vitamin_c.jpg", 4),
            MedSeed("Vitamin D3 5000IU", "فيتامين د3 5000 وحدة", "Bone health and immune system support", "One softgel daily with food or as directed", "Check levels with long-term use; avoid excess calcium.", "Usually well tolerated; excess may cause nausea.", "Store below 25C.", "Pfizer", 16.50, false, "/images/medicines/vitamin_d3.jpg", 4),
            MedSeed("Omega-3 Fish Oil 1000mg", "أوميغا 3 زيت السمك 1000مجم", "Heart and brain health omega-3 supplement", "1000mg 1-2 times daily with meals", "Tell your doctor if on blood thinners or before surgery.", "Fishy aftertaste, mild stomach upset.", "Store in a cool dry place.", "Novartis", 25.99, false, "/images/medicines/omega3.jpg", 5),
            MedSeed("Centrum Multivitamin", "سنتروم متعدد الفيتامينات", "Complete daily multivitamin for adults", "One tablet daily with breakfast", "Not a substitute for a varied diet. Keep away from children (iron).", "Mild nausea if taken on an empty stomach.", "Store below 30C.", "Pfizer", 28.75, false, "/images/medicines/centrum.jpg", 5),
            MedSeed("Calcium + Magnesium 500mg", "كالسيوم + مغنيسيوم 500مجم", "Bone strength and muscle function support", "As directed, usually with evening meal", "Separate from some antibiotics by 2 hours.", "Constipation or loose stools in sensitive users.", "Store in a dry place.", "Bayer", 18.20, false, "/images/medicines/calcium_magnesium.jpg", 4),
            MedSeed("Zinc 50mg", "زنك 50مجم", "Immune support and wound healing mineral", "50mg once daily with food", "Long-term high doses need medical supervision (copper).", "Nausea on empty stomach.", "Store at room temperature.", "Teva", 9.40, false, "/images/medicines/zinc.jpg", 4),
            MedSeed("Hydrocortisone Cream 1%", "كريم هيدروكورتيزون 1%", "Topical corticosteroid for eczema and dermatitis", "Thin layer 1-2 times daily to affected area", "Prescription strength. Avoid face, eyes, and broken skin unless directed.", "Burning or thinning with prolonged use.", "Store below 25C; do not freeze.", "Bayer", 13.50, true, "/images/medicines/hydrocortisone.jpg", 6),
            MedSeed("Cetaphil Moisturizer 250ml", "سيتافيل مرطب 250مل", "Gentle daily moisturizer for sensitive skin", "Apply liberally to face and body daily", "For external use only. Avoid contact with eyes.", "Rare mild irritation.", "Store at room temperature.", "Johnson & Johnson", 19.99, false, "/images/medicines/cetaphil.jpg", 7),
            MedSeed("Eucerin Cream 150ml", "يوسيرين كريم 150مل", "Intensive repair cream for very dry skin", "Apply to very dry areas 1-2 times daily", "For external use only.", "Rare tingling on cracked skin.", "Store below 30C.", "Novartis", 21.30, false, "/images/medicines/eucerin.jpg", 7),
            MedSeed("Differin Gel 0.1% 30g", "ديفرين جل 0.1% 30غ", "Adapalene gel for acne treatment", "Pea-sized amount nightly to clean dry skin", "Prescription strength. Use sunscreen; avoid waxing treated areas.", "Dryness, redness, peeling in first weeks.", "Store below 30C away from light.", "GSK", 32.00, true, "/images/medicines/differin.jpg", 6),
            MedSeed("Nivea Soft Cream 200ml", "نيفيا سوفت كريم 200مل", "Light moisturizing cream for face and body", "Apply as needed to face, hands and body", "For external use only.", "Rare sensitivity to fragrance.", "Store at room temperature.", "Bayer", 7.99, false, "/images/medicines/nivea_soft.jpg", 7),
            MedSeed("Bioderma Sunscreen SPF50", "بيوديرما واقي شمس SPF50", "High protection sunscreen for sensitive skin", "Apply generously 15 minutes before sun; reapply often", "Reapply after swimming or sweating. Avoid midday sun.", "Rare eye sting; rinse with water.", "Store below 30C.", "Hikma Pharmaceuticals", 26.50, false, "/images/medicines/bioderma_sunscreen.jpg", 7),
            MedSeed("Panadol Cold & Flu", "بنادول كولد اند فلو", "Relief for cold, flu and fever symptoms", "As directed on pack, max daily paracetamol limits apply", "Contains paracetamol; avoid alcohol and other cold remedies.", "Drowsiness (some formulas), dry mouth.", "Store below 30C.", "GSK", 6.99, false, "/images/medicines/panadol_cold_flu.jpg", 8),
            MedSeed("Loratadine 10mg", "لوراتادين 10مجم", "Non-drowsy antihistamine for allergies", "10mg once daily", "Usually non-drowsy; avoid alcohol.", "Headache or dry mouth in some users.", "Store at room temperature.", "Bayer", 8.99, false, "/images/medicines/loratadine.jpg", 8),
            MedSeed("Cetirizine 10mg", "سيتيريزين 10مجم", "Antihistamine for hay fever and hives", "10mg once daily", "May cause drowsiness in some; avoid driving if affected.", "Drowsiness, dry mouth.", "Store below 30C.", "Pfizer", 7.50, false, "/images/medicines/cetirizine.jpg", 8),
            MedSeed("Oseltamivir 75mg", "أوسيلتاميفير 75مجم", "Antiviral for influenza A and B", "75mg twice daily for 5 days as prescribed", "Prescription only. Start within 48 hours of symptoms for best effect.", "Nausea, vomiting, headache.", "Store below 30C.", "Roche", 45.99, true, "/images/medicines/oseltamivir.jpg", 8),
            MedSeed("Dextromethorphan Syrup 100ml", "ديكستروميثورفان شراب 100مل", "Cough suppressant for dry cough", "As directed on pack with measuring cup", "Not for productive cough with mucus; ask a pharmacist for asthma.", "Drowsiness, dizziness.", "Store upright below 30C.", "Cipla", 6.75, false, "/images/medicines/dextromethorphan.jpg", 9),
            MedSeed("Strepsils Lozenges Honey", "ستربسلز أقراص عسل", "Soothing lozenges for sore throat", "Dissolve one lozenge slowly every 2-3 hours", "Not for children under 6. See a doctor if fever persists.", "Mild mouth tingling.", "Store in a dry place.", "Merck", 3.50, false, "/images/medicines/strepsils.jpg", 9),
            MedSeed("Omeprazole 20mg", "أوميبرازول 20مجم", "Proton pump inhibitor for acid reflux", "20mg once daily before breakfast", "Prescription strength. Long-term use needs medical review.", "Headache, abdominal pain.", "Store below 30C in original pack.", "AstraZeneca", 10.99, true, "/images/medicines/omeprazole.jpg", 10),
            MedSeed("Nexium 40mg", "نيكسيوم 40مجم", "Esomeprazole for GERD and ulcers", "40mg once daily as prescribed", "Prescription only. Tell your doctor about long-term use.", "Headache, nausea.", "Store below 30C.", "AstraZeneca", 35.40, true, "/images/medicines/nexium.jpg", 10),
            MedSeed("Gaviscon Syrup 200ml", "جافيسكون شراب 200مل", "Antacid for heartburn and indigestion", "10-20ml after meals and at bedtime; shake well", "Separate from other medicines by 2 hours.", "Mild bloating; contains sodium.", "Store upright at room temperature.", "Merck", 11.20, false, "/images/medicines/gaviscon.jpg", 10),
            MedSeed("Buscopan 10mg", "بوسكوبان 10مجم", "Antispasmodic for abdominal cramps", "10mg up to 3 times daily as needed", "Avoid with glaucoma or urinary retention unless directed.", "Dry mouth, blurred vision.", "Store below 30C.", "Sanofi", 9.80, false, "/images/medicines/buscopan.jpg", 11),
            MedSeed("Dulcolax 5mg", "دولكولاكس 5مجم", "Laxative for constipation relief", "5-10mg at night for short-term use", "Short-term use only; drink plenty of fluids.", "Cramps, diarrhea with overuse.", "Store in a dry place.", "Sanofi", 5.20, false, "/images/medicines/dulcolax.jpg", 11),
            MedSeed("Enterogermina Probiotics 10 vials", "إنتروجرمينا بروبيوتيك 10 عبوات", "Probiotic for gut flora balance and diarrhea", "One vial daily or as directed", "Shake before use. Ask a pharmacist for infants.", "Usually well tolerated.", "Store below 30C.", "Merck", 18.90, false, "/images/medicines/enterogermina.jpg", 11)
        )
        medicines.forEach { m ->
            Medicines.insert {
                it[titleEn] = m.titleEn
                it[titleAr] = m.titleAr
                it[descriptionEn] = m.descriptionEn
                it[descriptionAr] = m.descriptionEn
                it[dosage] = m.dosage
                it[warnings] = m.warnings
                it[sideEffects] = m.sideEffects
                it[storageInfo] = m.storage
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

    private fun slugEmail(name: String): String {
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), ".").trim('.')
        return "$slug@eilaji.com"
    }

    private fun ensurePharmacist(email: String, displayName: String): UUID {
        val existing = Users.selectAll().where { Users.email eq email }.singleOrNull()
        if (existing != null) return existing[Users.id]
        val id = UUID.randomUUID()
        Users.insert {
            it[Users.id] = id
            it[Users.email] = email
            it[Users.passwordHash] = BCrypt.hashpw("password123", BCrypt.gensalt())
            it[Users.fullName] = displayName
            it[Users.role] = "PHARMACIST"
            it[Users.isVerified] = true
            it[Users.isActive] = true
            it[Users.createdAt] = Instant.now()
            it[Users.updatedAt] = Instant.now()
        }
        return id
    }

    private fun seedPharmacies() {
        println("Seeding pharmacies...")
        // Every pharmacy gets its own owner login: <pharmacy-name-slug>@eilaji.com / password123
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
            val license: String
        )
        val pharmacies = listOf(
            PharmSeed("Al-Shifa Gaza", "Al-Rimal District, Omar Al-Mukhtar St, Gaza", "Gaza", 31.4495, 34.3925, "+970-8-2841234", true, 4.8, 150, "LIC-001"),
            PharmSeed("Al-Quds Gaza", "Al-Nasr St, Gaza City", "Gaza", 31.4498, 34.3932, "+970-8-2841235", false, 4.5, 120, "LIC-002"),
            PharmSeed("Al-Aqsa Gaza", "Al-Wehda St, Gaza", "Gaza", 31.4500, 34.3940, "+970-8-2841236", true, 4.2, 85, "LIC-003"),
            PharmSeed("Al-Rimal Gaza", "Al-Rashid St, Gaza Beach Camp", "Gaza", 31.4492, 34.3950, "+970-8-2841237", false, 4.6, 95, "LIC-004"),
            PharmSeed("Al-Zahra Gaza", "Al-Jalaa St, Gaza", "Gaza", 31.4496, 34.3960, "+970-8-2841238", true, 3.9, 60, "LIC-005"),
            PharmSeed("Al-Salam Gaza", "Salah Al-Din St, Gaza", "Gaza", 31.5000, 34.4700, "+970-8-2841239", false, 4.4, 110, "LIC-006"),
            PharmSeed("Al-Noor Gaza", "Al-Maghazi Camp St, Gaza", "Gaza", 31.5010, 34.4710, "+970-8-2841240", true, 3.7, 45, "LIC-007"),
            PharmSeed("Al-Amal Gaza", "Khan Younis Border St, Gaza", "Gaza", 31.4980, 34.4690, "+970-8-2841241", false, 4.1, 70, "LIC-008"),
            PharmSeed("Al-Hayat Gaza", "Beach Road, Gaza Port", "Gaza", 31.4485, 34.3920, "+970-8-2841242", true, 3.5, 30, "LIC-009"),
            PharmSeed("Al-Wafa Gaza", "Al-Shifa Hospital St, Gaza", "Gaza", 31.4510, 34.3970, "+970-8-2841243", false, 4.0, 55, "LIC-010"),
            PharmSeed("Al-Shifa Damascus", "123 Main St, Damascus", "Damascus", 33.5138, 36.2765, "+963-11-1234567", true, 4.7, 140, "LIC-011"),
            PharmSeed("Al-Hayat Aleppo", "456 Old City, Aleppo", "Aleppo", 36.2021, 37.1343, "+963-21-7654321", false, 4.3, 100, "LIC-012"),
            PharmSeed("Al-Noor Homs", "Al-Hamra St, Homs", "Homs", 34.7260, 36.7230, "+963-31-2345678", true, 4.0, 80, "LIC-013"),
            PharmSeed("Al-Amal Latakia", "Corniche St, Latakia", "Latakia", 35.5407, 35.7890, "+963-41-3456789", false, 4.5, 90, "LIC-014"),
            PharmSeed("Al-Salam Hama", "Al-Assi Sq, Hama", "Hama", 35.1318, 36.7578, "+963-33-4567890", true, 3.8, 65, "LIC-015"),
            PharmSeed("Cairo Central Pharmacy", "Tahrir Square, Downtown Cairo", "Cairo", 30.0444, 31.2357, "+20-2-27912345", false, 4.6, 130, "LIC-016"),
            PharmSeed("Alexandria Care Pharmacy", "Corniche Road, Alexandria", "Alexandria", 31.2001, 29.9187, "+20-3-4845678", true, 4.2, 95, "LIC-017"),
            PharmSeed("Giza Health Pharmacy", "Pyramids Ave, Giza", "Giza", 30.0131, 31.2089, "+20-2-35678901", false, 4.4, 105, "LIC-018"),
            PharmSeed("Luxor Nile Pharmacy", "Karnak Temple St, Luxor", "Luxor", 25.6872, 32.6396, "+20-95-2376789", true, 3.9, 75, "LIC-019"),
            PharmSeed("Aswan Nubian Pharmacy", "Corniche El Nil St, Aswan", "Aswan", 24.0889, 32.8998, "+20-97-2314567", false, 4.1, 85, "LIC-020"),
            PharmSeed("Jeddah Seaside Pharmacy", "Corniche Road, Jeddah", "Jeddah", 21.5433, 39.1728, "+966-12-6531234", true, 4.8, 160, "LIC-021"),
            PharmSeed("Riyadh Central Pharmacy", "King Fahd Road, Riyadh", "Riyadh", 24.7136, 46.6753, "+966-11-4612345", false, 4.7, 155, "LIC-022"),
            PharmSeed("Mecca Holy Pharmacy", "Ajyad St, Mecca", "Mecca", 21.3891, 39.8579, "+966-12-5426789", true, 4.6, 145, "LIC-023"),
            PharmSeed("Medina Noor Pharmacy", "Quba Road, Medina", "Medina", 24.4672, 39.6111, "+966-14-8223456", false, 4.5, 135, "LIC-024"),
            PharmSeed("Dammam Gulf Pharmacy", "King Saud St, Dammam", "Dammam", 26.4207, 50.0888, "+966-13-8334567", true, 4.3, 115, "LIC-025"),
            PharmSeed("Khobar Care Pharmacy", "Corniche St, Khobar", "Khobar", 26.2833, 50.2100, "+966-13-8645678", false, 4.2, 100, "LIC-026"),
            PharmSeed("Taif Mountain Pharmacy", "Shubra St, Taif", "Taif", 21.2703, 40.4158, "+966-12-7326789", true, 3.8, 70, "LIC-027"),
            PharmSeed("Tabuk Northern Pharmacy", "Tabuk City Center", "Tabuk", 28.3838, 36.5550, "+966-14-4227890", false, 3.6, 50, "LIC-028"),
            PharmSeed("Abha Heights Pharmacy", "King Khalid St, Abha", "Abha", 18.2164, 42.5052, "+966-17-2298901", true, 4.0, 80, "LIC-029"),
            PharmSeed("Najran Oasis Pharmacy", "King Abdulaziz St, Najran", "Najran", 17.4917, 44.1320, "+966-17-5229012", false, 3.9, 60, "LIC-030")
        )
        pharmacies.forEachIndexed { idx, p ->
            val tier = when {
                p.city == "Gaza" -> Triple(1.5, 5.0, 12.0)
                p.city in listOf("Damascus", "Aleppo", "Homs", "Latakia", "Hama") -> Triple(2.0, 8.0, 15.0)
                p.city in listOf("Cairo", "Alexandria", "Giza", "Luxor", "Aswan") -> Triple(2.5, 10.0, 20.0)
                else -> Triple(3.0, 15.0, 25.0)
            }
            Pharmacies.insert {
                it[ownerUserId] = ensurePharmacist(slugEmail(p.name), p.name)
                it[name] = p.name
                it[description] = "Verified community pharmacy in ${p.city} — prescription dispensing, OTC and daily essentials with pharmacist chat support."
                it[address] = p.address
                it[city] = p.city
                it[latitude] = p.lat
                it[longitude] = p.lng
                it[phone] = p.phone
                it[isVerified] = true
                it[isOpen] = p.isOpen
                it[openingHours] = if (p.isOpen) "9:00-23:00" else "9:00-22:00"
                it[licenseNumber] = p.license
                it[ratingAvg] = p.ratingAvg.toBigDecimal()
                it[totalRatings] = p.totalRatings
                try { it[deliveryFee] = tier.first.toBigDecimal() } catch (_: Exception) {}
                try { it[minOrderAmount] = tier.second.toBigDecimal() } catch (_: Exception) {}
                try { it[prepTimeMin] = 15 + (idx % 3) * 5 } catch (_: Exception) {}
                try { it[deliveryRadiusKm] = tier.third.toBigDecimal() } catch (_: Exception) {}
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
