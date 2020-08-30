package org.igor.onlinegames.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.igor.onlinegames.model.GameInfoDto;
import org.igor.onlinegames.model.GameState;
import org.igor.onlinegames.model.OnlineGamesUser;
import org.igor.onlinegames.rpc.RpcMethod;
import org.igor.onlinegames.rpc.RpcMethodsCollection;
import org.igor.onlinegames.websocket.State;
import org.igor.onlinegames.websocket.StateManager;
import org.igor.onlinegames.xogame.dto.history.XoGamePlayerInfoDto;
import org.igor.onlinegames.xogame.dto.history.XoGameRecordDto;
import org.igor.onlinegames.xogame.dto.history.XoGameRecordSummaryDto;
import org.igor.onlinegames.xogame.manager.XoGameState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.stream.Stream;

@RpcMethodsCollection
@Component
@Slf4j
public class GameManager {
    private static final Duration ONE_HOUR = Duration.of(1, ChronoUnit.HOURS);
    private static final Duration ONE_DAYS = Duration.of(24, ChronoUnit.HOURS);

    @Value("${app.xogame.history-path}")
    private String historyPath;

    @Autowired
    private StateManager stateManager;
    @Autowired
    private OnlineGamesUser user;
    @Autowired
    private ObjectMapper mapper;

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
    public List<XoGameRecordSummaryDto> getHistory() {
        final UUID userId = user.getUserData().getUserId();
        final Instant currTime = Instant.now();
        return Stream.of(currTime, currTime.minus(ONE_DAYS))
                .map(inst -> XoGameState.getGameHistoryDirPath(historyPath, inst))
                .map(Paths::get)
                .filter(path -> path.toFile().exists())
                .flatMap(this::listDirEntries)
                .filter(path -> path.getFileName().toString().startsWith("xogame-"))
                .map(path -> readValue(path.toFile(), XoGameRecordDto.class))
                .filter(
                        game -> game.getPlayers().stream()
                                .filter(player -> userId.equals(player.getUserId()))
                                .findAny()
                                .isPresent()
                ).sorted(Comparator.comparing(XoGameRecordDto::getStartedAt).reversed())
                .map(
                        game -> {
                            final XoGamePlayerInfoDto winner =
                                    game.getWinnerId() == null
                                            ? null
                                            : game.getPlayers().stream()
                                            .filter(player -> game.getWinnerId().equals(player.getPlayerId()))
                                            .findAny()
                                            .orElse(null);
                            return XoGameRecordSummaryDto.builder()
                                    .gameId(game.getGameId())
                                    .startedAt(game.getStartedAt().toEpochMilli())
                                    .fieldSize(game.getFieldSize())
                                    .goal(game.getGoal())
                                    .secondsPerMove(game.getSecondsPerMove())
                                    .playerNames(
                                            game.getPlayers().stream()
                                                    .map(XoGamePlayerInfoDto::getPlayerName)
                                                    .collect(Collectors.toList())
                                    )
                                    .winnerName(winner == null ? null : winner.getPlayerName())
                                    .draw(game.getDraw())
                                    .currUserIsWinner(winner == null ? null : userId.equals(winner.getUserId()))
                                    .build();
                        }
                ).collect(Collectors.toList());
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
                            .currUserIsOwner(gameState.isOwner(user.getUserData()))
                            .build();
                }).collect(Collectors.toList());
    }

    private boolean isGameToList(State state) {
        return state instanceof GameState && ((GameState) state).isWaitingForPlayersToJoin();
    }

    @SneakyThrows
    private <T> T readValue(File src, Class<T> valueType) {
        return mapper.readValue(src, valueType);
    }

    @SneakyThrows
    private Stream<Path> listDirEntries(Path dir) {
        return Files.list(dir);
    }
}
