# 🚀 Eilaji Backend Migration Plan
## From Firebase to Cutting-Edge Kotlin Backend

---

## 📋 Executive Summary

This document outlines a comprehensive plan to migrate **Eilaji** from Firebase-dependent architecture to a modern, self-hosted Kotlin backend using **Ktor + Exposed + PostgreSQL**. This migration will provide:

- ✅ Full control over data and business logic
- ✅ Better inter-app communication with eilaji-plus
- ✅ Cost optimization at scale
- ✅ Enhanced security and compliance for healthcare data
- ✅ Type-safe end-to-end Kotlin development
- ✅ Real-time messaging without vendor lock-in

---

## 🎯 Technology Stack Selection

### **Backend Framework: Ktor Server**
- **Why?** Built by JetBrains specifically for Kotlin (not Java ported)
- Native coroutine support for async operations
- Lightweight, fast, and highly modular
- Perfect fit for your Kotlin expertise

### **Database: PostgreSQL**
- **Why?** Industry-standard relational database
- Excellent JSON support for flexible schemas
- Strong consistency for healthcare data
- Geospatial queries for pharmacy location features

### **ORM: JetBrains Exposed**
- **Why?** Type-safe SQL in pure Kotlin
- Compile-time query validation
- No raw SQL strings
- Fluent DSL that feels natural to Kotlin developers

### **Real-Time Communication: Ktor WebSockets + Redis Pub/Sub**
- **Why?** Replace Firebase Realtime Database
- Self-hosted WebSocket connections
- Redis for message broadcasting across instances
- Lower latency than polling-based solutions

### **Authentication: JWT + Ktor Security**
- **Why?** Stateless, scalable authentication
- Easy integration with mobile apps
- Support for refresh tokens
- Role-based access control (patients, pharmacists, admins)

### **File Storage: MinIO or AWS S3**
- **Why?** S3-compatible object storage
- Prescription images, pharmacy photos, chat media
- MinIO for self-hosted option
- Direct upload URLs for mobile clients

### **Message Queue: Redis Streams / Bull**
- **Why?** Async prescription delivery to eilaji-plus
- Decouple services
- Retry logic and dead-letter queues
- Event-driven architecture

### **API Documentation: OpenAPI/Swagger with Ktor OpenAPI**
- **Why?** Auto-generated API docs
- Client SDK generation
- Interactive testing interface

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        EILAJI MOBILE APP                         │
│                     (Android - Kotlin)                           │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTPS / WebSocket
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    KTOR API GATEWAY                              │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
│  │   Auth      │  │   Rate       │  │   Request/Response   │   │
│  │   Filter    │  │   Limiting   │  │   Logging            │   │
│  └─────────────┘  └──────────────┘  └──────────────────────┘   │
└────────────────────────┬────────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────────┐
│  User Service   │ │  Medicine       │ │  Prescription       │
│  - Auth         │ │  Catalog        │ │  Service            │
│  - Profiles     │ │  - CRUD         │ │  - Create           │
│  - Roles        │ │  - Search       │ │  - Validate         │
│                 │ │  - Categories   │ │  - Route to         │
└─────────────────┘ └─────────────────┘ │    eilaji-plus      │
                                        └─────────────────────┘
         ▼               ▼               ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────────┐
│  Pharmacy       │ │  Chat/Messaging │ │  Location/Maps      │
│  Service        │ │  Service        │ │  Service            │
│  - Registry     │ │  - WebSocket    │ │  - Geo queries      │
│  - Verification │ │  - Messages     │ │  - Nearby search    │
│  - Ratings      │ │  - Presence     │ │  - Distance calc    │
└─────────────────┘ └─────────────────┘ └─────────────────────┘
         ▼               ▼               ▼
┌─────────────────────────────────────────────────────────────────┐
│                      DATA LAYER                                  │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
│  │  PostgreSQL │  │    Redis     │  │      MinIO/S3        │   │
│  │  (Exposed)  │  │  (Cache +    │  │  (Prescription &     │   │
│  │             │  │   Pub/Sub)   │  │   Media Storage)     │   │
│  └─────────────┘  └──────────────┘  └──────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    EILAJI-PLUS APP                               │
│              (Prescription Processing System)                    │
│         ← REST API / Webhooks / Message Queue →                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📊 Database Schema Design

### **Core Tables**

