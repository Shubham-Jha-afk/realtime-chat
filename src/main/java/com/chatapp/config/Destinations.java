package com.chatapp.config;

import java.util.Optional;

public final class Destinations {

    public static final String ROOMS_TOPIC = "/topic/rooms";
    public static final String USER_HISTORY = "/user/queue/history";
    public static final String HISTORY_QUEUE = "/queue/history";
    public static final String ROOM_PREFIX = "/topic/room.";
    public static final String USERS_SUFFIX = ".users";
    public static final String SEND_PREFIX = "/app/chat/";

    private Destinations() {
    }

    public static String roomTopic(String roomId) {
        return ROOM_PREFIX + roomId;
    }

    public static String usersTopic(String roomId) {
        return ROOM_PREFIX + roomId + USERS_SUFFIX;
    }

    public static boolean isRoomMessageTopic(String destination) {
        return destination != null
                && destination.startsWith(ROOM_PREFIX)
                && !destination.endsWith(USERS_SUFFIX);
    }

    public static Optional<String> roomIdOf(String destination) {
        if (destination == null || !destination.startsWith(ROOM_PREFIX)) {
            return Optional.empty();
        }
        String rest = destination.substring(ROOM_PREFIX.length());
        if (rest.endsWith(USERS_SUFFIX)) {
            rest = rest.substring(0, rest.length() - USERS_SUFFIX.length());
        }
        return rest.isEmpty() ? Optional.empty() : Optional.of(rest);
    }

    public static Optional<String> sendRoomIdOf(String destination) {
        if (destination == null || !destination.startsWith(SEND_PREFIX)) {
            return Optional.empty();
        }
        String rest = destination.substring(SEND_PREFIX.length());
        return rest.isEmpty() ? Optional.empty() : Optional.of(rest);
    }
}
