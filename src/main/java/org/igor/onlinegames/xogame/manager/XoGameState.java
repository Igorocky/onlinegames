package org.igor.onlinegames.xogame.manager;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.igor.onlinegames.common.OnlinegamesUtils;
import org.igor.onlinegames.exceptions.OnlinegamesException;
import org.igor.onlinegames.model.GameState;
import org.igor.onlinegames.model.UserSessionData;
import org.igor.onlinegames.rpc.RpcMethod;
import org.igor.onlinegames.websocket.State;
import org.igor.onlinegames.xogame.dto.XoCellDto;
import org.igor.onlinegames.xogame.dto.XoGameErrorDto;
import org.igor.onlinegames.xogame.dto.XoGameIncorrectPasscodeErrorDto;
import org.igor.onlinegames.xogame.dto.XoGameNoAvailablePlacesErrorDto;
import org.igor.onlinegames.xogame.dto.XoGamePasscodeIsRequiredErrorDto;
import org.igor.onlinegames.xogame.dto.XoGamePhase;
import org.igor.onlinegames.xogame.dto.XoGameStateDto;
import org.igor.onlinegames.xogame.dto.XoPlayerDto;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.igor.onlinegames.common.OnlinegamesUtils.listOf;
import static org.igor.onlinegames.common.OnlinegamesUtils.nullSafeGetter;

@Component("XoGame")
@Scope("prototype")
public class XoGameState extends State implements GameState {
    private static final String PLAYER_STATE = "playerState";
    private static final List<Character> POSSIBLE_SYMBOLS = listOf('x','o','s','t','a');
    public static final int MAX_NUMBER_OF_PLAYERS = POSSIBLE_SYMBOLS.size();
    private static final String TITLE = "title";
    private static final String PASSCODE = "passcode";
    private static final String FIELD_SIZE = "fieldSize";
    private static final String GOAL = "goal";

    private String title;
    private String passcode;
    private Set<UUID> userIdsEverConnected = new HashSet<>();
    private XoGamePhase phase = XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN;
    private UUID gameOwnerUserId;
    private List<XoPlayer> players;
    private int fieldSize;
    private int goal;
    private Character[][] field;
    private XoPlayer playerToMove;
    private XoPlayer winner;
    private List<List<Integer>> winnerPath;

    @Override
    protected void init(JsonNode args) {
        fieldSize = args.get(FIELD_SIZE).asInt();
        field = new Character[fieldSize][fieldSize];
        goal = args.get(GOAL).asInt();
        if (fieldSize < goal) {
            throw new OnlinegamesException("fieldSize < goal");
        }

        if (args.has(TITLE)) {
            title = StringUtils.trimToNull(args.get(TITLE).asText(null));
        }
        if (args.has(PASSCODE)) {
            passcode = StringUtils.trimToNull(args.get(PASSCODE).asText(null));
        }
    }

