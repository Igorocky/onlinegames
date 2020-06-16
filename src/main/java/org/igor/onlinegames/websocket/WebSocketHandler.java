package org.igor.onlinegames.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;

public class WebSocketHandler extends TextWebSocketHandler {
    public static final String STATE_ID = "stateId";
    @Autowired
    private StateManager stateManager;
    @Autowired
    private ObjectMapper mapper;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        final String payload = message.getPayload();
        UUID stateId = (UUID) session.getAttributes().get(STATE_ID);
        if (stateId == null) {
            stateId = UUID.fromString(payload);
            session.getAttributes().put(STATE_ID, stateId);
            stateManager.bindSessionToState(stateId, session);
        } else {
            AsyncWebSocketRpcCall request = mapper.readValue(payload, AsyncWebSocketRpcCall.class);
            stateManager.invokeMethodOnBackendState(
                    stateId,
                    request.getMethodName(),
                    request.getParams()
            );
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID stateId = (UUID) session.getAttributes().get(STATE_ID);
        if (stateId != null) {
            stateManager.unbindSessionFromState(stateId);
        }
    }
}
