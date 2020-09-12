package org.igor.onlinegames.wordsgame.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.igor.onlinegames.common.GamePlayersCounts;
import org.igor.onlinegames.common.OnlinegamesUtils;
import org.igor.onlinegames.exceptions.OnlinegamesException;
import org.igor.onlinegames.model.GameState;
import org.igor.onlinegames.model.UserSessionData;
import org.igor.onlinegames.rpc.RpcMethod;
import org.igor.onlinegames.websocket.State;
import org.igor.onlinegames.wordsgame.dto.WordsGameErrorDto;
import org.igor.onlinegames.wordsgame.dto.WordsGameIncorrectPasscodeErrorDto;
import org.igor.onlinegames.wordsgame.dto.WordsGameNoAvailablePlacesErrorDto;
import org.igor.onlinegames.wordsgame.dto.WordsGamePasscodeIsRequiredErrorDto;
import org.igor.onlinegames.wordsgame.dto.WordsGamePhase;
import org.igor.onlinegames.wordsgame.dto.WordsGamePlayerNameIsOccupiedErrorDto;
import org.igor.onlinegames.wordsgame.dto.WordsGamePlayerNameWasSetMsgDto;
import org.igor.onlinegames.wordsgame.dto.WordsGameStateDto;
import org.igor.onlinegames.wordsgame.dto.WordsPlayerDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.igor.onlinegames.common.OnlinegamesUtils.nullSafeGetter;
import static org.igor.onlinegames.wordsgame.dto.WordsGamePhase.DISCARDED;
import static org.igor.onlinegames.wordsgame.dto.WordsGamePhase.ENTER_WORD;
import static org.igor.onlinegames.wordsgame.dto.WordsGamePhase.SELECT_WORD;
import static org.igor.onlinegames.wordsgame.dto.WordsGamePhase.WAITING_FOR_PLAYERS_TO_JOIN;

@Component("WordsGame")
@Scope("prototype")
public class WordsGameState extends State implements GameState {

    private static final String PLAYER_STATE = "playerState";
    public static final int MAX_NUMBER_OF_PLAYERS = 10;
    private static final String TITLE = "title";
    private static final String PASSCODE = "passcode";
    private static final String WORDS_TO_LEARN = "wordsToLearn";
    private static final String TIMER = "timer";
    private static final Pattern TIMER_VALUE_PATTERN_1 = Pattern.compile("^(\\d+)([sm])$");
    private static final Pattern TIMER_VALUE_PATTERN_2 = Pattern.compile("^(\\d+)m(\\d+)s$");
    private static final String PLAYER_NAME = "PLAYER_NAME";
    private static final Duration INACTIVITY_INTERVAL = Duration.of(30, ChronoUnit.MINUTES);

    @Value("${app.wordsgame.history-path}")
    private String historyPath;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    @Qualifier("wordsGamePlayersCounts")
    private GamePlayersCounts gamePlayersCounts;
    private Instant lastActionAt = Instant.now();

    private String title;
    private String passcode;
    private String wordsToLearnStr;
    private Set<UUID> userIdsEverConnected = new HashSet<>();
    private WordsGamePhase phase = WAITING_FOR_PLAYERS_TO_JOIN;
    private UUID gameOwnerUserId;
    private List<WordsPlayer> players;
    private Map<UUID,WordsPlayer> userIdToPlayer;
    private String timerStr;
    private Integer timerSeconds;
    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> timerHandle;
    private WordsPlayer playerToMove;

    @Override
    protected void init(JsonNode args) {
        wordsToLearnStr = args.get(WORDS_TO_LEARN).asText();
        timerStr = getNonEmptyTextFromParams(args, TIMER);
        timerSeconds = parseTimerValue(timerStr);
        title = getNonEmptyTextFromParams(args, TITLE);
        passcode = getNonEmptyTextFromParams(args, PASSCODE);
    }

