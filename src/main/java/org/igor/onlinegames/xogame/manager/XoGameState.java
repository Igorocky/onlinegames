package org.igor.onlinegames.xogame.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
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
import org.igor.onlinegames.xogame.dto.XoGamePlayerNameIsOccupiedErrorDto;
import org.igor.onlinegames.xogame.dto.XoGamePlayerNameWasSetMsgDto;
import org.igor.onlinegames.xogame.dto.XoGameStateDto;
import org.igor.onlinegames.xogame.dto.XoPlayerDto;
import org.igor.onlinegames.xogame.dto.history.XoGameMoveDto;
import org.igor.onlinegames.xogame.dto.history.XoGamePlayerInfoDto;
import org.igor.onlinegames.xogame.dto.history.XoGameRecordDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.FileWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.igor.onlinegames.common.OnlinegamesUtils.listOf;
import static org.igor.onlinegames.common.OnlinegamesUtils.nullSafeGetter;

@Component("XoGame")
@Scope("prototype")
public class XoGameState extends State implements GameState {

    @Value("${app.xogame.history-path}")
    private String historyPath;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private XoGamePlayersCounts xoGamePlayersCounts;

    private static final String PLAYER_STATE = "playerState";
    private static final List<Character> POSSIBLE_SYMBOLS = listOf('x','o','s','t','a');
    public static final int MAX_NUMBER_OF_PLAYERS = POSSIBLE_SYMBOLS.size();
    private static final String TITLE = "title";
    private static final String PASSCODE = "passcode";
    private static final String FIELD_SIZE = "fieldSize";
    private static final String GOAL = "goal";
    private static final String TIMER = "timer";
    private static final Pattern TIMER_VALUE_PATTERN_1 = Pattern.compile("^(\\d+)([sm])$");
    private static final Pattern TIMER_VALUE_PATTERN_2 = Pattern.compile("^(\\d+)m(\\d+)s$");
    private static final String PLAYER_NAME = "PLAYER_NAME";

    private String title;
    private String passcode;
    private Set<UUID> userIdsEverConnected = new HashSet<>();
    private XoGamePhase phase = XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN;
    private UUID gameOwnerUserId;
    private List<XoPlayer> players;
    private Map<UUID,XoPlayer> userIdToPlayer;
    private int fieldSize;
    private int goal;
    private String timerStr;
    private Integer timerSeconds;
    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> timerHandle;
    private Character[][] field;
    private List<Integer> lastCell;
    private XoPlayer playerToMove;
    private XoPlayer winner;
    private List<List<Integer>> winnerPath;

    private XoGameRecordDto history;
    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd");
    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd__HH_mm_ss");

    @Override
    protected void init(JsonNode args) {
        fieldSize = args.get(FIELD_SIZE).asInt();
        field = new Character[fieldSize][fieldSize];
        goal = args.get(GOAL).asInt();
        if (fieldSize < goal) {
            throw new OnlinegamesException("fieldSize < goal");
        }

        timerStr = getNonEmptyTextFromParams(args, TIMER);
        timerSeconds = parseTimerValue(timerStr);
        title = getNonEmptyTextFromParams(args, TITLE);
        passcode = getNonEmptyTextFromParams(args, PASSCODE);
    }

