package org.igor.onlinegames.xogame.manager;

import org.igor.onlinegames.websocket.State;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component(XoPlayerState.XO_PLAYER)
@Scope("prototype")
public class XoPlayerState extends State {
    public static final String XO_PLAYER = "XoPlayer";
    private boolean connected;
    private UUID playerId;
    private Character playerSymbol;
    private XoGameState gameState;

    public void sendMessageToFe(Object msg) {
        super.sendMessageToFe(msg);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
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

    @Override
    protected Object getViewRepresentation() {
        return null;
    }
}
