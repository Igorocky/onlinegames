package org.igor.onlinegames.xogame.dto.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XoGameRecordSummaryDto {
    private UUID gameId;
    private Long startedAt;
    private int fieldSize;
    private int goal;
    private Integer secondsPerMove;
    private List<String> playerNames;
    private String winnerName;
    private Boolean draw;
    private Boolean currUserIsWinner;
}
