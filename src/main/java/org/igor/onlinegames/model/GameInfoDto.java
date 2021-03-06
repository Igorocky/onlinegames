package org.igor.onlinegames.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class GameInfoDto {
    private UUID gameId;
    private String gameType;
    private String gameDisplayType;
    private String title;
    private String shortDescription;
    private boolean hasPasscode;
    private boolean currUserIsOwner;
    private boolean inProgress;
}
