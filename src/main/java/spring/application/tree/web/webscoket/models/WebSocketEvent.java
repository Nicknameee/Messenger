package spring.application.tree.web.webscoket.models;

public enum WebSocketEvent {
    SENDING_MESSAGE, UPDATING_MESSAGE, DELETING_MESSAGE,
    UPDATING_CHAT, DELETING_CHAT, NEW_USER_IN_CHAT, USER_LEAVES_CHAT;
}
