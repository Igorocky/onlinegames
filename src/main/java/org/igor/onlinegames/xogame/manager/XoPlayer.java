package org.igor.onlinegames.xogame.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

@Data
@AllAr
public class XoPlayer {
    private static final Logger LOG = LoggerFactory.getLogger(XoPlayer.class);

    private final UUID userId;
    private final boolean gameOwner;
    private final int playerId;
    private final Character playerSymbol;
    private final ObjectMapper mapper;
    private Instant lastInMsgAt;

    public <T> T ifGameOwner(Supplier<T> exp) {
        if (gameOwner) {
            return exp.get();
        } else {
            return null;
        }
    }

    public void sendMessageToFe(Object msg) {
        try {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }
}
