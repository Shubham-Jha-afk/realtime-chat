package com.chatapp.model;

import java.util.List;

public record PresenceEvent(String roomId, List<String> users) {
}
