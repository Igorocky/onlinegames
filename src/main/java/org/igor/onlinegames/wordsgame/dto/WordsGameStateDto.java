package org.igor.onlinegames.wordsgame.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.With;
import org.igor.onlinegames.wordsgame.manager.TextToken;

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
    private Boolean currentUserIsGameOwner;
    private String currentPlayerName;
    private Integer timerSeconds;
    private List<WordsPlayerDto> players;
    private Integer currentPlayerId;
    private Integer playerIdToMove;
    @With
    private String textToLearn;
    @With
    private List<List<TextToken>> words;
    private SelectedWordDto selectedWord;
    private List<WordsPlayerDto> finalScores;
}
