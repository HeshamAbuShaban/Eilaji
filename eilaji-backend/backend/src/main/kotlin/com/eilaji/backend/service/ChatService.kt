package com.eilaji.backend.service

import com.eilaji.backend.dto.*
import com.eilaji.backend.model.*
import io.ktor.server.auth.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class ChatService {
    
    fun getChatsForUser(userId: String, page: Int = 0, pageSize: Int = 20): PaginatedResult<ChatDto> {
        return transaction {
            val total = Chats.select { Chats.userId eq userId }.count()
            
            val chats = Chats.leftJoin(Pharmacies)
                .leftJoin(Users)
                .select { Chats.userId eq userId }
                .orderBy(Chats.lastMessageAt.desc())
                .limit(pageSize, (page * pageSize).toLong())
                .map { row ->
                    ChatDto(
                        id = row[Chats.id],
                        prescriptionId = row[Chats.prescriptionId],
                        pharmacyId = row[Chats.pharmacyId],
                        pharmacyName = row[Pharmacies.name],
                        userId = row[Chats.userId],
                        userName = row[Users.fullName],
                        lastMessageAt = row[Chats.lastMessageAt],
                        lastMessage = row[Chats.lastMessage],
                        unreadCount = getUnreadCount(row[Chats.id], userId),
                        createdAt = row[Chats.createdAt]
                    )
                }
            
            PaginatedResult(
                items = chats,
                total = total,
                page = page,
                pageSize = pageSize,
                totalPages = (total + pageSize - 1) / pageSize
            )
        }
    }
    
    private fun getUnreadCount(chatId: Long, userId: String): Int {
        return Messages.select { 
            Messages.chatId eq chatId and (Messages.senderId neq userId) and (Messages.isRead eq false)
        }.count().toInt()
    }
    
    fun createChat(request: CreateChatRequest, currentUserId: String): ChatDto {
        return transaction {
            val chatId = Chats.insertAndGetId {
                it[prescriptionId] = request.prescriptionId
                it[pharmacyId] = request.pharmacyId
                it[userId] = request.userId ?: currentUserId
                it[lastMessageAt] = Instant.now()
            }
            
            Chats.leftJoin(Pharmacies)
                .leftJoin(Users)
                .select { Chats.id eq chatId.value }
                .map { row ->
                    ChatDto(
                        id = row[Chats.id],
                        prescriptionId = row[Chats.prescriptionId],
                        pharmacyId = row[Chats.pharmacyId],
                        pharmacyName = row[Pharmacies.name],
                        userId = row[Chats.userId],
                        userName = row[Users.fullName],
                        lastMessageAt = row[Chats.lastMessageAt],
                        lastMessage = row[Chats.lastMessage],
                        createdAt = row[Chats.createdAt]
                    )
                }.first()
        }
    }
    
    fun getChatById(chatId: Long, userId: String): ChatDto? {
        return transaction {
            Chats.leftJoin(Pharmacies)
                .leftJoin(Users)
                .select { Chats.id eq chatId }
                .map { row ->
                    ChatDto(
                        id = row[Chats.id],
                        prescriptionId = row[Chats.prescriptionId],
                        pharmacyId = row[Chats.pharmacyId],
                        pharmacyName = row[Pharmacies.name],
                        userId = row[Chats.userId],
                        userName = row[Users.fullName],
                        lastMessageAt = row[Chats.lastMessageAt],
                        lastMessage = row[Chats.lastMessage],
                        unreadCount = getUnreadCount(chatId, userId),
                        createdAt = row[Chats.createdAt]
                    )
                }.firstOrNull()
        }
    }
    
    fun updateLastMessage(chatId: Long, message: String) {
        transaction {
            Chats.update({ Chats.id eq chatId }) {
                it[lastMessage] = message
                it[lastMessageAt] = Instant.now()
                it[updatedAt] = Instant.now()
            }
        }
    }
}
