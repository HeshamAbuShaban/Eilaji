package com.eilaji.backend.service

import com.eilaji.backend.data.*
import com.eilaji.backend.dto.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class ChatService {

    fun getChatsForUser(userId: String, page: Int = 0, pageSize: Int = 20): PaginatedResult<ChatDto> {
        return transaction {
            val query = Chats.join(Pharmacies, JoinType.LEFT, Chats.pharmacyId, Pharmacies.id)
                .selectAll()
                .where { Chats.userId eq userId }

            val total = query.count()

            val chats = query
                .orderBy(Chats.lastMessageAt, SortOrder.DESC_NULLS_LAST)
                .limit(pageSize, (page * pageSize).toLong())
                .map { row ->
                    ChatDto(
                        id = row[Chats.id],
                        prescriptionId = row[Chats.prescriptionId],
                        pharmacyId = row[Chats.pharmacyId],
                        pharmacyName = row.getOrNull(Pharmacies.name),
                        userId = row[Chats.userId],
                        lastMessage = row[Chats.lastMessage],
                        lastMessageAt = row[Chats.lastMessageAt]?.toString(),
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

    fun getChatById(chatId: Long, userId: String): ChatDto? {
        return transaction {
            Chats.join(Pharmacies, JoinType.LEFT, Chats.pharmacyId, Pharmacies.id)
                .selectAll()
                .where { (Chats.id eq chatId) and (Chats.userId eq userId) }
                .map { row ->
                    ChatDto(
                        id = row[Chats.id],
                        prescriptionId = row[Chats.prescriptionId],
                        pharmacyId = row[Chats.pharmacyId],
                        pharmacyName = row.getOrNull(Pharmacies.name),
                        userId = row[Chats.userId],
                        lastMessage = row[Chats.lastMessage],
                        lastMessageAt = row[Chats.lastMessageAt]?.toString(),
                        createdAt = row[Chats.createdAt].toString()
                    )
                }.firstOrNull()
        }
    }

    fun createChat(userId: String, prescriptionId: Int?, pharmacyId: Int?): ChatDto {
        return transaction {
            val chatId = Chats.insert {
                it[Chats.prescriptionId] = prescriptionId
                it[Chats.pharmacyId] = pharmacyId
                it[Chats.userId] = userId
                it[Chats.lastMessage] = null
                it[Chats.lastMessageAt] = Instant.now()
                it[Chats.createdAt] = Instant.now()
                it[Chats.updatedAt] = Instant.now()
            } get Chats.id

            Chats.join(Pharmacies, JoinType.LEFT, Chats.pharmacyId, Pharmacies.id)
                .selectAll()
                .where { Chats.id eq chatId }
                .map { row ->
                    ChatDto(
                        id = row[Chats.id],
                        prescriptionId = row[Chats.prescriptionId],
                        pharmacyId = row[Chats.pharmacyId],
                        pharmacyName = row.getOrNull(Pharmacies.name),
                        userId = row[Chats.userId],
                        lastMessage = row[Chats.lastMessage],
                        lastMessageAt = row[Chats.lastMessageAt]?.toString(),
                        createdAt = row[Chats.createdAt].toString()
                    )
                }.first()
        }
    }
}
