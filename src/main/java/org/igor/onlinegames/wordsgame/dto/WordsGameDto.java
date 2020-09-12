package org.igor.onlinegames.wordsgame.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = WordsGameStateDto.class, name = "state"),
        @JsonSubTypes.Type(value = WordsGameMsgDto.class, name = "msg"),
        @JsonSubTypes.Type(value = WordsGamePlayerNameWasSetMsgDto.class, name = "msg:PlayerNameWasSet"),
        @JsonSubTypes.Type(value = WordsGameErrorDto.class, name = "error"),
        @JsonSubTypes.Type(value = WordsGameNoAvailablePlacesErrorDto.class, name = "error:NoAvailablePlaces"),
        @JsonSubTypes.Type(value = WordsGamePasscodeIsRequiredErrorDto.class, name = "error:PasscodeRequired"),
        @JsonSubTypes.Type(value = WordsGameIncorrectPasscodeErrorDto.class, name = "error:IncorrectPasscode"),
        @JsonSubTypes.Type(value = WordsGamePlayerNameIsOccupiedErrorDto.class, name = "error:PlayerNameIsOccupied")
})
public interface WordsGameDto {
}
