package org.igor.onlinegames.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import org.igor.onlinegames.exceptions.OnlinegamesException;
import org.igor.onlinegames.rpc.RpcDispatcher;
import org.igor.onlinegames.rpc.RpcMethod;
import org.igor.onlinegames.rpc.RpcMethodsCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.Clock;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RpcMethodsCollection
@Component
public class StateManager {
    private static final Logger LOG = LoggerFactory.getLogger(StateManager.class);
    private Map<UUID, State> states = new HashMap<>();
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RpcDispatcher rpcDispatcher;

    private Clock clock = Clock.systemUTC();

    @RpcMethod
    public UUID createNewBackendState(String stateType) {
        UUID newId = UUID.randomUUID();
        State stateObj = (State) applicationContext.getBean(stateType);
        stateObj.setMethodMap(rpcDispatcher.createMethodMap(stateObj));
        stateObj.setCreatedAt(clock.instant());
        states.put(newId, stateObj);
        return newId;
    }

    public State getBackendState(UUID id) {
        return states.get(id);
    }

    @RpcMethod
    public List<StateInfoDto> listBeStates() {
        return states.entrySet().stream()
                .sorted(Comparator.comparing(uuidStateEntry -> uuidStateEntry.getValue().getCreatedAt()))
                .map(uuidStateEntry -> {
                        final State state = uuidStateEntry.getValue();
                        return StateInfoDto.builder()
                                .stateId(uuidStateEntry.getKey())
                                .stateType(state.getClass().getSimpleName())
                                .createdAt(state.getCreatedAt().toString())
                                .lastInMsgAt(state.getLastInMsgAt() == null ? null : state.getLastInMsgAt().toString())
                                .lastOutMsgAt(state.getLastOutMsgAt() == null ? null : state.getLastOutMsgAt().toString())
                                .viewRepresentation(state.getViewRepresentation())
                                .build();
                }).collect(Collectors.toList());
    }

    @RpcMethod
    public void removeBackendState(UUID stateId) {
        State stateObj = getStateObject(stateId);
        stateObj.closeSession();
        states.remove(stateId);
    }

    public void invokeMethodOnBackendState(UUID stateId, String methodName, JsonNode params) {
        executorService.submit(() -> {
            final State stateObject = getStateObject(stateId);
            stateObject.setLastInMsgAt(clock.instant());
            try {
                Object result = rpcDispatcher.dispatchRpcCall(methodName, params, stateObject.getMethodMap());
                if (result != null) {
                    stateObject.sendMessageToFe(result);
                }
            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
                throw new OnlinegamesException(ex);
            }
        });
    }

    public void bindSessionToState(UUID stateId, WebSocketSession session) {
        final State stateObject = getStateObject(stateId);
        stateObject.closeSession();
        stateObject.setSession(session);
    }

    public void unbindSessionFromState(UUID stateId) {
        getStateObject(stateId).setSession(null);
    }

    private State getStateObject(UUID stateId) {
        return states.get(stateId);
    }
}
