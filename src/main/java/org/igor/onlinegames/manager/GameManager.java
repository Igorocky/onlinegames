package org.igor.onlinegames.manager;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.igor.onlinegames.model.GameInfoDto;
import org.igor.onlinegames.model.GameState;
import org.igor.onlinegames.rpc.RpcMethod;
import org.igor.onlinegames.rpc.RpcMethodsCollection;
import org.igor.onlinegames.websocket.State;
import org.igor.onlinegames.websocket.StateManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RpcMethodsCollection
@Component
@Slf4j
public class GameManager {
    private static final Duration ONE_HOUR = Duration.of(1, ChronoUnit.HOURS);

    @Autowired
    private StateManager stateManager;

    @Autowired
    @Qualifier("scheduledExecutorService")
    private ScheduledExecutorService scheduledExecutorService;

    @PostConstruct
    public void postConstruct() {
        scheduledExecutorService.scheduleAtFixedRate(
                () -> {
                    try {
                        final Instant currTime = Instant.now();
                        final List<Map.Entry<UUID, State>> statesToRemove = stateManager.getStates().entrySet().stream()
                                .filter(entry -> {
                                    final State state = entry.getValue();
                                    return state instanceof GameState
                                            && Duration.between(state.getLastOutMsgAt(), currTime).compareTo(ONE_HOUR) > 0;
                                })
                                .collect(Collectors.toList());
                        statesToRemove.stream()
                                .map(Map.Entry::getKey)
                                .forEach(stateManager::removeBackendState);
                    } catch (Exception ex) {
                        log.error(ex.getMessage(), ex);
                    }
                },
                0,
                1,
                TimeUnit.HOURS
        );
    }

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
                            .title(StringUtils.abbreviate(gameState.getTitle(), 50))
                            .shortDescription(gameState.getShortDescription())
                            .hasPasscode(gameState.hasPasscode())
                            .build();
                }).collect(Collectors.toList());
    }

    private boolean isGameToList(State state) {
        return state instanceof GameState && ((GameState) state).isWaitingForPlayersToJoin();
    }
}
