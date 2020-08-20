package org.igor.onlinegames.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.igor.onlinegames.common.OnlinegamesUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class WebSocketHandler extends TextWebSocketHandler {
    @Autowired
    private StateManager stateManager;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    @Qualifier("executorService")
    private ExecutorService executorService;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        final String payload = message.getPayload();
        AsyncWebSocketRpcCall request = mapper.readValue(payload, AsyncWebSocketRpcCall.class);
        Optional<UUID> stateIdOpt = OnlinegamesUtils.extractDestinationStateId(session);
        if ("-bindToState".equals(request.getMethodName())) {
            UUID newStateId = UUID.fromString(request.getParams().get("stateId").asText());
            UUID oldStateId = stateIdOpt.orElse(null);
            if (!newStateId.equals(oldStateId)) {
                if (oldStateId != null) {
                    stateManager.getBackendState(oldStateId).unbind(session);
                    OnlinegamesUtils.setDestinationStateId(session, null);
                }
                if (stateManager.getBackendState(newStateId).bind(session, request.getParams().get("bindParams"))) {
                    OnlinegamesUtils.setDestinationStateId(session, newStateId);
                }
            }
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
        UUID stateId = OnlinegamesUtils.extractDestinationStateId(session).orElse(null);
        if (stateId != null) {
            stateManager.getBackendState(stateId).unbindAndClose(session);
        }
    }
}
