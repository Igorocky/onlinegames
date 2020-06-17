package org.igor.onlinegames.xogame.manager;

import org.igor.onlinegames.websocket.State;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

@Component(XoPlayerState.XO_PLAYER)
@Scope("prototype")
public class XoPlayerState extends State {
    public static final String XO_PLAYER = "XoPlayer";
    private boolean connected;
    private boolean gameOwner;
    private UUID joinId;
    private long playerId;
    private Character playerSymbol;
    private XoGameState gameState;

    public void sendMessageToFe(Object msg) {
        super.sendMessageToFe(msg);
    }

    public <T> T ifGameOwner(Supplier<T> exp) {
        if (gameOwner) {
            return exp.get();
        } else {
            return null;
        }
    }

    public UUID getJoinId() {
        return joinId;
    }

    public void setJoinId(UUID joinId) {
        this.joinId = joinId;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public Character getPlayerSymbol() {
        return playerSymbol;
    }

    public void setPlayerSymbol(Character playerSymbol) {
        this.playerSymbol = playerSymbol;
    }

    public void setGameState(XoGameState gameState) {
        this.gameState = gameState;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isGameOwner() {
        return gameOwner;
    }

    public void setGameOwner(boolean gameOwner) {
        this.gameOwner = gameOwner;
    }

    @Override
    protected Object getViewRepresentation() {
        return null;
    }
}
