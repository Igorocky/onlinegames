package org.igor.onlinegames.xogame.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import org.igor.onlinegames.model.UserSessionData;
import org.igor.onlinegames.rpc.RpcMethod;
import org.igor.onlinegames.websocket.State;
import org.igor.onlinegames.xogame.dto.XoCellDto;
import org.igor.onlinegames.xogame.dto.XoGameConfigDto;
import org.igor.onlinegames.xogame.dto.XoGameConnectDto;
import org.igor.onlinegames.xogame.dto.XoGameErrorDto;
import org.igor.onlinegames.xogame.dto.XoGamePhase;
import org.igor.onlinegames.xogame.dto.XoGameStateDto;
import org.igor.onlinegames.xogame.dto.XoPlayerConfigDto;
import org.igor.onlinegames.xogame.dto.XoPlayerDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.igor.onlinegames.common.OnlinegamesUtils.nullSafeGetter;
import static org.igor.onlinegames.websocket.WebSocketHandler.USER_DATA;

@Component("XoGame")
@Scope("prototype")
public class XoGameState extends State {
    private static final String PLAYER_STATE = "playerState";

    private XoGamePhase phase;
    private List<XoPlayer> players;
    private Character[][] field;
    private XoPlayer playerToMove;
    private XoPlayer winner;

    @Autowired
    private ObjectMapper mapper;

    public void startGame() {

    }

    @RpcMethod
    public void clickCell(WebSocketSession session, int x, int y) {
        final XoPlayer playerState = extractPlayerFromSession(session);
        playerState.setLastInMsgAt(Instant.now());
        clickCell(playerState, x, y);
    }

    @RpcMethod
    public void ping(WebSocketSession session) {
        final XoPlayer playerState = extractPlayerFromSession(session);
        playerState.setLastInMsgAt(Instant.now());
    }

    private XoPlayer extractPlayerFromSession(WebSocketSession session) {
        return (XoPlayer) session.getAttributes().get(PLAYER_STATE);
    }

    private UUID extractUserIdFromSession(WebSocketSession session) {
        return ((UserSessionData) session.getAttributes().get(USER_DATA)).getUserId();
    }

    private XoGameStateDto createViewOfCurrentState(XoPlayer viewer) {
        return XoGameStateDto.builder()
                .phase(phase)
                .field(createFieldDto(field))
                .currentPlayerId(viewer.getPlayerId())
                .players(createPlayersDto(viewer, players))
                .playerIdToMove(nullSafeGetter(playerToMove, XoPlayer::getPlayerId))
                .winnerId(nullSafeGetter(winner, XoPlayer::getPlayerId))
                .build();
    }

    public void clickCell(XoPlayer player, int x, int y) {
        if (phase == XoGamePhase.IN_PROGRESS) {
            if (!(0 <= x && x < field.length && 0 <= y && y < field[0].length)) {
                player.sendMessageToFe(XoGameErrorDto.builder()
                        .errorDescription("Incorrect coordinates: x = " + x + ", y = " + y + ".").build());
            } else if (playerToMove != player) {
                player.sendMessageToFe(XoGameErrorDto.builder().errorDescription("It's not your turn.").build());
            } else if (field[x][y] != null) {
                player.sendMessageToFe(
                        XoGameErrorDto.builder().errorDescription("The cell you clicked is not empty.").build()
                );
            } else {
                field[x][y] = player.getPlayerSymbol();

                Character winnerSymbol = findWinnerSymbol();
                if (winnerSymbol != null) {
                    playerToMove = null;
                    winner = players.stream()
                            .filter(xoPlayer -> winnerSymbol.equals(xoPlayer.getPlayerSymbol()))
                            .findFirst()
                            .get();
                    phase = XoGamePhase.FINISHED;
                } else if (isDraw()) {
                    playerToMove = null;
                    phase = XoGamePhase.FINISHED;
                } else {
                    playerToMove = getNextPlayerToMove();
                }
                broadcastGameState();
            }
        } else {
            broadcastGameState();
        }
    }

