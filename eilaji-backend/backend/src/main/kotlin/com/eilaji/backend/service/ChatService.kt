package com.eilaji.backend.service

import com.eilaji.backend.data.*
import com.eilaji.backend.dto.*
import com.eilaji.backend.security.ApiException
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class ChatService {

    /** A user may only act on a chat they take part in. */
    fun isParticipant(chatId: UUID, userId: String): Boolean {
        val userUuid = try { UUID.fromString(userId) } catch (_: Exception) { return false }
        return transaction {
            Chats.selectAll()
                .where { (Chats.id eq chatId) and ((Chats.patientUserId eq userUuid) or (Chats.pharmacyUserId eq userUuid)) }
                .any()
        }
    }

    /** Throws 404 for a chat the caller does not take part in. */
    fun ensureParticipant(chatId: UUID, userId: String) {
        if (!isParticipant(chatId, userId)) {
            throw ApiException(HttpStatusCode.NotFound, "Chat not found")
        }
    }

    fun getChatsForUser(userId: String, page: Int = 0, pageSize: Int = 20): PaginatedResult<ChatDto> {
        val userUuid = UUID.fromString(userId)
        return transaction {
            val query = Chats.join(Users, JoinType.LEFT, Chats.pharmacyUserId, Users.id)
                .join(Pharmacies, JoinType.LEFT, Chats.pharmacyUserId, Pharmacies.ownerUserId)
                .selectAll()
                .where { (Chats.patientUserId eq userUuid) or (Chats.pharmacyUserId eq userUuid) }

            val total = query.count()

            val chats = query
                .orderBy(Chats.lastMessageAt, SortOrder.DESC_NULLS_LAST)
                .limit(pageSize, (page * pageSize).toLong())
                .map { row -> toDto(row, userUuid) }

            PaginatedResult(
                items = chats,
                total = total,
                page = page,
                pageSize = pageSize,
                totalPages = if (total > 0) ((total + pageSize - 1) / pageSize).toInt() else 0
            )
        }
    }

    fun getChatById(chatId: UUID, userId: String): ChatDto? {
        val userUuid = UUID.fromString(userId)
        return transaction {
            Chats.join(Users, JoinType.LEFT, Chats.pharmacyUserId, Users.id)
                .join(Pharmacies, JoinType.LEFT, Chats.pharmacyUserId, Pharmacies.ownerUserId)
                .selectAll()
                .where { (Chats.id eq chatId) and ((Chats.patientUserId eq userUuid) or (Chats.pharmacyUserId eq userUuid)) }
                .map { row -> toDto(row, userUuid) }
                .firstOrNull()
        }
    }

    /**
     * Opens a thread with a pharmacy. The caller always becomes the patient side.
     * Returns null when the pharmacy (or pharmacist account) does not exist.
     */
    fun createChat(userId: String, prescriptionId: UUID?, pharmacyUserId: UUID?): ChatDto? {
        val userUuid = UUID.fromString(userId)
        return transaction {
            val resolvedPharmacyUserId = when {
                pharmacyUserId == null -> null // general/support thread without a pharmacy
                Users.selectAll().where { Users.id eq pharmacyUserId }.singleOrNull() != null -> pharmacyUserId
                else -> Pharmacies.selectAll().where { Pharmacies.id eq pharmacyUserId }.singleOrNull()
                    ?.let { it[Pharmacies.ownerUserId] }
                    ?: return@transaction null
            }

            // Reuse the existing thread between the same participants instead of duplicating.
            val existing = Chats.selectAll().where {
                (Chats.patientUserId eq userUuid) and
                    (if (resolvedPharmacyUserId != null) Chats.pharmacyUserId eq resolvedPharmacyUserId else Chats.pharmacyUserId.isNull()) and
                    (if (prescriptionId != null) (Chats.prescriptionId eq prescriptionId) else Chats.prescriptionId.isNull())
            }.singleOrNull()
            if (existing != null) return@transaction getChatDto(existing[Chats.id])

            val chatId = Chats.insert {
                it[Chats.prescriptionId] = prescriptionId
                it[Chats.patientUserId] = userUuid
                it[Chats.pharmacyUserId] = resolvedPharmacyUserId
                it[Chats.lastMessageText] = null
                it[Chats.lastMessageImageUrl] = null
                it[Chats.lastMessageSenderId] = null
                it[Chats.lastMessageAt] = Instant.now()
                it[Chats.unreadCountPatient] = 0
                it[Chats.unreadCountPharmacy] = 0
                it[Chats.isArchived] = false
                it[Chats.createdAt] = Instant.now()
                it[Chats.updatedAt] = Instant.now()
            } get Chats.id

            getChatDto(chatId)
        }
    }

    private fun getChatDto(chatId: UUID): ChatDto? = transaction {
        Chats.join(Users, JoinType.LEFT, Chats.pharmacyUserId, Users.id)
            .join(Pharmacies, JoinType.LEFT, Chats.pharmacyUserId, Pharmacies.ownerUserId)
            .selectAll()
            .where { Chats.id eq chatId }
            .map { row -> toDto(row, row[Chats.patientUserId]) }
            .firstOrNull()
    }

    fun updateLastMessage(chatId: UUID, message: String, senderId: String, messageType: String = "TEXT", imageUrl: String? = null) {
        val senderUuid = UUID.fromString(senderId)
        transaction {
            val chat = Chats.selectAll().where { Chats.id eq chatId }.firstOrNull() ?: return@transaction
            val isPatient = chat[Chats.patientUserId] == senderUuid
            val currentPharmacyUnread = chat[Chats.unreadCountPharmacy]
            val currentPatientUnread = chat[Chats.unreadCountPatient]
            Chats.update({ Chats.id eq chatId }) {
                it[Chats.lastMessageText] = if (messageType == "TEXT") message else null
                it[Chats.lastMessageImageUrl] = if (messageType == "IMAGE") imageUrl else null
                it[Chats.lastMessageSenderId] = senderUuid
                it[Chats.lastMessageAt] = Instant.now()
                it[Chats.updatedAt] = Instant.now()
                if (isPatient) it[Chats.unreadCountPharmacy] = currentPharmacyUnread + 1
                else it[Chats.unreadCountPatient] = currentPatientUnread + 1
            }
        }
    }

    private fun toDto(row: ResultRow, viewerUuid: UUID): ChatDto {
        val isPatient = row[Chats.patientUserId] == viewerUuid
        return ChatDto(
            id = row[Chats.id].toString(),
            prescriptionId = row[Chats.prescriptionId]?.toString(),
            pharmacyId = row[Chats.pharmacyUserId]?.toString().orEmpty(),
            pharmacyName = row.getOrNull(Pharmacies.name),
            userId = row[Chats.patientUserId].toString(),
            userName = row.getOrNull(Users.fullName),
            lastMessage = row[Chats.lastMessageText],
            lastMessageAt = row[Chats.lastMessageAt]?.toString(),
            unreadCount = if (isPatient) row[Chats.unreadCountPatient] else row[Chats.unreadCountPharmacy],
            createdAt = row[Chats.createdAt].toString()
        )
    }
}
