package org.igor.onlinegames.wordsgame.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class SelectedWordDto {
    private int paragraphIndex;
    private int wordIndex;
    private List<String> expectedText;
    private List<UserInputDto> userInputs;
}