    @Override
    public synchronized boolean bind(WebSocketSession session, JsonNode bindParams) {
        if (gameOwnerUserId == null) {
            gameOwnerUserId = extractUserIdFromSession(session);
        }
        if (getNumberOfWaitingPlayers() >= MAX_NUMBER_OF_PLAYERS) {
            sendMessageToFe(session, new XoGameNoAvailablePlacesErrorDto());
            return false;
        } else if (!checkPasscode(session, bindParams)) {
            return false;
        } else {
            if (super.bind(session, bindParams)) {
                userIdsEverConnected.add(extractUserIdFromSession(session));
                broadcastGameState();
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public synchronized void unbind(WebSocketSession session) {
        super.unbind(session);
        broadcastGameState();
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
            List<Character> possibleSymbols = new ArrayList<>(POSSIBLE_SYMBOLS);
            final Random rnd = new Random();
            while (!userIds.isEmpty()) {
                players.add(createPlayer(
                        userIds.remove(rnd.nextInt(userIds.size())),
                        players.size(),
                        possibleSymbols.remove(0)
                ));
            }
            playerToMove = players.get(0);
            phase = XoGamePhase.IN_PROGRESS;
            broadcastGameState();
        }
    }

    @RpcMethod
    public void clickCell(WebSocketSession session, int x, int y) {
        executeOnBehalfOfPlayer(session, player -> clickCell(player, x, y));
    }

    private boolean checkPasscode(WebSocketSession session, JsonNode bindParams) {
        if (passcode == null || sessions.isEmpty() || userIdsEverConnected.contains(extractUserIdFromSession(session))) {
            return true;
        } else if (bindParams != null && bindParams.has(PASSCODE)) {
            String userProvidedPasscode = StringUtils.trimToNull(bindParams.get(PASSCODE).asText(null));
            final boolean passcodeMatches = passcode.equals(userProvidedPasscode);
            if (!passcodeMatches) {
                sendMessageToFe(session, new XoGameIncorrectPasscodeErrorDto());
            }
            return passcodeMatches;
        } else {
            sendMessageToFe(session, new XoGamePasscodeIsRequiredErrorDto());
            return false;
        }
    }

    private XoPlayer sessionToMinimalPlayer(WebSocketSession session) {
        return XoPlayer.builder()
                .gameOwner(extractUserIdFromSession(session).equals(gameOwnerUserId))
                .build();
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

    private void executeOnBehalfOfPlayer(WebSocketSession session, Consumer<XoPlayer> executor) {
        final XoPlayer player = extractPlayerFromSession(session);
        if (player != null) {
            executor.accept(player);
        } else {
            throw new OnlinegamesException("player == null");
        }
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
                    .title(title)
                    .passcode(player.ifGameOwner(() -> passcode))
                    .phase(phase)
                    .numberOfWaitingPlayers(getNumberOfWaitingPlayers())
                    .currentUserIsGameOwner(player.isGameOwner())
                    .fieldSize(fieldSize)
                    .goal(goal)
                    .field(createFieldDto(field))
                    .build();
        } else {
            return XoGameStateDto.builder()
                    .phase(phase)
                    .currentUserIsGameOwner(player.isGameOwner())
                    .fieldSize(fieldSize)
                    .goal(goal)
                    .field(createFieldDto(field))
                    .currentPlayerId(player.getPlayerId())
                    .players(createPlayersDto(player, players))
                    .playerIdToMove(nullSafeGetter(playerToMove, XoPlayer::getPlayerId))
                    .winnerId(nullSafeGetter(winner, XoPlayer::getPlayerId))
                    .winnerPath(winnerPath)
                    .build();
        }
    }

    private Long getNumberOfWaitingPlayers() {
        return sessions.stream()
                .map(this::extractUserIdFromSession)
                .distinct()
                .count();
    }

    private void clickCell(XoPlayer player, int x, int y) {
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

                winnerPath = findWinnerPath();
                if (winnerPath != null) {
                    playerToMove = null;
                    Character winnerSymbol = field[winnerPath.get(0).get(0)][winnerPath.get(0).get(1)];
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
        }
    }

    private XoPlayer getNextPlayerToMove() {
        return players.get((playerToMove.getPlayerId()+1)%players.size());
    }

    private void broadcastGameState() {
        if (phase == XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN) {
            sessions.forEach(session -> sendMessageToFe(session, createViewOfCurrentState(sessionToMinimalPlayer(session))));
        } else {
            players.forEach(player -> player.sendMessageToFe(createViewOfCurrentState(player)));
        }
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

    private void iterateCells(int x, int y, int dx, int dy, BiFunction<Integer, Integer, Boolean> coordsConsumer) {
        if (dx == 0 && dy == 0) {
            return;
        } else {
            int maxX = fieldSize-1;
            int maxY = fieldSize-1;
            while (0<=x && x<=maxX && 0<=y && y<=maxY && coordsConsumer.apply(x,y)) {
                x+=dx;
                y+=dy;
            }
        }
    }

    private int countPathLength(int startX, int startY, int dx, int dy) {
        final Character symbol = field[startX][startY];
        if (symbol == null) {
            return 0;
        } else {
            final int[] length = {0};
            iterateCells(startX, startY, dx, dy, (x,y) -> {
                final boolean doContinue = symbol.equals(field[x][y]);
                if (doContinue) {
                    length[0]++;
                }
                return doContinue;
            });
            return length[0];
        }
    }

    private List<List<Integer>> createWinnerPath(int startX, int startY, int dx, int dy) {
        final Character symbol = field[startX][startY];
        if (symbol == null || countPathLength(startX, startY, dx, dy) < goal) {
            return null;
        } else {
            final ArrayList<List<Integer>> path = new ArrayList<>();
            iterateCells(startX, startY, dx, dy, (x,y) -> {
                final boolean doContinue = symbol.equals(field[x][y]);
                if (doContinue) {
                    path.add(listOf(x,y));
                }
                return doContinue;
            });
            return path;
        }
    }

    private List<List<Integer>> findWinnerPath() {
        for (int x = 0; x < fieldSize; x++) {
            for (int y = 0; y < fieldSize; y++) {
                for (int dx = -1; dx < 2; dx++) {
                    for (int dy = -1; dy < 2; dy++) {
                        List<List<Integer>> winnerPath = createWinnerPath(x, y, dx, dy);
                        if (winnerPath != null) {
                            return winnerPath;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isDraw() {
        return !Arrays.asList(field).stream().flatMap(r -> Arrays.asList(r).stream()).anyMatch(Objects::isNull);
    }

    @Override
    protected Object getViewRepresentation() {
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean isWaitingForPlayersToJoin() {
        return phase == XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN;
    }

    @Override
    public String gameType() {
        return "XoGame";
    }

    @Override
    public String gameDisplayType() {
        return "XO Game";
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public boolean hasPasscode() {
        return passcode != null;
    }
}