    @Override
    public synchronized boolean bind(WebSocketSession session, JsonNode bindParams) {
        if (phase == WAITING_FOR_PLAYERS_TO_JOIN) {
            if (gameOwnerUserId == null) {
                gameOwnerUserId = extractUserIdFromSession(session);
            }
            if (getNumberOfWaitingPlayers() >= MAX_NUMBER_OF_PLAYERS) {
                sendMessageToFe(session, new WordsGameNoAvailablePlacesErrorDto());
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
            }
            sendMessageToFe(session, new WordsGamePlayerNameWasSetMsgDto(playerName));
            broadcastGameState();
        }
    }

    @RpcMethod
    public synchronized void discard(WebSocketSession session) {
        if (phase != WAITING_FOR_PLAYERS_TO_JOIN) {
            return;
        }
        final UUID userId = extractUserIdFromSession(session);
        if (!userId.equals(gameOwnerUserId)) {
            sendMessageToFe(session, new WordsGameErrorDto("You don't have permissions to discard this game."));
        } else {
            phase = DISCARDED;
            broadcastGameState();
        }
    }

    @RpcMethod
    public synchronized void startGame(WebSocketSession session) {
        if (phase != WAITING_FOR_PLAYERS_TO_JOIN) {
            return;
        }
        if (!extractUserIdFromSession(session).equals(gameOwnerUserId)) {
            sendMessageToFe(session, new WordsGameErrorDto("You don't have permissions to start this game."));
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
            userIds = gamePlayersCounts.getAndUpdateOrderOfPlayers(userIds);
            for (UUID userId : userIds) {
                players.add(createPlayer(
                        userId,
                        players.size(),
                        getPlayerNameFromSession(userId)
                ));
            }
            userIdToPlayer = players.stream().collect(Collectors.toMap(
                    WordsPlayer::getUserId,
                    Function.identity()
            ));
            playerToMove = players.get(0);
            phase = SELECT_WORD;
            if (timerSeconds != null) {
                scheduledExecutorService = Executors.newScheduledThreadPool(1);
                startTimerForCurrentPlayer();
            }
            broadcastGameState();
        }
    }

    @RpcMethod
    public synchronized void selectWord(WebSocketSession session) {
        if (phase == SELECT_WORD) {
            executeOnBehalfOfPlayer(session, player -> selectWord(player));
        }
    }

