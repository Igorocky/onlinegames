package org.igor.onlinegames.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

@Getter
public class AsyncWebSocketRpcCall {
    private String methodName;
    private JsonNode params;
}
