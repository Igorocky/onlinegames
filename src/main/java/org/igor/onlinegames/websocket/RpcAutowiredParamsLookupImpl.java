package org.igor.onlinegames.websocket;

import org.igor.onlinegames.rpc.RpcAutowiredParamsLookup;
import org.springframework.web.socket.WebSocketSession;

public class RpcAutowiredParamsLookupImpl implements RpcAutowiredParamsLookup {
    private final WebSocketSession session;

    public RpcAutowiredParamsLookupImpl(WebSocketSession session) {
        this.session = session;
    }

    @Override
    public <T> T getByClass(Class<T> clazz) {
        if (WebSocketSession.class.isAssignableFrom(clazz)) {
            return (T) session;
        } else {
            return null;
        }
    }
}
