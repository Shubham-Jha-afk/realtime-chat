package com.chatapp.model;

import java.time.Instant;
import java.util.UUID;

public record ChatMessage(String id,
                          String roomId,
                          String sender,
                          String content,
                          MessageType type,
                          Instant timestamp) {

    public static ChatMessage chat(String roomId, String sender, String content) {
        return new ChatMessage(UUID.randomUUID().toString(), roomId, sender, content, MessageType.CHAT, Instant.now());
    }

    public static ChatMessage event(MessageType type, String roomId, String sender, String content) {
        return new ChatMessage(UUID.randomUUID().toString(), roomId, sender, content, type, Instant.now());
    }
}
