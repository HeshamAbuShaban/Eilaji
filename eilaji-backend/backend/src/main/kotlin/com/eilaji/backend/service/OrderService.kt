package com.eilaji.backend.service

import com.eilaji.backend.data.*
import com.eilaji.backend.dto.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

class OrderService {

    @Serializable
    data class OrderCreateRequest(
        val prescriptionId: String? = null,
        val pharmacyId: String,
        val totalAmount: Double,
        val paymentMethod: String? = null,
        val deliveryAddress: String? = null,
        val deliveryNotes: String? = null,
        val notes: String? = null
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

        fun normalizeStatus(raw: String): String? {
            val s = raw.trim().uppercase()
            if (ORDER_STATUSES.contains(s)) return s
            return STATUS_ALIASES[s]
        }
    }

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
        val courierLat: Double? = null,
        val courierLng: Double? = null,
        val etaMinutes: Int? = null,
        val handoffCode: String? = null,
        val createdAt: String,
        val updatedAt: String
    )

    fun createOrder(request: OrderCreateRequest, userId: String): OrderResult? {
        val userUuid = try { UUID.fromString(userId) } catch (_: Exception) { return null }
        val pharmacyUuid = try { UUID.fromString(request.pharmacyId) } catch (_: Exception) { return null }
        // Prescription-linked flow: must reference an ACCEPTED prescription.
        // Direct OTC flow: null/blank prescriptionId creates a prescription-less order.
        val prescriptionUuid = request.prescriptionId?.trim()?.takeIf { it.isNotEmpty() }?.let {
            try { UUID.fromString(it) } catch (_: Exception) { return null }
        }
        return transaction {
            if (prescriptionUuid != null) {
                val prescription = Prescriptions.selectAll()
                    .where { Prescriptions.id eq prescriptionUuid }
                    .firstOrNull()
                if (prescription == null || prescription[Prescriptions.status] != "ACCEPTED") {
                    return@transaction null
                }
            }
            val pharmacyExists = Pharmacies.selectAll().where { Pharmacies.id eq pharmacyUuid }.singleOrNull() != null
            if (!pharmacyExists) return@transaction null

            val orderId = Orders.insert {
                it[Orders.prescriptionId] = prescriptionUuid
                it[Orders.patientId] = userUuid
                it[Orders.pharmacyId] = pharmacyUuid
                it[Orders.status] = "PENDING"
                it[Orders.totalAmount] = request.totalAmount.toBigDecimal()
                it[Orders.paymentMethod] = request.paymentMethod
                it[Orders.paymentStatus] = "PENDING"
                it[Orders.deliveryAddress] = request.deliveryAddress
                it[Orders.deliveryNotes] = request.deliveryNotes ?: request.notes
                try { it[Orders.handoffCode] = (1000 + kotlin.random.Random.nextInt(9000)).toString() } catch (_: Exception) {}
                it[Orders.createdAt] = Instant.now()
                it[Orders.updatedAt] = Instant.now()
            } get Orders.id

            getOrderById(orderId.toString(), userId, null)
        }
    }

    fun getUserOrders(userId: String, userRole: com.eilaji.backend.data.UserRole): List<OrderResult> {
        val userUuid = try { UUID.fromString(userId) } catch (_: Exception) { return emptyList() }
        return transaction {
            val query = when (userRole) {
                com.eilaji.backend.data.UserRole.ADMIN ->
                    Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id).selectAll()
                com.eilaji.backend.data.UserRole.PHARMACIST -> {
                    // Scope to pharmacies owned by this pharmacist
                    val owned = Pharmacies.selectAll().where { Pharmacies.ownerUserId eq userUuid }.map { it[Pharmacies.id] }
                    Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id)
                        .selectAll()
                        .where { Orders.pharmacyId inList owned }
                }
                else ->
                    Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id)
                        .selectAll()
                        .where { Orders.patientId eq userUuid }
            }

            query.orderBy(Orders.createdAt, SortOrder.DESC).map { row ->
                mapRowToOrder(row)
            }
        }
    }

    fun getOrderById(orderId: String, userId: String, userRole: com.eilaji.backend.data.UserRole?): OrderResult? {
        val userUuid = UUID.fromString(userId)
        val orderUuid = UUID.fromString(orderId)
        return transaction {
            val query = Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id)
                .selectAll()
                .where { Orders.id eq orderUuid }

            val order = query.firstOrNull() ?: return@transaction null

            if (userRole == null || userRole == com.eilaji.backend.data.UserRole.PATIENT) {
                if (order[Orders.patientId] != userUuid) return@transaction null
            }

            mapRowToOrder(order)
        }
    }

    fun updateOrderStatus(orderId: String, status: String, paymentStatus: String?, userId: String, userRole: com.eilaji.backend.data.UserRole): OrderResult? {
        val orderUuid = try { UUID.fromString(orderId) } catch (_: Exception) { return null }
        val userUuid = try { UUID.fromString(userId) } catch (_: Exception) { return null }
        val next = normalizeStatus(status) ?: return null
        val pay = paymentStatus?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }?.let {
            if (!PAYMENT_STATUSES.contains(it)) return null else it
        }
        return transaction {
            val existingOrder = Orders.selectAll().where { Orders.id eq orderUuid }.firstOrNull()
                ?: return@transaction null

            if (userRole != com.eilaji.backend.data.UserRole.PHARMACIST && userRole != com.eilaji.backend.data.UserRole.ADMIN) {
                return@transaction null
            }
            // Pharmacists may only transition orders of pharmacies they own
            if (userRole == com.eilaji.backend.data.UserRole.PHARMACIST) {
                val owned = Pharmacies.selectAll().where { Pharmacies.ownerUserId eq userUuid }.map { it[Pharmacies.id] }
                if (!owned.contains(existingOrder[Orders.pharmacyId])) return@transaction null
            }
            val current = existingOrder[Orders.status].uppercase()
            if (TERMINAL.contains(current)) return@transaction null
            val allowed = TRANSITIONS[current] ?: return@transaction null
            if (!allowed.contains(next)) return@transaction null

            Orders.update({ Orders.id eq orderUuid }) {
                it[Orders.status] = next
                if (pay != null) {
                    it[Orders.paymentStatus] = pay
                }
                it[Orders.updatedAt] = Instant.now()
            }

            getOrderById(orderId, userId, userRole)
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
            val existingOrder = Orders.selectAll().where { Orders.id eq orderUuid }.firstOrNull()
                ?: return@transaction null
            if (userRole != com.eilaji.backend.data.UserRole.PHARMACIST && userRole != com.eilaji.backend.data.UserRole.ADMIN) {
                return@transaction null
            }
            if (userRole == com.eilaji.backend.data.UserRole.PHARMACIST) {
                val owned = Pharmacies.selectAll().where { Pharmacies.ownerUserId eq userUuid }.map { it[Pharmacies.id] }
                if (!owned.contains(existingOrder[Orders.pharmacyId])) return@transaction null
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
        val pharmUuid = UUID.fromString(pharmacyId)
        return transaction {
            val query = if (status != null) {
                Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id)
                    .selectAll()
                    .where { (Orders.pharmacyId eq pharmUuid) and (Orders.status eq status) }
            } else {
                Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id)
                    .selectAll()
                    .where { Orders.pharmacyId eq pharmUuid }
            }

            val total = query.count()

            val orders = query
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

    fun getOrdersForUser(userId: String, status: String? = null, page: Int = 0, pageSize: Int = 20): PaginatedResult<OrderResult> {
        val userUuid = UUID.fromString(userId)
        return transaction {
            val query = if (status != null) {
                Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id)
                    .selectAll()
                    .where { (Orders.patientId eq userUuid) and (Orders.status eq status) }
            } else {
                Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id)
                    .selectAll()
                    .where { Orders.patientId eq userUuid }
            }

            val total = query.count()

            val orders = query
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

    fun deleteOrder(orderId: String, userId: String): Boolean {
        val userUuid = UUID.fromString(userId)
        val orderUuid = UUID.fromString(orderId)
        return transaction {
            val order = Orders.selectAll().where { Orders.id eq orderUuid }.firstOrNull()
                ?: return@transaction false

            if (order[Orders.patientId] != userUuid) {
                return@transaction false
            }

            Orders.deleteWhere { Orders.id eq orderUuid }
            true
        }
    }

    private fun mapRowToOrder(row: ResultRow): OrderResult {
        return OrderResult(
            id = row[Orders.id].toString(),
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
            courierLat = try { row.getOrNull(Orders.courierLat) } catch (_: Exception) { null },
            courierLng = try { row.getOrNull(Orders.courierLng) } catch (_: Exception) { null },
            etaMinutes = try { row.getOrNull(Orders.etaMinutes) } catch (_: Exception) { null },
            handoffCode = try { row.getOrNull(Orders.handoffCode) } catch (_: Exception) { null },
            createdAt = row[Orders.createdAt].toString(),
            updatedAt = row[Orders.updatedAt].toString()
        )
    }
}
