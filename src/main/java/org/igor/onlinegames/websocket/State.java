package org.igor.onlinegames.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class State {
    private static final Logger LOG = LoggerFactory.getLogger(StateManager.class);
    private Map<String, Pair<Object, Method>> methodMap;
    private Instant createdAt;
    private Instant lastInMsgAt;
    private Instant lastOutMsgAt;
    private List<WebSocketSession> sessions = new ArrayList<>();
    private Clock clock = Clock.systemUTC();
    private UUID stateId;

    @Autowired
    private ObjectMapper mapper;

    public void setMethodMap(Map<String, Pair<Object, Method>> methodMap) {
        this.methodMap = methodMap;
    }

    public Map<String, Pair<Object, Method>> getMethodMap() {
        return methodMap;
    }

    public synchronized void bind(WebSocketSession session, JsonNode bindParams) {
        sessions.add(session);
    }

    public synchronized void unbind(WebSocketSession session) {
        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i) == session) {
                sessions.remove(i);
                i--;
            }
        }
    }

    public synchronized void unbindAndClose(WebSocketSession session) {
        unbind(session);
        if (session.isOpen()) {
            try {
                session.close();
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
    }

    public synchronized void unbindAndCloseAllWebSockets() {
        sessions.forEach(this::unbindAndClose);
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastInMsgAt() {
        return lastInMsgAt;
    }

    public void setLastInMsgAt(Instant lastInMsgAt) {
        this.lastInMsgAt = lastInMsgAt;
    }

    public Instant getLastOutMsgAt() {
        return lastOutMsgAt;
    }

    public void setLastOutMsgAt(Instant lastOutMsgAt) {
        this.lastOutMsgAt = lastOutMsgAt;
    }

    public UUID getStateId() {
        return stateId;
    }

    public void setStateId(UUID stateId) {
        this.stateId = stateId;
    }

    protected synchronized void sendMessageToFe(Object msg) {
        if (!sessions.isEmpty()) {
            setLastOutMsgAt(clock.instant());
            for (WebSocketSession session : sessions) {
                try {
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
                } catch (IOException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
        }
    }

    abstract protected Object getViewRepresentation();
    abstract protected void init(JsonNode args);
}
