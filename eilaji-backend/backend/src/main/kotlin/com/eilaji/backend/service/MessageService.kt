package com.eilaji.backend.service

import com.eilaji.backend.dto.*
import com.eilaji.backend.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class MessageService {
    
    fun getMessagesForChat(chatId: Long, page: Int = 0, pageSize: Int = 50): PaginatedResult<MessageDto> {
        return transaction {
            val total = Messages.select { Messages.chatId eq chatId }.count()
            
            val messages = Messages.join(Users, JoinType.LEFT, Messages.senderId, Users.id)
                .select { Messages.chatId eq chatId }
                .orderBy(Messages.createdAt.asc())
                .limit(pageSize, (page * pageSize).toLong())
                .map { row ->
                    MessageDto(
                        id = row[Messages.id],
                        chatId = row[Messages.chatId],
                        senderId = row[Messages.senderId],
                        senderName = row[Users.fullName],
                        content = row[Messages.content],
                        messageType = row[Messages.messageType],
                        attachmentUrl = row[Messages.attachmentUrl],
                        isRead = row[Messages.isRead],
                        readAt = row[Messages.readAt],
                        createdAt = row[Messages.createdAt]
                    )
                }
            
            PaginatedResult(
                items = messages,
                total = total,
                page = page,
                pageSize = pageSize,
                totalPages = (total + pageSize - 1) / pageSize
            )
        }
    }
    
    fun createMessage(chatId: Long, senderId: String, request: SendMessageRequest): MessageDto {
        return transaction {
            val messageId = Messages.insertAndGetId {
                it[chatId] = chatId
                it[senderId] = senderId
                it[content] = request.content
                it[messageType] = request.messageType
                it[attachmentUrl] = request.attachmentUrl
            }
            
            Messages.join(Users, JoinType.LEFT, Messages.senderId, Users.id)
                .select { Messages.id eq messageId.value }
                .map { row ->
                    MessageDto(
                        id = row[Messages.id],
                        chatId = row[Messages.chatId],
                        senderId = row[Messages.senderId],
                        senderName = row[Users.fullName],
                        content = row[Messages.content],
                        messageType = row[Messages.messageType],
                        attachmentUrl = row[Messages.attachmentUrl],
                        isRead = row[Messages.isRead],
                        readAt = row[Messages.readAt],
                        createdAt = row[Messages.createdAt]
                    )
                }.first()
        }
    }
    
    fun markMessagesAsRead(messageIds: List<Long>, userId: String): List<MessageDto> {
        return transaction {
            Messages.update({ 
                Messages.id inList messageIds and (Messages.senderId neq userId)
            }) {
                it[isRead] = true
                it[readAt] = Instant.now()
            }
            
            Messages.join(Users, JoinType.LEFT, Messages.senderId, Users.id)
                .select { Messages.id inList messageIds }
                .map { row ->
                    MessageDto(
                        id = row[Messages.id],
                        chatId = row[Messages.chatId],
                        senderId = row[Messages.senderId],
                        senderName = row[Users.fullName],
                        content = row[Messages.content],
                        messageType = row[Messages.messageType],
                        attachmentUrl = row[Messages.attachmentUrl],
                        isRead = row[Messages.isRead],
                        readAt = row[Messages.readAt],
                        createdAt = row[Messages.createdAt]
                    )
                }
        }
    }
    
    fun markChatAsRead(chatId: Long, userId: String): Int {
        return transaction {
            Messages.update({ 
                Messages.chatId eq chatId and (Messages.senderId neq userId) and (Messages.isRead eq false)
            }) {
                it[isRead] = true
                it[readAt] = Instant.now()
            }
        }
    }
    
    fun getMessageById(messageId: Long): MessageDto? {
        return transaction {
            Messages.join(Users, JoinType.LEFT, Messages.senderId, Users.id)
                .select { Messages.id eq messageId }
                .map { row ->
                    MessageDto(
                        id = row[Messages.id],
                        chatId = row[Messages.chatId],
                        senderId = row[Messages.senderId],
                        senderName = row[Users.fullName],
                        content = row[Messages.content],
                        messageType = row[Messages.messageType],
                        attachmentUrl = row[Messages.attachmentUrl],
                        isRead = row[Messages.isRead],
                        readAt = row[Messages.readAt],
                        createdAt = row[Messages.createdAt]
                    )
                }.firstOrNull()
        }
    }
}
