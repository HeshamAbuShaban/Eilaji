package com.eilaji.backend.service

import com.eilaji.backend.data.*
import com.eilaji.backend.dto.*
import com.eilaji.backend.security.ApiException
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable

class OrderService {

    @Serializable
    data class OrderItemRequest(
        val medicineId: String? = null,
        val medicineName: String = "",
        val quantity: Int = 1,
        val unitPrice: Double = 0.0
    )

    @Serializable
    data class OrderCreateRequest(
        val prescriptionId: String? = null,
        val pharmacyId: String,
        val totalAmount: Double = 0.0,
        val paymentMethod: String? = null,
        val deliveryAddress: String? = null,
        val deliveryNotes: String? = null,
        val notes: String? = null,
        val items: List<OrderItemRequest> = emptyList()
    )

    companion object {
        val ORDER_STATUSES = setOf("PENDING", "CONFIRMED", "PREPARING", "SHIPPED", "DELIVERED", "CANCELLED")
        // Legacy aliases accepted from older clients
        private val STATUS_ALIASES = mapOf("PAID" to "CONFIRMED", "PROCESSING" to "PREPARING")
        val PAYMENT_STATUSES = setOf("PENDING", "PAID", "FAILED", "REFUNDED", "COD_COLLECTED")
        private val TERMINAL = setOf("DELIVERED", "CANCELLED")
        private val TRANSITIONS = mapOf(
            "PENDING" to setOf("CONFIRMED", "CANCELLED"),
            "CONFIRMED" to setOf("PREPARING", "CANCELLED"),
            "PREPARING" to setOf("SHIPPED", "CANCELLED"),
            "SHIPPED" to setOf("DELIVERED", "CANCELLED"),
            "DELIVERED" to emptySet(),
            "CANCELLED" to emptySet()
        )
        private val PAYMENT_TRANSITIONS = mapOf(
            "PENDING" to setOf("PAID", "FAILED"),
            "PAID" to setOf("REFUNDED"),
            "FAILED" to setOf("PAID"),
            "COD_COLLECTED" to setOf("REFUNDED"),
            "REFUNDED" to emptySet()
        )
        const val MAX_ORDER_TOTAL = 100_000.0

        private val secureRandom = SecureRandom()

        /** Four digits from a cryptographic source: the code is shown to the courier and the patient. */
        private fun secureRandomHandoffCode(): String = (1..4).map { secureRandom.nextInt(10) }.joinToString("")

        fun normalizeStatus(raw: String): String? {
            val s = raw.trim().uppercase()
            if (ORDER_STATUSES.contains(s)) return s
            return STATUS_ALIASES[s]
        }
    }

    @Serializable
    data class OrderItemResult(
        val id: String,
        val medicineId: String?,
        val medicineName: String,
        val quantity: Int,
        val unitPrice: Double,
        val lineTotal: Double
    )

    @Serializable
    data class OrderUpdateStatusRequest(
        val status: String,
        val paymentStatus: String? = null
    )

    @Serializable
    data class OrderResult(
        val id: String,
        val prescriptionId: String? = null,
        val patientId: String,
        val pharmacyId: String,
        val pharmacyName: String?,
        val status: String,
        val totalAmount: Double,
        val paymentMethod: String?,
        val paymentStatus: String,
        val deliveryAddress: String?,
        val deliveryNotes: String?,
        val items: List<OrderItemResult> = emptyList(),
        val courierLat: Double? = null,
        val courierLng: Double? = null,
        val etaMinutes: Int? = null,
        val handoffCode: String? = null,
        val createdAt: String,
        val updatedAt: String
    )