    public void playerConnected(XoPlayer player) {
        if (players.stream().allMatch(p -> !p.isGameOwner())) {
            player.setGameOwner(true);
        }
        player.setConnected(true);
        if (phase == XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN && players.stream().allMatch(XoPlayer::isConnected)) {
            phase = XoGamePhase.IN_PROGRESS;
        }
        broadcastGameState();
    }

    private XoPlayer getNextPlayerToMove() {
        return players.stream().filter(player -> player != playerToMove).findFirst().get();
    }

    private void broadcastGameState() {
        players.forEach(xoPlayer -> xoPlayer.sendMessageToFe(createViewOfCurrentState(xoPlayer)));
    }

    private List<XoPlayerDto> createPlayersDto(XoPlayer viewer, List<XoPlayer> players) {
        return players.stream()
                .map(xoPlayer -> XoPlayerDto.builder()
                        .joinId(viewer.ifGameOwner(() -> xoPlayer.getJoinId()))
                        .gameOwner(viewer.ifGameOwner(() -> xoPlayer.isGameOwner()))
                        .playerId(xoPlayer.getPlayerId())
                        .connected(xoPlayer.isConnected())
                        .symbol(xoPlayer.getPlayerSymbol())
                        .build()
                )
                .collect(Collectors.toList());
    }

    private List<XoCellDto> createFieldDto(Character[][] field) {
        final ArrayList<XoCellDto> cellsDto = new ArrayList<>();
        for (int x = 0; x < field.length; x++) {
            for (int y = 0; y < field[x].length; y++) {
                final Character cellSymbol = field[x][y];
                if (cellSymbol != null) {
                    cellsDto.add(XoCellDto.builder().x(x).y(y).symbol(cellSymbol).build());
                }
            }
        }
        return cellsDto;
    }

    private Character findWinnerSymbol() {
        if (allCellsAreOfSameSymbol(0, 0, 0, 1, 0, 2)) {
            return field[0][0];
        } else if (allCellsAreOfSameSymbol(1, 0, 1, 1, 1, 2)) {
            return field[1][0];
        } else if (allCellsAreOfSameSymbol(2, 0, 2, 1, 2, 2)) {
            return field[2][0];
        } else if (allCellsAreOfSameSymbol(0, 0, 1, 0, 2, 0)) {
            return field[0][0];
        } else if (allCellsAreOfSameSymbol(0, 1, 1, 1, 2, 1)) {
            return field[0][1];
        } else if (allCellsAreOfSameSymbol(0, 2, 1, 2, 2, 2)) {
            return field[0][2];
        } else if (allCellsAreOfSameSymbol(0, 0, 1, 1, 2, 2)) {
            return field[0][0];
        } else if (allCellsAreOfSameSymbol(0, 2, 1, 1, 2, 0)) {
            return field[0][2];
        } else {
            return null;
        }
    }

    private boolean isDraw() {
        return !Arrays.asList(field).stream().flatMap(r -> Arrays.asList(r).stream()).anyMatch(Objects::isNull);
    }

    private boolean allCellsAreOfSameSymbol(int x1, int y1, int x2, int y2, int x3, int y3) {
        return field[x1][y1] != null
                && field[x2][y2] != null
                && field[x3][y3] != null
                && field[x1][y1].equals(field[x2][y2])
                && field[x2][y2].equals(field[x3][y3]);
    }

    private List<XoPlayer> findPlayersWithDelayMoreThan(int delaySeconds) {
        final Instant currTime = Instant.now();
        return players.stream()
                .filter(playerState -> Duration.between(playerState.getLastInMsgAt(), currTime).getSeconds() > delaySeconds)
                .collect(Collectors.toList());
    }

    private List<XoPlayer> findPlayersWithDelayLessThan(int delaySeconds) {
        final Instant currTime = Instant.now();
        return players.stream()
                .filter(playerState -> Duration.between(playerState.getLastInMsgAt(), currTime).getSeconds() < delaySeconds)
                .collect(Collectors.toList());
    }