#### 1. **Users**
```kotlin
object Users : Table("users") {
    val id = uuid("id").primaryKey()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val fullName = varchar("full_name", 255)
    val phone = varchar("phone", 20)
    val role = enumerationByName("role", 20, UserRole::class) // PATIENT, PHARMACIST, ADMIN
    val profileImageUrl = varchar("profile_image_url", 500).nullable()
    val fcmToken = varchar("fcm_token", 500).nullable() // For push notifications
    val isVerified = bool("is_verified").default(false)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
```

#### 2. **Pharmacies**
```kotlin
object Pharmacies : Table("pharmacies") {
    val id = uuid("id").primaryKey()
    val ownerId = reference("owner_id", Users.id)
    val name = varchar("name", 255)
    val address = text("address")
    val phone = varchar("phone", 20)
    val latitude = double("latitude")
    val longitude = double("longitude")
    val licenseNumber = varchar("license_number", 100).uniqueIndex()
    val imageUrl = varchar("image_url", 500).nullable()
    val isOpen = bool("is_open").default(true)
    val operatingHours = json("operating_hours") // JSON: {monday: {open: "09:00", close: "22:00"}, ...}
    val rating = double("rating").default(0.0)
    val totalRatings = integer("total_ratings").default(0)
    val isVerified = bool("is_verified").default(false)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    
    // PostGIS extension for geospatial queries
    index("geolocation_index", false, "latitude", "longitude")
}
```

#### 3. **Categories & SubCategories**
```kotlin
object Categories : Table("categories") {
    val id = uuid("id").primaryKey()
    val nameAr = varchar("name_ar", 255) // Arabic
    val nameEn = varchar("name_en", 255) // English
    val iconUrl = varchar("icon_url", 500).nullable()
    val sortOrder = integer("sort_order").default(0)
}

object SubCategories : Table("sub_categories") {
    val id = uuid("id").primaryKey()
    val categoryId = reference("category_id", Categories.id)
    val nameAr = varchar("name_ar", 255)
    val nameEn = varchar("name_en", 255)
    val sortOrder = integer("sort_order").default(0)
}
```

#### 4. **Medicines**
```kotlin
object Medicines : Table("medicines") {
    val id = uuid("id").primaryKey()
    val nameAr = varchar("name_ar", 255).index()
    val nameEn = varchar("name_en", 255).index()
    val scientificName = varchar("scientific_name", 255).nullable()
    val description = text("description").nullable()
    val dosageForm = varchar("dosage_form", 100).nullable() // Tablet, Syrup, Injection
    val concentration = varchar("concentration", 100).nullable() // 500mg, 20ml
    val manufacturer = varchar("manufacturer", 255).nullable()
    val price = decimal("price", 10, 2).nullable()
    val imageUrl = varchar("image_url", 500).nullable()
    val subCategoryId = reference("sub_category_id", SubCategories.id)
    val requiresPrescription = bool("requires_prescription").default(false)
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object MedicineAlternatives : Table("medicine_alternatives") {
    val medicineId = reference("medicine_id", Medicines.id)
    val alternativeMedicineId = reference("alternative_medicine_id", Medicines.id)
    primaryKey(medicineId, alternativeMedicineId)
}
```

#### 5. **Prescriptions**
```kotlin
object Prescriptions : Table("prescriptions") {
    val id = uuid("id").primaryKey()
    val patientId = reference("patient_id", Users.id)
    val pharmacyId = reference("pharmacy_id", Pharmacies.id).nullable()
    val imageUrl = varchar("image_url", 500)
    val notes = text("notes").nullable()
    val status = enumerationByName("status", 20, PrescriptionStatus::class) 
        // PENDING, SENT_TO_PHARMACY, ACCEPTED, REJECTED, FILLED, CANCELLED
    val latitude = double("latitude").nullable() // Patient location
    val longitude = double("longitude").nullable()
    val sentAt = datetime("sent_at")
    val respondedAt = datetime("responded_at").nullable()
    val pharmacistNotes = text("pharmacist_notes").nullable()
    val eilajiPlusReference = uuid("eilaji_plus_reference").nullable() // Link to eilaji-plus system
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
```

