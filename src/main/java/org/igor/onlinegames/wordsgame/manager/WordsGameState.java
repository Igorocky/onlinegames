package org.igor.onlinegames.wordsgame.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igor.onlinegames.common.GamePlayersCounts;
import org.igor.onlinegames.common.OnlinegamesUtils;
import org.igor.onlinegames.exceptions.OnlinegamesException;
import org.igor.onlinegames.model.GameState;
import org.igor.onlinegames.model.UserSessionData;
import org.igor.onlinegames.rpc.RpcMethod;
import org.igor.onlinegames.websocket.State;
import org.igor.onlinegames.wordsgame.dto.SelectedWordDto;
import org.igor.onlinegames.wordsgame.dto.UserInputDto;
import org.igor.onlinegames.wordsgame.dto.WordsGameErrorDto;
import org.igor.onlinegames.wordsgame.dto.WordsGameIncorrectPasscodeErrorDto;
import org.igor.onlinegames.wordsgame.dto.WordsGameNewTextWasSavedMsgDto;
import org.igor.onlinegames.wordsgame.dto.WordsGameNoAvailablePlacesErrorDto;
import org.igor.onlinegames.wordsgame.dto.WordsGamePasscodeIsRequiredErrorDto;
import org.igor.onlinegames.wordsgame.dto.WordsGamePhase;
import org.igor.onlinegames.wordsgame.dto.WordsGamePlayerNameIsOccupiedErrorDto;
import org.igor.onlinegames.wordsgame.dto.WordsGamePlayerNameWasSetMsgDto;
import org.igor.onlinegames.wordsgame.dto.WordsGameStateDto;
import org.igor.onlinegames.wordsgame.dto.WordsPlayerDto;
import org.igor.onlinegames.wordsgame.dto.WordsPlayerScoreDto;
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
import java.util.Collections;
import java.util.Comparator;
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
import java.util.stream.Stream;

import static org.igor.onlinegames.common.OnlinegamesUtils.nullSafeGetter;
import static org.igor.onlinegames.wordsgame.dto.WordsGamePhase.DISCARDED;
import static org.igor.onlinegames.wordsgame.dto.WordsGamePhase.ENTER_WORD;
import static org.igor.onlinegames.wordsgame.dto.WordsGamePhase.FINISHED;
import static org.igor.onlinegames.wordsgame.dto.WordsGamePhase.SELECT_WORD;
import static org.igor.onlinegames.wordsgame.dto.WordsGamePhase.WAITING_FOR_PLAYERS_TO_JOIN;

@Component("WordsGame")
@Scope("prototype")
public class WordsGameState extends State implements GameState {

    // TODO: 13.09.2020 timer
    // TODO: 13.09.2020 improve UI components layout

    private static final String PLAYER_STATE = "playerState";
    public static final int MAX_NUMBER_OF_PLAYERS = 10;
    private static final String TITLE = "title";
    private static final String PASSCODE = "passcode";
    private static final String TEXT_TO_LEARN = "textToLearn";
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
    private Set<UUID> userIdsEverConnected = new HashSet<>();
    private WordsGamePhase phase = WAITING_FOR_PLAYERS_TO_JOIN;
    private UUID gameOwnerUserId;
    private List<WordsPlayer> players;
    private Map<UUID,WordsPlayer> userIdToPlayer;
    private Map<Integer,WordsPlayer> playerIdToPlayer;
    private String timerStr;
    private Integer timerSeconds;
    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> timerHandle;
    private WordsPlayer playerToMove;
    private String textToLearn;
    private List<List<TextToken>> words;
    private SelectedWord prevSelectedWord;
    private SelectedWord selectedWord;