    /**
     * Creates an order.
     *
     * The total is always recomputed on the server from the supplied items. When the client sends
     * no items the total is accepted but bounded, and no stock is touched.
     */
    fun createOrder(request: OrderCreateRequest, userId: String): OrderResult {
        val userUuid = try { UUID.fromString(userId) } catch (_: Exception) {
            throw ApiException(HttpStatusCode.Unauthorized, "Invalid user")
        }
        val pharmacyUuid = try { UUID.fromString(request.pharmacyId) } catch (_: Exception) {
            throw ApiException(HttpStatusCode.BadRequest, "Invalid pharmacy")
        }
        val prescriptionUuid = request.prescriptionId?.trim()?.takeIf { it.isNotEmpty() }?.let {
            try {
                UUID.fromString(it)
            } catch (_: Exception) {
                throw ApiException(HttpStatusCode.BadRequest, "Invalid prescription")
            }
        }

        val items = request.items.mapNotNull { item ->
            val name = item.medicineName.trim().ifEmpty { null }
            if (name == null && item.medicineId == null) null
            else item to name
        }
        if (items.size > 50) throw ApiException(HttpStatusCode.BadRequest, "Too many items in one order")
        items.forEach { (item, _) ->
            if (item.quantity < 1 || item.quantity > 99) throw ApiException(HttpStatusCode.BadRequest, "Item quantity must be between 1 and 99")
            if (item.unitPrice < 0) throw ApiException(HttpStatusCode.BadRequest, "Item price cannot be negative")
        }

        val computedTotal = items.fold(BigDecimal.ZERO) { acc, (item, _) ->
            acc + BigDecimal.valueOf(item.unitPrice).multiply(BigDecimal.valueOf(item.quantity.toLong()))
        }.setScale(2, java.math.RoundingMode.HALF_UP)

        val total = if (items.isNotEmpty()) computedTotal else {
            val requested = BigDecimal.valueOf(request.totalAmount).setScale(2, java.math.RoundingMode.HALF_UP)
            if (requested <= BigDecimal.ZERO) throw ApiException(HttpStatusCode.BadRequest, "Order total must be greater than zero")
            if (requested.toDouble() > MAX_ORDER_TOTAL) throw ApiException(HttpStatusCode.BadRequest, "Order total is too large")
            requested
        }

        return transaction {
            if (prescriptionUuid != null) {
                val prescription = Prescriptions.selectAll().where { Prescriptions.id eq prescriptionUuid }.firstOrNull()
                    ?: throw ApiException(HttpStatusCode.NotFound, "Prescription not found")
                if (prescription[Prescriptions.patientUserId] != userUuid) {
                    throw ApiException(HttpStatusCode.Forbidden, "This prescription belongs to another account")
                }
                if (!prescription[Prescriptions.status].equals("ACCEPTED", ignoreCase = true)) {
                    throw ApiException(HttpStatusCode.BadRequest, "Prescription is not accepted yet")
                }
            }

            val pharmacy = Pharmacies.selectAll().where { Pharmacies.id eq pharmacyUuid }.singleOrNull()
                ?: throw ApiException(HttpStatusCode.NotFound, "Pharmacy not found")

            // Reserve stock for tracked items before creating the order.
            val reservations = items.mapNotNull { (item, _) ->
                val medicineId = item.medicineId?.let { mid -> try { UUID.fromString(mid) } catch (_: Exception) { null } }
                    ?: return@mapNotNull null
                val stockRow = PharmacyMedicines.selectAll()
                    .where { (PharmacyMedicines.pharmacyId eq pharmacyUuid) and (PharmacyMedicines.medicineId eq medicineId) }
                    .singleOrNull() ?: return@mapNotNull null
                val available = stockRow[PharmacyMedicines.stockQuantity]
                if (available <= 0) return@mapNotNull null // 0 or unknown stock: not tracked, order is allowed
                if (item.quantity > available) {
                    throw ApiException(HttpStatusCode.Conflict, "Only $available left of ${item.medicineName}")
                }
                medicineId to item.quantity
            }

            val orderId = Orders.insert {
                it[Orders.prescriptionId] = prescriptionUuid
                it[Orders.patientId] = userUuid
                it[Orders.pharmacyId] = pharmacyUuid
                it[Orders.status] = "PENDING"
                it[Orders.totalAmount] = total
                it[Orders.paymentMethod] = request.paymentMethod?.trim()?.take(20)
                it[Orders.paymentStatus] = "PENDING"
                it[Orders.deliveryAddress] = request.deliveryAddress?.trim()?.take(500)
                it[Orders.deliveryNotes] = (request.deliveryNotes ?: request.notes)?.trim()?.take(500)
                it[Orders.handoffCode] = secureRandomHandoffCode()
                it[Orders.createdAt] = Instant.now()
                it[Orders.updatedAt] = Instant.now()
            } get Orders.id

            items.forEach { (item, name) ->
                val medicineId = item.medicineId?.let { mid -> try { UUID.fromString(mid) } catch (_: Exception) { null } }
                OrderItems.insert {
                    it[OrderItems.orderId] = orderId
                    it[OrderItems.medicineId] = medicineId
                    it[OrderItems.medicineName] = name ?: "Medicine"
                    it[OrderItems.quantity] = item.quantity
                    it[OrderItems.unitPrice] = BigDecimal.valueOf(item.unitPrice).setScale(2, java.math.RoundingMode.HALF_UP)
                    it[OrderItems.lineTotal] = BigDecimal.valueOf(item.unitPrice * item.quantity).setScale(2, java.math.RoundingMode.HALF_UP)
                }
            }

            reservations.forEach { (medicineId, quantity) ->
                PharmacyMedicines.update({
                    (PharmacyMedicines.pharmacyId eq pharmacyUuid) and (PharmacyMedicines.medicineId eq medicineId)
                }) {
                    with(SqlExpressionBuilder) {
                        it.update(PharmacyMedicines.stockQuantity, PharmacyMedicines.stockQuantity - quantity)
                    }
                }
            }

            getOrderById(orderId.toString(), userId, null)
                ?: throw ApiException(HttpStatusCode.InternalServerError, "Order was created but could not be read back")
        }
    }

