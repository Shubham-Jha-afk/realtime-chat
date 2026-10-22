package com.chatapp.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SessionRegistry {

    private final Map<String, String> userBySession = new ConcurrentHashMap<>();
    private final Set<String> activeNames = ConcurrentHashMap.newKeySet();
    private final Map<String, Map<String, String>> roomBySubscription = new ConcurrentHashMap<>();

    public void register(String sessionId, String username) {
        if (!activeNames.add(username)) {
            throw new IllegalStateException("Username already in use: " + username);
        }
        userBySession.put(sessionId, username);
    }

    public Optional<String> usernameOf(String sessionId) {
        return Optional.ofNullable(userBySession.get(sessionId));
    }

    public boolean isActive(String username) {
        return activeNames.contains(username);
    }

    public void trackSubscription(String sessionId, String subscriptionId, String roomId) {
        roomBySubscription.computeIfAbsent(sessionId, key -> new ConcurrentHashMap<>()).put(subscriptionId, roomId);
    }

    public Optional<String> untrack(String sessionId, String subscriptionId) {
        Map<String, String> subscriptions = roomBySubscription.get(sessionId);
        if (subscriptions == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(subscriptions.remove(subscriptionId));
    }

    public List<String> subscribedRooms(String sessionId) {
        Map<String, String> subscriptions = roomBySubscription.get(sessionId);
        if (subscriptions == null) {
            return List.of();
        }
        return List.copyOf(new HashSet<>(subscriptions.values()));
    }

    public Optional<String> remove(String sessionId) {
        roomBySubscription.remove(sessionId);
        String username = userBySession.remove(sessionId);
        if (username != null) {
            activeNames.remove(username);
        }
        return Optional.ofNullable(username);
    }
}