    @Override
    protected void init(JsonNode args) {
        textToLearn = getNonEmptyTextFromParams(args, TEXT_TO_LEARN);
        if (StringUtils.isEmpty(textToLearn)) {
            throw new OnlinegamesException("StringUtils.isEmpty(wordsToLearnStr)");
        }
        words = TextProcessing.splitOnParagraphs(textToLearn);
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
    public synchronized void end(WebSocketSession session) {
        if (phase != SELECT_WORD && phase != ENTER_WORD) {
            return;
        }
        final UUID userId = extractUserIdFromSession(session);
        if (!userId.equals(gameOwnerUserId)) {
            sendMessageToFe(session, new WordsGameErrorDto("You don't have permissions to end this game."));
        } else {
            phase = FINISHED;
            broadcastGameState();
        }
    }

    @RpcMethod
    public synchronized void setTextToLearn(WebSocketSession session, String newTextToLearn) {
        executeOnBehalfOfPlayer(session, player -> setTextToLearn(player, newTextToLearn));
    }

    private void setTextToLearn(WordsPlayer player, String newTextToLearn) {
        if (phase != DISCARDED && phase != ENTER_WORD && phase != FINISHED
                && player.isGameOwner()
                && StringUtils.isNoneBlank(newTextToLearn)) {
            textToLearn = newTextToLearn;
            words = TextProcessing.splitOnParagraphs(textToLearn);
            player.sendMessageToFe(new WordsGameNewTextWasSavedMsgDto());
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
            lastActionAt = Instant.now();
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
            playerIdToPlayer = players.stream().collect(Collectors.toMap(
                    WordsPlayer::getPlayerId,
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
    public synchronized void selectWord(WebSocketSession session, int paragraphIndex, int wordIndex, String text) {
        if (phase == SELECT_WORD) {
            executeOnBehalfOfPlayer(session, player -> selectWord(player, paragraphIndex, wordIndex, text));
        }
    }

    @RpcMethod
    public synchronized void enterWord(WebSocketSession session, String text) {
        if (phase == ENTER_WORD) {
            executeOnBehalfOfPlayer(session, player -> enterWord(player, text));
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
                .feMessageSender(msg -> sendMessageToFe(session, msg))
                .build();
    }

    private WordsPlayer createPlayer(UUID userId, int playerId, String playerName) {
        return WordsPlayer.builder()
                .userId(userId)
                .gameOwner(userId.equals(gameOwnerUserId))
                .playerId(playerId)
                .name(playerName)
                .score(new WordsPlayerScore())
                .feMessageSender(msg -> sendMessageToFe(userId, msg))
                .build();
    }

    private void sendMessageToFe(UUID userId, Object msg) {
        sessions.stream()
                .filter(session -> extractUserIdFromSession(session).equals(userId))
                .forEach(session -> sendMessageToFe(session, msg));
    }

    private void executeOnBehalfOfPlayer(WebSocketSession session, Consumer<WordsPlayer> executor) {
        WordsPlayer player = extractPlayerFromSession(session);
        if (player == null) {
            player = sessionToMinimalPlayer(session);
        }
        executor.accept(player);
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
                    .timerSeconds(getRemainingTimerDelay())
                    .textToLearn(textToLearn)
                    .words(words)
                    .build();
        } else {
            final Comparator<Pair<Double, WordsPlayerDto>> comparator = Comparator.comparing(Pair::getLeft);
            return WordsGameStateDto.builder()
                    .phase(phase)
                    .currentUserIsGameOwner(player.isGameOwner())
                    .timerSeconds(getRemainingTimerDelay())
                    .currentPlayerId(player.getPlayerId())
                    .players(createPlayersDto(player, players))
                    .playerIdToMove(nullSafeGetter(playerToMove, WordsPlayer::getPlayerId))
                    .textToLearn(textToLearn)
                    .words(words)
                    .selectedWord(createViewOfSelectedWord(player).getSelectedWord())
                    .finalScores(
                            phase != FINISHED ? null
                                    : createPlayersDto(player, players).stream()
                            .map(playerDto -> Pair.of(getPlayerOverallScore(playerDto), playerDto))
                            .sorted(comparator.reversed())
                            .map(Pair::getRight)
                            .collect(Collectors.toList())
                    )
                    .build();
        }
    }

    private SelectedWord getSelectedWordToShow() {
        if (phase == SELECT_WORD) {
            return prevSelectedWord;
        } else if (phase == ENTER_WORD) {
            return selectedWord;
        } else {
            return null;
        }
    }

    private WordsGameStateDto createViewOfSelectedWord(WordsPlayer player) {
        final SelectedWord selectedWord = getSelectedWordToShow();
        return WordsGameStateDto.builder()
                .phase(phase)
                .selectedWord(
                        selectedWord == null ? null
                                : SelectedWordDto.builder()
                                .paragraphIndex(selectedWord.getParagraphIndex())
                                .wordIndex(selectedWord.getWordIndex())
                                .expectedText(createExpectedTextForPlayer(player))
                                .userInputs(createUserInputsDtoForPlayer(player))
                                .build()
                )
                .build();
    }

    private List<String> stringToTableOfChars(String str) {
        final SelectedWord selectedWord = getSelectedWordToShow();
        if (selectedWord != null) {
            final int listLength = Stream.concat(
                    Stream.of(selectedWord.getText()),
                    selectedWord.getUserInputs().entrySet().stream()
                            .map(e -> e.getValue())
                            .map(UserInput::getText)
            )
                    .map(String::length)
                    .max(Integer::compareTo)
                    .get();
            final ArrayList<String> result = new ArrayList<>(listLength);
            for (int i = 0; i < listLength; i++) {
                if (i < str.length()) {
                    result.add(str.substring(i, i + 1));
                } else {
                    result.add("");
                }
            }
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    private boolean canShowSelectedWordInfoToPlayer(WordsPlayer player) {
        final SelectedWord selectedWord = getSelectedWordToShow();
        return phase == SELECT_WORD
                || selectedWord == null
                || player.getPlayerId() == null
                || playerToMove == player
                || !selectedWord.getUserInputs().containsKey(player.getPlayerId())
                || selectedWord.getUserInputs().get(player.getPlayerId()).getCorrect().isPresent();
    }

    private List<String> createExpectedTextForPlayer(WordsPlayer player) {
        if (canShowSelectedWordInfoToPlayer(player)) {
            return stringToTableOfChars(getSelectedWordToShow().getText());
        } else {
            return null;
        }
    }

    private Double getPlayerOverallScore(WordsPlayer player) {
        return getPlayerOverallScore(
                player.getScore().getNumOfCorrectWords(),
                player.getScore().getNumOfAllWords()
        );
    }

    private Double getPlayerOverallScore(WordsPlayerDto player) {
        return getPlayerOverallScore(
                player.getScore().getNumOfCorrectWords(),
                player.getScore().getNumOfAllWords()
        );
    }

    private Double getPlayerOverallScore(int numOfCorrectWords, int numOfAllWords) {
        if (numOfAllWords == 0) {
            return 0.0;
        } else {
            return numOfCorrectWords * 1.0 / numOfAllWords;
        }
    }

    private List<UserInputDto> createUserInputsDtoForPlayer(WordsPlayer player) {
        if (canShowSelectedWordInfoToPlayer(player)) {
            final SelectedWord selectedWord = getSelectedWordToShow();
            final Comparator<Pair<Double, Map.Entry<Integer, UserInput>>> comparator = Comparator.comparing(Pair::getLeft);
            return selectedWord.getUserInputs().entrySet().stream()
                    .map(entry -> Pair.of(playerIdToPlayer.get(entry.getKey()), entry))
                    .map(pair -> Pair.of(getPlayerOverallScore(pair.getLeft()), pair.getRight()))
                    .sorted(comparator.reversed())
                    .map(Pair::getRight)
                    .map(entry ->
                            UserInputDto.builder()
                                    .playerId(entry.getKey())
                                    .text(stringToTableOfChars(entry.getValue().getText()))
                                    .correct(entry.getValue().getCorrect().orElse(null))
                                    .confirmed(entry.getValue().isConfirmed())
                                    .build()
                    )
                    .sorted(Comparator.comparing(UserInputDto::getPlayerId))
                    .collect(Collectors.toList());
        } else {
            return null;
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

    private void enterWord(WordsPlayer player, String text) {
        if (phase == ENTER_WORD && playerToMove != player && player.getPlayerId() != null) {
            if (StringUtils.isBlank(text)) {
                player.sendMessageToFe(new WordsGameErrorDto(
                        "StringUtils.isBlank(text)"
                ));
                return;
            }
            final UserInput userInput = selectedWord.getUserInputs().get(player.getPlayerId());
            if (userInput.getCorrect().isPresent() && userInput.isConfirmed()) {
                player.sendMessageToFe(new WordsGameErrorDto(
                        "userInput.getCorrect().isPresent() && userInput.isConfirmed()"
                ));
                return;
            }
            lastActionAt = Instant.now();
            String userInputTextUpperCase = StringUtils.trimToEmpty(text).toUpperCase();
            if (!userInput.getCorrect().isPresent()) {
                userInput.setCorrect(Optional.of(selectedWord.getTextUpperCase().equals(userInputTextUpperCase)));
                userInput.setText(text.trim());
                player.getScore().setNumOfAllWords(player.getScore().getNumOfAllWords()+1);
                if (userInput.getCorrect().get()) {
                    userInput.setConfirmed(true);
                    player.getScore().setNumOfCorrectWords(player.getScore().getNumOfCorrectWords()+1);
                }
            } else if (!userInput.getCorrect().get() && !userInput.isConfirmed()) {
                userInput.setConfirmed(selectedWord.getTextUpperCase().equals(userInputTextUpperCase));
            }
            if (!selectedWord.getUserInputs().entrySet().stream()
                    .map(e -> e.getValue())
                    .map(UserInput::isConfirmed)
                    .filter(c -> !c)
                    .findAny()
                    .isPresent()) {
                prevSelectedWord = selectedWord;
                selectedWord = null;
                phase = SELECT_WORD;
                playerToMove = getNextPlayerToMove();
                broadcastPhaseChange();
            } else {
                broadcastSelectedWord();
            }
        }
    }

    private void selectWord(WordsPlayer player, int paragraphIndex, int wordIndex, String text) {
        if (phase == SELECT_WORD && playerToMove == player && selectedWord == null) {
            if (paragraphIndex < 0 || paragraphIndex >= words.size()) {
                player.sendMessageToFe(new WordsGameErrorDto(
                        "paragraphIndex < 0 || paragraphIndex >= words.size()"
                ));
                return;
            }
            List<TextToken> paragraph = words.get(paragraphIndex);
            if (wordIndex < 0 || wordIndex >= paragraph.size()) {
                player.sendMessageToFe(new WordsGameErrorDto(
                        "wordIndex < 0 || wordIndex >= paragraph.size()"
                ));
                return;
            }
            final TextToken selectedWordToken = paragraph.get(wordIndex);
            if (selectedWordToken.getActive() == null || !selectedWordToken.getActive()) {
                player.sendMessageToFe(new WordsGameErrorDto(
                        "Selected word is not active"
                ));
                return;
            }
            if (!selectedWordToken.getValue().equals(text)) {
                player.sendMessageToFe(new WordsGameErrorDto(
                        "!selectedWordToken.getValue().equals(text)"
                ));
                return;
            }
            lastActionAt = Instant.now();
            selectedWord = SelectedWord.builder()
                    .paragraphIndex(paragraphIndex)
                    .wordIndex(wordIndex)
                    .text(StringUtils.trimToEmpty(text))
                    .textUpperCase(StringUtils.trimToEmpty(text).toUpperCase())
                    .userInputs(
                            players.stream()
                                .collect(Collectors.toMap(
                                        p -> p.getPlayerId(),
                                        p -> UserInput.builder()
                                                .text("")
                                                .correct(Optional.empty())
                                                .confirmed(p == player)
                                                .build()
                                ))
                    )
                    .build();
            phase = ENTER_WORD;
            broadcastSelectedWord();
        }
    }

    private WordsPlayer getNextPlayerToMove() {
        return players.get((playerToMove.getPlayerId()+1)%players.size());
    }

    private void broadcastGameState() {
        sessions.forEach(session -> sendMessageToFe(session, createViewOfCurrentState(sessionToPlayer(session))));
    }

    private void broadcastSelectedWord() {
        sessions.forEach(session -> sendMessageToFe(session, createViewOfSelectedWord(sessionToPlayer(session))));
    }

    private void broadcastPhaseChange() {
        sessions.forEach(session -> sendMessageToFe(
                session,
                createViewOfCurrentState(sessionToPlayer(session))
                .withTextToLearn(null)
                .withWords(null)
        ));
    }

    private List<WordsPlayerDto> createPlayersDto(WordsPlayer viewer, List<WordsPlayer> players) {
        if (players == null) {
            return null;
        } else {
            return players.stream()
                    .map(player -> WordsPlayerDto.builder()
                            .gameOwner(viewer.ifGameOwner(() -> player.isGameOwner()))
                            .playerId(player.getPlayerId())
                            .name(player.getName())
                            .score(
                                    player.getScore() == null ? null
                                            : WordsPlayerScoreDto.builder()
                                                .numOfAllWords(player.getScore().getNumOfAllWords())
                                                .numOfCorrectWords(player.getScore().getNumOfCorrectWords())
                                                .build()
                            )
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
        return "W " + words.stream()
                .map(List::stream)
                .map(stream -> stream.filter(word -> word.getActive() != null && word.getActive()).count())
                .reduce(0l, Long::sum)
                + (timerSeconds == null ? "" : " / T " + timerSeconds);
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
