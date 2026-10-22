package com.chatapp.model;

import java.util.List;

public record RoomSnapshot(String roomId, List<ChatMessage> messages, List<String> users) {
}
