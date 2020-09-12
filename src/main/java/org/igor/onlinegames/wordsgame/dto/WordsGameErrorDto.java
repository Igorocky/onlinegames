package org.igor.onlinegames.wordsgame.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@Data
public class WordsGameErrorDto implements WordsGameDto {
    private String errorDescription;
}
