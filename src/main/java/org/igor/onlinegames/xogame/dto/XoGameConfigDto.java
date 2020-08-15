package org.igor.onlinegames.xogame.dto;

import lombok.Data;

import java.util.List;

@Data
public class XoGameConfigDto {
    private int fieldSize = 3;
    private List<XoPlayerConfigDto> playerConfigList;
    private int secondsToWaitJoin = 60;
}
