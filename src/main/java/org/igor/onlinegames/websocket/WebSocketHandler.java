package org.igor.onlinegames.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketHandler extends TextWebSocketHandler {
    public static final String STATE_ID = "stateId";
    @Autowired
    private StateManager stateManager;
    @Autowired
    private ObjectMapper mapper;
    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        final String payload = message.getPayload();
        Optional<UUID> stateIdOpt = Optional.ofNullable((UUID) session.getAttributes().get(STATE_ID));
        if (!stateIdOpt.isPresent()) {
            UUID stateId = UUID.fromString(payload);
            session.getAttributes().put(STATE_ID, stateId);
            bindSessionToState(stateManager.getBackendState(stateId), session);
        } else {
            final UUID stateId = stateIdOpt.get();
            AsyncWebSocketRpcCall request = mapper.readValue(payload, AsyncWebSocketRpcCall.class);
            executorService.submit(() -> {
                Object resp = stateManager.invokeMethodOnBackendState(
                        stateId, request.getMethodName(), request.getParams()
                );
                if (resp != null) {
                    stateManager.getBackendState(stateId).sendMessageToFe(resp);
                }
            });
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID stateId = (UUID) session.getAttributes().get(STATE_ID);
        if (stateId != null) {
            unbindSessionFromState(stateManager.getBackendState(stateId));
        }
    }

    private void bindSessionToState(State stateObject, WebSocketSession session) {
        stateObject.closeSession();
        stateObject.setSession(session);
    }

    private void unbindSessionFromState(State stateObject) {
        stateObject.closeSession();
        stateObject.setSession(null);
    }
}
