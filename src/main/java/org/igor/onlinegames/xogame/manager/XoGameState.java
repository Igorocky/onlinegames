package org.igor.onlinegames.xogame.manager;

import org.igor.onlinegames.exceptions.OnlinegamesException;
import org.igor.onlinegames.rpc.RpcMethod;
import org.igor.onlinegames.websocket.State;
import org.igor.onlinegames.websocket.StateManager;
import org.igor.onlinegames.xogame.dto.XoCellDto;
import org.igor.onlinegames.xogame.dto.XoGamePhase;
import org.igor.onlinegames.xogame.dto.XoGameStateDto;
import org.igor.onlinegames.xogame.dto.XoPlayerDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.igor.onlinegames.common.OnlinegamesUtils.nullSafeGetter;

@Component("XoGame")
@Scope("prototype")
public class XoGameState extends State {
    private XoGamePhase phase;
    private List<XoPlayerState> players;
    private List<List<Character>> field;
    private XoPlayerState playerToMove;
    private XoPlayerState winner;

    @Autowired
    private StateManager stateManager;

    @PostConstruct
    public void init() {
        phase = XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN;
        players = new ArrayList<>();

        UUID xPlayerStateId = stateManager.createNewBackendState(XoPlayerState.XO_PLAYER);
        XoPlayerState xPlayer = (XoPlayerState) stateManager.getBackendState(xPlayerStateId);
        xPlayer.setGameState(this);
        xPlayer.setJoinId(xPlayerStateId);
        xPlayer.setPlayerId(1);
        xPlayer.setPlayerSymbol('x');
        players.add(xPlayer);

        UUID oPlayerStateId = stateManager.createNewBackendState(XoPlayerState.XO_PLAYER);
        XoPlayerState oPlayer = (XoPlayerState) stateManager.getBackendState(oPlayerStateId);
        oPlayer.setGameState(this);
        oPlayer.setJoinId(oPlayerStateId);
        oPlayer.setPlayerId(2);
        oPlayer.setPlayerSymbol('o');
        players.add(oPlayer);

        playerToMove = oPlayer;

        field = new ArrayList<>(3);
        for (int x = 0; x < 3; x++) {
            field.add(new ArrayList<>());
            for (int y = 0; y < 3; y++) {
                field.get(x).add(null);
            }
        }
    }

    @RpcMethod
    public XoGameStateDto getCurrentState() {
        XoPlayerState gameOwner = new XoPlayerState();
        gameOwner.setGameOwner(true);
        return createViewOfCurrentState(gameOwner);
    }

    private XoGameStateDto createViewOfCurrentState(XoPlayerState viewer) {
        return XoGameStateDto.builder()
                .phase(phase)
                .field(createFieldDto(field))
                .players(createPlayersDto(viewer, players))
                .playerIdToMove(nullSafeGetter(playerToMove, XoPlayerState::getPlayerId))
                .winnerId(nullSafeGetter(winner, XoPlayerState::getPlayerId))
                .build();
    }

    public void makeMove(XoPlayerState player, int x, int y) {
        if (winner == null) {
            if (!(0 <= x && x < field.size() && 0 <= y && y < field.get(0).size())) {
                throw new OnlinegamesException("Incorrect coordinates: x = " + x + ", y = " + y + ".");
            }

            field.get(x).set(y, player.getPlayerSymbol());

            Character winnerSymbol = findWinnerSymbol();
            if (winnerSymbol != null) {
                playerToMove = null;
                winner = players.stream()
                        .filter(xoPlayerState -> winnerSymbol.equals(xoPlayerState.getPlayerSymbol()))
                        .findFirst()
                        .get();
                phase = XoGamePhase.FINISHED;
            }
        }
        broadcastGameState();
    }

    /**
     * This method should be called only once for each user - when the user connects to its XoPlayerState object for
     * the first time.
     * @param player a player who connected
     */
    public void playerConnected(XoPlayerState player) {
        if (players.stream().allMatch(p -> !p.isGameOwner())) {
            player.setGameOwner(true);
        }
        player.setConnected(true);
        if (phase == XoGamePhase.WAITING_FOR_PLAYERS_TO_JOIN && players.stream().allMatch(XoPlayerState::isConnected)) {
            phase = XoGamePhase.IN_PROGRESS;
        }
        broadcastGameState();
    }

    private void broadcastGameState() {
        players.forEach(xoPlayerState -> xoPlayerState.sendMessageToFe(createViewOfCurrentState(xoPlayerState)));
    }

    private List<XoPlayerDto> createPlayersDto(XoPlayerState viewer, List<XoPlayerState> players) {
        return players.stream()
                .map(xoPlayerState -> XoPlayerDto.builder()
                        .joinId(viewer.ifGameOwner(() -> xoPlayerState.getJoinId()))
                        .gameOwner(viewer.ifGameOwner(() -> xoPlayerState.isGameOwner()))
                        .playerId(xoPlayerState.getPlayerId())
                        .connected(xoPlayerState.isConnected())
                        .build()
                )
                .collect(Collectors.toList());
    }

    private List<List<XoCellDto>> createFieldDto(List<List<Character>> field) {
        List<List<XoCellDto>> dto = field.stream()
                .map(row -> row.stream()
                        .map(character -> XoCellDto.builder()
                                .symbol(character)
                                .build()
                        )
                        .collect(Collectors.toList())
                )
                .collect(Collectors.toList());

        for (int x = 0; x < dto.size(); x++) {
            for (int y = 0; y < dto.get(x).size(); y++) {
                XoCellDto cellDto = dto.get(x).get(y);
                cellDto.setXCoord(x);
                cellDto.setXCoord(y);
            }
        }

        return dto;
    }

    private Character findWinnerSymbol() {
        if (allCellsAreOfSameSymbol(0, 0, 0, 1, 0, 2)) {
            return field.get(0).get(0);
        } else if (allCellsAreOfSameSymbol(1, 0, 1, 1, 1, 2)) {
            return field.get(1).get(0);
        } else if (allCellsAreOfSameSymbol(2, 0, 2, 1, 2, 2)) {
            return field.get(2).get(0);
        } else if (allCellsAreOfSameSymbol(0, 0, 1, 0, 2, 0)) {
            return field.get(0).get(0);
        } else if (allCellsAreOfSameSymbol(0, 1, 1, 1, 2, 1)) {
            return field.get(0).get(1);
        } else if (allCellsAreOfSameSymbol(0, 2, 1, 2, 2, 2)) {
            return field.get(0).get(2);
        } else if (allCellsAreOfSameSymbol(0, 0, 1, 1, 2, 2)) {
            return field.get(0).get(0);
        } else if (allCellsAreOfSameSymbol(0, 2, 1, 1, 2, 0)) {
            return field.get(0).get(2);
        } else {
            return null;
        }
    }

    private boolean allCellsAreOfSameSymbol(int x1, int y1, int x2, int y2, int x3, int y3) {
        return field.get(x1).get(y1).equals(field.get(x2).get(y2))
                && field.get(x2).get(y2).equals(field.get(x3).get(y3));
    }

    @Override
    protected Object getViewRepresentation() {
        return getCurrentState();
    }
}
