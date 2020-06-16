package org.igor.onlinegames.xogame.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class XoPlayerDto {
    private UUID playerId;
    private boolean connected;
}
