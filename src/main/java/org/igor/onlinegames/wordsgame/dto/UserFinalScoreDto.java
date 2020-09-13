package org.igor.onlinegames.wordsgame.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class UserFinalScoreDto {
    private int playerId;
    private List<String> text;
    private Boolean correct;
    private boolean confirmed;
}
