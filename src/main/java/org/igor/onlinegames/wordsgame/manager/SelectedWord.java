package org.igor.onlinegames.wordsgame.manager;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder
@Data
public class SelectedWord {
    private int paragraphIndex;
    private int wordIndex;
    private String text;
    private Map<Integer,UserInput> userInputs;
}
