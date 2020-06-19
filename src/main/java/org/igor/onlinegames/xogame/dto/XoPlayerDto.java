package org.igor.onlinegames.xogame.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XoPlayerDto {
    private UUID joinId;
    private int playerId;
    private boolean connected;
    private Boolean gameOwner;
}
