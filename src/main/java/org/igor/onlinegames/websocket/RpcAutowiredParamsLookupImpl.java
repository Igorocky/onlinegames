package org.igor.onlinegames.websocket;

import lombok.Builder;
import org.igor.onlinegames.rpc.RpcAutowiredParamsLookup;
import org.springframework.web.socket.WebSocketSession;

@Builder
public class RpcAutowiredParamsLookupImpl implements RpcAutowiredParamsLookup {
    private final WebSocketSession session;

    @Override
    public <T> T getByClass(Class<T> clazz) {
        if (WebSocketSession.class.isAssignableFrom(clazz)) {
            return (T) session;
        } else {
            return null;
        }
    }
}
