package org.igor.onlinegames.xogame.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class XoGameStateDto implements XoGameDto {
    private List<List<XoCellDto>> field;
    private List<XoPlayerDto> players;
    private UUID playerIdToMove;
    private UUID winnerId;
}
