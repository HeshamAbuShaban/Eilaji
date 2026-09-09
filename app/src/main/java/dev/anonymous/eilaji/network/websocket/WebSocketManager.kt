package dev.anonymous.eilaji.network.websocket

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dev.anonymous.eilaji.BuildConfig
import dev.anonymous.eilaji.network.MessageDto
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

data class WebSocketMessage(
    @SerializedName("type") val type: String,
    @SerializedName("chatId") val chatId: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("message") val message: MessageDto? = null,
    @SerializedName("isOnline") val isOnline: Boolean? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

class WebSocketManager @JvmOverloads constructor(
    private val token: String,
    private val client: OkHttpClient = OkHttpClient()
) {
    private var webSocket: WebSocket? = null
    private val gson = Gson()

    var onMessage: ((MessageDto) -> Unit)? = null
    var onRead: ((String) -> Unit)? = null
    var onPresence: ((String, Boolean) -> Unit)? = null
    var onPong: (() -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private fun buildUrl(): String {
        val base = if (BuildConfig.DEBUG) "http://10.0.2.2:8080/api/v1/" else "https://api.eilaji.com/api/v1/"
        val wsBase = base.replace("http://", "ws://").replace("https://", "wss://")
        return wsBase + "ws/chat?token=" + token
    }

    fun connect() {
        val request = Request.Builder().url(buildUrl()).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) { onConnected?.invoke() }
            override fun onMessage(ws: WebSocket, text: String) { handleMessage(text) }
            override fun onMessage(ws: WebSocket, bytes: ByteString) { handleMessage(bytes.utf8()) }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) { onError?.invoke(t.message ?: "ws failure") }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) { }
        })
    }

    private fun handleMessage(text: String) {
        try {
            val msg = gson.fromJson(text, WebSocketMessage::class.java) ?: return
            when (msg.type.uppercase()) {
                "MESSAGE" -> msg.message?.let { onMessage?.invoke(it) } ?: msg.content?.let {
                    val dto = MessageDto(id = "", chatId = msg.chatId ?: "", senderId = msg.userId ?: "", senderName = null, content = it, messageType = "TEXT", attachmentUrl = null, isRead = false, readAt = null, createdAt = msg.timestamp ?: "")
                    onMessage?.invoke(dto)
                }
                "MESSAGE_SENT" -> msg.message?.let { onMessage?.invoke(it) }
                "READ" -> msg.chatId?.let { onRead?.invoke(it) }
                "PRESENCE" -> if (msg.userId != null && msg.isOnline != null) onPresence?.invoke(msg.userId, msg.isOnline)
                "PONG" -> onPong?.invoke()
                "CONNECTED", "JOINED", "LEFT" -> {}
                else -> {}
            }
        } catch (_: Exception) {}
    }

    private fun send(wsMessage: WebSocketMessage) { webSocket?.send(gson.toJson(wsMessage)) }

    fun sendJoin(chatId: String) { send(WebSocketMessage(type = "JOIN", chatId = chatId)) }
    fun sendLeave(chatId: String) { send(WebSocketMessage(type = "LEAVE", chatId = chatId)) }
    @JvmOverloads fun sendMessage(chatId: String, content: String, messageType: String = "TEXT", attachmentUrl: String? = null) {
        val inner = if (attachmentUrl != null || messageType != "TEXT") MessageDto(id = "", chatId = chatId, senderId = "", senderName = null, content = content, messageType = messageType, attachmentUrl = attachmentUrl, isRead = false, readAt = null, createdAt = "") else null
        send(WebSocketMessage(type = "MESSAGE", chatId = chatId, content = content, message = inner))
    }
    fun sendRead(chatId: String) { send(WebSocketMessage(type = "READ", chatId = chatId)) }
    fun sendPing() { send(WebSocketMessage(type = "PING")) }
    fun disconnect() { try { webSocket?.close(1000, "bye") } catch (_: Exception) {} ; webSocket = null }
}
