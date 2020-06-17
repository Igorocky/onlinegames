package org.igor.onlinegames.xogame.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XoGameMsgDto implements XoGameDto {
    private String msg;
}
