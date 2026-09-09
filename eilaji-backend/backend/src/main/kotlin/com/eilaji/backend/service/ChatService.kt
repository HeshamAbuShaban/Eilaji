package com.eilaji.backend.service

import com.eilaji.backend.data.*
import com.eilaji.backend.dto.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class ChatService {

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
                .map { row ->
                    val isPatient = row[Chats.patientUserId] == userUuid
                    ChatDto(
                        id = row[Chats.id].toString(),
                        prescriptionId = row[Chats.prescriptionId]?.toString(),
                        pharmacyId = row[Chats.pharmacyUserId].toString(),
                        pharmacyName = row.getOrNull(Pharmacies.name),
                        userId = row[Chats.patientUserId].toString(),
                        userName = row.getOrNull(Users.fullName),
                        lastMessage = row[Chats.lastMessageText],
                        lastMessageAt = row[Chats.lastMessageAt]?.toString(),
                        unreadCount = if (isPatient) row[Chats.unreadCountPatient] else row[Chats.unreadCountPharmacy],
                        createdAt = row[Chats.createdAt].toString()
                    )
                }

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
                .map { row ->
                    val isPatient = row[Chats.patientUserId] == userUuid
                    ChatDto(
                        id = row[Chats.id].toString(),
                        prescriptionId = row[Chats.prescriptionId]?.toString(),
                        pharmacyId = row[Chats.pharmacyUserId].toString(),
                        pharmacyName = row.getOrNull(Pharmacies.name),
                        userId = row[Chats.patientUserId].toString(),
                        userName = row.getOrNull(Users.fullName),
                        lastMessage = row[Chats.lastMessageText],
                        lastMessageAt = row[Chats.lastMessageAt]?.toString(),
                        unreadCount = if (isPatient) row[Chats.unreadCountPatient] else row[Chats.unreadCountPharmacy],
                        createdAt = row[Chats.createdAt].toString()
                    )
                }.firstOrNull()
        }
    }

    fun createChat(userId: String, prescriptionId: UUID?, pharmacyUserId: UUID?): ChatDto {
        val userUuid = UUID.fromString(userId)
        return transaction {
            val chatId = Chats.insert {
                it[Chats.prescriptionId] = prescriptionId
                it[Chats.patientUserId] = userUuid
                it[Chats.pharmacyUserId] = pharmacyUserId ?: UUID.fromString("00000000-0000-0000-0000-000000000000")
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

            Chats.join(Users, JoinType.LEFT, Chats.pharmacyUserId, Users.id)
                .join(Pharmacies, JoinType.LEFT, Chats.pharmacyUserId, Pharmacies.ownerUserId)
                .selectAll()
                .where { Chats.id eq chatId }
                .map { row ->
                    ChatDto(
                        id = row[Chats.id].toString(),
                        prescriptionId = row[Chats.prescriptionId]?.toString(),
                        pharmacyId = row[Chats.pharmacyUserId].toString(),
                        pharmacyName = row.getOrNull(Pharmacies.name),
                        userId = row[Chats.patientUserId].toString(),
                        userName = row.getOrNull(Users.fullName),
                        lastMessage = row[Chats.lastMessageText],
                        lastMessageAt = row[Chats.lastMessageAt]?.toString(),
                        unreadCount = 0,
                        createdAt = row[Chats.createdAt].toString()
                    )
                }.first()
        }
    }

    fun updateLastMessage(chatId: UUID, message: String, senderId: String, messageType: String = "TEXT", imageUrl: String? = null) {
        val senderUuid = UUID.fromString(senderId)
        transaction {
            val chat = Chats.selectAll().where { Chats.id eq chatId }.firstOrNull()
            val isPatient = chat?.get(Chats.patientUserId) == senderUuid
            val currentPharmacyUnread = chat?.get(Chats.unreadCountPharmacy) ?: 0
            val currentPatientUnread = chat?.get(Chats.unreadCountPatient) ?: 0
            Chats.update({ Chats.id eq chatId }) {
                it[Chats.lastMessageText] = if (messageType == "TEXT") message else null
                it[Chats.lastMessageImageUrl] = if (messageType == "IMAGE") imageUrl else null
                it[Chats.lastMessageSenderId] = senderUuid
                it[Chats.lastMessageAt] = Instant.now()
                it[Chats.updatedAt] = Instant.now()
                if (isPatient) {
                    it[Chats.unreadCountPharmacy] = currentPharmacyUnread + 1
                } else {
                    it[Chats.unreadCountPatient] = currentPatientUnread + 1
                }
            }
        }
    }
}
