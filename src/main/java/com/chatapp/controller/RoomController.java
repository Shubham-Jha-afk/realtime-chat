package com.chatapp.controller;

import com.chatapp.config.Destinations;
import com.chatapp.dto.CreateRoomRequest;
import com.chatapp.dto.JoinRoomRequest;
import com.chatapp.model.Room;
import com.chatapp.model.RoomType;
import com.chatapp.model.RoomView;
import com.chatapp.service.RoomService;
import com.chatapp.service.SessionRegistry;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService rooms;
    private final SessionRegistry sessions;
    private final SimpMessagingTemplate template;

    public RoomController(RoomService rooms, SessionRegistry sessions, SimpMessagingTemplate template) {
        this.rooms = rooms;
        this.sessions = sessions;
        this.template = template;
    }

    @GetMapping
    public List<RoomView> list() {
        return rooms.publicRooms().stream().map(RoomView::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomView create(@Valid @RequestBody CreateRoomRequest request) {
        requireConnected(request.username());
        Room room = rooms.create(request.name(), request.type(), request.password(), request.username());
        RoomView view = RoomView.of(room);
        if (room.type() == RoomType.PUBLIC) {
            template.convertAndSend(Destinations.ROOMS_TOPIC, view);
        }
        return view;
    }

    @PostMapping("/{id}/join")
    public RoomView join(@PathVariable String id, @Valid @RequestBody JoinRoomRequest request) {
        requireConnected(request.username());
        return RoomView.of(rooms.join(id, request.username(), request.password()));
    }

    private void requireConnected(String username) {
        if (!sessions.isActive(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Connect over WebSocket before using the API");
        }
    }
}
