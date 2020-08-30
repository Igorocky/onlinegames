package org.igor.onlinegames.xogame.dto.history;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.igor.onlinegames.common.InstantDeserializer;
import org.igor.onlinegames.common.InstantSerializer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XoGameRecord {
    private UUID gameId;
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant startedAt;
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant finishedAt;
    private int fieldSize;
    private Integer secondsPerMove;
    private List<XoGamePlayerInfo> players;
    private List<XoGameMove> moves;
    private Boolean draw;
    private List<List<Integer>> winnerPath;
    private int winnerId;
}
