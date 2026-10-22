package com.chatapp.dto;

import com.chatapp.model.RoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(@NotBlank @Size(max = 40) String name,
                                @NotNull RoomType type,
                                @Size(max = 72) String password,
                                @NotBlank String username) {
}
