package org.igor.onlinegames.xogame.manager;

import lombok.Getter;
import lombok.Setter;
import org.igor.onlinegames.rpc.RpcMethod;
import org.igor.onlinegames.websocket.State;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

@Component(XoPlayerState.XO_PLAYER)
@Scope("prototype")
public class XoPlayerState extends State {
    public static final String XO_PLAYER = "XoPlayer";

    @Setter @Getter
    private boolean connected;

    @Setter @Getter
    private boolean gameOwner;

    @Setter @Getter
    private UUID joinId;

    @Setter @Getter
    private int playerId;

    @Setter @Getter
    private Character playerSymbol;

    @Setter
    private XoGameState gameState;

    @RpcMethod
    public void connect() {
        gameState.playerConnected(this);
    }

    @RpcMethod
    public void clickCell(int x, int y) {
        gameState.clickCell(this, x, y);
    }

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

    @Override
    protected Object getViewRepresentation() {
        return "joinId = " + joinId;
    }
}
