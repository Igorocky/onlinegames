package org.igor.onlinegames.wordsgame.manager;

import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Builder
@Data
public class UserInput {
    private String text;
    private Optional<Boolean> correct;
    private boolean confirmed;
}
