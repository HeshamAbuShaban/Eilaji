package com.eilaji.backend.service

import com.eilaji.backend.data.*
import com.eilaji.backend.dto.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

class OrderService {

    @Serializable
    data class OrderCreateRequest(
        val prescriptionId: UUID,
        val pharmacyId: UUID,
        val totalAmount: Double,
        val paymentMethod: String? = null,
        val deliveryAddress: String? = null,
        val deliveryNotes: String? = null
    )

    @Serializable
    data class OrderUpdateStatusRequest(
        val status: String,
        val paymentStatus: String? = null
    )

    @Serializable
    data class OrderResult(
        val id: UUID,
        val prescriptionId: UUID,
        val patientId: String,
        val pharmacyId: UUID,
        val pharmacyName: String?,
        val status: String,
        val totalAmount: Double,
        val paymentMethod: String?,
        val paymentStatus: String,
        val deliveryAddress: String?,
        val deliveryNotes: String?,
        val createdAt: Instant,
        val updatedAt: Instant
    )

    fun createOrder(request: OrderCreateRequest, userId: String): OrderResult? {
        val userUuid = UUID.fromString(userId)
        return transaction {
            val prescription = Prescriptions.selectAll()
                .where { Prescriptions.id eq request.prescriptionId }
                .firstOrNull()

            if (prescription == null || prescription[Prescriptions.status] != "ACCEPTED") {
                return@transaction null
            }

            val orderId = Orders.insert {
                it[Orders.prescriptionId] = request.prescriptionId
                it[Orders.patientId] = userUuid
                it[Orders.pharmacyId] = request.pharmacyId
                it[Orders.status] = "PENDING"
                it[Orders.totalAmount] = request.totalAmount.toBigDecimal()
                it[Orders.paymentMethod] = request.paymentMethod
                it[Orders.paymentStatus] = "PENDING"
                it[Orders.deliveryAddress] = request.deliveryAddress
                it[Orders.deliveryNotes] = request.deliveryNotes
                it[Orders.createdAt] = Instant.now()
                it[Orders.updatedAt] = Instant.now()
            } get Orders.id

            getOrderById(orderId, userId, null)
        }
    }

    fun getUserOrders(userId: String, userRole: com.eilaji.backend.data.UserRole): List<OrderResult> {
        val userUuid = UUID.fromString(userId)
        return transaction {
            val query = if (userRole == com.eilaji.backend.data.UserRole.PHARMACIST || userRole == com.eilaji.backend.data.UserRole.ADMIN) {
                Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id).selectAll()
            } else {
                Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id)
                    .selectAll()
                    .where { Orders.patientId eq userUuid }
            }

            query.orderBy(Orders.createdAt, SortOrder.DESC).map { row ->
                mapRowToOrder(row)
            }
        }
    }

    fun getOrderById(orderId: UUID, userId: String, userRole: com.eilaji.backend.data.UserRole?): OrderResult? {
        val userUuid = UUID.fromString(userId)
        return transaction {
            val query = Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id)
                .selectAll()
                .where { Orders.id eq orderId }

            val order = query.firstOrNull() ?: return@transaction null

            if (userRole == null || userRole == com.eilaji.backend.data.UserRole.PATIENT) {
                if (order[Orders.patientId] != userUuid) return@transaction null
            }

            mapRowToOrder(order)
        }
    }

    fun updateOrderStatus(orderId: UUID, status: String, paymentStatus: String?, userId: String, userRole: com.eilaji.backend.data.UserRole): OrderResult? {
        return transaction {
            val existingOrder = Orders.selectAll().where { Orders.id eq orderId }.firstOrNull()
                ?: return@transaction null

            if (userRole != com.eilaji.backend.data.UserRole.PHARMACIST && userRole != com.eilaji.backend.data.UserRole.ADMIN) {
                return@transaction null
            }

            Orders.update({ Orders.id eq orderId }) {
                it[Orders.status] = status
                if (paymentStatus != null) {
                    it[Orders.paymentStatus] = paymentStatus
                }
                it[Orders.updatedAt] = Instant.now()
            }

            getOrderById(orderId, userId, userRole)
        }
    }

    fun getOrdersForPharmacy(pharmacyId: UUID, status: String? = null, page: Int = 0, pageSize: Int = 20): PaginatedResult<OrderResult> {
        return transaction {
            val query = if (status != null) {
                Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id)
                    .selectAll()
                    .where { (Orders.pharmacyId eq pharmacyId) and (Orders.status eq status) }
            } else {
                Orders.join(Pharmacies, JoinType.LEFT, Orders.pharmacyId, Pharmacies.id)
                    .selectAll()
                    .where { Orders.pharmacyId eq pharmacyId }
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

    fun deleteOrder(orderId: UUID, userId: String): Boolean {
        val userUuid = UUID.fromString(userId)
        return transaction {
            val order = Orders.selectAll().where { Orders.id eq orderId }.firstOrNull()
                ?: return@transaction false

            if (order[Orders.patientId] != userUuid) {
                return@transaction false
            }

            Orders.deleteWhere { Orders.id eq orderId }
            true
        }
    }

    private fun mapRowToOrder(row: ResultRow): OrderResult {
        return OrderResult(
            id = row[Orders.id],
            prescriptionId = row[Orders.prescriptionId],
            patientId = row[Orders.patientId].toString(),
            pharmacyId = row[Orders.pharmacyId],
            pharmacyName = row.getOrNull(Pharmacies.name),
            status = row[Orders.status],
            totalAmount = row[Orders.totalAmount].toDouble(),
            paymentMethod = row[Orders.paymentMethod],
            paymentStatus = row[Orders.paymentStatus],
            deliveryAddress = row[Orders.deliveryAddress],
            deliveryNotes = row[Orders.deliveryNotes],
            createdAt = row[Orders.createdAt],
            updatedAt = row[Orders.updatedAt]
        )
    }
}
