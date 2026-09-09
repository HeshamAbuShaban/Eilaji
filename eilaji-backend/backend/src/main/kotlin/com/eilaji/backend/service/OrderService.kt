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
        val prescriptionId: String,
        val pharmacyId: String,
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
        val id: String,
        val prescriptionId: String,
        val patientId: String,
        val pharmacyId: String,
        val pharmacyName: String?,
        val status: String,
        val totalAmount: Double,
        val paymentMethod: String?,
        val paymentStatus: String,
        val deliveryAddress: String?,
        val deliveryNotes: String?,
        val createdAt: String,
        val updatedAt: String
    )

    fun createOrder(request: OrderCreateRequest, userId: String): OrderResult? {
        val userUuid = UUID.fromString(userId)
        val prescriptionUuid = UUID.fromString(request.prescriptionId)
        val pharmacyUuid = UUID.fromString(request.pharmacyId)
        return transaction {
            val prescription = Prescriptions.selectAll()
                .where { Prescriptions.id eq prescriptionUuid }
                .firstOrNull()

            if (prescription == null || prescription[Prescriptions.status] != "ACCEPTED") {
                return@transaction null
            }

            val orderId = Orders.insert {
                it[Orders.prescriptionId] = prescriptionUuid
                it[Orders.patientId] = userUuid
                it[Orders.pharmacyId] = pharmacyUuid
                it[Orders.status] = "PENDING"
                it[Orders.totalAmount] = request.totalAmount.toBigDecimal()
                it[Orders.paymentMethod] = request.paymentMethod
                it[Orders.paymentStatus] = "PENDING"
                it[Orders.deliveryAddress] = request.deliveryAddress
                it[Orders.deliveryNotes] = request.deliveryNotes
                it[Orders.createdAt] = Instant.now()
                it[Orders.updatedAt] = Instant.now()
            } get Orders.id

            getOrderById(orderId.toString(), userId, null)
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
        val orderUuid = UUID.fromString(orderId)
        return transaction {
            val existingOrder = Orders.selectAll().where { Orders.id eq orderUuid }.firstOrNull()
                ?: return@transaction null

            if (userRole != com.eilaji.backend.data.UserRole.PHARMACIST && userRole != com.eilaji.backend.data.UserRole.ADMIN) {
                return@transaction null
            }

            Orders.update({ Orders.id eq orderUuid }) {
                it[Orders.status] = status
                if (paymentStatus != null) {
                    it[Orders.paymentStatus] = paymentStatus
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
            prescriptionId = row[Orders.prescriptionId].toString(),
            patientId = row[Orders.patientId].toString(),
            pharmacyId = row[Orders.pharmacyId].toString(),
            pharmacyName = row.getOrNull(Pharmacies.name),
            status = row[Orders.status],
            totalAmount = row[Orders.totalAmount].toDouble(),
            paymentMethod = row[Orders.paymentMethod],
            paymentStatus = row[Orders.paymentStatus],
            deliveryAddress = row[Orders.deliveryAddress],
            deliveryNotes = row[Orders.deliveryNotes],
            createdAt = row[Orders.createdAt].toString(),
            updatedAt = row[Orders.updatedAt].toString()
        )
    }
}