#### 6. **Chats & Messages**
```kotlin
object Chats : Table("chats") {
    val id = uuid("id").primaryKey()
    val participant1Id = reference("participant_1_id", Users.id)
    val participant2Id = reference("participant_2_id", Users.id)
    val lastMessageText = text("last_message_text").nullable()
    val lastMessageImageUrl = varchar("last_message_image_url", 500).nullable()
    val lastMessageSenderId = reference("last_message_sender_id", Users.id).nullable()
    val lastMessageTimestamp = datetime("last_message_timestamp").nullable()
    val unreadCount1 = integer("unread_count_1").default(0)
    val unreadCount2 = integer("unread_count_2").default(0)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    
    uniqueIndex(participant1Id, participant2Id)
}

object Messages : Table("messages") {
    val id = uuid("id").primaryKey()
    val chatId = reference("chat_id", Chats.id).index()
    val senderId = reference("sender_id", Users.id)
    val receiverId = reference("receiver_id", Users.id)
    val text = text("text").nullable()
    val imageUrl = varchar("image_url", 500).nullable()
    val medicineName = varchar("medicine_name", 255).nullable()
    val prescriptionId = reference("prescription_id", Prescriptions.id).nullable()
    val isRead = bool("is_read").default(false)
    val readAt = datetime("read_at").nullable()
    val createdAt = datetime("created_at")
    
    index("message_timestamp_index", false, chatId, createdAt)
}
```

#### 7. **Favorites**
```kotlin
object Favorites : Table("favorites") {
    val userId = reference("user_id", Users.id)
    val medicineId = reference("medicine_id", Medicines.id).nullable()
    val pharmacyId = reference("pharmacy_id", Pharmacies.id).nullable()
    val createdAt = datetime("created_at")
    
    primaryKey(userId, medicineId, pharmacyId)
    check("has_one_reference") { 
        "(medicine_id IS NOT NULL AND pharmacy_id IS NULL) OR (medicine_id IS NULL AND pharmacy_id IS NOT NULL)" 
    }
}
```

#### 8. **Ratings**
```kotlin
object Ratings : Table("ratings") {
    val id = uuid("id").primaryKey()
    val userId = reference("user_id", Users.id)
    val pharmacyId = reference("pharmacy_id", Pharmacies.id)
    val medicineId = reference("medicine_id", Medicines.id).nullable()
    val rating = integer("rating") // 1-5
    val comment = text("comment").nullable()
    val createdAt = datetime("created_at")
    
    check("rating_range") { "rating >= 1 AND rating <= 5" }
}
```

#### 9. **Reminders** (Sync from local Room DB)
```kotlin
object Reminders : Table("reminders") {
    val id = varchar("id", 100).primaryKey()
    val userId = reference("user_id", Users.id).index()
    val text = text("text")
    val scheduledTime = datetime("scheduled_time")
    val reminderType = enumerationByName("reminder_type", 20, ReminderType::class)
    val soundNumber = integer("sound_number").default(1)
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at")
}
```

#### 10. **Addresses**
```kotlin
object Addresses : Table("addresses") {
    val id = uuid("id").primaryKey()
    val userId = reference("user_id", Users.id)
    val label = varchar("label", 100) // Home, Work, etc.
    val fullAddress = text("full_address")
    val latitude = double("latitude")
    val longitude = double("longitude")
    val isDefault = bool("is_default").default(false)
    val createdAt = datetime("created_at")
}
```

#### 11. **Ads/Banners**
```kotlin
object Ads : Table("ads") {
    val id = uuid("id").primaryKey()
    val title = varchar("title", 255)
    val imageUrl = varchar("image_url", 500)
    val targetUrl = varchar("target_url", 500).nullable()
    val isActive = bool("is_active").default(true)
    val startDate = datetime("start_date")
    val endDate = datetime("end_date")
    val displayOrder = integer("display_order").default(0)
    val createdAt = datetime("created_at")
}
```

#### 12. **Audit Logs** (For compliance)
```kotlin
object AuditLogs : Table("audit_logs") {
    val id = uuid("id").primaryKey()
    val userId = reference("user_id", Users.id).nullable()
    val action = varchar("action", 100) // LOGIN, PRESCRIPTION_SENT, MESSAGE_SENT, etc.
    val entityType = varchar("entity_type", 50).nullable() // PRESCRIPTION, MESSAGE, etc.
    val entityId = uuid("entity_id").nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val metadata = json("metadata").nullable()
    val createdAt = datetime("created_at")
    
    index("entity_index", false, entityType, entityId)
}
```

### **Enums**
```kotlin
enum class UserRole { PATIENT, PHARMACIST, ADMIN }
enum class PrescriptionStatus { PENDING, SENT_TO_PHARMACY, ACCEPTED, REJECTED, FILLED, CANCELLED }
enum class ReminderType { ONCE, DAILY, WEEKLY, MONTHLY, CUSTOM }
```

