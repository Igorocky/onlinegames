package org.igor.onlinegames.model;

public interface GameState {
    boolean isWaitingForPlayersToJoin();
    String gameType();
    String gameDisplayType();
    String getTitle();
    boolean hasPasscode();
    String getShortDescription();
}
