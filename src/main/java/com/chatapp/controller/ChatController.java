package com.chatapp.controller;

import com.chatapp.dto.SendMessageRequest;
import com.chatapp.model.ChatMessage;
import com.chatapp.service.RoomService;
import com.chatapp.service.SessionRegistry;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private static final int MAX_LENGTH = 1000;

    private final SessionRegistry sessions;
    private final RoomService rooms;

    public ChatController(SessionRegistry sessions, RoomService rooms) {
        this.sessions = sessions;
        this.rooms = rooms;
    }

    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/room.{roomId}")
    public ChatMessage send(@DestinationVariable String roomId,
                            @Payload SendMessageRequest request,
                            SimpMessageHeaderAccessor headers) {
        String sender = sessions.usernameOf(headers.getSessionId()).orElse(null);
        if (sender == null || request == null || request.content() == null) {
            return null;
        }
        String content = request.content().trim();
        if (content.isEmpty()) {
            return null;
        }
        if (content.length() > MAX_LENGTH) {
            content = content.substring(0, MAX_LENGTH);
        }
        ChatMessage message = ChatMessage.chat(roomId, sender, content);
        rooms.get(roomId).addToHistory(message);
        return message;
    }
}
