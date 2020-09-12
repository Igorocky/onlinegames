package org.igor.onlinegames.wordsgame.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.igor.onlinegames.wordsgame.dto.WordsGameDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Data
@AllArgsConstructor
@Builder
public class WordsPlayer {
    private static final Logger LOG = LoggerFactory.getLogger(WordsPlayer.class);

    private final UUID userId;
    private final boolean gameOwner;
    private final Integer playerId;
    private String name;
    private final Consumer<WordsGameDto> feMessageSender;

    public <T> T ifGameOwner(Supplier<T> exp) {
        if (gameOwner) {
            return exp.get();
        } else {
            return null;
        }
    }

    public void sendMessageToFe(WordsGameDto msg) {
        feMessageSender.accept(msg);
    }
}