---

## 🔐 Authentication & Authorization

### **JWT Token Structure**
```kotlin
data class JwtClaims(
    val userId: UUID,
    val email: String,
    val role: UserRole,
    val issuedAt: Long,
    val expiresAt: Long,
    val refreshTokenId: UUID?
)
```

### **Auth Flow**
1. **Registration**: Email/password + phone verification (SMS OTP)
2. **Login**: Returns access token (15min) + refresh token (7 days)
3. **Token Refresh**: Silent refresh using refresh token
4. **Password Reset**: Email-based reset link
5. **Role-Based Access**: Different endpoints for patients vs pharmacists

### **Security Features**
- Password hashing with bcrypt (cost factor 12)
- Rate limiting on auth endpoints
- Account lockout after 5 failed attempts
- HTTPS enforcement
- CORS configuration for mobile apps
- Input validation with Zod-like Kotlin validation

---

## 📡 API Endpoints Design

### **Authentication**
```
POST   /api/v1/auth/register          # Register new user
POST   /api/v1/auth/login             # Login
POST   /api/v1/auth/refresh           # Refresh token
POST   /api/v1/auth/logout            # Logout (invalidate refresh token)
POST   /api/v1/auth/forgot-password   # Request password reset
POST   /api/v1/auth/reset-password    # Reset password with token
POST   /api/v1/auth/verify-phone      # Verify phone with OTP
GET    /api/v1/auth/me                # Get current user profile
PUT    /api/v1/auth/me                # Update profile
```

### **Medicines**
```
GET    /api/v1/medicines              # List medicines (paginated, filterable)
GET    /api/v1/medicines/{id}         # Get medicine details
GET    /api/v1/medicines/search       # Search by name (Arabic/English)
GET    /api/v1/medicines/{id}/alternatives  # Get alternative medicines
GET    /api/v1/categories             # List categories
GET    /api/v1/subcategories          # List subcategories
```

### **Pharmacies**
```
GET    /api/v1/pharmacies             # List pharmacies (geo-sorted)
GET    /api/v1/pharmacies/nearby      # Find nearby pharmacies (lat, lng, radius)
GET    /api/v1/pharmacies/{id}        # Get pharmacy details
POST   /api/v1/pharmacies             # Register pharmacy (pharmacist only)
PUT    /api/v1/pharmacies/{id}        # Update pharmacy info
GET    /api/v1/pharmacies/{id}/ratings # Get pharmacy ratings
POST   /api/v1/pharmacies/{id}/ratings # Add rating
```

### **Prescriptions**
```
POST   /api/v1/prescriptions          # Create prescription (upload image)
GET    /api/v1/prescriptions          # List user's prescriptions
GET    /api/v1/prescriptions/{id}     # Get prescription details
PUT    /api/v1/prescriptions/{id}     # Update prescription (add notes)
DELETE /api/v1/prescriptions/{id}     # Cancel prescription
POST   /api/v1/prescriptions/{id}/send-to-pharmacy  # Send to specific pharmacy
GET    /api/v1/prescriptions/{id}/status  # Track prescription status
```

### **Chat/Messaging**
```
GET    /api/v1/chats                  # List user's chats
GET    /api/v1/chats/{chatId}         # Get chat messages (paginated)
POST   /api/v1/chats                  # Create/start new chat
WS     /ws/chat                       # WebSocket for real-time messaging
POST   /api/v1/messages/{messageId}/read  # Mark message as read
```

### **Favorites**
```
GET    /api/v1/favorites/medicines    # List favorite medicines
GET    /api/v1/favorites/pharmacies   # List favorite pharmacies
POST   /api/v1/favorites/medicines/{medicineId}  # Add medicine to favorites
DELETE /api/v1/favorites/medicines/{medicineId}  # Remove from favorites
POST   /api/v1/favorites/pharmacies/{pharmacyId} # Add pharmacy to favorites
DELETE /api/v1/favorites/pharmacies/{pharmacyId} # Remove from favorites
```

### **Reminders**
```
GET    /api/v1/reminders              # List user's reminders
POST   /api/v1/reminders              # Create reminder
PUT    /api/v1/reminders/{id}         # Update reminder
DELETE /api/v1/reminders/{id}         # Delete reminder
POST   /api/v1/reminders/sync         # Bulk sync from mobile app
```

