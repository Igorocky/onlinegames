package org.igor.onlinegames.xogame.manager;

import org.igor.onlinegames.rpc.RpcMethod;
import org.igor.onlinegames.websocket.State;
import org.igor.onlinegames.websocket.StateManager;
import org.igor.onlinegames.xogame.dto.XoCellDto;
import org.igor.onlinegames.xogame.dto.XoGameErrorDto;
import org.igor.onlinegames.xogame.dto.XoGamePhase;
import org.igor.onlinegames.xogame.dto.XoGameStateDto;
import org.igor.onlinegames.xogame.dto.XoPlayerDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.igor.onlinegames.common.OnlinegamesUtils.nullSafeGetter;

// TODO: 10.08.2020 replace Arrays.asList with loops

@Component("XoGame")
@Scope("prototype")
public class XoGameState extends State {
    private XoGamePhase phase;
    private List<XoPlayerState> players;
    private Character[][] field;
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

        playerToMove = xPlayer;

        field = new Character[3][3];
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
                .currentPlayerId(viewer.getPlayerId())
                .players(createPlayersDto(viewer, players))
                .playerIdToMove(nullSafeGetter(playerToMove, XoPlayerState::getPlayerId))
                .winnerId(nullSafeGetter(winner, XoPlayerState::getPlayerId))
                .build();
    }

    public void clickCell(XoPlayerState player, int x, int y) {
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
                            .filter(xoPlayerState -> winnerSymbol.equals(xoPlayerState.getPlayerSymbol()))
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

    private XoPlayerState getNextPlayerToMove() {
        return players.stream().filter(player -> player != playerToMove).findFirst().get();
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
                        .symbol(xoPlayerState.getPlayerSymbol())
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

    @Override
    protected Object getViewRepresentation() {
        return getCurrentState();
    }
}