    /** Patient cancels their own order, or the pharmacy/admin cancels an order they manage. */
    fun cancelOrder(orderId: String, userId: String, userRole: com.eilaji.backend.data.UserRole): OrderResult {
        val orderUuid = try { UUID.fromString(orderId) } catch (_: Exception) {
            throw ApiException(HttpStatusCode.BadRequest, "Invalid order")
        }
        val userUuid = try { UUID.fromString(userId) } catch (_: Exception) {
            throw ApiException(HttpStatusCode.Unauthorized, "Invalid user")
        }
        return transaction {
            val order = Orders.selectAll().where { Orders.id eq orderUuid }.singleOrNull()
                ?: throw ApiException(HttpStatusCode.NotFound, "Order not found")
            val isOwner = order[Orders.patientId] == userUuid
            if (!isOwner && userRole != com.eilaji.backend.data.UserRole.ADMIN) {
                val owned = Pharmacies.selectAll().where { Pharmacies.ownerUserId eq userUuid }.map { it[Pharmacies.id] }
                if (order[Orders.pharmacyId] !in owned) throw ApiException(HttpStatusCode.NotFound, "Order not found")
            }
            if (!isOwner && userRole == com.eilaji.backend.data.UserRole.PATIENT) {
                throw ApiException(HttpStatusCode.Forbidden, "You can only cancel your own orders")
            }
            val current = order[Orders.status].uppercase()
            if (current == "DELIVERED") throw ApiException(HttpStatusCode.Conflict, "A delivered order cannot be cancelled")
            if (current == "CANCELLED") return@transaction mapRowToOrder(order)

            Orders.update({ Orders.id eq orderUuid }) {
                it[Orders.status] = "CANCELLED"
                it[Orders.updatedAt] = Instant.now()
            }
            // Give reserved stock back for the items we know about.
            val items = OrderItems.selectAll().where { OrderItems.orderId eq orderUuid }
            items.forEach { row ->
                val medicineId = row[OrderItems.medicineId] ?: return@forEach
                val updated = PharmacyMedicines.update({
                    (PharmacyMedicines.pharmacyId eq order[Orders.pharmacyId]) and
                        (PharmacyMedicines.medicineId eq medicineId) and
                        (PharmacyMedicines.stockQuantity greater 0)
                }) {
                    with(SqlExpressionBuilder) {
                        it.update(PharmacyMedicines.stockQuantity, PharmacyMedicines.stockQuantity + row[OrderItems.quantity])
                    }
                }
                if (updated == 0) {
                    // Stock row is absent or not tracked; nothing to restore.
                }
            }

            getOrderById(orderId, userId, userRole)
                ?: throw ApiException(HttpStatusCode.NotFound, "Order not found")
        }
    }

