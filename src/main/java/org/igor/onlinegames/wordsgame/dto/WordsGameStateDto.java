package org.igor.onlinegames.wordsgame.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WordsGameStateDto implements WordsGameDto {
    private String title;
    private String passcode;
    private WordsGamePhase phase;
    private Long numberOfWaitingPlayers;
    private List<String> namesOfWaitingPlayers;
    private boolean currentUserIsGameOwner;
    private String currentPlayerName;
    private String wordsToLearnStr;
    private Integer timerSeconds;
    private List<WordsPlayerDto> players;
    private Integer currentPlayerId;
    private Integer playerIdToMove;
}