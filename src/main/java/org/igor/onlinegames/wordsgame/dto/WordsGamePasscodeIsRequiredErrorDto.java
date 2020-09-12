package org.igor.onlinegames.wordsgame.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class WordsGamePasscodeIsRequiredErrorDto implements WordsGameDto {
}
