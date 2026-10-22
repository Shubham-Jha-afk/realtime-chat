package com.chatapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chatapp.model.ChatMessage;
import com.chatapp.model.Room;
import com.chatapp.model.RoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RoomServiceTest {

    private RoomService service;

    @BeforeEach
    void setUp() {
        service = new RoomService();
        service.createLobby();
    }

    @Test
    void lobbyIsPublicAndAccessibleToEveryone() {
        assertTrue(service.canAccess(RoomService.LOBBY_ID, "anyone"));
        assertEquals(1, service.publicRooms().size());
    }

    @Test
    void privateRoomRequiresPassword() {
        assertThrows(ResponseStatusException.class,
                () -> service.create("secret", RoomType.PRIVATE, " ", "alice"));
    }

    @Test
    void privateRoomIsHiddenFromPublicListing() {
        service.create("secret", RoomType.PRIVATE, "pw", "alice");
        assertEquals(1, service.publicRooms().size());
    }

    @Test
    void ownerIsMemberButOthersMustJoinWithPassword() {
        Room room = service.create("secret", RoomType.PRIVATE, "pw", "alice");
        assertTrue(service.canAccess(room.id(), "alice"));
        assertFalse(service.canAccess(room.id(), "bob"));
        assertThrows(ResponseStatusException.class, () -> service.join(room.id(), "bob", "wrong"));
        service.join(room.id(), "bob", "pw");
        assertTrue(service.canAccess(room.id(), "bob"));
    }

    @Test
    void revokingMembershipsRemovesPrivateAccess() {
        Room room = service.create("secret", RoomType.PRIVATE, "pw", "alice");
        service.revokeMemberships("alice");
        assertFalse(service.canAccess(room.id(), "alice"));
    }

    @Test
    void historyKeepsOnlyTheLatestFiftyMessages() {
        Room room = service.get(RoomService.LOBBY_ID);
        for (int i = 0; i < 60; i++) {
            room.addToHistory(ChatMessage.chat(room.id(), "alice", "m" + i));
        }
        assertEquals(50, room.history().size());
        assertEquals("m10", room.history().get(0).content());
    }

    @Test
    void unknownRoomIsNotFound() {
        assertThrows(ResponseStatusException.class, () -> service.get("nope"));
    }
}
