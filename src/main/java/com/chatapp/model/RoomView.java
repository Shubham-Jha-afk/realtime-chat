package com.chatapp.model;

public record RoomView(String id, String name, RoomType type, int online) {

    public static RoomView of(Room room) {
        return new RoomView(room.id(), room.name(), room.type(), room.onlineCount());
    }
}