### **Addresses**
```
GET    /api/v1/addresses              # List user's addresses
POST   /api/v1/addresses              # Add new address
PUT    /api/v1/addresses/{id}         # Update address
DELETE /api/v1/addresses/{id}         # Delete address
PUT    /api/v1/addresses/{id}/default # Set as default
```

### **Admin (Pharmacist/Admin only)**
```
GET    /api/v1/admin/prescriptions    # All prescriptions (filtered by pharmacy)
PUT    /api/v1/admin/prescriptions/{id}/status  # Update prescription status
GET    /api/v1/admin/users            # List users
PUT    /api/v1/admin/users/{id}/verify  # Verify user/pharmacy
GET    /api/v1/admin/analytics        # Dashboard analytics
```

---

## 🔄 Eilaji-Plus Integration

### **Prescription Routing Architecture**

```
┌─────────────────┐      ┌──────────────────────┐      ┌─────────────────┐
│   Eilaji App    │─────▶│   Ktor Backend       │─────▶│  Eilaji-Plus    │
│   (Patient)     │      │   (Prescription      │      │  (Processing)   │
│                 │      │    Service)          │      │                 │
└─────────────────┘      └──────────────────────┘      └─────────────────┘
                                │
                                │ Webhook Callback
                                ▼
                         ┌─────────────────┐
                         │  Status Update  │
                         │  to Eilaji App  │
                         └─────────────────┘
```

### **Integration Methods**

#### **Option 1: REST API (Recommended for MVP)**
```kotlin
// Backend sends prescription to eilaji-plus
suspend fun sendPrescriptionToEilajiPlus(prescription: Prescription): EilajiPlusResponse {
    return httpClient.post("${eilajiPlusBaseUrl}/api/prescriptions") {
        contentType(ContentType.Application.Json)
        setBody(prescription.toEilajiPlusDto())
        header("X-API-Key", eilajiPlusApiKey)
    }
}
```

#### **Option 2: Message Queue (Redis Streams)**
```kotlin
// Add prescription to queue
redis.xadd("eilaji-plus-prescriptions", mapOf(
    "prescription_id" to prescription.id.toString(),
    "patient_id" to prescription.patientId.toString(),
    "image_url" to prescription.imageUrl,
    "notes" to prescription.notes.orEmpty(),
    "timestamp" to System.currentTimeMillis().toString()
))

// Eilaji-plus consumes from queue
```

#### **Option 3: Webhooks (For status updates)**
```kotlin
// Eilaji-plus calls this endpoint when prescription is processed
post("/api/v1/webhooks/eilaji-plus/prescription-status") {
    val update = call.receive<PrescriptionStatusUpdate>()
    prescriptionService.updateStatus(update.prescriptionId, update.status)
    // Notify patient via FCM
    notificationService.sendPrescriptionStatusNotification(
        update.patientId, 
        update.status
    )
}
```

### **Data Contract for Eilaji-Plus**
```kotlin
data class EilajiPlusPrescription(
    val id: String,
    val patientId: String,
    val patientName: String,
    val patientPhone: String,
    val prescriptionImageUrl: String,
    val notes: String?,
    val location: Location?,
    val timestamp: Long,
    val priority: String // NORMAL, URGENT
)

data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String
)
```

---

## 💬 Real-Time Messaging Implementation

### **WebSocket Architecture**

```kotlin
// Ktor WebSocket route
webSocket("/ws/chat") {
    val userId = authenticateAndGetUserId()
    
    // Join user's chat rooms
    val userChats = chatService.getUserChats(userId)
    userChats.forEach { chat ->
        joinRoom("chat:${chat.id}")
    }
    
    // Handle incoming messages
    for (frame in incoming) {
        frame as? Frame.Text ?: continue
        val message = parseJson<MessageDto>(frame.readText())
        
        // Save to database
        val savedMessage = messageService.save(message)
        
        // Broadcast to chat room
        broadcast("chat:${message.chatId}", message)
        
        // Send push notification if receiver offline
        if (!userService.isOnline(message.receiverId)) {
            notificationService.sendChatNotification(message.receiverId, message)
        }
    }
}
```

### **Presence System**
```kotlin
// Track online users with Redis
redis.setex("user:$userId:online", 30, "true") // 30s TTL

// Heartbeat from client every 15s
// If no heartbeat, user marked offline automatically
```

### **Message Delivery Guarantee**
1. Message saved to PostgreSQL (persistent)
2. Broadcast via Redis Pub/Sub to all backend instances
3. WebSocket pushes to online recipients
4. FCM push notification if offline
5. Message marked as delivered when acknowledged

