package dev.anonymous.eilaji.models

class MessageModel {
    var senderUid: String? = null
    var receiverUid: String? = null
    var messageText: String? = null
    var messageImageUrl: String? = null
    var medicineName: String? = null
    var timestamp: Long? = null

    constructor() {}
    constructor(
        senderUid: String?, receiverUid: String?, messageText: String?,
        messageImageUrl: String?, medicineName: String?, timestamp: Long?
    ) {
        this.senderUid = senderUid
        this.receiverUid = receiverUid
        this.messageText = messageText
        this.messageImageUrl = messageImageUrl
        this.medicineName = medicineName
        this.timestamp = timestamp
    }
}