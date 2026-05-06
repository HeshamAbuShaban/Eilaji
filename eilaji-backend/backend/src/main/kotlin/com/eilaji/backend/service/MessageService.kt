package com.eilaji.backend.service

import com.eilaji.backend.data.*
import com.eilaji.backend.dto.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class MessageService {

    fun getMessages(chatId: Long, page: Int = 0, pageSize: Int = 50): PaginatedResult<MessageDto> {
        return transaction {
            val query = Messages.join(Users, JoinType.LEFT, Messages.senderId, Users.id)
                .selectAll()
                .where { Messages.chatId eq chatId }

            val total = query.count()

            val messages = query
                .orderBy(Messages.createdAt)
                .limit(pageSize, (page * pageSize).toLong())
                .map { row ->
                    MessageDto(
                        id = row[Messages.id],
                        chatId = row[Messages.chatId],
                        senderId = row[Messages.senderId],
                        senderName = row.getOrNull(Users.fullName),
                        content = row[Messages.content],
                        messageType = row[Messages.messageType],
                        attachmentUrl = row[Messages.attachmentUrl],
                        isRead = row[Messages.isRead],
                        readAt = row[Messages.readAt]?.toString(),
                        createdAt = row[Messages.createdAt].toString()
                    )
                }

            PaginatedResult(
                items = messages,
                total = total,
                page = page,
                pageSize = pageSize,
                totalPages = if (total > 0) ((total + pageSize - 1) / pageSize).toInt() else 0
            )
        }
    }

    fun getMessagesForChat(chatId: Long, userId: String, page: Int = 0, pageSize: Int = 50): PaginatedResult<MessageDto> {
        return getMessages(chatId, page, pageSize)
    }

    fun sendMessage(chatId: Long, senderId: String, content: String,
                   messageType: String = "TEXT", attachmentUrl: String? = null): MessageDto {
        return transaction {
            val messageId = Messages.insert {
                it[Messages.chatId] = chatId
                it[Messages.senderId] = senderId
                it[Messages.content] = content
                it[Messages.messageType] = messageType
                it[Messages.attachmentUrl] = attachmentUrl
                it[Messages.isRead] = false
                it[Messages.createdAt] = Instant.now()
            } get Messages.id

            Chats.update({ Chats.id eq chatId }) {
                it[Chats.lastMessage] = content
                it[Chats.lastMessageAt] = Instant.now()
                it[Chats.updatedAt] = Instant.now()
            }

            Messages.join(Users, JoinType.LEFT, Messages.senderId, Users.id)
                .selectAll()
                .where { Messages.id eq messageId }
                .map { row ->
                    MessageDto(
                        id = row[Messages.id],
                        chatId = row[Messages.chatId],
                        senderId = row[Messages.senderId],
                        senderName = row.getOrNull(Users.fullName),
                        content = row[Messages.content],
                        messageType = row[Messages.messageType],
                        attachmentUrl = row[Messages.attachmentUrl],
                        isRead = row[Messages.isRead],
                        readAt = row[Messages.readAt]?.toString(),
                        createdAt = row[Messages.createdAt].toString()
                    )
                }.first()
        }
    }

    fun markAsRead(messageId: Long): Boolean {
        return transaction {
            Messages.update({ Messages.id eq messageId }) {
                it[Messages.isRead] = true
                it[Messages.readAt] = Instant.now()
            }
            true
        }
    }

    fun markChatAsRead(chatId: Long, userId: String): Int {
        return transaction {
            val result = Messages.update({ (Messages.chatId eq chatId) and (Messages.senderId neq userId) and (Messages.isRead eq false) }) {
                it[Messages.isRead] = true
                it[Messages.readAt] = Instant.now()
            }
            result
        }
    }

    fun getUnreadCount(chatId: Long, userId: String): Int {
        return transaction {
            Messages.selectAll()
                .where { (Messages.chatId eq chatId) and (Messages.isRead eq false) and (Messages.senderId neq userId) }
                .count().toInt()
        }
    }
}