---

## 🗺️ Maps & Location Features

### **Geospatial Queries with PostgreSQL**

```kotlin
// Enable PostGIS extension
// CREATE EXTENSION IF NOT EXISTS postgis;

// Find nearby pharmacies
fun findNearbyPharmacies(lat: Double, lng: Double, radiusKm: Double): List<Pharmacy> {
    return transaction {
        Pharmacies.select {
            // Haversine formula for distance calculation
            eq(
                haversineDistance(lat, lng, Pharmacies.latitude, Pharmacies.longitude), 
                lessEq(radiusKm)
            )
        }.orderBy(haversineDistance(...)).limit(50)
    }
}

// Or use PostGIS
fun findNearbyPharmaciesPostGIS(lat: Double, lng: Double, radiusKm: Double): List<Pharmacy> {
    return transaction {
        exec(
            """
            SELECT * FROM pharmacies 
            WHERE ST_DWithin(
                ST_MakePoint(longitude, latitude)::geography,
                ST_MakePoint($lng, $lat)::geography,
                ${radiusKm * 1000}
            )
            ORDER BY ST_Distance(...)
            """.trimIndent()
        ) { /* map results */ }
    }
}
```

### **Location-Based Features**
- Nearby pharmacy discovery
- Distance calculation for delivery estimates
- Geofencing for pharmacy service areas
- Location-based prescription routing (nearest pharmacy first)

---

## 📱 Mobile App Integration Strategy

### **API Client Generation**
```bash
# Generate Kotlin client from OpenAPI spec
openapi-generator generate \
  -i openapi.yaml \
  -g kotlin \
  --library jvm-retrofit2 \
  -o ./app/src/main/java/dev/anonymous/eilaji/api
```

### **Repository Pattern Update**
```kotlin
// Before (Firebase)
class MedicineRepository {
    private val firestore = FirebaseFirestore.getInstance()
    
    suspend fun getMedicines(): Flow<List<Medicine>> {
        return callbackFlow {
            firestore.collection("medicines")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { close(error); return@addSnapshotListener }
                    trySend(snapshot?.toObjects(Medicine::class.java) ?: emptyList())
                }
        }
    }
}

// After (REST API)
class MedicineRepository(
    private val api: EilajiApiService,
    private val dao: MedicineDao // Local cache
) {
    suspend fun getMedicines(): Flow<List<Medicine>> {
        return flow {
            // Try cache first
            val cached = dao.getAll()
            if (cached.isNotEmpty()) emit(cached)
            
            // Fetch from API
            val fresh = api.getMedicines()
            dao.insertAll(fresh)
            emit(fresh)
        }.flowOn(Dispatchers.IO)
    }
}
```

### **Offline-First Strategy**
```kotlin
// Room database for caching
@Entity(tableName = "medicines_cache")
data class MedicineCache(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val imageUrl: String?,
    val price: Double?,
    val lastFetchedAt: Long,
    val data: String // JSON string
)

// Sync manager
class SyncManager(
    private val api: EilajiApiService,
    private val dao: CacheDao,
    private val workManager: WorkManager
) {
    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
```

---

## 🚀 Deployment & DevOps

### **Infrastructure Options**

#### **Option A: Docker + Kubernetes (Production)**
```yaml
# docker-compose.yml
version: '3.8'
services:
  api:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=jdbc:postgresql://db:5432/eilaji
      - REDIS_URL=redis://redis:6379
    depends_on:
      - db
      - redis
  
  db:
    image: postgres:15-alpine
    volumes:
      - postgres_data:/var/lib/postgresql/data
    environment:
      - POSTGRES_DB=eilaji
      - POSTGRES_USER=eilaji_user
      - POSTGRES_PASSWORD=secure_password
  
  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
  
  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data

volumes:
  postgres_data:
  redis_data:
  minio_data:
```

#### **Option B: Cloud Providers**
- **DigitalOcean App Platform** (Simplest)
- **AWS ECS Fargate** (Scalable)
- **Google Cloud Run** (Serverless containers)
- **Heroku** (Quick deployment)

### **CI/CD Pipeline**
```yaml
# .github/workflows/deploy.yml
name: Deploy Backend

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run tests
        run: ./gradlew test
  
  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build Docker image
        run: docker build -t eilaji-backend .
      - name: Push to registry
        run: docker push registry.eilaji.com/backend:latest
  
  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to production
        run: kubectl apply -f k8s/
```

