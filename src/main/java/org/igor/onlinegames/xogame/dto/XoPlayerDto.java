package org.igor.onlinegames.xogame.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XoPlayerDto {
    private int playerId;
    private String name;
    private Boolean gameOwner;
    private Character symbol;
}
