package com.eilaji.backend.service

import com.eilaji.backend.data.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class OrderService {
    
    data class OrderCreateRequest(
        val prescriptionId: Int,
        val totalAmount: Double,
        val paymentMethod: String? = null,
        val deliveryAddress: String? = null,
        val deliveryNotes: String? = null
    )
    
    data class OrderUpdateStatusRequest(
        val status: OrderStatus,
        val paymentStatus: PaymentStatus? = null
    )
    
    data class OrderDto(
        val id: Int,
        val prescriptionId: Int,
        val patientId: String,
        val pharmacyId: Int,
        val status: OrderStatus,
        val totalAmount: Double,
        val paymentMethod: String?,
        val paymentStatus: PaymentStatus,
        val deliveryAddress: String?,
        val deliveryNotes: String?,
        val createdAt: Instant,
        val updatedAt: Instant
    )
    
    fun createOrder(request: OrderCreateRequest, userId: String): OrderDto? {
        return transaction {
            // Verify prescription exists and belongs to user
            val prescription = Prescriptions.select { Prescriptions.id eq request.prescriptionId }
                .singleOrNull() ?: return@transaction null
            
            if (prescription[Prescriptions.userId] != userId) {
                return@transaction null
            }
            
            // Check if prescription is accepted
            if (prescription[Prescriptions.status] != "ACCEPTED") {
                return@transaction null
            }
            
            val pharmacyId = prescription[Prescriptions.pharmacyId] ?: return@transaction null
            
            // Create order
            val orderId = Orders.insert {
                it[prescriptionId] = request.prescriptionId
                it[patientId] = userId
                it[pharmacyId] = pharmacyId
                it[status] = OrderStatus.PENDING
                it[totalAmount] = request.totalAmount.toBigDecimal()
                it[paymentMethod] = request.paymentMethod
                it[paymentStatus] = PaymentStatus.PENDING
                it[deliveryAddress] = request.deliveryAddress
                it[deliveryNotes] = request.deliveryNotes
                it[createdAt] = Instant.now()
                it[updatedAt] = Instant.now()
            } get Orders.id
            
            Orders.select { Orders.id eq orderId }.map { row ->
                OrderDto(
                    id = row[Orders.id],
                    prescriptionId = row[Orders.prescriptionId],
                    patientId = row[Orders.patientId],
                    pharmacyId = row[Orders.pharmacyId],
                    status = row[Orders.status],
                    totalAmount = row[Orders.totalAmount].toDouble(),
                    paymentMethod = row[Orders.paymentMethod],
                    paymentStatus = row[Orders.paymentStatus],
                    deliveryAddress = row[Orders.deliveryAddress],
                    deliveryNotes = row[Orders.deliveryNotes],
                    createdAt = row[Orders.createdAt],
                    updatedAt = row[Orders.updatedAt]
                )
            }.firstOrNull()
        }
    }
    
    fun getUserOrders(userId: String, role: UserRole): List<OrderDto> {
        return transaction {
            val query = if (role == UserRole.ADMIN) {
                Orders.selectAll()
            } else if (role == UserRole.PHARMACIST) {
                Orders.join(Pharmacies, JoinType.INNER, Orders.pharmacyId, Pharmacies.id)
                    .select { Pharmacies.ownerId eq userId }
            } else {
                Orders.select { Orders.patientId eq userId }
            }
            
            query.orderBy(Orders.createdAt.desc()).map { row ->
                OrderDto(
                    id = row[Orders.id],
                    prescriptionId = row[Orders.prescriptionId],
                    patientId = row[Orders.patientId],
                    pharmacyId = row[Orders.pharmacyId],
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
    }
    
    fun getOrderById(orderId: Int, userId: String, role: UserRole): OrderDto? {
        return transaction {
            val order = Orders.select { Orders.id eq orderId }.singleOrNull() ?: return@transaction null
            
            // Check access rights
            val canAccess = when (role) {
                UserRole.ADMIN -> true
                UserRole.PHARMACIST -> {
                    val pharmacy = Pharmacies.select { Pharmacies.id eq order[Orders.pharmacyId] }.singleOrNull()
                    pharmacy?.get(Pharmacies.ownerId) == userId
                }
                else -> order[Orders.patientId] == userId
            }
            
            if (!canAccess) return@transaction null
            
            OrderDto(
                id = order[Orders.id],
                prescriptionId = order[Orders.prescriptionId],
                patientId = order[Orders.patientId],
                pharmacyId = order[Orders.pharmacyId],
                status = order[Orders.status],
                totalAmount = order[Orders.totalAmount].toDouble(),
                paymentMethod = order[Orders.paymentMethod],
                paymentStatus = order[Orders.paymentStatus],
                deliveryAddress = order[Orders.deliveryAddress],
                deliveryNotes = order[Orders.deliveryNotes],
                createdAt = order[Orders.createdAt],
                updatedAt = order[Orders.updatedAt]
            )
        }
    }
    
    fun updateOrderStatus(orderId: Int, status: OrderStatus, paymentStatus: PaymentStatus?, userId: String, role: UserRole): OrderDto? {
        return transaction {
            val order = Orders.select { Orders.id eq orderId }.singleOrNull() ?: return@transaction null
            
            // Check if user can update this order
            val canUpdate = when (role) {
                UserRole.ADMIN -> true
                UserRole.PHARMACIST -> {
                    val pharmacy = Pharmacies.select { Pharmacies.id eq order[Orders.pharmacyId] }.singleOrNull()
                    pharmacy?.get(Pharmacies.ownerId) == userId
                }
                else -> false
            }
            
            if (!canUpdate) return@transaction null
            
            Orders.update({ Orders.id eq orderId }) {
                it[Orders.status] = status
                if (paymentStatus != null) {
                    it[Orders.paymentStatus] = paymentStatus
                }
                it[Orders.updatedAt] = Instant.now()
            }
            
            Orders.select { Orders.id eq orderId }.map { row ->
                OrderDto(
                    id = row[Orders.id],
                    prescriptionId = row[Orders.prescriptionId],
                    patientId = row[Orders.patientId],
                    pharmacyId = row[Orders.pharmacyId],
                    status = row[Orders.status],
                    totalAmount = row[Orders.totalAmount].toDouble(),
                    paymentMethod = row[Orders.paymentMethod],
                    paymentStatus = row[Orders.paymentStatus],
                    deliveryAddress = row[Orders.deliveryAddress],
                    deliveryNotes = row[Orders.deliveryNotes],
                    createdAt = row[Orders.createdAt],
                    updatedAt = row[Orders.updatedAt]
                )
            }.firstOrNull()
        }
    }
    
    fun createOrderFromAcceptedPrescription(prescriptionId: Int, quotedPrice: Double): OrderDto? {
        return transaction {
            val prescription = Prescriptions.select { Prescriptions.id eq prescriptionId }.singleOrNull() ?: return@transaction null
            
            if (prescription[Prescriptions.status] != "ACCEPTED") {
                return@transaction null
            }
            
            val pharmacyId = prescription[Prescriptions.pharmacyId] ?: return@transaction null
            val patientId = prescription[Prescriptions.userId]
            
            // Check if order already exists
            val existingOrder = Orders.select { 
                Orders.prescriptionId eq prescriptionId 
            }.singleOrNull()
            
            if (existingOrder != null) {
                return@transaction null
            }
            
            val orderId = Orders.insert {
                it[Orders.prescriptionId] = prescriptionId
                it[Orders.patientId] = patientId
                it[Orders.pharmacyId] = pharmacyId
                it[Orders.status] = OrderStatus.PENDING
                it[Orders.totalAmount] = quotedPrice.toBigDecimal()
                it[Orders.paymentStatus] = PaymentStatus.PENDING
                it[Orders.createdAt] = Instant.now()
                it[Orders.updatedAt] = Instant.now()
            } get Orders.id
            
            Orders.select { Orders.id eq orderId }.map { row ->
                OrderDto(
                    id = row[Orders.id],
                    prescriptionId = row[Orders.prescriptionId],
                    patientId = row[Orders.patientId],
                    pharmacyId = row[Orders.pharmacyId],
                    status = row[Orders.status],
                    totalAmount = row[Orders.totalAmount].toDouble(),
                    paymentMethod = row[Orders.paymentMethod],
                    paymentStatus = row[Orders.paymentStatus],
                    deliveryAddress = row[Orders.deliveryAddress],
                    deliveryNotes = row[Orders.deliveryNotes],
                    createdAt = row[Orders.createdAt],
                    updatedAt = row[Orders.updatedAt]
                )
            }.firstOrNull()
        }
    }
}
