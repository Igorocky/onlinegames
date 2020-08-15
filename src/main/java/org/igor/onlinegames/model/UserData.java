package org.igor.onlinegames.model;

import lombok.Data;

import java.util.UUID;

@Data
public class UserData {
    private String userID = UUID.randomUUID().toString();
}
