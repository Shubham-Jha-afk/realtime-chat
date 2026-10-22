package com.chatapp.service;

import com.chatapp.model.Room;
import com.chatapp.model.RoomType;
import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoomService {

    public static final String LOBBY_ID = "general";

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostConstruct
    void createLobby() {
        rooms.put(LOBBY_ID, new Room(LOBBY_ID, "General", RoomType.PUBLIC, null, "system"));
    }

    public Room create(String name, RoomType type, String password, String owner) {
        if (type == RoomType.PRIVATE && (password == null || password.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Private rooms need a password");
        }
        String id = UUID.randomUUID().toString().substring(0, 8);
        String hash = type == RoomType.PRIVATE ? passwordEncoder.encode(password) : null;
        Room room = new Room(id, name.trim(), type, hash, owner);
        if (type == RoomType.PRIVATE) {
            room.addMember(owner);
        }
        rooms.put(id, room);
        return room;
    }

    public Room get(String id) {
        Room room = rooms.get(id);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        return room;
    }

    public List<Room> publicRooms() {
        return rooms.values().stream()
                .filter(room -> room.type() == RoomType.PUBLIC)
                .sorted(Comparator.comparing(Room::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Room join(String id, String username, String password) {
        Room room = get(id);
        if (room.type() == RoomType.PRIVATE) {
            if (password == null || !passwordEncoder.matches(password, room.passwordHash())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong password");
            }
            room.addMember(username);
        }
        return room;
    }

    public boolean canAccess(String id, String username) {
        Room room = rooms.get(id);
        return room != null && (room.type() == RoomType.PUBLIC || room.isMember(username));
    }

    public boolean isOnline(String id, String username) {
        Room room = rooms.get(id);
        return room != null && room.isOnline(username);
    }

    public void revokeMemberships(String username) {
        rooms.values().forEach(room -> room.removeMember(username));
    }
}
