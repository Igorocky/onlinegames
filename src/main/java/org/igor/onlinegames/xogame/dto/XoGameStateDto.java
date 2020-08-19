package org.igor.onlinegames.xogame.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XoGameStateDto implements XoGameDto {
    private String title;
    private String passcode;
    private XoGamePhase phase;
    private Long numberOfWaitingPlayers;
    private boolean currentUserIsGameOwner;
    private int fieldSize;
    private int goal;
    private List<XoCellDto> field;
    private List<XoPlayerDto> players;
    private Integer currentPlayerId;
    private Integer playerIdToMove;
    private Integer winnerId;
    private List<List<Integer>> winnerPath;
}
