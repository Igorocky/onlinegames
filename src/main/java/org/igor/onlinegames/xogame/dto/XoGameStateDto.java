package org.igor.onlinegames.xogame.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XoGameStateDto implements XoGameDto {
    private XoGamePhase phase;
    private List<List<XoCellDto>> field;
    private List<XoPlayerDto> players;
    private Long playerIdToMove;
    private Long winnerId;
}