    @Override
    protected Object getViewRepresentation() {
        return this.getClass().getSimpleName();
    }

    @SneakyThrows
    @Override
    public synchronized void bind(WebSocketSession session, JsonNode bindParams) {
        if (phase == XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN || phase == XoGamePhase.IN_PROGRESS) {
            final XoGameConnectDto connectParams = mapper.readValue(mapper.treeAsTokens(bindParams), XoGameConnectDto.class);
            if (connectParams.getJoinId() != null) {
                players.stream()
                        .filter(player -> player.getJoinId().equals(connectParams.getJoinId()))
                        .findAny()
                        .ifPresent(player -> {
                            bindSessionToPlayer(session, player);
                            onTimer();
                        });
            } else if (phase == XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN) {
                findPlayersByUserId(extractUserIdFromSession(session)).forEach(this::unbindSessionFromPlayer);
                final XoPlayer playerToBindTo = players.stream()
                        .filter(player -> !player.isConnected() && !player.isOptional())
                        .findAny()
                        .orElseGet(
                                () -> players.stream()
                                        .filter(player -> !player.isConnected())
                                        .findAny()
                                        .orElse(null)
                        );
                if (playerToBindTo != null) {
                    bindSessionToPlayer(session, playerToBindTo);
                }
            } else if (phase == XoGamePhase.IN_PROGRESS) {
                val playersWithSameUserId = findPlayersByUserId(extractUserIdFromSession(session));
                playersWithSameUserId.forEach(this::unbindSessionFromPlayer);
                if (!playersWithSameUserId.isEmpty()) {
                    bindSessionToPlayer(session, playersWithSameUserId.get(0));
                }
            }
        }
    }

    private List<XoPlayer> findPlayersByUserId(UUID userId) {
        return players.stream()
                .filter(playerState -> playerState.getSession() != null && userId.equals(extractUserIdFromSession(playerState.getSession())))
                .collect(Collectors.toList());
    }

    private void bindSessionToPlayer(WebSocketSession session, XoPlayer player) {
        unbindSessionFromPlayer(player);
        session.getAttributes().put(PLAYER_STATE, player);
        player.setSession(session);
        player.setConnected(true);
        player.setLastInMsgAt(Instant.now());
    }

    @SneakyThrows
    private void unbindSessionFromPlayer(XoPlayer player) {
        if (player.getSession() != null) {
            player.getSession().close();
            player.getSession().getAttributes().remove(PLAYER_STATE);
        }
        player.setSession(null);
        player.setConnected(false);
    }

    @SneakyThrows
    @Override
    protected void init(JsonNode args) {
        final XoGameConfigDto gameConfig = mapper.readValue(mapper.treeAsTokens(args), XoGameConfigDto.class);

        phase = XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN;
        players = new ArrayList<>();

        for (int i = 0; i < gameConfig.getPlayerConfigList().size(); i++) {
            final XoPlayerConfigDto playerConfig = gameConfig.getPlayerConfigList().get(i);
            UUID playerStateId = UUID.randomUUID();
            XoPlayer player = new XoPlayer(mapper);
            player.setJoinId(playerStateId);
            player.setPlayerId(i+1);
            player.setPlayerSymbol(validatePlayerSymbol(players, playerConfig.getPlayerSymbol()));
            player.setOptional(playerConfig.isOptional());
            players.add(player);
        }

        field = new Character[gameConfig.getFieldSize()][gameConfig.getFieldSize()];
    }

    private Character validatePlayerSymbol(List<XoPlayer> players, Character symbolFromConfig) {
        if (symbolFromConfig == null || !(symbolFromConfig.charValue() == 'x' || symbolFromConfig.charValue() == 'o')) {
            return null;
        } else {
            final Set<Character> existingSymbols = players.stream()
                    .map(XoPlayer::getPlayerSymbol)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (existingSymbols.contains(symbolFromConfig)) {
                return null;
            } else {
                return symbolFromConfig;
            }
        }
    }
}
