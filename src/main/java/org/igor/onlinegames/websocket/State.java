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
    private static final Logger LOG = LoggerFactory.getLogger(State.class);
    private Map<String, Pair<Object, Method>> methodMap;
    private Instant createdAt;
    private Instant lastInMsgAt;
    private Instant lastOutMsgAt;
    protected List<WebSocketSession> sessions = new ArrayList<>();
    private Clock clock = Clock.systemUTC();
    protected UUID stateId;

    @Autowired
    private ObjectMapper mapper;

    public synchronized boolean bind(WebSocketSession session, JsonNode bindParams) {
        sessions.add(session);
        return true;
    }

    public synchronized void unbind(WebSocketSession session) {
        sessions.remove(session);
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
        new ArrayList<>(sessions).forEach(this::unbindAndClose);
    }

    protected synchronized void sendMessageToFe(WebSocketSession session, Object msg) {
        try {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
            setLastOutMsgAt(clock.instant());
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    protected synchronized void sendMessageToFe(Object msg) {
        sessions.forEach(session -> sendMessageToFe(session, msg));
    }

    public void setMethodMap(Map<String, Pair<Object, Method>> methodMap) {
        this.methodMap = methodMap;
    }

    public Map<String, Pair<Object, Method>> getMethodMap() {
        return methodMap;
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

    protected Object getViewRepresentation() {
        return this.toString();
    }

    protected void init(JsonNode args) {}
}