    @Override
    public synchronized boolean bind(WebSocketSession session, JsonNode bindParams) {
        if (phase == XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN) {
            if (gameOwnerUserId == null) {
                gameOwnerUserId = extractUserIdFromSession(session);
            }
            if (getNumberOfWaitingPlayers() >= MAX_NUMBER_OF_PLAYERS) {
                sendMessageToFe(session, new XoGameNoAvailablePlacesErrorDto());
                return false;
            } else if (!checkPasscode(session, bindParams)) {
                return false;
            } else {
                String playerName = sanitizePlayerName(getNonEmptyTextFromParams(bindParams, "playerName"));
                if (!checkPlayerNameIsUnique(session, playerName)) {
                    return false;
                } else {
                    savePlayerNameToSession(session, playerName);
                }
                if (super.bind(session, bindParams)) {
                    userIdsEverConnected.add(extractUserIdFromSession(session));
                    broadcastGameState();
                    return true;
                } else {
                    return false;
                }
            }
        } else if (super.bind(session, bindParams)) {
            broadcastGameState();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized void unbind(WebSocketSession session) {
        super.unbind(session);
        broadcastGameState();
    }

    @RpcMethod
    public synchronized void setPlayerName(WebSocketSession session, String playerName) {
        playerName = sanitizePlayerName(playerName);
        if (checkPlayerNameIsUnique(session, playerName)) {
            savePlayerNameToSession(session, playerName);
            if (players != null) {
                UUID userId = extractUserIdFromSession(session);
                final String finalPlayerName = playerName;
                players.stream()
                        .filter(player -> player.getUserId().equals(userId))
                        .forEach(player -> player.setName(finalPlayerName));
                history.getPlayers().stream()
                        .filter(player -> player.getUserId().equals(userId))
                        .forEach(player -> player.setPlayerName(finalPlayerName));
            }
            sendMessageToFe(session, new XoGamePlayerNameWasSetMsgDto(playerName));
            broadcastGameState();
        }
    }

    @RpcMethod
    public synchronized void discard(WebSocketSession session) {
        if (phase != XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN) {
            return;
        }
        final UUID userId = extractUserIdFromSession(session);
        if (!userId.equals(gameOwnerUserId)) {
            sendMessageToFe(session, new XoGameErrorDto("You don't have permissions to discard game."));
        } else {
            phase = XoGamePhase.DISCARDED;
            broadcastGameState();
        }
    }

    @RpcMethod
    public synchronized void startGame(WebSocketSession session) {
        if (phase != XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN) {
            return;
        }
        if (!extractUserIdFromSession(session).equals(gameOwnerUserId)) {
            sendMessageToFe(session, new XoGameErrorDto("You don't have permissions to start game."));
        } else {
            players = new ArrayList<>();
            List<UUID> userIds = new ArrayList<>(
                    sessions.stream()
                            .map(this::extractUserIdFromSession)
                            .filter(id -> !gameOwnerUserId.equals(id))
                            .distinct()
                            .limit(MAX_NUMBER_OF_PLAYERS-1)
                            .collect(Collectors.toList())
            );
            userIds.add(gameOwnerUserId);
            userIds = xoGamePlayersCounts.getAndUpdateOrderOfPlayers(userIds);
            List<Character> possibleSymbols = new ArrayList<>(POSSIBLE_SYMBOLS);
            for (UUID userId : userIds) {
                players.add(createPlayer(
                        userId,
                        players.size(),
                        possibleSymbols.remove(0),
                        getPlayerNameFromSession(userId)
                ));
            }
            userIdToPlayer = players.stream().collect(Collectors.toMap(
                    XoPlayer::getUserId,
                    Function.identity()
            ));
            playerToMove = players.get(0);
            phase = XoGamePhase.IN_PROGRESS;
            if (timerSeconds != null) {
                scheduledExecutorService = Executors.newScheduledThreadPool(1);
                startTimerForCurrentPlayer();
            }
            history = XoGameRecordDto.builder()
                    .gameId(stateId)
                    .startedAt(Instant.now())
                    .fieldSize(fieldSize)
                    .goal(goal)
                    .secondsPerMove(timerSeconds)
                    .players(
                            players.stream()
                            .map(
                                    playerState -> XoGamePlayerInfoDto.builder()
                                            .userId(playerState.getUserId())
                                            .playerId(playerState.getPlayerId())
                                            .playerName(playerState.getName())
                                            .build()
                            ).collect(Collectors.toList())
                    )
                    .moves(new LinkedList<>())
                    .build();
            broadcastGameState();
        }
    }

    @RpcMethod
    public synchronized void clickCell(WebSocketSession session, int x, int y) {
        if (phase == XoGamePhase.IN_PROGRESS) {
            executeOnBehalfOfPlayer(session, player -> clickCell(player, x, y));
        }
    }

    protected static String sanitizePlayerName(String playerName) {
        final String trimmed = StringUtils.trimToNull(playerName);
        if (trimmed != null) {
            return trimmed.replaceAll("\\s", "_");
        } else {
            return null;
        }
    }

    private boolean checkPasscode(WebSocketSession session, JsonNode bindParams) {
        final UUID userId = extractUserIdFromSession(session);
        if (passcode == null || userId.equals(gameOwnerUserId) || userIdsEverConnected.contains(userId)) {
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

    private Optional<JsonNode> getFromParams(JsonNode params, String attrName) {
        if (params != null) {
            return Optional.ofNullable(params.get(attrName));
        } else {
            return Optional.empty();
        }
    }

    private String getNonEmptyTextFromParams(JsonNode params, String attrName) {
        return getFromParams(params, attrName)
                .map(JsonNode::asText)
                .map(StringUtils::trimToNull)
                .orElse(null);
    }

    private List<String> getConnectedPlayerNames(WebSocketSession exceptSession) {
        final ArrayList<String> result = new ArrayList<>();
        for (WebSocketSession session : sessions) {
            if (session != exceptSession) {
                final String playerName = extractPlayerNameFromSession(session);
                if (playerName != null && !result.contains(playerName)) {
                    result.add(playerName);
                }
            }
        }
        return result;
    }

    private boolean checkPlayerNameIsUnique(WebSocketSession session, String playerName) {
        if (playerName == null) {
            return true;
        } else {
            if (getConnectedPlayerNames(session).contains(playerName)) {
                sendMessageToFe(session, new XoGamePlayerNameIsOccupiedErrorDto(playerName));
                return false;
            } else {
                return true;
            }
        }
    }

    private XoPlayer sessionToPlayer(WebSocketSession session) {
        UUID userId = extractUserIdFromSession(session);
        if (userIdToPlayer == null || !userIdToPlayer.containsKey(userId)) {
            return sessionToMinimalPlayer(session);
        } else {
            return userIdToPlayer.get(userId);
        }
    }

    private XoPlayer sessionToMinimalPlayer(WebSocketSession session) {
        return XoPlayer.builder()
                .gameOwner(extractUserIdFromSession(session).equals(gameOwnerUserId))
                .name(extractPlayerNameFromSession(session))
                .build();
    }

    private XoPlayer createPlayer(UUID userId, int playerId, Character playerSymbol, String playerName) {
        return XoPlayer.builder()
                .userId(userId)
                .gameOwner(userId.equals(gameOwnerUserId))
                .playerId(playerId)
                .playerSymbol(playerSymbol)
                .name(playerName)
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

    private String extractPlayerNameFromSession(WebSocketSession session) {
        return StringUtils.trimToNull((String) session.getAttributes().get(PLAYER_NAME));
    }

    private void savePlayerNameToSession(WebSocketSession session, String playerName) {
        playerName = StringUtils.trimToNull(playerName);
        if (playerName != null) {
            session.getAttributes().put(PLAYER_NAME, playerName);
        } else {
            session.getAttributes().remove(PLAYER_NAME);
        }
    }

    private XoGameStateDto createViewOfCurrentState(XoPlayer player) {
        if (phase == XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN || phase == XoGamePhase.DISCARDED) {
            return XoGameStateDto.builder()
                    .title(title)
                    .passcode(player.ifGameOwner(() -> passcode))
                    .phase(phase)
                    .numberOfWaitingPlayers(getNumberOfWaitingPlayers())
                    .namesOfWaitingPlayers(getConnectedPlayerNames(null))
                    .currentPlayerName(player.getName())
                    .currentUserIsGameOwner(player.isGameOwner())
                    .fieldSize(fieldSize)
                    .goal(goal)
                    .timerSeconds(getRemainingTimerDelay())
                    .field(createFieldDto(field))
                    .build();
        } else {
            return XoGameStateDto.builder()
                    .phase(phase)
                    .currentUserIsGameOwner(player.isGameOwner())
                    .fieldSize(fieldSize)
                    .goal(goal)
                    .timerSeconds(getRemainingTimerDelay())
                    .field(createFieldDto(field))
                    .lastCell(lastCell)
                    .currentPlayerId(player.getPlayerId())
                    .players(createPlayersDto(player, players))
                    .playerIdToMove(nullSafeGetter(playerToMove, XoPlayer::getPlayerId))
                    .winnerId(nullSafeGetter(winner, XoPlayer::getPlayerId))
                    .winnerPath(winnerPath)
                    .build();
        }
    }

    private Integer getRemainingTimerDelay() {
        long timerHandleDelay = timerHandle != null ? timerHandle.getDelay(TimeUnit.SECONDS) : 0;
        if (timerHandleDelay > 0) {
            return Math.toIntExact(timerHandleDelay);
        } else {
            return timerSeconds;
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
                player.sendMessageToFe(new XoGameErrorDto("Incorrect coordinates: x = " + x + ", y = " + y + "."));
            } else if (playerToMove != player) {
                player.sendMessageToFe(new XoGameErrorDto("It's not your turn."));
            } else if (field[x][y] != null) {
                player.sendMessageToFe(new XoGameErrorDto("The cell you clicked is not empty."));
            } else {
                if (timerHandle != null) {
                    timerHandle.cancel(true);
                    timerHandle = null;
                }
                history.getMoves().add(
                        XoGameMoveDto.builder()
                                .moveNumber(history.getMoves().size()+1)
                                .playerId(player.getPlayerId())
                                .time(Instant.now())
                                .x(x)
                                .y(y)
                                .build()
                );
                field[x][y] = player.getPlayerSymbol();
                lastCell = listOf(x,y);

                winnerPath = findPath(true);
                if (winnerPath != null) {
                    history.setWinnerPath(winnerPath);
                    playerToMove = null;
                    Character winnerSymbol = field[winnerPath.get(0).get(0)][winnerPath.get(0).get(1)];
                    winner = players.stream()
                            .filter(xoPlayer -> winnerSymbol.equals(xoPlayer.getPlayerSymbol()))
                            .findFirst()
                            .get();
                    history.setWinnerId(winner.getPlayerId());
                    phase = XoGamePhase.FINISHED;
                } else if (isDraw()) {
                    playerToMove = null;
                    history.setDraw(true);
                    phase = XoGamePhase.FINISHED;
                } else {
                    setNextPlayerToMove();
                    if (timerSeconds != null) {
                        startTimerForCurrentPlayer();
                    }
                }
                broadcastGameState();
            }
            if (phase == XoGamePhase.FINISHED) {
                shutdownTimer();
                saveHistory();
            }
        }
    }

    public static String getGameHistoryDirPath(String historyPath, Instant startedAt) {
        return historyPath + "/" + DATE_FORMATTER.format(startedAt.atZone(ZoneOffset.UTC));
    }

    protected static String getGameHistoryFilePath(String historyPath, Instant startedAt, UUID gameId) {
        return getGameHistoryDirPath(historyPath, startedAt)
                + "/xogame-" + DATE_TIME_FORMATTER.format(startedAt.atZone(ZoneOffset.UTC))
                + "-" + gameId + ".json";
    }

    @SneakyThrows
    private void saveHistory() {
        history.getPlayers().stream()
                .filter(player -> player.getPlayerName() == null)
                .forEach(player -> player.setPlayerName("Incognito"));
        final File file = new File(getGameHistoryFilePath(historyPath, history.getStartedAt(), history.getGameId()));
        file.getParentFile().mkdirs();
        try (final FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(mapper.writeValueAsString(history));
        }
    }

    private XoPlayer getNextPlayerToMove() {
        return players.get((playerToMove.getPlayerId()+1)%players.size());
    }

    private void broadcastGameState() {
        sessions.forEach(session -> sendMessageToFe(session, createViewOfCurrentState(sessionToPlayer(session))));
    }

    private List<XoPlayerDto> createPlayersDto(XoPlayer viewer, List<XoPlayer> players) {
        if (players == null) {
            return null;
        } else {
            return players.stream()
                    .map(xoPlayer -> XoPlayerDto.builder()
                            .gameOwner(viewer.ifGameOwner(() -> xoPlayer.isGameOwner()))
                            .playerId(xoPlayer.getPlayerId())
                            .name(xoPlayer.getName())
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

    private int countPathLength(BiFunction<Integer, Integer, Boolean> cellBelongsToPath, int startX, int startY, int dx, int dy) {
        final int[] length = {0};
        iterateCells(startX, startY, dx, dy, (x,y) -> {
            final boolean doContinue = cellBelongsToPath.apply(x,y);
            if (doContinue) {
                length[0]++;
            }
            return doContinue;
        });
        return length[0];
    }

    private List<List<Integer>> createPath(BiFunction<Integer, Integer, Boolean> cellBelongsToPath, int startX, int startY, int dx, int dy) {
        if (countPathLength(cellBelongsToPath, startX, startY, dx, dy) < goal) {
            return null;
        } else {
            final ArrayList<List<Integer>> path = new ArrayList<>();
            iterateCells(startX, startY, dx, dy, (x,y) -> {
                final boolean doContinue = cellBelongsToPath.apply(x,y);
                if (doContinue) {
                    path.add(listOf(x,y));
                }
                return doContinue;
            });
            return path;
        }
    }

    private List<List<Integer>> findPath(boolean forWinner) {
        for (int x = 0; x < fieldSize; x++) {
            for (int y = 0; y < fieldSize; y++) {
                for (int dx = -1; dx < 2; dx++) {
                    for (int dy = -1; dy < 2; dy++) {
                        Character symbol = field[x][y];
                        BiFunction<Integer, Integer, Boolean> contFunc = null;
                        if (forWinner) {
                            if (symbol != null) {
                                contFunc = (xx,yy) -> symbol.equals(field[xx][yy]);
                            }
                        } else {
                            if (symbol != null) {
                                contFunc = (xx,yy) -> symbol.equals(field[xx][yy]) || null == field[xx][yy];
                            } else {
                                final Character[] firstFound = {null};
                                contFunc = (xx,yy) -> {
                                    final Character currSymb = field[xx][yy];
                                    if (firstFound[0] == null && currSymb != null) {
                                        firstFound[0] = currSymb;
                                    }
                                    return null == currSymb || Objects.equals(firstFound[0], currSymb);
                                };
                            }
                        }
                        if (contFunc != null) {
                            List<List<Integer>> path = createPath(contFunc, x, y, dx, dy);
                            if (path != null) {
                                return path;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isDraw() {
        return CollectionUtils.isEmpty(findPath(false));
    }

    private Integer parseTimerValue(String timerStr) {
        if (timerStr == null) {
            return null;
        } else {
            final Matcher matcher1 = TIMER_VALUE_PATTERN_1.matcher(timerStr);
            if (matcher1.matches()) {
                if ("s".equals(matcher1.group(2))) {
                    return Integer.parseInt(matcher1.group(1));
                } else {
                    return Integer.parseInt(matcher1.group(1))*60;
                }
            } else {
                final Matcher matcher2 = TIMER_VALUE_PATTERN_2.matcher(timerStr);
                if (matcher2.matches()) {
                    return Integer.parseInt(matcher2.group(1))*60 + Integer.parseInt(matcher2.group(2));
                } else {
                    return null;
                }
            }
        }
    }

    private synchronized void startTimerForCurrentPlayer() {
        if (timerHandle != null) {
            timerHandle.cancel(true);
        }
        timerHandle = startTimerForPlayer(playerToMove);
    }

    private synchronized ScheduledFuture<?> startTimerForPlayer(XoPlayer player) {
        return scheduledExecutorService.schedule(
                () -> {
                    if (playerToMove == player) {
                        setNextPlayerToMove();
                        broadcastGameState();
                        startTimerForCurrentPlayer();
                    }
                },
                timerSeconds + 1,
                TimeUnit.SECONDS
        );
    }

    private synchronized void setNextPlayerToMove() {
        playerToMove = getNextPlayerToMove();
    }

    private void shutdownTimer() {
        if (timerHandle != null) {
            timerHandle.cancel(true);
            timerHandle = null;
        }
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
            scheduledExecutorService = null;
        }
    }

    private String getPlayerNameFromSession(UUID userId) {
        return sessions.stream()
                .filter(session -> userId.equals(extractUserIdFromSession(session)))
                .map(this::extractPlayerNameFromSession)
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    @Override
    protected Object getViewRepresentation() {
        return this.getClass().getSimpleName() + "[scheduledExecutorService=" + scheduledExecutorService + "]";
    }

    @Override
    public boolean isWaitingForPlayersToJoin() {
        return phase == XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN;
    }

    @Override
    public boolean isInProgress() {
        return phase == XoGamePhase.IN_PROGRESS;
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

    @Override
    public String getShortDescription() {
        return "F " + fieldSize + " / G " + goal + (timerSeconds == null ? "" : " / T " + timerSeconds);
    }

    @Override
    public boolean isOwner(UserSessionData userData) {
        return userData.getUserId().equals(gameOwnerUserId);
    }
}
