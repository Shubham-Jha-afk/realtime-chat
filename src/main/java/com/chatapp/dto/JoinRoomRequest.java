package com.chatapp.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinRoomRequest(@NotBlank String username, String password) {
}