    protected synchronized void onTimer() {
        if (timerHandle != null) {

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
                sendMessageToFe(session, new WordsGameIncorrectPasscodeErrorDto());
            }
            return passcodeMatches;
        } else {
            sendMessageToFe(session, new WordsGamePasscodeIsRequiredErrorDto());
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
                sendMessageToFe(session, new WordsGamePlayerNameIsOccupiedErrorDto(playerName));
                return false;
            } else {
                return true;
            }
        }
    }

    private WordsPlayer sessionToPlayer(WebSocketSession session) {
        UUID userId = extractUserIdFromSession(session);
        if (userIdToPlayer == null || !userIdToPlayer.containsKey(userId)) {
            return sessionToMinimalPlayer(session);
        } else {
            return userIdToPlayer.get(userId);
        }
    }

    private WordsPlayer sessionToMinimalPlayer(WebSocketSession session) {
        return WordsPlayer.builder()
                .gameOwner(extractUserIdFromSession(session).equals(gameOwnerUserId))
                .name(extractPlayerNameFromSession(session))
                .build();
    }

    private WordsPlayer createPlayer(UUID userId, int playerId, String playerName) {
        return WordsPlayer.builder()
                .userId(userId)
                .gameOwner(userId.equals(gameOwnerUserId))
                .playerId(playerId)
                .name(playerName)
                .feMessageSender(msg -> sendMessageToFe(userId, msg))
                .build();
    }

    private void sendMessageToFe(UUID userId, Object msg) {
        sessions.stream()
                .filter(session -> extractUserIdFromSession(session).equals(userId))
                .forEach(session -> sendMessageToFe(session, msg));
    }

    private void executeOnBehalfOfPlayer(WebSocketSession session, Consumer<WordsPlayer> executor) {
        final WordsPlayer player = extractPlayerFromSession(session);
        if (player != null) {
            executor.accept(player);
        } else {
            throw new OnlinegamesException("player == null");
        }
    }

    private WordsPlayer extractPlayerFromSession(WebSocketSession session) {
        WordsPlayer boundPlayer = (WordsPlayer) session.getAttributes().get(PLAYER_STATE);
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

    private WordsGameStateDto createViewOfCurrentState(WordsPlayer player) {
        if (phase == WAITING_FOR_PLAYERS_TO_JOIN || phase == DISCARDED) {
            return WordsGameStateDto.builder()
                    .title(title)
                    .passcode(player.ifGameOwner(() -> passcode))
                    .phase(phase)
                    .numberOfWaitingPlayers(getNumberOfWaitingPlayers())
                    .namesOfWaitingPlayers(getConnectedPlayerNames(null))
                    .currentPlayerName(player.getName())
                    .currentUserIsGameOwner(player.isGameOwner())
                    .wordsToLearnStr(wordsToLearnStr)
                    .timerSeconds(getRemainingTimerDelay())
                    .build();
        } else {
            return WordsGameStateDto.builder()
                    .phase(phase)
                    .currentUserIsGameOwner(player.isGameOwner())
                    .wordsToLearnStr(wordsToLearnStr)
                    .timerSeconds(getRemainingTimerDelay())
                    .currentPlayerId(player.getPlayerId())
                    .players(createPlayersDto(player, players))
                    .playerIdToMove(nullSafeGetter(playerToMove, WordsPlayer::getPlayerId))
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

    private void selectWord(WordsPlayer player) {
        if (phase == SELECT_WORD) {
            if (playerToMove != player) {
                player.sendMessageToFe(new WordsGameErrorDto("It's not your turn."));
            }
        }
    }

    private WordsPlayer getNextPlayerToMove() {
        return players.get((playerToMove.getPlayerId()+1)%players.size());
    }

    private void broadcastGameState() {
        sessions.forEach(session -> sendMessageToFe(session, createViewOfCurrentState(sessionToPlayer(session))));
    }

    private List<WordsPlayerDto> createPlayersDto(WordsPlayer viewer, List<WordsPlayer> players) {
        if (players == null) {
            return null;
        } else {
            return players.stream()
                    .map(xoPlayer -> WordsPlayerDto.builder()
                            .gameOwner(viewer.ifGameOwner(() -> xoPlayer.isGameOwner()))
                            .playerId(xoPlayer.getPlayerId())
                            .name(xoPlayer.getName())
                            .build()
                    )
                    .collect(Collectors.toList());
        }
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

    private void startTimerForCurrentPlayer() {
        if (timerHandle != null) {
            timerHandle.cancel(true);
        }
        timerHandle = startTimerForPlayer(playerToMove);
    }

    private ScheduledFuture<?> startTimerForPlayer(WordsPlayer player) {
        return scheduledExecutorService.schedule(
                () -> {
                    if (playerToMove == player) {
                        onTimer();
                    }
                },
                timerSeconds + 1,
                TimeUnit.SECONDS
        );
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
    protected synchronized Object getViewRepresentation() {
        return this.getClass().getSimpleName() + "[scheduledExecutorService=" + scheduledExecutorService + "]";
    }

    @Override
    public synchronized boolean isWaitingForPlayersToJoin() {
        return phase == WAITING_FOR_PLAYERS_TO_JOIN;
    }

    @Override
    public synchronized boolean isInProgress() {
        return phase == SELECT_WORD || phase == ENTER_WORD;
    }

    @Override
    public synchronized String gameType() {
        return "WordsGame";
    }

    @Override
    public synchronized String gameDisplayType() {
        return "Words Game";
    }

    @Override
    public synchronized String getTitle() {
        return title;
    }

    @Override
    public synchronized boolean hasPasscode() {
        return passcode != null;
    }

    @Override
    public synchronized String getShortDescription() {
        return "W " + wordsToLearnStr.length() + (timerSeconds == null ? "" : " / T " + timerSeconds);
    }

    @Override
    public synchronized boolean isOwner(UserSessionData userData) {
        return userData.getUserId().equals(gameOwnerUserId);
    }

    @Override
    public synchronized boolean mayBeRemoved() {
        return Duration.between(lastActionAt, Instant.now()).compareTo(INACTIVITY_INTERVAL) > 0;
    }

    @Override
    public synchronized void preDestroy() {
        unbindAndCloseAllWebSockets();
        shutdownTimer();
    }
}