### **Monitoring & Logging**
- **Prometheus + Grafana**: Metrics and dashboards
- **ELK Stack**: Centralized logging
- **Sentry**: Error tracking
- **Health checks**: `/health`, `/ready` endpoints

---

## 📈 Development Roadmap

### **Phase 1: Foundation (Weeks 1-3)**
- [ ] Project setup with Ktor + Exposed
- [ ] Database schema implementation
- [ ] Basic authentication (register, login, JWT)
- [ ] User profile management
- [ ] Health check endpoints

### **Phase 2: Core Features (Weeks 4-6)**
- [ ] Medicine catalog API (CRUD, search)
- [ ] Category/subcategory management
- [ ] Pharmacy registration and listing
- [ ] Geospatial queries for nearby pharmacies
- [ ] File upload (prescription images)

### **Phase 3: Messaging (Weeks 7-8)**
- [ ] WebSocket setup for real-time chat
- [ ] Chat rooms and message persistence
- [ ] Presence system (online/offline)
- [ ] Push notifications (FCM integration)
- [ ] Message read receipts

### **Phase 4: Prescriptions (Weeks 9-10)**
- [ ] Prescription creation and upload
- [ ] Prescription status workflow
- [ ] Eilaji-plus integration (REST API)
- [ ] Webhook handling for status updates
- [ ] Notification system for prescription updates

### **Phase 5: Advanced Features (Weeks 11-12)**
- [ ] Favorites system
- [ ] Ratings and reviews
- [ ] Reminders sync API
- [ ] Address management
- [ ] Ads/banners management

### **Phase 6: Admin & Analytics (Weeks 13-14)**
- [ ] Admin dashboard endpoints
- [ ] User verification workflow
- [ ] Analytics and reporting
- [ ] Audit logging
- [ ] Rate limiting and abuse prevention

### **Phase 7: Mobile Integration (Weeks 15-16)**
- [ ] API client generation
- [ ] Repository pattern refactoring
- [ ] Offline-first caching
- [ ] Sync manager implementation
- [ ] Testing and bug fixes

### **Phase 8: Production Ready (Weeks 17-18)**
- [ ] Load testing
- [ ] Security audit
- [ ] Documentation completion
- [ ] Deployment pipeline
- [ ] Monitoring setup
- [ ] Backup strategy

---

## 🔒 Security Considerations

### **Healthcare Data Compliance**
- **Encryption at rest**: PostgreSQL TDE or disk encryption
- **Encryption in transit**: TLS 1.3 everywhere
- **Access logs**: Audit trail for all sensitive operations
- **Data retention**: Configurable policies for prescription data
- **Right to deletion**: GDPR-compliant user data deletion

### **API Security**
- Rate limiting (100 requests/minute per user)
- Input validation (prevent SQL injection, XSS)
- CORS configuration (only allow mobile app origins)
- API versioning (`/api/v1/...`)
- Request signing for sensitive operations

### **Authentication Hardening**
- Multi-factor authentication (optional for pharmacists)
- Session management (revoke sessions)
- Password complexity requirements
- Account recovery with identity verification

---

## 📝 Code Examples

### **Ktor Application Setup**
```kotlin
// Application.kt
fun main() {
    embeddedServer(Netty, port = 8080) {
        configureDatabase()
        configureRouting()
        configureSerialization()
        configureSecurity()
        configureWebSockets()
        configureCors()
        configureRateLimiting()
    }.start(wait = true)
}

// Database.kt
fun Application.configureDatabase() {
    val dbConfig = environment.config.config("database")
    Database.connect(
        url = dbConfig.property("url").getString(),
        driver = "org.postgresql.Driver",
        user = dbConfig.property("user").getString(),
        password = dbConfig.property("password").getString()
    )
    
    // Run migrations
    transaction {
        SchemaUtils.createMissingTablesAndColumns(
            Users, Pharmacies, Medicines, Categories, SubCategories,
            Prescriptions, Chats, Messages, Favorites, Ratings,
            Reminders, Addresses, Ads, AuditLogs, MedicineAlternatives
        )
    }
}

// Routing.kt
fun Application.configureRouting() {
    install(Routing) {
        // Health checks
        get("/health") { call.respondText("OK") }
        
        // API v1
        route("/api/v1") {
            // Public routes
            route("/auth") {
                post("/register") { /* ... */ }
                post("/login") { /* ... */ }
                post("/refresh") { /* ... */ }
            }
            
            // Protected routes
            authenticate("jwt-auth") {
                route("/medicines") {
                    get { /* List medicines */ }
                    get("/{id}") { /* Get medicine */ }
                }
                
                route("/prescriptions") {
                    post { /* Create prescription */ }
                    get { /* List prescriptions */ }
                }
                
                // WebSocket
                webSocket("/ws/chat") { /* ... */ }
            }
        }
    }
}
```

