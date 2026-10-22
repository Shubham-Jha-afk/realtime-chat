package com.chatapp.listener;

import com.chatapp.config.Destinations;
import com.chatapp.model.ChatMessage;
import com.chatapp.model.MessageType;
import com.chatapp.model.PresenceEvent;
import com.chatapp.model.Room;
import com.chatapp.model.RoomSnapshot;
import com.chatapp.service.RoomService;
import com.chatapp.service.SessionRegistry;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final SessionRegistry sessions;
    private final RoomService rooms;
    private final SimpMessagingTemplate template;

    public WebSocketEventListener(SessionRegistry sessions, RoomService rooms, SimpMessagingTemplate template) {
        this.sessions = sessions;
        this.rooms = rooms;
        this.template = template;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("Session connected: {} ({})", accessor.getSessionId(),
                sessions.usernameOf(accessor.getSessionId()).orElse("unknown"));
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (!Destinations.isRoomMessageTopic(destination)) {
            return;
        }
        String sessionId = accessor.getSessionId();
        Optional<String> username = sessions.usernameOf(sessionId);
        Optional<String> roomId = Destinations.roomIdOf(destination);
        if (username.isEmpty() || roomId.isEmpty()) {
            return;
        }
        sessions.trackSubscription(sessionId, accessor.getSubscriptionId(), roomId.get());
        enter(sessionId, roomId.get(), username.get());
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Optional<String> username = sessions.usernameOf(sessionId);
        Optional<String> roomId = sessions.untrack(sessionId, accessor.getSubscriptionId());
        if (username.isPresent() && roomId.isPresent()) {
            leave(roomId.get(), username.get());
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        List<String> roomIds = sessions.subscribedRooms(sessionId);
        sessions.remove(sessionId).ifPresent(username -> {
            roomIds.forEach(roomId -> leave(roomId, username));
            rooms.revokeMemberships(username);
            log.info("Session disconnected: {} ({})", sessionId, username);
        });
    }

    private void enter(String sessionId, String roomId, String username) {
        Room room = rooms.get(roomId);
        boolean joined = room.markOnline(username);
        sendSnapshot(sessionId, room);
        if (joined) {
            template.convertAndSend(Destinations.roomTopic(roomId),
                    ChatMessage.event(MessageType.JOIN, roomId, username, username + " joined the room"));
            broadcastPresence(room);
        }
    }

    private void leave(String roomId, String username) {
        Room room = rooms.get(roomId);
        if (room.markOffline(username)) {
            template.convertAndSend(Destinations.roomTopic(roomId),
                    ChatMessage.event(MessageType.LEAVE, roomId, username, username + " left the room"));
            broadcastPresence(room);
        }
    }

    private void sendSnapshot(String sessionId, Room room) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);
        RoomSnapshot snapshot = new RoomSnapshot(room.id(), room.history(), room.onlineUsers());
        template.convertAndSendToUser(sessionId, Destinations.HISTORY_QUEUE, snapshot, headers.getMessageHeaders());
    }

    private void broadcastPresence(Room room) {
        template.convertAndSend(Destinations.usersTopic(room.id()), new PresenceEvent(room.id(), room.onlineUsers()));
    }
}
