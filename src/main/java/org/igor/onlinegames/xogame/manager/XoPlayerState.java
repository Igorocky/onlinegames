package org.igor.onlinegames.xogame.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.java.Log;
import org.igor.onlinegames.model.UserData;
import org.igor.onlinegames.websocket.StateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

@Log
@Data
public class XoPlayerState {
    private static final Logger LOG = LoggerFactory.getLogger(XoPlayerState.class);

    private final ObjectMapper mapper;
    private Instant createdAt = Instant.now();
    private Instant lastInMsgAt;
    private Instant lastOutMsgAt;
    private boolean connected;
    private boolean gameOwner;
    private UUID joinId;
    private int playerId;
    private Character playerSymbol;
    private UserData userData;
    private boolean optional;
    private WebSocketSession session;

    public XoPlayerState(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public <T> T ifGameOwner(Supplier<T> exp) {
        if (gameOwner) {
            return exp.get();
        } else {
            return null;
        }
    }

    public void sendMessageToFe(Object msg) {
        setLastOutMsgAt(Instant.now());
        try {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }
}
