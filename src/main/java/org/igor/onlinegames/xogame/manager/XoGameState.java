package org.igor.onlinegames.xogame.manager;

import com.fasterxml.jackson.databind.JsonNode;
import org.igor.onlinegames.common.OnlinegamesUtils;
import org.igor.onlinegames.model.UserSessionData;
import org.igor.onlinegames.rpc.RpcMethod;
import org.igor.onlinegames.websocket.State;
import org.igor.onlinegames.xogame.dto.XoCellDto;
import org.igor.onlinegames.xogame.dto.XoGameErrorDto;
import org.igor.onlinegames.xogame.dto.XoGamePhase;
import org.igor.onlinegames.xogame.dto.XoGameStateDto;
import org.igor.onlinegames.xogame.dto.XoPlayerDto;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.igor.onlinegames.common.OnlinegamesUtils.listOf;
import static org.igor.onlinegames.common.OnlinegamesUtils.nullSafeGetter;

@Component("XoGame")
@Scope("prototype")
public class XoGameState extends State {
    private static final String PLAYER_STATE = "playerState";
    public static final int MAX_NUMBER_OF_PLAYERS = 2;

    private XoGamePhase phase = XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN;
    private UUID gameOwnerUserId;
    private List<XoPlayer> players;
    private Character[][] field;
    private XoPlayer playerToMove;
    private XoPlayer winner;

    @Override
    public synchronized void bind(WebSocketSession session, JsonNode bindParams) {
        if (gameOwnerUserId == null) {
            gameOwnerUserId = extractUserIdFromSession(session);
        }
        super.bind(session, bindParams);
    }

    @RpcMethod
    public synchronized void startGame(WebSocketSession session) {
        if (phase != XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN) {
            return;
        }
        final UUID userId = extractUserIdFromSession(session);
        if (!userId.equals(gameOwnerUserId)) {
            sendMessageToFe(
                    session,
                    XoGameErrorDto.builder().errorDescription("You don't have permissions to start game.").build()
            );
        } else {
            players = new ArrayList<>();
            final List<UUID> userIds = new ArrayList<>(
                    sessions.stream()
                            .map(OnlinegamesUtils::extractUserSessionData)
                            .map(Optional::get)
                            .map(UserSessionData::getUserId)
                            .filter(id -> !userId.equals(id))
                            .distinct()
                            .limit(MAX_NUMBER_OF_PLAYERS-1)
                            .collect(Collectors.toList())
            );
            userIds.add(userId);
            List<Character> possibleSymbols = new ArrayList<>(listOf('x','o'));
            final Random rnd = new Random();
            while (!userIds.isEmpty()) {
                players.add(createPlayer(
                        userIds.remove(rnd.nextInt(userIds.size())),
                        players.size(),
                        possibleSymbols.remove(0)
                ));
            }
            playerToMove = players.get(0);
            field = new Character[3][3];
            phase = XoGamePhase.IN_PROGRESS;
            broadcastGameState();
        }
    }

    @RpcMethod
    public void clickCell(WebSocketSession session, int x, int y) {
        executeOnBehalfOfPlayerVoid(session, player -> clickCell(player, x, y));
    }

    @RpcMethod
    public XoGameStateDto getCurrentState(WebSocketSession session) {
        return createViewOfCurrentState(
                XoPlayer.builder()
                        .gameOwner(extractUserIdFromSession(session).equals(gameOwnerUserId))
                        .build()
        );
    }

    private XoPlayer createPlayer(UUID userId, int playerId, Character playerSymbol) {
        return XoPlayer.builder()
                .userId(userId)
                .gameOwner(userId.equals(gameOwnerUserId))
                .playerId(playerId)
                .playerSymbol(playerSymbol)
                .feMessageSender(msg -> sendMessageToFe(userId, msg))
                .build();
    }

    private void sendMessageToFe(UUID userId, Object msg) {
        sessions.stream()
                .filter(session -> extractUserIdFromSession(session).equals(userId))
                .forEach(session -> sendMessageToFe(session, msg));
    }

    private <T> T executeOnBehalfOfPlayer(WebSocketSession session, Function<XoPlayer, T> executor) {
        final XoPlayer player = extractPlayerFromSession(session);
        if (player != null) {
            return executor.apply(player);
        } else {
            return null;
        }
    }

    private void executeOnBehalfOfPlayerVoid(WebSocketSession session, Consumer<XoPlayer> executor) {
        executeOnBehalfOfPlayer(session, player -> {
            executor.accept(player);
            return null;
        });
    }

    private XoPlayer extractPlayerFromSession(WebSocketSession session) {
        XoPlayer boundPlayer = (XoPlayer) session.getAttributes().get(PLAYER_STATE);
        if (boundPlayer == null && players != null) {
            UUID userId = extractUserIdFromSession(session);
            boundPlayer = players.stream()
                    .filter(player -> userId.equals(player.getUserId()))
                    .findFirst()
                    .get();
            session.getAttributes().put(PLAYER_STATE, boundPlayer);
        }
        return boundPlayer;
    }

    private UUID extractUserIdFromSession(WebSocketSession session) {
        return OnlinegamesUtils.extractUserSessionData(session).get().getUserId();
    }

    private XoGameStateDto createViewOfCurrentState(XoPlayer player) {
        if (phase == XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN) {
            return XoGameStateDto.builder()
                    .phase(phase)
                    .numberOfWaitingPlayers(getNumberOfWaitingPlayers())
                    .currentUserIsGameOwner(player.isGameOwner())
                    .build();
        } else {
            return XoGameStateDto.builder()
                    .phase(phase)
                    .currentUserIsGameOwner(player.isGameOwner())
                    .field(createFieldDto(field))
                    .currentPlayerId(player.getPlayerId())
                    .players(createPlayersDto(player, players))
                    .playerIdToMove(nullSafeGetter(playerToMove, XoPlayer::getPlayerId))
                    .winnerId(nullSafeGetter(winner, XoPlayer::getPlayerId))
                    .build();
        }
    }

    private Long getNumberOfWaitingPlayers() {
        return sessions.stream()
                .map(this::extractUserIdFromSession)
                .distinct()
                .count();
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

    private XoPlayer getNextPlayerToMove() {
        return players.get(playerToMove.getPlayerId()%players.size());
    }

    private void broadcastGameState() {
        players.forEach(player -> player.sendMessageToFe(createViewOfCurrentState(player)));
    }

    private List<XoPlayerDto> createPlayersDto(XoPlayer viewer, List<XoPlayer> players) {
        if (players == null) {
            return null;
        } else {
            return players.stream()
                    .map(xoPlayer -> XoPlayerDto.builder()
                            .gameOwner(viewer.ifGameOwner(() -> xoPlayer.isGameOwner()))
                            .playerId(xoPlayer.getPlayerId())
                            .symbol(xoPlayer.getPlayerSymbol())
                            .build()
                    )
                    .collect(Collectors.toList());
        }
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

    @Override
    protected Object getViewRepresentation() {
        return this.getClass().getSimpleName();
    }
}
