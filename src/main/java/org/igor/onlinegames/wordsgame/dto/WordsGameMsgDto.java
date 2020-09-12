package org.igor.onlinegames.wordsgame.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WordsGameMsgDto implements WordsGameDto {
    private String msg;
}
