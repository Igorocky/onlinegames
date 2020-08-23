package org.igor.onlinegames.xogame.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.igor.onlinegames.xogame.dto.XoGameDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Data
@AllArgsConstructor
@Builder
public class XoPlayer {
    private static final Logger LOG = LoggerFactory.getLogger(XoPlayer.class);

    private final UUID userId;
    private final boolean gameOwner;
    private final int playerId;
    private String name;
    private final Character playerSymbol;
    private final Consumer<XoGameDto> feMessageSender;

    public <T> T ifGameOwner(Supplier<T> exp) {
        if (gameOwner) {
            return exp.get();
        } else {
            return null;
        }
    }

    public void sendMessageToFe(XoGameDto msg) {
        feMessageSender.accept(msg);
    }
}
