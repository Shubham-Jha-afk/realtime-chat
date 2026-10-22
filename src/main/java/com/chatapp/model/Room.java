package com.chatapp.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Room {

    private static final int HISTORY_LIMIT = 50;

    private final String id;
    private final String name;
    private final RoomType type;
    private final String passwordHash;
    private final String owner;
    private final Set<String> members = ConcurrentHashMap.newKeySet();
    private final Set<String> online = ConcurrentHashMap.newKeySet();
    private final Deque<ChatMessage> history = new ArrayDeque<>();

    public Room(String id, String name, RoomType type, String passwordHash, String owner) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.passwordHash = passwordHash;
        this.owner = owner;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public RoomType type() {
        return type;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String owner() {
        return owner;
    }

    public void addMember(String username) {
        members.add(username);
    }

    public void removeMember(String username) {
        members.remove(username);
    }

    public boolean isMember(String username) {
        return members.contains(username);
    }

    public boolean markOnline(String username) {
        return online.add(username);
    }

    public boolean markOffline(String username) {
        return online.remove(username);
    }

    public boolean isOnline(String username) {
        return online.contains(username);
    }

    public int onlineCount() {
        return online.size();
    }

    public List<String> onlineUsers() {
        return online.stream().sorted().toList();
    }

    public synchronized void addToHistory(ChatMessage message) {
        history.addLast(message);
        while (history.size() > HISTORY_LIMIT) {
            history.removeFirst();
        }
    }

    public synchronized List<ChatMessage> history() {
        return List.copyOf(history);
    }
}
