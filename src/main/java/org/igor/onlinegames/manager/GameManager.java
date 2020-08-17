package org.igor.onlinegames.manager;

import org.igor.onlinegames.model.GameInfoDto;
import org.igor.onlinegames.model.GameState;
import org.igor.onlinegames.rpc.RpcMethod;
import org.igor.onlinegames.rpc.RpcMethodsCollection;
import org.igor.onlinegames.websocket.State;
import org.igor.onlinegames.websocket.StateManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RpcMethodsCollection
@Component
public class GameManager {
    @Autowired
    private StateManager stateManager;

    @RpcMethod
    public List<GameInfoDto> listNewGames() {
        return stateManager.getStates().entrySet().stream()
                .filter(entry -> isGameToList(entry.getValue()))
                .sorted(Comparator.comparing(entry -> entry.getValue().getCreatedAt()))
                .map(entry -> {
                    GameState gameState = (GameState) entry.getValue();
                    return GameInfoDto.builder()
                            .gameId(entry.getKey())
                            .gameType(gameState.gameType())
                            .gameDisplayType(gameState.gameDisplayType())
                            .build();
                }).collect(Collectors.toList());
    }

    private boolean isGameToList(State state) {
        return state instanceof GameState && ((GameState) state).isWaitingForPlayersToJoin();
    }
}