### **Repository Pattern Example**
```kotlin
interface MedicineRepository {
    suspend fun getAll(page: Int, limit: Int): List<Medicine>
    suspend fun getById(id: UUID): Medicine?
    suspend fun search(query: String, language: String): List<Medicine>
    suspend fun getAlternatives(medicineId: UUID): List<Medicine>
    suspend fun getByCategory(categoryId: UUID, page: Int, limit: Int): List<Medicine>
}

class MedicineRepositoryImpl(
    private val database: Database
) : MedicineRepository {
    
    override suspend fun getAll(page: Int, limit: Int): List<Medicine> {
        return withContext(Dispatchers.IO) {
            transaction {
                Medicines.select { Medicines.isActive eq true }
                    .limit(limit, offset = (page - 1) * limit.toLong())
                    .map { it.toMedicine() }
            }
        }
    }
    
    override suspend fun search(query: String, language: String): List<Medicine> {
        return withContext(Dispatchers.IO) {
            transaction {
                val nameColumn = if (language == "ar") Medicines.nameAr else Medicines.nameEn
                Medicines.select {
                    Medicines.isActive eq true and 
                    nameColumn.like("%$query%", ignoreCase = true)
                }.map { it.toMedicine() }
            }
        }
    }
    
    // ... other methods
}
```

---

## 📚 Resources & Learning Path

### **Documentation**
- [Ktor Official Docs](https://ktor.io/docs/welcome.html)
- [Exposed GitHub Wiki](https://github.com/JetBrains/Exposed/wiki)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [JWT.io](https://jwt.io/introduction)

### **Sample Projects**
- [Ktor Sample Backend](https://github.com/ktorio/ktor-samples)
- [Exposed Examples](https://github.com/JetBrains/Exposed-examples)

### **Books**
- "Kotlin in Action" (for advanced Kotlin)
- "Building Microservices with Kotlin" (O'Reilly)

---

## 💰 Cost Comparison

### **Firebase (Current)**
- Firestore: ~$0.036/100K reads (scales quickly with chat messages)
- Realtime Database: ~$1/GB stored + $0.10/GB transferred
- Authentication: Free up to 10K/month, then $0.015/1K verifications
- Cloud Functions: ~$0.0000025/invocation
- Storage: ~$0.026/GB
- **Estimated monthly cost at 10K users**: $200-500

### **Self-Hosted Backend**
- VPS (DigitalOcean/AWS): $20-40/month (basic)
- Managed PostgreSQL: $15-30/month
- Managed Redis: $10-20/month
- Object Storage (MinIO/S3): $5-10/month
- **Estimated monthly cost at 10K users**: $50-100
- **At 100K users**: $200-400 (vs $2000+ with Firebase)

**ROI**: Break-even at ~5K active users, significant savings thereafter.

---

## ✅ Success Metrics

### **Technical**
- API response time < 200ms (p95)
- WebSocket latency < 50ms
- 99.9% uptime SLA
- Zero data loss guarantee

### **Business**
- Reduced infrastructure costs by 60%
- Faster feature development (no Firebase limitations)
- Full control over data and compliance
- Seamless eilaji-plus integration

### **User Experience**
- Real-time chat with <1s delivery
- Instant prescription status updates
- Reliable offline mode
- Smooth performance at scale

---

## 🎉 Conclusion

This migration plan leverages your existing Kotlin expertise while introducing you to cutting-edge backend technologies. The Ktor + Exposed + PostgreSQL stack provides:

1. **Type safety** from database to API
2. **Full control** over your healthcare data
3. **Cost efficiency** as you scale
4. **Seamless integration** with eilaji-plus
5. **Modern architecture** ready for growth

The phased approach ensures you can iterate quickly while maintaining stability. Start with Phase 1 this week, and you'll have a production-ready backend in 18 weeks!

**Next Steps:**
1. Review and approve this plan
2. Set up development environment
3. Create GitHub repository for backend
4. Begin Phase 1 implementation

Let's make Eilaji shine! 🚀
