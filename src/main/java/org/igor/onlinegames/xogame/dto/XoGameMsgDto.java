package org.igor.onlinegames.xogame.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class XoGameMsgDto implements XoGameDto {
    private String msg;
}
