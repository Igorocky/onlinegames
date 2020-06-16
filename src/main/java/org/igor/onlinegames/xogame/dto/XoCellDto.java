package org.igor.onlinegames.xogame.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class XoCellDto {
    private Character symbol;
    private int xCoord;
    private int yCoord;
}
