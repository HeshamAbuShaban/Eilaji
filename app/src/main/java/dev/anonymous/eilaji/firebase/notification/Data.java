package dev.anonymous.eilaji.firebase.notification;

public class Data {
    private String userUid;
    private String fullName;
    private String message;
    private String userImageUrl;
    private String messageImageUrl;

    public Data(String userUid, String fullName, String message, String userImageUrl,
                String messageImageUrl) {
        this.userUid = userUid;
        this.fullName = fullName;
        this.message = message;
        this.userImageUrl = userImageUrl;
        this.messageImageUrl = messageImageUrl;
    }

    public String getUserUid() {
        return userUid;
    }

    public void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserImageUrl() {
        return userImageUrl;
    }

    public void setUserImageUrl(String userImageUrl) {
        this.userImageUrl = userImageUrl;
    }

    public String getMessageImageUrl() {
        return messageImageUrl;
    }

    public void setMessageImageUrl(String messageImageUrl) {
        this.messageImageUrl = messageImageUrl;
    }
}
