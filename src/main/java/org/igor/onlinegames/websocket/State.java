package org.igor.onlinegames.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.igor.onlinegames.exceptions.OnlinegamesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

public abstract class State {
    private static final Logger LOG = LoggerFactory.getLogger(StateManager.class);
    private Map<String, Pair<Object, Method>> methodMap;
    private Instant createdAt;
    private Instant lastInMsgAt;
    private Instant lastOutMsgAt;
    private WebSocketSession session;
    private Clock clock = Clock.systemUTC();

    @Autowired
    private ObjectMapper mapper;

    public void setMethodMap(Map<String, Pair<Object, Method>> methodMap) {
        this.methodMap = methodMap;
    }

    public Map<String, Pair<Object, Method>> getMethodMap() {
        return methodMap;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
    }

    public void closeSession() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        session = null;
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

    protected synchronized void sendMessageToFe(Object msg) {
        setLastOutMsgAt(clock.instant());
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
            } catch (IOException ex) {
                throw new OnlinegamesException(ex);
            }
        }
    }

    abstract protected Object getViewRepresentation();
}
