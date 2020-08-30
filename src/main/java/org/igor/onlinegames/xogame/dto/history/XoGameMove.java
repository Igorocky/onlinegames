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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XoGameMove {
    private int moveNumber;
    private int playerId;
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant time;
    private int x;
    private int y;
}
