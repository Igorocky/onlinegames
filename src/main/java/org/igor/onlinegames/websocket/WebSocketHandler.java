package org.igor.onlinegames.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketHandler extends TextWebSocketHandler {
    public static final String STATE_ID = "STATE_ID";
    public static final String USER_DATA = "USER_DATA";
    @Autowired
    private StateManager stateManager;
    @Autowired
    private ObjectMapper mapper;
    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        final String payload = message.getPayload();
        AsyncWebSocketRpcCall request = mapper.readValue(payload, AsyncWebSocketRpcCall.class);
        Optional<UUID> stateIdOpt = Optional.ofNullable((UUID) session.getAttributes().get(STATE_ID));
        if ("-bindToState".equals(request.getMethodName())) {
            UUID newStateId = UUID.fromString(request.getParams().get("stateId").asText());
            UUID oldStateId = stateIdOpt.orElse(null);
            if (!newStateId.equals(oldStateId)) {
                if (oldStateId != null) {
                    stateManager.getBackendState(oldStateId).unbind(session);
                }
                stateManager.getBackendState(newStateId).bind(session, request.getParams().get("bindParams"));
                session.getAttributes().put(STATE_ID, newStateId);
            }
            stateManager.getBackendState(newStateId).setLastInMsgAt(Instant.now());
        } else {
            final UUID stateId = stateIdOpt.get();
            executorService.submit(() -> {
                Object resp = stateManager.invokeMethodOnBackendState(
                        stateId, request.getMethodName(), request.getParams(), session
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
            stateManager.getBackendState(stateId).unbindAndClose(session);
        }
    }
}
