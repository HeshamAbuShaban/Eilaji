package com.eilaji.backend.service

import com.eilaji.backend.data.*
import com.eilaji.backend.dto.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class MessageService {

    fun getMessages(chatId: UUID, page: Int = 0, pageSize: Int = 50): PaginatedResult<MessageDto> {
        return transaction {
            val query = Messages.join(Users, JoinType.LEFT, Messages.senderUserId, Users.id)
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
                        senderId = row[Messages.senderUserId].toString(),
                        senderName = row.getOrNull(Users.fullName),
                        content = row[Messages.messageText],
                        messageType = row[Messages.messageType],
                        attachmentUrl = row[Messages.messageImageUrl],
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

    fun getMessagesForChat(chatId: UUID, userId: String, page: Int = 0, pageSize: Int = 50): PaginatedResult<MessageDto> {
        return getMessages(chatId, page, pageSize)
    }

    fun sendMessage(chatId: UUID, senderId: String, content: String,
                   messageType: String = "TEXT", attachmentUrl: String? = null): MessageDto {
        val senderUuid = UUID.fromString(senderId)
        return transaction {
            val messageId = Messages.insert {
                it[Messages.chatId] = chatId
                it[Messages.senderUserId] = senderUuid
                it[Messages.messageText] = if (messageType == "TEXT") content else null
                it[Messages.messageImageUrl] = if (messageType == "IMAGE") attachmentUrl else null
                it[Messages.medicineName] = null
                it[Messages.messageType] = messageType
                it[Messages.isRead] = false
                it[Messages.createdAt] = Instant.now()
            } get Messages.id

            val chat = Chats.selectAll().where { Chats.id eq chatId }.firstOrNull()
            if (chat != null) {
                val isPatient = chat[Chats.patientUserId] == senderUuid
                
                Chats.update({ Chats.id eq chatId }) {
                    it[Chats.lastMessageText] = if (messageType == "TEXT") content else null
                    it[Chats.lastMessageImageUrl] = if (messageType == "IMAGE") attachmentUrl else null
                    it[Chats.lastMessageSenderId] = senderUuid
                    it[Chats.lastMessageAt] = Instant.now()
                    it[Chats.updatedAt] = Instant.now()
                    if (isPatient) {
                        it[Chats.unreadCountPharmacy] = Chats.unreadCountPharmacy + 1
                    } else {
                        it[Chats.unreadCountPatient] = Chats.unreadCountPatient + 1
                    }
                }
            }

            Messages.join(Users, JoinType.LEFT, Messages.senderUserId, Users.id)
                .selectAll()
                .where { Messages.id eq messageId }
                .map { row ->
                    MessageDto(
                        id = row[Messages.id],
                        chatId = row[Messages.chatId],
                        senderId = row[Messages.senderUserId].toString(),
                        senderName = row.getOrNull(Users.fullName),
                        content = row[Messages.messageText],
                        messageType = row[Messages.messageType],
                        attachmentUrl = row[Messages.messageImageUrl],
                        isRead = row[Messages.isRead],
                        readAt = row[Messages.readAt]?.toString(),
                        createdAt = row[Messages.createdAt].toString()
                    )
                }.first()
        }
    }

    fun markAsRead(messageId: UUID): Boolean {
        return transaction {
            Messages.update({ Messages.id eq messageId }) {
                it[Messages.isRead] = true
                it[Messages.readAt] = Instant.now()
            }
            true
        }
    }

    fun markChatAsRead(chatId: UUID, userId: String): Int {
        val userUuid = UUID.fromString(userId)
        return transaction {
            val result = Messages.update({ (Messages.chatId eq chatId) and (Messages.senderUserId neq userUuid) and (Messages.isRead eq false) }) {
                it[Messages.isRead] = true
                it[Messages.readAt] = Instant.now()
            }
            result
        }
    }

    fun getUnreadCount(chatId: UUID, userId: String): Int {
        val userUuid = UUID.fromString(userId)
        return transaction {
            Messages.selectAll()
                .where { (Messages.chatId eq chatId) and (Messages.isRead eq false) and (Messages.senderUserId neq userUuid) }
                .count().toInt()
        }
    }
}