    fun getUserOrders(userId: String, userRole: com.eilaji.backend.data.UserRole, page: Int = 0, pageSize: Int = 20): PaginatedResult<OrderResult> {
        val userUuid = try { UUID.fromString(userId) } catch (_: Exception) { return PaginatedResult(emptyList(), 0, page, pageSize, 0) }
        return transaction {
            val baseQuery = when (userRole) {
                com.eilaji.backend.data.UserRole.ADMIN ->
                    Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id).selectAll()
                com.eilaji.backend.data.UserRole.PHARMACIST, com.eilaji.backend.data.UserRole.USER -> {
                    val owned = Pharmacies.selectAll().where { Pharmacies.ownerUserId eq userUuid }.map { it[Pharmacies.id] }
                    if (owned.isEmpty()) return@transaction PaginatedResult(emptyList(), 0, page, pageSize, 0)
                    Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id)
                        .selectAll()
                        .where { Orders.pharmacyId inList owned }
                }
                else ->
                    Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id)
                        .selectAll()
                        .where { Orders.patientId eq userUuid }
            }

            val total = baseQuery.count()
            val orders = baseQuery
                .orderBy(Orders.createdAt, SortOrder.DESC)
                .limit(pageSize, (page * pageSize).toLong())
                .map { row -> mapRowToOrder(row) }

            PaginatedResult(
                items = orders,
                total = total,
                page = page,
                pageSize = pageSize,
                totalPages = if (total > 0) ((total + pageSize - 1) / pageSize).toInt() else 0
            )
        }
    }

    fun getOrderById(orderId: String, userId: String, userRole: com.eilaji.backend.data.UserRole?): OrderResult? {
        val userUuid = try { UUID.fromString(userId) } catch (_: Exception) { return null }
        val orderUuid = try { UUID.fromString(orderId) } catch (_: Exception) { return null }
        return transaction {
            val order = Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id)
                .selectAll()
                .where { Orders.id eq orderUuid }
                .firstOrNull() ?: return@transaction null

            when (userRole) {
                null, com.eilaji.backend.data.UserRole.PATIENT, com.eilaji.backend.data.UserRole.USER ->
                    if (order[Orders.patientId] != userUuid) return@transaction null
                com.eilaji.backend.data.UserRole.PHARMACIST -> {
                    val owned = Pharmacies.selectAll().where { Pharmacies.ownerUserId eq userUuid }.map { it[Pharmacies.id] }
                    if (order[Orders.pharmacyId] !in owned) return@transaction null
                }
                com.eilaji.backend.data.UserRole.ADMIN -> Unit
            }

            mapRowToOrder(order)
        }
    }

    fun updateOrderStatus(
        orderId: String,
        status: String,
        paymentStatus: String?,
        userId: String,
        userRole: com.eilaji.backend.data.UserRole
    ): OrderResult {
        val orderUuid = try { UUID.fromString(orderId) } catch (_: Exception) {
            throw ApiException(HttpStatusCode.BadRequest, "Invalid order")
        }
        val userUuid = try { UUID.fromString(userId) } catch (_: Exception) {
            throw ApiException(HttpStatusCode.Unauthorized, "Invalid user")
        }
        val next = normalizeStatus(status)
            ?: throw ApiException(HttpStatusCode.BadRequest, "Unknown status: $status")

        val nextPayment = paymentStatus?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }?.let {
            if (!PAYMENT_STATUSES.contains(it)) throw ApiException(HttpStatusCode.BadRequest, "Unknown payment status")
            it
        }

        return transaction {
            val existingOrder = Orders.selectAll().where { Orders.id eq orderUuid }.singleOrNull()
                ?: throw ApiException(HttpStatusCode.NotFound, "Order not found")

            if (userRole != com.eilaji.backend.data.UserRole.PHARMACIST && userRole != com.eilaji.backend.data.UserRole.ADMIN) {
                throw ApiException(HttpStatusCode.Forbidden, "Only pharmacists and admins can update an order")
            }
            if (userRole == com.eilaji.backend.data.UserRole.PHARMACIST) {
                val owned = Pharmacies.selectAll().where { Pharmacies.ownerUserId eq userUuid }.map { it[Pharmacies.id] }
                if (existingOrder[Orders.pharmacyId] !in owned) throw ApiException(HttpStatusCode.NotFound, "Order not found")
            }

            val current = existingOrder[Orders.status].uppercase()
            if (TERMINAL.contains(current)) throw ApiException(HttpStatusCode.Conflict, "Order is already $current")
            val allowed = TRANSITIONS[current] ?: throw ApiException(HttpStatusCode.Conflict, "Order is already $current")
            if (next !in allowed) throw ApiException(HttpStatusCode.Conflict, "Cannot move an order from $current to $next")

            val currentPayment = existingOrder[Orders.paymentStatus].uppercase()
            val paymentAllowed = PAYMENT_TRANSITIONS[currentPayment] ?: setOf()
            if (nextPayment != null && nextPayment != currentPayment && nextPayment !in paymentAllowed) {
                throw ApiException(HttpStatusCode.Conflict, "Cannot change payment status from $currentPayment to $nextPayment")
            }

            Orders.update({ Orders.id eq orderUuid }) {
                it[Orders.status] = next
                if (nextPayment != null) it[Orders.paymentStatus] = nextPayment
                it[Orders.updatedAt] = Instant.now()
            }

            getOrderById(orderId, userId, userRole)!!
        }
    }

    fun updateCourier(orderId: String, lat: Double?, lng: Double?, etaMinutes: Int?, clear: Boolean, userId: String, userRole: com.eilaji.backend.data.UserRole): OrderResult? {
        val orderUuid = try { UUID.fromString(orderId) } catch (_: Exception) { return null }
        val userUuid = try { UUID.fromString(userId) } catch (_: Exception) { return null }
        if ((lat == null || lng == null) && !clear) return null
        if (lat != null && (lat < -90 || lat > 90)) return null
        if (lng != null && (lng < -180 || lng > 180)) return null
        if (etaMinutes != null && (etaMinutes < 0 || etaMinutes > 600)) return null
        return transaction {
            val existingOrder = Orders.selectAll().where { Orders.id eq orderUuid }.singleOrNull()
                ?: return@transaction null
            if (userRole != com.eilaji.backend.data.UserRole.PHARMACIST && userRole != com.eilaji.backend.data.UserRole.ADMIN) {
                return@transaction null
            }
            if (userRole == com.eilaji.backend.data.UserRole.PHARMACIST) {
                val owned = Pharmacies.selectAll().where { Pharmacies.ownerUserId eq userUuid }.map { it[Pharmacies.id] }
                if (existingOrder[Orders.pharmacyId] !in owned) return@transaction null
            }
            Orders.update({ Orders.id eq orderUuid }) {
                if (clear) {
                    it[Orders.courierLat] = null
                    it[Orders.courierLng] = null
                    it[Orders.etaMinutes] = null
                } else {
                    if (lat != null) it[Orders.courierLat] = lat
                    if (lng != null) it[Orders.courierLng] = lng
                    if (etaMinutes != null) it[Orders.etaMinutes] = etaMinutes
                }
                it[Orders.updatedAt] = Instant.now()
            }
            getOrderById(orderId, userId, userRole)
        }
    }

    fun getOrdersForPharmacy(pharmacyId: String, status: String? = null, page: Int = 0, pageSize: Int = 20): PaginatedResult<OrderResult> {
        val pharmUuid = try { UUID.fromString(pharmacyId) } catch (_: Exception) {
            return PaginatedResult(emptyList(), 0, page, pageSize, 0)
        }
        return transaction {
            val base = Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id)
                .selectAll()
                .where { Orders.pharmacyId eq pharmUuid }
            val query = if (status != null) base.andWhere { Orders.status eq status } else base
            val total = query.count()
            val orders = query.orderBy(Orders.createdAt, SortOrder.DESC)
                .limit(pageSize, (page * pageSize).toLong())
                .map { row -> mapRowToOrder(row) }
            PaginatedResult(orders, total, page, pageSize, if (total > 0) ((total + pageSize - 1) / pageSize).toInt() else 0)
        }
    }

    private fun mapRowToOrder(row: ResultRow): OrderResult {
        val orderId = row[Orders.id]
        val items = try {
            OrderItems.selectAll().where { OrderItems.orderId eq orderId }.map { item ->
                OrderItemResult(
                    id = item[OrderItems.id].toString(),
                    medicineId = item[OrderItems.medicineId]?.toString(),
                    medicineName = item[OrderItems.medicineName],
                    quantity = item[OrderItems.quantity],
                    unitPrice = item[OrderItems.unitPrice].toDouble(),
                    lineTotal = item[OrderItems.lineTotal].toDouble()
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
        return OrderResult(
            id = orderId.toString(),
            prescriptionId = try { row.getOrNull(Orders.prescriptionId)?.toString() } catch (_: Exception) { null },
            patientId = row[Orders.patientId].toString(),
            pharmacyId = row[Orders.pharmacyId].toString(),
            pharmacyName = row.getOrNull(Pharmacies.name),
            status = row[Orders.status],
            totalAmount = row[Orders.totalAmount].toDouble(),
            paymentMethod = row[Orders.paymentMethod],
            paymentStatus = row[Orders.paymentStatus],
            deliveryAddress = row[Orders.deliveryAddress],
            deliveryNotes = row[Orders.deliveryNotes],
            items = items,
            courierLat = try { row.getOrNull(Orders.courierLat) } catch (_: Exception) { null },
            courierLng = try { row.getOrNull(Orders.courierLng) } catch (_: Exception) { null },
            etaMinutes = try { row.getOrNull(Orders.etaMinutes) } catch (_: Exception) { null },
            handoffCode = try { row.getOrNull(Orders.handoffCode) } catch (_: Exception) { null },
            createdAt = row[Orders.createdAt].toString(),
            updatedAt = row[Orders.updatedAt].toString()
        )
    }
}
