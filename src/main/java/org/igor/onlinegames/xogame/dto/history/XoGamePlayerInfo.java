package org.igor.onlinegames.xogame.dto.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XoGamePlayerInfo {
    private UUID userId;
    private int playerId;
    private String playerName;
}
