package com.chatapp.config;

import com.chatapp.service.RoomService;
import com.chatapp.service.SessionRegistry;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_-]{3,20}$");

    private final SessionRegistry sessions;
    private final RoomService rooms;

    public StompAuthChannelInterceptor(SessionRegistry sessions, RoomService rooms) {
        this.sessions = sessions;
        this.rooms = rooms;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case CONNECT -> onConnect(accessor);
            case SUBSCRIBE -> onSubscribe(accessor);
            case SEND -> onSend(accessor);
            default -> {
            }
        }
        return message;
    }

    private void onConnect(StompHeaderAccessor accessor) {
        String username = accessor.getFirstNativeHeader("username");
        if (username == null || !USERNAME.matcher(username).matches()) {
            throw new MessagingException("Username must be 3-20 characters: letters, digits, _ or -");
        }
        sessions.register(accessor.getSessionId(), username);
        accessor.setUser(new StompPrincipal(username));
    }

    private void onSubscribe(StompHeaderAccessor accessor) {
        String username = requireUser(accessor);
        String destination = accessor.getDestination();
        if (Destinations.ROOMS_TOPIC.equals(destination) || Destinations.USER_HISTORY.equals(destination)) {
            return;
        }
        String roomId = Destinations.roomIdOf(destination)
                .orElseThrow(() -> new MessagingException("Unknown destination: " + destination));
        if (!rooms.canAccess(roomId, username)) {
            throw new MessagingException("Access denied to room " + roomId);
        }
    }

    private void onSend(StompHeaderAccessor accessor) {
        String username = requireUser(accessor);
        String destination = accessor.getDestination();
        String roomId = Destinations.sendRoomIdOf(destination)
                .orElseThrow(() -> new MessagingException("Unknown destination: " + destination));
        if (!rooms.isOnline(roomId, username)) {
            throw new MessagingException("Join room " + roomId + " before sending messages");
        }
    }

    private String requireUser(StompHeaderAccessor accessor) {
        return sessions.usernameOf(accessor.getSessionId())
                .orElseThrow(() -> new MessagingException("Not connected"));
    }
}
