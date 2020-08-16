package org.igor.onlinegames.model;

import lombok.Data;

import java.util.UUID;

@Data
public class UserSessionData {
    private UUID userId = UUID.randomUUID();
}
